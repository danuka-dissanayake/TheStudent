# The AI English Tutor — a call, not a chat box

`app/src/main/java/com/example/ui/screens/AiTutorScreen.kt` (screen) ·
`TheStudentViewModel` (call engine) · `data/remote/TutorTurnPrompt.kt`,
`EnglishGrammarRules.kt`, `TutorLessonCatalog.kt` (brain) ·
`data/model/TutorCallModels.kt` (shape)

---

## 1. What this screen is for

A Sri Lankan student who can read English but freezes when speaking it. The
product is not "ask an AI about grammar" — that already exists in the AI Study
Assistant. This is **speaking practice**: out loud, with someone patient, who
corrects inside the conversation instead of after it, and who follows a lesson so
ten minutes of talking has an outcome.

That is why the screen is shaped like a **phone call**:

| A chat box | A call (this screen) |
| --- | --- |
| You type, then wait | The microphone opens itself after every tutor sentence |
| The tutor's answer is a paragraph | Two sentences, ending in a question |
| Corrections arrive at the end | One correction per turn, while the words are still warm |
| Leaving is "back" | Leaving is hanging up, and the call leaves a summary |

---

## 2. The state machine

```
IDLE ──startTutorCall──▶ CONNECTING ──opener spoken──▶ LIVE ⇄ ENDED
                                   ▲                   │
                    endTutorCall ──┴───────────────────┘
```

`CallPhase` is the only source of truth for what the screen is doing. Inside
`LIVE`, the audio side still uses `AssistantVoiceState`
(`LISTENING → THINKING → SPEAKING → LISTENING …`), because that is what the
loader animation and the status pill read.

The loop, once `LIVE`:

1. `startVoiceListening()` — the recogniser opens, `ExtraPartialResults` gives
   live captions.
2. The student stops talking; the recogniser's own endpointer fires
   `onResults`.
3. `submitCallTurn(text)` — local grammar check first (instant), then the model.
4. `deliverTutorTurn(...)` — state, then speech, sentence by sentence.
5. The **last** sentence's `onDone` calls `onTutorSpeechFinished()` → back to 1.

Nothing in that loop needs a tap, which is the whole point: the student's hands
are free to be nervous.

---

## 3. Why the loop is built the way it is

**The opener is local.** A free Hugging Face Space can take seconds to wake up.
Waiting for it before saying hello would make every call begin with dead air, so
the first line comes from `EnglishVoiceTutorEngine.getStarterGreeting()` (or the
lesson's first step) and the model is only asked from the student's first turn
onwards.

**Speech is streamed sentence by sentence.** `speakTutorSentences()` queues each
sentence with `QUEUE_ADD`; the student hears the first one while the rest is still
being handed to the engine. Time-to-first-word is what makes a call feel alive.

**Every utterance is tagged.** `tutor-call-<seq>-<index>` — when the student
interrupts, the sequence is bumped and the late `onDone` callbacks of the
cancelled answer are ignored. Without this, the tutor's own stale callback would
reopen the microphone in the middle of a new sentence.

**Barge-in is a tap on the mic, not shouting.** True acoustic barge-in needs an
echo canceller and a separate VAD stream; on a phone speaker the microphone hears
the tutor and interrupts itself. So the contract is explicit: while the tutor
speaks, the mic button shows **Stop** and `interruptTutor()` cuts the audio and
hands the floor back immediately. (Headsets would allow real acoustic barge-in —
a follow-up, noted in §9.)

---

## 4. When it goes wrong (the part a chat UI never needs)

| Failure | What the app does | Why |
| --- | --- | --- |
| `ERROR_NO_MATCH` / speech timeout | miss count + 1, listen again after 900 ms | a miss is not a reason to end a call |
| 3 misses in a row | tutor says "No rush. I'm still here." | silence usually means the student is thinking, not gone |
| 5 misses | call ends, summary shown, snackbar explains | after a minute of nothing the microphone is broken |
| `onEndOfSpeech` with no result ever | 4.5 s watchdog closes the turn as a miss | some devices never deliver a result |
| Model unreachable | local script + local grammar rules + `Local tutor mode` | a lesson that needs internet is not a reliable lesson |
| Answer arrives after hang-up | dropped (`deliverTutorTurn` checks the phase) | never resurrect an ended call |
| TTS engine missing | captions only, loop continues | the screen must not dead-end |

Backchannels ("mm-hmm", "okay") are recognised by
`EnglishGrammarRules.isBackchannel()` and are not treated as turns; "stop",
"wait" are `isStopRequest()` and cut the audio without being answered.

---

## 5. Correction: two checkers, one voice

1. **Local, instant** — `EnglishGrammarRules`. Narrow, deterministic rules for the
   errors Sri Lankan learners actually make: past tense after *did*, *have* +
   simple past, *she don't*, third-person *-s*, *a/an*, uncountable plurals,
   *since/for*, and phrases like *discuss about*, *explain me*, *good name*.
   It runs before the network call, so the correction appears while the tutor is
   still "thinking" — and it still works offline.
2. **The model** — asked for a labelled answer:

```
SAY: <two sentences, ends with a question>
FIX: <the student's sentence corrected, or NONE>
RULE: <the grammar point in three words, or NONE>
WORD: <one useful word, or NONE>
```

`TutorTurnPrompt.parse()` is forgiving: no labels at all means the whole answer is
speech, and the correction falls back to the local rules. A model that echoes the
sentence back unchanged produces **no** correction — agreeing with the student is
not a fix.

Only **one** correction is shown per turn (the amber strip under the student's
line), because a wall of red teaches nothing except fear. Everything is collected
and listed properly in the summary.

---

## 6. Lessons

`TutorLessonCatalog` holds six lessons, each a four-step plan:

**Teach → Model → Practise → Stretch**

Every step carries a `teacherLine` (offline script), a `modelLine` (shown to the
model as the target), and `starters` (tap-to-say replies for a student who
freezes). The tutor is told which step it is on and is told not to jump ahead;
after each answered turn `advanceLesson()` moves one step, and the last step
switches the call to free conversation with a completion line.

Topics (Coffee Shop, Job Interview, Travel, Grammar Doctor, A/L Oral) still exist
for free conversation. Topic and lesson are mutually exclusive: choosing one
clears the other, so the tutor is never answering two briefs at once.

---

## 7. The screen

```
┌───────────────────────────────┐
│  [ Uiverse orbit loader ]     │  ← unchanged, directly under the top bar
│  Miss Emma · 02:41            │
│  ● Emma is speaking…          │
│  ┌─────────────────────────┐  │
│  │ TUTOR                   │  │  caption, revealed at speaking pace
│  │ That sounds fun! What…  │  │
│  └─────────────────────────┘  │
│  YOU SAID                     │
│  "I go to the beach yesterday"│
│  ┌ BETTER: "I went…" ──────┐  │  amber strip + rule + Hear it
│  TRY SAYING  [chip] [chip]    │
│  (mute) (type) (stop) (end)   │  ← call dock
└───────────────────────────────┘
```

- **The animation stays exactly where it was.** `UiverseLoaderVisualizer` is the
  first element under the app bar in every phase; what was added is a halo behind
  it that answers the tutor's state (green speaking, red listening, amber
  thinking, grey muted).
