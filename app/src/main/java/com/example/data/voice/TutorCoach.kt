package com.example.data.voice

import com.example.data.model.LessonStep
import java.util.Locale

/**
 * The tutor's decision layer.
 *
 * A speaking tutor is not a chat model with a microphone bolted on. It has to
 * decide, on every turn, how long to wait, when to correct, when to be quiet,
 * what to say when the network is gone, and whether a lesson step was actually
 * earned. Those decisions used to be scattered across the ViewModel as magic
 * numbers. They live here now, as pure functions, so their behaviour can be
 * reasoned about (and unit-tested) without a phone, an emulator or a server.
 *
 * The rules are grounded in the research collected in `docs/AI_TUTOR_RESEARCH.md`:
 * short contingent turns with one question, real wait time, selective feedback,
 * self-repair before modelling, ASR transcripts treated as untrusted, a hard
 * latency budget, and lesson steps that advance on evidence.
 */
object TutorCoach {

    /** Free conversation protects fluency; a focused drill may correct directly. */
    enum class FeedbackMode { FLUENCY, ACCURACY }

    /** What the tutor does about a silent microphone, measured in seconds. */
    enum class SilenceAction { SILENT_RETRY, EASIER_QUESTION, SENTENCE_STARTER, END_CALL }

    /** A reply that has been made safe to speak aloud. */
    data class ShapedReply(val speech: String, val question: String)

    /** Where a lesson goes after this student turn. */
    data class LessonProgress(
        /** The step the tutor's *reply* belongs to (not the step just answered). */
        val targetIndex: Int,
        val completed: Boolean,
        val advanced: Boolean
    )

    /** Evidence from a whole call, for the CEFR decision. */
    data class CallEvidence(
        val turns: Int,
        val cleanPercent: Int,
        val avgWords: Double,
        val usedComplex: Boolean
    )

    /** One model answer is never allowed to hold a live call in silence. */
    const val MAX_TURN_MS = 30_000L

    private const val MAX_REPLY_CHARS = 240
    private const val MAX_TRACKED_QUESTIONS = 12

