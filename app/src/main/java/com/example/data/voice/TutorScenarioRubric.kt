package com.example.data.voice

/**
 * One line of a scenario's scorecard.
 *
 * [label] is what was being tested ("Asked a question back"), [met] is whether
 * the student did it, and [tip] is the sentence to try next time — shown only
 * when it was missed, because a tip for something you already did is clutter.
 */
data class ScenarioCheck(
    val label: String,
    val met: Boolean,
    val tip: String
)

/**
 * The scorecard for a role-play.
 *
 * A scenario without criteria is a vibe: the student talks, the tutor answers,
 * and nothing tells them whether the "job interview" went well. These checks are
 * deliberately local and deterministic — they read the student's own lines, so
 * the scorecard works offline and can never contradict what was actually said.
 *
 * Every check is a *thing a student could have done*, not a grammar rule. Grammar
 * already has its own cards and its own summary section; this answers the
 * question "did I do the task?".
 */
object TutorScenarioRubric {

    fun evaluate(
        scenario: String,
        studentLines: List<String>,
        turns: Int
    ): List<ScenarioCheck> {
        val spoken = studentLines.joinToString(" ").lowercase()
        if (studentLines.isEmpty()) return emptyList()
        return when (scenario) {
            "Job Interview" -> interview(spoken, studentLines)
            "Coffee Shop & Cafe" -> cafe(spoken)
            "Travel & Airport" -> travel(spoken)
            "A/L Exam Speaking" -> exam(spoken, studentLines)
            "Grammar Doctor" -> grammarDoctor(spoken, studentLines)
            else -> freeConversation(spoken, studentLines, turns)
        }
    }

    private fun interview(spoken: String, lines: List<String>): List<ScenarioCheck> = listOf(
        ScenarioCheck(
            label = "Talked about your experience",
            met = has(spoken, "experience", "i worked", "i have worked", "i was working", "my job", "i studied"),
            tip = "Try: \"I worked as a cashier for two years.\""
        ),
        ScenarioCheck(
            label = "Used the past tense",
            met = has(spoken, "worked", "studied", "went", "was", "had", "did", "managed", "joined"),
            tip = "Try: \"I joined the company in 2023.\""
        ),
        ScenarioCheck(
            label = "Described a strength",
            met = has(spoken, "good at", "i can", "i am able", "skilled", "team", "leader", "responsible"),
            tip = "Try: \"I'm good at staying calm under pressure.\""
        ),
        ScenarioCheck(
            label = "Asked a question back",
            met = lines.any { it.contains('?') || it.trim().lowercase().startsWith("could you") || it.trim().lowercase().startsWith("can you") },
            tip = "Try: \"Could you tell me more about the team?\""
        )
    )

    private fun cafe(spoken: String): List<ScenarioCheck> = listOf(
        ScenarioCheck(
            label = "Ordered politely",
            met = has(spoken, "could i", "i'd like", "i would like", "may i", "can i have", "please"),
            tip = "Try: \"Could I have a coffee, please?\""
        ),
        ScenarioCheck(
            label = "Gave a size or detail",
            met = has(spoken, "small", "medium", "large", "cup", "glass", "one", "two", "with milk", "sugar"),
            tip = "Try: \"A small one with milk, please.\""
        ),
        ScenarioCheck(
            label = "Said thank you",
            met = has(spoken, "thank"),
            tip = "Try: \"Thank you, that's perfect.\""
        )
    )

    private fun travel(spoken: String): List<ScenarioCheck> = listOf(
        ScenarioCheck(
            label = "Mentioned the journey",
            met = has(spoken, "flight", "airport", "gate", "ticket", "passport", "hotel", "train", "bus", "luggage"),
            tip = "Try: \"I'm flying to Singapore on Friday.\""
        ),
        ScenarioCheck(
            label = "Asked about time or price",
            met = (has(spoken, "what time", "when", "how much", "price", "cost", "how long")),
            tip = "Try: \"What time does the flight leave?\""
        ),
        ScenarioCheck(
            label = "Used a polite request",
            met = has(spoken, "could you", "can you", "please", "would you"),
            tip = "Try: \"Could you help me with my bag, please?\""
        )
    )

    private fun exam(spoken: String, lines: List<String>): List<ScenarioCheck> = listOf(
        ScenarioCheck(
            label = "Spoke in full sentences",
            met = lines.any { it.split(Regex("\\s+")).count { w -> w.isNotBlank() } >= 8 },
            tip = "Try answering in two sentences instead of one word."
        ),
        ScenarioCheck(
            label = "Gave a reason",
            met = has(spoken, "because", "since", "so that", "that is why", "therefore"),
            tip = "Try: \"I prefer the city because everything is close.\""
        ),
        ScenarioCheck(
            label = "Used a linking word",
            met = has(spoken, "however", "although", "moreover", "in addition", "on the other hand", "also", "besides"),
            tip = "Try: \"However, it can be very noisy at night.\""
        )
    )

    private fun grammarDoctor(spoken: String, lines: List<String>): List<ScenarioCheck> = listOf(
        ScenarioCheck(
            label = "Described what confuses you",
            met = has(spoken, "confused", "difference between", "when do i", "how do i", "not sure", "don't understand", "dont understand"),
            tip = "Try: \"I'm not sure when to use 'since' and 'for'.\""
        ),
        ScenarioCheck(
            label = "Tried an example sentence",
            met = lines.any { it.split(Regex("\\s+")).count { w -> w.isNotBlank() } >= 6 },
            tip = "Try: \"For example, 'I have lived here for two years.'\""
        )
    )

    private fun freeConversation(spoken: String, lines: List<String>, turns: Int): List<ScenarioCheck> = listOf(
        ScenarioCheck(
            label = "Kept the conversation going",
            met = turns >= 5,
            tip = "Aim for five or six turns — a conversation needs both sides."
        ),
        ScenarioCheck(
            label = "Said more than a few words",
            met = lines.any { it.split(Regex("\\s+")).count { w -> w.isNotBlank() } >= 8 },
            tip = "Try adding one detail: where, when, or how you felt."
        ),
        ScenarioCheck(
            label = "Asked something back",
            met = lines.any { it.contains('?') },
            tip = "Try: \"What about you?\" or \"Have you been there?\""
        )
    )

    private fun has(text: String, vararg needles: String): Boolean =
        needles.any { text.contains(it) }
}
