"""
Miss Emma — the tutor backend for The Student app.

A deliberately small server: one GGUF model, one OpenAI-compatible endpoint, and
nothing else. The app (`HuggingFaceSpaceClient`) speaks exactly this protocol:

    POST /v1/chat/completions   { messages, temperature, max_tokens, stream:false }
    ->  { choices: [ { message: { role, content }, finish_reason } ], usage }

Anything this file does beyond that is either (a) protecting the model from the
app's 35-second read timeout, or (b) keeping a phone conversation from being
slow. There is no Gradio UI, no training code, no dataset, and no second model —
every one of those costs boot time on every restart and buys the call nothing.

Design notes that matter for the app specifically:

  * The app sends its OWN system prompt (the SAY/FIX/RULE/WORD/NOTE protocol in
    `TutorTurnPrompt`). This server never rewrites or prepends to it. A backend
    that adds "you are a helpful assistant" on top silently breaks the parser.
  * `max_tokens` from the app is 200 for a tutor turn. That is the whole budget:
    we clamp it, never grow it.
  * Context: the app keeps six turns of history plus the system prompt, which is
    under ~1,200 tokens. `n_ctx=2048` is therefore plenty, and a small context is
    what makes the model fit and stay fast on 2 vCPU.
  * One generation at a time. llama.cpp on 2 cores does not get faster with a
    second request; it gets two slow ones, and the second loses the race with the
    35-second timeout. A busy call gets a fast 503 instead, which the app already
    understands as "space is waking up".
"""

from __future__ import annotations

import asyncio
import os
import re
import threading
import time
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse
from huggingface_hub import hf_hub_download
from llama_cpp import Llama

# ── configuration ────────────────────────────────────────────────────────────
# Everything is an env var so the Space can be re-pointed without editing code.

MODEL_REPO = os.getenv("MODEL_REPO", "bartowski/Qwen3-1.7B-GGUF")
MODEL_FILE = os.getenv("MODEL_FILE", "Qwen3-1.7B-Q4_K_M.gguf")

# The app asks for 200 tokens and will not wait longer than 35 s. Keep a margin.
MAX_TOKENS_CAP = int(os.getenv("MAX_TOKENS_CAP", "256"))
N_CTX = int(os.getenv("N_CTX", "2048"))
N_THREADS = int(os.getenv("N_THREADS", str(max(1, (os.cpu_count() or 2)))))

# Qwen3 ships a thinking mode. On a phone call there is no room for it: the
# student hears silence while the model reasons, and the app's parser would have
# to strip the block anyway.
DISABLE_THINKING = os.getenv("DISABLE_THINKING", "1") == "1"

# Optional shared secret. Leave unset and the Space is public; set it and the app
# must send `Authorization: Bearer <value>`.
API_KEY = os.getenv("TUTOR_API_KEY", "").strip()

# A busy server returns 503 immediately rather than queueing past the timeout.
BUSY_TIMEOUT_SECONDS = float(os.getenv("BUSY_TIMEOUT_SECONDS", "12"))

THINK_BLOCK = re.compile(r"<think\b[^>]*>.*?<｜end▁of▁thinking｜>", re.DOTALL | re.IGNORECASE)

llm: Llama | None = None
model_ready = False
generate_lock = threading.Lock()


def load_model() -> None:
    """Download (once) and load the quantised model, then warm it up."""
    global llm, model_ready

    local_path = os.getenv("MODEL_PATH", "").strip()
    if not local_path:
        local_path = hf_hub_download(repo_id=MODEL_REPO, filename=MODEL_FILE)

    started = time.time()
    llm = Llama(
        model_path=local_path,
        n_ctx=N_CTX,
        n_threads=N_THREADS,
        n_batch=256,
        # Keep the whole model in RAM: a Space restart should not re-read the file
        # from disk on the first request.
        use_mmap=True,
        use_mlock=False,
        verbose=False,
    )
    # A one-token generation pays the cost of the first matmul, the KV allocation
    # and the tokenizer load, so the student's first real turn is not the one
    # that takes five seconds.
    try:
        llm.create_chat_completion(
            messages=[{"role": "user", "content": "hi"}],
            max_tokens=1,
            temperature=0.0,
        )
    except Exception:
        pass
    model_ready = True
    print(f"[tutor] model ready in {time.time() - started:.1f}s "
          f"({MODEL_REPO}/{MODEL_FILE}, n_ctx={N_CTX}, threads={N_THREADS})", flush=True)


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Loading happens off the event loop so /health can answer "warming up"
    # instead of the platform deciding the container is dead.
    threading.Thread(target=load_model, daemon=True).start()
    yield


app = FastAPI(title="Miss Emma tutor backend", lifespan=lifespan)


# ── helpers ──────────────────────────────────────────────────────────────────

def _authorised(request: Request) -> bool:
    if not API_KEY:
        return True
    header = request.headers.get("authorization", "")
    return header.removeprefix("Bearer ").strip() == API_KEY


