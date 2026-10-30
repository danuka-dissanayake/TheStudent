package com.example.data.voice

/**
 * Turns a written reply into something a person would actually say out loud.
 *
 * A language model writes for the eye: long sentences, markdown, the odd emoji,
 * numbered lists. Handed to text-to-speech as-is, that produces the flat,
 * hurried, slightly wrong-sounding speech people associate with phone menus.
 * Three things fix it, and all three are about *where the utterance boundaries
 * are*:
 *
 *  1. **Short units.** Engines pick their intonation per utterance. A 40-word
 *     sentence read in one breath has no shape; the same words split at the
 *     commas get a rise going in and a fall coming out. So a long sentence is
 *     broken at its own punctuation and re-joined into units of at most
 *     [MAX_UTTERANCE] characters — long enough to breathe, short enough to have
 *     a melody.
 *  2. **Nothing to read literally.** Asterisks, hashes, backticks, emoji and
 *     bullets are removed, `…` becomes a comma, and quotes are normalised. A
 *     tutor that says "asterisk" out loud is not a tutor.
 *  3. **Every unit ends with punctuation.** Engines decide whether a unit is a
 *     statement or a question from its last character; a fragment with no
 *     terminal mark gets a rushed, clipped delivery.
 *
 * This is a pure function so it can be unit-tested and, more importantly, so the
 * text shown in the transcript is never touched — only what is spoken.
 */
object TutorSpeech {

    /** Roughly 15 spoken words: the point where one breath of intonation ends. */
    private const val MAX_UTTERANCE = 95

    private val EMOJI = Regex(
        "[\\u2190-\\u21FF\\u2300-\\u23FF\\u25A0-\\u27BF\\u2B00-\\u2BFF" +
            "\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\uFE0F\\u2600-\\u26FF]"
    )
    private val MARKDOWN = Regex("[*_`#>•·]+")
    private val SPACES = Regex("\\s+")
    private val SENTENCE_END = Regex("(?<=[.!?…])\\s+")
    private val CLAUSE_END = Regex("(?<=[,;:—–])\\s+")

    private val QUESTION_WORDS = setOf(
        "what", "where", "when", "why", "who", "whom", "whose", "which", "how"
    )
    private val AUXILIARIES = setOf(
        "do", "does", "did", "can", "could", "would", "should", "will", "shall",
        "is", "are", "am", "was", "were", "have", "has", "had", "may", "might"
    )
    // "is it", "do you" are questions; "is going" is not, so a participle right
    // after the auxiliary keeps the sentence a statement.
    private val CONTRACTIONS_AFTER_AUX = setOf(
        "going", "gonna", "coming", "trying", "doing", "making", "very", "so",
        "really", "quite", "always", "still", "just", "not"
    )

