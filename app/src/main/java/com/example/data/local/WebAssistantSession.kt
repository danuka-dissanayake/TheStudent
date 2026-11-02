package com.example.data.local

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Session state for the in-app AI assistant WebView.
 *
 * The website keeps its own chat state in cookies / localStorage. That survives
 * closing and reopening the screen, but it must NOT survive a sign-out: on a
 * shared phone the next student would otherwise inherit the previous student's
 * conversations. `clear()` is called from the same place the app clears tutor
 * memory and speaking stats.
 */
object WebAssistantSession {

    fun prepare(webView: WebView) {
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        // Only first-party state. The assistant has no reason to read anything
        // belonging to another origin loaded inside this WebView.
        cookies.setAcceptThirdPartyCookies(webView, false)
    }

    fun flush() {
        CookieManager.getInstance().flush()
    }

    fun clear(context: Context) {
        val cookies = CookieManager.getInstance()
        cookies.removeAllCookies(null)
        cookies.flush()
        // localStorage / Web SQL / IndexedDB for every origin in this app's
        // WebView. There is only one WebView and it belongs to this feature.
        WebStorage.getInstance().deleteAllData()
    }
}
