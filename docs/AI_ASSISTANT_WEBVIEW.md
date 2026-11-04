# AI Assistant WebView

Why the AI chat screen felt like a website in a box, what was changed, and how to
check it again. Written 2026-11-04.

The screen hosts `https://chatbot-liart-rho-81.vercel.app/` — a separate Vercel
project, **not in this repository**. Everything below is therefore native-side:
the app injects the "native feel" layer itself, so nothing has to be redeployed
on the web for the improvements to land. [The last section](#optional-web-app-patches)
lists the handful of things that genuinely belong in the web app.

---

## What was actually wrong

### 1. The WebView was destroyed on every visit (the big one)

```kotlin
AndroidView(factory = { … WebView(ctx) … loadUrl(URL) },
            onRelease = { … child.destroy() … })
```

Every exit from the screen threw away the whole renderer. Coming back paid for a
cold Chromium start, a fresh DNS + TLS handshake, and a full JavaScript boot of
the chat app — every single time. That is the entire reason it felt like opening
a browser tab instead of a screen.

**Fixed** by moving ownership out of the composable into `AiWebSession`, which
owns one `WebView` and a stable `FrameLayout` host. `AndroidView` now just
re-parents that host:

```kotlin
AndroidView(factory = { session.attach() }, onRelease = { session.detach() })
```

`detach()` pauses the page; `attach()` resumes it. Scroll position, the open
conversation and a streaming answer all survive.

### 2. Nothing was warm when the student tapped the AI button

**Fixed** by `AiWebSessionManager.prewarm()`, scheduled 450 ms after
`MainActivity.onCreate` (only when a session token exists). It builds the
`WebView` and starts loading while the dashboard is still on screen, plus fires a
background `HEAD` request to warm DNS + TLS.

### 3. The loader waited for `onPageFinished`

`onPageFinished` fires after *every* sub-resource is done — on a React SPA that
is long after the UI is usable. The screen also never set `isPageLoading` back to
`true`, so a reload showed no feedback at all.

**Fixed**: the skeleton is dropped on `onPageCommitVisible` (first painted
frame), or earlier if the injected script reports `notifyReady()`. State is reset
on `onPageStarted`.

### 4. The loading state was a spinner on a black rectangle

Nothing says "WebView" louder.

**Fixed** with `AiChatSkeleton` — the chat screen's own silhouette (header,
bubbles, composer) in the web app's exact palette, cross-fading into the page.

### 5. Lifecycle was ignored

`onPause()` / `onResume()` / `pauseTimers()` were never called, so a backgrounded
chat page kept animating, polling and streaming behind the lock screen.

**Fixed**: `detach()` → `onPause()`, `MainActivity.onStop` → `pauseTimers()`,
`onStart` → `resumeTimers()`, cookies flushed on pause.

`pauseTimers()` / `resumeTimers()` are resolved reflectively: they are public
platform API, but the class stub this project compiles against (`compileSdk 36`)
does not expose them. `AiWebSession.setTimersPaused()` handles static and
instance forms and no-ops if the platform has neither.

### 6. Offline was a dead end

One generic message and a manual Retry, with `cacheMode` pinned to `LOAD_DEFAULT`.

**Fixed**: the first main-frame failure automatically retries once from the HTTP
cache (`LOAD_CACHE_ELSE_NETWORK`); a `ConnectivityManager` callback reloads when
the network comes back; and the overlay now distinguishes offline / timeout /
server error with different copy.

### 7. Back could not close what was open on screen

`BackHandler` only checked `canGoBack()`, so pressing back with the web sidebar or
a dialog open exited the whole screen.

**Fixed**: the injected script watches for open overlays
(`[aria-modal]`, `[role=dialog][data-state=open]`, `[data-ts-drawer="open"]`, …)
and reports through the bridge. Back now closes the page's own overlay first,
then walks history, then leaves.

### 8. Everything the OS does for free was missing

No haptics, no theme sync, no share/copy, tap highlights, visible scrollbars,
rubber-band overscroll, `target="_blank"` links doing nothing, and a file picker
with no way to photograph a question.

**Fixed** by the bridge additions plus `assets/webview/ai_native_feel.{js,css}`.

---

## How it works now

```
MainActivity.onCreate ──(450 ms)──► AiWebSessionManager.prewarm()
                                        │  builds WebView + loadUrl + TLS warm-up
                                        ▼
                              AiWebSession (one per activity)
        ┌───────────────────────────────┼───────────────────────────────┐
        │                               │                               │
   AndroidView(factory)           WebViewClient                   AiWebBridge
   session.attach()               page state → Compose            window.TheStudentAndroid
   session.detach()               injects native feel             token · haptics · theme
        │                          onPageCommitVisible             share · back · ready
        ▼
   kept warm for 15 min, dropped on logout / renderer crash / trim memory
```

