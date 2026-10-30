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

### 11.1 Round 18, second pass

- **Palette is the app's**, not violet: emerald/teal/lime from `ui/theme/Color.kt`
  worn as glass, with amber reserved for corrections. Mockup:
  `designs/36-ai-tutor-emerald.png`.
- **The orb reacts.** `IridescentOrb` takes an `OrbMode`: it breathes when idle,
  swells with the real microphone loudness while listening (throttled RMS on
  `TheStudentViewModel.voiceLevel`, a flow kept out of UiState), pulses in a
  ~600 ms speech rhythm while the tutor talks, shimmers while it thinks, and
  presses in on touch. Tapping the idle orb starts the call. All animated values
  are read inside the draw/layer blocks, so the sphere redraws without
  recomposing the thread.
- **Type is bigger**: bubbles 14.5sp, correction 15sp, header 18/12.5sp,
  headline 28sp, mic caption 12.5sp.
- **The mic glyph is centred** — it was wrapped in a second, unsized `Box` that
  pinned it to the top-left corner of the button.

### 11.2 Round 18, third pass

- **The orb casts a shadow.** It draws its own ground shadow (dark emerald, offset
  below the sphere) and a brand bloom, then masks only the sphere itself with
  `clipPath`, so the light can spill outside the circle. Mockup:
  `designs/37a-tutor-idle-emerald.png`.
- **Entry cards are rows, not squares.** `ModeTile` is a full-width glass row:
  rounded icon plate, title on one line, promise underneath, level/steps badge
  and a chevron. It survives long lesson titles and narrow screens.
- **The mic has five faces** (`MicFace`): mic when idle/listening (a ring goes out
  while it listens), level bars while the tutor talks (tap = interrupt), ellipsis
  while it thinks, mic-off when muted. The glyph crossfades. See
  `designs/37b-tutor-mic-faces.png`.
- **Picking a topic starts the call.** It used to only swap state, so the screen
  stayed idle and the greeting went nowhere. Now the sheet closes, the topic or
  lesson is applied, and the call opens (permission included). Mid-call the topic
  simply changes under the same call, and the VM no longer speaks a duplicate
  opener when it is about to start one.
- **New lines rise in.** `AppearIn` fades each bubble/correction card over 240 ms
  while sliding it up 8 dp; height is reserved from the first frame, so the
  thread does not jump.
- **Model settings are gone** from the tutor UI (and the `ModelSettingsDialog`
  composable with them). The call still degrades to the local tutor on its own;
  the status line keeps saying so.

### 11.3 Round 18, fourth pass

- **No rings on the sphere.** The animated ripple rings are gone. What is left is
  a stacked drop shadow (tight contact shadow + wide soft one + brand bloom),
  a deeper glass body with a shaded underside, a bright inner rim and a hairline
  outer edge — so the ball is legible against its own pastel background.
  Mockup: `designs/38a-tutor-idle-emerald.png`.
- **Cards follow the reference.** Two off-white `ModeCard`s side by side: tinted
  icon chip, corner arrow, bold title, two or three lines of explanation. The
  speaking-level strip was removed with them.
- **The mic is a squircle**, not a ring stack: a brand gradient key with a
  coloured drop shadow, a gloss across the top and the five `MicFace` glyphs.
- **The home screen never scrolls.** `BoxWithConstraints` sizes the sphere from
  the space actually left (`min(width * 0.62, height * 0.42)`, 120–250 dp) and
  the gaps are weights, so the header, greeting, cards and mic all fit on a
  640 dp phone as well as a tablet.

### 11.4 Round 18, fifth pass

- **The orb is now the assistant-ui voice orb in emerald**
  (`assistant-ui.com/elements/orb`). A saturated liquid sphere rather than a
  pastel bubble: a lit top-left to deep sea-green base, six wide colour bands and
  three thin marble streaks drawn as *elliptical* gradients (rotate + scale), a
  specular hot spot, a dark rim vignette and a sweep-gradient rim light. Each
  band turns at its own speed, so the inside flows instead of pulsing.
  `DrawScope.orbBand` is the shared helper. Mockup: `designs/39a-tutor-idle-orb.png`.
- **Copy**: the greeting line is now "Ready to practice English with me?".
- **Free conversation card** uses a chat icon, not a microphone.
- **The mic is a circle again** — 72 dp, brand gradient, coloured drop shadow,
  gloss — keeping the five `MicFace` glyphs.

### 11.5 Round 19 — the cloud orb replaces the sphere

The iridescent sphere is gone. `IridescentOrb`, `orbBand` and `OrbMode` were
deleted; the screen now draws `CloudOrb` (`ui/components/CloudOrb.kt`), a port of
the supplied WebGL "Cloud Orb" prototype. Nothing else on the screen changed.

**Why a CPU port.** The prototype is a fragment shader. AGSL needs API 33 and this
app ships to API 24, so the same maths runs on the CPU into a 96 px
`ARGB_8888` bitmap that is drawn upscaled with `FilterQuality.High`. The field is
smooth by construction, so 96 px is enough — the upscale softens it exactly the
way the shader's own antialiasing does.

