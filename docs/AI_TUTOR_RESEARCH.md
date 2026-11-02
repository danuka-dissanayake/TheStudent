# What an AI English tutor should actually do — research behind Round 21

Scope: the speaking-practice call in `ui/screens/AiTutorScreen.kt` and its engine in
`TheStudentViewModel`. This is **not** about colours, the orb or the mic button. It is
about the decisions that make a ten-minute call either teach someone to speak or
make them freeze.

The short version: a tutor is measured by **what the student produces**, not by how
much feedback the screen prints. Everything below is a decision rule, and every
decision rule now has a home in `data/voice/TutorCoach.kt` (pure Kotlin, unit-tested)
so it can be reasoned about without a phone.

---

## 1. The tutor contract

| # | The tutor must | Why |
|---|---|---|
| 1 | **Let the student do most of the talking.** One or two short sentences, exactly **one** question, then stop. | AI dialogue already gives learners longer, slower turns and more floor time than human dialogue; longer, less complete AI turns leave room for learner-initiated language [4](https://educationaldatamining.org/edm2026/proceedings/2026.EDM.full-papers.210/index.html). A field experiment on voice tutors found roughly twice the dialogue turns per minute in voice, and its own prompt rules require one question and short turns [3](https://arxiv.org/html/2609.23958v1). |
| 2 | **Wait.** No talking for at least ~12 s of silence; then narrow the question instead of filling the gap with encouragement. | 3–5 s of wait time increases answer length, correctness and willingness to communicate, and lowers anxiety for EFL learners [1](https://www.structural-learning.com/post/questioning-in-teaching) [2](https://files.eric.ed.gov/fulltext/EJ1220212.pdf). In a six-product test of AI tutors, only one let a 10 s silence stand and then offered a *narrowing* question; products that filled the space with warmth "destroyed the exercise" [2](https://thereviewatnyu.com/blog/language-learning-with-ai-tutors). A structured voice tutor's non-negotiable rule is ~30 s of think time before offering to repeat [3](https://arxiv.org/html/2609.23958v1). |
| 3 | **Never ask the same question twice** in a call, and react to the words the student actually used. | A tutor who asks the same five questions every call is a form, not a tutor. Contingency (using one detail from the learner's turn) is what makes a recast noticeable. |
| 4 | **Correct selectively, not exhaustively.** At most a handful of explicit fixes per call; in free conversation prefer a quiet recast inside the reply. | Teachers calibrate timing and explicitness to the task: in fluency work they allow extended turns and delay or soften feedback, because correcting mid-sentence makes learners lose the thread; in accuracy drills they intervene immediately with a brief model [1](https://link.springer.com/article/10.1186/s40862-026-00442-4) [5](https://www.diva-portal.org/smash/get/diva2:1955974/FULLTEXT01.pdf). Practical guidance for AI tutors: correct only errors that impede comprehension, use recasts, and no more than 2–3 errors per turn [1](https://dev.to/pocket_linguist/i-built-an-ai-language-tutor-heres-what-i-learned-about-nlp-5c05). |
| 5 | **Prefer self-repair, then a hint, then a model.** | Prompts and elicitation produce more learner-generated repair than simply being handed the answer [2](https://www.researchgate.net/publication/336685964_Oral_corrective_feedback_students'_responses_to_different_types_of_prompting) [3](https://tpls.academypublication.com/index.php/tpls/article/download/2799/2297/8754) [5](https://www.researchgate.net/publication/352983715_Designing_a_Feedback_Component_of_Intelligent_Tutoring_System_for_Foreign_Language). Modified output is a sign of uptake, and self-initiated self-repair is where most of it happens [1](https://www.sciencedirect.com/science/article/pii/S0346251X19305482). |
| 6 | **Treat the transcript as untrusted.** Never correct a one-word ASR fragment, never log an ASR artefact as a learner error. | ASR is measurably worse on accented / L2 speech; transcription errors come from accent, connected speech and disfluency, and a dictation system's score does **not** track whether a human would understand the learner [1](https://www.researchgate.net/publication/389712981_Automatic_Speech_Recognition_for_Non-Native_English_Accuracy_and_Disfluency_Handling) [4](https://www.researchgate.net/publication/401914465_ASR-Dictation_Accuracy_for_Second_Language_Speech_Programs_Tasks_and_Language_Backgrounds). Accent bias produces misleading corrective feedback that reflects the model, not the student [5](https://www.researchgate.net/publication/411892069_Beyond_Native_Norms_Decolonizing_AI_Speech_Recognition_for_Accent_Equity_in_Nigerian_ESL_Classrooms). |
| 7 | **Be fast.** Hard-timeout the model and speak a local answer; never leave a live call in silence. | Users tolerate roughly 800–1200 ms before a voice product feels broken; every external call needs a timeout and a spoken fallback [1](https://dev.to/pocket_linguist/i-built-an-ai-language-tutor-heres-what-i-learned-about-nlp-5c05) [3](https://www.bitontree.com/ai-voice-agent-latency) [4](https://www.raftlabs.com/blog/ai-voice-agents-guide). Endpointing is the hidden half of the delay; fixed silence thresholds should be adapted to whether the sentence sounds complete [1](https://safeguard.sh/resources/blog/voice-ai-latency-budget-200-ms-turn-taking) [5](https://www.autointerviewai.com/blog/solving-end-of-turn-detection-voice-ai-agent-2026). |
| 8 | **Use level-appropriate input and scaffold, not praise.** | CEFR-graded vocabulary and sentence frames are standard scaffolding for English learners [5](https://www.jeffbullas.com/thread/practical-ways-ai-can-support-english-language-learners-with-scaffolding-easy-tools-and-prompts/). Prototype tutors were explicitly tuned to "take short turns, allow time, avoid excessive praise" [4](https://educationaldatamining.org/edm2026/proceedings/2026.EDM.full-papers.210/index.html). Generic praise ("That is very interesting!") teaches nothing and crowds out the one question. |
| 9 | **A lesson advances on evidence, not on a turn counter.** | A step is a can-do target. Moving on after one word, or repeating the same step in the reply that should open the next one, is a plan that exists only in the code. Scaffold after two short attempts, then move. |
| 10 | **Remember only safe things, and let the student erase them.** | Memory makes the tutor feel continuous, but a device shared between students must not inherit the previous student's level, exam date or name, and the disclosure has to say what is kept. |

## 2. What was actually wrong in the call engine

These are the logical faults found by reading the code, not styling notes:

1. **A 35-second read timeout on the model.** `HuggingFaceSpaceClient` waited up to
   35 s on a free Space. On a live call that is dead air; the student hangs up.
2. **Replay and "Hear it" were tagged as call turns.** `speakText()` used the
   `tutor-call-` utterance prefix, so its `onDone` called `onTutorSpeechFinished()`
   and reopened the microphone *while the model was still generating*. The next
   student turn then hit `isTutorTurnInFlight` and was silently discarded.
3. **Stale TTS `onError` callbacks had no sequence guard** (only `onDone` did), so an
   interrupted answer could hand the floor back mid-turn and wipe the dictation buffer.
4. **Barge-in during THINKING dropped the student's words.** `submitCallTurn()`
   returned early when a reply was in flight; there was no way to take the floor back.
5. **Silence was measured in failed recogniser sessions, not seconds.** The ladder
   nudged with "Take your time." after ~3 misses (often <15 s), re-showed the previous
   correction card, and hung up after 7 misses regardless of elapsed time.
6. **A fixed 2.8 s endpoint** committed a turn even when the buffer ended on "and",
   "because" or "so" — exactly the pause a second-language learner needs.
7. **The lesson advanced one step per student turn** with no evidence check, and the
   system prompt was built from the *current* step rather than the step the reply was
   moving into (off-by-one). A four-step lesson was "complete" after four words.
8. **The model's answer was parsed line-by-line without tracking the label**, so a
   wrapped `WORD:`/`NOTE:` continuation was appended to `SAY` and read aloud; stray
   markdown labels could be spoken too.
9. **Corrections were generated from untrusted ASR text.** `WordUpgrades` fired on
   bare words like "good", "nice", "great", "important" (a card on almost every
   correct sentence), and `EnglishGrammarRules` produced wrong output for
   "I have went yesterday" ("I had went yesterday"), "good in the morning" and
   "Do you know about the exam?". `isStopRequest()`/`isBackchannel()` compared
   *punctuated* text (`"Wait."`, `"Okay."`) against unpunctuated sets, so they never
   matched.
10. **The CEFR level could move up after a single long clean turn**, and `cleanTurnPercent`
    de-duplicated by sentence text, so repeating the same mistake five times counted
    as one wrong turn.
11. **"Hear it" / "Say it" stopped working after the model replied**: `deliverTutorTurn`
    overwrote `callFix` with null, but the card's buttons read `callFix`.
12. **Tutor memory and level were stored in a SharedPreferences file that was never
    cleared on logout**, and the consent screen did not mention remembered facts.
13. **A cancelled recogniser session had a one-shot `discardingRecognition` flag**,
    so a late `onError` followed by a late `onResults` from the same session could
    submit a duplicate turn.
14. **The idle "Free conversation" card did not clear the active lesson**, so it
    started the guided lesson from the previous call.
15. **Local/offline replies were long canned paragraphs** with generic praise and
    three sentences — the opposite of the two-sentence, one-question contract.

## 3. How the code now implements the contract

| Decision | Owner |
|---|---|
| Short, contingent, one-question replies; strip praise/markdown; no repeated questions | `TutorCoach.shapeReply()`, applied to **every** tutor line (model, offline, opener, nudge) in `deliverTutorTurn()` |
| 12 s silent wait → narrowing question → sentence starter → polite hang-up | `TutorCoach.silenceAction()` / `easierQuestion()` / `scaffoldLine()` in `handleRecognitionMiss()` |
| Adaptive endpointing (4.6 s for an unfinished thought, 2.0 s otherwise) + 30 s max turn | `TutorCoach.endpointDelayMs()` / `MAX_TURN_MS` in `scheduleDictationCommit()`, `handleDictationSegment()` |
| Hard 6.5 s model timeout + local spoken answer | `HuggingFaceSpaceClient(callTimeoutMs)`, `TUTOR_REPLY_TIMEOUT_MS`, `TutorCoach.localReply()` |
| Fluency vs accuracy feedback budget; ASR-safe correction gate; plausible minimal fix | `TutorCoach.feedbackMode()`, `canCorrect()`, `plausibleFix()`, `maxCorrections()`, `localFixFor()`, `modelFixFor()` |
| Evidence-based lesson steps, correct step target, completion wrap-up | `TutorCoach.lessonProgress()` + `submitCallTurn()` / `applyLessonProgress()` |
| Barge-in that keeps the words | per-turn reply job/generation, `submitCallTurn()` cancels the stale reply instead of dropping the student |
| Preview utterances never drive the loop | `tutor-preview-` prefix + dedicated `onDone`/`onError` handling |
| No duplicate turns from cancelled sessions | per-session recognition generation, checked in every `RecognitionListener` callback |
| Level changes need ≥6 turns **and** two consecutive calls of evidence | `TutorCoach.levelTrend()` + streaks in `TutorProgressStore` |
| Honest memory | consent v2, sensitive-fact filter, `forgetTutorMemory()`, cleared on logout |

## 4. Sources

- [1](https://link.springer.com/article/10.1186/s40862-026-00442-4) — oral corrective feedback: timing, explicitness, task orientation, affective safety.
- [5](https://www.diva-portal.org/smash/get/diva2:1955974/FULLTEXT01.pdf) — teachers' and students' views on OCF; fluency vs accuracy tasks.
- [4](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2023.1026174/full) — systematic review of immediate vs delayed corrective feedback.
- [2](https://www.researchgate.net/publication/336685964_Oral_corrective_feedback_students'_responses_to_different_types_of_prompting) — clarification requests/prompts and learner uptake.
- [3](https://tpls.academypublication.com/index.php/tpls/article/download/2799/2297/8754) — uptake and repair rates by feedback type.
- [1](https://www.sciencedirect.com/science/article/pii/S0346251X19305482) — modified output as evidence of uptake.
- [2](https://thereviewatnyu.com/blog/language-learning-with-ai-tutors) — six-product AI tutor probe, including the ten-second silence test.
- [3](https://arxiv.org/html/2609.23958v1) — randomised field experiment on voice vs text tutoring; think-time prompt rules.
- [4](https://educationaldatamining.org/edm2026/proceedings/2026.EDM.full-papers.210/index.html) — interactional fluency with AI interlocutors; short turns, less praise.
- [1](https://www.structural-learning.com/post/questioning-in-teaching) — Rowe wait time, 3–5 s.
- [2](https://files.eric.ed.gov/fulltext/EJ1220212.pdf) — extended wait time and EFL willingness to communicate.
- [1](https://dev.to/pocket_linguist/i-built-an-ai-language-tutor-heres-what-i-learned-about-nlp-5c05) — correction budget, recasts, 800–1200 ms latency budget.
- [1](https://safeguard.sh/resources/blog/voice-ai-latency-budget-200-ms-turn-taking) — endpointing as the hidden latency cost.
- [3](https://www.bitontree.com/ai-voice-agent-latency) — timeouts and spoken fallbacks for voice agents.
- [4](https://www.raftlabs.com/blog/ai-voice-agents-guide) — 400–800 ms round-trip target, adaptive turn-taking.
- [5](https://www.autointerviewai.com/blog/solving-end-of-turn-detection-voice-ai-agent-2026) — semantic completeness vs fixed silence thresholds.
- [1](https://www.researchgate.net/publication/389712981_Automatic_Speech_Recognition_for_Non-Native_English_Accuracy_and_Disfluency_Handling) — ASR accuracy on non-native accented English.
- [4](https://www.researchgate.net/publication/401914465_ASR-Dictation_Accuracy_for_Second_Language_Speech_Programs_Tasks_and_Language_Backgrounds) — ASR transcription errors vs human intelligibility judgements.
- [5](https://www.researchgate.net/publication/411892069_Beyond_Native_Norms_Decolonizing_AI_Speech_Recognition_for_Accent_Equity_in_Nigerian_ESL_Classrooms) — accent bias and misleading corrective feedback.
- [5](https://www.jeffbullas.com/thread/practical-ways-ai-can-support-english-language-learners-with-scaffolding-easy-tools-and-prompts/) — CEFR-aware scaffolding and sentence frames.
- [5](https://www.researchgate.net/publication/352983715_Designing_a_Feedback_Component_of_Intelligent_Tutoring_System_for_Foreign_Language) — prompting strategies outperform giving the answer.
