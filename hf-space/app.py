"""
Miss Emma — the tutor backend for The Student app.

One GGUF model, one OpenAI-compatible endpoint, and nothing else. The app
(`HuggingFaceSpaceClient`) speaks exactly this protocol:

    POST /v1/chat/completions   { messages, temperature, max_tokens, stream:false }
    ->  { choices: [ { message: { role, content }, finish_reason } ], usage }

Everything else here exists only to protect the phone call: the app waits 35 s
for an answer and asks for 200 tokens, and a phone conversation that takes longer
than that is a broken call, not a slow one.

Design notes that matter for this app specifically:

  * The app sends its OWN system prompt (the SAY/FIX/RULE/WORD/NOTE protocol in
    `TutorTurnPrompt`). This server never rewrites or prepends to it — a backend
    that adds "you are a helpful assistant" on top silently breaks the parser.
  * `max_tokens` is clamped, never grown.
  * Context: six turns of history plus the system prompt is under ~1,200 tokens,
    so `n_ctx=2048` is plenty and a small context is what keeps generation fast.
  * One generation at a time. llama.cpp on 2 vCPU does not get faster with a
    second request; both get slower and both miss the deadline. A busy call gets
    an instant 503, which the app already treats as "space is waking up".

Model default is the one this Space already runs:
`ggml-org/gemma-4-E4B-it-GGUF`. The quant is discovered automatically, so a repo
rename or a new Q4 file does not need a code change.
"""

from __future__ import annotations

import os

# The model is 2–5 GB. huggingface_hub reads HF_HOME at import time, so this has
# to be set before it is imported, or every cold start downloads the whole quant
# again into the image's writable layer.
os.environ.setdefault("HF_HOME", "/data/hf")
try:
    os.makedirs(os.environ["HF_HOME"], exist_ok=True)
except OSError:
    pass

import asyncio
import re
import threading
import time
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse
from huggingface_hub import hf_hub_download, list_repo_files

# `llama_cpp` is imported *inside* the loader, not here.
#
# Its wheels are the least predictable part of this image: the prebuilt "CPU"
# index has shipped musl builds that die on a glibc container with
# "libc.musl-x86_64.so.1: cannot open shared object file". An import at module
# scope turns that into a container that never starts, and a Space that will not
# start cannot tell you why. Importing lazily means the API still comes up and
# /health reports the real error.

# ── configuration ────────────────────────────────────────────────────────────
# Everything is an env var, so the Space can be re-pointed without editing code.

DEFAULT_REPO = "ggml-org/gemma-4-E4B-it-GGUF"

# Where the quant is cached. /data survives a restart on a Space; the default
# (~/.cache/huggingface) does not. HF_HOME is set at the top of this file too, but
# Gradio imports huggingface_hub before this module runs, so the download call
# passes cache_dir explicitly rather than trusting the env var.
MODEL_CACHE_DIR = os.getenv("MODEL_CACHE_DIR", "/data/hf")

# Preference order for the quant. Q4_K_M is the size/quality point that fits a
# free Space; the rest are fallbacks in case a repo ships a different set.
QUANT_PREFERENCE = ("q4_k_m", "q4_k_s", "q4_0", "q5_k_m", "q8_0", "q6_k")

MODEL_REPO = os.getenv("MODEL_REPO", DEFAULT_REPO).strip()
# Empty = discover the best quant in MODEL_REPO (the normal case).
MODEL_FILE = os.getenv("MODEL_FILE", "").strip()

MAX_TOKENS_CAP = int(os.getenv("MAX_TOKENS_CAP", "256"))
N_CTX = int(os.getenv("N_CTX", "2048"))
N_THREADS = int(os.getenv("N_THREADS", str(max(1, (os.cpu_count() or 2)))))

# Optional shared secret. Unset = public Space; set = the app must send
# `Authorization: Bearer <value>`.
API_KEY = os.getenv("TUTOR_API_KEY", "").strip()

