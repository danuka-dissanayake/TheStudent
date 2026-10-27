# Student Chat (production)

1:1 messaging between **registered TheStudent students**, backed by MySQL and
served by `backend/api/chat.php`. There is no mock data left in the app: the
chat tab reads real accounts, real messages, real presence.

---

## 1. What students see

| Screen | What it shows | Where it comes from |
| --- | --- | --- |
| Chat tab | Every active, e-mail-verified student — **full name only** (privacy), plus their uploaded profile picture when they have one, else an initials avatar | `GET api/chat.php?action=threads` |
| Online row | Students active in the last 10 seconds (2 s heartbeats), with a live count ("12 online") | same response, `online_count` |
| Thread row | Last message preview, "typing…", unread badge, time stamp | same response |
| Conversation | Message history, own bubbles on the right, read ticks, "typing…" bubble, "Online" / "last seen 5 min ago" | `GET api/chat.php?action=messages` |
| Safety notice | Bottom banner: *never share phone numbers, home address, school, bank details or passwords* — shown **once per account** until acknowledged | `users.chat_safety_ack_at` |
| Typing | Three-dot bubble (rounded, one squared corner): each dot lifts 7 px and brightens at the 30 % mark of a 1.2 s loop, staggered 160 ms left→right | `chat_typing` |
| Backup card | After a reinstall: emerald "You have a backup chat history" card with **Restore messages** / **Skip**, a rotating-cube loader and a live progress bar while restoring | `GET threads → history_backup` |

Presence, typing, unread badges and the online counter are all refreshed by
polling. Timings (see the constants at the bottom of `TheStudentViewModel`):

| What | Interval | Notes |
| --- | --- | --- |
| Presence heartbeat | **2 s** | the server treats 10 s of silence as offline, so a student flips to "last seen …" within ~10 s and back to online within ~2 s |
| Open conversation | 2 s | new messages, typing flag, read ticks |
| Chat list / online count | 3 s | names, avatars, badges, online dots |

Nothing is faked or randomised client-side.

**Privacy guarantees**

* The API never returns e-mail, phone, institution, stream or role.
* Only `id`, `full_name`, `profile_picture` and coarse presence leave the server.
* Presence is derived from `users.last_seen_at` (10 s window) — no IP addresses
  or device details are exposed to other students.
* "Clear chat" removes the conversation **for me only**; the other student keeps
  their copy (stored as `chat_clears.cleared_seq`).

---

## 1b. End-to-end encryption

Messages are encrypted **on the phone**; the server and MySQL only ever hold
ciphertext. Implementation: `data/local/ChatCrypto.kt`.

1. Each student has a long-lived **EC P-256 identity key pair**. The private key
   is sealed with an AES-256-GCM key inside the **Android Keystore** (hardware
   backed where available) and never leaves the device.
2. The public half is published with `POST ?action=set_key` into
   `users.chat_public_key`.
3. Two students derive the same conversation secret with **ECDH**
   (`ECDH(mine, theirs) == ECDH(theirs, mine)`), hashed with SHA-256 into an
   AES-256 key. Keys are cached per peer in memory only.
4. Every message body is `e2ee:v1:` + Base64(IV ‖ AES-256-GCM ciphertext) before
   upload, and is decrypted when it comes back. Bodies without the prefix
   (pre-upgrade messages) are shown unchanged.

Consequences you should know:

* **The server cannot read messages** — not the admin panel, not a database
  dump. Chat search on the server is impossible by design.
* A student who has never opened chat has no published key yet, so the
  composer is disabled and says so instead of falling back to plain text.
* Replacing a published key is refused (`409`) unless the account has no
  password-protected backup, because old messages would become unreadable.

### Restoring history on a new phone

At e-mail sign-in the app wraps a copy of the private key with a key derived
from the account password (**PBKDF2-HMAC-SHA256, 120 000 iterations**,
AES-256-GCM) and uploads only the wrapped blob (`users.chat_key_backup`).

After a reinstall:

1. The app notices the server already has a key it cannot reproduce and shows
   the **backup card**.
2. *Restore messages* asks for the account password once; the server verifies it
   with `password_verify()` against your normal login hash, returns the wrapped
   blob, and the phone unwraps it locally (the password never leaves the phone
   in a form the server can keep — it is only checked, and the blob cannot be
   unwrapped without it).
3. Restoring then walks every conversation and rebuilds the history, while the
   card shows the cube loader + progress bar.
4. *Skip* (`action=discard_history`) deletes this account's messages from the
   database and then rotates the encryption key so chat keeps working.

Google-only accounts have no password to derive a wrap key, so their backup
cannot be unlocked on a new device — the card says so instead of pretending.
Set a password in Profile → Security to enable this.

---

## 2. Deploy the database

You do **not** need SSH. Either path works:

**A. phpMyAdmin (recommended, explicit)**
1. DirectAdmin → *MySQL Management* → phpMyAdmin → open your database.
2. *Import* → choose `backend/database_chat.sql` → Go.
3. If it stops at the `ALTER TABLE users ...` statement with
   *“Duplicate column name”*, that only means the columns already exist —
   re-run the file with those two `ALTER TABLE` lines removed, or just let the
   API add them (path B).

