package com.omnipad.other.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

interface BackupEncryptionSession

data class DecryptedBackupManifest(
    val plaintext: ByteArray,
    val session: BackupEncryptionSession?
)

interface BackupEncryptor {
    suspend fun createSession(password: CharArray): BackupEncryptionSession
    suspend fun decryptManifest(encryptedFile: ByteArray, password: CharArray): DecryptedBackupManifest
    suspend fun encrypt(input: InputStream, output: OutputStream, session: BackupEncryptionSession)
    suspend fun decrypt(input: InputStream, output: OutputStream, session: BackupEncryptionSession)
}

/**
 * AES-256-GCM with a PBKDF2-derived, per-backup key. Each file and chunk has a
 * unique counter-based 96-bit nonce; authenticated chunks keep memory use bounded.
 */
class AesGcmBackupEncryptor : BackupEncryptor {

    private companion object {
        const val SALT_SIZE = 16
        const val NONCE_SIZE = 12
        const val GCM_TAG_BITS = 128
        const val TAG_SIZE = GCM_TAG_BITS / 8
        const val PBKDF2_ITERATIONS = 210_000
        const val KEY_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_FILE_ID = Int.MAX_VALUE
        const val MAX_GCM_INVOCATIONS = 1L shl 32
        const val FINAL_CHUNK_LENGTH = -1
    }

    override suspend fun createSession(password: CharArray): BackupEncryptionSession =
        withContext(Dispatchers.IO) {
            val salt = randomBytes(SALT_SIZE)
            EncryptionSession(deriveKey(password, salt), salt)
        }

    override suspend fun decryptManifest(
        encryptedFile: ByteArray,
        password: CharArray
    ): DecryptedBackupManifest = withContext(Dispatchers.IO) {
        val input = ByteArrayInputStream(encryptedFile)
        val header = readHeader(input)
        val key = deriveKey(password, header.salt)
        val session = EncryptionSession(key, header.salt)
        session.registerFile(header.fileId)
        val output = ByteArrayOutputStream()
        decryptChunks(input, output, session, header)
        DecryptedBackupManifest(output.toByteArray(), session)
    }

    override suspend fun encrypt(
        input: InputStream,
        output: OutputStream,
        session: BackupEncryptionSession
    ) = withContext(Dispatchers.IO) {
        val backupSession = requireSession(session)
        val fileId = backupSession.allocateFile()
        val header = createHeader(backupSession.salt, fileId)
        output.write(header.bytes)

        val buffer = ByteArray(BUFFER_SIZE)
        var sequence = 0L
        while (true) {
            var count = 0
            while (count < buffer.size) {
                val read = input.read(buffer, count, buffer.size - count)
                if (read < 0) break
                if (read == 0) {
                    val singleByte = input.read()
                    if (singleByte < 0) break
                    buffer[count++] = singleByte.toByte()
                } else {
                    count += read
                }
            }
            if (count == 0) break
            writeChunk(output, backupSession, header.bytes, fileId, sequence, buffer, count)
            sequence++
            if (count < buffer.size) break
        }

        backupSession.consumeInvocation()
        writeInt(output, FINAL_CHUNK_LENGTH)
        output.write(
            createChunkCipher(
                Cipher.ENCRYPT_MODE,
                backupSession.key,
                fileId,
                sequence,
                FINAL_CHUNK_LENGTH,
                header.bytes
            ).doFinal()
        )
        output.flush()
    }

    override suspend fun decrypt(
        input: InputStream,
        output: OutputStream,
        session: BackupEncryptionSession
    ) = withContext(Dispatchers.IO) {
        val backupSession = requireSession(session)
        val header = readHeader(input)
        if (!header.salt.contentEquals(backupSession.salt)) {
            throw BackupCryptoException.WrongPasswordOrCorrupted(
                IOException("Encrypted file belongs to a different backup")
            )
        }
        backupSession.registerFile(header.fileId)
        decryptChunks(input, output, backupSession, header)
    }

    private fun writeChunk(
        output: OutputStream,
        session: EncryptionSession,
        header: ByteArray,
        fileId: Int,
        sequence: Long,
        buffer: ByteArray,
        count: Int
    ) {
        session.consumeInvocation()
        writeInt(output, count)
        val encryptedChunk = createChunkCipher(
            Cipher.ENCRYPT_MODE,
            session.key,
            fileId,
            sequence,
            count,
            header
        ).doFinal(buffer, 0, count)
        output.write(encryptedChunk)
    }

