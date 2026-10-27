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
| Thread row | Last message preview ("📷 Photo", "🎤 Voice note", "📎 notes.pdf" or the text), "typing…", unread badge, time stamp | **device database** |
| Day separators | The conversation is grouped as **Today**, **Yesterday**, then "12 October" / "12 October 2024" | message timestamps |
| Auto-scroll | Sending always scrolls to your own newest bubble; an incoming message scrolls while you are at the bottom. It is decided from `listState.canScrollForward` (nothing below the viewport) rather than from the last visible index, and the scroll waits one frame so the new row is laid out first — index-based, same-frame checks were why the list appeared never to move. Scrolling up through history or an active search is never yanked. |
| Conversation | History, own bubbles on the right in **black**, tighter padding, **Markdown** formatting, emoji-only messages rendered large, replies quoted, voice notes playable, upload/download progress on files, read ticks, "typing…" bubble, "Online" / "last seen 5 min ago" | **device database** + `threads` presence |
| Encryption note | Grey line *"Messages are end-to-end encrypted"* with a small lock, **directly under the message input bar** | static |
| Attachments | Paperclip opens a styled sheet: **Photo** (gallery) or **Document** (PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT, ZIP), each with its icon tile and description. The pick is previewed above the input with its size and a ✕, a caption can be typed, and the bubble then shows a rounded photo or a document card. Tap to open it. Max **25 MB**. | `upload_attachment` + `attachment` |
| Message actions | Long-press a bubble: **Reply**, **Copy text**, and **Delete for everyone** on your own messages (removes it from the other student's chat too) or **Remove from this chat** on theirs | relay `delete` envelope |
| Reply | **Swipe a bubble from left to right** (both sides) or pick Reply from the long-press sheet. An emerald arrow grows behind the bubble and triggers with a haptic tick; the quoted message then shows above the composer and inside the next bubble, labelled with whoever wrote it **on the reading device** ("You" or the student's first name) | encrypted envelope `r` |
| Loading | Opening a chat shows a two-dot orbit loader (45° tilt, green + orange on perpendicular planes, 0.4 s apart) **until the conversation is parked at the newest message**: the list is built underneath the overlay, jumped (not animated) to the bottom, and only then faded in over 220 ms. A 4 s watchdog releases it if anything stalls | – |
| Composer focus | Tapping anywhere outside the input bar closes the keyboard, drops the focus border and clears the "typing…" indicator for the other student | – |
| Search | Magnifier in the conversation header. Nothing is hidden: every hit is highlighted in **green**, the ↑ / ↓ buttons jump between them (with a `2/7` counter), the bubble you land on gets a green ring, and typing a new query jumps to the first hit | local database |
| Emoji | Smiley button in the input toggles a scrollable strip of 50 common emoji; taps append to the text | – |
| Voice notes | Mic button (empty composer, no attachment) records AAC/M4A; a live timer with a pulsing dot, cancel or send; the bubble plays it back with a progress line | `upload_attachment` (kind `audio`) |
| Safety notice | Bottom banner: *never share phone numbers, home address, school, bank details or passwords* — shown **once per account** until acknowledged | `users.chat_safety_ack_at` |
| Typing | Three-dot bubble (rounded, one squared corner): each dot lifts 7 px and brightens at the 30 % mark of a 1.2 s loop, staggered 160 ms left→right | `chat_typing` |

Presence, typing and the online counter are refreshed by polling. Timings (see
the constants at the bottom of `TheStudentViewModel`):

| What | Interval | Notes |
| --- | --- | --- |
| Presence heartbeat | **2 s** | the server treats 10 s of silence as offline, so a student flips to "last seen …" within ~10 s and back to online within ~2 s |
| Relay inbox (new messages + read receipts) | 2 s | delivery only — nothing is kept on the server |
| Chat list / presence / online count | 3 s | names, avatars, online dots |
| Attachment fetch | with the inbox | a failed blob is retried after 60 s, not every poll |

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

## 1b-2. Markdown in messages

Message bodies are rendered through `ui/screens/ChatMarkdown.kt`, a small
dependency-free renderer that produces one `AnnotatedString` (so a bubble is
still a single `Text` and the emoji-sizing and search highlight keep working):

| Syntax | Result |
| --- | --- |
| `*bold*` or `**bold**` | **bold** |
| `_italic_` or `__italic__` | *italic* (never inside a word) |
| `~strike~` or `~~strike~~` | struck through |
| ``` `code` ``` | monospace chip with a tinted background |
| ```` ``` ```` fenced block | monospace lines on a tinted background (fences hidden) |
| `# H1` … `### H3` | bold 17 / 15.5 / 14.5 sp headings |
| `- item`, `* item` | bullet |
| `1. item` | numbered line |
| `> quote` | italic with a `▍` marker |
| `[label](url)` | underlined link colour (tap behaviour unchanged) |

Markers are **one character on each side**, because that is what people type on
a phone keyboard; the doubled forms are accepted as well so pasted Markdown keeps
working. Two guards keep ordinary text safe:

* a marker followed by a space is never emphasis, so `* item` stays a bullet and
  `2 * 3 * 4` is left as written;
* `_` only italicises when it is not glued to a word, so `student_id` and
  `file_name` survive untouched.

Inside code spans (`` `…` ``) nothing is interpreted, so `*` in code stays literal.
Emphasis nests: `*bold with _italic_ inside*` renders both.

Colours adapt to the bubble: white text, translucent code chips and a mint link
colour inside the black outgoing bubble; ink text, grey chips and emerald links
on the white incoming bubble. Lines starting with a marker followed by a space
are treated as lists, so a lone asterisk never swallows the rest of a message.

---

## 1c. Attachments (photos & documents)

A file is treated exactly like a message body: **encrypted on the phone, relayed
once, then deleted from the server**.

1. The student picks a photo or document. The app copies it into its own cache
   (so a revoked `content://` Uri cannot break the send) and shows a small
   preview above the input bar — thumbnail for images, icon + name + size for
   documents, with a ✕ to remove it and a caption field still available.
2. On send the bytes are sealed with **the same ECDH conversation key** used for
   text (AES-256-GCM, random 12-byte IV) and POSTed to `upload_attachment`. The
   server stores an opaque `.bin` blob and answers with an attachment id.
3. The message itself is a normal `e2ee:v1:…` body: the *plaintext* is a tiny
   JSON envelope carrying the caption and the file's metadata (id, name, mime,
   size, image/document). The relay cannot tell that the message even has a file.
4. The bubble appears immediately: the sender already has the file locally, so
   the photo/files card renders from `filesDir`. The recipient stores the message
   first and downloads the blob on the next poll, decrypting it before it ever
   touches the disk.
5. Tapping a bubble opens the file with `FILE_VIEW` through a `FileProvider` —
   no permission is requested and nothing is exported. A voice note plays
   inline instead (play/pause, progress line and length).

**Photos are re-encoded first.** `ChatMediaPrep` downscales anything above
1600 px on its longer edge, bakes in the EXIF rotation (otherwise portraits
arrive sideways) and saves JPEG at quality 85. A 9 MB camera photo typically
becomes 300–500 KB — 10–20x less to encrypt, upload, download and store — and
looks identical in a bubble. GIFs are never touched so they keep animating, and
if re-encoding would ever produce a bigger file the original is used.

**Progress instead of a spinner.** Uploads stream through a counting
`RequestBody` and downloads through a counting `InputStream`, both feeding
`attachmentUploadProgress` / `attachmentDownloadProgress`, so a 25 MB document
shows a percentage and a thin bar on the draft card and in the bubble.

**Voice notes** are recorded with `MediaRecorder` (AAC, mono, 44.1 kHz, 96 kbps —
about 240 KB/minute) into the cache, then travel exactly like any other
attachment with `kind = audio` and their length in the encrypted envelope. A
recording under one second is discarded rather than sent.

Why this stays private: the blob is ciphertext on the server, the relay deletes
it on delivery (or after 7 days), the file name, type and caption are inside the
encrypted envelope, and the decrypted copy lives only in the app's private
storage. The local attachment cache is capped at 120 MB (oldest dropped first)
and is cleared on sign-out.

Limits: **25 MB** per file in the app, 26 MB server-side, 12 uploads per rolling
minute. Some shared hosts cap PHP uploads at 2 MB (`upload_max_filesize` /
`post_max_size` in the PHP settings) — the app then shows the server's message
instead of failing silently, and the limit has to be raised in the hosting panel.

---

## 1d. Device notifications

A new message raises a real Android notification (`notifications/ChatNotifications.kt`,
avatars in `notifications/ChatNotificationAvatars.kt`, reconciliation in
`notifications/ChatNotifier.kt`).

| Behaviour | Detail |
| --- | --- |
| Channel | `chat_messages_v2`, **High** importance, so it can pop as a heads-up banner. Alert = a shipped two-tone "beep … beep" (`res/raw/chat_beep.wav`) matched by a two-pulse vibration (70 ms, 80 ms gap, 70 ms). The `_v2` suffix exists because Android freezes a channel's sound and pattern once created; the old `chat_messages` channel is deleted on upgrade. Users can mute chat alone in system settings. |
| One per conversation | The notification is a `MessagingStyle` thread titled with the student's name. Messages arriving from the same student are **appended to that notification**, not stacked as new ones. |
| Collapsed | App icon (left) · student name · time, with the newest message on the line below. Expands on the usual drag; nothing auto-expands. **No `contentTitle`** and **no conversation title**, so the name is never printed twice, and **no large icon**, so the photo is never the thumbnail *and* the row avatar. |
| Why the name/photo used to repeat | Three separate causes, all removed: `contentTitle` set alongside the style, `setGroupConversation(true)` (which labels every message with its sender), and `setShortcutId` — from Android 11 a notification with a shortcut id is drawn as a "conversation" and the shortcut's icon (the photo) plus the person's name are rendered in the header next to the app icon/name. |
| Actions | The expanded view ends with **Reply** (inline `RemoteInput`: the reply is typed in the shade, encrypted with the same conversation key and sent through the relay) and **Mark as read** (marks the rows read, sends the read receipt, clears the notification). Handled by `ChatNotificationReceiver` → `ChatNotificationActions`. |
| Expanded | The system header (app icon · app name · time), then one row per message: the student's **profile photo** (centre-cropped circle, `ChatNotificationAvatars`, 192 px, cached in memory) · their name · the message. A student without a photo gets a coloured circle with their initial. The photo is also attached to the `Person` and to the conversation shortcut. |
| Grouped | With **two or more** unread conversations, each is a child of one summary (`InboxStyle`, "5 new messages · From 2 students"). With a single conversation there is **no group and no summary**: a notification that is a group child with no summary cannot be expanded, so grouping is applied only when a summary actually exists. |
| Tap | Opens `MainActivity` (`singleTop`) with the peer id; the view model switches to the chat tab and opens that conversation. Tapping the summary opens the chat list. |
| Clearing | The notification disappears the moment you open that conversation, and as soon as the rows are marked read. Signing out cancels everything. |
| Arrives while the app is open | Yes. The app being in the foreground suppresses nothing; only the conversation **currently on screen** is skipped (`ChatNotifications.uiActivePeerId`), so a message can never pop over the chat you are already reading. On any other screen — home, papers, even the chat list — the notification still arrives, within ~3 s (foreground poll) or instantly with FCM. |
| Not shown when | That exact conversation is open, or the notification permission is off. |

**When do they arrive?** With FCM configured a push arrives immediately. Without
a push key the relay is polled, so delivery follows the polling window:

* the open conversation — 2 s loop, and never notified (it is on screen);
* **any other screen while the app is open** — the `CHAT_FOREGROUND_POLL_MS`
  (3 s) loop keeps fetching and notifying;
* app in the background — `CHAT_BACKGROUND_POLL_MS` (10 s);
* app **not running** — the server sends an FCM data message, which starts the
  app's `ChatMessagingService`, fetches and decrypts the message and posts the
  notification. The payload carries no text (only "a chat message is waiting"),
  so nothing readable passes through Firebase. Push needs a Firebase project plus
  a service-account key on the server; without the key the app falls back to the
  polling window above.

Android 13+ asks for `POST_NOTIFICATIONS` on first launch; declining is harmless
— chat still works, just silently.

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
| `chat_attachments` | id, sender, recipient, size — the encrypted blob lives in `backend/uploads/chat/<id>.bin` and is deleted on delivery |
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
| POST | `upload_attachment` | multipart: `peer_id`, `file` (already-encrypted bytes) | `attachment_id`, `bytes` |
| GET | `attachment` | `id` | raw `application/octet-stream`; **deleted once the recipient has it** |
| POST | `delete_message` | `{peer_id, server_id}` | queues a `delete` envelope so both sides drop the message |
| POST | `set_key` | `{public_key, allow_replace?}` | `public_key` (409 when a different key is already published and `allow_replace` is absent) |
| GET | `version` | – | deploy fingerprint (`2.2.0`) |

Rules enforced server-side:

* Body must match `^e2ee:v1:[A-Za-z0-9+/=]+$` and be at most 6000 characters —
  plain text is rejected, so a buggy client cannot leak a message body.
* Sending to a student with no published key returns 409.
* 30 messages per rolling minute per student (HTTP 429 + `Retry-After`).
* You cannot message yourself, an inactive account or an unverified account.
* Attachments: 26 MB per blob server-side (the app stops at 25 MB), 12 uploads per
  rolling minute, only the sender or recipient may fetch one, and the recipient's
  fetch deletes both the file and its row.
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
| `data/local/ChatMediaPrep.kt` | downscales + re-encodes photos, EXIF-aware, 1600 px / q85 |
| `data/local/ChatVoiceRecorder.kt` | AAC voice-note recorder with duration guard |
| `data/local/ChatAttachmentStore.kt` | decrypted attachment files in `filesDir/chat_attachments`, 120 MB cap, wiped on sign-out |
| `data/local/TheStudentDao.kt` | `observeConversation`, `observeSummaries`, upserts, read/pending flags, per-thread and full wipes |
| `data/local/TheStudentDatabase.kt` | Room **version 5** — `MIGRATION_2_3` (history), `MIGRATION_3_4` (attachments), `MIGRATION_4_5` (replies + voice length) |
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
               attachmentId/Name/Mime/Size/Kind/Local/DurationMs
               replyToServerId  replyToIsMine  replyToPreview
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
| Online count flickers | Should not happen on 2.1.0+ — both `threads` and `heartbeat` use one shared, caller-excluded 10 s count on the database clock | Confirm `api/chat.php` was uploaded completely (`api/chat.php?action=version` → 2.2.0) |
| Chat list empty after deploy | `chat.php` returned a 500 | Check `php-error.log`; verify the upload completed |
| Messages send but the other side never sees them | Polling blocked by battery saver, or the recipient's app never fetched the relay before the 7-day TTL | Keep the conversation open; the app polls every 2 s while visible |
| Composer disabled in a conversation | That student has not opened chat on an updated build, so they have no public key yet | They must open the chat tab once |
| History disappeared after reinstalling | The history lived only in the old install's database; there is no backup by design | Expected. Chat works again after the key is rotated automatically |
| "Secure chat could not start: this account already has a different encryption key" | The server published a key this phone cannot reproduce, *and* the local database still has messages | Sign out and in on this phone to clear the local history, then reopen chat to rotate the key |
| Photo or document stuck on "Fetching…" | The blob was already delivered to another install, expired after 7 days, or the send never finished | The caption and file card still show; ask the sender to resend the file |
| "That file is larger than this server accepts (2M)" | PHP `upload_max_filesize` / `post_max_size` on the host | Raise both to 8M in the hosting panel (DirectAdmin → PHP settings) |
| Unread badge missing while on the chat list | Fixed in 2.1.0: a read receipt used to mark *incoming* messages as read | Update the app — no server change needed for this one |
| "Delete for everyone" only removed my copy | The peer's app was offline, so the `delete` envelope is still queued | It disappears on their next inbox poll (2 s), or after 7 days if they never open the app |
| Messages show "🔒 can't be decrypted on this device" | A message was encrypted to a key this phone no longer holds | Expected after a key rotation; new messages decrypt normally |
