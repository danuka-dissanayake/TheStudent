package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject

/**
 * One row in the assistant's chat-history drawer. The website owns the real
 * conversations; the app only receives this small, non-credential summary over
 * the origin-pinned bridge so the student can pick one without a website
 * sidebar (which is hidden in `?app=android` embed mode).
 */
data class AssistantChatItem(
    val id: String,
    val title: String,
    val updatedAt: String? = null
)

/**
 * Defensive parsing of `CHAT_HISTORY` bridge payloads.
 *
 * Web content is untrusted: ids are restricted to a safe charset before they
 * can ever be sent back into JS, titles are length-capped, and the list is
 * bounded so a malicious/buggy page cannot flood the drawer.
 */
object AssistantChatHistory {

    const val MAX_ITEMS = 50
    const val MAX_TITLE_LENGTH = 120
    const val MAX_TIME_LENGTH = 40

    private val SAFE_ID = Regex("^[A-Za-z0-9._-]{1,128}$")

    fun parse(payload: JSONObject?): List<AssistantChatItem> {
        if (payload == null) return emptyList()
        val raw = payload.optJSONArray("chats") ?: JSONArray()
        val result = ArrayList<AssistantChatItem>(minOf(raw.length(), MAX_ITEMS))
        for (i in 0 until raw.length()) {
            if (result.size >= MAX_ITEMS) break
            val entry = raw.optJSONObject(i) ?: continue
            val id = entry.optString("id")
            if (!SAFE_ID.matches(id)) continue
            val title = entry.optString("title").trim().take(MAX_TITLE_LENGTH)
            if (title.isEmpty()) continue
            val time = entry.optString("updatedAt").take(MAX_TIME_LENGTH).ifBlank { null }
            result.add(AssistantChatItem(id = id, title = title, updatedAt = time))
        }
        return result
    }
}
