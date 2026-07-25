package com.flux.other

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Log

/**
 * Handles one-way password hashing for workspace passkeys.
 * Stored format: "<iterations>$<base64Salt>$<base64Hash>"
 */
object PasswordHasher {

    private const val TAG = "PasswordHasher"
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val DELIMITER = "$"

    /** Hashes [rawPassword] with a freshly generated salt. */
    fun hash(rawPassword: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).apply { SecureRandom().nextBytes(this) }
        val hashBytes = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)

        val encodedSalt = Base64.encodeToString(salt, Base64.NO_WRAP)
        val encodedHash = Base64.encodeToString(hashBytes, Base64.NO_WRAP)

        return "$ITERATIONS$DELIMITER$encodedSalt$DELIMITER$encodedHash"
    }

    /** Verifies [rawPassword] against a previously [hash]ed value. */
    fun verify(rawPassword: String, stored: String): Boolean {
        return try {
            val (iterations, salt, expectedHash) = parse(stored) ?: return false
            val actualHash = pbkdf2(rawPassword.toCharArray(), salt, iterations, expectedHash.size * 8)
            actualHash.contentEquals(expectedHash)
        } catch (e: Exception) {
            Log.e(TAG, "verify: unable to verify password", e)
            false
        }
    }

    /** True if [value] is already in our hashed format (used to make migration idempotent). */
    fun isHashed(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return parse(value) != null
    }

    private fun parse(stored: String): Triple<Int, ByteArray, ByteArray>? {
        val parts = stored.split(DELIMITER)
        if (parts.size != 3) return null
        val iterations = parts[0].toIntOrNull() ?: return null
        return try {
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val hash = Base64.decode(parts[2], Base64.NO_WRAP)
            Triple(iterations, salt, hash)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}