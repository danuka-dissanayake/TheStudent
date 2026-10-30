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
        return if (last == '.' || last == '!' || last == '?' || last == '"' || last == '\'') {
            text
        } else {
            "$text."
        }
    }
}