# A busy server answers immediately instead of queueing past the app's timeout.
BUSY_TIMEOUT_SECONDS = float(os.getenv("BUSY_TIMEOUT_SECONDS", "12"))

THINK_BLOCK = re.compile(r"<think\b[^>]*>.*?<｜end▁of▁thinking｜>", re.DOTALL | re.IGNORECASE)

# Gemma's turn markers, used only if the GGUF has no chat template baked in.
GEMMA_TURN_END = "<end_of_turn>"

llm: Any = None
model_ready = False
load_started = False
load_error = ""
_load_lock = threading.Lock()
has_chat_template = True
model_label = MODEL_FILE or DEFAULT_REPO
generate_lock = threading.Lock()


# ── model discovery ──────────────────────────────────────────────────────────

def _pick_quant(repo_id: str) -> str:
    """
    Choose one GGUF file from the repo.

    Multimodal GGUFs (Gemma E-series among them) ship a separate `mmproj-*.gguf`
    for the vision/audio projector. Downloading that as "the model" would load a
    projector with no weights, so those are excluded by name.
    """
    try:
        files = list_repo_files(repo_id)
    except Exception as error:  # noqa: BLE001 - surface it in the logs, not a crash
        print(f"[tutor] could not list {repo_id}: {error}", flush=True)
        return ""

    candidates = [
        name for name in files
        if name.lower().endswith(".gguf")
        and "mmproj" not in name.lower()
        and "projector" not in name.lower()
    ]
    if not candidates:
        return ""

    for quant in QUANT_PREFERENCE:
        for name in candidates:
            if quant in name.lower():
                return name
    # Nothing matched the preference list: take the smallest plausible file.
    return sorted(candidates, key=lambda n: n.lower())[0]


def load_model() -> None:
    """Download (once) and load the quantised model, then warm it up.

    Guarded so that two entrypoints (the Docker lifespan and the Gradio shim)
    can both ask for it without loading the weights twice.
    """
    global llm, model_ready, has_chat_template, model_label, load_started, load_error

    with _load_lock:
        if load_started:
            return
        load_started = True

    try:
        import llama_cpp

        Llama = llama_cpp.Llama
        print(f"[tutor] llama_cpp {getattr(llama_cpp, '__version__', '?')}", flush=True)
    except Exception as error:  # noqa: BLE001 - reported by /health
        load_error = f"llama_cpp could not load: {error}"
        print(f"[tutor] {load_error}", flush=True)
        return

    local_path = os.getenv("MODEL_PATH", "").strip()
    if not local_path:
        filename = MODEL_FILE or _pick_quant(MODEL_REPO)
        if not filename:
            print(f"[tutor] no .gguf found in {MODEL_REPO}; set MODEL_FILE", flush=True)
            return
        model_label = filename
        try:
            os.makedirs(MODEL_CACHE_DIR, exist_ok=True)
        except OSError:
            pass
        local_path = hf_hub_download(
            repo_id=MODEL_REPO,
            filename=filename,
            cache_dir=MODEL_CACHE_DIR,
        )
        print(f"[tutor] using {MODEL_REPO}/{filename}", flush=True)

    started = time.time()
    try:
        llm = Llama(
            model_path=local_path,
            n_ctx=N_CTX,
            n_threads=N_THREADS,
            n_batch=256,
            use_mmap=True,
            use_mlock=False,
            verbose=False,
        )
    except Exception as error:  # noqa: BLE001 - reported by /health
        # "Failed to load model from file" almost always means the bundled
        # llama.cpp is older than the model's architecture — which is exactly what
        # llama-cpp-python 0.3.2 (Nov 2024) did with Gemma 4. Say so, because the
        # raw message gives no hint and the download has already succeeded.
        hint = ""
        if "Failed to load model" in str(error):
            hint = (" — the bundled llama.cpp probably predates this model's "
                    "architecture; use a newer llama-cpp-python")
        load_error = f"model could not be loaded: {error}{hint}"
        print(f"[tutor] {load_error}", flush=True)
        return
    has_chat_template = bool(
        (getattr(llm, "metadata", {}) or {}).get("tokenizer.chat_template")
    )

    # One token pays the first matmul, the KV allocation and the tokenizer load, so
    # the student's first real turn is not the slow one.
    try:
        generate([{"role": "user", "content": "hi"}], max_tokens=1, temperature=0.0)
    except Exception:  # noqa: BLE001 - a failed warm-up is not fatal
        pass

    model_ready = True
    print(
        f"[tutor] model ready in {time.time() - started:.1f}s "
        f"({model_label}, n_ctx={N_CTX}, threads={N_THREADS}, "
        f"chat_template={has_chat_template})",
        flush=True,
    )


