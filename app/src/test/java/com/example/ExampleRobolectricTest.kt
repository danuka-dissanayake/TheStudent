package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TheStudent", appName)
  }

  @Test
  fun `verify sample educational courses and past papers exist`() {
    val courses = com.example.data.repository.SampleEducationalData.courses
    val papers = com.example.data.repository.SampleEducationalData.pastPapers
    val vocab = com.example.data.repository.SampleEducationalData.vocabularyWords

    org.junit.Assert.assertTrue(courses.isNotEmpty())
    org.junit.Assert.assertTrue(papers.isNotEmpty())
    org.junit.Assert.assertTrue(vocab.isNotEmpty())

    // Verify Sri Lankan curriculum levels
    val hasSchoolLevel = papers.any { it.level == com.example.data.model.EducationLevel.SCHOOL }
    val hasUndergradLevel = papers.any { it.level == com.example.data.model.EducationLevel.UNDERGRADUATE }
    org.junit.Assert.assertTrue(hasSchoolLevel)
    org.junit.Assert.assertTrue(hasUndergradLevel)
  }

  @Test
  fun `verify AI tutor responses for A L physics and mathematics`() {
    val bernoulliResponse = com.example.data.repository.DzdRepository.generateAiTutorResponse("Explain Bernoulli principle")
    org.junit.Assert.assertTrue(bernoulliResponse.content.contains("Bernoulli", ignoreCase = true))

    val calculusResponse = com.example.data.repository.DzdRepository.generateAiTutorResponse("Integration formulas in calculus")
    org.junit.Assert.assertTrue(calculusResponse.content.contains("LIATE", ignoreCase = true))
  }

  @Test
  fun `verify English Voice Tutor Engine scenarios and grammar corrections`() {
    val cafeStarter = com.example.data.remote.EnglishVoiceTutorEngine.getStarterGreeting("Coffee Shop & Cafe")
    org.junit.Assert.assertTrue(cafeStarter.spokenText.contains("Cafe", ignoreCase = true))
    org.junit.Assert.assertNotNull(cafeStarter.grammarTip)

    val grammarFix = com.example.data.remote.EnglishVoiceTutorEngine.generateLocalConversationalResponse("I have went there yesterday", "Grammar Doctor")
    org.junit.Assert.assertTrue(grammarFix.spokenText.contains("Did you go", ignoreCase = true) || grammarFix.spokenText.contains("go", ignoreCase = true))

    val interviewStarter = com.example.data.remote.EnglishVoiceTutorEngine.getStarterGreeting("Job Interview")
    org.junit.Assert.assertTrue(interviewStarter.spokenText.contains("interview", ignoreCase = true) || interviewStarter.spokenText.contains("background", ignoreCase = true))
  }
}
