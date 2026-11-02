package com.example

import com.example.data.remote.TutorTurnPrompt
import com.example.data.voice.TutorCoach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The labelled answer format must never leak into what the tutor reads aloud. */
class TutorTurnPromptTest {

    @Test
    fun `wrapped lines continue their own label instead of being spoken`() {
        val turn = TutorTurnPrompt.parse(
            """
            SAY: You went to the beach yesterday. What was the weather like?
            FIX: NONE
            RULE: NONE
            WORD: very good => excellent
            for your trip
            NOTE: I have an exam
            on Tuesday
            """.trimIndent()
        )
        assertTrue(turn.say.contains("weather"))
        assertFalse(turn.say.contains("for your trip", ignoreCase = true))
        assertFalse(turn.say.contains("NOTE", ignoreCase = true))
        assertNull(turn.fixCorrected)
        assertEquals("I have an exam on Tuesday", turn.note)
    }

    @Test
    fun `an answer with no labels is treated as speech`() {
        val turn = TutorTurnPrompt.parse("Hello there, what did you do today?")
        assertTrue(turn.say.contains("Hello"))
        assertNull(turn.fixCorrected)
    }

    @Test
    fun `markdown around the labels is ignored`() {
        val turn = TutorTurnPrompt.parse(
            "**SAY:** Hi there, how are you?\n" +
                "**FIX:** NONE\n**RULE:** NONE\n**WORD:** NONE\n**NOTE:** NONE"
        )
        assertTrue(turn.say.contains("Hi there"))
        assertFalse(turn.say.contains("**"))
        assertNull(turn.fixCorrected)
        assertNull(turn.note)
    }

    @Test
    fun `none is never a correction or a remembered fact`() {
        val turn = TutorTurnPrompt.parse(
            "SAY: How was your weekend?\nFIX: NONE.\nRULE: none\nWORD: N/A\nNOTE: null"
        )
        assertNull(turn.fixCorrected)
        assertNull(turn.rule)
        assertNull(turn.note)
    }

    @Test
    fun `the system prompt carries the teaching contract`() {
        val prompt = TutorTurnPrompt.system(
            scenario = "Free Conversation",
            lesson = null,
            stepIndex = 0,
            level = "B1",
            askedQuestions = listOf("what happened next"),
            mode = TutorCoach.FeedbackMode.ACCURACY
        )
        assertTrue(prompt.contains("exactly ONE question"))
        assertTrue(prompt.contains("what happened next"))
        assertTrue(prompt.contains("minimal edit"))
        assertTrue(prompt.contains("CORRECTION MODE: focused form practice"))
    }

    @Test
    fun `an untrusted transcript switches correction off`() {
        val prompt = TutorTurnPrompt.system(
            scenario = "Free Conversation",
            lesson = null,
            stepIndex = 0,
            trustTranscript = false
        )
        assertTrue(prompt.contains("MICROPHONE MAY HAVE MISHEARD"))
        assertTrue(prompt.contains("Do not recast"))
        assertFalse(
            TutorTurnPrompt.system(
                scenario = "Free Conversation",
                lesson = null,
                stepIndex = 0
            ).contains("MICROPHONE MAY HAVE MISHEARD")
        )
    }
}
