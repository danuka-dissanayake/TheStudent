package com.example.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Shrinks a picked photo before it is encrypted and uploaded.
 *
 * A modern phone camera produces 4–12 MB JPEGs, which is slow to send over
 * mobile data and pointless on a chat bubble. Anything above 1600 px on its
 * longer edge is downscaled and re-encoded at quality 85, which is visually
 * identical on a phone screen and typically 5–10x smaller.
 *
 * Documents are never touched, and a GIF is left alone so it keeps animating.
 */
object ChatMediaPrep {

    private const val TAG = "ChatMediaPrep"
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    /** Mime types this helper is willing to re-encode. */
    fun isCompressible(mime: String, name: String): Boolean {
        val lowerName = name.lowercase()
        if (lowerName.endsWith(".gif")) return false
        return mime == "image/jpeg" || mime == "image/jpg" || mime == "image/png" ||
            mime == "image/webp" || mime == "image/heic" || mime == "image/heif" ||
            mime.startsWith("image/") && !lowerName.endsWith(".gif")
    }

    /**
     * Returns a new JPEG file, or null when the image cannot be decoded (in which
     * case the caller simply sends the original bytes).
     */
    fun compress(source: File, targetDir: File): File? {
        return try {
            // 1. Bounds only, so a 100 MP photo never lands in the heap whole.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (
                bounds.outWidth / sampleSize > MAX_DIMENSION * 2 ||
                bounds.outHeight / sampleSize > MAX_DIMENSION * 2
            ) {
                sampleSize *= 2
            }

            // 2. Decode at the smallest power-of-two scale that is still larger
            //    than the target, then scale exactly.
            val decoded = BitmapFactory.decodeFile(
                source.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            ) ?: return null

            val rotated = applyExifRotation(source, decoded)
            val scaled = scaleToFit(rotated, MAX_DIMENSION)
            if (scaled !== rotated && !rotated.isRecycled) rotated.recycle()

            val outFile = File(targetDir, "chat_img_" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(outFile).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            scaled.recycle()

            // Never make a file bigger than the original.
            if (outFile.length() >= source.length()) {
                outFile.delete()
                return null
            }
            outFile
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Photo too large to re-encode; sending the original")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Photo re-encode failed: ${e.message}")
            null
        }
    }

    private fun scaleToFit(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    /**
     * Cameras store the orientation in EXIF instead of rotating the pixels, so a
     * downscaled copy has to bake that rotation in or portraits come out sideways.
     */
    private fun applyExifRotation(source: File, bitmap: Bitmap): Bitmap {
        val degrees = try {
            @Suppress("DEPRECATION")
            when (
                ExifInterface(source.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }

        if (degrees == 0f) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it !== bitmap) bitmap.recycle() }
        } catch (e: Exception) {
            Log.w(TAG, "EXIF rotation failed: ${e.message}")
            bitmap
        }
    }
}
