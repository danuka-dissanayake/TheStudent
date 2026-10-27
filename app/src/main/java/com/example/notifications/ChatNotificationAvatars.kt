package com.example.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Pictures for chat notifications.
 *
 * The large icon of a message notification is the student's photo, exactly like
 * every other messaging app. Two details make it look deliberate rather than
 * improvised:
 *
 *  * photos are downloaded once and kept in a small in-memory cache, because
 *    notifications are rebuilt on every poll;
 *  * a student **without** a photo gets a coloured circle with their initial, so
 *    the icon is never blank and the colour stays the same for that student.
 *
 * Nothing here may throw: a missing picture must never cost a notification.
 */
object ChatNotificationAvatars {

    private const val TAG = "ChatAvatars"

    /** Enough for the large icon; the shade never shows more. */
    private const val ICON_SIZE_PX = 192

    /** A profile picture bigger than this is not worth the download time. */
    private const val MAX_DOWNLOAD_BYTES = 2 * 1024 * 1024

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val cache = object : LruCache<String, Bitmap>(24) {}

    /**
     * The student's photo as a circle, or null when they have none / it cannot
     * be fetched right now.
     */
    suspend fun photo(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bytes = response.body?.bytes() ?: return@use null
                    if (bytes.isEmpty() || bytes.size > MAX_DOWNLOAD_BYTES) return@use null
                    val decoded = decodeScaled(bytes) ?: return@use null
                    circle(decoded).also { cache.put(url, it) }
                }
            }.onFailure { Log.w(TAG, "Could not load notification avatar", it) }
                .getOrNull()
        }
    }

    /**
     * A coloured circle with the student's first letter, used when there is no
     * photo. The colour is derived from the peer id, so it never changes for a
     * given student.
     */
    fun initials(name: String, peerId: Int): Bitmap? = runCatching {
        val size = ICON_SIZE_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paletteColor(peerId)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, background)

        val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metrics = text.fontMetrics
        val baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(letter, size / 2f, baseline, text)
        bitmap
    }.getOrNull()

    private fun paletteColor(peerId: Int): Int {
        // The same family of greens and teals the app already uses, rotated per
        // student so two people next to each other never look identical.
        val palette = intArrayOf(
            0xFF10B981.toInt(),
            0xFF0EA5E9.toInt(),
            0xFF6366F1.toInt(),
            0xFF8B5CF6.toInt(),
            0xFFF59E0B.toInt(),
            0xFFEC4899.toInt(),
            0xFF14B8A6.toInt()
        )
        val index = ((peerId % palette.size) + palette.size) % palette.size
        return palette[index]
    }

    /** Scale down while decoding, so a 4000 px photo never costs 60 MB. */
    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / sample > ICON_SIZE_PX * 2) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val scale = ICON_SIZE_PX.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1)
        if (scale >= 1f) return decoded
        return Bitmap.createScaledBitmap(
            decoded,
            max(1, (decoded.width * scale).toInt()),
            max(1, (decoded.height * scale).toInt()),
            true
        )
    }

    /** Centre-crop to a square and mask it to a circle. */
    private fun circle(source: Bitmap): Bitmap {
        val size = ICON_SIZE_PX
        val side = minOf(source.width, source.height).coerceAtLeast(1)
        val square = Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 2,
            side,
            side
        )

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(
            Matrix().apply { setScale(size.toFloat() / side, size.toFloat() / side) }
        )
        paint.shader = shader
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return output
    }
}
