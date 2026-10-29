#!/usr/bin/env bash
#
# Shrink a past-paper archive before importing it.
#
#   ./scripts/compress_papers.sh SRC_DIR OUT_DIR [dpi]
#
# Mirrors the whole tree into OUT_DIR with every PDF re-written by Ghostscript,
# keeping the same file names so scripts/import_past_papers.py --scan OUT_DIR
# pairs the papers and marking schemes exactly as before. A scan-heavy 5 GB
# archive typically lands around 1–1.5 GB at 150 dpi with no readable loss,
# which is the difference between a four-hour upload and a forty-minute one.
#
# /ebook (150 dpi) is the default because exam papers are text-on-white; use
# /printer (300 dpi) if a diagram-heavy subject looks soft, or pass a dpi.
set -euo pipefail

if [ "$#" -lt 2 ]; then
    sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
fi

SRC="${1%/}"
OUT="${2%/}"
DPI="${3:-150}"

if [ ! -d "$SRC" ]; then
    echo "No such directory: $SRC" >&2
    exit 1
fi
if ! command -v gs >/dev/null 2>&1; then
    cat >&2 <<'MSG'
Ghostscript (gs) is not installed.

  Debian / Ubuntu / WSL   sudo apt install ghostscript
  macOS                   brew install ghostscript
  Fedora                  sudo dnf install ghostscript
MSG
    exit 1
fi

case "$DPI" in
    300) SETTING=/printer ;;
    *)   SETTING=/ebook ;;
esac

mkdir -p "$OUT"

total=0
done=0
before=0
after=0

while IFS= read -r -d '' file; do
    total=$((total + 1))
done < <(find "$SRC" -type f -iname '*.pdf' -print0)

echo "Compressing $total PDF(s) at $DPI dpi ($SETTING) → $OUT"
echo

while IFS= read -r -d '' file; do
    rel="${file#"$SRC"/}"
    target="$OUT/$rel"
    mkdir -p "$(dirname "$target")"

    if [ -s "$target" ]; then
        done=$((done + 1))
        continue
    fi

    size_in=$(stat -c %s "$file" 2>/dev/null || stat -f %z "$file")
    gs -q -dNOPAUSE -dBATCH -dSAFER \
       -sDEVICE=pdfwrite \
       -dCompatibilityLevel=1.5 \
       -dPDFSETTINGS="$SETTING" \
       -dColorImageResolution="$DPI" \
       -dGrayImageResolution="$DPI" \
       -dMonoImageResolution="$DPI" \
       -dEmbedAllFonts=true \
       -dSubsetFonts=true \
       -sOutputFile="$target" \
       "$file" >/dev/null 2>&1 || {
           echo "  ! gs failed, copying as-is: $rel" >&2
           cp "$file" "$target"
       }

    size_out=$(stat -c %s "$target" 2>/dev/null || stat -f %z "$target")
    before=$((before + size_in))
    after=$((after + size_out))
    done=$((done + 1))

    if [ $((done % 25)) -eq 0 ] || [ "$done" -eq "$total" ]; then
        pct=$((done * 100 / total))
        mb_in=$((before / 1048576))
        mb_out=$((after / 1048576))
        printf '  %4d/%d  %3d%%  %d MB → %d MB\n' "$done" "$total" "$pct" "$mb_in" "$mb_out"
    fi
done < <(find "$SRC" -type f -iname '*.pdf' -print0)

echo
printf 'Done. %d MB → %d MB (%d%% of the original)\n' \
    $((before / 1048576)) $((after / 1048576)) \
    $(( before > 0 ? after * 100 / before : 100 ))
echo
echo "Now import from the compressed tree:"
echo "  python3 scripts/import_past_papers.py --scan \"$OUT\" --manifest papers.csv"
