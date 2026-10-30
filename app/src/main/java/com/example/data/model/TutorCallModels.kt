package com.example.data.model

/**
 * The state of a tutor call.
 *
 * A voice lesson is not a chat window with a microphone bolted on: it has the
 * same shape as a phone call, and the student should be able to tell where they
 * are in it at a glance. These four phases are that shape, and nothing else in
 * the app is allowed to invent a fifth one.
 *
 *  - [IDLE]       no call has started (or the last one was dismissed)
 *  - [CONNECTING] the tutor is being woken up and the opening line is prepared
 *  - [LIVE]       the call is running; turns alternate until [endTutorCall]
 *  - [ENDED]      the call is over and [CallSummary] is on screen
 */
enum class CallPhase { IDLE, CONNECTING, LIVE, ENDED }

/**
 * State of the on-device voice pack (the Piper model Miss Emma speaks with).
 *
 * [MISSING] and [FAILED] both mean "the call cannot start yet", which is why the
 * idle screen shows a card with a progress bar (or a retry) instead of letting a
 * conversation begin on the phone's robotic default voice.
 */
enum class TutorVoiceState { MISSING, DOWNLOADING, UNPACKING, READY, FAILED }

/**
 * One line of the call, in the order it happened.
 *
 * The screen draws a thread, not a transcript dump: a student line, the
 * correction it earned, then the tutor's answer. Keeping the order in the data
 * means the correction sits *between* the two replies it belongs to instead of
 * floating at the bottom of the page.
 */
data class TutorCallEntry(
    val role: Role,
    val text: String,
    val fix: GrammarFix? = null
) {
    enum class Role { STUDENT, TUTOR, FIX }
}

/**
 * One correction, kept exactly as the student said it.
 *
 * [original] is what the microphone heard (or what was typed), so the student can
 * recognise their own sentence; [corrected] is the same idea said properly. The
 * pair is what makes a correction teachable instead of just "wrong" — and
 * [rule] names the grammar point in a few words, which is what turns a fix into
 * something the student can look up afterwards.
 */
data class GrammarFix(
    val original: String,
    val corrected: String,
    val rule: String = "",
    /** Which spoken turn produced it, so the summary can say "turn 3 of 12". */
    val turnIndex: Int = 0
) {
    val isUseful: Boolean get() = corrected.isNotBlank() && original.trim() != corrected.trim()
}

/**
 * One parsed tutor turn.
 *
 * The model is asked for four labelled lines (see `TutorTurnPrompt`), because a
 * voice tutor has to do two different jobs in the same answer: say something the
 * student can reply to, and quietly record what was wrong. Mixing them in one
 * paragraph makes the speech read like a textbook margin.
 */
data class TutorTurn(
    /** What is spoken aloud. Two sentences at most, always ends with a question. */
    val say: String,
    /** The student's sentence, corrected — or null when it was already right. */
    val fixOriginal: String? = null,
    val fixCorrected: String? = null,
    /** Short name of the grammar point, e.g. "past simple". */
    val rule: String? = null,
    /** One word or phrase worth keeping from this turn. */
    val word: String? = null
) {
    fun toFix(turnIndex: Int): GrammarFix? {
        val corrected = fixCorrected?.trim().orEmpty()
        val original = fixOriginal?.trim().orEmpty()
        if (corrected.isBlank() || original.isBlank()) return null
        val fix = GrammarFix(
            original = original,
            corrected = corrected,
            rule = rule?.trim().orEmpty(),
            turnIndex = turnIndex
        )
        return fix.takeIf { it.isUseful }
    }
}

/**
 * One step of a structured lesson.
 *
 * Lessons exist because free conversation plateaus: a student can chat happily
 * for an hour while repeating the same three tenses. A lesson is a short, ordered
 * plan the tutor follows, so "practise English" has a target instead of a vibe.
 *
 * [teacherLine] is the offline script — if the server cannot be reached the call
 * still runs, because a lesson that only works with internet is not a lesson the
 * student can rely on.
 */
data class LessonStep(
    /** "Teach", "Model", "Practise", "Stretch" — shown as a stepper. */
    val name: String,
    /** What the student is asked to do, in one line. */
    val instruction: String,
    /** What the tutor says to open this step when the model is unavailable. */
    val teacherLine: String,
    /** A worked example the model is asked to imitate. */
    val modelLine: String = "",
    /** Replies the student can tap if they freeze. */
    val starters: List<String> = emptyList()
)

/**
 * A lesson the tutor can teach.
 *
 * [level] follows CEFR (A2, B1, B2) because that is what a student can look up
 * and what an employer understands; "beginner/intermediate" means different
 * things to different people.
 */
data class TutorLesson(
    val id: String,
    val title: String,
    val goal: String,
    val level: String,
    val steps: List<LessonStep>
) {
    val stepCount: Int get() = steps.size
    fun step(index: Int): LessonStep? = steps.getOrNull(index)
}

/**
 * What the student sees after hanging up.
 *
 * It is a record, not a scoreboard: time actually spoken, how many turns they
 * took, every correction with the rule it belongs to, and the words they met.
 * [cleanTurnPercent] is deliberately about turns rather than marks — a student
 * who spoke ten sentences and fixed two did better than one who said "yes" ten
 * times perfectly.
 */
data class CallSummary(
    val durationSeconds: Int = 0,
    val turns: Int = 0,
    val corrections: List<GrammarFix> = emptyList(),
    val words: List<String> = emptyList(),
    val cleanTurnPercent: Int = 0,
    val scenario: String = "",
    val lessonTitle: String? = null,
    val lessonCompleted: Boolean = false
) {
    val durationLabel: String
        get() = if (durationSeconds >= 60) {
            "${durationSeconds / 60}m ${durationSeconds % 60}s"
        } else {
            "${durationSeconds}s"
        }
}
