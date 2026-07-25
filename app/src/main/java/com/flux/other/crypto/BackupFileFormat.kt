package com.flux.other.crypto

/**
 * Self-describing header so encrypted and legacy plaintext backups can coexist
 * in the same folder, and imports can tell them apart without relying on file extension.
 */
object BackupFileFormat {
    val MAGIC: ByteArray = "FLUXBK1".toByteArray(Charsets.US_ASCII) // 7 bytes
    const val VERSION: Byte = 1

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
}