package com.example.data.local

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Where decrypted attachments live on this phone.
 *
 * Files are written to the app's private `filesDir`, which is covered by
 * Android's file-based encryption and excluded from cloud/ADB backup
 * (`android:allowBackup="false"`). Nothing here is ever uploaded to our server
 * in readable form: the relay only ever holds the AES-GCM blob produced by
 * [ChatCrypto.encryptBytes].
 */
object ChatAttachmentStore {

    private const val TAG = "ChatAttachmentStore"
    private const val DIR_NAME = "chat_attachments"
    private const val MAX_CACHE_BYTES = 120L * 1024L * 1024L  // 120 MB

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Absolute path for one attachment, or null when [attachmentId] is not the
     * random hex id the server issues (defensive: never build a path from an
     * untrusted string without checking it first).
     */
    fun pathFor(context: Context, attachmentId: String): String? {
        if (!attachmentId.matches(Regex("^[A-Fa-f0-9]{8,64}$"))) return null
        return File(dir(context), "$attachmentId.bin").absolutePath
    }

    fun exists(context: Context, attachmentId: String): Boolean {
        val path = pathFor(context, attachmentId) ?: return false
        return File(path).let { it.exists() && it.length() > 0L }
    }

    fun save(context: Context, attachmentId: String, bytes: ByteArray): String? {
        val path = pathFor(context, attachmentId) ?: return null
        return try {
            val file = File(path)
            val tmp = File(path + ".part")
            tmp.writeBytes(bytes)
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                file.writeBytes(bytes)
                tmp.delete()
            }
            path
        } catch (e: Exception) {
            Log.e(TAG, "Could not store attachment $attachmentId: ${e.message}")
            null
        }
    }

    fun delete(context: Context, attachmentId: String) {
        val path = pathFor(context, attachmentId) ?: return
        runCatching { File(path).delete() }
    }

    fun clearAll(context: Context) {
        runCatching { dir(context).listFiles()?.forEach { it.delete() } }
    }

    /**
     * Keep the cache bounded: the oldest files are dropped once the folder grows
     * past [MAX_CACHE_BYTES]. Messages stay in the database, so a dropped file
     * simply shows the "download again" placeholder.
     */
    fun trim(context: Context) {
        try {
            val files = dir(context).listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() }
                ?: return
            var total = files.sumOf { it.length() }
            if (total <= MAX_CACHE_BYTES) return
            for (file in files) {
                if (total <= MAX_CACHE_BYTES) break
                val size = file.length()
                if (file.delete()) total -= size
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attachment cache trim failed: ${e.message}")
        }
    }
}