def _clamp_messages(messages: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """
    Keep the request inside the context window.

    The system prompt is always kept — it is the tutor's whole behaviour — and the
    oldest history is dropped first, because the newest exchange is what the reply
    is about.
    """
    cleaned: list[dict[str, Any]] = []
    for message in messages:
        role = str(message.get("role", "user"))
        content = message.get("content", "")
        if isinstance(content, list):  # tolerate multimodal blocks from clients
            content = " ".join(
                str(part.get("text", "")) for part in content if isinstance(part, dict)
            )
        content = str(content)
        if not content.strip():
            continue
        cleaned.append({"role": role if role in {"system", "user", "assistant"} else "user",
                        "content": content})
    if len(cleaned) <= 1:
        return cleaned

    budget_chars = N_CTX * 3  # ~3 chars/token, deliberately conservative
    system = [m for m in cleaned if m["role"] == "system"]
    rest = [m for m in cleaned if m["role"] != "system"]
    used = sum(len(m["content"]) for m in system)
    kept: list[dict[str, Any]] = []
    for message in reversed(rest):
        if used + len(message["content"]) > budget_chars:
            break
        kept.append(message)
        used += len(message["content"])
    kept.reverse()
    return system + kept


def _no_think(messages: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """
    Ask Qwen3 not to think, without touching the app's system prompt.

    `/no_think` is a soft switch the tokenizer understands; the system prompt
    above it stays exactly as the app wrote it.
    """
    if not DISABLE_THINKING:
        return messages
    for message in reversed(messages):
        if message["role"] == "user":
            if "/no_think" not in message["content"]:
                message["content"] = message["content"].rstrip() + " /no_think"
            break
    return messages


def _strip_thinking(text: str) -> str:
    return THINK_BLOCK.sub("", text).strip()


# ── endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
def health() -> JSONResponse:
    if not model_ready:
        # 503 is what the app already treats as "waking up", and it is honest:
        # the container is up but cannot answer a real request yet.
        return JSONResponse({"status": "warming_up"}, status_code=503)
    return JSONResponse({"status": "ok", "model": MODEL_FILE})


@app.get("/v1/models")
def models() -> dict[str, Any]:
    return {
        "object": "list",
        "data": [{"id": MODEL_FILE.removesuffix(".gguf"), "object": "model", "owned_by": "local"}],
    }


@app.get("/", response_class=HTMLResponse)
def root() -> str:
    state = "ready" if model_ready else "warming up"
    return (
        "<html><body style='font-family:system-ui;padding:40px'>"
        f"<h2>Miss Emma tutor backend</h2><p>Status: <b>{state}</b></p>"
        f"<p>Model: <code>{MODEL_FILE}</code></p>"
        "<p>Endpoint: <code>POST /v1/chat/completions</code></p>"
        "</body></html>"
    )


@app.post("/v1/chat/completions")
async def chat_completions(request: Request):
    if not _authorised(request):
        raise HTTPException(status_code=401, detail="Invalid or missing API key.")
    if not model_ready or llm is None:
        return JSONResponse(
            {"error": {"message": "Model is still loading.", "type": "warming_up"}},
            status_code=503,
        )

    try:
        payload = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Body must be JSON.")

    raw_messages = payload.get("messages") or []
    if not isinstance(raw_messages, list) or not raw_messages:
        raise HTTPException(status_code=400, detail="`messages` is required.")

    messages = _no_think(_clamp_messages(raw_messages))
    try:
        max_tokens = int(payload.get("max_tokens") or 200)
    except (TypeError, ValueError):
        max_tokens = 200
    max_tokens = max(16, min(max_tokens, MAX_TOKENS_CAP))
    try:
        temperature = float(payload.get("temperature") or 0.6)
    except (TypeError, ValueError):
        temperature = 0.6
    temperature = max(0.0, min(temperature, 1.2))

    # One generation at a time. A second caller waits a little, then is told to
    # come back — a 12-second wait beats a 35-second timeout with no answer.
    acquired = await asyncio.get_event_loop().run_in_executor(
        None, generate_lock.acquire, True, BUSY_TIMEOUT_SECONDS
    )
    if not acquired:
        return JSONResponse(
            {"error": {"message": "Busy, try again.", "type": "busy"}},
            status_code=503,
        )

    try:
        result = await asyncio.get_event_loop().run_in_executor(
            None,
            lambda: llm.create_chat_completion(
                messages=messages,
                max_tokens=max_tokens,
                temperature=temperature,
                top_p=0.9,
                top_k=40,
                repeat_penalty=1.05,
                stop=["\n\n\n"],
            ),
        )
    except Exception as error:  # noqa: BLE001 - the app needs a body, not a traceback
        print(f"[tutor] generation failed: {error}", flush=True)
        return JSONResponse(
            {"error": {"message": "Generation failed.", "type": "server_error"}},
            status_code=500,
        )
    finally:
        generate_lock.release()

    choice = (result.get("choices") or [{}])[0]
    content = _strip_thinking((choice.get("message") or {}).get("content", "") or "")
    if not content:
        return JSONResponse(
            {"error": {"message": "Empty completion.", "type": "empty"}},
            status_code=502,
        )

    return {
        "id": result.get("id", "chatcmpl-local"),
        "object": "chat.completion",
        "created": int(time.time()),
        "model": MODEL_FILE.removesuffix(".gguf"),
        "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": content},
            "finish_reason": choice.get("finish_reason", "stop"),
        }],
        "usage": result.get("usage", {}),
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "7860")))