### Files

| File | Role |
|---|---|
| `ui/webview/AiWebConfig.kt` | URL, origin locks, TTL, asset paths, `AiWebError` |
| `ui/webview/AiWebSession.kt` | The `WebView`, its clients, state, attach/detach/destroy |
| `ui/webview/AiWebBridge.kt` | `window.TheStudentAndroid` — origin-gated, thread-safe |
| `ui/webview/AiWebSessionManager.kt` | Singleton, prewarm, TTL, trim-memory, logout |
| `ui/components/AiChatSkeleton.kt` | Loading silhouette |
| `assets/webview/ai_native_feel.js` | Back, haptics, keyboard, links, ready signal |
| `assets/webview/ai_native_feel.css` | Tap highlight, scrollbars, overscroll, text-size |
| `ui/webview/AiSpeechRecognition.kt` | Native speech-to-text behind the polyfill |
| `ui/webview/AiTextToSpeech.kt` | Native text-to-speech behind the polyfill |
| `assets/webview/ai_speech.js` | `SpeechRecognition` + `speechSynthesis` polyfills |

### Memory policy

The renderer is the most expensive thing the app keeps off screen, so it is
dropped when:

- the account changes or logs out (`releaseForLogout`),
- the renderer crashes (`onRenderProcessGone` → rebuild on next visit),
- the session has been detached for 15 minutes (`KEEP_ALIVE_MS`),
- the process reports `TRIM_MEMORY_RUNNING_CRITICAL` / `MODERATE` / `COMPLETE`.

It is also released with its Activity (`onDestroy`), because a `WebView` holds a
`Context`: a rotation reloads the page. That is deliberate — the alternative is
leaking an Activity or showing popups against a dead window token.

---

## Speech: why "talk" mode was dead in the app

Android's System WebView implements **no** Web Speech API. Not a partial one — none:

| API | Chrome (desktop/PWA) | Android WebView |
|---|---|---|
| `window.webkitSpeechRecognition` | ✅ | ❌ `undefined` |
| `window.speechSynthesis` | ✅ | ❌ `undefined` |

So `new webkitSpeechRecognition()` throws `TypeError` inside the app while the same
page works perfectly in a browser, and the voice recorder (`getUserMedia` +
`MediaRecorder`) fails silently when the microphone runtime grant is missing.

**Fixed** by rebuilding both interfaces on the native engines and injecting them on
every page load. Existing page code keeps working unchanged:

```js
const rec = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
rec.lang = 'en-US';
rec.onresult = e => console.log(e.results[0][0].transcript);
rec.start();
```

### Bridge surface

| `window.TheStudentAndroid` | Backed by |
|---|---|
| `isSpeechRecognitionAvailable()` | `SpeechRecognizer.isRecognitionAvailable` |
| `startSpeechRecognition(lang, continuous)` | `SpeechRecognizer.startListening` |
| `stopSpeechRecognition()` | `stopListening` (final transcript still arrives) |
| `cancelSpeechRecognition()` | `cancel` |
| `speakText(text, lang)` / `stopSpeaking()` | `TextToSpeech` |
| `hasAudioPermission()` / `requestAudioPermission()` | `RECORD_AUDIO` runtime grant |

Native events are pushed back into the page as
`window.__tsSpeech.onNativeEvent(type, payload)` with `start`, `audiostart`,
`speechstart`, `speechend`, `result`, `nomatch`, `error`, `end` — the same names the
Web Speech API uses. Errors are mapped onto its vocabulary (`no-speech`, `aborted`,
`not-allowed`, `network`, `audio-capture`, `service-not-allowed`).

### Details that matter

- **Permission first.** Tapping the mic with no `RECORD_AUDIO` grant asks once and
  then resumes the recognition it was going to start (`pendingSpeechStart`), instead
  of throwing an error the page has to interpret.
- **`continuous` is emulated.** The platform recogniser stops after a pause; on a
  final result in continuous mode the session restarts itself after 220 ms, which is
  long enough for the service to release the microphone.
- **`interimResults` is honoured** — partial results are dropped in the polyfill when
  the page did not ask for them.
- **No `onRmsChanged` forwarding.** It fires ~10×/second; forwarding it would mean ten
  `evaluateJavascript` calls a second, which is the cost this screen removed.
- **Manifest.** Android 11+ hides `RecognitionService` from the app unless it is
  declared in `<queries>`, so `isRecognitionAvailable()` returns false without it.
  The manifest now declares `android.speech.RecognitionService` and
  `android.intent.action.TTS_SERVICE`, plus
  `<uses-feature android:name="android.hardware.microphone" android:required="false" />`.