    /**
     * The reply, ready to be queued one utterance at a time.
     *
     * Returns an empty list when there is nothing speakable, so callers can treat
     * that exactly like "no reply".
     */
    fun linesOf(reply: String): List<String> {
        val cleaned = clean(reply)
        if (cleaned.isEmpty()) return emptyList()

        val out = ArrayList<String>()
        for (sentence in cleaned.split(SENTENCE_END)) {
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.length <= MAX_UTTERANCE) {
                out.add(terminate(trimmed))
                continue
            }
            // A long sentence: break it where the writer already paused, then
            // re-join the pieces until each unit is worth a breath.
            val clauses = trimmed.split(CLAUSE_END).map { it.trim() }.filter { it.isNotEmpty() }
            val builder = StringBuilder()
            for (clause in clauses) {
                val piece = if (clause.last() in ",;:—–") clause.dropLast(1).trim() else clause
                if (piece.isEmpty()) continue
                if (builder.length == 0) {
                    builder.append(piece)
                    continue
                }
                if (builder.length + 2 + piece.length <= MAX_UTTERANCE) {
                    builder.append(", ").append(piece)
                } else {
                    out.add(terminate(builder.toString()))
                    builder.setLength(0)
                    builder.append(piece)
                }
            }
            if (builder.length > 0) out.add(terminate(builder.toString()))
        }
        return out.map { it.trim() }.filter { it.length > 1 }
    }

    // ── the student's own speech ─────────────────────────────────────────────

    /**
     * Punctuation a speech recogniser never gives us.
     *
     * Google's recogniser returns a bare lowercase word chain — "i went to the
     * beach yesterday with my cousins" — and the transcript then looks like a
     * text message, not the student's *sentence*. Worse, the grammar checker and
     * the model both do better work on punctuated text.
     *
     * This is deliberately conservative: it never rewrites words, only
     * presentation. It capitalises sentence starts and the pronoun "I", adds a
     * comma after a leading discourse marker, and decides between `?` and `.`
     * from the shape of the sentence. Anything it is unsure about gets a full
     * stop.
     */
    fun polishRecognised(raw: String): String {
        val base = raw.replace(SPACES, " ").trim()
        if (base.isEmpty()) return base

        val sentences = splitSentences(base)
        val out = StringBuilder()
        for (sentence in sentences) {
            val cleaned = sentence.trim().trimEnd(',', ';', ':').trim()
            if (cleaned.isEmpty()) continue
            if (out.isNotEmpty()) out.append(' ')
            out.append(terminate(capitalise(cleaned)))
        }
        return out.toString()
    }

    /**
     * Split a bare recognised string into sentence-sized pieces.
     *
     * The recogniser does not punctuate, so there is nothing to split on. Two
     * things still mark a boundary: explicit punctuation when the engine does
     * produce it, and a long "and/but/so/then" run — the point where a speaker has
     * clearly started a new thought. Both are handled here, and nothing shorter
     * than ~60 characters is ever broken, so a normal sentence stays whole.
     */
    private fun splitSentences(text: String): List<String> {
        if (text.any { it == '.' || it == '?' || it == '!' }) {
            return text.split(SENTENCE_END).map { it.trim() }.filter { it.isNotEmpty() }
        }
        val breakers = Regex("\\s+(?=(?:and then|but then|but|so|and)\\s)", RegexOption.IGNORE_CASE)
        val pieces = text.split(breakers).map { it.trim() }.filter { it.isNotEmpty() }
        if (pieces.size <= 1) return listOf(text)
        val merged = ArrayList<String>()
        val builder = StringBuilder()
        for (piece in pieces) {
            if (builder.isEmpty()) {
                builder.append(piece)
                continue
            }
            if (builder.length < 60) {
                builder.append(' ').append(piece)
            } else {
                merged.add(builder.toString())
                builder.setLength(0)
                builder.append(piece)
            }
        }
        if (builder.isNotEmpty()) merged.add(builder.toString())
        return merged
    }

    /** First letter up, and the pronoun "I" up wherever it stands alone. */
    private fun capitalise(text: String): String {
        if (text.isEmpty()) return text
        var out = text
            .replace(Regex("\\bi\\b"), "I")
            .replace(Regex("\\bi'"), "I'")
        out = out.replaceFirstChar { it.uppercaseChar() }
        // A leading discourse marker reads as a pause, not as part of the clause.
        val markers = listOf("well", "so", "okay", "ok", "actually", "honestly",
            "basically", "anyway", "yeah", "oh", "hmm")
        for (marker in markers) {
            val pattern = Regex("^" + Regex.escape(marker) + "\\s+", RegexOption.IGNORE_CASE)
            if (out.startsWith(marker, ignoreCase = true) && !out.startsWith("$marker,", true)) {
                out = pattern.replace(out, marker.replaceFirstChar { it.uppercaseChar() } + ", ")
                break
            }
        }
        return out
    }

    /** Strip everything a reader would silently ignore but a voice would read. */
    private fun clean(text: String): String {
        var out = text
            .replace("\r", "\n")
            .replace(Regex("(?m)^\\s*[-•*]\\s+"), " ")      // bullets
            .replace(Regex("(?m)^\\s*\\d+[.)]\\s+"), " ")   // numbered lists
        out = EMOJI.replace(out, " ")
        out = MARKDOWN.replace(out, "")
        out = out
            .replace("“", "\"").replace("”", "\"")
            .replace("‘", "'").replace("’", "'")
            .replace("…", ", ")
            .replace(" - ", ", ")                          // a dash is a pause
            .replace(SPACES, " ")
            .trim()
        return out
    }

    /**
     * Give the unit an ending the engine can read a melody into.
     *
     * `?` and `!` are left alone — they are the interesting ones. Anything else
     * gets a full stop, which is also what makes the synthesised pause between
     * sentences sound intentional rather than accidental.
     */
    private fun terminate(unit: String): String {
        val text = unit.trim().trimEnd(',', ';', ':', '-', '—', '–').trim()
        if (text.isEmpty()) return ""
        val last = text.last()
        if (last == '.' || last == '!' || last == '?' || last == '"' || last == '\'') return text
        return if (looksLikeQuestion(text)) "$text?" else "$text."
    }

    /**
     * Does this read as a question?
     *
     * The recognised text has no `?`, so the only signal is how it opens: a
     * question word, or an auxiliary in front of the subject. "I asked him where
     * he lived" must stay a statement, so a wh-word only counts as the *first*
     * word.
     */
    private fun looksLikeQuestion(text: String): Boolean {
        val first = text.substringBefore(' ').lowercase().trim(',', '.')
        if (first in QUESTION_WORDS) return true
        val second = text.split(' ').getOrNull(1)?.lowercase().orEmpty()
        return first in AUXILIARIES && second.isNotEmpty() && second !in CONTRACTIONS_AFTER_AUX
    }
}
