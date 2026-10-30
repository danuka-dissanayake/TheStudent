package com.example.data.voice

import android.content.Context
import android.util.Log
import com.example.data.model.GrammarFix
import org.json.JSONArray
import org.json.JSONObject

/**
 * One mistake the student made, waiting to be asked about again.
 *
 * [wrong] is what they actually said — it is what makes the question land —
 * and [right] is the sentence they are being asked to produce. [dueAt] follows a
 * small spaced-repetition ladder: one day, three days, a week, then it is gone.
 */
data class ReviewItem(
    val wrong: String,
    val right: String,
    val rule: String,
    val dueAt: Long,
    val successes: Int
)

/**
 * What the tutor remembers between calls.
 *
 * Three things outlive a single call, and each one earns its place:
 *
 *  - **level** — the CEFR target, so a student who has grown out of A2 questions
 *    stops being asked them;
 *  - **reviews** — their own corrected sentences, resurfacing on a schedule;
 *  - **facts** — "I have an exam on Tuesday", so the next call can ask how it
 *    went. This is the difference between a tutor and a stranger with a script.
 *
 * Kept in its own small SharedPreferences file rather than the session store:
 * none of it is authentication, none of it is sensitive, and a student who signs
 * out should not lose the sentences they have been practising.
 */
class TutorProgressStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** CEFR level the tutor aims at. */
    var level: String
        get() = prefs.getString(KEY_LEVEL, DEFAULT_LEVEL) ?: DEFAULT_LEVEL
        set(value) = prefs.edit().putString(KEY_LEVEL, value).apply()

    /** One line describing the last call, for the idle screen. */
    var lastCallNote: String?
        get() = prefs.getString(KEY_LAST_CALL, null)
        set(value) = prefs.edit().putString(KEY_LAST_CALL, value).apply()

    // ── reviews ─────────────────────────────────────────────────────────────

    /** Everything the student has got wrong and not yet mastered. */
    fun savedReviews(): List<ReviewItem> = readReviews()

    /** What is due now, oldest first. */
    fun dueReviews(now: Long = System.currentTimeMillis(), limit: Int = 3): List<ReviewItem> =
        readReviews().filter { it.dueAt <= now }.sortedBy { it.dueAt }.take(limit)

    fun dueCount(now: Long = System.currentTimeMillis()): Int =
        readReviews().count { it.dueAt <= now }

    /**
     * Add this call's corrections to the ladder.
     *
     * A sentence corrected twice is stored once: seeing the same wrong sentence
     * listed four times is noise, and the point of the list is what to practise.
     */
    fun saveCorrections(fixes: List<GrammarFix>, now: Long = System.currentTimeMillis()) {
        val useful = fixes.filter { it.isUseful && it.corrected.isNotBlank() }
        if (useful.isEmpty()) return
        val all = readReviews().toMutableList()
        useful.forEach { fix ->
            val index = all.indexOfFirst { it.right.equals(fix.corrected, ignoreCase = true) }
            val fresh = ReviewItem(
                wrong = fix.original,
                right = fix.corrected,
                rule = fix.rule,
                dueAt = now + ONE_DAY,
                successes = if (index >= 0) all[index].successes else 0
            )
            if (index >= 0) all[index] = fresh else all.add(fresh)
        }
        while (all.size > MAX_REVIEWS) all.removeAt(0)
        writeReviews(all)
    }

    /** Record how a review drill went, and push the item along the ladder. */
    fun markReviewed(item: ReviewItem, correct: Boolean, now: Long = System.currentTimeMillis()) {
        val all = readReviews().toMutableList()
        val index = all.indexOfFirst { it.right.equals(item.right, ignoreCase = true) }
        if (index < 0) return
        val current = all[index]
        if (correct) {
            val successes = current.successes + 1
            if (successes >= GRADUATE_AT) {
                all.removeAt(index)
            } else {
                all[index] = current.copy(
                    successes = successes,
                    dueAt = now + if (successes == 1) THREE_DAYS else ONE_WEEK
                )
            }
        } else {
            // Back to tomorrow: it is clearly not automatic yet.
            all[index] = current.copy(successes = 0, dueAt = now + ONE_DAY)
        }
        writeReviews(all)
    }

    // ── facts the student has told the tutor ────────────────────────────────

    fun memory(): List<String> = try {
        val array = JSONArray(prefs.getString(KEY_MEMORY, "[]"))
        (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Tutor memory unreadable: ${e.message}")
        emptyList()
    }

    /**
     * Remember one fact about the student.
     *
     * De-duplicated on substring: "an exam on Tuesday" and "the exam on Tuesday"
     * are the same fact, and a memory full of near-copies stops being useful.
     */
    fun addMemory(text: String) {
        val fact = text.trim().trim('"', '.', ' ').take(90)
        if (fact.length < 4) return
        val existing = memory().toMutableList()
        val alreadyKnown = existing.any { known ->
            known.equals(fact, ignoreCase = true) ||
                known.contains(fact, ignoreCase = true) ||
                fact.contains(known, ignoreCase = true)
        }
        if (alreadyKnown) return
        existing.add(fact)
        while (existing.size > MAX_MEMORIES) existing.removeAt(0)
        prefs.edit().putString(KEY_MEMORY, JSONArray(existing).toString()).apply()
    }

    fun forgetMemory() {
        prefs.edit().remove(KEY_MEMORY).apply()
    }

    // ── storage ─────────────────────────────────────────────────────────────

    private fun readReviews(): List<ReviewItem> = try {
        val array = JSONArray(prefs.getString(KEY_REVIEWS, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            val right = row.optString("r")
            if (right.isBlank()) return@mapNotNull null
            ReviewItem(
                wrong = row.optString("w"),
                right = right,
                rule = row.optString("rule"),
                dueAt = row.optLong("due", 0L),
                successes = row.optInt("ok", 0)
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Review list unreadable: ${e.message}")
        emptyList()
    }

    private fun writeReviews(items: List<ReviewItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("w", item.wrong)
                    .put("r", item.right)
                    .put("rule", item.rule)
                    .put("due", item.dueAt)
                    .put("ok", item.successes)
            )
        }
        prefs.edit().putString(KEY_REVIEWS, array.toString()).apply()
    }

    companion object {
        private const val TAG = "TutorProgressStore"
        private const val PREFS_NAME = "thestudent_tutor_progress"
        private const val KEY_LEVEL = "tutor_level"
        private const val KEY_LAST_CALL = "tutor_last_call"
        private const val KEY_REVIEWS = "tutor_reviews"
        private const val KEY_MEMORY = "tutor_memory"

        const val DEFAULT_LEVEL = "B1"
        val CEFR_LADDER = listOf("A2", "B1", "B2")

        private const val MAX_REVIEWS = 30
        private const val MAX_MEMORIES = 8
        private const val GRADUATE_AT = 3

        private const val ONE_DAY = 24L * 60 * 60 * 1000
        private const val THREE_DAYS = 3L * ONE_DAY
        private const val ONE_WEEK = 7L * ONE_DAY
    }
}
