package com.example.data.remote

import com.example.data.model.LessonStep
import com.example.data.model.TutorLesson
import com.example.data.model.TutorTurn
import com.example.data.voice.TutorCoach

/**
 * Builds the instructions for one call turn, and reads the answer back.
 *
 * A spoken tutor has two jobs in the same answer: keep the conversation going
 * and quietly record what went wrong. If both are squeezed into one paragraph
 * the student hears a lecture instead of a conversation, so the model is asked
 * for five labelled lines:
 *
 * ```
 * SAY:  what the student hears (one or two sentences, one question)
 * FIX:  the student's own sentence, minimally corrected, or NONE
 * RULE: the grammar point in three words, or NONE
 * WORD: a plain phrase the student used => a stronger phrase, or NONE
 * NOTE: one safe fact worth remembering, or NONE
 * ```
 *
 * The instructions are written as a teaching contract, not a character sheet.
 * `docs/AI_TUTOR_RESEARCH.md` explains each rule: short contingent turns, one
 * question, no generic praise, CEFR-graded words, selective correction
 * (recasts for fluency, explicit fixes for drills), self-repair first, and a
 * strict ban on repeating a question. The ViewModel applies
 * [TutorCoach.shapeReply] to the result as a second line of defence.
 *
 * The parser is deliberately forgiving, but it is now **stateful**: a wrapped
 * line continues the label above it. The old line-by-line parser put a wrapped
 * `WORD:` or `NOTE:` continuation into `SAY` and read it aloud.
 */
object TutorTurnPrompt {

    private enum class Label { SAY, FIX, RULE, WORD, NOTE }

