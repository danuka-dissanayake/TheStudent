#!/usr/bin/env bash
# Restore the Space to the version that worked, by resetting its git history.
#
# Nothing was ever deleted from the Space's history: every push rewrote files, so
# the previous app, Dockerfile, requirements and even the old .gguf pointers are
# still reachable by SHA. This finds the commit before the first "Tutor backend"
# deploy, checks it out and force-pushes it back.
#
#   export HF_TOKEN=hf_...            # write token for the Space
#   ./hf-space/restore.sh             # show what will be restored (no push)
#   ./hf-space/restore.sh --yes       # actually restore
#   ./hf-space/restore.sh --to <sha> --yes    # restore to a specific commit
#
# The Space is https://huggingface.co/spaces/danuka21/Qwen3.8-27B
set -euo pipefail
export GIT_LFS_SKIP_SMUDGE=1     # never download the old weights just to push them back

SPACE="${SPACE:-danuka21/Qwen3.8-27B}"
BRANCH="${BRANCH:-main}"
TARGET=""
CONFIRMED=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --to) TARGET="${2:-}"; shift 2 ;;
    --yes) CONFIRMED=true; shift ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

[[ -n "${HF_TOKEN:-}" ]] || { echo "error: export HF_TOKEN=hf_... (write token for $SPACE)" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "==> cloning $SPACE (history only, no LFS)"
git clone "https://user:${HF_TOKEN}@huggingface.co/spaces/${SPACE}" "$WORK/space" >/dev/null 2>&1 \
  || { echo "error: clone failed — check the token and the Space name" >&2; exit 1; }
cd "$WORK/space"

echo "==> recent history (newest first)"
git log --format='    %h  %ad  %s' --date=short -n 15

if [[ -z "$TARGET" ]]; then
  # Our deploys all start with "Tutor backend". The *oldest* of them is the first
  # push; its parent is the app that worked.
  FIRST_DEPLOY="$(git log --format='%H%x09%s' | awk -F'\t' '$2 ~ /^Tutor backend/ {sha=$1} END {print sha}')"
  if [[ -z "$FIRST_DEPLOY" ]]; then
    echo "==> no 'Tutor backend' commit found; pass --to <sha> to choose a version" >&2
    exit 1
  fi
  TARGET="$(git rev-parse "${FIRST_DEPLOY}^")"
fi

git rev-parse --verify "$TARGET" >/dev/null 2>&1 \
  || { echo "error: $TARGET is not a commit in this Space" >&2; exit 1; }

echo
echo "==> restoring to $(git log --format='%h  %ad  %s' --date=short -n1 "$TARGET")"
echo "==> files that change:"
git diff --stat "$TARGET" HEAD | sed 's/^/    /'

if [[ "$CONFIRMED" != true ]]; then
  echo
  echo "This is a dry run. Re-run with --yes to force-push this version back."
  exit 0
fi

echo
echo "==> force-pushing the restored tree"
git checkout --detach "$TARGET" >/dev/null 2>&1
git push --force "https://user:${HF_TOKEN}@huggingface.co/spaces/${SPACE}" "HEAD:${BRANCH}"

cat <<EOF

==> done. The Space is rebuilding on its previous code.

    open  https://huggingface.co/spaces/${SPACE}     # build logs
    curl -i https://danuka21-qwen3-8-27b.hf.space/     # whatever it served before

If you want a clean copy of the old version kept in the app repo as a reference,
the same commit can be fetched with:

    git clone https://huggingface.co/spaces/${SPACE} && git show ${TARGET} --stat
EOF
