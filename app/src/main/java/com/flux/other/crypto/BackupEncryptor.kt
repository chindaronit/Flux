package com.flux.other.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
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
}

/**
 * AES-256-GCM with a PBKDF2-derived key. Salt + IV are random per export and stored
 * in the file header, so the file is fully self-contained and portable across devices —
 * only the password (known to the user) is needed to open it anywhere.
 */
class AesGcmBackupEncryptor : BackupEncryptor {

    private companion object {
        const val SALT_SIZE = 16
        const val IV_SIZE = 12
        const val GCM_TAG_BITS = 128
        const val PBKDF2_ITERATIONS = 210_000 // OWASP-recommended floor for PBKDF2-HMAC-SHA256
        const val KEY_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    override suspend fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray =
        withContext(Dispatchers.Default) {
            val salt = randomBytes(SALT_SIZE)
            val iv = randomBytes(IV_SIZE)
            val key = deriveKey(password, salt)

            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            val ciphertext = cipher.doFinal(plaintext)

            BackupFileFormat.MAGIC + byteArrayOf(BackupFileFormat.VERSION) + salt + iv + ciphertext
        }

    override suspend fun decrypt(encryptedFile: ByteArray, password: CharArray): ByteArray =
        withContext(Dispatchers.Default) {
            val buffer = ByteBuffer.wrap(encryptedFile).apply { position(BackupFileFormat.MAGIC.size) }

            val version = buffer.get()
            if (version != BackupFileFormat.VERSION) {
                throw BackupCryptoException.UnsupportedVersion(version.toInt())
            }

            val salt = ByteArray(SALT_SIZE).also { buffer.get(it) }
            val iv = ByteArray(IV_SIZE).also { buffer.get(it) }
            val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }

            try {
                cipher.doFinal(ciphertext)
            } catch (e: AEADBadTagException) {
                throw BackupCryptoException.WrongPasswordOrCorrupted(e)
            }
        }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
}