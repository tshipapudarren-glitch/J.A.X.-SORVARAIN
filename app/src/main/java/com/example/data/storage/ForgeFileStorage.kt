package com.example.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class SavedArtifactInfo(
    val file: File,
    val fileName: String,
    val byteSize: Int,
    val sha256Short: String
)

class ForgeFileStorage(private val context: Context) {

    private fun projectDir(projectId: String, version: String): File {
        val safeProj = projectId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeVer = version.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(File(context.filesDir, "indicator_forge"), "$safeProj/$safeVer").apply { mkdirs() }
    }

    fun writeTextArtifact(
        projectId: String,
        version: String,
        fileName: String,
        content: String
    ): SavedArtifactInfo {
        val bytes = content.toByteArray(Charsets.UTF_8)
        return writeBinaryArtifact(projectId, version, fileName, bytes)
    }

    fun writeBinaryArtifact(
        projectId: String,
        version: String,
        fileName: String,
        bytes: ByteArray
    ): SavedArtifactInfo {
        val dir = projectDir(projectId, version)
        val outFile = File(dir, fileName)
        outFile.writeBytes(bytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.take(6).joinToString("") { "%02x".format(it) }
        return SavedArtifactInfo(
            file = outFile,
            fileName = fileName,
            byteSize = bytes.size,
            sha256Short = hex
        )
    }

    fun buildZipBundle(
        projectId: String,
        version: String,
        bundleName: String,
        entries: Map<String, ByteArray>
    ): SavedArtifactInfo {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, payload) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(payload)
                zos.closeEntry()
            }
        }
        return writeBinaryArtifact(projectId, version, bundleName, baos.toByteArray())
    }

    fun exportToUserUri(sourceFile: File, targetUri: Uri): Int {
        val bytes = sourceFile.readBytes()
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: throw IllegalStateException("Unable to open destination file stream.")
        return bytes.size
    }

    fun createShareIntent(file: File, mimeType: String = "text/plain"): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "J.A.X. Indicator Forge: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
