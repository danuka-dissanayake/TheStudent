# TheStudent AI inside the app — WebView assistant

The AI assistant website is shown **inside the app**, in a native-framed
`WebView`, reached from a floating button on every main screen except Chats.
Students never see a URL, a browser, or a redirect.

---

## 1. What was built (Android side)

| File | Job |
| --- | --- |
| `ui/screens/WebAssistantScreen.kt` | Full-screen Compose screen with an app-owned header, native loading/error/offline states, back handling, domain lock and file upload |
| `ui/components/AssistantFab.kt` | The one app-wide floating button |
| `data/local/WebAssistantConfig.kt` | Pure URL rules: HTTPS-only base URL, host allowlist, `?app=android` page, identity script |
| `data/local/WebAssistantSession.kt` | Cookies on, third-party cookies off, flush on close, **clear on sign-out** |
| `TheStudentViewModel.kt` | `isWebAssistantOpen`, `webAssistantBaseUrl`, `webAssistantInitialPrompt`, `openWebAssistant()`, `closeWebAssistant()`, `setWebAssistantBaseUrl()` |
| `MainActivity.kt` | Renders the screen, adds the FAB overlay with visibility rules, disables the global back handler while open |
| `WebAssistantConfigTest.kt` | JVM tests for the URL rules |

No new dependency was added. `WebView` + Compose `AndroidView` is all it takes.
`android.permission.INTERNET` was already in the manifest and
`usesCleartextTraffic="false"` is fine because Vercel is HTTPS.

**Entry points now all open the website:** sidebar drawer → AI Assistant,
"Ask AI" on a past paper (its question is passed as `?q=`), and the floating
button. The old 4,526-line native `AiAssistantScreen` is still in the tree but
unreachable; it is queued for a later cleanup pass.

### Where the FAB appears

```
screenFlow == MAIN
  && currentTab != CHAT                 // chat list AND conversation excluded
  && !isChatConversationOpen
  && !isWebAssistantOpen                // not on its own screen
  && no full-screen page / dialog open  // EXP, Progress, Account, Admin,
                                        // Language Lab, Academic setup, Auth,
                                        // paper/course details
  && callPhase != LIVE && != CONNECTING // never covers the tutor's mic
```
It sits above the bottom navigation, not on top of it.

---

## 2. Why it feels like an app screen

- **Native frame:** app top bar ("TheStudent AI" / "Your study assistant"),
  app colours and `MaisonNeue`, status-bar and navigation-bar padding, native
  back arrow.
- **No browser cues:** no URL bar (WebView has none), no zoom controls, no
  overscroll glow, no scrollbars, no `target="_blank"` windows
  (`setSupportMultipleWindows(false)`), downloads and external links are
  consumed with a native snackbar.
- **Domain lock:** `shouldOverrideUrlLoading` returns `false` only for
  `https://<configured host>`; everything else is blocked. `/terms` and
  `/privacy` stay inside because they are the same host.
- **Back button:** `webView.canGoBack()` → `goBack()`, otherwise close. The
  global "return to Home" `BackHandler` is disabled while the screen is open.
- **"New chat" button:** the website hides its sidebar in embed mode, which also
  hides its "New chat" control. The native header has its own `+` button that
  reloads the clean embed URL (no `?q=`, no stale chat).
- **Native states:** indeterminate progress bar while loading, a branded
  "TheStudent AI is unavailable / Try again" card on a main-frame error or SSL
  failure instead of Chrome's `net::ERR_...` page.
