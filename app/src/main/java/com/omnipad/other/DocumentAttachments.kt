package com.omnipad.other

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.util.UUID

data class ImportedDocumentAttachment(
    val fileName: String,
    val isPdf: Boolean
) {
    val markdown: String
        get() {
            val encodedFileName = Uri.encode(fileName)
            return if (isPdf) {
                "![PDF: $fileName](https://$ATTACHMENT_HOST/pdf/$encodedFileName)"
            } else {
                "[$fileName](https://$ATTACHMENT_HOST/document/$encodedFileName)"
            }
        }
}

@Throws(IOException::class)
fun copyDocumentAttachment(
    context: Context,
    storageRootUri: Uri,
    sourceUri: Uri
): ImportedDocumentAttachment {
    val root = DocumentFile.fromTreeUri(context, storageRootUri)
        ?: throw IOException("The storage root is unavailable")
    val omnipadDirectory = findOrCreateDirectory(root, Constants.File.OMNIPAD)
    val documentsDirectory = findOrCreateDirectory(omnipadDirectory, Constants.File.OMNIPAD_DOCUMENTS)
    val resolver = context.contentResolver
    val sourceName = resolver.query(sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) cursor.getString(nameColumn) else null
        }
        ?.takeIf { it.isNotBlank() }
        ?: sourceUri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "document"
    val extension = sourceName.substringAfterLast('.', "")
        .takeIf { it.isNotBlank() && sourceName != ".$it" }
        ?.replace(Regex("[^A-Za-z0-9]"), "")
        .orEmpty()
    val baseName = sourceName.substringBeforeLast('.', sourceName)
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('.', '_')
        .ifBlank { "document" }
    val fileName = buildString {
        append(baseName)
        append('-')
        append(UUID.randomUUID().toString().take(8))
        if (extension.isNotBlank()) append(".$extension")
    }
    val mimeType = resolver.getType(sourceUri)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        ?: "application/octet-stream"
    val destination = documentsDirectory.createFile(mimeType, fileName)
        ?: throw IOException("Could not create attachment $fileName")

    try {
        resolver.openInputStream(sourceUri)?.use { source ->
            val output = resolver.openOutputStream(destination.uri)
                ?: throw IOException("Could not write attachment $fileName")
            output.use { target -> source.copyTo(target) }
        } ?: throw IOException("Could not read the selected document")
    } catch (exception: Exception) {
        destination.delete()
        if (exception is IOException) throw exception
        throw IOException("Could not copy attachment $fileName: ${exception.localizedMessage}", exception)
    }

    val actualFileName = destination.name ?: fileName
    val isPdf = mimeType.equals("application/pdf", ignoreCase = true) ||
        actualFileName.endsWith(".pdf", ignoreCase = true)
    return ImportedDocumentAttachment(actualFileName, isPdf)
}

private fun findOrCreateDirectory(parent: DocumentFile, name: String): DocumentFile {
    parent.findFile(name)?.let { existing ->
        if (existing.isDirectory) return existing
        throw IOException("$name exists but is not a folder")
    }
    return parent.createDirectory(name) ?: throw IOException("Could not create folder $name")
}
