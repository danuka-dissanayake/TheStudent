# syntax=docker/dockerfile:1

# A CPU-only image for one GGUF model.
#
# The reason this is a Dockerfile rather than a Gradio Space is build time: the
# React/Gradio path pulls a Python stack that is an order of magnitude larger than
# what this needs, and a Space rebuilds — and cold-starts — every time the app
# wakes from sleep. Smaller image, faster wake-up, and the app's 35-second read
# timeout stops being a gamble.
FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PORT=7860 \
    HF_HOME=/data/hf

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential cmake curl \
    && rm -rf /var/lib/apt/lists/*

# Prebuilt CPU wheels where available (seconds), source build only as a fallback.
COPY requirements.txt .
RUN (pip install --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cpu \
        -r requirements.txt \
     || pip install -r requirements.txt) \
    && apt-get purge -y build-essential cmake \
    && apt-get autoremove -y

COPY app.py .

# The model is downloaded at first boot and cached in /data, which HF persists for
# this Space — so a restart does not re-download ~1 GB.
RUN mkdir -p /data/hf

EXPOSE 7860

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:7860/health || exit 1

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7860", "--workers", "1"]
