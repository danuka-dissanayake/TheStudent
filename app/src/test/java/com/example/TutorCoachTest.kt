package com.example

import com.example.data.voice.TutorCoach
import com.example.data.voice.TutorCoach.CallEvidence
import com.example.data.voice.TutorCoach.FeedbackMode
import com.example.data.voice.TutorCoach.SilenceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the tutor's decision layer.
 *
 * These are the rules that used to be magic numbers scattered through the
 * ViewModel: how long to wait, when a step is earned, whether a correction is
 * believable, and what shape a spoken reply may have. No emulator, no model.
 */
class TutorCoachTest {

    @Test
    fun `praise is dropped and exactly one question survives`() {
        val shaped = TutorCoach.shapeReply(
            "Great job! You went to Kandy. What did you do there?"
        )
        assertFalse(shaped.speech.contains("Great job", ignoreCase = true))
        assertTrue(shaped.speech.contains("Kandy"))
        assertTrue(shaped.speech.endsWith("?"))
        assertEquals(1, shaped.speech.count { it == '?' })
    }

    @Test
    fun `a question already asked is replaced, never repeated`() {
        val asked = setOf(TutorCoach.normaliseQuestion("What did you do there?"))
        val shaped = TutorCoach.shapeReply(
            "You went to Kandy. What did you do there?", asked
        )
        assertFalse(shaped.speech.contains("What did you do there", ignoreCase = true))
        assertTrue(shaped.speech.endsWith("?"))
    }

    @Test
    fun `an empty model answer still gives the student a turn`() {
        val shaped = TutorCoach.shapeReply("")
        assertTrue(shaped.speech.isNotBlank())
        assertTrue(shaped.speech.endsWith("?"))
    }

    @Test
    fun `silence is measured in seconds, not failed recogniser sessions`() {
        assertEquals(SilenceAction.SILENT_RETRY, TutorCoach.silenceAction(5_000L, 0))
        assertEquals(SilenceAction.EASIER_QUESTION, TutorCoach.silenceAction(15_000L, 0))
        assertEquals(SilenceAction.SENTENCE_STARTER, TutorCoach.silenceAction(31_000L, 1))
        assertEquals(SilenceAction.END_CALL, TutorCoach.silenceAction(61_000L, 2))
    }

    @Test
    fun `an unfinished thought gets a longer endpointing pause`() {
        val unfinished = TutorCoach.endpointDelayMs("i went to the shop and")
        val finished = TutorCoach.endpointDelayMs("i went to the shop yesterday")
        assertTrue(unfinished > finished)
        assertTrue(TutorCoach.MAX_TURN_MS >= 30_000L)
    }

    @Test
    fun `a lesson step is earned by an attempt, not by a turn counter`() {
        val tooShort = TutorCoach.lessonProgress(0, 4, 1, 0, false)
        assertEquals(0, tooShort.targetIndex)
        assertFalse(tooShort.advanced)
        assertFalse(tooShort.completed)

        val realAttempt = TutorCoach.lessonProgress(0, 4, 7, 0, false)
        assertEquals(1, realAttempt.targetIndex)
        assertTrue(realAttempt.advanced)

        val scaffolded = TutorCoach.lessonProgress(0, 4, 2, 2, false)
        assertEquals(1, scaffolded.targetIndex)

        val lastStep = TutorCoach.lessonProgress(3, 4, 8, 0, false)
        assertTrue(lastStep.completed)
    }

    @Test
    fun `short and doubled ASR output is never corrected`() {
        assertFalse(TutorCoach.canCorrect("Yes", FeedbackMode.FLUENCY))
        assertFalse(TutorCoach.canCorrect("I went went to the shop", FeedbackMode.FLUENCY))
        assertFalse(TutorCoach.canCorrect("I went home", FeedbackMode.FLUENCY))
        assertTrue(
            TutorCoach.canCorrect(
                "I have went to school yesterday",
                FeedbackMode.ACCURACY
            )
        )
    }

    @Test
    fun `only a minimal edit of the student's own words is a fix`() {
        assertTrue(
            TutorCoach.plausibleFix(
                "I go to the beach yesterday",
                "I went to the beach yesterday"
            )
        )
        assertFalse(
            TutorCoach.plausibleFix(
                "I go shop",
                "Let me tell you a long story about my weekend in Kandy with my cousins"
            )
        )
    }

    @Test
    fun `clean percent counts turns and level changes need a whole call`() {
        assertEquals(80, TutorCoach.cleanPercent(10, 2))
        assertEquals(0, TutorCoach.cleanPercent(0, 0))
        assertEquals(0, TutorCoach.levelTrend(CallEvidence(1, 100, 12.0, true)))
        assertEquals(1, TutorCoach.levelTrend(CallEvidence(8, 90, 10.0, true)))
        assertEquals(-1, TutorCoach.levelTrend(CallEvidence(6, 30, 4.0, false)))
    }

    @Test
    fun `offline reply is short, contingent and ends with a question`() {
        val reply = TutorCoach.localReply(
            studentText = "I visited Kandy with my cousins",
            scenario = "Free Conversation",
            asked = emptySet()
        )
        assertTrue(reply.speech.endsWith("?"))
        assertFalse(reply.speech.contains("very interesting", ignoreCase = true))
        assertTrue(TutorCoach.wordCount(reply.speech) <= 24)
    }

    @Test
    fun `feedback budget depends on the task`() {
        assertEquals(
            FeedbackMode.ACCURACY,
            TutorCoach.feedbackMode("Grammar Doctor", null)
        )
        assertEquals(
            FeedbackMode.ACCURACY,
            TutorCoach.feedbackMode("Free Conversation", "Practise")
        )
        assertEquals(
            FeedbackMode.FLUENCY,
            TutorCoach.feedbackMode("Free Conversation", "Model")
        )
        assertTrue(
            TutorCoach.maxCorrections(FeedbackMode.ACCURACY) >
                TutorCoach.maxCorrections(FeedbackMode.FLUENCY)
        )
    }
}
