package com.example.ui.webview

import android.app.Activity
import android.content.ComponentCallbacks2
import android.util.Log
import android.webkit.WebView
import com.example.BuildConfig
import com.example.data.local.TheStudentPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Owns the single warm AI Assistant [AiWebSession].
 *
 * Three things keep the screen feeling native:
 *
 * 1. **Warm start** — [prewarm] builds the WebView while the student is still on
 *    the dashboard, so the first tap shows a painted page instead of a spinner.
 *    A cheap `HEAD` request runs on a background thread to warm DNS + TLS.
 * 2. **Retention** — the session survives leaving the screen, so coming back is
 *    instant. It is dropped on logout, renderer crash, 15 minutes idle, or when
 *    the process is under memory pressure.
 * 3. **Thread safety** — every entry point is main-thread only.
 */
object AiWebSessionManager {

    private const val TAG = "AiWebSession"

    @Volatile
    private var session: AiWebSession? = null

    /** Only used for the one-shot connection warm-up. */
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var connectionWarmed = false

    init {
        // Lets the page be inspected from chrome://inspect in debug builds only.
        if (BuildConfig.DEBUG) {
            runCatching { WebView.setWebContentsDebuggingEnabled(true) }
        }
    }

    /** The session the screen should render, creating it if needed. */
    fun obtain(activity: Activity): AiWebSession {
        val existing = session
        if (existing != null &&
            existing.ownerActivity === activity &&
            !existing.isExpired()
        ) {
            return existing
        }
        existing?.let { runCatching { it.destroy() } }
        val fresh = AiWebSession(activity)
        session = fresh
        return fresh
    }

    fun peek(): AiWebSession? = session

    /**
     * Build and start loading the page ahead of time. Called a few hundred
     * milliseconds after the app is up, so it never competes with first paint.
     */
    fun prewarm(activity: Activity) {
        if (session != null) return
        val preferences = TheStudentPreferences(activity.applicationContext)
        if (!preferences.hasUsableSession()) return
        runCatching {
            session = AiWebSession(activity).also { it.warmUp() }
        }.onFailure {
            Log.w(TAG, "prewarm failed", it)
            session = null
        }
        warmUpConnection()
    }

    fun onAppBackground() {
        session?.onAppBackground()
    }

    fun onAppForeground() {
        session?.onAppForeground()
    }

    /** The hosting activity is going away (rotation or finish). */
    fun onActivityDestroyed(activity: Activity) {
        val current = session ?: return
        if (current.ownerActivity !== activity) return
        runCatching { current.destroy() }
        session = null
        // The WebView holds an Activity reference; never keep it past that Activity.
        Log.d(TAG, "session released with activity")
    }

    /**
     * Memory pressure. The retained renderer is the most expensive thing this app
     * keeps off screen, so it goes first — cheaper than the OS killing the process.
     */
    fun onTrimMemory(level: Int) {
        val current = session ?: return
        if (current.isAttached) return
        val evict = level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level == ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
            level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        if (!evict) return
        runCatching { current.destroy() }
        session = null
        Log.d(TAG, "session evicted on trim memory level $level")
    }

    /** Logout must not leave a signed-in page alive in RAM. */
    fun releaseForLogout() {
        val current = session ?: return
        runCatching { current.destroy() }
        session = null
    }

    /**
     * Warm DNS + TLS for the chat host so the real navigation skips the handshake.
     * Failure here is invisible and harmless: the WebView does its own connection.
     */
    private fun warmUpConnection() {
        if (connectionWarmed) return
        connectionWarmed = true
        background.launch {
            val client = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
            try {
                client.newCall(Request.Builder().url(AI_ASSISTANT_WEB_URL).head().build())
                    .execute()
                    .close()
            } catch (_: IOException) {
                // Offline, captive portal, DNS blocked — nothing to do.
            } finally {
                runCatching { client.connectionPool.evictAll() }
                runCatching { client.dispatcher.executorService.shutdown() }
            }
        }
    }
}
