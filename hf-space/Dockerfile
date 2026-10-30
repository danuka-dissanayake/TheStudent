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

# Try the prebuilt CPU wheel first (seconds), then *prove it imports*. The index
# has shipped musl builds that install cleanly and then fail on this glibc image
# with "libc.musl-x86_64.so.1: cannot open shared object file", so the check is an
# import, not an install. If it fails, rebuild from source — that is why the
# compiler is still installed at this point.
COPY requirements.txt .
RUN pip install --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cpu \
        -r requirements.txt \
    && (python -c "import llama_cpp" \
        || pip install --no-cache-dir --force-reinstall --no-binary llama-cpp-python \
               "llama-cpp-python==0.3.2") \
    && python -c "import llama_cpp; print('llama_cpp ok')" \
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