### Voice messages (`getUserMedia` + `MediaRecorder`)

That path was already handled by `onPermissionRequest` granting
`RESOURCE_AUDIO_CAPTURE`; what was missing was a way for the page to ask *before*
recording. `hasAudioPermission()` / `requestAudioPermission()` cover it, and the
page can now prompt on the mic tap rather than getting an opaque failure.

### Testing without a phone

`node scripts/test_speech_polyfill.js` stubs the bridge, drives the native events by
hand and asserts the whole surface: partial → final results, single `end`, the error
path, `abort()` detaching the recogniser, and the TTS start/end cycle.

---

## Verifying it

1. `adb shell setprop log.tag.AiWeb DEBUG` and watch for `onRenderProcessGone`.
2. Enter the AI screen → leave → re-enter. The second entry should show **no
   skeleton at all**: the page is already painted.
3. Turn on flight mode, cold-open the screen: skeleton → offline card with the
   "You're offline" copy. Turn flight mode off: it reloads by itself.
4. Open the web sidebar, press system back: the sidebar closes and the screen
   stays.
5. Focus the composer with the keyboard closed, then open the keyboard: the
   composer stays above it, no scroll jump.
6. `chrome://inspect` against the debug build shows the injected style element
   `#ts-native-feel` in `<head>`.
7. In `chrome://inspect`'s console, `typeof window.webkitSpeechRecognition` must be
   `"function"` and `typeof window.speechSynthesis` must be `"object"`.
8. Tap the site's mic: the Android microphone prompt appears once, then the "listening"
   UI runs and the transcript lands in the composer.

---

## Deliberately not done

- **`androidx.webkit`.** It would add back-forward cache, algorithmic darkening
  and `addWebMessageListener`. The page is dark-only and SPA-navigated (so BF
  cache rarely applies), and the message channel needs a web-side change. Not
  worth a new dependency yet — see the note on the bridge below.
- **`setLayerType(LAYER_TYPE_HARDWARE)`.** Known to render black on some Samsung
  builds; see `docs/COURSES.md`.
- **`pauseTimers()` on detach** (as opposed to on app background). It is
  process-wide, so it would freeze any other visible `WebView`.

## Known residual risk

`addJavascriptInterface` exposes the bridge to every frame of the page, including
third-party iframes the web app might embed. `getSessionToken()` is gated on the
main frame's host, but a same-page iframe could still call it. The token is
already only readable by script on the trusted origin, so the exposure is small.
The clean fix is `WebViewCompat.addWebMessageListener` with
`allowedOriginRules = setOf(AiWebConfig.TRUSTED_ORIGIN)`, which needs a matching
change on the web side.

---

## Optional web app patches

These are the only improvements that cannot be made from Kotlin. All optional.

1. **Tell the shell when the drawer opens** (makes back exact instead of
   heuristic):
   ```jsx
   <aside data-ts-drawer={open ? "open" : undefined}> … </aside>
   ```
2. **Opt into theme variables** already injected as `--ts-accent`:
   ```css
   :root { --accent: var(--ts-accent, #10b981); }
   ```
3. **Use `dvh` for full-height chat layouts** (`100dvh` instead of `100vh`) so
   the composer does not jump when the on-screen keyboard shrinks the viewport.
4. **Explicit close buttons** with `aria-label="Close"` on modals — the injected
   back handler looks for them before falling back to a synthetic Escape.
5. **Explicit bridge calls** where the DOM is ambiguous:
   ```js
   window.TheStudentAndroid?.notifyRoute(open ? 'open' : 'idle');
   window.TheStudentAndroid?.haptic();
   ```
6. **Long-press actions** (`copy`, `share`) can call
   `window.TheStudentAndroid.copyText(md)` / `.share(md, title)` for native
   clipboard and share-sheet behaviour.
7. **Create speech objects lazily.** The polyfill is injected when the page loads,
   which can be *after* a bundled module has already evaluated
   `const SR = window.webkitSpeechRecognition` (capturing `undefined`). Resolve it
   where it is used instead:

   ```js
   function createRecognition() {
     const Ctor = window.SpeechRecognition || window.webkitSpeechRecognition;
     return Ctor ? new Ctor() : null;   // null → show the "type instead" state
   }
   // call inside the mic click handler, not at module scope
   ```

   Same for `speechSynthesis`: read `window.speechSynthesis` at call time.
   If you want zero ambiguity, skip the polyfill entirely and call
   `window.TheStudentAndroid?.startSpeechRecognition(lang, continuous)` directly,
   then listen for `window.__tsSpeech.onNativeEvent(...)`.