def start_loader() -> None:
    """Begin loading in the background, off whatever loop is serving requests."""
    threading.Thread(target=load_model, daemon=True).start()


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Loading runs off the event loop so /health can answer "warming up" instead
    # of the platform deciding the container is dead.
    start_loader()
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

    The system prompt is always kept — it *is* the tutor's behaviour — and the
    oldest history goes first, because the newest exchange is what the reply is
    about.
    """
    cleaned: list[dict[str, Any]] = []
    for message in messages:
        role = str(message.get("role", "user"))
        content = message.get("content", "")
        if isinstance(content, list):  # tolerate multimodal blocks
            content = " ".join(
                str(part.get("text", "")) for part in content if isinstance(part, dict)
            )
        content = str(content)
        if not content.strip():
            continue
        cleaned.append({
            "role": role if role in {"system", "user", "assistant"} else "user",
            "content": content,
        })
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


def _is_qwen() -> bool:
    return "qwen" in model_label.lower()


def _no_think(messages: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """
    Ask Qwen3 not to think, without touching the app's system prompt.

    Gemma has no such mode, so this is a no-op for it — the *only* model-specific
    behaviour in the server, and it never edits the system message.
    """
    if not _is_qwen():
        return messages
    for message in reversed(messages):
        if message["role"] == "user":
            if "/no_think" not in message["content"]:
                message["content"] = message["content"].rstrip() + " /no_think"
            break
    return messages


def _strip_thinking(text: str) -> str:
    return THINK_BLOCK.sub("", text).strip()


def _manual_prompt(messages: list[dict[str, Any]]) -> str:
    """Gemma turn format, for a GGUF that shipped without a chat template."""
    parts: list[str] = []
    system = [m["content"] for m in messages if m["role"] == "system"]
    if system:
        parts.append(f"<start_of_turn>user\n{system[0]}<end_of_turn>")
    for message in messages:
        if message["role"] == "system":
            continue
        role = "model" if message["role"] == "assistant" else "user"
        parts.append(f"<start_of_turn>{role}\n{message['content']}<end_of_turn>")
    parts.append("<start_of_turn>model")
    return "\n".join(parts)


def generate(
    messages: list[dict[str, Any]],
    max_tokens: int,
    temperature: float,
) -> tuple[str, str]:
    """One completion. Returns (text, finish_reason). Blocking — call off-loop."""
    if llm is None:
        raise RuntimeError("model not loaded")

    if has_chat_template:
        result = llm.create_chat_completion(
            messages=messages,
            max_tokens=max_tokens,
            temperature=temperature,
            top_p=0.9,
            top_k=40,
            repeat_penalty=1.05,
            stop=["\n\n\n"],
        )
        choice = (result.get("choices") or [{}])[0]
        text = (choice.get("message") or {}).get("content", "") or ""
        return text, choice.get("finish_reason", "stop")

    result = llm(
        _manual_prompt(messages),
        max_tokens=max_tokens,
        temperature=temperature,
        top_p=0.9,
        top_k=40,
        repeat_penalty=1.05,
        stop=[GEMMA_TURN_END, "\n\n\n"],
    )
    choice = (result.get("choices") or [{}])[0]
    text = (choice.get("text") or "").replace(GEMMA_TURN_END, "").strip()
    return text, choice.get("finish_reason", "stop")


# ── endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
def health() -> JSONResponse:
    if not model_ready:
        # 503 is what the app already treats as "waking up", and it is honest: the
        # container is up but cannot serve a real request yet. `error` is filled in
        # when the failure is permanent (a bad wheel, a missing quant), so the
        # Space's own URL tells you what is wrong instead of a container log.
        body: dict[str, Any] = {"status": "warming_up", "model": model_label}
        if load_error:
            body = {"status": "error", "model": model_label, "error": load_error}
        return JSONResponse(body, status_code=503)
    return JSONResponse({"status": "ok", "model": model_label})


@app.get("/v1/models")
def models() -> dict[str, Any]:
    return {
        "object": "list",
        "data": [{
            "id": model_label.removesuffix(".gguf") or "tutor",
            "object": "model",
            "owned_by": "local",
        }],
    }


@app.get("/", response_class=HTMLResponse)
def root() -> str:
    state = "ready" if model_ready else ("failed" if load_error else "warming up")
    error_html = f"<p style='color:#b00'><b>{load_error}</b></p>" if load_error else ""
    return (
        "<html><body style='font-family:system-ui;padding:40px'>"
        "<h2>Miss Emma tutor backend</h2>"
        f"<p>Status: <b>{state}</b></p>"
        f"{error_html}"
        f"<p>Model: <code>{MODEL_REPO}/{model_label}</code></p>"
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

    loop = asyncio.get_event_loop()
    acquired = await loop.run_in_executor(
        None, generate_lock.acquire, True, BUSY_TIMEOUT_SECONDS
    )
    if not acquired:
        return JSONResponse(
            {"error": {"message": "Busy, try again.", "type": "busy"}},
            status_code=503,
        )

    try:
        text, finish_reason = await loop.run_in_executor(
            None, generate, messages, max_tokens, temperature
        )
    except Exception as error:  # noqa: BLE001 - the app needs a body, not a traceback
        print(f"[tutor] generation failed: {error}", flush=True)
        return JSONResponse(
            {"error": {"message": "Generation failed.", "type": "server_error"}},
            status_code=500,
        )
    finally:
        generate_lock.release()

    content = _strip_thinking(text)
    if not content:
        return JSONResponse(
            {"error": {"message": "Empty completion.", "type": "empty"}},
            status_code=502,
        )

    return {
        "id": f"chatcmpl-{int(time.time())}",
        "object": "chat.completion",
        "created": int(time.time()),
        "model": model_label.removesuffix(".gguf") or "tutor",
        "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": content},
            "finish_reason": finish_reason,
        }],
        "usage": {
            "prompt_tokens": 0,
            "completion_tokens": 0,
            "total_tokens": 0,
        },
    }


def _port_is_free(port: int) -> bool:
    """Can we bind this port right now?"""
    import socket

    probe = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        probe.bind(("0.0.0.0", port))
        return True
    except OSError:
        return False
    finally:
        probe.close()


if __name__ == "__main__":
    # Same rule as the Gradio entrypoint: whoever already owns the port keeps it,
    # and this process stays alive rather than exiting — uvicorn turns a failed
    # bind into sys.exit(3), which is reported as a runtime error.
    import sys
    import threading

    port = int(os.getenv("PORT", "7860"))
    if not _port_is_free(port):
        print(f"[tutor] port {port} already in use; idling", flush=True)
        threading.Event().wait()
        sys.exit(0)

    try:
        import uvicorn

        uvicorn.run(app, host="0.0.0.0", port=port)
    except BaseException as error:  # noqa: BLE001 - SystemExit from uvicorn included
        print(f"[tutor] server did not start: {error}", flush=True)
        threading.Event().wait()
        sys.exit(0)
