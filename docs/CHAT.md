# Student Chat (production)

1:1 messaging between **registered TheStudent students**. Two pieces, with a
strict split of responsibility:

* **The phone** — encrypts, stores and owns the conversation history
  (`chat_messages`, a Room/SQLite table).
* **The server** — a **relay and presence board**, never an archive. It forwards
  encrypted envelopes and deletes them as soon as they are collected.

There is no mock data in the app: the chat tab reads real accounts, real
messages, real presence.

---

## 1. What students see

| Screen | What it shows | Where it comes from |
| --- | --- | --- |
| Chat tab | Every active, e-mail-verified student — **full name only** (privacy), plus their uploaded profile picture when they have one, else an initials avatar | `GET api/chat.php?action=threads` |
| Online row | Students active in the last 10 seconds (2 s heartbeats), with a live count ("12 online") | same response, `online_count` |
| Thread row | Last message preview, "typing…", unread badge, time stamp | **device database** |
| Conversation | History, own bubbles on the right, read ticks, "typing…" bubble, "Online" / "last seen 5 min ago" | **device database** + `threads` presence |
| Encryption note | Grey line *"Messages are end-to-end encrypted"* with a small lock, **directly under the message input bar** | static |
| Safety notice | Bottom banner: *never share phone numbers, home address, school, bank details or passwords* — shown **once per account** until acknowledged | `users.chat_safety_ack_at` |
| Typing | Three-dot bubble (rounded, one squared corner): each dot lifts 7 px and brightens at the 30 % mark of a 1.2 s loop, staggered 160 ms left→right | `chat_typing` |

Presence, typing and the online counter are refreshed by polling. Timings (see
the constants at the bottom of `TheStudentViewModel`):

| What | Interval | Notes |
| --- | --- | --- |
| Presence heartbeat | **2 s** | the server treats 10 s of silence as offline, so a student flips to "last seen …" within ~10 s and back to online within ~2 s |
| Relay inbox (new messages + read receipts) | 2 s | delivery only — nothing is kept on the server |
| Chat list / presence / online count | 3 s | names, avatars, online dots |

Nothing is faked or randomised client-side.

**Privacy guarantees**

* The API never returns e-mail, phone, institution, stream or role.
* Only `id`, `full_name`, `profile_picture` and coarse presence leave the server.
* Presence is derived from `users.last_seen_at` (10 s window) — no IP addresses
  or device details are exposed to other students.
* **Conversation history never reaches the server.** Bodies are encrypted on the
  phone and the relay row is deleted the moment it is delivered.
* "Clear chat" deletes the conversation from this phone only.

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
   upload, and is decrypted when it arrives. Bodies without the prefix
   (pre-upgrade messages) are shown unchanged.

Consequences you should know:

* **The server cannot read messages** — not the admin panel, not a database
  dump. Chat search on the server is impossible by design.
* A student who has never opened chat has no published key yet, so the composer
  is disabled and says so instead of falling back to plain text.
* **There is no key backup and no restore card** — that feature was removed on
  purpose. The private key lives in the Android Keystore, which a reinstall
  wipes.
* **After a reinstall the history is gone.** That is the privacy trade-off: the
  history existed only in the old install's database, and nothing on the server
  can bring it back. Chat itself keeps working: on the first `threads` call the
  app sees the server still holds the old public key, and (only because the
  local database is empty) publishes the fresh key so the other side can encrypt
  to it again.
* If the local database is *not* empty and the server key has changed, the app
  refuses to rotate and shows an error instead of silently breaking old
  messages.

---

## 2. Deploy the database

You do **not** need SSH. Either path works:

**A. phpMyAdmin (recommended, explicit)**
1. DirectAdmin → *MySQL Management* → phpMyAdmin → open your database.
2. *Import* → choose `backend/database_chat.sql` → Go.
3. Every statement is `IF NOT EXISTS`, so re-running is safe.

