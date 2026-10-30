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
 *  - Only whole phrases match. "good" alone is never touched; "very good" is,
 *    because that exact pair is what a learner says when they have no stronger
 *    word yet.
 *  - Nothing here is ever presented as a mistake. The app calls it *try*, not
 *    *wrong*, and the grammar fix keeps its own line.
 */
object WordUpgrades {

    /** Plain phrase → the phrase a fluent speaker would reach for. */
    private val upgrades: List<Pair<String, String>> = listOf(
        "very good" to "excellent",
        "very bad" to "awful",
        "very big" to "huge",
        "very small" to "tiny",
        "very happy" to "delighted",
        "very sad" to "miserable",
        "very tired" to "exhausted",
        "very hungry" to "starving",
        "very thirsty" to "parched",
        "very hot" to "boiling",
        "very cold" to "freezing",
        "very beautiful" to "stunning",
        "very ugly" to "hideous",
        "very interesting" to "fascinating",
        "very boring" to "tedious",
        "very difficult" to "tough",
        "very easy" to "simple",
        "very important" to "crucial",
        "very angry" to "furious",
        "very fast" to "rapid",
        "very slow" to "sluggish",
        "very clean" to "spotless",
        "very dirty" to "filthy",
        "very smart" to "brilliant",
        "very funny" to "hilarious",
        "very scary" to "terrifying",
        "very old" to "ancient",
        "very new" to "brand new",
        "very rich" to "wealthy",
        "very poor" to "destitute",
        "a lot of" to "plenty of",
        "lots of" to "plenty of",
        "very often" to "frequently",
        "nowadays" to "these days",
        "in my opinion" to "personally, I think",
        "I think" to "I believe",
        "I want to say" to "I would like to add",
        "nice" to "lovely",
        "happy" to "glad",
        "tired" to "worn out",
        "difficult" to "challenging",
        "important" to "significant",
        "interesting" to "fascinating",
        "beautiful" to "stunning",
        "angry" to "furious",
        "afraid" to "frightened",
        "surprised" to "astonished",
        "great" to "remarkable",
        "good" to "excellent",
        "bad" to "poor",
        "big" to "large",
        "small" to "modest"
    )

    /**
     * The single best upgrade for this sentence, or null.
     *
     * Longest phrase first, so "very good" wins over "good" — otherwise the
     * student is told to replace the wrong half of their own sentence.
     */
    fun suggest(sentence: String): Pair<String, String>? {
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