- **Idle is not empty.** The same orb, then a card with the topic/lesson, a
  `Start call` button and the connection status.
- **The end is a summary, not a score.** Minutes spoken, turns, every correction
  with its rule, the words met, and one sentence about what to do next.
- Type instead of speaking is always available (`Add a turn by keyboard`), and a
  typed turn goes through the same correction, answer and lesson step.

---

## 8. Offline and degraded modes

The screen never shows an error where a conversation could be. If the Space is
asleep, unauthorised, or the phone has no data:

- the answer comes from `EnglishVoiceTutorEngine` (script) or the next lesson step;
- the correction still comes from the local rules;
- the status line says which of the three is happening (`Local tutor mode`,
  `Space asleep`, `Private space: token required`);
- the summary is still produced and still logged as a Language activity.

Minimum viable call: microphone permission + a TTS engine. Without TTS the call
degrades to captions only, which still exercises the reading and the corrections.

---

## 9. Known limits / next steps

- Acoustic barge-in needs a headset (echo); today it is a deliberate tap.
- `SpeechRecognizer` on some OEM builds stops after a few restarts — the miss
  ladder ends the call rather than looping forever.
- A call earns a `LearningActivity`, not EXP: the EXP ledger (`api/_exp.php`)
  has no speaking rule yet. Adding one would need a `speaking:{date}` ref key so
  the daily cap and the one-award-per-ref rule still hold.
- The CEFR level is a state field (`tutorLevel`, default B1); it should come from
  the academic profile once that is wired to the tutor.

---

## 10. Checking a change

1. Start a call on a device with a microphone → the opener is heard within ~2 s.
2. Say "I go to the beach yesterday" → the correction strip appears before the
   tutor's reply finishes.
3. Say nothing for 5 s, twice → the tutor nudges; a third time and it hangs up.
4. Tap the mic while the tutor is speaking → the audio stops and listening begins.
5. Turn on flight mode mid-call → the next answer is local and the status line
   says so; the call keeps running.
6. Hang up → the summary lists the corrections and words, and the activity log
   gains a `AI Call (…)` row.

---

## 11. The screen (glass edition, round 18)

Approved mockup: `designs/35-ai-tutor-glass.png`. The tutor tab is a full-bleed
page — no app bar, no tab bar — because the reference design is a call, and a
call owns the whole screen.

- **One control.** `MicDock` is the only button, before and during the call. Its
  meaning changes with the turn (start → stop → interrupt → replay) and the
  caption under it says which one it is. There is no keyboard, no reply chips
  and no text field anywhere in the tutor UI.
- **The orb** (`IridescentOrb`) is the animation directly under the status bar:
  218 dp before the call, 84 dp during it, drawn in one `Canvas` and clipped to
  a circle so it reads as a ball of liquid glass.
- **The thread** is `uiState.callTranscript`: a student line, the correction it
  earned (`Role.FIX`, drawn as the warm `CorrectionCard`), then the tutor's
  answer. Capped at 40 lines on screen and 8 turns in the model's context.
- **Chrome** is in the header: back leaves the tab, `⋯` opens the menu sheet
  (conversation topics, lessons, model settings, end call).
- **Level strip** is minutes actually spoken, read from
  `TheStudentPreferences.speakingSeconds` / `speakingCalls`, which
  `endTutorCall` increments — the number cannot be gamed by tapping.