**B. Do nothing** — open the chat tab once in the app. `api/chat.php` creates
`chat_relay`, `chat_typing` and the three `users` columns on its first request
(the same self-healing approach `auth.php` uses).

Tables created:

| Table | Purpose |
| --- | --- |
| `chat_relay` | encrypted envelopes waiting to be **collected**, then deleted (`kind = 'msg'`), plus read receipts (`kind = 'read'`). 7-day TTL as a safety net. |
| `chat_typing` | per-pair typing flag with a 6 s lifetime |
| `users.last_seen_at` | presence heartbeat (2 s pings) |
| `users.chat_safety_ack_at` | one-time safety notice acknowledgement |
| `users.chat_public_key` | E2EE identity public key (base64, uncompressed P-256 point) |

**No message table exists.** Upgrading from an older build leaves unused
`chat_messages`, `chat_clears`, `chat_key_backup` and `chat_key_salt` behind;
the current API never touches them, so dropping them (see the commented
cleanup at the bottom of `database_chat.sql`) is optional and safe.

No new secrets, keys or environment variables are required — chat reuses the
existing session token from `auth.php`.

---

## 3. API reference

All endpoints require `Authorization: Bearer <session token>`; the token is
never accepted in a query string. Successful responses use
`{"status":"success", ...}`; failures use `{"status":"error","message":"…"}`.

| Method | Action | Body / query | Returns |
| --- | --- | --- | --- |
| GET | `threads` | – | `threads[]` (id, name, avatar, presence, `public_key`, `is_typing`), `online_count`, `total_students`, `safety_acknowledged`, `online_window_seconds`, `server_epoch`, `my_public_key` |
| GET | `inbox` | – | `messages[]` (`{id, sender_id, body, created_at}`), `receipts[]` (`{peer_id, up_to_server_id}`) — **each row is deleted as it is handed out** |
| POST | `send` | `{peer_id, body}` | `message_id` |
| POST | `typing` | `{peer_id, is_typing}` | – |
| POST | `read` | `{peer_id, up_to_server_id}` | – (queued as a `read` envelope for the sender) |
| POST | `ack_safety` | – | `safety_acknowledged: true` |
| POST | `heartbeat` | – | `online_count`, `server_epoch` |
| POST | `set_key` | `{public_key, allow_replace?}` | `public_key` (409 when a different key is already published and `allow_replace` is absent) |
| GET | `version` | – | deploy fingerprint (`2.0.0`) |

Rules enforced server-side:

* Body must match `^e2ee:v1:[A-Za-z0-9+/=]+$` and be at most 6000 characters —
  plain text is rejected, so a buggy client cannot leak a message body.
* Sending to a student with no published key returns 409.
* 30 messages per rolling minute per student (HTTP 429 + `Retry-After`).
* You cannot message yourself, an inactive account or an unverified account.
* Inbox is capped at 200 envelopes per fetch; expired rows (older than 7 days)
  and stale typing flags are purged at the start of every request.
* Every query is parameterised; ids are validated as integers.

**Online count** comes from one helper shared by `threads` and `heartbeat`:
`users.is_active = 1 AND is_email_verified = 1 AND last_seen_at > now - 10 s`,
**excluding the caller**, with "now" read from the database
(`SELECT UNIX_TIMESTAMP(NOW())`). Using the same clock and the same window for
both endpoints is what stops the count flickering between 1 and 0.

---

## 4. Client implementation

