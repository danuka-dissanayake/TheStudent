package com.example.data.remote

data class VoiceTutorResponse(
    val spokenText: String,
    val grammarTip: String? = null,
    val suggestedReplyPills: List<String> = emptyList(),
    val vocabularyHighlight: String? = null
)

object EnglishVoiceTutorEngine {

    val scenarios = listOf(
        "Free Conversation",
        "Coffee Shop & Cafe",
        "Job Interview",
        "Travel & Airport",
        "Grammar Doctor",
        "A/L Exam Speaking"
    )

    fun getStarterGreeting(scenario: String): VoiceTutorResponse {
        return when (scenario) {
            "Coffee Shop & Cafe" -> VoiceTutorResponse(
                spokenText = "Welcome to the Green Leaf Cafe! What can I get started for you today?",
                grammarTip = "Tip: Use 'I'd like...' or 'Could I please have...' for polite ordering.",
                suggestedReplyPills = listOf("I'd like an iced latte, please.", "What pastries do you recommend?", "Can I see the drink menu?"),
                vocabularyHighlight = "Pastry / Decaf / Barista"
            )
            "Job Interview" -> VoiceTutorResponse(
                spokenText = "Good morning! Thank you for joining us today. To start off, could you tell me a little bit about yourself and your background?",
                grammarTip = "Tip: Use present perfect ('I have studied...') and past simple ('I graduated...') correctly.",
                suggestedReplyPills = listOf("I recently graduated with a focus on...", "I have two years of experience in...", "I am very passionate about teamwork."),
                vocabularyHighlight = "Strengths / Qualifications / Experience"
            )
            "Travel & Airport" -> VoiceTutorResponse(
                spokenText = "Hello traveler! May I please see your passport and boarding pass before you proceed to gate 4?",
                grammarTip = "Tip: When answering airport personnel, clear and concise sentences are best.",
                suggestedReplyPills = listOf("Here is my passport and boarding pass.", "Which terminal is flight UL504?", "Is this carry-on luggage allowed?"),
                vocabularyHighlight = "Boarding Pass / Gate / Baggage"
            )
            "Grammar Doctor" -> VoiceTutorResponse(
                spokenText = "Hello! I am your English Grammar Doctor. Speak any sentence you'd like to check, and I will examine your grammar and pronunciation!",
                grammarTip = "Say something like 'I have went to Kandy yesterday' to see how I fix it.",
                suggestedReplyPills = listOf("Did you went to school yesterday?", "She don't like spicy food.", "Between you and me, I am confident."),
                vocabularyHighlight = "Subject-Verb Agreement / Tenses"
            )
            "A/L Exam Speaking" -> VoiceTutorResponse(
                spokenText = "Ayubowan! Let's practice for the G.C.E. Advanced Level General English oral exam. What are your views on the importance of renewable energy in Sri Lanka?",
                grammarTip = "Tip: Structure your thoughts: 'Firstly...', 'Moreover...', 'In conclusion...'",
                suggestedReplyPills = listOf("In my opinion, solar power is vital...", "Sri Lanka has great potential for hydro energy.", "I believe government investment is essential."),
                vocabularyHighlight = "Renewable / Sustainability / Feasible"
            )
            else -> VoiceTutorResponse(
                spokenText = "Hello! I am your English Voice Tutor. I am excited to talk with you! How has your day been so far?",
                grammarTip = "Tip: Speak naturally into your microphone whenever you're ready.",
                suggestedReplyPills = listOf("My day has been really productive!", "I'm practicing English for my exams.", "Can you help me improve my vocabulary?"),
                vocabularyHighlight = "Productive / Fluency / Conversational"
            )
        }
    }