    private val SENTENCE_SPLIT = Regex("(?<=[.!?])\\s+")
    private val WORD_SPLIT = Regex("[^a-z0-9']+")
    private val LABEL_LINE = Regex(
        "^(say|fix|rule|word|note)\\s*[:\\-—]\\s*(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val LEADING_MARKUP = Regex("^[*_#`>\\s-]+")
    private val STAGE_DIRECTIONS = Regex("\\([^)]*\\)|\\[[^]]*\\]")
    private val THINK_BLOCK = Regex("<think>[\\s\\S]*?</think>")
    private val THINK_TAG = Regex("</?think>")
    private val WHITESPACE = Regex("\\s+")
    private val PRAISE = Regex(
        "\\b(?:great job|well done|good job|good try|nice work|well said|" +
            "very interesting|fantastic|excellent|awesome|articulated|" +
            "that(?:'s| is) (?:very )?(?:interesting|great|wonderful|nice)|" +
            "you did (?:very )?well|i'm impressed)\\b",
        RegexOption.IGNORE_CASE
    )

    private val DANGLING_ENDS = setOf(
        "and", "but", "so", "because", "then", "or", "with", "to", "of", "the",
        "a", "an", "i", "my", "for", "when", "if", "although", "while", "after",
        "before", "also", "which", "who", "that"
    )

    private val ANCHOR_STOP_WORDS = setOf(
        "that", "this", "with", "have", "has", "had", "was", "were", "there",
        "their", "about", "really", "very", "just", "then", "when", "what",
        "because", "could", "would", "should", "from", "they", "them", "your",
        "mine", "went", "said", "like", "some", "more", "much", "many", "also",
        "into", "after", "before", "today", "yesterday", "tomorrow", "well",
        "okay", "yeah", "please", "thank", "thanks", "think", "want", "feel",
        "went", "been", "being", "will", "can", "did", "does", "are", "was"
    )

    private val GENERIC_QUESTIONS = listOf(
        "What happened next?",
        "What was that like for you?",
        "Why did you choose that?",
        "Who were you with?",
        "When did it happen?",
        "How did you feel about it?",
        "Can you give me one example?",
        "What would you do differently next time?",
        "What do you usually do at the weekend?",
        "Is that something you enjoy?",
        "What did you learn from it?",
        "Can you tell me one more detail?"
    )

    private val EASIER_QUESTIONS = listOf(
        "Was it yesterday or last week?",
        "Did you go alone or with someone?",
        "Was it good or not so good?",
        "Did that happen at home or at school?",
        "Would you like to talk about something easier?",
        "Was it in the morning or the evening?"
    )

    // ── what the tutor says ─────────────────────────────────────────────────

    /**
     * Make any tutor line safe to speak and safe to learn from.
     *
     * Models (and the old offline scripts) produce praise, two questions, lists,
     * markdown and stage directions. A call needs one short reaction and exactly
     * one question. This function:
     *  - strips think-tags, markdown, labels and stage directions;
     *  - drops generic praise rather than letting it replace the reaction;
     *  - keeps at most one statement and one question (the question always last);
     *  - swaps a question the tutor already asked for a fresh one;
     *  - guarantees the line ends with a question, so the student knows it is
     *    their turn.
     */
    fun shapeReply(raw: String, asked: Set<String> = emptySet()): ShapedReply {
        val cleaned = cleanForSpeech(raw)
        val candidates = if (cleaned.isBlank()) {
            emptyList()
        } else {
            SENTENCE_SPLIT.split(cleaned).map { it.trim() }.filter { it.isNotEmpty() }
        }
        val usable = candidates.filterNot { isGenericPraise(it) }
        val questions = usable.filter { it.contains('?') }
        val statements = usable.filterNot { it.contains('?') }

        var question = questions.lastOrNull { q -> !asked.contains(normaliseQuestion(q)) }
        if (question == null) {
            // Every question in this answer has been used. Either the learner has
            // heard the whole pool (reuse is better than nonsense), or we pick a
            // fresh one. A tutor must never repeat a question inside one call.
            question = if (asked.size >= MAX_TRACKED_QUESTIONS) {
                questions.lastOrNull() ?: freshQuestion(asked)
            } else {
                freshQuestion(asked)
            }
        }
        val finalQuestion = trimToOneQuestion(question ?: freshQuestion(asked))

        var statement = statements.lastOrNull()?.let { trimToOneSentence(it) }
        if (statement != null && statement.length > 150) statement = null
        var speech = listOfNotNull(statement, finalQuestion).joinToString(" ").trim()
        if (speech.length > MAX_REPLY_CHARS || wordCount(speech) > 36) {
            speech = finalQuestion
        }
        if (!speech.endsWith("?")) {
            speech = listOfNotNull(speech.takeIf { it.isNotBlank() }, freshQuestion(asked))
                .joinToString(" ")
        }
        return ShapedReply(speech.trim(), normaliseQuestion(finalQuestion))
    }

    /** A question that has not been asked in this call, with a scenario bias. */
    fun freshQuestion(asked: Set<String>, scenario: String = ""): String =
        (scenarioQuestions(scenario) + GENERIC_QUESTIONS)
            .firstOrNull { !asked.contains(normaliseQuestion(it)) }
            ?: "Can you tell me a little more about that?"

    /**
     * After real silence, narrow the task instead of filling the air with warmth.
     * An either/or question is answerable by a learner who has frozen.
     */
    fun easierQuestion(scenario: String, asked: Set<String> = emptySet()): String {
        val specific = when {
            scenario.contains("Coffee", true) -> listOf("Would you like tea or coffee?")
            scenario.contains("Interview", true) -> listOf("Was that at school or at work?")
            scenario.contains("Travel", true) -> listOf("Was the journey long or short?")
            scenario.contains("Grammar", true) -> listOf("Is that about the past or about now?")
            else -> emptyList()
        }
        return (specific + EASIER_QUESTIONS)
            .firstOrNull { !asked.contains(normaliseQuestion(it)) }
            ?: "Can you say that in one short sentence?"
    }

    /** The second rung of the silence ladder: hand over a sentence to start with. */
    fun scaffoldLine(scenario: String, asked: Set<String>, starter: String? = null): String {
        val hint = starter
            ?.trim()
            ?.trimEnd('.', '!', '?')
            ?.take(70)
            ?.takeIf { it.isNotBlank() }
            ?.let { "You can start with: $it." }
            ?: "You can start with: I think it was interesting."
        return "$hint ${freshQuestion(asked, scenario)}"
    }

    /**
     * The offline reply when the Space is asleep, private or slow.
     *
     * It is deliberately not a canned paragraph: it uses one word the student
     * actually said, asks one fresh question, and follows the lesson script on
     * the first turn of a step. A lesson that needs the internet is not a lesson
     * a student can rely on.
     */
    fun localReply(
        studentText: String,
        scenario: String,
        asked: Set<String>,
        lessonStep: LessonStep? = null,
        turnsInStep: Int = 0,
        wrapUp: Boolean = false
    ): ShapedReply {
        if (wrapUp) {
            return shapeReply(
                "That is the end of this lesson. What shall we talk about now?",
                asked
            )
        }
        val teacherLine = lessonStep?.teacherLine
        if (!teacherLine.isNullOrBlank() && turnsInStep == 0) {
            return shapeReply(teacherLine, asked)
        }
        val w = words(studentText)
        val anchor = contentAnchor(w)
        val reaction = when {
            w.size < 3 -> "Tell me a little more, in one full sentence."
            anchor != null -> "You mentioned $anchor."
            else -> "I heard you."
        }
        return shapeReply("$reaction ${freshQuestion(asked, scenario)}", asked)
    }

    /** Normalised question text, used to stop the tutor asking twice. */
    fun normaliseQuestion(text: String): String =
        text.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s']"), " ")
            .replace(WHITESPACE, " ")
            .trim()
            .removePrefix("so ")
            .removePrefix("and ")
            .removePrefix("well ")
            .trim()

    // ── corrections ────────────────────────────────────────────────────────

    fun feedbackMode(scenario: String, stepName: String?, drillActive: Boolean = false): FeedbackMode =
        when {
            drillActive -> FeedbackMode.ACCURACY
            scenario.equals("Grammar Doctor", ignoreCase = true) -> FeedbackMode.ACCURACY
            stepName?.equals("Practise", ignoreCase = true) == true -> FeedbackMode.ACCURACY
            else -> FeedbackMode.FLUENCY
        }

    /** Explicit cards per call. Fluency work gets one; a drill gets three. */
    fun maxCorrections(mode: FeedbackMode): Int =
        if (mode == FeedbackMode.ACCURACY) 3 else 1

    /** A better word is encouragement, so one per call is plenty. */
    fun maxUpgrades(): Int = 1

    /**
     * Is this transcript reliable enough to be corrected at all?
     *
     * Speech-to-text is measurably worse on accented, second-language speech.
     * A one-word fragment, a doubled word from a restarted recogniser session or
     * a 25-letter token is far more likely to be a transcription artefact than a
     * grammar mistake. Correcting it teaches the wrong thing and, worse, tells a
     * student they were wrong when they were not.
     */
    fun isUnreliableTranscript(text: String): Boolean {
        val w = words(text)
        if (w.size < 3) return true
        if (w.any { it.length > 18 }) return true
        for (i in 1 until w.size) {
            if (w[i].equals(w[i - 1], ignoreCase = true) && w[i].length >= 4) return true
        }
        return false
    }

    fun canCorrect(text: String, mode: FeedbackMode): Boolean {
        val w = words(text)
        if (w.size < 3 || isUnreliableTranscript(text)) return false
        return mode == FeedbackMode.ACCURACY || w.size >= 5
    }

    /**
     * Is this a minimal edit of the student's own words, or a different sentence?
     *
     * Models often put their reply, a paraphrase, or a punctuation-only copy on
     * the FIX line. A real correction keeps most of the words and changes only a
     * few of them.
     */
    fun plausibleFix(original: String, corrected: String): Boolean {
        val a = words(original)
        val b = words(corrected)
        if (a.isEmpty() || b.isEmpty() || a == b) return false
        val shared = a.count { it in b }
        if (shared * 2 < a.size) return false
        if (corrected.length > original.length * 1.6 + 8) return false
        return (maxOf(a.size, b.size) - shared) <= 4
    }

    // ── turn-taking ────────────────────────────────────────────────────────

    /**
     * How long a pause ends the turn.
     *
     * A buffer ending in "and", "because" or a two-word fragment is a learner
     * still assembling a sentence in a second language: give them ~4.6 s. A
     * complete-sounding answer gets ~2 s so the call stays lively. This is the
     * cheap, on-device version of semantic endpointing.
     */
    fun endpointDelayMs(buffer: String): Long {
        val w = words(buffer)
        if (w.isEmpty()) return 2_000L
        return if (w.last() in DANGLING_ENDS || w.size < 4) 4_600L else 2_000L
    }

    /**
     * Silence is measured in seconds, not in failed recogniser sessions.
     *
     * Wait-time research puts useful thinking time at 3–5 s minimum; AI tutor
     * reviews show that filling ten seconds of silence with encouragement
     * replaces the learner's retrieval practice with a demonstration. So: stay
     * quiet for ~14 s, then narrow the question, then offer a sentence starter,
     * and only hang up after a genuinely long silence.
     */
    fun silenceAction(elapsedMs: Long, nudgesShown: Int): SilenceAction = when {
        nudgesShown == 0 && elapsedMs >= 14_000L -> SilenceAction.EASIER_QUESTION
        nudgesShown == 1 && elapsedMs >= 30_000L -> SilenceAction.SENTENCE_STARTER
        nudgesShown >= 3 && elapsedMs >= 45_000L -> SilenceAction.END_CALL
        nudgesShown >= 2 && elapsedMs >= 60_000L -> SilenceAction.END_CALL
        else -> SilenceAction.SILENT_RETRY
    }

    // ── lessons and progress ───────────────────────────────────────────────

    /**
     * A step is earned by an attempt, not by a turn counter.
     *
     * The tutor's reply belongs to the step the student is *moving into*, so the
     * target index is what the system prompt and the offline script use. Two
     * one-word attempts get a scaffold and then a move, so a shy student is
     * never stuck on step 1 for a whole call.
     */
    fun lessonProgress(
        currentIndex: Int,
        stepCount: Int,
        studentWords: Int,
        turnsInStep: Int,
        alreadyComplete: Boolean
    ): LessonProgress {
        if (stepCount <= 0 || alreadyComplete) {
            return LessonProgress(currentIndex.coerceAtLeast(0), alreadyComplete, false)
        }
        val ready = studentWords >= 4 || turnsInStep >= 2
        if (!ready) return LessonProgress(currentIndex, false, false)
        if (currentIndex >= stepCount - 1) {
            return LessonProgress(currentIndex, true, false)
        }
        return LessonProgress(currentIndex + 1, false, true)
    }

    /**
     * Clean *turns*, not distinct sentences: repeating a mistake counts. The
     * caller de-duplicates by turn index before passing the wrong-turn count.
     */
    fun cleanPercent(turns: Int, wrongTurns: Int): Int {
        if (turns <= 0) return 0
        return ((turns - wrongTurns.coerceIn(0, turns)).coerceAtLeast(0) * 100) / turns
    }

    /** +1 = evidence to move up, -1 = evidence to move down, 0 = stay. */
    fun levelTrend(evidence: CallEvidence): Int = when {
        evidence.turns >= 6 &&
            evidence.cleanPercent >= 80 &&
            evidence.avgWords >= 9.0 &&
            evidence.usedComplex -> 1
        evidence.turns >= 4 &&
            evidence.cleanPercent < 50 &&
            evidence.avgWords < 6.0 -> -1
        else -> 0
    }

    // ── small helpers ──────────────────────────────────────────────────────

    fun wordCount(text: String): Int = words(text).size

    private fun words(text: String): List<String> =
        text.lowercase(Locale.US)
            .split(WORD_SPLIT)
            .filter { it.isNotBlank() }

    private fun isGenericPraise(sentence: String): Boolean =
        wordCount(sentence) <= 14 && PRAISE.containsMatchIn(sentence)

    private fun trimToOneQuestion(sentence: String): String {
        val index = sentence.indexOf('?')
        val text = if (index >= 0) sentence.substring(0, index + 1) else sentence
        return text.trim().trimEnd('.', ',', ';', ':').trim().let {
            if (it.endsWith("?")) it else "$it?"
        }
    }

    private fun trimToOneSentence(sentence: String): String {
        val index = sentence.indexOfFirst { it == '.' || it == '!' }
        val text = if (index >= 0) sentence.substring(0, index + 1) else sentence
        return text.trim().trimEnd(',', ';', ':').trim().let {
            if (it.endsWith('.') || it.endsWith('!')) it else "$it."
        }
    }

    private fun contentAnchor(words: List<String>): String? =
        words.lastOrNull { it.length > 3 && it !in ANCHOR_STOP_WORDS }?.lowercase(Locale.US)

    private fun scenarioQuestions(scenario: String): List<String> = when {
        scenario.contains("Coffee", true) -> listOf(
            "What would you like to drink?",
            "Have you been to that cafe before?",
            "What did you order?"
        )
        scenario.contains("Interview", true) -> listOf(
            "What did you learn from that?",
            "Can you give me one example from your experience?",
            "What would you like to ask me about the role?"
        )
        scenario.contains("Travel", true) -> listOf(
            "How long did the journey take?",
            "Where did you go after that?",
            "What was the best part of the trip?"
        )
        scenario.contains("Grammar", true) -> listOf(
            "Can you say that sentence again more slowly?",
            "Which part of that sentence are you not sure about?",
            "Can you make another sentence with the same rule?"
        )
        scenario.contains("Exam", true) -> listOf(
            "Can you give me one reason for that?",
            "What example would you use in the exam?",
            "How would you conclude that answer?"
        )
        else -> emptyList()
    }

    private fun cleanForSpeech(raw: String): String {
        val withoutThink = THINK_BLOCK.replace(raw, " ").let { THINK_TAG.replace(it, " ") }
        val lines = withoutThink.lines().map { line ->
            val bare = LEADING_MARKUP.replace(line.trim(), "")
            LABEL_LINE.replace(bare, "$2")
        }
        val joined = lines.joinToString(" ")
        return TutorSpeech.cleanForSpeech(STAGE_DIRECTIONS.replace(joined, " "))
            .replace(WHITESPACE, " ")
            .trim()
    }
}
