package com.flux.other

import com.flux.data.database.FluxBackup
import com.flux.data.model.EventExceptionModel
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.EventOccurrenceColorModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.ProgressBoardModel
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.flux.other.crypto.AesGcmBackupEncryptor
import com.flux.other.crypto.BackupCryptoException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private val testJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    classDiscriminator = "type"
}

private fun BackupDocument.readBytes(): ByteArray =
    openInputStream()!!.use { it.readBytes() }

private fun BackupDocument.readText(): String = readBytes().toString(Charsets.UTF_8)

private fun BackupDocument.writeBytes(bytes: ByteArray) {
    openOutputStream()!!.use { it.write(bytes) }
}

private fun BackupDocument.overwrite(bytes: ByteArray) = writeBytes(bytes)

class FullBackupArchiveTest {
    private val encryptor = AesGcmBackupEncryptor()

    @Test
    fun plainBackupAndRestorePreservesMediaAndWorkspaceCoverWithoutDuplicate() = runBlocking {
        val source = sourceRoot()
        val coverBytes = "cover-image-content".toByteArray()
        val photoBytes = "other-image-content".toByteArray()
        val audioBytes = byteArrayOf(1, 2, 3, 4)
        val videoBytes = byteArrayOf(5, 6, 7, 8)
        val documentBytes = "document".toByteArray()
        source.addMedia(Constants.File.FLUX_IMAGES, "cover.jpg", "image/jpeg", coverBytes)
        source.addMedia(Constants.File.FLUX_IMAGES, "photo.png", "image/png", photoBytes)
        source.addMedia(Constants.File.FLUX_AUDIO, "voice.m4a", "audio/mp4", audioBytes)
        source.addMedia(Constants.File.FLUX_VIDEOS, "clip.mp4", "video/mp4", videoBytes)
        source.addMedia(Constants.File.FLUX_DOCUMENTS, "notes.pdf", "application/pdf", documentBytes)
        val workspace = WorkspaceModel(
            workspaceId = "workspace-1",
            title = "Workspace",
            cover = "content://flux/Images/cover.jpg"
        )
        val store = FakeBackupDataStore(FluxBackup(workspaces = listOf(workspace)))

        FullBackupArchive.create(source, store, encryptor, password = null)

        val backup = source.backupFolder()
        assertNotNull(backup.findFile(".complete"))
        assertEquals("2", backup.findFile(".complete")!!.readText())
        val archived = readManifest(backup)
        assertEquals(mapOf(workspace.workspaceId to "cover.jpg"), archived.workspaceCovers)
        assertEquals(5, archived.mediaFiles.size)
        assertFalse(archived.mediaFiles.any { it.name.startsWith("workspace-cover-") })
        assertEquals("", archived.workspaces.single().cover)
        assertArrayEquals(coverBytes, backup.media("images", "cover.jpg").readBytes())
        assertArrayEquals(photoBytes, backup.media("images", "photo.png").readBytes())
        assertArrayEquals(audioBytes, backup.media("audio", "voice.m4a").readBytes())
        assertArrayEquals(videoBytes, backup.media("videos", "clip.mp4").readBytes())
        assertArrayEquals(documentBytes, backup.media("documents", "notes.pdf").readBytes())

        val destination = MemoryDocument.directory("restore-root")
        val oldImages = destination.ensureDirectory("Flux").ensureDirectory(Constants.File.FLUX_IMAGES)
        oldImages.createFile("image/jpeg", "obsolete.jpg")!!.writeBytes(byteArrayOf(99))
        val restoredStore = FakeBackupDataStore()
        var mediaRestoredCallbackCalled = false
        FullBackupArchive.restore(
            backup,
            destination,
            restoredStore,
            encryptor,
            password = null
        ) { mediaRestoredCallbackCalled = true }

        assertTrue(mediaRestoredCallbackCalled)
        assertArrayEquals(coverBytes, destination.media(Constants.File.FLUX_IMAGES, "cover.jpg").readBytes())
        assertArrayEquals(photoBytes, destination.media(Constants.File.FLUX_IMAGES, "photo.png").readBytes())
        assertArrayEquals(audioBytes, destination.media(Constants.File.FLUX_AUDIO, "voice.m4a").readBytes())
        assertArrayEquals(videoBytes, destination.media(Constants.File.FLUX_VIDEOS, "clip.mp4").readBytes())
        assertArrayEquals(
            documentBytes,
            destination.media(Constants.File.FLUX_DOCUMENTS, "notes.pdf").readBytes()
        )
        assertNull(destination.mediaOrNull(Constants.File.FLUX_IMAGES, "obsolete.jpg"))
        assertEquals(
            destination.media(Constants.File.FLUX_IMAGES, "cover.jpg").uri,
            restoredStore.restoredBackup!!.workspaces.single().cover
        )
        assertEquals(archived.mediaFiles, restoredStore.restoredBackup!!.mediaFiles)
        assertTrue(destination.findFile("Flux")!!.listFiles().none { it.name!!.startsWith("RestoreStaging-") })
    }

