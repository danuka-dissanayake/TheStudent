"""
Gradio entrypoint — for a Space whose hardware is ZeroGPU.

ZeroGPU is only offered on the Gradio SDK, so a Space pinned to it cannot run a
Dockerfile, however good the Dockerfile is. This file exists to satisfy that
requirement without giving up the app's API: the tutor server is mounted inside a
Gradio app, and the phone still calls `POST /v1/chat/completions`.

Layering, from the outside in:

    gradio UI  ("/ui")       <- exists only because ZeroGPU requires a Gradio app
    tutor API  ("/v1/...")   <- the real server, shipped here as tutor_api.py

Only `app` is exported. The Spaces runtime uses a module-level `app` when it is an
ASGI application, and a module-level `demo` when it is a `gr.Blocks`; exporting
both would let the runtime pick the Blocks and serve the UI alone, with every
`/v1/...` call answering 404. The Blocks object lives inside `app`, which is all
that is needed.

Note on the GPU: llama.cpp loads on the CPU. ZeroGPU hands a GPU only to a
function decorated with `@spaces.GPU`, and the weights are loaded long before any
request arrives, so pretending otherwise would add a failure mode and no speed.
For real GPU speed, use a paid GPU with the Docker image — see README.md.
"""

import threading

import gradio as gr

import tutor_api

# Start loading now: mounting a Gradio app replaces the FastAPI lifespan, so the
# API's own loader would never run and /health would stay "warming up" forever.
threading.Thread(target=tutor_api.start_loader, daemon=True).start()


def _build_ui() -> gr.Blocks:
    with gr.Blocks(title="Miss Emma tutor backend") as blocks:
        gr.Markdown(
            "## Miss Emma — tutor backend\n\n"
            "This page exists because ZeroGPU requires the Gradio SDK. The app talks "
            "to `POST /v1/chat/completions`.\n\n"
            "* `/health` — 503 while the model loads, 200 when a call can be served\n"
            "* `/v1/models` — OpenAI-compatible model list\n"
            "* `/v1/chat/completions` — what the phone calls"
        )
        gr.Markdown(f"**Model repo:** `{tutor_api.MODEL_REPO}`")
    return blocks


_blocks = _build_ui()

# The API keeps the root; the UI lives at /ui.
app = gr.mount_gradio_app(tutor_api.app, _blocks, path="/ui")


if __name__ == "__main__":
    # Two launch paths exist, and starting a second server is fatal:
    #
    #   * the Space platform may already serve this module (it detects the ASGI
    #     `app` and binds GRADIO_SERVER_PORT itself) — then our bind fails with
    #     "[Errno 98] address already in use" and the container exits 3;
    #   * or nothing is serving it, and this block is the only server.
    #
    # So: try to bind, and if the port is taken, keep the process alive and let
    # whoever holds it serve. Exiting here is what turned a working Space into a
    # "runtime error" with a model that had already downloaded successfully.
    import os
    import sys
    import threading

    import uvicorn

    port = int(os.getenv("GRADIO_SERVER_PORT") or os.getenv("PORT") or "7860")
    try:
        uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
    except OSError as error:
        print(f"[tutor] not starting a second server on {port}: {error}", flush=True)
        print("[tutor] the platform's own server is serving this app", flush=True)
        threading.Event().wait()
        sys.exit(0)