| File | Role |
| --- | --- |
| `data/model/ChatModels.kt` | `ChatPeer`, `ChatMessage`, `ChatThread`, `ChatDirectory` |
| `data/local/TheStudentEntities.kt` | `ChatMessageEntity` (history, device only), `ChatSummaryRow` (thread previews) |
| `data/local/TheStudentDao.kt` | `observeConversation`, `observeSummaries`, upserts, read/pending flags, per-thread and full wipes |
| `data/local/TheStudentDatabase.kt` | Room **version 3** — `MIGRATION_2_3` creates `chat_messages` + indices |
| `data/remote/TheStudentChatClient.kt` | OkHttp client for every action above (never throws; returns `ChatResult`) |
| `viewmodel/TheStudentViewModel.kt` | `chatState`, directory/inbox/presence polling, typing throttle, send, Room observation |
| `data/local/ChatCrypto.kt` | E2EE identity, ECDH conversation keys, AES-256-GCM bodies |
| `ui/screens/ChatScreen.kt` | Directory, online rail, conversation, typing dots, safety banner, encryption note, clear-chat dialog |

**Message flow**

1. `sendChatMessage` writes a **pending** row into `chat_messages` first, so the
   message can never be lost by a network failure.
2. The body is encrypted with the peer's key and POSTed to `send`. On success the
   row is updated with the relay id and the server timestamp; on failure it is
   marked failed and an error banner appears (tap retry after reconnecting).
3. The inbox loop fetches the relay every 2 s, decrypts, and upserts each
   message as `localId = "srv-<serverId>"` — an idempotent key, so a duplicate
   fetch can never duplicate a bubble (this also covers a crash between storing
   a row and acknowledging it).
4. `openChat(peer)` collects `observeConversation(peerId)`, marks the peer's
   messages read and queues a read receipt; the sender's ticks update on its own
   next inbox poll.

**Local database schema**

```
chat_messages  localId (PK, UUID)  serverId  peerId  senderId  isMine
               body (plain text, decrypted)  sentAtEpoch  isRead  isPending
               indices: peerId, sentAtEpoch

chat_summaries (projection)  peerId  lastBody  lastIsMine  lastEpoch  unreadCount
```

The bodies sit in the app's private storage, which is excluded from cloud and
ADB backup (`android:allowBackup="false"`), and `endSession()` (sign-out) wipes
`chat_messages` along with the cached conversation keys.

The chat tab uses the app-wide emerald wash (`TheStudentAppBackground`) plus the
onboarding background art, and keeps the status bar transparent so it matches
every other screen.

Polling intervals (constants in `TheStudentViewModel`):

```
CHAT_PRESENCE_MS        = 2_000    // must stay well below the 10 s online window
CHAT_DIRECTORY_POLL_MS  = 3_000
CHAT_INBOX_POLL_MS      = 2_000
CHAT_TYPING_THROTTLE_MS = 3_000
```

---

## 5. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| "Sign in to chat with other students." | No stored session token | Sign in again (session may have expired after 30 days) |
| "No other students have signed up yet" | Only one verified account exists | Register a second account and verify it |
| Everyone shows as offline | Heartbeat can't run (app in background) or `users.last_seen_at` missing | Open the chat tab in both apps; confirm `database_chat.sql` was applied |
| Online count flickers | Should not happen on 2.0.0 — both `threads` and `heartbeat` use one shared, caller-excluded 10 s count on the database clock | Confirm `api/chat.php` was uploaded completely (`api/chat.php?action=version` → 2.0.0) |
| Chat list empty after deploy | `chat.php` returned a 500 | Check `php-error.log`; verify the upload completed |
| Messages send but the other side never sees them | Polling blocked by battery saver, or the recipient's app never fetched the relay before the 7-day TTL | Keep the conversation open; the app polls every 2 s while visible |
| Composer disabled in a conversation | That student has not opened chat on an updated build, so they have no public key yet | They must open the chat tab once |
| History disappeared after reinstalling | The history lived only in the old install's database; there is no backup by design | Expected. Chat works again after the key is rotated automatically |
| "Secure chat could not start: this account already has a different encryption key" | The server published a key this phone cannot reproduce, *and* the local database still has messages | Sign out and in on this phone to clear the local history, then reopen chat to rotate the key |
| Messages show "🔒 can't be decrypted on this device" | A message was encrypted to a key this phone no longer holds | Expected after a key rotation; new messages decrypt normally |
