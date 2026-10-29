package com.example.data.remote

import com.example.data.model.LessonStep
import com.example.data.model.TutorLesson

/**
 * The lessons the tutor can actually teach.
 *
 * Free conversation is the product, but a student who only ever free-talks keeps
 * using the grammar they already know. Each lesson below is a short, ordered
 * plan — teach the point, model it, make the student use it, then stretch it —
 * and the tutor follows that plan turn by turn, so a ten-minute call has a
 * measurable outcome at the end.
 *
 * Every step carries both a [LessonStep.teacherLine] and an instruction for the
 * model: the model makes the practice feel alive, the script keeps the lesson
 * running when the model cannot be reached.
 */
object TutorLessonCatalog {

    val lessons: List<TutorLesson> = listOf(
        TutorLesson(
            id = "past_simple",
            title = "Telling a story in the past",
            goal = "Say what happened yesterday without slipping into the present tense.",
            level = "A2",
            steps = listOf(
                LessonStep(
                    name = "Teach",
                    instruction = "Show the student that finished actions take the past simple.",
                    teacherLine = "Today we are telling stories about the past. When something is finished, " +
                        "the verb changes: go becomes went, see becomes saw. Say one thing you did yesterday, " +
                        "and I will help you get the verb right.",
                    modelLine = "Yesterday I walked to the shop and I bought some bread.",
                    starters = listOf("Yesterday I went to school.", "Last night I watched a film.")
                ),
                LessonStep(
                    name = "Model",
                    instruction = "Give one short past-tense sentence of your own, then ask for theirs.",
                    teacherLine = "Yesterday evening I cooked rice and I watched the news with my family. " +
                        "Now you — what did you do yesterday evening?",
                    modelLine = "Yesterday evening I cooked rice and I watched the news.",
                    starters = listOf("I studied for my exam.", "I met my friends in town.")
                ),
                LessonStep(
                    name = "Practise",
                    instruction = "Ask about a trip or a weekend and insist on past tense verbs.",
                    teacherLine = "Let's try a longer story. Tell me about a trip you took — where did you go, " +
                        "who did you go with, and what did you do there?",
                    modelLine = "Last year I went to Kandy with my cousins and we visited the temple.",
                    starters = listOf("Last year I went to Kandy.", "We visited the temple and ate lunch.")
                ),
                LessonStep(
                    name = "Stretch",
                    instruction = "Ask for a story with two events in order, using then or after that.",
                    teacherLine = "Beautiful. Now put two things in order. Say what happened first, " +
                        "then use after that or then for the second thing.",
                    modelLine = "We arrived in Kandy, then we visited the temple after that.",
                    starters = listOf("First we went to the beach, then we ate lunch.", "After that, we went home.")
                )
            )
        ),
        TutorLesson(
            id = "present_perfect",
            title = "Experience with the present perfect",
            goal = "Talk about life experience without saying when it happened.",
            level = "B1",
            steps = listOf(
                LessonStep(
                    name = "Teach",
                    instruction = "Explain have + past participle for life experience.",
                    teacherLine = "For life experience we use have plus the past participle: I have been, " +
                        "you have seen, she has eaten. Ask me a question with have you ever.",
                    modelLine = "Have you ever been to India?",
                    starters = listOf("Have you ever been to India?", "Have you ever eaten sushi?")
                ),
                LessonStep(
                    name = "Model",
                    instruction = "Answer one have-you-ever question yourself, then hand the turn back.",
                    teacherLine = "Yes, I have been to India once. It was a train journey. " +
                        "Your turn — have you ever climbed a mountain?",
                    modelLine = "Yes, I have been to India once.",
                    starters = listOf("Yes, I have.", "No, I have never climbed a mountain.")
                ),
                LessonStep(
                    name = "Practise",
                    instruction = "Keep asking experience questions and correct any past simple answer.",
                    teacherLine = "Tell me something you have always wanted to try, and something you have " +
                        "already tried. Use I have always wanted and I have already.",
                    modelLine = "I have always wanted to learn to swim, and I have already tried surfboarding.",
                    starters = listOf("I have always wanted to travel to Japan.", "I have already tried camping.")
                ),
                LessonStep(
                    name = "Stretch",
                    instruction = "Contrast present perfect for experience with past simple for details.",
                    teacherLine = "One more thing — if you add the time, the sentence changes: I have been " +
                        "to India, but I went to India in 2019. Try a sentence with a time in it.",
                    modelLine = "I have been to India, but I went there in 2019.",
                    starters = listOf("I have visited Kandy, but I went there last year.")
                )
            )
        ),
        TutorLesson(
            id = "polite_requests",
            title = "Polite requests and offers",
            goal = "Get what you need without sounding like a command.",
            level = "A2",
            steps = listOf(
                LessonStep(
                    name = "Teach",
                    instruction = "Teach could you and would you for polite requests.",
                    teacherLine = "In English, could you and would you make a request polite. " +
                        "Could you open the window, please. Try asking me for one thing with could you.",
                    modelLine = "Could you speak a little more slowly, please?",
                    starters = listOf("Could you repeat that, please?", "Could you help me?")
                ),
                LessonStep(
                    name = "Model",
                    instruction = "Play a shop assistant and invite a polite request.",
                    teacherLine = "Let's practise in a shop. I am the shopkeeper. Ask me to show you a shirt, " +
                        "politely.",
                    modelLine = "Could you show me that blue shirt, please?",
                    starters = listOf("Could I see that shirt, please?", "Would you show me the blue one?")
                ),
                LessonStep(
                    name = "Practise",
                    instruction = "Ask the student to make three requests in a cafe scene.",
                    teacherLine = "We are in a cafe now. Ask for the menu, a glass of water, and the bill — " +
                        "one request at a time, each one polite.",
                    modelLine = "Could I have the menu, please? And could you bring a glass of water?",
                    starters = listOf("Could I have the bill, please?", "Could you bring some water?")
                ),
                LessonStep(
                    name = "Stretch",
                    instruction = "Ask for a polite way to refuse and to check understanding.",
                    teacherLine = "Good. Now say no politely — imagine I asked you to help tonight but you cannot. " +
                        "Refuse, give a reason, and offer another time.",
                    modelLine = "I'm sorry, I can't tonight, but I could help tomorrow evening.",
                    starters = listOf("I'm sorry, I can't tonight.", "Could we do it tomorrow?")
                )
            )
        ),
        TutorLesson(
            id = "opinion_oral",
            title = "Opinions for the A/L oral",
            goal = "Answer a discussion question with opinion, reason and example.",
            level = "B1",
            steps = listOf(
                LessonStep(
                    name = "Teach",
                    instruction = "Give the opinion, reason, example structure for exam speaking.",
                    teacherLine = "For the oral exam, an answer needs three parts: your opinion, a reason, " +
                        "and an example. In my opinion, then because, then for example. Let's build one together " +
                        "about social media.",
                    modelLine = "In my opinion, social media helps students learn, because it gives access to " +
                        "free lessons. For example, I follow a mathematics channel.",
                    starters = listOf("In my opinion, social media is useful because...", "For example, ...")
                ),
                LessonStep(
                    name = "Model",
                    instruction = "Ask the student's opinion on renewable energy in Sri Lanka.",
                    teacherLine = "Your question: how important is renewable energy for Sri Lanka? " +
                        "Tell me your opinion, one reason, and one example.",
                    modelLine = "I believe renewable energy is essential, because fuel is expensive. " +
                        "For example, solar panels could power rural schools.",
                    starters = listOf("I believe it is important because...", "For example, solar power...")
                ),
                LessonStep(
                    name = "Practise",
                    instruction = "Push for a counter-argument and a conclusion sentence.",
                    teacherLine = "Now add balance. Say what someone else might think, then say why you still " +
                        "disagree, and finish with in conclusion.",
                    modelLine = "Some people think it is too expensive. However, it saves money later. " +
                        "In conclusion, I believe it is worth doing.",
                    starters = listOf("Some people think...", "However, in conclusion...")
                ),
                LessonStep(
                    name = "Stretch",
                    instruction = "Time a ninety-second answer on a new topic.",
                    teacherLine = "Ninety seconds, no notes: should school students have mobile phones? " +
                        "Opinion, reason, example, balance, conclusion.",
                    modelLine = "In my opinion, students should have phones in secondary school...",
                    starters = listOf("In my opinion, students should...", "However, ...")
                )
            )
        ),
        TutorLesson(
            id = "interview",
            title = "A job interview that sounds natural",
            goal = "Answer tell-me-about-yourself without listing adjectives.",
            level = "B1",
            steps = listOf(
                LessonStep(
                    name = "Teach",
                    instruction = "Explain present perfect for experience and past simple for achievements.",
                    teacherLine = "Interviews mix two tenses: I have worked with children for two years, " +
                        "and I led a project last year. Tell me one thing you have done and one thing you did " +
                        "last year.",
                    modelLine = "I have worked in a shop for two years, and I led a project last year.",
                    starters = listOf("I have studied English for six years.", "I started a club last year.")
                ),
                LessonStep(
                    name = "Model",
                    instruction = "Ask tell me about yourself and require one concrete detail.",
                    teacherLine = "Let's begin the interview. Tell me about yourself — and give me one concrete " +
                        "detail, like a number or a name.",
                    modelLine = "I am a final-year student in Kandy, and I have led a debate team of fifteen " +
                        "students.",
                    starters = listOf("I am a student at...", "I have led...")
                ),
                LessonStep(
                    name = "Practise",
                    instruction = "Ask a strength question with the star structure.",
                    teacherLine = "What is your biggest strength? Use a story: the situation, what you did, " +
                        "and the result.",
                    modelLine = "When our school event lost its organiser, I took over the timetable and we " +
                        "finished on time.",
                    starters = listOf("When..., I...", "As a result, ...")
                ),
                LessonStep(
                    name = "Stretch",
                    instruction = "Ask the closing question and require a question back.",
                    teacherLine = "Last one: why should we hire you? And at the end, ask me a question — " +
                        "that is what good candidates do.",
                    modelLine = "I learn quickly, and I have proof: I taught myself spreadsheets in a month. " +
                        "May I ask how success is measured in this role?",
                    starters = listOf("I learn quickly because...", "May I ask...")
                )
            )
        ),
        TutorLesson(
            id = "pronunciation",
            title = "Clearer consonants and word stress",
            goal = "Be understood on the first try.",
            level = "A2",
            steps = listOf(
                LessonStep(
                    name = "Teach",
                    instruction = "Focus on th and v/w, two sounds Sinhala speakers often merge.",
                    teacherLine = "We will work on two sounds. Put your tongue between your teeth for th: think, " +
                        "three, thank you. For v, your top teeth touch your bottom lip: very, value. " +
                        "Say them back to me.",
                    modelLine = "Thank you very much.",
                    starters = listOf("Thank you very much.", "Three very good friends.")
                ),
                LessonStep(
                    name = "Model",
                    instruction = "Have the student read a short sentence, then echo the correct version.",
                    teacherLine = "Now read this after me: I have three very valuable books.",
                    modelLine = "I have three very valuable books.",
                    starters = listOf("I have three valuable books.", "Those three things are valuable.")
                ),
                LessonStep(
                    name = "Practise",
                    instruction = "Ask the student to use three th words and two v words in a sentence.",
                    teacherLine = "Make one sentence of your own with three th words and two v words. " +
                        "For example: I think the other visitors arrived.",
                    modelLine = "I think the other visitors arrived.",
                    starters = listOf("I think these things are valuable.", "The weather is very hot there.")
                ),
                LessonStep(
                    name = "Stretch",
                    instruction = "Work on word stress in long words.",
                    teacherLine = "Last: stress. In important the middle is louder — imPORtant. In university " +
                        "it is the third syllable — univerSIty. Say a sentence with both.",
                    modelLine = "My university is important; it is very valuable to me.",
                    starters = listOf("My university is very important.", "Education is valuable.")
                )
            )
        )
    )

    fun find(id: String?): TutorLesson? = lessons.firstOrNull { it.id == id }
}