    private val LABEL_LINE = Regex(
        "^(say|fix|rule|word|note)\\s*[:\\-—]\\s*(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val MARKUP = Regex("[*_#`]+")
    private val THINK_BLOCK = Regex("<think>[\\s\\S]*?</think>")

    /**
     * The tutor's persona and the rules of the call.
     */
    fun system(
        scenario: String,
        lesson: TutorLesson?,
        stepIndex: Int,
        level: String = "B1",
        /** A rule the student has broken more than once on this call. */
        focusRule: String = "",
        /** Stable facts from earlier calls, so this is not a stranger's form. */
        knownFacts: List<String> = emptyList(),
        /** Normalised questions already asked; the tutor must not repeat one. */
        askedQuestions: List<String> = emptyList(),
        mode: TutorCoach.FeedbackMode = TutorCoach.FeedbackMode.FLUENCY,
        /** The lesson's last step was earned: wrap up and move to free talk. */
        wrapUp: Boolean = false
    ): String {
        val step: LessonStep? = lesson?.step(stepIndex)
        val lessonBlock = when {
            wrapUp ->
                "The guided lesson is now complete. Say one warm sentence about what the student practised, " +
                    "then ask one new question to move into free conversation. Do not start another step."
            lesson != null && step != null ->
                """
                You are teaching a short lesson called "${lesson.title}".
                Goal: ${lesson.goal}
                The tutor's next line opens step ${stepIndex + 1} of ${lesson.stepCount}: ${step.name} — ${step.instruction}
                A good model sentence for this step is: "${step.modelLine}".
                Follow this step. Do not jump ahead and do not explain the whole lesson at once.
                """.trimIndent()
            else ->
                "There is no fixed lesson. Keep the conversation going on the topic: $scenario."
        }

        val memoryBlock = if (knownFacts.isEmpty()) {
            ""
        } else {
            """
            You remember these things about this student from earlier calls: ${knownFacts.joinToString("; ")}.
            Bring one of them up only if it fits naturally — one, never a list.
            """.trimIndent()
        }

        val askedBlock = if (askedQuestions.isEmpty()) {
            ""
        } else {
            "Questions already asked in this call (do not repeat any of them, even reworded): " +
                askedQuestions.takeLast(8).joinToString(" | ")
        }

        val focusBlock = if (focusRule.isBlank()) {
            ""
        } else {
            """
            This student has now made the same mistake twice on this call: $focusRule.
            Do not lecture and do not put this in SAY. When it fits, ask one question that makes them use the correct form once.
            """.trimIndent()
        }

        val correctionBlock = if (mode == TutorCoach.FeedbackMode.ACCURACY) {
            """
            CORRECTION MODE: focused form practice.
            - Put the minimal corrected version of the student's OWN sentence on FIX.
            - Do NOT put the correction inside SAY. In SAY, briefly invite them to say that sentence once more, then ask the next question.
            """.trimIndent()
        } else {
            """
            CORRECTION MODE: fluency conversation.
            - If there is a small grammar mistake, quietly use the correct form inside SAY (a recast) while replying to the meaning.
            - Never say "mistake", "wrong", "better", "you should say", and never explain the rule in SAY.
            - Write NONE on FIX unless the error makes the sentence hard to understand.
            """.trimIndent()
        }

        return """
            You are Miss Emma, a warm, patient English speaking tutor on a phone call with a Sri Lankan student (CEFR $level). Your job is to make the STUDENT speak.

            $lessonBlock
            $memoryBlock
            $askedBlock
            $focusBlock
            $correctionBlock

            SAY: one or two short sentences (max 30 words) ending in exactly ONE question. Never answer your own question. Use one detail from what the student said; never open with praise such as "Great job". Use everyday $level words: no idioms, lists, markdown, brackets, stage directions or "as an AI". Never repeat a question from the asked list. For a one-word answer, ask an easy either/or question.
            FIX: a minimal edit of the student's OWN sentence — keep their words and meaning — or NONE when it is correct, when only punctuation differs, or when you would have to rewrite it. RULE: the point in three words, or NONE when FIX is NONE.
            WORD: only a plain phrase the student actually said => a genuinely stronger alternative, or NONE. Never rewrite a correct sentence.
            NOTE: one short, stable, NON-sensitive fact (name, town, exam, hobby, course, job goal), or NONE. Never health, religion, address, phone number or facts about other people. Never invent one.
            One correction per turn at most. Everything on SAY is read aloud; FIX, RULE, WORD and NOTE are silent.

            Answer in exactly these five lines and nothing else:
            SAY: <what you say out loud>
            FIX: <the student's own sentence with the smallest possible correction, or NONE>
            RULE: <the grammar point in three words, or NONE>
            WORD: <plain phrase => better phrase, or NONE>
            NOTE: <one safe fact about the student, or NONE>
        """.trimIndent()
    }

    /** What the student said, with the context the model needs to respond well. */
    fun userMessage(studentText: String): String =
        "The student just said: <<<$studentText>>>\n" +
            "FIX must be built from those exact words only. Reply in the format above."

    /** The opening line of a call, when there is no model answer yet. */
    fun openerMessage(scenario: String, lesson: TutorLesson?, stepIndex: Int): String =
        if (lesson != null) {
            "Start the call. Greet the student by name in one short sentence, then begin step " +
                "${stepIndex + 1}: ${lesson.step(stepIndex)?.instruction.orEmpty()}"
        } else {
            "Start the call on the topic \"$scenario\". Greet the student in one short sentence " +
                "and ask one easy opening question."
        }

    /**
     * Read the model's answer.
     *
     * Labels may be bold, lower-case, wrapped over two lines or missing entirely.
     * A wrapped line continues its label (so a long NOTE is not spoken aloud), and
     * an answer with no labels at all is treated as speech so the student is
     * never left with a silent tutor.
     */
    fun parse(raw: String): TutorTurn {
        val text = THINK_BLOCK.replace(raw, "")
            .replace(Regex("```\\w*"), "")
            .replace("```", "")
            .trim()

        val buffers = Label.values().toList().associateWith { StringBuilder() }
        var current = Label.SAY
        var sawLabel = false

        fun add(label: Label, value: String) {
            if (value.isNotBlank()) buffers.getValue(label).append(value).append(' ')
        }

        text.lines().forEach { rawLine ->
            val line = MARKUP.replace(rawLine.trim(), "")
                .trimStart('-', '>', ' ')
                .trim()
            if (line.isEmpty()) return@forEach
            val match = LABEL_LINE.find(line)
            if (match != null) {
                current = when (match.groupValues[1].lowercase()) {
                    "fix" -> Label.FIX
                    "rule" -> Label.RULE
                    "word" -> Label.WORD
                    "note" -> Label.NOTE
                    else -> Label.SAY
                }
                sawLabel = true
                add(current, match.groupValues[2].trim())
            } else {
                add(current, line)
            }
        }

        val value = { label: Label -> buffers.getValue(label).toString().trim().cleanValue() }
        val spoken = buffers.getValue(Label.SAY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { if (sawLabel) "" else text }

        val wordRaw = value(Label.WORD)
        val arrow = wordRaw?.let { v ->
            when {
                v.contains("=>") -> v.split("=>", limit = 2)
                v.contains("->") -> v.split("->", limit = 2)
                else -> null
            }
        }
        val upgradeFrom = arrow?.getOrNull(0)?.trim()?.trim('"')?.ifBlank { null }
        val upgradeTo = arrow?.getOrNull(1)?.trim()?.trim('"')?.ifBlank { null }

        return TutorTurn(
            say = spoken.takeIf { it.isNotBlank() }
                ?: "Sorry, I lost you for a second. Could you say that again?",
            fixOriginal = null, // filled by the caller, which knows what was said
            fixCorrected = value(Label.FIX),
            rule = value(Label.RULE),
            word = upgradeTo ?: wordRaw,
            upgradeFrom = upgradeFrom,
            upgradeTo = upgradeTo,
            note = value(Label.NOTE)
        )
    }

    private fun String.cleanValue(): String? {
        val value = trim().trim('"', '\'').trimEnd(',', ';', ':').trim()
        val bare = value.trimEnd('.').trim()
        if (bare.isBlank()) return null
        if (bare.equals("none", ignoreCase = true)) return null
        if (bare.equals("null", ignoreCase = true)) return null
        if (bare.equals("n/a", ignoreCase = true)) return null
        return value.takeUnless { it.startsWith("<") }
    }
}
