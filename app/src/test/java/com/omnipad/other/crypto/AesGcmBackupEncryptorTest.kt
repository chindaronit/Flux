package com.omnipad.other.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class AesGcmBackupEncryptorTest {
    private val encryptor = AesGcmBackupEncryptor()

    @Test
    fun roundTripsEmptySingleAndMultiChunkPayloadsAcrossFilesInOneSession() = runBlocking {
        val password = "test password".toCharArray()
        val session = encryptor.createSession(password)
        val payloads = listOf(
            byteArrayOf(),
            byteArrayOf(0),
            ByteArray(64 * 1024 - 1) { (it * 7).toByte() },
            ByteArray(64 * 1024) { (it * 11).toByte() },
            ByteArray(64 * 1024 + 1) { (it * 13).toByte() },
            ByteArray(250_000) { (it * 17).toByte() }
        )
        val encrypted = payloads.map { payload ->
            ByteArrayOutputStream().also { out ->
                encryptor.encrypt(ByteArrayInputStream(payload), out, session)
            }.toByteArray()
        }

        assertEquals(encrypted.size, encrypted.map(::fileId).toSet().size)
        assertTrue(encrypted.all { it.copyOfRange(0, BackupFileFormat.MAGIC.size)
            .contentEquals(BackupFileFormat.MAGIC) })
        assertTrue(encrypted.all { it[BackupFileFormat.MAGIC.size] == BackupFileFormat.VERSION })

        val manifestSession = encryptor.decryptManifest(encrypted.first(), password).session!!
        payloads.drop(1).zip(encrypted.drop(1)).forEach { (expected, encryptedFile) ->
            val actual = ByteArrayOutputStream()
            encryptor.decrypt(ByteArrayInputStream(encryptedFile), actual, manifestSession)
            assertArrayEquals(expected, actual.toByteArray())
        }
    }

    @Test
    fun independentSessionsUseDifferentRandomSaltsAndKeys() = runBlocking {
        val password = "same password".toCharArray()
        val payload = "same plaintext".toByteArray()
        val first = encryptor.createSession(password)
        val second = encryptor.createSession(password)
        val encryptedFirst = encrypt(payload, first)
        val encryptedSecond = encrypt(payload, second)

        assertFalse(
            encryptedFirst.copyOfRange(BackupFileFormat.MAGIC.size + 1, BackupFileFormat.MAGIC.size + 17)
                .contentEquals(
                    encryptedSecond.copyOfRange(
                        BackupFileFormat.MAGIC.size + 1,
                        BackupFileFormat.MAGIC.size + 17
                    )
                )
        )
        assertFalse(encryptedFirst.contentEquals(encryptedSecond))
    }

    @Test
    fun manifestRejectsWrongPasswordAndTamperedCiphertext() = runBlocking {
        val password = "correct".toCharArray()
        val encrypted = encrypt("authenticated manifest".toByteArray(), encryptor.createSession(password))

        val wrongPasswordFailure = runCatching {
            encryptor.decryptManifest(encrypted, "wrong".toCharArray())
        }.exceptionOrNull()
        assertTrue(wrongPasswordFailure is BackupCryptoException.WrongPasswordOrCorrupted)

        val tampered = encrypted.copyOf().also { it[it.lastIndex - 1] = (it[it.lastIndex - 1].toInt() xor 1).toByte() }
        val tamperFailure = runCatching {
            encryptor.decryptManifest(tampered, password)
        }.exceptionOrNull()
        assertTrue(tamperFailure is BackupCryptoException.WrongPasswordOrCorrupted)
    }

    @Test
    fun mediaDecryptionRejectsTamperingTruncationAndTrailingBytes() = runBlocking {
        val password = "password".toCharArray()
        val session = encryptor.createSession(password)
        val encrypted = encrypt(ByteArray(140_000) { it.toByte() }, session)
        val decryptSession = encryptor.decryptManifest(
            encrypt("manifest".toByteArray(), session),
            password
        ).session!!

        listOf(
            encrypted.copyOf().also { it[it.lastIndex - 4] = (it[it.lastIndex - 4].toInt() xor 0x20).toByte() },
            encrypted.copyOf(encrypted.size - 1),
            encrypted + byteArrayOf(1)
        ).forEach { invalid ->
            val failure = runCatching {
                encryptor.decrypt(ByteArrayInputStream(invalid), ByteArrayOutputStream(), decryptSession)
            }.exceptionOrNull()
            assertTrue(failure is BackupCryptoException.WrongPasswordOrCorrupted)
        }
    }

    @Test
    fun rejectsUnsupportedEncryptedFormatVersion() = runBlocking {
        val encrypted = encrypt("payload".toByteArray(), encryptor.createSession("password".toCharArray()))
        val invalidVersion = encrypted.copyOf().also {
            it[BackupFileFormat.MAGIC.size] = (BackupFileFormat.VERSION + 1).toByte()
        }

        val failure = runCatching {
            encryptor.decryptManifest(invalidVersion, "password".toCharArray())
        }.exceptionOrNull()

        assertTrue(failure is BackupCryptoException.UnsupportedVersion)
    }

    private suspend fun encrypt(payload: ByteArray, session: BackupEncryptionSession): ByteArray =
        ByteArrayOutputStream().also { output ->
            encryptor.encrypt(ByteArrayInputStream(payload), output, session)
        }.toByteArray()

    private fun fileId(encrypted: ByteArray): Int =
        ByteBuffer.wrap(encrypted, BackupFileFormat.MAGIC.size + 1 + 16, Int.SIZE_BYTES).int
}
