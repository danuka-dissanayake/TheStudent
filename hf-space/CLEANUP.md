# Cleaning up `danuka21/Qwen3.8-27B`

Written to be followed in the Space UI, top to bottom. Nothing here needs the
token you pasted, and nothing here touches the app.

> **First: revoke that token.** It was posted in a chat, so treat it as public.
> huggingface.co → Settings → Access Tokens → *Revoke*. A write token can change
> every repo you own. Then create a **fine-grained read token** for the app, scoped
> to this one Space, and put it in the app's own settings — never in the repo
> (`.env` is already gitignored, use it there).

---

## 1. What the app actually needs from the Space

Exactly this, and nothing else:

| The app sends | The Space must return |
| --- | --- |
| `POST /v1/chat/completions` | `choices[0].message.content` |
| `messages[]` (its own system prompt + 6 turns + the student's line) | text, under **35 s** |
| `max_tokens: 200`, `temperature: 0.6`, `stream: false` | nothing else is read |

The app also treats **401/403** as "private, needs a token" and **503/504** as
"waking up" — so the backend should return 503 while the model is still loading
rather than hanging for 35 seconds.

## 2. Delete — files

Everything here is dead weight that slows every rebuild and every cold start:

- [ ] **Extra model weights.** Keep **one** `.gguf` (Q4_K_M). `.gitattributes`
      LFS entries for the others go with it. Two quants = double the download and
      a slower boot for zero benefit.
- [ ] **`requirements.txt`**: drop `torch`, `torchvision`, `transformers`,
      `accelerate`, `bitsandbytes`, `xformers`, `sentencepiece`, `triton`,
      `flash-attn`, `gradio` (if you switch to the Docker backend in this folder).
      The GGUF path needs **four** packages.
- [ ] Duplicate entrypoints: `app (1).py`, `app_backup.py`, `test.py`,
      `inference.py`, `train.py`, `fine_tune*.py`, any `.ipynb`.
- [ ] `examples/`, `assets/`, `images/`, `samples/`, `__pycache__/`,
      `.ipynb_checkpoints/` — a backend has no screenshots.
- [ ] Datasets committed to the repo, if any (`.jsonl`, `.csv`, `.parquet`, `.zip`).
      A tuning set belongs in the Dataset tab or a private repo, not in the Space.
- [ ] The Gradio demo, if you keep the API: the app never opens it, and it keeps
      the container alive doing nothing.

## 3. Change — settings

- [ ] **Hardware.** A 27B-class model cannot serve a phone call from free CPU; it
      either times out or speaks after 30 seconds. The app's whole design assumes
      a reply in a couple of seconds. Use a 1.7B–4B quantised model on CPU, or a
      paid GPU if you want the bigger model. `README.md` in this folder documents
      both.
- [ ] **Sleep time.** Free Spaces sleep; the first call after sleep is the ~60 s
      cold start. Either accept it (the app shows "waking up") or pay for
      "always on" — but do not paper over it with a shorter timeout in the app.
- [ ] **Private + token**, not public: a write-token Space is an open door to your
      quota. Set `TUTOR_API_KEY` and hand the app that value.
- [ ] Pin the SDK/version in the Space settings so a redeploy is reproducible.

## 4. Replace — code

The four files in this folder are the whole backend:

| File | What it does |
| --- | --- |
| `app.py` | OpenAI-compatible endpoint, one model, one lock, thinking disabled |
| `requirements.txt` | four packages |
| `Dockerfile` | CPU image, prebuilt llama.cpp wheels, model cached in `/data` |
| `README.md` | Space card + env table |

Why each piece exists:

- **`max_tokens` is clamped, never grown.** The app sends 200; a backend that
  quietly generates 800 tokens is what turns a 3-second reply into a 25-second one.
- **`n_ctx=2048`.** The app sends six turns; a 32k context reserves memory the
  call never uses and slows every token.
- **`DISABLE_THINKING=1`.** Qwen3's reasoning block is silence in the student's
  ear, and the app's parser would have to strip it anyway.
- **One generation lock.** On 2 vCPU a second concurrent request does not go
  faster, it makes both miss the 35-second deadline. Busy → instant 503.
- **Warm-up at boot.** One 1-token generation pays the first-matmul and tokenizer
  cost, so the student's first real turn is not the slow one.
- **No prompt rewriting.** The app's `TutorTurnPrompt` is the tutor's entire
  behaviour; adding a persona on top breaks the `SAY/FIX/RULE/WORD/NOTE` parser.

## 5. Verify

After the rebuild settles:

```bash
# 1. is it up? (503 = loading, 200 = ready)
curl https://<space-subdomain>.hf.space/health

# 2. does it answer in the app's format?
curl -s https://<space-subdomain>.hf.space/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TUTOR_API_KEY" \
  -d '{"messages":[{"role":"system","content":"Answer with SAY: only."},
                   {"role":"user","content":"hello"}],
       "max_tokens":60,"temperature":0.6,"stream":false}'
```

Then in the app: Settings → Hugging Face base URL (if you use a custom one) →
start a call. The call should greet within a couple of seconds, and the summary
should show a non-zero latency in `Connected (…ms)`.

## 6. Not worth adding

- A second model for "better grammar" inside the Space — the app already has
  `EnglishGrammarRules` and `WordUpgrades` running on the phone, instantly and
  offline. A backend that also corrects will disagree with them.
- Streaming: the app requests `stream:false` and plays audio sentence by sentence
  from a complete reply. Add streaming only if the app is changed to parse
  partial `SAY:` blocks, otherwise it buys nothing.
- Chat history storage in the Space: the app keeps its own transcript, and a
  shared Space would leak one student's conversation into another's.
