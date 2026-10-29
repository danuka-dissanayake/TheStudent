# Bulk past-paper import

The admin panel uploads one paper at a time, which is fine for a handful and
hopeless for an archive. `scripts/import_past_papers.py` reads a folder tree,
works out every database field from the paths, and posts each paper through the
same `api/upload_paper.php` endpoint the panel uses — so rows are created by the
same validation path, with the same admin guard.

It needs nothing but Python 3.8+ and the standard library.

## 1. The archive layout it understands

```
Branded_Past_Papers/
└── A_L Insight Bot/
    ├── 1️⃣ A-L Past Papers_/
    │   ├── Sinhala Medium/<Subject>/<year>-<subject>-Sinhala_TheStudent.pdf
    │   ├── Tamil Medium/<Subject>/…
    │   ├── English Medium/<Subject>/…
    │   └── Languages/<Language>/<year>-<language>-0_TheStudent.pdf
    └── 2️⃣ A-L Marking Schemes/
        ├── Sinhala Medium/<Subject>/<year>-<subject>-Sinhala-Scheme_TheStudent.pdf
        └── Languages/<Language>/…
```

| Field | Where it comes from |
|---|---|
| `subject` | the subject folder, canonicalised (see the alias table below) |
| `year` | the leading year in the file name |
| `medium` | the medium folder — or, in `Languages`, the language itself |
| `grade` | always `Grade 13` (what the app's A/L bucketing checks) |
| `category` | always `A/L` |
| `exam_type` | always `G.C.E. Advanced Level` |
| `title` | `"<Year> <Subject> <Medium> Medium Past Paper"` — e.g. `2024 Science for Technology Sinhala Medium Past Paper`; a language paper is already named by its medium, so `2019 Japanese Past Paper` |
| `stream` | from `SUBJECT_STREAM` in the script |
| `pages` | counted from the PDF: page objects first, then the page tree's `/Count` (which catches linearised and partly compressed files) |
| `file_size` | the real byte size on disk, formatted like the panel does (`3.42 MB`); the endpoint also re-measures the stored file, so the row is never wrong |
| `marking_scheme_url` | the scheme file with the same subject + year + medium |

Nothing is typed by hand, and no field is invented: a value the script cannot
work out is reported instead of guessed.

## 2. Plan first (no network, no upload)

```bash
python3 scripts/import_past_papers.py --scan ~/Branded_Past_Papers \
    --manifest papers.csv --issues issues.csv --report plan.txt
```

The plan prints counts per medium, per subject with the year span, the scheme
pairing, how many page counts could be read from the PDFs, every subject it does
not recognise, every file without a year, and every file that would breach the
25 MB upload limit. Planning from `--tree` alone cannot see inside the files, so
it says the payload is unknown instead of printing zeros — use `--scan` for real
sizes and page counts. `papers.csv` is the exact
set of rows that would be created; `issues.csv` is the "look at this" list.

You can also plan from a `tree` listing alone, without the PDFs:

```bash
python3 scripts/import_past_papers.py --tree structure.txt --manifest papers.csv
```

## 3. Upload

```bash
python3 scripts/import_past_papers.py --scan ~/Branded_Past_Papers --upload \
    --email admin@gmail.com
```

It signs in through `api/auth.php?action=login`, checks the account really has
the `admin` role, then uploads one paper at a time with the scheme attached.
Use `--token` (or `TS_ADMIN_TOKEN`) instead of an email/password if you already
have a session token.

If the server emails a verification code because the machine is on a new
network, the script asks for it **in the same run**:

```
  Sign-in from a new location detected.
  Code sent to you@example.com (6 digits): 482915
```

Type the code that has just arrived and it carries on to the upload. (`echo 123456
| python3 scripts/import_past_papers.py …` works too, for unattended runs.) A code
from an earlier run cannot be used: every login attempt issues a fresh code and
marks the previous one used, so `--otp` only helps if the code is newer than the
sign-in attempt that triggers it.

The API sits behind Cloudflare, whose bot protection rejects a script's default
`Python-urllib/3.x` agent with `HTTP 403 · error code: 1010`. The script
therefore identifies itself as a normal browser (`--user-agent` overrides it).
A 1010 at sign-in means the request never reached PHP — it is not a wrong
password. If it happens anyway, allow the machine's IP under Cloudflare →
Security → WAF, or upload from the server itself.

Useful flags: `--limit N` (first N papers), `--status draft`, `--delay 1`,
`--retries 3`, `--languages-medium english`, `--no-pages`, `--base-url`.

### It is safe to re-run

* Before the first upload it reads `get_papers.php?status=all` and skips any
  paper whose **subject + year + medium** already exists.
* Every finished file is recorded in `import_state.jsonl`, so a Ctrl-C, a dead
  connection or a closed laptop costs nothing — re-run the same command.
* Failures go to `upload_failed.csv` and are retried twice by default.
* Two uploads of same-named files within one second are spaced out, because the
  endpoint names stored files `time() + original name`.

## 4. Subject aliases

The database matches subjects with `=`, so folder spellings are canonicalised
through `SUBJECT_CANON` in the script:

* `Bio-Resource Technology` → `Bio Resource Technology`
* `Electrical, Electronic and Information Technology` and
  `Electric , Electronic And Information Technology` →
  `Electric, Electronic and Information Technology`
* `Combined Maths` → `Combined Mathematics`, `Bio Systems Tech` → `Bio Systems Technology`,
  `ICT` → `ICT`, `GIT` → `GIT`, …

A subject the table has never seen is uploaded verbatim **and printed in the
plan**, so it can be added to the table before the real run.

## 5. Language papers and the medium filter

Papers in `Languages/` carry the language as their medium (`Japanese`,
`Arabic`, …) because that is the truth about the document; `Sinhala`, `Tamil`
and `English` map onto the three ordinary mediums. The app builds its medium
chips from the data, so these appear as extra chips. Pass
`--languages-medium english` (or `sinhala` / `tamil`) to file them under one of
the three ordinary mediums instead.

## 6. What it will not do

* Upload a PDF over 25 MB (`ts_validate_uploaded_pdf()` rejects it) — the plan
  lists those files instead of failing on them mid-run.
* Attach a scheme whose subject + year + medium has no paper in the archive.
  Those are reported as **orphaned schemes**; the usual cause is a scheme from a
  year whose paper is missing from the archive.
* Create thumbnails: paper covers are drawn procedurally in the app from the
  subject code.

## 7. Making the upload faster

A 5 GB archive at ~290 KB/s takes roughly four hours, so measure before
blaming the script:

```bash
# what the server is actually taking (a completed paper's rate)
# then: what your line can do at all
curl -o /dev/null -T "some-paper.pdf" https://speed.cloudflare.com/__up
```

If that second number is also ~2 Mbps, you are at the ceiling of your uplink and
nothing on either side can beat it. If it is much higher, the bottleneck is
per-connection, and parallel uploads help:

```bash
python3 scripts/import_past_papers.py --scan "<dir>" --upload \
    --email you@example.com --workers 5
```

`--workers N` sends N papers at once; 4–6 saturates a typical home uplink, and
beyond that the requests just queue on the shared host. Keep `--workers 1` if
you see 429s or 5xx from the host — that is it asking for less concurrency.

The bigger lever is **not sending 5 GB in the first place**:

```bash
./scripts/compress_papers.sh ~/Branded_Past_Papers ~/Branded_Compressed 150
python3 scripts/import_past_papers.py --scan ~/Branded_Compressed --manifest papers.csv
```

Ghostscript re-writes every PDF at 150 dpi (`/ebook`) with the tree and file
names intact, so papers and schemes still pair up. Scan-heavy archives typically
drop to 20–30 % of their size — a 5 GB set becomes ~1.2 GB, which is not just a
faster upload but a faster download for every student afterwards. It also brings
the six papers over the 25 MB limit back under it. Use `300` instead of `150`
for diagram-heavy subjects, or run `--scan` on the compressed tree first and
compare the page counts in the plan before uploading.

## 8. "Paper title is required." — a body limit, not a bug

If uploads fail with `Paper title is required.` after the first few papers,
nothing on the client is wrong: PHP discarded the request body, so `$_POST`
arrived empty and `upload_paper.php` never saw the `title` it was sent. That
happens when the multipart body is larger than `post_max_size` — usually 8 MB on
shared hosting, and a paper plus its marking scheme passes that easily. Retrying
cannot help, so the importer stops the run and prints the fix.

Check what the host allows (define `DIAG_TOKEN` in `config.secrets.php` first):

```
https://app.quizontal.site/api/health.php?token=YOUR_DIAG_TOKEN
```

`php.post_max_size` and `php.upload_max_filesize` are reported, and
`upload_warnings` names anything too small. On DirectAdmin, open **PHP Settings**
for the domain (or drop a `.user.ini` beside `api/`):

```ini
post_max_size = 64M
upload_max_filesize = 40M
memory_limit = 256M
max_execution_time = 120
```

Then re-run the same command — finished papers are in `import_state.jsonl`, so it
resumes exactly where it stopped.

If the limits cannot be changed, import in two passes:

```bash
python3 scripts/import_past_papers.py --scan "<dir>" --upload --email you@x \
    --scheme-mode auto --max-body-mb 8
```

`auto` attaches a scheme only when paper + scheme fit the budget; otherwise the
paper is uploaded alone and the scheme is written to `schemes_pending.csv` for
later. A single paper larger than `post_max_size` cannot be worked around at
all — compress it (`scripts/compress_papers.sh`) or raise the limit.

## 9. Response shapes

`api/auth.php` builds responses with `sendJsonResponse()`, which **merges** its
payload into the top level — `{"status": "success", "token": …, "user": {…}}` —
while `get_papers.php` wraps rows in a `data` key. The importer reads either
shape (`payload_field()`), so a change on one endpoint cannot silently produce a
"no session token" failure.
