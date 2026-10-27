#!/usr/bin/env bash
# =============================================================================
# TheStudent — secret leak guard
# =============================================================================
#
# Scans the files you are about to commit for things that look like credentials.
# Run it manually before pushing, or install it as a pre-commit hook:
#
#   ln -sf ../../scripts/check-secrets.sh .git/hooks/pre-commit
#
# Usage:
#   ./scripts/check-secrets.sh            # scan staged files (pre-commit mode)
#   ./scripts/check-secrets.sh --all      # scan the whole working tree
#   ./scripts/check-secrets.sh --all CI   # scan everything (CI mode, same thing)
#
# Exit code 1 means: a secret was found, the commit was blocked.
# -----------------------------------------------------------------------------
set -u

RED=$'\033[31m'; YEL=$'\033[33m'; GRN=$'\033[32m'; RST=$'\033[0m'

MODE="staged"
[ "${1:-}" = "--all" ] && MODE="all"

# Files that legitimately contain placeholders / documentation.
EXCLUDE_RE='(^|/)(\.env\.example|config\.secrets\.example\.php|check-secrets\.sh)$|\.(md|png|jpg|jpeg|webp|keystore|jks|jar)$|^docs/'

if [ "$MODE" = "all" ]; then
    FILES=$(git ls-files)
else
    FILES=$(git diff --cached --name-only --diff-filter=ACM)
    if [ -z "$FILES" ]; then
        echo "${GRN}No staged files — nothing to check.${RST}"
        exit 0
    fi
fi

FILES=$(printf '%s\n' "$FILES" | grep -vE "$EXCLUDE_RE" || true)
[ -z "$FILES" ] && { echo "${GRN}No scannable files.${RST}"; exit 0; }

FOUND=0

scan() {
    local label="$1" pattern="$2"
    local hits
    # shellcheck disable=SC2086
    hits=$(printf '%s\n' "$FILES" | while IFS= read -r f; do
        [ -f "$f" ] || continue
        grep -nIE -- "$pattern" "$f" 2>/dev/null | sed "s|^|$f:|"
    done)

    if [ -n "$hits" ]; then
        echo "${RED}✗ $label${RST}"
        printf '%s\n' "$hits" | head -10
        echo
        FOUND=1
    fi
}

# --- High-confidence provider key formats ------------------------------------
scan "Google API key (AIza…)"          'AIza[0-9A-Za-z_-]{30,}'
scan "Hugging Face token (hf_…)"       'hf_[A-Za-z0-9]{30,}'
scan "OpenAI-style key (sk-…)"         '(^|[^A-Za-z0-9])sk-[A-Za-z0-9_-]{20,}'
scan "Groq key (gsk_…)"                'gsk_[A-Za-z0-9]{20,}'
scan "xAI key (xai-…)"                 'xai-[A-Za-z0-9]{20,}'
scan "AWS access key (AKIA…)"          'AKIA[0-9A-Z]{16}'
scan "JWT / Supabase key (eyJ…)"       'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}'

# --- Credentials assigned in code --------------------------------------------
scan "Password literal"                "(DB_PASS|SMTP_PASS|PASSWORD|passwd|password)[\"']?\s*[:=]\s*[\"'][^\"']{6,}[\"']"
scan "Secret/API key literal"          "(API_KEY|APIKEY|SECRET|ACCESS_TOKEN|AUTH_TOKEN|BEARER)[\"']?\s*[:=]\s*[\"'][^\"']{12,}[\"']"
scan "Private key block"               'BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY'

# --- Files that must never be committed --------------------------------------
BAD_FILES=$(printf '%s\n' "$FILES" | grep -E '(^|/)(\.env|config\.secrets\.php|google-services\.json|my-upload-key\.jks)$|\.(zip|jks|keystore)$' || true)
if [ -n "$BAD_FILES" ]; then
    echo "${RED}✗ Secret-bearing file staged for commit${RST}"
    printf '%s\n' "$BAD_FILES"
    echo
    FOUND=1
fi

if [ "$FOUND" -ne 0 ]; then
    echo "${YEL}Blocked: remove the secret, move it to .env (app) or backend/config.secrets.php (server),"
    echo "and revoke/rotate the value — it is already exposed if it was ever pushed.${RST}"
    exit 1
fi

echo "${GRN}✓ No obvious secrets found in $(printf '%s\n' "$FILES" | wc -l | tr -d ' ') file(s).${RST}"
exit 0
