---
title: Miss Emma Tutor
emoji: 🎙️
colorFrom: green
colorTo: teal
sdk: docker
app_port: 7860
pinned: false
short_description: OpenAI-compatible voice tutor backend for The Student app
---

# Miss Emma — tutor backend

The smallest backend that can run a phone call with an English learner.

```
POST /v1/chat/completions
{
  "messages": [{"role": "system", "content": "…"}, {"role": "user", "content": "…"}],
  "temperature": 0.6,
  "max_tokens": 200,
  "stream": false
}
```

The reply is a plain OpenAI completion. The app parses `choices[0].message.content`
as a `SAY / FIX / RULE / WORD / NOTE` block — this server never rewrites the system
prompt, because doing so would silently break that parser.

## Settings that matter

| Variable | Default | Why |
| --- | --- | --- |
| `MODEL_REPO` | `bartowski/Qwen3-1.7B-GGUF` | Swap to `Qwen3-4B-Instruct-2507` on paid hardware |
| `MODEL_FILE` | `Qwen3-1.7B-Q4_K_M.gguf` | One quant only — never ship several |
| `N_CTX` | `2048` | The app sends six turns plus the system prompt; 32k would only cost RAM |
| `N_THREADS` | CPU count | 2 on free CPU |
| `DISABLE_THINKING` | `1` | Qwen3 reasoning is silence on a phone call |
| `TUTOR_API_KEY` | *(unset)* | Set it to make the Space private to your build |
| `BUSY_TIMEOUT_SECONDS` | `12` | A busy call gets a fast 503, not a 35-second hang |

## Endpoints

- `GET /health` — 503 while the model loads, 200 when a call can be served
- `GET /v1/models` — OpenAI-compatible model list
- `POST /v1/chat/completions` — the only endpoint the app calls
- `GET /` — a status page, so opening the Space URL shows something useful

## Pointing the app at this Space

The app's default is `https://danuka21-qwen3-8-27b.hf.space`. If the Space name
changes, update `DEFAULT_SPACE_DIRECT_URL` in
`app/src/main/java/com/example/data/remote/HuggingFaceSpaceClient.kt`.