**The shader, transcribed.** `hash → noise (smoothstep lerp) → fbm` (5 octaves,
amplitude 0.52, rotated `mat2(0.80,0.60,-0.60,0.80)` and scaled 1.92 each octave),
then: domain warp from two `fbm`s, a `curl` term from `sin`/`cos`, a broad field
and a folded field mixed by activity, a horizon line, an `upper` half tinted deep
emerald, a Gaussian milk band along the horizon, a lower mist, and a circle mask
with a `0.488 → 0.5` soft edge. Palette: `#059669 / #10B981 / #A7F3D0 / #E6FBF1`.
`uvY` is flipped (`1 - (y + 0.5) / res`) because GL counts y up and bitmaps count
it down.

**Motion, from the prototype's presets** (`speed` / `roughness`, roughness feeds
the shader's `activity`):

| state | speed | roughness | scale |
|---|---|---|---|
| listening | `0.72 + 0.78·vol` | `0.28 + 0.32·vol` | `1 − 0.13·vol` |
| speaking | `1.65 + 1.55·vol` | `0.66 + 0.34·vol` | `1 + 0.09·vol` |
| thinking | `0.24` | `0.18` | slow breath `±2 %` |
| idle / connecting / muted | `0.30` | `0.12` | `1` |

`vol` is the microphone level the call engine already produces. The tutor's own
voice has no meter, so speaking runs at a fixed lively `0.85`. Thinking barely
moves in the prototype, so a 5.5 s breath was added — otherwise "thinking" and
"idle" look identical.

