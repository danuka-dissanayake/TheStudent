package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Reads a photo the student picked and turns it into a small, safe JPEG.
 *
 * Why this exists
 * ---------------
 * 1. A phone camera photo is 4–12 MB. Uploading that over a Sri Lankan mobile
 *    connection fails constantly, and the server caps avatars at 5 MB.
 * 2. We only trust pixels: the file is decoded with [BitmapFactory], so a file
 *    that is really a script/HTML renamed to `.jpg` simply fails to decode.
 * 3. Re-encoding drops EXIF (including GPS location) — a real privacy win that
 *    students never notice.
 *
 * The result is always JPEG (alpha flattened onto white) with the longest edge
 * at most [MAX_EDGE] px, normally 40–200 KB.
 */
object AvatarPreparer {

    private const val TAG = "AvatarPreparer"

    /** Hard client-side ceiling; the server independently enforces 5 MB. */
    const val MAX_BYTES = 5 * 1024 * 1024L

    /** Longest edge after downscaling. */
    const val MAX_EDGE = 1024

    /** Smallest acceptable source image. */
    const val MIN_EDGE = 64

    /** MIME types the system picker may hand us (HEIC is decoded natively). */
    private val ACCEPTED_MIME = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif"
    )

    sealed class Result {
        data class Success(val jpegBytes: ByteArray, val width: Int, val height: Int) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun prepare(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val mime = runCatching { resolver.getType(uri) }.getOrNull()?.lowercase()
        if (mime != null && mime !in ACCEPTED_MIME) {
            return@withContext Result.Failure("Please choose a JPG or PNG photo.")
        }

        // ---- 1. bounds only (cheap, no pixels decoded) --------------------
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }.onFailure { Log.w(TAG, "bounds read failed: ${it.message}") }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@withContext Result.Failure(
                "That file is not a valid image. Please choose a JPG or PNG photo."
            )
        }
        if (bounds.outWidth < MIN_EDGE || bounds.outHeight < MIN_EDGE) {
            return@withContext Result.Failure(
                "That image is too small (minimum $MIN_EDGE×$MIN_EDGE pixels)."
            )
        }

        // ---- 2. decode at a reduced sample size ---------------------------
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE * 2 && bounds.outHeight / sample > MAX_EDGE * 2) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }

        val decoded = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }.getOrNull()

        if (decoded == null) {
            return@withContext Result.Failure(
                "That image could not be read. Please try another photo."
            )
        }

        // ---- 3. scale to the target edge ---------------------------------
        val longest = maxOf(decoded.width, decoded.height)
        val scale = if (longest > MAX_EDGE) MAX_EDGE.toFloat() / longest else 1f
        val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
        val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)

        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        // ---- 4. flatten transparency onto white, then compress ------------
        val flattened = if (scaled.hasAlpha()) {
            Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888).also { canvas ->
                val c = android.graphics.Canvas(canvas)
                c.drawColor(android.graphics.Color.WHITE)
                c.drawBitmap(scaled, 0f, 0f, null)
            }
        } else {
            scaled
        }

        var quality = 88
        var bytes: ByteArray = ByteArray(0)
        while (quality >= 45) {
            val out = ByteArrayOutputStream()
            flattened.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            if (bytes.size <= MAX_BYTES.toInt()) break
            quality -= 12
        }

        val width = flattened.width
        val height = flattened.height
        if (flattened !== scaled) flattened.recycle()
        scaled.recycle()

        if (bytes.isEmpty()) {
            return@withContext Result.Failure("That image could not be processed. Please try another photo.")
        }

        Log.d(TAG, "prepared avatar ${width}x$height, ${bytes.size / 1024} KB")
        Result.Success(bytes, width, height)
    }
}
