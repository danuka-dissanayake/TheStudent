# App ↔ website single sign-on (assistant session bridge)

**Status:** implemented on the Android + PHP side in this repo. The website side is a
separate repository; the exact contract is in [§7](#7-copy-paste-prompt-for-the-website-repo).

---

## 1. The problem

A student registers or signs in **inside the Android app**. When they open the
embedded AI assistant (a WebView showing the website), they must already be signed
in — no second password entry, no "Sign in with Google" inside a WebView, and no
visible browser redirect.

The naive solutions are all wrong:

| Anti-pattern | Why it is dangerous |
|---|---|
| `?token=<app session>` in the URL | Leaks into WebView history, `Referer`, server logs, screenshots, crash reports. |
| `evaluateJavascript("localStorage.setItem('token', ...)")` | Any script on the page (including a compromised dependency or injected ad) can read it. XSS becomes full account takeover. |
| Reusing the app's Bearer token as the website's token | One credential with full app privileges now lives in two places; the website can act as the student on the app API. |
| `addJavascriptInterface` | Exposed to **every frame** with **no origin control** on older WebViews. |
| Trusting `userId` / email from `postMessage` | JavaScript is attacker-controlled; it is a display hint, never an authentication source. |
| OAuth popup / redirect flow inside a WebView | WebView has no tabbed browsing; popups, custom schemes and Google sign-in are unreliable. |

## 2. The implemented design

One-time **authorization-code** style SSO with PKCE, server-to-server exchange,
and a website-owned `HttpOnly` cookie.

```
 Android app                PHP backend (this repo)          Website server
     |                              |                              |
     | 1. POST /api/assistant_session.php?action=mint              |
     |    Authorization: Bearer <APP SESSION>  (never leaves here) |
     |    {code_challenge (S256), state}                            |
     | ----------------------------------------->                  |
     |    validate app session, store code_hash +                   |
     |    challenge + state_hash + user_id + token_id (90 s)        |
     | <--- {code, exchange_url, sub, expires_in} ----------------- |
     |                              |                              |
     | 2. WebView.postUrl(exchange_url, form body)                  |
     |    body: code, code_verifier, state, app, theme              |
     |    (code is in the BODY, never in a URL)                     |
     | ------------------------------------------------------------>|
     |                              |   3. POST ?action=exchange    |
     |                              |      HMAC-signed, server-side  |
     |                              |<----------------------------- |
     |                              |   verify PKCE + state,         |
     |                              |   atomically claim single-use  |
     |                              |---> {sub, name, email} ------->|
     |                              |                       rotate session id
     |                              |                       JIT provision by sub
     |                              |                       Set-Cookie: HttpOnly;
     |                              |                       Secure; SameSite=Lax
     | <----------- 303 redirect to /?app=android -------------------|
     |                              |                              |
     | 4. Logout: POST ?action=revoke [Bearer] -> assistant_revoked_at = NOW()
     |    also clears WebView cookies + web storage locally           |
```

**Key properties**

- The app's session token is used **only** as a Bearer header to our own PHP API,
  exactly like every other authenticated call. It never enters the WebView.
- The code is 256-bit random, **90 seconds**, **single-use** (atomic `UPDATE`),
  bound to one `users.id` **and** one `user_tokens.id`, stored only as a SHA-256
  digest, and never logged.
- **PKCE S256** means a stolen code is useless without the verifier; `state`
  binds the exchange to one app-side attempt.
- The website's server-to-server calls are authenticated with a **dedicated**
  `ASSISTANT_SSO_SECRET` + timestamp + single-use nonce — not `APP_KEY`, and not
  anything shipped in an APK.
- `sub` is the immutable numeric `users.id`. Emails change; IDs do not. Nothing
  from JavaScript is ever used to grant access.
- All SSO responses are `Cache-Control: no-store` and `Referrer-Policy: no-referrer`.
- **Fail closed everywhere**: unsupported `WebMessageListener` → no bridge;
  unconfigured secret/origin → no code; exchange URL on another host → guest mode.

## 3. Files added / changed in this repo

| File | Role |
|---|---|
| `backend/api/assistant_session.php` | `mint`, `exchange`, `validate`, `revoke` + lazy schema creation. |
| `backend/config.secrets.example.php` | New `ASSISTANT_SSO_SECRET`, `ASSISTANT_WEB_ORIGIN`, `ASSISTANT_EXCHANGE_PATH`. |
| `app/.../data/local/AssistantHandshake.kt` | Pure PKCE, state, form body, bridge allowlist. |
| `app/.../data/remote/AssistantSessionClient.kt` | OkHttp client for `mint` / `revoke`. |
| `app/.../data/local/WebAssistantConfig.kt` | `originOf()` + `isAllowedExchangeUrl()`. |
| `app/.../ui/screens/WebAssistantScreen.kt` | `postUrl` delivery, origin-pinned `addWebMessageListener`, native pending / retry UI. |
| `app/.../viewmodel/TheStudentViewModel.kt` | Mint / re-mint / revoke orchestration, in-memory-only code material. |
| `app/.../MainActivity.kt` | Passes SSO state into the screen; `studentId` is now opaque `sub`. |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | `androidx.webkit:webkit:1.12.1` for `WebViewCompat`. |
| `app/src/test/.../AssistantHandshakeTest.kt`, `WebAssistantConfigTest.kt` | JVM tests (RFC 7636 KAT, body shape, origin allowlist). |

## 4. Backend API contract

All actions are **POST** to `/api/assistant_session.php?action=<action>`. All
responses use the standard envelope `{ "status": "success" | "error", "message": "…", … }`.

### 4.1 `mint` — app → backend

Headers: `Authorization: Bearer <app session>`, `Content-Type: application/json`.
Body: `{ "code_challenge": "<43–128 unreserved chars>", "code_challenge_method": "S256", "state": "<16–128 unreserved chars>" }`.

Success:
```json
{
  "status": "success",
  "message": "Assistant sign-in code issued.",
  "code": "<64 hex chars>",
  "exchange_url": "https://chatbot-liart-rho-81.vercel.app/api/app-auth",
  "sub": "42",
  "expires_in": 90
}
```

Errors: `401` (missing/invalid/expired app session), `400` (bad challenge/state),
`429` (more than 5 codes/user/minute), `503` (SSO not configured), `403` (non-HTTPS).

### 4.2 `exchange` — website server → backend

This is the only action that crosses from the website's server to ours. It must
be called **from server code**, never from browser JavaScript.

Headers:

| Header | Value |
|---|---|
| `X-TheStudent-Timestamp` | Unix seconds, ±5 minutes tolerated |
| `X-TheStudent-Nonce` | Random 16–128 `[A-Za-z0-9-_]`; single use |
| `X-TheStudent-Signature` | Lowercase hex HMAC-SHA256 of the canonical string below |
| `Content-Type` | `application/json` |

Canonical string (exact, including `\n`):
```
POST\n/api/assistant_session.php?action=exchange\n<timestamp>\n<nonce>\n<sha256-hex of the RAW request body>
```
`signature = hash_hmac('sha256', canonical, ASSISTANT_SSO_SECRET)`.

Body: `{ "code": "<64 hex>", "code_verifier": "<PKCE verifier>", "state": "<state>" }`.

Success:
```json
{
  "status": "success",
  "message": "Assistant identity verified.",
  "sub": "42",
  "name": "Nimali Perera",
  "email": "nimali@example.com",
  "email_verified": 1,
  "revoked_at": null
}
```
The website must treat `sub` as the **account key** and JIT-provision / link it.
Never key on email.

### 4.3 `validate` — website server → backend

Same HMAC scheme with `action=validate` in the canonical string. Body `{ "sub": "42" }`
→ `{ "status":"success", "active": true|false, "revoked_at": null|"…" }`.
Use it (or the `revoked_at` returned by `exchange`) to refuse a session created
before the student's last app sign-out.

### 4.4 `revoke` — app → backend

Headers: `Authorization: Bearer <app session>`. Body may be `{}`.
Sets `users.assistant_revoked_at = NOW()` and deletes unused codes for that user.
Called from `TheStudentViewModel.endSession()` **before** `preferences.logout()`.
Local WebView cookies + web storage are always cleared, even if the network call
fails.

### 4.5 Schema (created lazily, no migration command needed)

- `assistant_exchange_codes(id, user_id, token_id, code_hash, code_challenge,
  state_hash, expires_at, used_at, created_at, ip_address)`
- `assistant_sso_nonces(nonce_hash, expires_at)`
- `users.assistant_revoked_at DATETIME NULL` (added via `ts_ensure_column`)

## 5. Deployment checklist (PHP side)

1. Copy `backend/config.secrets.example.php` → `config.secrets.php`, permissions `600`.
2. Generate **two different** secrets:
   ```
   php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;'
   ```
   One for `APP_KEY` (existing), one for `ASSISTANT_SSO_SECRET`. **Never reuse.**
3. The shipped origin is **`https://chatbot-liart-rho-81.vercel.app`** (no path,
   no trailing slash). `backend/config.php` already defaults
   `ASSISTANT_WEB_ORIGIN` to it, and `config.secrets.example.php` sets the same
   value. No domain edit is needed for this build. Never use a Vercel preview /
   unique deployment URL here.
4. It is already identical to `WebAssistantConfig.PRODUCTION_ORIGIN` /
   `DEFAULT_BASE_URL` in the Android app. **The two hosts MUST match**: the app
   refuses to POST a code to any other host. If you ever move the site to a
   custom domain, change both constants together (and the runtime override from
   `setWebAssistantBaseUrl()`, if one was persisted on a device).
5. Put the same `ASSISTANT_SSO_SECRET` in the website server's environment.
6. Deploy `backend/api/assistant_session.php`. The tables are created on the
   first request.
7. Confirm `REQUIRE_HTTPS=1` in production.

Never put `ASSISTANT_SSO_SECRET` in an `.env` that ships with the app, in the APK,
in `localStorage`, or in any client-side bundle.

## 6. Web → app bridge (non-credential details only)

The website may send small, typed events; it can never ask for or write a
credential.

```js
// Only on the allow-listed HTTPS origin, from the MAIN frame.
if (window.TheStudentBridge) {
  window.TheStudentBridge.postMessage(JSON.stringify({ type: "SESSION_EXPIRED" }));
  window.TheStudentBridge.onmessage = e => console.log(e.data); // {"ok":true}
}
```

Android installs the object with
`WebViewCompat.addWebMessageListener(webView, "TheStudentBridge", setOf("https://<exact-host>"), listener)`
only when `WebViewFeature.WEB_MESSAGE_LISTENER` is supported, then re-checks
`isMainFrame`, scheme, host, a 4 KB size cap, and this event allowlist:

`SESSION_READY`, `SESSION_EXPIRED`, `NEW_CHAT`, `CLOSE`, `OPEN_PAPERS`, `SHARE_TEXT`.

Unknown types and iframe messages are ignored. `addJavascriptInterface` is never
used. If the feature is unsupported the bridge simply does not exist (fail closed);
one-way `window.postMessage` for the guest identity hint still works.

## 7. Copy-paste prompt for the website repo

> **Implement the website half of TheStudent app ↔ web SSO.** Do not change the
> normal browser experience. Add a server-side POST route `/api/app-auth` (Node /
> Next.js route handler or equivalent) that:
>
> 1. Reads `code`, `code_verifier`, `state`, `app`, `theme` from the
>    `application/x-www-form-urlencoded` POST body. Reject everything else
>    (405 for GET, 400 for missing fields). The code MUST NOT appear in a URL,
>    query string, log, cookie value, or client-side JS.
> 2. Calls `POST https://app.quizontal.site/api/assistant_session.php?action=exchange`
>    **from server code** with JSON `{code, code_verifier, state}` and headers
>    `X-TheStudent-Timestamp` (unix seconds), `X-TheStudent-Nonce` (random),
>    `X-TheStudent-Signature` = lowercase hex
>    `HMAC_SHA256("POST\n/api/assistant_session.php?action=exchange\n" + timestamp + "\n" + nonce + "\n" + sha256hex(rawBody), process.env.ASSISTANT_SSO_SECRET)`.
>    The secret must stay server-side only. Set `Cache-Control: no-store` and
>    `Referrer-Policy: no-referrer` on every response of this route.
> 3. On success, treats `sub` (opaque numeric `users.id`) as the account key:
>    find-or-create the user by `sub` (never by email), update display name/email
>    as non-key profile fields, **rotate/regenerate the session id**, and store
>    the session server-side (or sign an httpOnly session cookie). Reject if
>    `status !== "success"`.
> 4. Responds with **303** and `Location: /?app=android&theme=<theme>` (carry
>    through the original `?q=` if it exists). Sets exactly one session cookie:
>    `HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=…`. Never put the code,
>    the verifier, a Bearer token or the session id into JS-readable storage.
> 5. On any failure, clears the session cookie and redirects (303) to
>    `/?app=android&auth=guest` — never render the code or the error body.
> 6. Keeps the existing `?app=android` embed mode: native-feeling layout, no
>    external navigation, no OAuth popups. **Do not** read `userId` from
>    `window.__THESTUDENT_USER__` / `postMessage` to grant access; the cookie is
>    the only authentication source. In the app's signed-in flow the app no
>    longer injects that identity at all, precisely to avoid account-switch loops.
> 7. When the website session expires, if `window.TheStudentBridge` exists, call
>    `window.TheStudentBridge.postMessage(JSON.stringify({type:"SESSION_EXPIRED"}))`
>    from the main frame on the production HTTPS origin; the app will mint a fresh
>    one-time code and re-POST. Allowed event types are only `SESSION_READY`,
>    `SESSION_EXPIRED`, `NEW_CHAT`, `CLOSE`, `OPEN_PAPERS`, `SHARE_TEXT`. Add an
>    `onmessage` handler that accepts `{"ok":true}`.
> 8. Optionally call `?action=validate` (same HMAC scheme, body `{sub}`) to
>    confirm the student has not signed out of the app since this session was
>    created; sign the website session out if `active === false`.
>
> Environment: `ASSISTANT_SSO_SECRET` (identical 64-hex value to the PHP server),
> `THESTUDENT_API_BASE=https://app.quizontal.site`. The site itself MUST be served
> at the pinned origin `https://chatbot-liart-rho-81.vercel.app` (identical to the
> app's `WebAssistantConfig.PRODUCTION_ORIGIN`); the app will refuse to POST a
> code to any other host.

## 8. Test checklist on a real phone

- [ ] Register a new student in the app → open Ask AI → no second sign-in, no
      "Guest" label, no visible URL or browser redirect.
- [ ] Kill the app, reopen, open Ask AI → still signed in via the cookie.
- [ ] Sign out in the app → reopen Ask AI as another student → previous chats
      gone (cookies + web storage cleared) and `revoke` recorded.
- [ ] Change the account email → the assistant account does not change (`sub`
      is the numeric id).
- [ ] Airplane mode on open → native "couldn't sign in" + guest fallback + Retry;
      the assistant still opens, no crash.
- [ ] Copy the code from a proxy (e.g. mitmproxy with a debug CA) → replaying
      the same POST fails ("already been used"); using it after 90 s fails.
- [ ] Modify the code in transit → PKCE verification fails.
- [ ] Inject `TheStudentBridge.postMessage('{"type":"GET_TOKEN"}')` from the
      console → ignored; from an iframe → ignored.
- [ ] Point `ASSISTANT_WEB_ORIGIN` at a different host → app refuses the code
      and opens in guest mode (fail closed).
- [ ] Old WebView without `WEB_MESSAGE_LISTENER` → no bridge, app still works.

## 9. Deliberately out of scope (agreed: "OIDC later")

This is a **first-party** SSO between our app, our PHP API and our own website.
It is not a full OIDC identity provider: there is no discovery document, no JWKS,
no `id_token`, no third-party relying parties, and no "Sign in with TheStudent"
button for other apps. If that is ever needed, the current `mint`/`exchange`
pair is the natural place to add standard authorization-code + PKCE endpoints,
scopes and signed ID tokens — the app-side flow would not need to change much.
