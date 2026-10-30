---
title: Miss Emma Tutor
emoji: 🎙️
colorFrom: green
colorTo: blue
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

**Model:** `ggml-org/gemma-4-E4B-it-GGUF` (the name of this Space is historical —
the weights are Gemma 4 E4B, not Qwen). The quant is discovered automatically:
Q4_K_M first, then Q4_K_S, Q4_0, Q5_K_M, Q8_0. `mmproj-*` projector files are
skipped, because loading one of those as "the model" loads no weights at all.

## Settings that matter

| Variable | Default | Why |
| --- | --- | --- |
| `MODEL_REPO` | `ggml-org/gemma-4-E4B-it-GGUF` | Use `…gemma-4-E2B-it-GGUF` on free CPU if replies feel slow |
| `MODEL_FILE` | *(empty = auto)* | Set only to pin a specific quant |
| `N_CTX` | `2048` | The app sends six turns plus the system prompt; 32k only costs RAM |
| `N_THREADS` | CPU count | 2 on free CPU |
| `MAX_TOKENS_CAP` | `256` | The app asks for 200; the cap stops a client asking for 2000 |
| `TUTOR_API_KEY` | *(unset)* | Set it to make the Space private to your build |
| `BUSY_TIMEOUT_SECONDS` | `12` | A busy call gets a fast 503, not a 35-second hang |

Qwen-specific: if you point `MODEL_REPO` at a Qwen3 repo, the server appends
`/no_think` to the newest user message so the model does not spend the call
reasoning in silence. Gemma has no such mode and is left untouched.

## Endpoints

- `GET /health` — 503 while the model loads, 200 when a call can be served
- `GET /v1/models` — OpenAI-compatible model list
- `POST /v1/chat/completions` — the only endpoint the app calls
- `GET /` — a status page, so opening the Space URL shows something useful

## ZeroGPU Spaces

ZeroGPU hardware is only offered on the **Gradio** SDK — a Space pinned to it
refuses a Dockerfile with *"ZeroGPU is only available on Gradio SDK"*. Two ways
out, and the first is better:

1. **Settings → Hardware → `CPU basic · 2 vCPU · 16 GB · FREE`.** The Docker
   image then builds as written. Nothing in this folder changes.
2. **Keep ZeroGPU:** deploy the Gradio variant, which mounts the same API inside a
   Gradio app (`app.py` = `gradio_app.py`, the server becomes `tutor_api.py`):

   ```bash
   bash hf-space/deploy.sh --gradio
   ```

   Two rules for this path, both learned on this Space:

   * **Never pin a package the platform pins.** The Gradio SDK installs `torch`,
     `gradio[oauth]==5.15.0`, `spaces==0.51.3`, `uvicorn` and `websockets` on top
     of `requirements.txt`; a second pin fails with `ResolutionImpossible`.
   * **Never trust the prebuilt CPU wheel index.** It has shipped musl builds
     (`libc.musl-x86_64.so.1: cannot open shared object file`) to this glibc
     image. `requirements-gradio.txt` builds `llama-cpp-python` from source
     (`--no-binary`), with the compiler from `packages.txt`.

   `packages.txt` must contain **package names only** — the platform pipes it
   into `xargs apt-get install -y`, so a `#` comment becomes
   `E: Unable to locate package #`. `deploy.sh` strips comments and blank lines
   before pushing it.

   `tutor_api.py` also imports `llama_cpp` lazily, so a broken library cannot
   stop the container: `/health` returns `503 {"status":"error","error":"…"}` with
   the real reason instead of a crash loop.

   The phone app is unaffected: it still calls `POST /v1/chat/completions`. The
   model runs on the CPU part of the container either way, because ZeroGPU only
   hands a GPU to a `@spaces.GPU`-decorated function and the weights are loaded
   long before any call. For real GPU speed, use a paid GPU with the Docker image.

## Deploying from this folder

```bash
export HF_TOKEN=hf_...          # a WRITE token for the Space
./deploy.sh                    # clean + push (Docker SDK)
./deploy.sh --gradio           # Gradio SDK + packages.txt, for ZeroGPU
./deploy.sh --gradio           # Gradio SDK, for ZeroGPU hardware
./deploy.sh --dry-run          # show what would change
./deploy.sh --no-prune         # push the code, delete nothing
```

`deploy.sh` clones the Space, installs these four files, removes the dead weight
(extra `.gguf`, notebooks, datasets, demo folders, duplicate entrypoints), commits
and pushes.

## Pointing the app at this Space

The app's default is `https://danuka21-qwen3-8-27b.hf.space`. If the Space name
changes, update `DEFAULT_SPACE_DIRECT_URL` in
`app/src/main/java/com/example/data/remote/HuggingFaceSpaceClient.kt`.