    @Test
    fun encryptedBackupAndRestoreUsesOpaqueNamesAndDoesNotExposeMediaBytes() = runBlocking {
        val source = sourceRoot()
        val firstBytes = ByteArray(170_123) { (it * 31).toByte() }
        val secondBytes = "private document data".toByteArray()
        source.addMedia(Constants.File.FLUX_IMAGES, "secret.jpg", "image/jpeg", firstBytes)
        source.addMedia(Constants.File.FLUX_DOCUMENTS, "secret.pdf", "application/pdf", secondBytes)
        val workspace = WorkspaceModel(
            workspaceId = "encrypted-cover",
            cover = "content://flux/Images/secret.jpg"
        )
        val store = FakeBackupDataStore(FluxBackup(workspaces = listOf(workspace)))
        val password = "strong test password".toCharArray()

        FullBackupArchive.create(source, store, encryptor, password)

        val backup = source.backupFolder()
        val encryptedManifest = backup.findFile("backup.json")!!.readBytes()
        val decryptedManifest = encryptor.decryptManifest(encryptedManifest, password)
        val manifest = testJson.decodeFromString<FluxBackup>(
            decryptedManifest.plaintext.toString(Charsets.UTF_8)
        )
        assertEquals(2, manifest.mediaFiles.size)
        assertEquals(mapOf(workspace.workspaceId to "secret.jpg"), manifest.workspaceCovers)

        manifest.mediaFiles.forEach { entry ->
            assertTrue(entry.archivedName.startsWith("media-"))
            assertFalse(
                backup.media(entry.directory, entry.archivedName).readBytes()
                    .containsSubsequence(secondBytes)
            )
        }
        assertFalse(backup.readAllBytes().containsSubsequence(secondBytes))

        val destination = MemoryDocument.directory("restore-root")
        val restoredStore = FakeBackupDataStore()
        FullBackupArchive.restore(backup, destination, restoredStore, encryptor, password)
        assertArrayEquals(
            firstBytes,
            destination.media(Constants.File.FLUX_IMAGES, "secret.jpg").readBytes()
        )
        assertArrayEquals(
            secondBytes,
            destination.media(Constants.File.FLUX_DOCUMENTS, "secret.pdf").readBytes()
        )
        assertEquals(
            destination.media(Constants.File.FLUX_IMAGES, "secret.jpg").uri,
            restoredStore.restoredBackup!!.workspaces.single().cover
        )
    }

