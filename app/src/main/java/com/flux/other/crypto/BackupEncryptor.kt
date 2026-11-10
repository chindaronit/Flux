package com.flux.other.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

interface BackupEncryptor {
    suspend fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray
    suspend fun decrypt(encryptedFile: ByteArray, password: CharArray): ByteArray
    suspend fun encrypt(input: InputStream, output: OutputStream, password: CharArray)
    suspend fun decrypt(input: InputStream, output: OutputStream, password: CharArray)
}

/**
 * AES-256-GCM with a PBKDF2-derived key. Salt + IV are random per file and stored
 * in its header, so encrypted backups are portable across devices.
 */
class AesGcmBackupEncryptor : BackupEncryptor {

    private companion object {
        const val SALT_SIZE = 16
        const val IV_SIZE = 12
        const val GCM_TAG_BITS = 128
        const val PBKDF2_ITERATIONS = 210_000
        const val KEY_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val BUFFER_SIZE = 16 * 1024
    }

    override suspend fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray =
        ByteArrayOutputStream().also { output ->
            encrypt(ByteArrayInputStream(plaintext), output, password)
        }.toByteArray()

    override suspend fun decrypt(encryptedFile: ByteArray, password: CharArray): ByteArray =
        ByteArrayOutputStream().also { output ->
            decrypt(ByteArrayInputStream(encryptedFile), output, password)
        }.toByteArray()

    override suspend fun encrypt(input: InputStream, output: OutputStream, password: CharArray) =
        withContext(Dispatchers.IO) {
            val salt = randomBytes(SALT_SIZE)
            val iv = randomBytes(IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            output.write(BackupFileFormat.MAGIC)
            output.write(BackupFileFormat.VERSION.toInt())
            output.write(salt)
            output.write(iv)
            transform(input, output, cipher)
        }

    override suspend fun decrypt(input: InputStream, output: OutputStream, password: CharArray) =
        withContext(Dispatchers.IO) {
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
            val iv = readExactly(input, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            try {
                transform(input, output, cipher)
            } catch (exception: AEADBadTagException) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
            } catch (exception: javax.crypto.BadPaddingException) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(exception)
            }
        }

    private fun transform(input: InputStream, output: OutputStream, cipher: Cipher) {
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            cipher.update(buffer, 0, count)?.let(output::write)
        }
        output.write(cipher.doFinal())
        output.flush()
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

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
}