- **Working web features:** JavaScript and DOM storage on; `<input type=file>`
  works through `WebChromeClient.onShowFileChooser` + an Activity result picker
  (needed for the site's "Summarize document" tool). Microphone requests are
  granted only if the app already holds `RECORD_AUDIO`; everything else is
  denied.
- **Lifecycle:** the WebView is created per page URL, removed from its parent
  and `destroy()`d on dispose, cookies flushed. Chat state survives closing and
  reopening through cookies/localStorage.

---

## 3. ⚠ Change this URL before shipping

`WebAssistantConfig.DEFAULT_BASE_URL` is currently

```
https://chatbot-liart-rho-81.vercel.app/
```

That matches Vercel's `project-{hash}-{scope}.vercel.app` **immutable
deployment URL** pattern. It will keep working, but it will **never show a newer
build** — every site update you make would be invisible to installed apps.
Use a production domain (`chatbot.vercel.app`) or your custom domain.

Two ways to change it without a full rebuild:

1. **Build time:** change `DEFAULT_BASE_URL` in `WebAssistantConfig.kt`.
2. **Runtime:** call `viewModel.setWebAssistantBaseUrl("https://your.domain/")`
   from a config response (for example from `verifySessionWithBackend()`), or
   add a tiny `backend/api/assistant_config.php` returning `{"url":"..."}` and
   call the setter with it. The value is stored in
   `TheStudentPreferences.webAssistantBaseUrl` and read on the next cold start.
   Only HTTPS is accepted; invalid values fall back to the default.

---

## 4. Contract for the website (your other repo)

The app loads:

```
https://<your-domain>/?app=android&theme=dark|light&q=<url-encoded question>
```

### 4.1 Embed mode — `?app=android`

On the site, read the query once and set an `isEmbedded` flag. When true:

- Hide your own header, sidebar toggle, sidebar, footer, "Download the app" /
  PWA install banners and any "open in new tab" affordances.
- Use a phone-first single-column layout with comfortable bottom padding — the
  native header above already says "TheStudent AI".
- Match the app theme from `theme=dark|light`.
- If `q=` is present, prefill the composer with it (do **not** auto-send).
- Keep all links same-origin. Any link to another host is blocked by the app;
  Terms and Privacy are fine because they are on your own domain.
- Do **not** use Google/Facebook OAuth popups inside the WebView — they do not
  work reliably there. Use email/password, or accept a session cookie issued by
  your backend. If you need social login, we would add a Custom Tab auth step
  and sync the cookie back.

### 4.2 Student identity — now real single sign-on

**Signed-in students are no longer identified by an injected JS object.** See
`docs/ASSISTANT_SSO.md` for the full contract. In short: the app asks our PHP
backend for a 90-second, single-use code, POSTs it in a request **body** to the
website's `/api/app-auth` route, and the website's server exchanges it with our
backend, provisions the account by the opaque `sub` (`users.id`) and sets its own
`HttpOnly; Secure; SameSite=Lax; Path=/` session cookie. No app token, password
or reusable credential ever reaches this WebView.

The old `THESTUDENT_USER` injection below is now a **guest-mode fallback only**:
it runs when no code could be minted (offline / backend not configured). In that
case `userId` is an opaque numeric id, **not** an email.

```js
(function () {
  var u = {
    type: 'THESTUDENT_USER',
    source: 'android',
    name: '...',       // display name
    userId: '...',     // opaque users.id, never an email or token
    theme: 'light'     // or 'dark'
  };
  window.__THESTUDENT_USER__ = u;
  window.postMessage(u, window.location.origin);
})();
```

On the site, use `name` for display only. **Never** use `userId` from JavaScript
to grant access or to key the real account — the cookie is the only authentication
source. Do not put a secret in the URL. Do not use `addJavascriptInterface`.

### 4.3 Site → app messages (implemented, origin-pinned)

The website may call, from the **main frame on the exact HTTPS production origin**:

```js
if (window.TheStudentBridge) {
  window.TheStudentBridge.onmessage = e => console.log(e.data); // {"ok":true}
  window.TheStudentBridge.postMessage(JSON.stringify({ type: 'SESSION_EXPIRED' }));
}
```

Android uses `WebViewCompat.addWebMessageListener` with an exact origin
allowlist, a `WEB_MESSAGE_LISTENER` feature gate, a main-frame + scheme + host
re-check, a 4 KB size cap and an explicit event allowlist (`SESSION_READY`,
`SESSION_EXPIRED`, `NEW_CHAT`, `CLOSE`, `OPEN_PAPERS`, `SHARE_TEXT`). Unknown
types and iframe messages are ignored; unsupported WebViews get **no bridge at
all** (fail closed). There are no credential read/write events.
`addJavascriptInterface` is never used.

### 4.4 Example embed-mode sketch (React / Next.js)

```tsx
// app/embed.ts — call once in your root layout
export function useAndroidEmbed() {
  const params = new URLSearchParams(window.location.search);
  const embedded = params.get('app') === 'android';
  const theme = params.get('theme') === 'dark' ? 'dark' : 'light';
  const initialQuestion = params.get('q') ?? '';
  return { embedded, theme, initialQuestion };
}

// In the layout:
//   {!embedded && <SiteHeader />}
//   {!embedded && <Sidebar />}
//   <Chat initialQuestion={initialQuestion} />
//   {!embedded && <Footer />}
```

---

## 5. Privacy / Play Store notes

- On **sign-out**, `WebAssistantSession.clear()` removes all WebView cookies and
  web storage, so the next student on a shared phone cannot see the previous
  student's chats. Tutor memory and speaking stats are cleared in the same place.
- Student questions go to **your website**, not only to the Gemini/Z.AI paths
  already in the app. Update the app's privacy policy and Play Data safety form
  accordingly; the site's own Terms/Privacy links still open inside the app.
- Google Play's "minimum functionality" rules can reject a bare website wrapper.
  Here the WebView is one feature of a large native app, with a native frame,
  back handling, error/offline states, file upload and session handling — that
  native integration is what keeps it compliant.

---

## 6. Test checklist on a real phone

1. Tap the floating button on Home / Papers / Profile → a full-screen app screen
   opens, no URL bar, header says "TheStudent AI".
2. Tap it on the **Chats** tab (list and a conversation) → the button is gone.
3. Start a tutor call → the button is gone; after hanging up it returns.
4. Open a paper → "Ask AI" → the website opens with the question prefilled.
5. Tap Terms / Privacy → stays in the app. Tap any external link → snackbar, no
   Chrome.
6. Press system back → walks the site history, then closes. Native back arrow
   closes immediately.
7. Turn on flight mode → native "unavailable / Try again" card, not a Chrome
   error page.
8. Use "Summarize document" → Android file picker opens and the upload works.
9. Close and reopen → previous chats are still there (cookies/localStorage).
10. Sign out, sign in as another student → no leftover chat from the first one.
11. Change the URL with `setWebAssistantBaseUrl()` → the new domain loads, and a
    non-HTTPS value is rejected.

## 7. Still to do later

- Delete the now-unreachable native `AiAssistantScreen` and its state/clients
  (kept in this pass on purpose).
- Add a backend config endpoint so the domain can change remotely.
- Full OIDC identity provider ("Sign in with TheStudent" for third parties) —
  deliberately deferred; today's first-party code exchange is enough.
- Optional: Custom Tab auth for social login on the *browser* site, offline
  caching / native share sheet.

**Already done:** app ↔ web single sign-on and the origin-pinned site → app
bridge. Setup, secrets, endpoint contract, the copy-paste prompt for the website
repo and the phone test checklist are in **`docs/ASSISTANT_SSO.md`**.


---

## 8. Website implementation review (compatibility notes)

The website now ships the `?app=android` contract. What matches the Android
side:

| Website | Android side | Verdict |
| --- | --- | --- |
| Detects `?app=android` **or** UA containing `TheStudentAndroid/` | UA is appended with ` TheStudentAndroid/1.0` | ✅ both triggers work |
| Theme from `theme=` applied before first paint, also from user data | Sends `theme=dark|light` in the URL and in `__THESTUDENT_USER__` | ✅ |
| `q` prefills the composer, never auto-sent, removed from the address | Sends `q` from "Ask AI" on a paper; `key(pageUrl)` does not change on `history.replaceState` | ✅ if `history.replaceState` is used |
| Reads `window.__THESTUDENT_USER__` at startup + `message` listener with origin/type checks | Injects on `onPageFinished` and retries once after 700 ms **only if the value is gone** | ✅ |
| External links copied, downloads/popups blocked with a toast, download buttons hidden | `shouldOverrideUrlLoading` consumes any navigation that slips through; `setDownloadListener` shows a native message | ✅ (double handling is harmless) |
| Plain `<input type="file">` | `WebChromeClient.onShowFileChooser` + Activity result picker | ✅ |
| Guest sign-in redirect preserves the full query | `onPageFinished` fires on the final page and injects identity there | ✅ |

### Rules the website must keep (sent back to the other agent)

1. **Deduplicate identical `THESTUDENT_USER` payloads.** The website signs its
   web session out when `userId` changes. That state must be persisted in
   `sessionStorage` (or a cookie), and the account switch must NOT be a full
   page reload — our WebView injects identity on *every* `onPageFinished`, so a
   reload + sign-out + reload would loop. Our side now suppresses the duplicate
   post on the same page, but the site still needs the persisted comparison.
2. **Remove `q` with `history.replaceState`, not `location.replace`** (or any
   real navigation). A real navigation fires `onPageFinished` again, re-injects
   identity and can re-run guest sign-in.
3. **`navigator.clipboard.writeText` is unreliable in Android WebView.** Add a
   `document.execCommand('copy')` fallback, or the "Link copied" toast will be
   a lie on some devices. Until a native bridge exists there is no site → app
   clipboard channel.
4. **Hide voice controls in embed mode** (same as download buttons).
   `webkitSpeechRecognition` is not implemented in Android WebView. Our
   `onPermissionRequest` grants the microphone only when the app already holds
   the runtime `RECORD_AUDIO` permission — which the AI Tutor screen requests,
   so a student who never used the tutor would be denied anyway.
5. **Keep a "New chat" affordance** (ours is native now) and keep `/terms` and
   `/privacy` as same-origin routes.

### Two product decisions

- **Per-account chats (Secure/HttpOnly cookie):** recommended, and it needs
  **no Android changes** — first-party cookies are already accepted and
  `WebAssistantSession.clear()` wipes them on sign-out. It is website/backend
  work only. Note that clearing the app's data still loses local state; a
  server-side session is what fixes cross-device continuity.
- **Voice inside the app:** either hide the buttons (cheap, recommended for
  now) or ask us to build a native bridge using the app's existing
  `SpeechRecognizer` pipeline and a `@JavascriptInterface` that returns a
  transcript to the composer. The latter is Android work in this repo.

### Android changes made in this pass

- `onPageFinished` retry is now conditional: it checks
  `window.__THESTUDENT_USER__` with `evaluateJavascript` and only re-posts when
  the value is missing (for example after a guest-sign-in reload). No duplicate
  identity post on the same page.
- Native **"New chat"** `IconButton` in the header.
- External-link fallback snackbar reworded to "That link can't be opened here."
  so it does not contradict the website's "Link copied" toast.
