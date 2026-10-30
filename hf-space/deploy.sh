#!/usr/bin/env bash
# Push the tutor backend into the Hugging Face Space.
#
# Run this on your own machine (the sandbox this was written in cannot reach
# huggingface.co, so it cannot push for you). It replaces the Space's app code
# with the four files in this folder, removes the dead weight, commits, and pushes.
#
#   export HF_TOKEN=hf_xxxxxxxx        # write token for the Space
#   ./hf-space/deploy.sh              # clean + push
#   ./hf-space/deploy.sh --no-prune   # push only, keep every existing file
#   ./hf-space/deploy.sh --dry-run    # show what would change
#   ./hf-space/deploy.sh --gradio     # ZeroGPU Spaces: Gradio SDK, no Dockerfile
#
# The Space is a git repo: https://huggingface.co/spaces/danuka21/Qwen3.8-27B
set -euo pipefail

SPACE="${SPACE:-danuka21/Qwen3.8-27B}"
BRANCH="${BRANCH:-main}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PRUNE=true
DRY_RUN=false
GRADIO=false
for arg in "$@"; do
  case "$arg" in
    --no-prune) PRUNE=false ;;
    --dry-run) DRY_RUN=true ;;
    --gradio) GRADIO=true ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) echo "unknown option: $arg" >&2; exit 2 ;;
  esac
done

if [[ -z "${HF_TOKEN:-}" ]]; then
  echo "error: export HF_TOKEN=hf_... (a WRITE token for $SPACE)" >&2
  exit 1
fi

if ! command -v git >/dev/null || ! command -v python3 >/dev/null; then
  echo "error: git and python3 are required" >&2
  exit 1
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "==> cloning $SPACE"
# LFS smudge off: the existing weights are exactly what we are about to delete,
# and downloading them first would take minutes on a large model.
GIT_LFS_SKIP_SMUDGE=1 git clone --depth 1 --branch "$BRANCH" \
  "https://user:${HF_TOKEN}@huggingface.co/spaces/${SPACE}" "$WORK/space" >/dev/null 2>&1 \
  || { echo "error: clone failed — check the token and the Space name" >&2; exit 1; }

cd "$WORK/space"

echo "==> what the Space currently has"
du -sh . 2>/dev/null || true
find . -maxdepth 2 -name '*.gguf' -printf '    %10s  %p\n' 2>/dev/null | sort -nr | head -10 || true

echo "==> validating the files the Space will install"
# pip and apt both read these files literally, and both fail *after* a long
# rebuild when a line is malformed. Three of the last four failures were one bad
# line in one of them, so they are checked before the push instead.
python3 - "$HERE" <<'PYVALIDATE'
import re
import sys
from pathlib import Path

here = Path(sys.argv[1])
# name, optional [extras], optional version specifier — "uvicorn[standard]==0.34.0"
REQ = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*(\[[A-Za-z0-9,._-]+\])?\s*([<>=!~].*)?$")
OPTION = re.compile(r"^(--[a-z-]+(=[^\s]+)?|-[a-zA-Z])$")