    private fun decryptChunks(
        input: InputStream,
        output: OutputStream,
        session: EncryptionSession,
        header: Header
    ) {
        var sequence = 0L
        while (true) {
            val length = try {
                readInt(input)
            } catch (exception: IOException) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
            }
            if (length == FINAL_CHUNK_LENGTH) {
                val finalTag = readExactly(input, TAG_SIZE)
                try {
                    session.consumeInvocation()
                    createChunkCipher(
                        Cipher.DECRYPT_MODE,
                        session.key,
                        header.fileId,
                        sequence,
                        length,
                        header.bytes
                    ).doFinal(finalTag)
                } catch (exception: AEADBadTagException) {
                    throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
                } catch (exception: javax.crypto.BadPaddingException) {
                    throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
                }
                if (input.read() != -1) {
                    throw BackupCryptoException.WrongPasswordOrCorrupted(
                        IOException("Unexpected data after encrypted backup")
                    )
                }
                output.flush()
                return
            }
            if (length !in 1..BUFFER_SIZE || sequence == Long.MAX_VALUE) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(
                    IOException("Invalid encrypted backup chunk")
                )
            }
            val encryptedChunk = readExactly(input, length + TAG_SIZE)
            val plaintext = try {
                session.consumeInvocation()
                createChunkCipher(
                    Cipher.DECRYPT_MODE,
                    session.key,
                    header.fileId,
                    sequence,
                    length,
                    header.bytes
                ).doFinal(encryptedChunk)
            } catch (exception: AEADBadTagException) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
            } catch (exception: javax.crypto.BadPaddingException) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
            }
            output.write(plaintext)
            sequence++
        }
    }

    private fun createChunkCipher(
        mode: Int,
        key: SecretKey,
        fileId: Int,
        sequence: Long,
        length: Int,
        header: ByteArray
    ): Cipher {
        val nonce = ByteBuffer.allocate(NONCE_SIZE)
            .putInt(fileId)
            .putLong(sequence)
            .array()
        val associatedData = ByteBuffer.allocate(header.size + Long.SIZE_BYTES + Int.SIZE_BYTES)
            .put(header)
            .putLong(sequence)
            .putInt(length)
            .array()
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            updateAAD(associatedData)
        }
    }

    private fun createHeader(salt: ByteArray, fileId: Int): Header {
        val bytes = ByteBuffer.allocate(
            BackupFileFormat.MAGIC.size + 1 + SALT_SIZE + Int.SIZE_BYTES
        )
            .put(BackupFileFormat.MAGIC)
            .put(BackupFileFormat.VERSION)
            .put(salt)
            .putInt(fileId)
            .array()
        return Header(bytes, salt, fileId)
    }

    private fun readHeader(input: InputStream): Header {
        val magic = readExactly(input, BackupFileFormat.MAGIC.size)
        if (!magic.contentEquals(BackupFileFormat.MAGIC)) {
            throw BackupCryptoException.WrongPasswordOrCorrupted(
                IOException("Invalid encrypted backup header")
            )
        }
        val version = readExactly(input, 1).first()
        if (version != BackupFileFormat.VERSION) {
            throw BackupCryptoException.UnsupportedVersion(version.toInt())
        }
        val salt = readExactly(input, SALT_SIZE)
        val fileId = readInt(input)
        if (fileId < 0) {
            throw BackupCryptoException.WrongPasswordOrCorrupted(
                IOException("Invalid encrypted backup file identifier")
            )
        }
        return createHeader(salt, fileId)
    }

    private fun readExactly(input: InputStream, size: Int): ByteArray {
        val result = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val count = input.read(result, offset, size - offset)
            if (count < 0) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(
                    IOException("Truncated encrypted backup")
                )
            }
            offset += count
        }
        return result
    }

    private fun readInt(input: InputStream): Int =
        ByteBuffer.wrap(readExactly(input, Int.SIZE_BYTES)).int

    private fun writeInt(output: OutputStream, value: Int) {
        output.write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array())
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
            try {
                SecretKeySpec(raw, "AES")
            } finally {
                raw.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

    private fun requireSession(session: BackupEncryptionSession): EncryptionSession =
        session as? EncryptionSession ?: throw IllegalArgumentException("Invalid backup encryption session")

    private data class Header(val bytes: ByteArray, val salt: ByteArray, val fileId: Int)

    private class EncryptionSession(
        val key: SecretKey,
        val salt: ByteArray
    ) : BackupEncryptionSession {
        private var nextFileId = 0L
        private var invocationCount = 0L
        private val usedFileIds = mutableSetOf<Int>()

        @Synchronized
        fun allocateFile(): Int {
            if (nextFileId > MAX_FILE_ID) throw IOException("Backup contains too many encrypted files")
            return nextFileId++.toInt()
        }

        @Synchronized
        fun registerFile(fileId: Int) {
            if (fileId < 0 || !usedFileIds.add(fileId)) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(
                    IOException("Duplicate or invalid encrypted backup file identifier")
                )
            }
        }

        @Synchronized
        fun consumeInvocation() {
            if (invocationCount >= MAX_GCM_INVOCATIONS) {
                throw IOException("Encrypted backup exceeds the AES-GCM usage limit")
            }
            invocationCount++
        }
    }
}