**Entrance** (the prototype's expressive preset): held at `0.63` for 180 ms, grows
to `1.178` in 300 ms with `CubicOut`, settles back to `1` over 1.35 s.

**Extras that are not shader:** a two-layer soft shadow (the orb is pale and would
dissolve on a pale page), a brand bloom, a static ring while connecting (the
prototype's 15 px spinner, rotated by a `rememberInfiniteTransition`), and a pale
veil over the whole ball when muted.

**Cost.** One render loop at ~30 fps, one `withContext(Dispatchers.Default)` of
~9 × 10³ pixels with ~80 hash evaluations each, and *no allocation per frame*:
two pre-made bitmaps are swapped (write back, draw front, never tear) and the
pixel array is reused. Every animated value is read inside `graphicsLayer` or the
`Canvas` draw block, never in composition, so the screen above it does not
recompose while the orb moves.

Mockups: `designs/40a-tutor-idle-cloud-orb.png`,
`designs/40b-tutor-live-cloud-orb.png`, `designs/40c-cloud-orb-states.png`.

### 11.6 Round 19b — the mic becomes a white glass disc

The filled green squircle/gradient button is gone. The microphone is now a 76 dp
white disc (`MicWhite 0xFAFFFFFF`) with a hairline `GlassEdge` border, a top-lit
white gloss, a brand-tinted soft shadow and a 104 dp white halo behind it — the
same language as the two glass circle buttons in the header, scaled up. The glyph
carries the state instead of the background: emerald `#059669` when idle or
listening, `#047857` while the tutor thinks, `#0F766E` while it speaks, grey
`#A3AEBC` with `MicOff` when muted. Nothing else about the control changed: one
tap to speak / stop, long press to hand the floor back, the same five `MicFace`
glyphs, the same gentle 1.5 s pulse.

Mockups: `designs/41a-tutor-idle-cloud-orb.png`,
`designs/41b-tutor-live-cloud-orb.png`.

### 11.7 Round 19c — the orb runs on the GPU, the idle page loses its status line

**Why the orb looked like 144p.** It was a 96 × 96 bitmap stretched to ~600
physical pixels, and the CPU loop could not afford more. Now, from Android 13
(API 33), the very same maths is compiled by AGSL into a `RuntimeShader` and
drawn by the canvas: one evaluation per *device* pixel, every frame, so there is
no texture to upscale and no staircase on the contour. The frame loop is driven
by `withFrameNanos`, i.e. the display's own vsync — the orb re-draws at the
screen's refresh rate instead of a 30 fps budget, which is what fixes the
stutter while the tutor speaks.

**Fallback.** `android.os.Build.VERSION.SDK_INT < 33`, or a driver that refuses
the program, silently keeps the CPU renderer (`OrbClouds`, 96 px, 30 fps). The
API check and the `catch` live in `createOrbGpu()`, *outside* the class that
mentions `RuntimeShader`, so an old device never resolves the missing type.

**Shader notes.** Uniforms are `uOrigin`/`uSize` (the square the canvas is about
to fill, in local pixels — Skia evaluates shaders in local space, so the
entrance scale does not skew them), `uTime` (the integrated flow) and
`uActivity`. Output is premultiplied. Note that AGSL needs a constant loop bound
(the fBm has none of the `while` loops the Kotlin version uses).

**Removed text.** The idle page had a small line under the cards that printed the
model host status ("Qwen…", then "Connected"). The user does not want to see the
plumbing, so the line — and the now-unused `hfStatusMessage` parameter — is gone.
The cards now sit closer to the mic.

Mockup: `designs/42a-orb-quality.png` (before / after).

### 11.8 Round 19d — idle life, bigger cards, no "connecting", a thread that follows itself

- **Idle orb is a little livelier.** The prototype idles at speed `0.30` /
  roughness `0.12`, which on a phone reads as almost frozen. `IDLE` now runs
  `0.46` / `0.16`: still well under listening (`0.72`) and nowhere near speaking
  (`1.65`). Connecting and muted keep the prototype's calm values.
- **Mode cards are bigger.** Title `13.5 → 15.5 sp` (line 17 → 20) and the
  description `11 → 12.5 sp` (line 15 → 17); the cards are taller and the
  weighted spacers above and below absorb it, so the page still never scrolls.
- **"Connecting" is gone.** `startTutorCall` moves to `CONNECTING` and only
  reaches `LIVE` when the greeting has finished speaking, so the call used to sit
  on the word "connecting". Now the header always shows the timer, the status
  label goes straight to the voice state, the empty-thread placeholder is
  removed, and the orb no longer has a connecting look — the tap feels immediate.
  `CallPhase.CONNECTING` still exists as an internal state (the call timer and
  `endTutorCall` use it); it is simply never displayed.
- **The thread now follows itself.** `scroll.maxValue` is measured from the
  layout that exists when the effect runs, so a single `animateScrollTo(maxValue)`
  landed one bubble short and the newest line sat under the mic. The effect now
  waits a frame, scrolls, and re-checks up to four times, keyed on the transcript
  size, the live correction and the voice state (the thinking bubble changes the
  height too). Bottom padding went `8 → 14 dp`.

### 11.9 Round 19e — a warmer, more natural tutor voice

The call still synthesises with the phone's own TTS engine (no API key, works
offline), but it no longer accepts whatever voice the engine offers by default.
`applyNarratorVoice` scores every English voice the engine exposes: `+60` for a
female voice, `−70` for a male one, `+30/+24/+8` for very-high/high/normal
quality, `+14` for a neural (network) voice, and a small bonus for `en-US`,
`en-GB` then `en-AU`. Anything flagged `notInstalled` is skipped. The winner is
applied with `setPitch(1.06)` and `setSpeechRate(0.97)`; if the device has no
suitable voice at all — or refuses the selection — the pitch goes to `1.10`
instead, which every engine honours. The chosen voice is logged as
`Tutor voice: <name> (<locale>)` so it is one `adb logcat` line away.

### 11.10 Round 19f — Miss Emma gets a real neural voice (Piper, on device)

The phone's text-to-speech engine is now only the fallback. The tutor speaks with
**Piper** — a small VITS neural voice (`en_US-amy-medium`, int8) — running locally
through **sherpa-onnx**, so it is natural, unlimited, private and works offline.

**Runtime.** sherpa-onnx is not on Maven Central (k2-fsa publish it as a GitHub
release asset only), so `app/build.gradle.kts` has a `fetchSherpaOnnx` task that
downloads `sherpa-onnx-2.0.1.aar` (47 MB) into `app/libs/` on the first build and
is wired into `preBuild`; the file is git-ignored. If the machine has no access to
GitHub the task fails with the exact URL to download by hand. `commons-compress`
is pulled from Maven Central to unpack the `.tar.bz2` voice pack, because Android
has no bzip2.

**Voice pack.** Downloaded once, on the first call, from sherpa-onnx's `tts-models`
release: `vits-piper-en_US-amy-medium-int8.tar.bz2` (20 MB — the same voice as the
64 MB fp32 build), unpacked into `filesDir/piper/`, and never touched again. The
model, `tokens.txt` and `espeak-ng-data/` are *found by walking the folder*, not by
assuming filenames, so a different Piper voice is a one-constant change.

**Playback.** `data/voice/PiperNarrator.kt` synthesises each sentence to a
temporary wav and plays it with a `MediaPlayer`, one after another, on a single
worker thread. Callbacks (`onStart`, `onSentenceDone`, `onError`) always arrive on
the main thread, and a token counter makes [stop] instant: an interrupt bumps the
token, the worker walks away and the player is released.

**The call loop is unchanged.** `speakTutorSentences` asks the narrator first and
falls back to the system engine when it is not ready — before the download
finishes, on a device where the native library will not load, or if a line fails
mid-answer. The same `tutorSpeechSeq` guard, the same
`pendingTutorSentences` countdown and the same `onTutorSpeechFinished` hand-back
apply to both engines, so barge-in still works exactly as before.

**Known trade-off:** sentences are synthesised one at a time, so there is a small
pause after each one instead of a seamless stream. Prefetching the next sentence
while the current one plays is the obvious next improvement.
