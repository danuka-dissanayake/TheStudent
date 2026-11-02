package com.example.data.voice

import java.util.Locale

/**
 * Pure decisions about what the microphone *probably* heard.
 *
 * Android's built-in [android.speech.SpeechRecognizer] is a one-shot engine: it
 * owns the microphone, returns one best guess after a pause, and stops. It also
 * gets short, unstressed words wrong more often than long ones ("did" -> "do",
 * "a" -> "the"), and it has never heard the vocabulary of a lesson.
 *
 * This object never invents a word. It only:
 *  - picks one of the guesses the engine actually returned (with a small boost
 *    for guesses containing the words the tutor was just talking about),
 *  - notices when two guesses disagree on a short word, or when the engine says
 *    it was unsure, so the app can ask the student instead of quietly grading a
 *    misheard sentence,
 *  - joins the pieces of a long turn without losing words at the seams.
 *
 * Nothing here touches Android: the same decisions are unit tested on the JVM
 * and used by the ViewModel on a real phone.
 */
object TranscriptReview {

    /** One guess from the engine. -1 means the engine did not report confidence. */
    data class Candidate(val text: String, val confidence: Float = -1f)

    data class Verdict(
        /** One of the engine's own guesses. Never a rewritten sentence. */
        val text: String,
        /** Ask the student to look at the words before they are sent. */
        val needsConfirmation: Boolean,
        /** Short words the engine heard two different ways. */
        val uncertainWords: List<String>,
        /** One short line explaining why we are asking. */
        val reason: String
    )

    /** Words short enough to be acoustically fragile or meaning-changing. */
    private val SHORT_WORDS = setOf(
        "a", "an", "the", "to", "of", "in", "on", "at", "by", "for", "with",
        "is", "it", "it's", "am", "are", "was", "were", "be", "been",
        "do", "does", "did", "don't", "doesn't", "didn't", "can", "can't",
        "could", "couldn't", "would", "wouldn't", "should", "have", "has",
        "had", "i", "you", "he", "she", "we", "they", "my", "your", "and",
        "but", "or", "not", "no", "yes", "will", "that", "this", "from",
        "never", "ever", "always"
    )

    /** Swapping one of these changes the answer, so it is worth one quick check. */
    private val MEANING_WORDS = setOf(
        "a", "an", "the", "to", "in", "on", "at", "is", "was", "were",
        "do", "does", "did", "don't", "doesn't", "didn't", "can", "can't",
        "could", "would", "should", "have", "has", "had", "been", "not",
        "no", "never", "it's", "from", "with", "for", "by"
    )

    /** A learner may really say these twice. Never delete a lone repeat. */
    private val REPEATABLE = setOf(
        "no", "yes", "okay", "ok", "wait", "stop", "sorry", "well", "go",
        "um", "uh", "hello", "thanks", "please"
    )

    /**
     * Glue words a restarted recogniser very often re-delivers as a one-word
     * tail ("...shop and" + "and then I came home"). They are safe to drop once;
     * the words in [REPEATABLE] are not.
     */
    private val TAIL_GLUE = setOf(
        "and", "but", "so", "then", "the", "a", "an", "to", "of", "in",
        "on", "at", "with", "that", "because", "or", "for", "from", "my", "your", "is"
    )

    /** Too common to be useful as a recognition hint. */
    private val HINT_STOP_WORDS = setOf(
        "about", "after", "again", "also", "asked", "because", "before",
        "could", "every", "from", "have", "into", "just", "lesson", "like",
        "more", "other", "please", "question", "really", "said", "sentence",
        "should", "some", "student", "teacher", "that", "their", "them",
        "then", "there", "these", "this", "today", "tutor", "what", "when",
        "where", "which", "will", "with", "would", "your", "yesterday"
    )

