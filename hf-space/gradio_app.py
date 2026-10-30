"""
Gradio entrypoint — for a Space whose hardware is ZeroGPU.

ZeroGPU is only offered on the Gradio SDK, so a Space pinned to it cannot run a
Dockerfile, however good the Dockerfile is. This file exists to satisfy that
requirement without giving up the app's API: the tutor server is mounted inside a
Gradio app, and the app still calls the same `POST /v1/chat/completions`.

Layering, from the outside in:

    gradio app  ("/ui")      <- exists only because ZeroGPU requires a Gradio app
    tutor API   ("/v1/...")  <- the real server, moved here as tutor_api.py

Nothing in the API changes shape, and `/health` still answers 503 while the model
loads, which is what the phone app listens for.

Note on the GPU: llama.cpp is loaded on the CPU. ZeroGPU hands a GPU to a function
decorated with `@spaces.GPU`, and it is not visible while the model is being
loaded, so pretending to use it would only add a failure mode. If you want GPU
speed, the honest route is a paid GPU with the Docker image — see README.md.
"""

import threading

import gradio as gr

import tutor_api

# Start loading immediately: mounting a Gradio app replaces the FastAPI lifespan,
# so the API's own loader would never run.
threading.Thread(target=tutor_api.start_loader, daemon=True).start()

with gr.Blocks(title="Miss Emma tutor backend") as demo:
    gr.Markdown(
        "## Miss Emma — tutor backend\n\n"
        "This page exists because ZeroGPU requires the Gradio SDK. The app talks to "
        "`POST /v1/chat/completions` instead.\n\n"
        "* `/health` — 503 while the model loads, 200 when a call can be served\n"
        "* `/v1/models` — OpenAI-compatible model list\n"
        "* `/v1/chat/completions` — what the phone calls"
    )
    gr.Markdown(f"**Model repo:** `{tutor_api.MODEL_REPO}`")

# The FastAPI app is mounted at the root; its own routes are matched first, so
# `/v1/chat/completions` and `/health` keep working, and Gradio lives at `/ui`.
app = gr.mount_gradio_app(tutor_api.app, demo, path="/ui")

if __name__ == "__main__":
    import os

    # Gradio Spaces set GRADIO_SERVER_PORT; fall back to the Docker port.
    app_port = int(os.getenv("GRADIO_SERVER_PORT", os.getenv("PORT", "7860")))
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=app_port)
