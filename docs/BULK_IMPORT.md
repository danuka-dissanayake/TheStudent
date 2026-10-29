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
| `title` | `"<Subject> <Year> (<Medium> Medium)"`; language papers just `"<Language> <Year>"` |
| `stream` | from `SUBJECT_STREAM` in the script |
| `pages` | counted from the PDF objects |
| `file_size` | the real byte size, formatted like the panel does |
| `marking_scheme_url` | the scheme file with the same subject + year + medium |

Nothing is typed by hand, and no field is invented: a value the script cannot
work out is reported instead of guessed.

## 2. Plan first (no network, no upload)

```bash
python3 scripts/import_past_papers.py --scan ~/Branded_Past_Papers \
    --manifest papers.csv --issues issues.csv --report plan.txt
```

The plan prints counts per medium, per subject with the year span, the scheme
pairing, every subject it does not recognise, every file without a year, and
every file that would breach the 25 MB upload limit. `papers.csv` is the exact
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
have a session token. If the server emails a verification code because the
machine is on a new network, re-run with `--otp <code>`.

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
