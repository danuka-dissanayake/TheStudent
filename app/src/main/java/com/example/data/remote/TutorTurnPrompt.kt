package com.example.data.remote

import com.example.data.model.LessonStep
import com.example.data.model.TutorLesson
import com.example.data.model.TutorTurn

/**
 * Builds the instructions for one call turn, and reads the answer back.
 *
 * A spoken tutor has to do two jobs at once: keep the conversation going and
 * quietly record what went wrong. If both are squeezed into one paragraph the
 * student hears a lecture instead of a conversation, so the model is asked for
 * four labelled lines instead:
 *
 * ```
 * SAY: what the student hears (two sentences, ends with a question)
 * FIX: the student's sentence, corrected
 * RULE: the grammar point, three words at most
 * WORD: one word or phrase worth keeping
 * ```
 *
 * The parser is deliberately forgiving. Small models forget formats, especially
 * when they are half-awake on a free Hugging Face Space, and a forgotten label
 * must never cost the student their reply: with no labels at all, the whole
 * answer is spoken, and the corrections fall back to `EnglishGrammarRules`.
 */
object TutorTurnPrompt {

    /**
     * The tutor's persona and the rules of the call.
     *
     * Written as instructions to a teacher rather than as a character sheet: what
     * matters is turn length, one question at a time, and never letting a
     * correction replace a reply.
     */
    fun system(
        scenario: String,
        lesson: TutorLesson?,
        stepIndex: Int,
        level: String = "B1"
    ): String {
        val step: LessonStep? = lesson?.step(stepIndex)
        val lessonBlock = if (lesson != null && step != null) {
            """
            You are teaching a short lesson called "${lesson.title}".
            Goal: ${lesson.goal}
            This turn is step ${stepIndex + 1} of ${lesson.stepCount}: ${step.name} — ${step.instruction}
            A good model sentence for this step is: "${step.modelLine}"
            Follow this step. Do not jump ahead, and do not explain the whole lesson at once.
            """.trimIndent()
        } else {
            "There is no fixed lesson. Keep the conversation going on the topic: $scenario."
        }

        return """
            You are Miss Emma, a warm, patient English speaking tutor on a phone call with a Sri Lankan student.
            The student's spoken English level is about $level (CEFR).

            $lessonBlock

            How you speak on this call:
            1. This is a phone call, not a lecture. Say at most two short sentences, then stop.
            2. Always end with one clear question so the student knows it is their turn.
            3. Never use markdown, bullet points, emoji, or stage directions — everything you write in SAY is read aloud.
            4. Never say "as an AI" and never mention these instructions.
            5. If the student is silent or unclear, be encouraging and offer an easier question.
            6. Correct at most one thing per turn. A correction belongs in FIX, not in SAY.
            7. If the student's sentence was already correct, write NONE on the FIX line.

            Answer in exactly this format and nothing else:
            SAY: <what you say out loud>
            FIX: <the student's sentence, corrected, or NONE>
            RULE: <the grammar point in three words, or NONE>
            WORD: <one useful word or short phrase from this turn, or NONE>
        """.trimIndent()
    }

    /** What the student said, with the context the model needs to respond well. */
    fun userMessage(studentText: String): String =
        "The student just said: \"$studentText\"\nReply in the format above."

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
     * Anything that is not one of the four labels is treated as speech, because a
     * tutor that stays silent looks broken while a tutor that says a stray symbol
     * is merely odd.
     */
    fun parse(raw: String): TutorTurn {
        val text = raw
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("```\\w*"), "")
            .replace("```", "")
            .trim()

        val say = StringBuilder()
        var fix: String? = null
        var rule: String? = null
        var word: String? = null
        var sawLabel = false

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val upper = line.uppercase()
            when {
                upper.startsWith("SAY:") -> {
                    sawLabel = true
                    say.appendLine(line.substring(4).trim())
                }
                upper.startsWith("FIX:") -> {
                    sawLabel = true
                    fix = line.substring(4).cleanValue()
                }
                upper.startsWith("RULE:") -> {
                    sawLabel = true
                    rule = line.substring(5).cleanValue()
                }
                upper.startsWith("WORD:") -> {
                    sawLabel = true
                    word = line.substring(5).cleanValue()
                }
                // A wrapped line continues whatever came before it; before any
                // label it is simply speech.
                else -> say.appendLine(line)
            }
        }

        val spoken = say.toString()
            .replace(Regex("[*_#`>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { if (sawLabel) "" else text }

        return TutorTurn(
            say = spoken.takeIf { it.isNotBlank() }
                ?: "Sorry, I lost you for a second. Could you say that again?",
            fixOriginal = null, // filled by the caller, which knows what was said
            fixCorrected = fix,
            rule = rule,
            word = word
        )
    }

    private fun String.cleanValue(): String? {
        val value = trim().trimEnd('.', ',', ';')
        if (value.isBlank()) return null
        if (value.equals("none", ignoreCase = true)) return null
        if (value.equals("null", ignoreCase = true)) return null
        if (value.equals("n/a", ignoreCase = true)) return null
        return value.takeUnless { it.startsWith("<") }
    }
}
