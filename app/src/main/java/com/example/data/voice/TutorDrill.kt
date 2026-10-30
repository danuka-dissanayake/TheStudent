package com.example.data.voice

/**
 * Comparing what the student *said* with what they were asked to say.
 *
 * A drill — "say the corrected sentence" — only works if the app can tell
 * whether it happened. The recogniser is not a judge of pronunciation, so this
 * is deliberately forgiving: it counts words, not sounds, and it ignores the
 * small words a recogniser drops anyway ("the", "a", "to").
 *
 * The rule that keeps it fair: **pass is about the content words**. A student who
 * says "I went to the temple yesterday" when asked for "I went to temple
 * yesterday" has done the thing being practised.
 */
object TutorDrill {

    /** Words a recogniser routinely swallows; they must not decide a pass. */
    private val FILLERS = setOf(
        "um", "uh", "er", "ah", "hmm", "mm", "like", "you", "know", "i", "mean",
        "the", "a", "an", "to", "of", "and", "so", "well", "okay", "ok"
    )

    /** Did they say the target sentence? */
    fun matches(heard: String, target: String): Boolean {
        val wanted = contentWords(target)
        if (wanted.isEmpty()) return false
        val said = contentWords(heard).toMutableList()
        if (said.isEmpty()) return false
        var hits = 0
        for (word in wanted) {
            val index = said.indexOf(word)
            if (index >= 0) {
                hits++
                said.removeAt(index)
            }
        }
        return hits.toFloat() / wanted.size >= PASS_RATIO
    }

    /** Content words of the target the recogniser never heard — the ones to practise. */
    fun missedWords(heard: String, target: String): List<String> {
        val said = contentWords(heard).toMutableList()
        val missed = ArrayList<String>()
        for (word in contentWords(target)) {
            val index = said.indexOf(word)
            if (index >= 0) said.removeAt(index) else missed.add(word)
        }
        return missed.distinct()
    }

    private fun contentWords(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z']+"))
            .filter { it.isNotBlank() && it !in FILLERS }

    /** Four words in five is a pass: one slipped word is not a failed drill. */
    private const val PASS_RATIO = 0.75f
}
