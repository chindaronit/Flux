package com.flux.other

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.flux.data.database.BackupMediaFile
import com.flux.data.database.FluxBackup
import com.flux.data.database.FluxDatabase
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.toScheduleRequest
import com.flux.other.crypto.BackupEncryptor
import com.flux.other.crypto.BackupFileFormat
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

object FullBackupArchive {
    private const val MANIFEST_FILE = "backup.json"
    private const val COMPLETE_MARKER = ".complete"
    private const val MEDIA_DIRECTORY = "media"
    private const val IMAGE_DIRECTORY = "images"
    private const val AUDIO_DIRECTORY = "audio"
    private const val VIDEO_DIRECTORY = "videos"
    private const val DOCUMENT_DIRECTORY = "documents"
    private const val RESTORE_STAGING_DIRECTORY = "RestoreStaging"

    private val mediaDirectories = listOf(
        IMAGE_DIRECTORY to Constants.File.FLUX_IMAGES,
        AUDIO_DIRECTORY to Constants.File.FLUX_AUDIO,
        VIDEO_DIRECTORY to Constants.File.FLUX_VIDEOS,
        DOCUMENT_DIRECTORY to Constants.File.FLUX_DOCUMENTS
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    class PasswordRequiredException : Exception()

    suspend fun create(
        context: Context,
        db: FluxDatabase,
        storageRootUri: Uri,
        encryptor: BackupEncryptor,
        password: CharArray?
    ) {
        val root = DocumentFile.fromTreeUri(context, storageRootUri)
            ?: throw IOException("The storage root is unavailable")
        val fluxDirectory = findOrCreateDirectory(root, Constants.File.FLUX)
        val backupParent = findOrCreateDirectory(fluxDirectory, Constants.File.FLUX_BACKUP)
        val snapshot = FluxBackup(
            workspaces = db.workspaceDao.getAll(),
            notes = db.notesDao.loadAllNotes(),
            todos = db.todoDao.loadAllLists(),
            todoInstances = db.todoInstanceDao.loadAllInstances(),
            habits = db.habitDao.loadAllHabits(),
            habitInstances = db.habitInstanceDao.loadAllInstances(),
            journals = db.journalDao.loadAllEntries(),
            labels = db.labelDao.getAll(),
            events = db.eventDao.loadAllEvents(),
            eventInstances = db.eventInstanceDao.getAll(),
            eventExceptions = db.eventExceptionDao.getAll(),
            progressBoardItems = db.progressBoardDao.getAllBoardItems()
        )

        val backupDirectory = createUniqueDirectory(backupParent, "FullBackup-${System.currentTimeMillis()}")
        val mediaDirectory = findOrCreateDirectory(backupDirectory, MEDIA_DIRECTORY)
        val outputDirectories = mediaDirectories.associate { (archiveName, _) ->
            archiveName to findOrCreateDirectory(mediaDirectory, archiveName)
        }
        val archivedNames = mutableMapOf<Pair<String, String>, String>()
        val mediaFiles = mutableListOf<BackupMediaFile>()
        val sourceFluxDirectory = root.findFile(Constants.File.FLUX)
        val sourceImagesDirectory = sourceFluxDirectory?.findFile(Constants.File.FLUX_IMAGES)
        mediaDirectories.forEach { (archiveName, sourceName) ->
            val sourceDirectory = sourceFluxDirectory?.findFile(sourceName)
            if (sourceDirectory?.isDirectory == true) {
                sourceDirectory.listFiles().filterNot { it.isDirectory }.forEach { source ->
                    val name = requireSafeFileName(source.name)
                    val archivedName = if (password == null) name else "media-${UUID.randomUUID()}"
                    if (archivedNames.put(archiveName to name, archivedName) != null) {
                        throw IOException("Media contains duplicate file names")
                    }
                    val mimeType = source.type ?: "application/octet-stream"
                    mediaFiles += BackupMediaFile(archiveName, name, archivedName, mimeType)
                    copyDocument(
                        context,
                        source,
                        outputDirectories.getValue(archiveName),
                        archivedName,
                        encryptor,
                        password,
                        if (password == null) CopyMode.PLAIN else CopyMode.ENCRYPT,
                        mimeType
                    )
                }
            }
        }

        val coverFiles = snapshot.workspaces.mapNotNull { workspace ->
            if (workspace.cover.isBlank()) return@mapNotNull null

            val coverUri = Uri.parse(workspace.cover)
            val coverName = coverUri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("Could not identify the cover image for ${workspace.title}")
            val coverFile = sourceImagesDirectory?.findFile(coverName)
                ?: throw IOException("Could not find the cover image for ${workspace.title} in Flux/Images")
            val name = "workspace-cover-${UUID.randomUUID()}"
            val archivedName = if (password == null) name else "media-${UUID.randomUUID()}"
            val archiveImages = outputDirectories.getValue(IMAGE_DIRECTORY)
            val mimeType = coverFile.type ?: "application/octet-stream"
            mediaFiles += BackupMediaFile(IMAGE_DIRECTORY, name, archivedName, mimeType)
            copyDocument(
                context,
                coverFile,
                archiveImages,
                archivedName,
                encryptor,
                password,
                if (password == null) CopyMode.PLAIN else CopyMode.ENCRYPT,
                mimeType
            )
            workspace.workspaceId to name
        }.toMap()

        val portableSnapshot = snapshot.copy(
            workspaces = snapshot.workspaces.map { it.copy(cover = "") },
            workspaceCovers = coverFiles,
            mediaFiles = mediaFiles
        )
        writeManifest(
            context,
            backupDirectory,
            json.encodeToString(FluxBackup.serializer(), portableSnapshot),
            encryptor,
            password
        )
        writeCompleteMarker(context, backupDirectory)
    }

    suspend fun restore(
        context: Context,
        db: FluxDatabase,
        backupFolderUri: Uri,
        storageRootUri: Uri?,
        encryptor: BackupEncryptor,
        password: CharArray?
    ) {
        val backupDirectory = DocumentFile.fromTreeUri(context, backupFolderUri)
            ?: throw IOException("The selected backup folder is unavailable")
        val completeMarker = backupDirectory.findFile(COMPLETE_MARKER)
        if (completeMarker != null) {
            val markerContents = context.contentResolver.openInputStream(completeMarker.uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (markerContents != "2") throw IOException("The backup was not completed successfully")
        }
        val manifest = backupDirectory.findFile(MANIFEST_FILE)
            ?: throw IOException("The selected folder does not contain a Flux backup")
        val encryptedManifest = context.contentResolver.openInputStream(manifest.uri)?.use {
            it.readBytes()
        } ?: throw IOException("Could not read the backup manifest")
        val isEncrypted = BackupFileFormat.isEncrypted(encryptedManifest)
        if (isEncrypted && password == null) throw PasswordRequiredException()
        val manifestBytes = if (isEncrypted) {
            encryptor.decrypt(encryptedManifest, password!!)
        } else {
            encryptedManifest
        }
        val backup = json.decodeFromString<FluxBackup>(manifestBytes.toString(Charsets.UTF_8))
        require(backup.formatVersion in 2..3) {
            "Unsupported backup version: ${backup.formatVersion}"
        }

        val archiveMedia = backupDirectory.findFile(MEDIA_DIRECTORY)
        if (archiveMedia?.isDirectory != true) {
            throw IOException("The backup is missing its media folder")
        }
        val hasMediaFiles = backup.formatVersion >= 3 ||
            backup.workspaceCovers.isNotEmpty() ||
            backup.mediaFiles.isNotEmpty() ||
            mediaDirectories.any { (name, _) ->
                archiveMedia.findFile(name)?.listFiles()?.any { it.isFile } == true
            }
        val storageRoot = if (hasMediaFiles) {
            val uri = storageRootUri ?: throw IOException("Select a storage root before restoring media")
            DocumentFile.fromTreeUri(context, uri)
                ?: throw IOException("The storage root is unavailable")
        } else {
            storageRootUri?.let { DocumentFile.fromTreeUri(context, it) }
        }

        val restoredMediaUris = if (storageRoot != null) {
            restoreMedia(context, backup, archiveMedia, storageRoot, encryptor, password, isEncrypted)
                .also { MediaCache.clearCaches() }
        } else {
            emptyMap()
        }
        val restoredWorkspaces = backup.workspaces.map { workspace ->
            val coverName = backup.workspaceCovers[workspace.workspaceId]
            if (coverName == null) {
                workspace
            } else {
                val coverUri = restoredMediaUris[IMAGE_DIRECTORY to requireSafeFileName(coverName)]
                    ?: throw IOException("The backup is missing workspace cover $coverName")
                workspace.copy(cover = coverUri.toString())
            }
        }

        val previousTodos = db.todoDao.loadAllLists()
            .filter { it.recurrence == RecurrenceRule.Weekly }
        val previousHabits = db.habitDao.loadAllHabits()
        val previousEvents = db.eventDao.loadAllEvents()
        db.withTransaction {
            db.todoInstanceDao.deleteAllInstances()
            db.eventExceptionDao.deleteAll()
            db.eventInstanceDao.deleteAll()
            db.habitInstanceDao.deleteAllInstances()
            db.progressBoardDao.deleteAll()
            db.notesDao.deleteAll()
            db.journalDao.deleteAll()
            db.labelDao.deleteAll()
            db.todoDao.deleteAll()
            db.habitDao.deleteAll()
            db.eventDao.deleteAll()
            db.workspaceDao.deleteAll()

            db.workspaceDao.upsertWorkspaces(restoredWorkspaces)
            backup.notes.forEach { db.notesDao.upsertNote(it) }
            backup.todos.forEach { db.todoDao.upsertList(it) }
            backup.todoInstances.forEach { db.todoInstanceDao.upsertTodoInstance(it) }
            backup.habits.forEach { db.habitDao.upsertHabit(it) }
            backup.habitInstances.forEach { db.habitInstanceDao.upsertInstance(it) }
            backup.journals.forEach { db.journalDao.upsertEntry(it) }
            backup.labels.forEach { db.labelDao.upsertLabel(it) }
            backup.events.forEach { db.eventDao.upsertEvent(it) }
            backup.eventInstances.forEach { db.eventInstanceDao.upsertEventInstance(it) }
            backup.eventExceptions.forEach { db.eventExceptionDao.upsert(it) }
            backup.progressBoardItems.forEach { db.progressBoardDao.upsertBoardItem(it) }
        }

        previousTodos.forEach { cancelReminder(context, it.toScheduleRequest()) }
        previousHabits.forEach { cancelReminder(context, it.toScheduleRequest()) }
        previousEvents.forEach { cancelReminder(context, it.toScheduleRequest()) }
        backup.todos
            .filter { it.recurrence == RecurrenceRule.Weekly }
            .forEach { scheduleNextReminder(context, it.toScheduleRequest()) }
        backup.habits.forEach { scheduleNextReminder(context, it.toScheduleRequest()) }
        backup.events.forEach { scheduleNextReminder(context, it.toScheduleRequest()) }
    }

    private suspend fun restoreMedia(
        context: Context,
        backup: FluxBackup,
        archiveMedia: DocumentFile,
        storageRoot: DocumentFile,
        encryptor: BackupEncryptor,
        password: CharArray?,
        isEncrypted: Boolean
    ): Map<Pair<String, String>, Uri> {
        val fluxDirectory = findOrCreateDirectory(storageRoot, Constants.File.FLUX)
        val stagingDirectory = createUniqueDirectory(
            fluxDirectory,
            "$RESTORE_STAGING_DIRECTORY-${System.currentTimeMillis()}"
        )
        try {
            val stagingMedia = findOrCreateDirectory(stagingDirectory, MEDIA_DIRECTORY)
            val archivedMediaFiles = backup.mediaFiles.associateBy {
                it.directory to it.archivedName
            }
            if (archivedMediaFiles.size != backup.mediaFiles.size) {
                throw IOException("Backup contains duplicate media entries")
            }
            val restoredArchiveNames = mutableSetOf<Pair<String, String>>()
            mediaDirectories.forEach { (archiveName, _) ->
                val stagedCategory = findOrCreateDirectory(stagingMedia, archiveName)
                val archiveCategory = archiveMedia.findFile(archiveName)
                if (archiveCategory?.isDirectory == true) {
                    archiveCategory.listFiles().filterNot { it.isDirectory }.forEach { source ->
                        val archivedName = requireSafeFileName(source.name)
                        val key = archiveName to archivedName
                        val mediaEntry = if (backup.mediaFiles.isEmpty()) {
                            null
                        } else {
                            val entry = archivedMediaFiles[key]
                                ?: throw IOException("Backup contains an unlisted media file")
                            restoredArchiveNames += key
                            entry
                        }
                        val name = requireSafeFileName(mediaEntry?.name ?: archivedName)
                        copyDocument(
                            context,
                            source,
                            stagedCategory,
                            name,
                            encryptor,
                            password,
                            if (isEncrypted) CopyMode.DECRYPT else CopyMode.PLAIN,
                            mediaEntry?.mimeType
                        )
                    }
                }
            }
            if (restoredArchiveNames.size != archivedMediaFiles.size) {
                throw IOException("Backup is missing one or more media files")
            }

            val restoredUris = mutableMapOf<Pair<String, String>, Uri>()
            mediaDirectories.forEach { (archiveName, storageName) ->
                if (archiveName == DOCUMENT_DIRECTORY && backup.formatVersion < 3) {
                    return@forEach
                }
                val target = findOrCreateDirectory(fluxDirectory, storageName)
                deleteChildren(target)
                val stagedCategory = stagingMedia.findFile(archiveName)
                stagedCategory?.listFiles()?.filterNot { it.isDirectory }?.forEach { stagedFile ->
                    val name = requireSafeFileName(stagedFile.name)
                    val restored = copyDocument(
                        context,
                        stagedFile,
                        target,
                        name,
                        encryptor,
                        password,
                        CopyMode.PLAIN
                    )
                    restoredUris[archiveName to name] = restored.uri
                }
            }

            backup.workspaceCovers.values.forEach { coverName ->
                if ((IMAGE_DIRECTORY to requireSafeFileName(coverName)) !in restoredUris) {
                    throw IOException("The backup is missing workspace cover $coverName")
                }
            }
            return restoredUris
        } finally {
            deleteDirectory(stagingDirectory)
        }
    }

    private fun findOrCreateDirectory(parent: DocumentFile, name: String): DocumentFile {
        parent.findFile(name)?.let { existing ->
            if (existing.isDirectory) return existing
            throw IOException("$name exists but is not a folder")
        }
        return parent.createDirectory(name) ?: throw IOException("Could not create folder $name")
    }

    private fun createUniqueDirectory(parent: DocumentFile, baseName: String): DocumentFile {
        repeat(100) { suffix ->
            val name = if (suffix == 0) baseName else "$baseName-$suffix"
            if (parent.findFile(name) == null) {
                return parent.createDirectory(name)
                    ?: throw IOException("Could not create folder $name")
            }
        }
        throw IOException("Could not create a unique backup folder")
    }

    private suspend fun copyDocument(
        context: Context,
        source: DocumentFile,
        destinationDirectory: DocumentFile,
        name: String,
        encryptor: BackupEncryptor,
        password: CharArray?,
        mode: CopyMode,
        mimeType: String? = null
    ): DocumentFile {
        val outputFile = destinationDirectory.createFile(
            if (mode == CopyMode.ENCRYPT) "application/octet-stream"
            else mimeType ?: source.type ?: "application/octet-stream",
            requireSafeFileName(name)
        ) ?: throw IOException("Could not create file $name")
        try {
            val input = context.contentResolver.openInputStream(source.uri)
                ?: throw IOException("Could not read file $name")
            input.use { sourceStream ->
                val output = context.contentResolver.openOutputStream(outputFile.uri)
                    ?: throw IOException("Could not write file $name")
                output.use { destinationStream ->
                    when (mode) {
                        CopyMode.PLAIN -> sourceStream.copyTo(destinationStream)
                        CopyMode.ENCRYPT -> {
                            val actualPassword = password
                                ?: throw IllegalStateException("Encryption password is missing")
                            encryptor.encrypt(sourceStream, destinationStream, actualPassword)
                        }
                        CopyMode.DECRYPT -> {
                            val actualPassword = password
                                ?: throw PasswordRequiredException()
                            encryptor.decrypt(sourceStream, destinationStream, actualPassword)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            outputFile.delete()
            throw exception
        }
        return outputFile
    }

    private suspend fun writeManifest(
        context: Context,
        directory: DocumentFile,
        text: String,
        encryptor: BackupEncryptor,
        password: CharArray?
    ) {
        val file = directory.createFile(
            if (password == null) "application/json" else "application/octet-stream",
            MANIFEST_FILE
        )
            ?: throw IOException("Could not create the backup manifest")
        val output = context.contentResolver.openOutputStream(file.uri)
            ?: throw IOException("Could not write the backup manifest")
        output.use { destination ->
            val manifestBytes = text.toByteArray(Charsets.UTF_8)
            if (password == null) {
                destination.write(manifestBytes)
            } else {
                encryptor.encrypt(ByteArrayInputStream(manifestBytes), destination, password)
            }
        }
    }

    private fun writeCompleteMarker(context: Context, directory: DocumentFile) {
        val marker = directory.createFile("text/plain", COMPLETE_MARKER)
            ?: throw IOException("Could not mark the backup as complete")
        context.contentResolver.openOutputStream(marker.uri)?.bufferedWriter()?.use { writer ->
            writer.write("2")
        } ?: throw IOException("Could not mark the backup as complete")
    }

    private fun deleteChildren(directory: DocumentFile) {
        directory.listFiles().forEach { child ->
            if (child.isDirectory) deleteChildren(child)
            if (!child.delete()) throw IOException("Could not replace ${child.name}")
        }
    }

    private fun deleteDirectory(directory: DocumentFile) {
        deleteChildren(directory)
        if (!directory.delete()) throw IOException("Could not remove ${directory.name}")
    }

    private fun requireSafeFileName(name: String?): String {
        if (name.isNullOrBlank() || name == "." || name == ".." ||
            '/' in name || '\\' in name
        ) {
            throw IOException("Backup contains an invalid file name")
        }
        return name
    }

    private enum class CopyMode {
        PLAIN,
        ENCRYPT,
        DECRYPT
    }
}
