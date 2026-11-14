package com.omnipad.other.crypto

/**
 * Self-describing header so encrypted and legacy plaintext backups can coexist
 * in the same folder, and imports can tell them apart without relying on file extension.
 */
object BackupFileFormat {
    val MAGIC: ByteArray = "OMNIPADBK1".toByteArray(Charsets.US_ASCII) // 7 bytes
    const val VERSION: Byte = 3

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
}