    @Test
    fun manifestRoundTripsEveryDatabaseCollection() = runBlocking {
        val source = sourceRoot()
        val backupData = FluxBackup(
            workspaces = listOf(WorkspaceModel(workspaceId = "workspace", title = "title")),
            notes = listOf(NotesModel(notesId = "note", title = "note title")),
            todos = listOf(TodoModel(id = "todo", title = "todo title")),
            todoInstances = listOf(TodoInstance(id = "todo-instance", todoId = "todo")),
            habits = listOf(HabitModel(id = "habit", title = "habit title")),
            habitInstances = listOf(HabitInstanceModel(habitId = "habit")),
            journals = listOf(JournalModel(journalId = "journal", text = "journal text")),
            labels = listOf(LabelModel(labelId = "label", value = "label value")),
            events = listOf(EventModel(id = "event", title = "event title")),
            eventInstances = listOf(EventInstanceModel(eventId = "event")),
            eventExceptions = listOf(
                EventExceptionModel(eventId = "event", occurrenceDate = 123, workspaceId = "workspace")
            ),
            eventOccurrenceColors = listOf(
                EventOccurrenceColorModel(
                    eventId = "event",
                    occurrenceDate = 123,
                    workspaceId = "workspace",
                    colorArgb = 0xFFE57373.toInt()
                )
            ),
            progressBoardItems = listOf(ProgressBoardModel(itemId = "progress", title = "progress title"))
        )
        val store = FakeBackupDataStore(backupData)

        FullBackupArchive.create(source, store, encryptor, password = null)
        val archive = source.backupFolder()
        val manifest = readManifest(archive)
        val restoredStore = FakeBackupDataStore()
        FullBackupArchive.restore(
            archive,
            MemoryDocument.directory("restore-root"),
            restoredStore,
            encryptor,
            password = null
        )

        assertEquals(backupData.copy(workspaces = backupData.workspaces.map { it.copy(cover = "") }), manifest)
        assertEquals(manifest, restoredStore.restoredBackup)
    }

    @Test
    fun wrongPasswordDoesNotReplaceDataStoreOrLeaveRestoreStaging() = runBlocking {
        val source = sourceRoot()
        source.addMedia(Constants.File.FLUX_IMAGES, "private.jpg", "image/jpeg", byteArrayOf(7, 8, 9))
        FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, "correct".toCharArray())
        val destination = MemoryDocument.directory("restore-root")
        val store = FakeBackupDataStore()

        val failure = runCatching {
            FullBackupArchive.restore(
                source.backupFolder(),
                destination,
                store,
                encryptor,
                "incorrect".toCharArray()
            )
        }.exceptionOrNull()

