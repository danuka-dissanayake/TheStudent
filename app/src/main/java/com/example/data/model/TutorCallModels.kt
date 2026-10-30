package com.example.data.model

import com.example.data.voice.ScenarioCheck

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
    val turnIndex: Int = 0,
    /**
     * A plain phrase the student used, and the stronger phrase worth trying.
     *
     * This is not a mistake — "very good" is perfectly correct English. It is the
     * second half of what a teacher gives: the fix, and one better word. Keeping
     * it separate from [corrected] means the card can show an upgrade for a
     * sentence that had nothing wrong with it at all.
     */
    val upgradeFrom: String = "",
    val upgradeTo: String = ""
) {
    val isUseful: Boolean get() = corrected.isNotBlank() && original.trim() != corrected.trim()

    val hasUpgrade: Boolean
        get() = upgradeFrom.isNotBlank() &&
            upgradeTo.isNotBlank() &&
            !upgradeFrom.equals(upgradeTo, ignoreCase = true)

    /** Something worth a card: a correction, an upgrade, or both. */
    val isWorthShowing: Boolean get() = isUseful || hasUpgrade
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
    val word: String? = null,
    /** The plain phrase the student said, when the model suggests a better one. */
    val upgradeFrom: String? = null,
    /** The better phrase to try instead. */
    val upgradeTo: String? = null,
    /** One fact about the student worth remembering for the next call. */
    val note: String? = null
) {
    fun toFix(turnIndex: Int): GrammarFix? {
        val corrected = fixCorrected?.trim().orEmpty()
        val original = fixOriginal?.trim().orEmpty()
        // An upgrade alone is still worth showing: the sentence was right, the
        // word could be better.
        val hasUpgrade = !upgradeFrom.isNullOrBlank() && !upgradeTo.isNullOrBlank()
        if (original.isBlank() || (corrected.isBlank() && !hasUpgrade)) return null
        val fix = GrammarFix(
            original = original,
            corrected = corrected,
            rule = rule?.trim().orEmpty(),
            turnIndex = turnIndex,
            upgradeFrom = upgradeFrom?.trim().orEmpty(),
            upgradeTo = upgradeTo?.trim().orEmpty()
        )
        return fix.takeIf { it.isWorthShowing }
    }
}

/**
 * A "say it properly" moment.
 *
 * Two things use it: the warm-up that brings back a sentence the student got
 * wrong on an earlier call, and the **Say it** button on a correction card.
 * Both are the same act — the student produces the right sentence instead of
 * only hearing it — so they share one piece of state and one judge
 * ([com.example.data.voice.TutorDrill]).
 */
data class TutorReviewState(
    /** The sentence the student is being asked to produce. */
    val target: String,
    /** What they said the first time, for the warm-up card. Empty for a drill. */
    val wrong: String = "",
    /** What the microphone heard this time. */
    val heard: String = "",
    val phase: Phase = Phase.LISTENING,
    /** True for the pre-call warm-up, false for a mid-call drill. */
    val isWarmUp: Boolean = false,
    /** Review sentences still waiting after this one. */
    val remaining: Int = 0
) {
    enum class Phase { LISTENING, CORRECT, TRY_AGAIN }
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
    val lessonCompleted: Boolean = false,
    /** Did they do the task, not just speak: the scenario's own scorecard. */
    val checks: List<ScenarioCheck> = emptyList(),
    /** Speaking pace for this call; 0 when there was not enough speech. */
    val paceWpm: Int = 0,
    /** Words the drill struggled with — the ones worth saying aloud again. */
    val practiceWords: List<String> = emptyList(),
    /** Review sentences answered correctly during the warm-up. */
    val reviewed: Int = 0,
    val reviewAsked: Int = 0,
    /** "B1 → B2", or empty when the level did not move. */
    val levelLabel: String = "",
    /** The one line the idle screen shows before the next call. */
    val nextStepNote: String = ""
) {
    val durationLabel: String
        get() = if (durationSeconds >= 60) {
            "${durationSeconds / 60}m ${durationSeconds % 60}s"
        } else {
            "${durationSeconds}s"
        }
}
