package com.example.data.voice

/**
 * Better words for the ones a learner reaches for first.
 *
 * A grammar checker says what is wrong. This says what could be *stronger*, which
 * is the other half of what a good teacher gives you — and the half a student
 * actually remembers, because "very good" → "excellent" is a fact they can use
 * in the next sentence.
 *
 * Two rules keep it honest:
 *
 *  - Only whole phrases match, and only in a sentence of at least five words.
 *    "good" alone is never touched; "very good" is, because that exact pair is
 *    what a learner says when they have no stronger word yet. Single-word
 *    "upgrades" used to put a yellow card on almost every correct sentence,
 *    which is nagging, not teaching.
 *  - Nothing here is ever presented as a mistake. The app calls it *try*, not
 *    *wrong*, and the grammar fix keeps its own line.
 */
object WordUpgrades {

    /** Plain phrase → the phrase a fluent speaker would reach for. */
    private val upgrades: List<Pair<String, String>> = listOf(
        "very good" to "excellent",
        "very bad" to "terrible",
        "very big" to "huge",
        "very small" to "tiny",
        "very happy" to "delighted",
        "very sad" to "miserable",
        "very tired" to "exhausted",
        "very hungry" to "starving",
        "very hot" to "boiling",
        "very cold" to "freezing",
        "very beautiful" to "stunning",
        "very interesting" to "fascinating",
        "very boring" to "tedious",
        "very difficult" to "challenging",
        "very easy" to "straightforward",
        "very important" to "crucial",
        "very angry" to "furious",
        "very nice" to "lovely",
        "very often" to "frequently",
        "a lot of" to "plenty of",
        "lots of" to "plenty of"
    )

    /**
     * The single best upgrade for this sentence, or null.
     *
     * Longest phrase first, so "very good" wins over "good" — otherwise the
     * student is told to replace the wrong half of their own sentence.
     */
    fun suggest(sentence: String): Pair<String, String>? {
        // A single word is too likely to be an ASR artefact, and "good" on its
        // own is not a weaker word. Only whole phrases in a real sentence count.
        if (TutorCoach.wordCount(sentence) < 5) return null
        val text = sentence.lowercase()
        return upgrades
            .sortedByDescending { it.first.length }
            .firstOrNull { (from, to) ->
                from != to && containsPhrase(text, from)
            }
    }

    private fun containsPhrase(text: String, phrase: String): Boolean {
        var index = text.indexOf(phrase)
        while (index >= 0) {
            val before = text.getOrNull(index - 1)
            val after = text.getOrNull(index + phrase.length)
            val startsClean = before == null || !before.isLetterOrDigit()
            val endsClean = after == null || !after.isLetterOrDigit()
            if (startsClean && endsClean) return true
            index = text.indexOf(phrase, index + 1)
        }
        return false
    }
}