    /**
     * Fallback or local conversational generator when network/token is pending
     */
    fun generateLocalConversationalResponse(userInput: String, scenario: String): VoiceTutorResponse {
        val lower = userInput.lowercase()

        // Check for common grammatical checks
        if (lower.contains("i have went") || lower.contains("did you went") || lower.contains("i didn't saw")) {
            return VoiceTutorResponse(
                spokenText = "That's a very common phrase! Remember that after auxiliary verbs like 'did' or 'have', we use the base or past participle form. Instead of 'did you went', say 'did you go'. Try repeating it: 'Did you go?'",
                grammarTip = "Correction: 'did' + base verb (e.g. 'did you go', 'didn't see').",
                suggestedReplyPills = listOf("Did you go to the library?", "I didn't see that coming!", "Thank you for the correction!"),
                vocabularyHighlight = "Irregular Past Participles"
            )
        }

        if (lower.contains("she don't") || lower.contains("he don't") || lower.contains("it don't")) {
            return VoiceTutorResponse(
                spokenText = "Good try! Remember for third-person singular (he, she, it), we use 'doesn't' instead of 'don't'. So say: 'She doesn't like it.' What else doesn't she like?",
                grammarTip = "Correction: He/She/It + doesn't (does not).",
                suggestedReplyPills = listOf("She doesn't like early mornings.", "He doesn't know the answer.", "Let's practice another rule."),
                vocabularyHighlight = "Third-Person Singular"
            )
        }

        when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                return VoiceTutorResponse(
                    spokenText = "Hello there! It is wonderful to hear your voice. What topic would you like to discuss today? We can talk about daily life, your studies, or practice for an exam.",
                    grammarTip = "You can answer in full sentences to build fluency.",
                    suggestedReplyPills = listOf("Let's talk about my daily routine.", "I want to prepare for an interview.", "Teach me five new idioms."),
                    vocabularyHighlight = "Greetings / Conversational Openers"
                )
            }
            lower.contains("how are you") -> {
                return VoiceTutorResponse(
                    spokenText = "I am doing fantastic, thank you for asking! I'm here and ready to help you speak fluent English with confidence. How are you feeling today?",
                    grammarTip = "Phrases: 'I'm doing great', 'Couldn't be better', 'A bit busy'.",
                    suggestedReplyPills = listOf("I am feeling great today!", "I am a bit tired from studying.", "Ready to learn some new English words!"),
                    vocabularyHighlight = "Enthusiastic / Confident"
                )
            }
            lower.contains("coffee") || lower.contains("latte") || lower.contains("tea") || lower.contains("drink") -> {
                return VoiceTutorResponse(
                    spokenText = "Excellent choice! Would you like that hot or iced, and what size can I get for you — small, medium, or large?",
                    grammarTip = "Adjectives: 'iced', 'steaming hot', 'sugar-free'.",
                    suggestedReplyPills = listOf("I'd prefer a medium iced latte.", "A small hot cappuccino with oat milk, please.", "Do you have any Ceylon tea?"),
                    vocabularyHighlight = "Brew / Beverage / Espresso"
                )
            }
            lower.contains("exam") || lower.contains("study") || lower.contains("a/l") || lower.contains("o/l") -> {
                return VoiceTutorResponse(
                    spokenText = "Studying for exams requires dedication and consistent revision! What subject are you focusing on today, and how do you feel about your English papers?",
                    grammarTip = "Use linking words: 'primarily', 'furthermore', 'nevertheless'.",
                    suggestedReplyPills = listOf("I am studying Combined Mathematics.", "I want to improve my essay writing.", "How can I score an 'A' in General English?"),
                    vocabularyHighlight = "Dedication / Consistent / Revise"
                )
            }
            lower.contains("weather") -> {
                return VoiceTutorResponse(
                    spokenText = "Talking about the weather is one of the best small talk icebreakers in English! What is the weather like outside your window right now?",
                    grammarTip = "Vocabulary: 'sunny', 'humid', 'overcast', 'drizzling'.",
                    suggestedReplyPills = listOf("It is quite sunny and warm.", "It is raining heavily outside.", "The breeze is very pleasant this evening."),
                    vocabularyHighlight = "Humid / Overcast / Breeze"
                )
            }
            lower.contains("job") || lower.contains("interview") || lower.contains("work") -> {
                return VoiceTutorResponse(
                    spokenText = "That's a vital skill! When answering interview questions, use the STAR technique: Situation, Task, Action, and Result. Can you describe a challenge you overcame?",
                    grammarTip = "STAR Method: Structure gives confidence.",
                    suggestedReplyPills = listOf("One challenge I faced was...", "I organized a student workshop...", "My biggest achievement was..."),
                    vocabularyHighlight = "Overcome / Initiative / Accomplishment"
                )
            }
            else -> {
                return VoiceTutorResponse(
                    spokenText = "That is very interesting! You articulated that well. Could you tell me a little more about why that matters to you, or give me an example?",
                    grammarTip = "Great flow! Keep up this natural speaking rhythm.",
                    suggestedReplyPills = listOf("For instance, in my school...", "The main reason is that...", "What do you think about it?"),
                    vocabularyHighlight = "Articulate / Perspective / Elaborate"
                )
            }
        }
    }
}
