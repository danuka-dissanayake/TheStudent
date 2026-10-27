# Student Chat (production)

1:1 messaging between **registered TheStudent students**, backed by MySQL and
served by `backend/api/chat.php`. There is no mock data left in the app: the
chat tab reads real accounts, real messages, real presence.

---

## 1. What students see

| Screen | What it shows | Where it comes from |
| --- | --- | --- |
| Chat tab | Every active, e-mail-verified student — **full name only** (privacy), plus their uploaded profile picture when they have one, else an initials avatar | `GET api/chat.php?action=threads` |
| Online row | Students active in the last 45 seconds, with a live count ("12 online") | same response, `online_count` |
| Thread row | Last message preview, "typing…", unread badge, time stamp | same response |
| Conversation | Message history, own bubbles on the right, read ticks, "typing…" bubble, "Online" / "last seen 5 min ago" | `GET api/chat.php?action=messages` |
| Safety notice | Bottom banner: *never share phone numbers, home address, school, bank details or passwords* — shown **once per account** until acknowledged | `users.chat_safety_ack_at` |

Presence, typing, unread badges and the online counter are all refreshed by
polling (2.5 s inside a conversation, 5 s on the list, presence heartbeat every
20 s). Nothing is faked or randomised client-side.

**Privacy guarantees**

* The API never returns e-mail, phone, institution, stream or role.
* Only `id`, `full_name`, `profile_picture` and coarse presence leave the server.
* Presence is derived from `users.last_seen_at` (45 s window) — no IP addresses
  or device details are exposed to other students.
* "Clear chat" removes the conversation **for me only**; the other student keeps
  their copy (stored as `chat_clears.cleared_seq`).

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
| `users.last_seen_at` | presence heartbeat |
| `users.chat_safety_ack_at` | one-time safety notice acknowledgement |

No new secrets, keys or environment variables are required — chat reuses the
existing session token from `auth.php`.

---

## 3. API reference

All endpoints require `Authorization: Bearer <session token>`; the token is
never accepted in a query string. Successful responses use
`{"status":"success", ...}`; failures use `{"status":"error","message":"…"}`.

| Method | Action | Body / query | Returns |
| --- | --- | --- | --- |
| GET | `threads` | – | `threads[]`, `online_count`, `total_students`, `safety_acknowledged`, `server_epoch` |
| GET | `messages` | `peer_id`, optional `after_id` | `peer`, `messages[]`, `peer_is_typing`, `safety_acknowledged` |
| POST | `send` | `{peer_id, body}` | `message` |
| POST | `typing` | `{peer_id, is_typing}` | – |
| POST | `read` | `{peer_id}` | `updated` |
| POST | `clear` | `{peer_id}` | `cleared_seq` |
| POST | `ack_safety` | – | `safety_acknowledged: true` |
| POST | `heartbeat` | – | `online_count` |
| GET | `version` | – | deploy fingerprint |

Message rules enforced server-side:

* 1–2000 characters, control characters stripped, blank messages rejected.
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
| `ui/screens/ChatScreen.kt` | Directory, online rail, conversation, safety banner, clear-chat dialog |

The chat tab uses the app-wide emerald wash (`TheStudentAppBackground`) plus the
onboarding background art, and keeps the status bar transparent so it matches
every other screen.

Polling intervals (constants in `TheStudentViewModel`):

```
CHAT_DIRECTORY_POLL_MS = 5_000
CHAT_MESSAGES_POLL_MS  = 2_500
CHAT_PRESENCE_MS       = 20_000   // must stay below the 45 s online window
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
| Messages send but the other side never sees them | Polling blocked by battery saver | Keep the conversation open; the app polls every 2.5 s while visible |
