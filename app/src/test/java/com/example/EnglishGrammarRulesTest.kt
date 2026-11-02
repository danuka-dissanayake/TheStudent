package com.example

import com.example.data.remote.EnglishGrammarRules
import com.example.data.voice.WordUpgrades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A correction shown for correct English is worse than no correction at all.
 * These cases were all real false positives (or missed punctuation) in the
 * shipped rules.
 */
class EnglishGrammarRulesTest {

    @Test
    fun `stop requests and backchannels survive punctuation`() {
        assertTrue(EnglishGrammarRules.isStopRequest("Wait."))
        assertTrue(EnglishGrammarRules.isStopRequest("Hold on."))
        assertTrue(EnglishGrammarRules.isBackchannel("Okay."))
        assertTrue(EnglishGrammarRules.isBackchannel("Yeah."))
    }

    @Test
    fun `perfect tense is not turned into nonsense`() {
        val fix = EnglishGrammarRules.check("I have went to school yesterday.")
        assertTrue(fix != null)
        assertTrue(fix!!.corrected.contains("gone"))
        assertFalse(fix.corrected.contains("had went"))
    }

    @Test
    fun `correct sentences are left alone`() {
        assertNull(EnglishGrammarRules.check("The weather is good in the morning."))
        assertNull(EnglishGrammarRules.check("Do you know about the exam?"))
        assertNull(EnglishGrammarRules.check("She likes tea in the evening."))
    }

    @Test
    fun `a and an use sound, not just the first letter`() {
        val fix = EnglishGrammarRules.check("I waited for a hour in the rain.")
        assertTrue(fix != null)
        assertTrue(fix!!.corrected.contains("an hour"))
    }

    @Test
    fun `third person singular is still corrected`() {
        val fix = EnglishGrammarRules.check("She don't like tea in the morning.")
        assertTrue(fix != null)
        assertTrue(fix!!.corrected.contains("doesn't"))
    }

    @Test
    fun `single words are not upgraded into yellow cards`() {
        assertNull(WordUpgrades.suggest("That is good."))
        assertEquals(
            "excellent",
            WordUpgrades.suggest("That is a very good idea.")?.second
        )
    }
}
