package com.flux.ui.viewModel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.database.FluxDatabase
import com.flux.data.model.SettingsModel
import com.flux.data.repository.SettingsRepository
import com.flux.other.Constants
import com.flux.other.MediaCache
import com.flux.other.getOrCreateDirectory
import com.flux.ui.effects.ScreenEffect
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.RootChangeState
import com.flux.ui.state.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class CopyResult(
    val success: Boolean,
    val totalFiles: Int,
    val copiedFiles: Int,
    val failedFiles: List<String>
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val repository: SettingsRepository,
    val database: FluxDatabase
) : ViewModel() {

    private val _state: MutableStateFlow<Settings> = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()
    private val _effect: Channel<ScreenEffect> = Channel()
    val effect = _effect.receiveAsFlow()

    private val _rootChangeState = MutableStateFlow<RootChangeState>(RootChangeState.Idle)
    val rootChangeState = _rootChangeState.asStateFlow()

    init { loadSettings() }

    private fun setEffect(builder: () -> ScreenEffect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }

    fun onEvent(event: SettingEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private fun updateState(reducer: (Settings) -> Settings) {
        _state.value = reducer(_state.value)
    }

    private fun reduce(event: SettingEvents) {
        when (event) {
            is SettingEvents.UpdateSettings -> updateSettings(event.data)
            is SettingEvents.ResetDatabase -> resetDatabase()
            is SettingEvents.ChangeStorageRoot -> changeStorageRoot(event.context, event.newRootUri)
            is SettingEvents.ResetRootChange -> resetRootChangeState()
        }
    }

    private fun loadSettings() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.loadSettings().collect { data ->
                if (data != null){ updateState { it.copy(isLoading = false, data = data) } }
                else updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateSettings(data: SettingsModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.upsertSettings(data) }
    }

    suspend fun isStorageReady(): Boolean {
        return repository.hasValidStorageRoot()
    }

    fun saveRootUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStorageRoot(uri)
        }
    }

    suspend fun copyDirectoryRecursively(
        context: Context,
        source: DocumentFile,
        destinationParent: DocumentFile,
        onProgress: (copied: Int, total: Int) -> Unit = { _, _ -> }
    ): CopyResult = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<DocumentFile>()

        fun collect(dir: DocumentFile) {
            dir.listFiles().forEach { entry ->
                if (entry.isDirectory) collect(entry) else allFiles.add(entry)
            }
        }
        collect(source)

        val total = allFiles.size
        val copiedCount = AtomicInteger(0)
        val failedFiles = mutableListOf<String>()

        onProgress(0, total)

        copyRecursive(
            context = context,
            src = source,
            destParent = destinationParent,
            total = total,
            copiedCount = copiedCount,
            failedFiles = failedFiles,
            onProgress = onProgress
        )

        CopyResult(
            success = failedFiles.isEmpty(),
            totalFiles = total,
            copiedFiles = copiedCount.get(),
            failedFiles = failedFiles
        )
    }

    private fun copyRecursive(
        context: Context,
        src: DocumentFile,
        destParent: DocumentFile,
        total: Int,
        copiedCount: AtomicInteger,
        failedFiles: MutableList<String>,
        onProgress: (copied: Int, total: Int) -> Unit
    ) {
        src.listFiles().forEach { child ->
            val name = child.name ?: return@forEach

            if (child.isDirectory) {
                val newDir = destParent.findFile(name)
                    ?.takeIf { it.isDirectory }
                    ?: destParent.createDirectory(name)

                if (newDir == null) {
                    failedFiles.add("$name/ (could not create directory)")
                    return@forEach
                }

                copyRecursive(context, child, newDir, total, copiedCount, failedFiles, onProgress)
            } else {
                val mime = child.type ?: "application/octet-stream"
                val existing = destParent.findFile(name)

                if (existing != null && !existing.isDirectory && existing.length() == child.length()) {
                    copiedCount.incrementAndGet()
                    onProgress(copiedCount.get(), total)
                    return@forEach
                }

                if (existing != null && existing.isDirectory) {
                    failedFiles.add(name)
                    return@forEach
                }

                existing?.delete()

                val newFile = destParent.createFile(mime, name)
                if (newFile == null) {
                    failedFiles.add(name)
                    return@forEach
                }

                var copiedOk = false
                try {
                    context.contentResolver.openInputStream(child.uri)?.use { input ->
                        context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { output ->
                            input.copyTo(output)
                            copiedOk = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    copiedOk = false
                }

                if (!copiedOk) {
                    newFile.delete()
                    failedFiles.add(name)
                    return@forEach
                }

                val sourceLength = child.length()
                val destLength = newFile.length()
                if (sourceLength > 0 && destLength != sourceLength) {
                    newFile.delete()
                    failedFiles.add(name)
                    return@forEach
                }

                copiedCount.incrementAndGet()
                onProgress(copiedCount.get(), total)
            }
        }
    }

    fun changeStorageRoot(context: Context, newRootUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _rootChangeState.value = RootChangeState.Copying()
            try {
                context.contentResolver.takePersistableUriPermission(
                    newRootUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val oldRootUri = repository.getStorageRoot()
                val oldFluxDir = oldRootUri.let {
                    DocumentFile.fromTreeUri(context, it)?.findFile(Constants.File.FLUX)
                }
                val newFluxDir = getOrCreateDirectory(context, newRootUri, Constants.File.FLUX)
                    ?: run {
                        _rootChangeState.value = RootChangeState.Failed("Could not create Flux folder in the new location")
                        return@launch
                    }

                if (oldFluxDir != null) {
                    val result = copyDirectoryRecursively(
                        context = context,
                        source = oldFluxDir,
                        destinationParent = newFluxDir,
                        onProgress = { copied, total ->
                            _rootChangeState.value = RootChangeState.Copying(copied, total)
                        }
                    )

                    if (!result.success) {
                        _rootChangeState.value = RootChangeState.Failed(
                            reason = "${result.failedFiles.size} of ${result.totalFiles} files failed to copy",
                            failedFiles = result.failedFiles
                        )
                        return@launch
                    }
                }

                repository.saveStorageRoot(newRootUri)
                MediaCache.clearCaches()
                _rootChangeState.value = RootChangeState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _rootChangeState.value = RootChangeState.Failed(e.message ?: "Unknown error")
            }
        }
    }

    fun resetRootChangeState() {
        _rootChangeState.value = RootChangeState.Idle
    }

    fun resetDatabase() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }
            }.onSuccess {
                setEffect {
                    ScreenEffect.ShowSnackBarMessage("Reset completed")
                }
            }.onFailure { throwable ->
                setEffect {
                    ScreenEffect.ShowSnackBarMessage(
                        "Failed to reset database: ${throwable.localizedMessage ?: "error"}"
                    )
                }
            }
        }
    }
}
