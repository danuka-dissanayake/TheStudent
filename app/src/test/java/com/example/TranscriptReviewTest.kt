package com.example

import com.example.data.voice.TranscriptReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The STT decisions are pure logic, so they are checked without a phone. */
class TranscriptReviewTest {

    private fun verdict(vararg guesses: Pair<String, Float>, hints: List<String> = emptyList()) =
        TranscriptReview.choose(
            guesses.map { TranscriptReview.Candidate(it.first, it.second) },
            hints
        )

    @Test
    fun `a lesson word wins without inventing a new sentence`() {
        val result = verdict(
            "I went to the temple" to -1f,
            "I went to the Temple of the Tooth" to -1f,
            hints = listOf("Temple of the Tooth", "Kandy")
        )
        assertEquals("I went to the Temple of the Tooth", result.text)
        assertFalse(result.needsConfirmation)
    }

    @Test
    fun `two guesses disagreeing on a short word ask the student`() {
        val result = verdict(
            "Yes I did go to Kandy" to -1f,
            "Yes I do go to Kandy" to -1f
        )
        assertTrue(result.needsConfirmation)
        assertTrue(result.uncertainWords.contains("did") || result.uncertainWords.contains("do"))
        assertTrue(result.reason.isNotBlank())
    }

    @Test
    fun `a low confidence score asks the student`() {
        val result = verdict("I have been to Kandy" to 0.31f)
        assertTrue(result.needsConfirmation)
        assertEquals("I wasn't sure I caught every word.", result.reason)
    }

    @Test
    fun `an engine with no confidence score does not nag every turn`() {
        val result = verdict("I have been to Kandy" to -1f)
        assertFalse(result.needsConfirmation)
        assertEquals("I have been to Kandy", result.text)
    }

    @Test
    fun `a disagreement about a long content word is not a short-word warning`() {
        val result = verdict(
            "I went to the beach yesterday" to -1f,
            "I went to the market yesterday" to -1f
        )
        assertFalse(result.needsConfirmation)
        assertTrue(result.uncertainWords.isEmpty())
    }

    @Test
    fun `an empty result is not sent`() {
        val result = TranscriptReview.choose(emptyList())
        assertEquals("", result.text)
        assertFalse(result.needsConfirmation)
    }

    @Test
    fun `a restarted session drops a shared tail exactly once`() {
        assertEquals(
            "I went to the beach",
            TranscriptReview.mergeSegments("I went to the", "to the beach")
        )
    }

    @Test
    fun `a cumulative final replaces instead of duplicating the whole turn`() {
        assertEquals(
            "I went to the beach and I ate lunch.",
            TranscriptReview.mergeSegments("I went to the", "I went to the beach and I ate lunch.")
        )
    }

    @Test
    fun `deliberate repetition is speech, not a stitching artefact`() {
        assertEquals(
            "No no no I did not",
            TranscriptReview.mergeSegments("No", "No no no I did not")
        )
    }

    @Test
    fun `a distinctive single word overlap is still removed`() {
        assertEquals(
            "I visited Kandy last year",
            TranscriptReview.mergeSegments("I visited Kandy", "Kandy last year")
        )
    }

    @Test
    fun `a one-word conjunction tail is not doubled`() {
        assertEquals(
            "I went to the shop and then I came home",
            TranscriptReview.mergeSegments("I went to the shop and", "and then I came home")
        )
    }

    @Test
    fun `case and punctuation do not hide an overlap`() {
        assertEquals(
            "I said, to the shop.",
            TranscriptReview.mergeSegments("I said, to the", "To the shop.")
        )
    }

    @Test
    fun `empty sides are harmless`() {
        assertEquals("Hello", TranscriptReview.mergeSegments("", "Hello"))
        assertEquals("Hello", TranscriptReview.mergeSegments("Hello", ""))
    }

    @Test
    fun `hint list puts the student's terms first and drops boilerplate`() {
        val hints = TranscriptReview.hintWords(
            sources = listOf("What did you do yesterday? I went to the beach."),
            extraTerms = listOf("Kandy", "Temple of the Tooth")
        )
        assertEquals("Kandy", hints[0])
        assertEquals("Temple of the Tooth", hints[1])
        assertFalse(hints.contains("what"))
        assertFalse(hints.contains("yesterday"))
        assertTrue(hints.contains("beach"))
        assertTrue(hints.size <= 36)
    }
}