        assertTrue(failure is BackupCryptoException.WrongPasswordOrCorrupted)
        assertNull(store.restoredBackup)
        assertNull(destination.findFile("Flux"))
    }

    @Test
    fun encryptedMediaTamperingFailsBeforeDataStoreReplacementAndCleansStaging() = runBlocking {
        val source = sourceRoot()
        source.addMedia(Constants.File.FLUX_IMAGES, "photo.jpg", "image/jpeg", ByteArray(80_000) { 4 })
        FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, "password".toCharArray())
        val backup = source.backupFolder()
        val manifest = encryptor.decryptManifest(
            backup.findFile("backup.json")!!.readBytes(),
            "password".toCharArray()
        )
        val decoded = testJson.decodeFromString<FluxBackup>(manifest.plaintext.toString(Charsets.UTF_8))
        val entry = decoded.mediaFiles.single()
        val encryptedMedia = backup.media(entry.directory, entry.archivedName)
        val bytes = encryptedMedia.readBytes()
        bytes[bytes.lastIndex - 2] = (bytes[bytes.lastIndex - 2].toInt() xor 0x40).toByte()
        encryptedMedia.overwrite(bytes)

        val destination = MemoryDocument.directory("restore-root")
        val store = FakeBackupDataStore()
        val failure = runCatching {
            FullBackupArchive.restore(backup, destination, store, encryptor, "password".toCharArray())
        }.exceptionOrNull()

        assertTrue(failure is BackupCryptoException.WrongPasswordOrCorrupted)
        assertNull(store.restoredBackup)
        assertTrue(destination.findFile("Flux")!!.listFiles().none { it.name!!.startsWith("RestoreStaging-") })
    }

    @Test
    fun restoreRequiresStorageRootBeforeChangingDataStoreWhenMediaExists() = runBlocking {
        val source = sourceRoot()
        source.addMedia(Constants.File.FLUX_IMAGES, "photo.jpg", "image/jpeg", byteArrayOf(1))
        FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, password = null)
        val store = FakeBackupDataStore()

        val failure = runCatching {
            FullBackupArchive.restore(source.backupFolder(), null, store, encryptor, password = null)
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.contains("storage root"))
        assertNull(store.restoredBackup)
    }

    @Test
    fun incompleteBackupMarkerRejectsRestore() = runBlocking {
        val source = sourceRoot()
        FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, password = null)
        val backup = source.backupFolder()
        backup.findFile(".complete")!!.overwrite("failed".toByteArray())
        val store = FakeBackupDataStore()

        val failure = runCatching {
            FullBackupArchive.restore(
                backup,
                MemoryDocument.directory("restore-root"),
                store,
                encryptor,
                password = null
            )
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.contains("not completed"))
        assertNull(store.restoredBackup)
    }

    @Test
    fun createRejectsWorkspaceCoverThatIsNotAmongArchivedImages() = runBlocking {
        val source = sourceRoot()
        val store = FakeBackupDataStore(
            FluxBackup(workspaces = listOf(WorkspaceModel(workspaceId = "w", cover = "content://x/missing.jpg")))
        )

        val failure = runCatching {
            FullBackupArchive.create(source, store, encryptor, password = null)
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.contains("cover image"))
        assertNull(source.findFile(Constants.File.FLUX)!!.findFile(Constants.File.FLUX_BACKUP)
            ?.listFiles()?.singleOrNull()?.findFile("backup.json"))
    }

    @Test
    fun restoreRejectsUnlistedArchiveMediaAndDoesNotReplaceDataStore() = runBlocking {
        val source = sourceRoot()
        source.addMedia(Constants.File.FLUX_IMAGES, "photo.jpg", "image/jpeg", byteArrayOf(3, 2, 1))
        FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, password = null)
        val backup = source.backupFolder()
        backup.findFile("media")!!.findFile("images")!!
            .createFile("image/jpeg", "unexpected.jpg")!!
            .writeBytes(byteArrayOf(9))
        val store = FakeBackupDataStore()

        val failure = runCatching {
            FullBackupArchive.restore(
                backup,
                MemoryDocument.directory("restore-root"),
                store,
                encryptor,
                password = null
            )
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.contains("unlisted"))
        assertNull(store.restoredBackup)
    }

    @Test
    fun restoreRejectsDuplicateMediaManifestEntries() = runBlocking {
        val source = sourceRoot()
        source.addMedia(Constants.File.FLUX_IMAGES, "photo.jpg", "image/jpeg", byteArrayOf(3, 2, 1))
        FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, password = null)
        val backup = source.backupFolder()
        val manifest = readManifest(backup)
        val entry = manifest.mediaFiles.single()
        val tamperedManifest = manifest.copy(mediaFiles = listOf(entry, entry))
        backup.findFile("backup.json")!!.overwrite(
            testJson.encodeToString(FluxBackup.serializer(), tamperedManifest).toByteArray()
        )
        val store = FakeBackupDataStore()

        val failure = runCatching {
            FullBackupArchive.restore(
                backup,
                MemoryDocument.directory("restore-root"),
                store,
                encryptor,
                password = null
            )
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.contains("duplicate media"))
        assertNull(store.restoredBackup)
    }

    @Test
    fun createRejectsUnsafeSourceMediaNames() = runBlocking {
        val source = sourceRoot()
        source.findFile(Constants.File.FLUX)!!.findFile(Constants.File.FLUX_IMAGES)!!
            .createFile("image/jpeg", "../outside.jpg")!!
            .writeBytes(byteArrayOf(1))

        val failure = runCatching {
            FullBackupArchive.create(source, FakeBackupDataStore(), encryptor, password = null)
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertNull(source.findFile(Constants.File.FLUX)!!.findFile(Constants.File.FLUX_BACKUP)
            ?.listFiles()?.singleOrNull()?.findFile("backup.json"))
    }

    private fun sourceRoot(): MemoryDocument {
        val root = MemoryDocument.directory("root")
        val flux = root.ensureDirectory(Constants.File.FLUX)
        flux.ensureDirectory(Constants.File.FLUX_IMAGES)
        flux.ensureDirectory(Constants.File.FLUX_AUDIO)
        flux.ensureDirectory(Constants.File.FLUX_VIDEOS)
        flux.ensureDirectory(Constants.File.FLUX_DOCUMENTS)
        return root
    }

    private fun readManifest(backup: MemoryDocument): FluxBackup =
        testJson.decodeFromString(backup.findFile("backup.json")!!.readBytes().toString(Charsets.UTF_8))

    private fun MemoryDocument.backupFolder(): MemoryDocument =
        findFile(Constants.File.FLUX)!!
            .findFile(Constants.File.FLUX_BACKUP)!!
            .listFiles()
            .single { it.name!!.startsWith("FullBackup-") } as MemoryDocument

    private fun MemoryDocument.media(category: String, name: String): MemoryDocument =
        (findFile("media") ?: findFile(Constants.File.FLUX))!!
            .findFile(category)!!
            .findFile(name) as MemoryDocument

    private fun MemoryDocument.mediaOrNull(category: String, name: String): MemoryDocument? =
        findFile(Constants.File.FLUX)?.findFile(category)?.findFile(name) as? MemoryDocument

    private fun MemoryDocument.addMedia(category: String, name: String, mimeType: String, bytes: ByteArray) {
        findFile(Constants.File.FLUX)!!.findFile(category)!!.createFile(mimeType, name)!!.writeBytes(bytes)
    }

    private fun MemoryDocument.readAllBytes(): ByteArray =
        allFiles().flatMap { it.readBytes().toList() }.toByteArray()

    private fun MemoryDocument.allFiles(): List<MemoryDocument> =
        listFiles().flatMap { child ->
            val memoryChild = child as MemoryDocument
            if (memoryChild.isDirectory) memoryChild.allFiles() else listOf(memoryChild)
        }

    private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > size) return false
        return (0..size - needle.size).any { offset ->
            needle.indices.all { this[offset + it] == needle[it] }
        }
    }
}