**B. Do nothing** — open the chat tab once in the app. `api/chat.php` creates
`chat_messages`, `chat_typing`, `chat_clears` and adds the two `users` columns
on its first request (the same self-healing approach `auth.php` uses).

Tables created:

| Table | Purpose |
| --- | --- |
| `chat_messages` | id, sender_id, recipient_id, body, read_at, created_at |
| `chat_typing` | per-pair typing flag with a 6 s lifetime |
| `chat_clears` | per-student `cleared_seq` so "clear chat" is one-sided |
| `users.last_seen_at` | presence heartbeat (2 s pings) |
| `users.chat_safety_ack_at` | one-time safety notice acknowledgement |
| `users.chat_public_key` | E2EE identity public key (base64 SPKI, P-256) |
| `users.chat_key_backup` | private key wrapped with the account password |
| `users.chat_key_salt` | PBKDF2 salt for that wrapped key |

No new secrets, keys or environment variables are required — chat reuses the
existing session token from `auth.php`.

---

## 3. API reference

All endpoints require `Authorization: Bearer <session token>`; the token is
never accepted in a query string. Successful responses use
`{"status":"success", ...}`; failures use `{"status":"error","message":"…"}`.

| Method | Action | Body / query | Returns |
| --- | --- | --- | --- |
| GET | `threads` | – | `threads[]`, `online_count`, `total_students`, `safety_acknowledged`, `history_backup`, `my_public_key`, `key_backup_available` |
| GET | `messages` | `peer_id`, optional `after_id` | `peer`, `messages[]`, `peer_is_typing`, `safety_acknowledged` |
| POST | `send` | `{peer_id, body}` | `message` |
| POST | `typing` | `{peer_id, is_typing}` | – |
| POST | `read` | `{peer_id}` | `updated` |
| POST | `clear` | `{peer_id}` | `cleared_seq` |
| POST | `ack_safety` | – | `safety_acknowledged: true` |
| POST | `heartbeat` | – | `online_count` |
| POST | `set_key` | `{public_key, key_backup?, key_salt?, allow_replace?}` | `public_key` (409 when replacing a backed-up key) |
| POST | `unlock_key_backup` | `{password}` | `key_backup`, `key_salt`, `public_key` |
| POST | `discard_history` | – | `deleted` |
| GET | `version` | – | deploy fingerprint |

Message rules enforced server-side:

* 1–6000 characters of ciphertext (≈2000 characters of plain text), control
  characters stripped, blank messages rejected.
* Bodies are opaque ciphertext: the server never validates or indexes the text.
* 30 messages per rolling minute per student (HTTP 429 + `Retry-After`).
* You cannot message yourself, an inactive account or an unverified account.
* Every query is parameterised; the peer id is validated as an integer.

---

## 4. Client implementation

| File | Role |
| --- | --- |
| `data/model/ChatModels.kt` | `ChatPeer`, `ChatMessage`, `ChatThread`, `ChatDirectory`, `ChatConversationState` |
| `data/remote/TheStudentChatClient.kt` | OkHttp client for every action above (never throws; returns `ChatResult`) |
| `viewmodel/TheStudentViewModel.kt` | `chatState`, directory/message polling, presence heartbeat, typing throttle, send/clear/ack |
| `data/local/ChatCrypto.kt` | E2EE identity, ECDH conversation keys, AES-256-GCM bodies, password-wrapped key backup |
| `ui/screens/ChatScreen.kt` | Directory, online rail, conversation, typing dots, safety banner, backup/restore card + cube loader, clear-chat dialog |

The chat tab uses the app-wide emerald wash (`TheStudentAppBackground`) plus the
onboarding background art, and keeps the status bar transparent so it matches
every other screen.

Polling intervals (constants in `TheStudentViewModel`):

```
CHAT_PRESENCE_MS        = 2_000    // must stay well below the 10 s online window
CHAT_DIRECTORY_POLL_MS  = 3_000
CHAT_MESSAGES_POLL_MS   = 2_000
CHAT_TYPING_THROTTLE_MS = 3_000
```

---

## 5. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| "Sign in to chat with other students." | No stored session token | Sign in again (session may have expired after 30 days) |
| "No other students have signed up yet" | Only one verified account exists | Register a second account and verify it |
| Everyone shows as offline | Heartbeat can't run (app in background) or `users.last_seen_at` missing | Open the chat tab in both apps; confirm `database_chat.sql` was applied |
| Chat list empty after deploy | `chat.php` returned a 500 | Check `php-error.log`; verify `chat.php` was uploaded completely (`api/chat.php?action=version`) |
| Messages send but the other side never sees them | Polling blocked by battery saver | Keep the conversation open; the app polls every 2 s while visible |
| "Unlock your encrypted chat backup first" | The device's key differs from the key the server published (reinstall) | Use the backup card → Restore (password) or Skip (deletes history and rotates the key) |
| Backup card asks for a password that does not work | Password changed since the backup was created | Skip restores chat with a fresh key; old messages stay unreadable by design |
| Composer disabled in a conversation | That student has not opened chat on an updated build, so they have no public key yet | They must open the chat tab once |
| Messages show "🔒 can't be decrypted on this device" | History predates this device's key and was never restored | Restore from the password-protected backup |