problems = []
for name in ("requirements.txt", "requirements-gradio.txt"):
    for number, raw in enumerate((here / name).read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("-"):
            if not OPTION.match(line):
                problems.append(f"{name}:{number}: pip option must be --flag=value -> {line!r}")
            continue
        if not REQ.match(line):
            problems.append(f"{name}:{number}: not a valid requirement -> {line!r}")

for number, raw in enumerate((here / "packages.txt").read_text(encoding="utf-8").splitlines(), 1):
    line = raw.strip()
    if not line or line.startswith("#"):
        continue
    if not re.match(r"^[a-z0-9][a-z0-9.+-]*$", line):
        problems.append(f"packages.txt:{number}: not an apt package name -> {line!r}")

if problems:
    print("error: the Space would fail to build:", file=sys.stderr)
    for problem in problems:
        print(f"  {problem}", file=sys.stderr)
    sys.exit(1)
print("    requirements / packages files look installable")
PYVALIDATE

echo "==> installing the tutor backend"
if [[ "$GRADIO" == true ]]; then
  # ZeroGPU hardware only accepts the Gradio SDK, so the API is mounted inside a
  # Gradio app instead of being served by uvicorn from a Dockerfile.
  cp "$HERE/app.py" ./tutor_api.py
  cp "$HERE/gradio_app.py" ./app.py
  cp "$HERE/requirements-gradio.txt" ./requirements.txt
  # packages.txt is piped into `xargs apt-get install -y`, which treats every
  # token as a package name: a `#` comment becomes "E: Unable to locate package #".
  # Blank lines and comments are stripped here so the file on disk can stay
  # readable without breaking the build.
  grep -v '^[[:space:]]*#' "$HERE/packages.txt" | grep -v '^[[:space:]]*$' > ./packages.txt
  rm -f ./Dockerfile
  # Front matter must describe a Gradio Space. sdk_version is pinned to the one the
  # platform itself installs; python_version is left out so the SDK's base image
  # decides it (the platform ships 3.10 for gradio 5.x — setting 3.11 here is how a
  # Space ends up building against a Python it will not run on).
  python3 - "$HERE/README.md" > ./README.md <<'PY'
import sys

text = open(sys.argv[1], encoding="utf-8").read()
body = text.split("---", 2)[2].lstrip("\n") if text.startswith("---") else text

print("""---
title: Miss Emma Tutor
emoji: \U0001F399\uFE0F
colorFrom: green
colorTo: blue
sdk: gradio
sdk_version: 5.15.0
app_file: app.py
pinned: false
short_description: OpenAI-compatible voice tutor backend for The Student app
---
""")
print(body, end="")
PY
else
  cp "$HERE/app.py" ./app.py
  cp "$HERE/requirements.txt" ./requirements.txt
  cp "$HERE/Dockerfile" ./Dockerfile
  cp "$HERE/README.md" ./README.md
  rm -f ./tutor_api.py ./packages.txt
fi

if [[ "$PRUNE" == true ]]; then
  echo "==> removing what the app never uses"
  # Weights: the backend downloads its own quant at boot and caches it in /data,
  # so committed weights are only LFS quota and clone time.
  find . -name '*.gguf' -not -path './.git/*' -print -delete 2>/dev/null || true
  # A Space serving an API needs no demo, notebook, dataset or screenshot.
  find . -type d \
    \( -name '__pycache__' -o -name '.ipynb_checkpoints' -o -name 'examples' \
       -o -name 'assets' -o -name 'images' -o -name 'samples' \) \
    -not -path './.git/*' -prune -exec rm -rf {} + 2>/dev/null || true
  find . -maxdepth 1 \( -name '*.ipynb' -o -name '*.jsonl' -o -name '*.parquet' -o -name '*.csv' \) \
    -print -delete 2>/dev/null || true
  find . -maxdepth 1 -name 'requirements*.txt' ! -name 'requirements.txt' -print -delete 2>/dev/null || true
  # Duplicate entrypoints and dead training scripts.
  for f in "app (1).py" app_backup.py app_old.py test.py inference.py train.py \
           finetune.py fine_tune.py app_gradio.py; do
    if [[ -e "$f" ]]; then echo "    - $f"; rm -f "$f"; fi
  done
fi

echo "==> files after the change"
find . -path ./.git -prune -o -type f -print | sed 's|^\./||' | sort | head -40
echo "    (total: $(find . -path ./.git -prune -o -type f -print | wc -l | tr -d ' ') files)"

if ! git status --porcelain | grep -q .; then
  echo "==> nothing to push — the Space already matches this folder"
  exit 0
fi

echo "==> changed files"
git status --porcelain | sed 's/^/    /'

if [[ "$DRY_RUN" == true ]]; then
  echo "==> dry run: not committing"
  exit 0
fi

git config user.email "${GIT_AUTHOR_EMAIL:-deploy@thestudent.local}"
git config user.name "${GIT_AUTHOR_NAME:-TheStudent deploy}"
git add -A
if [[ "$GRADIO" == true ]]; then
  COMMIT_MSG="Tutor backend on the Gradio SDK (ZeroGPU hardware): API mounted, one GGUF model"
else
  COMMIT_MSG="Tutor backend: one GGUF model, OpenAI-compatible API, nothing else"
fi
git commit -m "$COMMIT_MSG



- app.py: /v1/chat/completions + /health + /v1/models, quant auto-discovered from
  ggml-org/gemma-4-E4B-it-GGUF, max_tokens clamped to the app's budget, n_ctx 2048,
  one generation lock with an instant 503 when busy, warm-up at boot
- the app's SAY/FIX/RULE/WORD/NOTE system prompt is never rewritten
- removed duplicate weights, demo apps, notebooks and unused requirements
- Dockerfile/README: docker SDK on port 7860, env table documented"

echo "==> pushing"
git push "https://user:${HF_TOKEN}@huggingface.co/spaces/${SPACE}" "HEAD:${BRANCH}"

echo
SUBDOMAIN="$(printf '%s-%s' "${SPACE%%/*}" "${SPACE##*/}" \
  | tr '[:upper:]' '[:lower:]' | sed 's/[._ ]/-/g' | sed 's/[^a-z0-9-]//g')"

echo "==> done. The Space is rebuilding; then check:"
echo "    curl -i https://${SUBDOMAIN}.hf.space/health        # 503 while loading, 200 when ready"
echo "    curl -s https://${SUBDOMAIN}.hf.space/v1/models      # 200 with the model id"
echo "    open https://huggingface.co/spaces/${SPACE}          # build logs"