    /**
     * Choose one of the engine's guesses and decide whether to check it.
     *
     * Confidence is a weak signal (many engines never return it), so a guess is
     * only boosted for containing words the tutor put in the student's ear a
     * moment ago. Guesses that disagree on a short word are *not* silently
     * resolved — that is exactly the case where the student should decide.
     */
    fun choose(candidates: List<Candidate>, hints: List<String> = emptyList()): Verdict {
        val usable = candidates
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return Verdict("", false, emptyList(), "")

        val hintPhrases = hints.map { normaliseWords(it) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(40)

        fun score(candidate: Candidate): Float {
            val confidence = if (candidate.confidence in 0f..1f) candidate.confidence else 0.72f
            val words = normaliseWords(candidate.text)
            val hits = hintPhrases.count { containsPhrase(words, it) }
            // A guess that keeps the little grammar words is usually the one
            // that heard the whole utterance, not a fluent paraphrase of it.
            val shortKept = words.count { it in SHORT_WORDS }
            return confidence + 0.06f * minOf(hits, 3) + 0.004f * minOf(shortKept, 6)
        }

        val ranked = usable.mapIndexed { index, candidate ->
            Triple(index, candidate, score(candidate))
        }.sortedWith(
            compareByDescending<Triple<Int, Candidate, Float>> { it.third }.thenBy { it.first }
        )
        val best = ranked.first().second
        val uncertain = disagreeingShortWords(best.text, ranked.drop(1).map { it.second.text })
        // Some engines report 0 for "no score", so only a real low score counts.
        val lowConfidence = best.confidence > 0f && best.confidence < 0.62f
        val needsConfirmation = lowConfidence || uncertain.any { it in MEANING_WORDS }
        val reason = when {
            lowConfidence -> "I wasn't sure I caught every word."
            uncertain.isNotEmpty() -> "I heard a short word two ways."
            else -> ""
        }
        return Verdict(best.text, needsConfirmation, uncertain, reason)
    }

    /**
     * Join two pieces of heard speech without losing words at the seam.
     *
     * Three things happen in practice when the recogniser restarts mid-turn:
     *  - it re-delivers the tail of the last phrase ("...to the" + "to the shop"),
     *  - it re-delivers the *whole* turn so far (a cumulative final),
     *  - it delivers a genuinely new stretch that happens to repeat one word.
     *
     * Overlaps of two words or more are dropped once. A single-word overlap is
     * dropped only for a distinctive word or a conjunction/preposition the
     * engine commonly repeats — "no, no, no" is speech, not a stitching
     * artefact. Comparisons ignore case and punctuation; the output always keeps
     * the student's original words.
     */
    fun mergeSegments(existing: String, incoming: String): String {
        val a = existing.trim()
        val b = incoming.trim()
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a

        val left = words(a)
        val right = words(b)
        val la = left.map(::norm)
        val rb = right.map(::norm)

        // A cumulative final already contains everything: replacing is correct,
        // appending would double the whole sentence.
        if (rb.size >= la.size && rb.take(la.size) == la) return b

        var overlap = 0
        val max = minOf(la.size, rb.size, 8)
        for (n in max downTo 1) {
            if (la.takeLast(n) != rb.take(n)) continue
            val first = la[la.size - n]
            if (n >= 2 || first in TAIL_GLUE ||
                (first.length >= 4 && first !in REPEATABLE)
            ) overlap = n
            break
        }
        if (overlap >= right.size) return a
        return (left + right.drop(overlap)).joinToString(" ")
    }

    /**
     * Build the short vocabulary hints handed to the recogniser.
     *
     * Android 13+ accepts these as `EXTRA_BIASING_STRINGS`; on older phones the
     * same list is used to prefer one of the engine's guesses. Short function
     * words and lesson boilerplate are removed — a hint list of "the, and, said"
     * makes recognition worse, not better.
     */
    fun hintWords(
        sources: List<String>,
        extraTerms: List<String> = emptyList(),
        limit: Int = 36
    ): List<String> {
        val out = LinkedHashSet<String>()
        // The student's own terms come first: names, places and subject words
        // are exactly what a general model cannot guess.
        extraTerms.forEach { term ->
            val clean = term.trim()
            if (clean.isNotBlank() && clean.length <= 40) out.add(clean)
        }
        for (source in sources) {
            val tokens = normaliseWords(source).filter { it.isNotBlank() }
            for (i in 0 until tokens.size - 1) {
                val first = tokens[i]
                val second = tokens[i + 1]
                if (first.length >= 4 && second.length >= 3 &&
                    first !in HINT_STOP_WORDS && second !in HINT_STOP_WORDS
                ) {
                    out.add("$first $second")
                }
            }
            for (token in tokens) {
                if (token.length >= 4 &&
                    token !in HINT_STOP_WORDS &&
                    token.all { it.isLetterOrDigit() || it == '\'' }
                ) {
                    out.add(token)
                }
            }
            if (out.size >= limit) break
        }
        return out.take(limit).toList()
    }

    /**
     * Short words that two guesses spelled differently.
     *
     * A simple index-by-index comparison is wrong: one guess often contains an
     * extra clause, which shifts every following word and invents disagreements
     * that never happened. A tiny word-level edit-distance alignment finds real
     * *substitutions* ("did" -> "do") and ignores insertions and deletions.
     */
    private fun disagreeingShortWords(base: String, others: List<String>): List<String> {
        val a = normaliseWords(base)
        if (a.isEmpty()) return emptyList()
        val found = LinkedHashSet<String>()
        for (other in others) {
            val b = normaliseWords(other)
            if (b.isEmpty() || b == a || a.size > 40 || b.size > 40) continue
            val dp = Array(a.size + 1) { IntArray(b.size + 1) }
            for (i in 0..a.size) dp[i][0] = i
            for (j in 0..b.size) dp[0][j] = j
            for (i in 1..a.size) {
                for (j in 1..b.size) {
                    val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + substitution
                    )
                }
            }
            var i = a.size
            var j = b.size
            while (i > 0 && j > 0) {
                val left = a[i - 1]
                val right = b[j - 1]
                when {
                    left == right -> { i--; j-- }
                    dp[i][j] == dp[i - 1][j - 1] + 1 -> {
                        // A substitution, not a missing word. Only short swaps
                        // are worth interrupting the student for.
                        if (left.length <= 5 || right.length <= 5) {
                            if (left in SHORT_WORDS) found.add(left)
                            if (right in SHORT_WORDS) found.add(right)
                        }
                        i--; j--
                    }
                    dp[i][j] == dp[i - 1][j] + 1 -> i--
                    dp[i][j] == dp[i][j - 1] + 1 -> j--
                    else -> { i--; j-- }
                }
            }
        }
        return found.toList()
    }

    private fun containsPhrase(haystack: List<String>, needle: List<String>): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        for (start in 0..haystack.size - needle.size) {
            if (haystack.subList(start, start + needle.size) == needle) return true
        }
        return false
    }

    private fun words(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun normaliseWords(text: String): List<String> = words(text).map(::norm)

    /** Keep Unicode letters and apostrophes; drop only edge punctuation. */
    private fun norm(token: String): String =
        token.trim { !it.isLetterOrDigit() && it != '\'' }
            .replace('\u2019', '\'')
            .lowercase(Locale.US)
}
