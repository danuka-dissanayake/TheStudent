# Making the tutor hear students properly (plain-English guide)

This is the write-up of the question: *"Sometimes I talk for a long time and the
message shows different words than I said, and short words/terms come out wrong.
Do we need a library or an AI model?"*

**Short answer:** No new library, no new model, no API key, and no cost were
needed for the long-message bug. That one was caused by *how the app used* the
phone's built-in speech recogniser. Short words and special terms get help from
features already built into Android 13+, plus a new "check what I heard" step.
A better STT engine is a **Phase 2** decision, not a requirement for this fix.

---

## 1. What the app was doing

The app uses Android's built-in `SpeechRecognizer` — the phone's own dictation
engine. It is designed for **one short utterance**: it owns the microphone,
returns one best guess after a pause, then stops. Android explicitly says the
API is not intended for continuous recognition [1](https://iut-fbleau.fr/docs/android/reference/android/speech/SpeechRecognizer.html).

To let a student talk longer, the app restarted the recogniser over and over and
glued the returned text together by matching repeated words. It kept **only the
single best guess**, was locked to `en-US`, and set silence hints that Android
says should almost never be set outside a segmented session [2](https://developer.android.com/reference/android/speech/RecognizerIntent).
The finished words were punctuated, displayed in a read-only chat bubble, and
then treated as fact by the tutor.

Nothing in the app was *rewriting* the student's words. The phone's engine was
guessing them.

## 2. Why a long answer came back with different words

1. **Every restart was a fresh, context-free guess.** Each chunk was transcribed
   from scratch with no memory of the words before it, so a phrase could flip to
   a similar-sounding, more common phrase. This restart-and-glue pattern is a
   known source of dropped and duplicated words [3](https://github.com/alvarolorentedev/opencode-mobile/pull/77).
2. **The glue was text-only**, with no audio timing. If the seam was heard
   differently in two segments, words were dropped, doubled or garbled — and a
   wrong word already written into the buffer stayed wrong.
3. **Only one guess was kept.** No alternate candidates, no confidence scores:
   the app literally could not notice the engine was unsure.
4. **Cut-offs varied by phone.** The four-second silence hints were set on every
   session; engines may ignore them, so some devices cut a sentence mid-clause.
   Live partial text also morphs into a different final text, which by itself
   looks like "the words changed" [4](https://www.voice.ai/hub/ai-voice-agents/android-speech-to-text-api/).
5. **`en-US` was hard-coded**, and generic models are measurably worse on
   accented / second-language speech (see `docs/AI_TUTOR_RESEARCH.md`).
6. **A 30-second ceiling** force-sent whatever was in the buffer.
7. **The bubble could not be edited**, so a machine error became a "fact" that
   the tutor might even correct.

## 3. Why short words and special terms are hard

Words like *a, an, the, to, did, it's, can* are tiny, unstressed and blended into
their neighbours. The language model then "smooths" them into the most probable
fluent sentence. Research consistently finds short, reduced, high-frequency
function words are the most misrecognised words, and substitutions are the most
common error type [5](https://theses.eurasip.org/wp-content/uploads/wepner-saskia-when-does-it-harm-to-be-incomplete-comparing-human-and-automatic-speech-recognition-of-syntactically-disfluent-utterances.pdf)
[6](https://nhsjs.com/2025/evaluating-the-accessibility-of-automatic-speech-recognition-technology-across-accents/).
Lesson terms, place names and subject vocabulary are rare in a general model,
so they get swapped for familiar-sounding words.

Even the best systems are around **95–98% word accuracy on clean audio**, and
worse on accents and noise [7](https://www.assemblyai.com/blog/how-accurate-speech-to-text).
So "perfect" is not a model you can install. It is a stack of small wins plus an
honest way to fix the last few words.

---

## 4. What changed now (Phase 1 — free)

No new dependency, no network audio, no API key. Voice still never leaves the
phone for transcription.

| Change | Why it helps |
| --- | --- |
| **One continuous session on Android 13+** (`EXTRA_SEGMENTED_SESSION` + `onSegmentResults`) | Long answers no longer need the restart-and-glue loop. Android delivers each finished stretch itself; the first syllable after a restart is no longer lost. Older phones keep the restart path automatically. |
| **Smarter seam joining** (`TranscriptReview.mergeSegments`) | Handles a repeated tail, a *cumulative* final (some engines re-send the whole turn), case/punctuation differences, and deliberately repeated speech ("no, no, no" is not a stitching artefact). |
| **All guesses, not just one** (`EXTRA_MAX_RESULTS = 4` + `CONFIDENCE_SCORES`) | The app can pick a better guess and notice when the engine was unsure. It never invents a word — it only chooses between words the engine actually returned. |
| **Vocabulary hints** (`EXTRA_BIASING_STRINGS`, API 33+) | Lesson sentences, the tutor's last question, scenario vocabulary and the student's own terms are pushed into the engine so short/rare words are expected. On older phones the same list helps choose between guesses [8](https://developer.android.com/reference/android/speech/RecognizerIntent). |
| **Accent setting** (`en-US / en-GB / en-IN / en-AU`) | A Sri Lankan learner is no longer forced into a US model. If a locale is not installed, the app quietly falls back to `en-US`. |
| **Built-in punctuation** (`EXTRA_ENABLE_FORMATTING`, API 33+) | The engine punctuates instead of the app guessing where a sentence ends. |
| **Silence hints removed** | Android calls them "extremely rare" outside a segmented session. The app now owns endpointing with its own 2 s / 4.6 s adaptive pause. |
| **"Did I hear you right?" card** | When confidence is low **or** two guesses disagree on a meaning-changing short word, the sentence is held, shown in an editable box, and only sent after the student taps **Send these words**. **Say it again** discards it. |
| **No grading of uncertain words** | No local correction card, no model `FIX`, and the prompt explicitly forbids recasts when the transcript is not trusted. A tutor must never correct a word the microphone invented. |
| **30 s ceiling always applies** | Even if an engine never returns a segment, the turn still ends. |

New settings live in the tutor's **⋯ → Speech settings**: pick the English
variety, and type your own words separated by commas (e.g. `Kandy, CEFR, past
participle`). Custom words are cleared on sign-out because they can be personal.

## 5. What this does *not* promise

- It cannot make the phone's own model hear a word it has no model for. If a
  handset has a weak recogniser, the app now **shows** the uncertainty and asks,
  instead of silently guessing.
- `EXTRA_BIASING_STRINGS` and segmented sessions are *optional* engine features:
  "depending on the recognizer implementation, this value may have no effect."
  Every new feature has a working fallback for older/other phones [2](https://developer.android.com/reference/android/speech/RecognizerIntent).
- Word-level confidence (Android 14+) is not used yet. Confidence is often
  missing entirely, so the current logic relies more on disagreement between
  guesses — which works on every API level.
- No cloud service, no per-minute cost, no APK size increase, no new key.

## 6. Phase 2, if real-phone testing still shows errors

Only consider this **after** testing Phase 1 with actual students. Adding a
model on top of a broken listening loop would still show wrong words.

| Route | What it gives | Trade-offs |
| --- | --- | --- |
| **Cloud streaming STT** (Deepgram / AssemblyAI / Google Cloud / Azure) | One live session, word-level confidence and timestamps, **keyword boosting** for lesson terms, generally the best accent handling [9](https://deepgram.com/blog/best-speech-to-text-apis) | Internet required, per-minute cost, voice leaves the phone (consent), a key must be minted server-side — your `.env.example` already warns that a key inside the APK is not secret. The current PHP backend cannot do real-time audio streaming, so this needs a small extra service. |
| **On-device model** (`sherpa-onnx` streaming Zipformer ~40–80 MB, Vosk ~40 MB, Whisper tiny/base) | Offline and private, no per-use cost [10](https://dev.to/voxrtio/on-device-streaming-asr-in-2026-five-approaches-and-when-to-pick-what-5h1n) | Bigger APK, CPU/battery, weaker on rare terms. Whisper is *batch* (waits until you stop) and chunked versions have word-at-border artefacts [11](https://dev.to/voxrtio/streaming-asr-vs-whisper-on-mobile-when-to-switch-5cm7). |
| **An LLM "rescorer"** | Can rank the engine's guesses using the tutor's last question | Must **never** silently invent words the student did not say. Candidates only, or ask the student. A general chatbot LLM cannot hear audio — it is not an STT engine. |

**Recommendation if Phase 2 is needed:** cloud streaming with keyword boosting
(best for L2 accents), with the built-in recogniser kept as the offline/fast
fallback. Only use an on-device model if offline/privacy becomes a hard
requirement.

## 7. How to check it on a real phone

The sandbox has no JDK/Android SDK, so Gradle compile and on-device tests could
not be run here. On a phone or in Android Studio:

1. Talk for 20–30 seconds with two natural pauses → the bubble should contain
   the whole answer, with no repeated tail or missing first word after a pause.
2. Say a sentence full of little words ("I did go to the shop and I did see
   him") → check the live text and the final bubble agree.
3. Say a lesson word / a home town name → open **⋯ → Speech settings**, add it
   to "Words she should recognise", and try again.
4. Switch accent to **India** or **UK** and compare on your own voice.
5. Force a hard case (noisy room, fast speech). If a short word is doubtful, the
   **Did I hear you right?** card should appear; edit it, tap **Send these
   words**, and confirm no grammar card is shown for the word you fixed.
6. On an Android 12 or older phone, confirm long answers still work via the
   restart fallback (the new segmented-session path is Android 13+ only).

Unit tests added: `TranscriptReviewTest` (guess choice, seam joining, hint
building) and a new `TutorTurnPromptTest` case for the no-correction prompt.

---

### Sources

1. [Android `SpeechRecognizer` reference — not intended for continuous recognition](https://iut-fbleau.fr/docs/android/reference/android/speech/SpeechRecognizer.html)
2. [Android `RecognizerIntent` reference — biasing strings, segmented sessions, formatting, silence hints](https://developer.android.com/reference/android/speech/RecognizerIntent)
3. [Real-world fix for dictation losing words at every recogniser pause](https://github.com/alvarolorentedev/opencode-mobile/pull/77)
4. [Android STT field notes — partial results morphing, short utterances](https://www.voice.ai/hub/ai-voice-agents/android-speech-to-text-api/)
5. [Dissertation: short, reduced function words are misrecognised most](https://theses.eurasip.org/wp-content/uploads/wepner-saskia-when-does-it-harm-to-be-incomplete-comparing-human-and-automatic-speech-recognition-of-syntactically-disfluent-utterances.pdf)
6. [ASR across accents — short single-syllable words and substitution errors](https://nhsjs.com/2025/evaluating-the-accessibility-of-automatic-speech-recognition-technology-across-accents/)
7. [How accurate is speech-to-text in 2026](https://www.assemblyai.com/blog/how-accurate-speech-to-text)
8. [Android `RecognitionListener` — `onSegmentResults` / `onEndOfSegmentedSession` (API 33)](https://developer.android.com/reference/android/speech/RecognitionListener)
9. [Best speech-to-text APIs — keyword boosting](https://deepgram.com/blog/best-speech-to-text-apis)
10. [On-device streaming ASR in 2026 — model sizes](https://dev.to/voxrtio/on-device-streaming-asr-in-2026-five-approaches-and-when-to-pick-what-5h1n)
11. [Streaming ASR vs Whisper on mobile — chunk border artefacts](https://dev.to/voxrtio/streaming-asr-vs-whisper-on-mobile-when-to-switch-5cm7)