private class FakeBackupDataStore(
    private val backup: FluxBackup = FluxBackup()
) : BackupDataStore {
    var restoredBackup: FluxBackup? = null
        private set

    override suspend fun loadBackup(): FluxBackup = backup

    override suspend fun replaceWith(backup: FluxBackup) {
        restoredBackup = backup
    }
}

private class MemoryDocument private constructor(
    override val name: String?,
    override val isDirectory: Boolean,
    override val type: String?,
    private val parent: MemoryDocument?,
    private val path: String
) : BackupDocument {
    private val children = linkedMapOf<String, MemoryDocument>()
    private var contents = byteArrayOf()

    override val isFile: Boolean
        get() = !isDirectory
    override val uri: String
        get() = "memory:///$path"

    override fun findFile(name: String): BackupDocument? = synchronized(children) { children[name] }

    override fun listFiles(): List<BackupDocument> = synchronized(children) { children.values.toList() }

    override fun createDirectory(name: String): BackupDocument? = synchronized(children) {
        if (children.containsKey(name)) return null
        directoryChild(name)
    }

    override fun createFile(mimeType: String, name: String): BackupDocument? = synchronized(children) {
        if (children.containsKey(name)) return null
        val file = MemoryDocument(name, false, mimeType, this, "$path/$name")
        children[name] = file
        file
    }

    override fun openInputStream(): InputStream? =
        if (isDirectory) null else ByteArrayInputStream(contents.copyOf())

    override fun openOutputStream(): OutputStream? {
        if (isDirectory) return null
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                contents = toByteArray()
            }
        }
    }

    override fun delete(): Boolean = synchronized(parent?.children ?: children) {
        if (parent == null) return false
        parent.children.remove(name)
        true
    }

    fun ensureDirectory(name: String): MemoryDocument {
        findFile(name)?.let { existing ->
            require(existing.isDirectory)
            return existing as MemoryDocument
        }
        return directoryChild(name)
    }

    fun readBytes(): ByteArray = contents.copyOf()

    private fun directoryChild(name: String): MemoryDocument =
        MemoryDocument(name, true, null, this, "$path/$name").also { children[name] = it }

    companion object {
        fun directory(name: String): MemoryDocument = MemoryDocument(name, true, null, null, name)
    }
}
