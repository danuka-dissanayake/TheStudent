#!/usr/bin/env python3
"""
TheStudent — bulk past-paper importer
=====================================

Uploads the branded A/L archive (papers + marking schemes) to the PHP backend
through the same `api/upload_paper.php` endpoint the in-app admin panel uses,
so every row is created by the same validation path.

The archive is expected to look like the branded tree:

    Branded_Past_Papers/
      A_L Insight Bot/
        1 <A-L Past Papers>/<Sinhala|Tamil|English> Medium/<Subject>/<year>-...pdf
        1 <A-L Past Papers>/Languages/<Language>/<year>-...pdf
        2 <A-L Marking Schemes>/<Sinhala|Tamil|English> Medium/<Subject>/<year>-...-Scheme.pdf
        2 <A-L Marking Schemes>/Languages/<Language>/<year>-...-Scheme.pdf

Everything except the year comes from the folders, so nothing has to be typed.

Usage
-----
    # 1. Plan only — no network, writes a manifest + a report you can read:
    python3 scripts/import_past_papers.py --scan ~/Branded_Past_Papers \
        --manifest papers.csv --report report.txt

    # 1b. Plan from a directory-listing text file (no PDFs needed):
    python3 scripts/import_past_papers.py --tree structure.txt --manifest papers.csv

    # 2. Upload for real (asks for the admin password, or takes --token):
    python3 scripts/import_past_papers.py --scan ~/Branded_Past_Papers \
        --upload --email admin@gmail.com --status published

Re-running is safe: a paper whose subject+year+medium is already on the server
is skipped, and files finished in an earlier run are remembered in --state.

Only the Python standard library is used, so it runs on a bare machine.
"""

from __future__ import annotations

import argparse
import csv
import getpass
import json
import os
import re
import ssl
import sys
import threading
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from pathlib import Path

# ── Backend contract ─────────────────────────────────────────────────────────
# Must stay in lockstep with api/upload_paper.php and api/_admin_guard.php.
DEFAULT_BASE_URL = "https://app.quizontal.site/"
UPLOAD_PATH = "api/upload_paper.php"
PAPERS_PATH = "api/get_papers.php"
AUTH_PATH = "api/auth.php"
MAX_PDF_BYTES = 26_214_400          # ts_validate_uploaded_pdf() hard limit (25 MB)

# The live host sits behind Cloudflare, whose bot protection answers urllib's
# default "Python-urllib/3.x" agent with HTTP 403 and a non-JSON body
# ("error code: 1010"). Presenting an ordinary browser identity is what makes
# the API reachable from a script; no other Cloudflare handling is needed.
USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)

GRADE = "Grade 13"
CATEGORY = "A/L"
EXAM_TYPE = "G.C.E. Advanced Level"

# ── Subject vocabulary ───────────────────────────────────────────────────────
# The archive's folder names are free text; the database matches subjects with
# `=`, so a stray spelling makes a paper invisible behind the subject filters.
# Keyed by normalised folder name → the name stored in `past_papers.subject`.
SUBJECT_CANON = {
    # sciences
    "combined mathematics": "Combined Mathematics",
    "combined maths": "Combined Mathematics",
    "physics": "Physics",
    "chemistry": "Chemistry",
    "biology": "Biology",
    "higher mathematics": "Higher Mathematics",
    "mathematics": "Mathematics",
    "agricultural science": "Agricultural Science",
    "agriculture": "Agricultural Science",
    # technology stream
    "engineering technology": "Engineering Technology",
    "engineering tech": "Engineering Technology",
    "science for technology": "Science for Technology",
    "bio systems technology": "Bio Systems Technology",
    "bio system technology": "Bio Systems Technology",
    "bio systems tech": "Bio Systems Technology",
    "agro technology": "Agro Technology",
    "bio resource technology": "Bio Resource Technology",   # also "Bio-Resource"
    "git": "GIT",
    "general information technology": "GIT",
    "ict": "ICT",
    "information communication technology": "ICT",
    "electric electronic and information technology": "Electric, Electronic and Information Technology",
    "electrical electronic and information technology": "Electric, Electronic and Information Technology",
    "electric and electronic technology": "Electric, Electronic and Information Technology",
    # commerce
    "accounting": "Accounting",
    "business studies": "Business Studies",
    "business statistics": "Business Statistics",
    "economics": "Economics",
    # arts
    "art": "Art",
    "drama and theatre": "Drama and Theatre",
    "drama theatre": "Drama and Theatre",
    "communication and media studies": "Communication & Media Studies",
    "communication media studies": "Communication & Media Studies",
    "political science": "Political Science",
    "geography": "Geography",
    "history": "History",
    "home economics": "Home Economics",
    # religions and civilisations
    "buddhism": "Buddhism",
    "buddhist civilization": "Buddhist Civilization",
    "hinduism": "Hinduism",
    "hindu civilization": "Hindu Civilization",
    "islam": "Islam",
    "islam civilization": "Islam Civilization",
    "christianity": "Christianity",
    "christian civilization": "Christian Civilization",
    # common
    "common general test": "Common General Test",
    "general english": "General English",
    # languages
    "sinhala": "Sinhala",
    "tamil": "Tamil",
    "english": "English",
    "arabic": "Arabic",
    "chinese": "Chinese",
    "french": "French",
    "german": "German",
    "japanese": "Japanese",
    "korean": "Korean",
    "russian": "Russian",
}

# A/L stream each subject is filed under in the app's Papers filters. A paper
# carries one stream, so subjects shared by two streams are filed under the one
# most of their students sit (Chemistry → Physical Science, and so on).
SUBJECT_STREAM = {
    "Combined Mathematics": "Physical Science",
    "Higher Mathematics": "Physical Science",
    "Mathematics": "Physical Science",
    "Physics": "Physical Science",
    "Chemistry": "Physical Science",
    "Biology": "Biological Science",
    "Agricultural Science": "Biological Science",
    "Engineering Technology": "Technology",
    "Science for Technology": "Technology",
    "Bio Systems Technology": "Technology",
    "Agro Technology": "Technology",
    "Bio Resource Technology": "Technology",
    "Electric, Electronic and Information Technology": "Technology",
    "ICT": "Technology",
    "GIT": "Technology",
    "Accounting": "Commerce",
    "Business Studies": "Commerce",
    "Business Statistics": "Commerce",
    "Economics": "Commerce",
    "Common General Test": "Common",
    "General English": "Common",
    # arts
    "Art": "Arts",
    "Drama and Theatre": "Arts",
    "Communication & Media Studies": "Arts",
    "Political Science": "Arts",
    "Geography": "Arts",
    "History": "Arts",
    "Home Economics": "Arts",
    "Buddhism": "Arts",
    "Buddhist Civilization": "Arts",
    "Hinduism": "Arts",
    "Hindu Civilization": "Arts",
    "Islam": "Arts",
    "Islam Civilization": "Arts",
    "Christianity": "Arts",
    "Christian Civilization": "Arts",
    # the language subjects are taken by arts students
    "Arabic": "Arts",
    "Chinese": "Arts",
    "English": "Arts",
    "French": "Arts",
    "German": "Arts",
    "Japanese": "Arts",
    "Korean": "Arts",
    "Russian": "Arts",
    "Sinhala": "Arts",
    "Tamil": "Arts",
}
# Only a subject this table has never heard of falls back to this stream, and
# the plan prints it so it can be corrected before uploading.
DEFAULT_STREAM = "Arts"

LANGUAGE_SUBJECTS = {
    "Arabic", "Chinese", "English", "French", "German",
    "Japanese", "Korean", "Russian", "Sinhala", "Tamil",
}

MEDIUM_FOLDERS = {
    "sinhala medium": "Sinhala",
    "tamil medium": "Tamil",
    "english medium": "English",
}

SCHOOL = re.compile(r"(marking|scheme)", re.IGNORECASE)
YEAR_RE = re.compile(r"(?<!\d)(19[5-9]\d|20[0-4]\d)(?!\d)")

# ── helpers ──────────────────────────────────────────────────────────────────


def norm(text: str) -> str:
    """Lowercase, strip punctuation, collapse whitespace — the matching key."""
    text = unicodedata.normalize("NFKD", text)
    text = text.replace("–", "-").replace("—", "-").replace("&", " and ")
    text = re.sub(r"[^A-Za-z0-9]+", " ", text)
    return re.sub(r"\s+", " ", text).strip().lower()


def canon_subject(folder: str) -> tuple[str, bool]:
    """Folder name → (subject as stored in the DB, was it recognised)."""
    key = norm(folder)
    if key in SUBJECT_CANON:
        return SUBJECT_CANON[key], True
    return folder.strip(), False


def parse_year(filename: str) -> int | None:
    stem = filename.rsplit(".", 1)[0]
    years = [int(y) for y in YEAR_RE.findall(stem)]
    return years[0] if years else None


def human_size(num_bytes: int) -> str:
    if num_bytes >= 1024 * 1024:
        return f"{num_bytes / 1048576:.2f} MB"
    if num_bytes >= 1024:
        return f"{num_bytes / 1024:.1f} KB"
    return f"{num_bytes} B"


def pdf_page_count(path: Path) -> int:
    """
    Count the pages in a PDF without a dependency.

    Two passes, because one alone is not enough: count the `/Type /Page`
    objects, then read `/Count` from the page-tree dictionaries (which is what
    a linearised or partially compressed file relies on). The larger of the two
    wins — a page object is only ever under-counted by the first pass.
    """
    try:
        blob = path.read_bytes()
    except OSError:
        return 0

    direct = len(re.findall(rb"/Type\s*/Page[^s]", blob))

    tree_count = 0
    for match in re.finditer(rb"/Type\s*/Pages\b", blob):
        window = blob[max(0, match.start() - 400): match.end() + 400]
        for number in re.findall(rb"/Count\s+(\d+)", window):
            tree_count = max(tree_count, int(number))

    # An outline can carry a /Count too, so the tree figure is only trusted when
    # it is in the same order of magnitude as the real page objects.
    if tree_count and (direct == 0 or tree_count <= direct * 4):
        return max(direct, tree_count)
    return direct


@dataclass
class Item:
    """One paper row, optionally carrying the scheme that belongs to it."""
    source: Path | None
    rel_path: str
    subject: str
    subject_known: bool
    medium: str
    year: int | None
    stream: str
    pages: int = 0
    size_bytes: int = 0
    scheme: "Item | None" = None
    notes: list[str] = field(default_factory=list)

    @property
    def key(self) -> tuple[str, int | None, str]:
        """Identity used both to de-dupe the batch and to skip live papers."""
        return (norm(self.subject), self.year, norm(self.medium))

    @property
    def title(self) -> str:
        """
        Year first, then subject, then medium, then "Past Paper" —
        e.g. "2024 Science for Technology Sinhala Medium Past Paper".
        A language paper is already named by its medium ("2019 Japanese
        Past Paper"), so it is not repeated.
        """
        if self.medium == self.subject:
            return f"{self.year} {self.subject} Past Paper"
        return f"{self.year} {self.subject} {self.medium} Medium Past Paper"

    @property
    def description(self) -> str:
        text = (f"Official G.C.E. Advanced Level {self.subject} paper, {self.year}, "
                f"{self.medium} medium.")
        if self.scheme is not None:
            text += " Marking scheme included."
        return text

    def label(self) -> str:
        """What the progress line shows — the row title, so the log matches."""
        return self.title


# ── discovery ────────────────────────────────────────────────────────────────


def walk_directory(root: Path) -> list[tuple[str, Path]]:
    """Every PDF under root, as (relative path, absolute path)."""
    found: list[tuple[str, Path]] = []
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() == ".pdf":
            found.append((str(path.relative_to(root)), path))
    return found


BOX = re.compile(r"^(?:[│ ]*)(?:├── |└── )(.*)$")


def walk_tree_dump(tree_file: Path) -> list[tuple[str, None]]:
    """
    Read a `tree`-style listing (the archive's structure.txt), so the whole plan
    can be reviewed before any PDF is touched. Indentation is derived from the
    box-drawing prefix rather than the text, since the dump pads with NBSPs.
    """
    entries: list[tuple[str, None]] = []
    stack: list[str] = []
    started = False
    for raw in tree_file.read_text(encoding="utf-8").splitlines():
        line = raw.replace("\u00a0", " ")
        if not started:
            started = "DIRECTORY TREE" in line.upper()
            continue
        if not line.strip() or set(line.strip()) <= {"-", "="}:
            continue
        match = BOX.match(line)
        if not match:
            continue
        depth = _depth_of(line)
        name = match.group(1).strip()
        del stack[depth:]
        stack.append(name)
        if name.lower().endswith(".pdf"):
            entries.append(("/".join(stack), None))
    return entries


def _depth_of(line: str) -> int:
    """Container columns before the branch glyph: 4 characters per level."""
    index = line.find("├── ")
    if index < 0:
        index = line.find("└── ")
    return max(index, 0) // 4


def classify(rel_path: str) -> dict:
    """Pull kind, medium folder, subject folder and filename out of a path."""
    parts = [p for p in rel_path.split("/") if p and p != "."]
    filename = parts[-1]
    folders = parts[:-1]
    is_scheme = any(SCHOOL.search(f) for f in folders) or bool(
        re.search(r"(?i)(marking|scheme)", filename)
    )
    medium_folder = "Languages"
    for folder in folders:
        if norm(folder) in MEDIUM_FOLDERS:
            medium_folder = MEDIUM_FOLDERS[norm(folder)]
            break
    subject_folder = folders[-1] if folders else ""
    return {
        "filename": filename,
        "is_scheme": is_scheme,
        "medium_folder": medium_folder,
        "subject_folder": subject_folder,
    }


def build_items(
    entries: list[tuple[str, Path | None]],
    languages_medium: str = "language",
    read_pages: bool = True,
) -> tuple[list[Item], list[Item], list[dict]]:
    """Turn file entries into paired papers + orphan schemes + issue rows."""
    papers: list[Item] = []
    schemes: list[Item] = []
    issues: list[dict] = []

    for rel_path, source in entries:
        info = classify(rel_path)
        filename = info["filename"]
        subject, known = canon_subject(info["subject_folder"])
        year = parse_year(filename)

        if info["medium_folder"] == "Languages":
            if languages_medium == "language":
                medium = subject if subject in LANGUAGE_SUBJECTS else "English"
            else:
                medium = languages_medium.capitalize()
        else:
            medium = info["medium_folder"]

        item = Item(
            source=source,
            rel_path=rel_path,
            subject=subject,
            subject_known=known,
            medium=medium,
            year=year,
            stream=SUBJECT_STREAM.get(subject, DEFAULT_STREAM),
            pages=0,
            size_bytes=source.stat().st_size if source else 0,
        )
        if not known:
            item.notes.append(f"unknown subject folder '{info['subject_folder']}'")
        if year is None:
            item.notes.append("no year in the filename")
        elif year > time.localtime().tm_year + 1:
            item.notes.append(f"implausible year {year} — check the file name")
        if read_pages and source is not None:
            item.pages = pdf_page_count(source)
        if item.size_bytes > MAX_PDF_BYTES:
            item.notes.append(
                f"over the {MAX_PDF_BYTES // 1048576} MB upload limit "
                f"({human_size(item.size_bytes)})"
            )
        (schemes if info["is_scheme"] else papers).append(item)

    # Pair each scheme with its paper on subject + year + medium.
    scheme_by_key: dict[tuple, Item] = {}
    duplicate_schemes: list[Item] = []
    for scheme in schemes:
        if scheme.key in scheme_by_key:
            duplicate_schemes.append(scheme)
            issues.append({
                "kind": "duplicate-scheme",
                "path": scheme.rel_path,
                "detail": f"second scheme for {scheme.label()}",
            })
            continue
        scheme_by_key[scheme.key] = scheme

    seen: dict[tuple, Item] = {}
    orphan_schemes: list[Item] = []
    for paper in papers:
        if paper.key in seen:
            issues.append({
                "kind": "duplicate-paper",
                "path": paper.rel_path,
                "detail": f"same subject/year/medium as {seen[paper.key].rel_path}",
            })
            continue
        seen[paper.key] = paper
        scheme = scheme_by_key.pop(paper.key, None)
        if scheme is not None:
            paper.scheme = scheme
        for note in paper.notes:
            issues.append({"kind": "paper", "path": paper.rel_path, "detail": note})

    # Whatever is left in scheme_by_key has no paper to hang on.
    for key, scheme in sorted(scheme_by_key.items(), key=lambda kv: kv[1].rel_path):
        orphan_schemes.append(scheme)
        issues.append({
            "kind": "orphan-scheme",
            "path": scheme.rel_path,
            "detail": f"no {scheme.subject} {scheme.year} ({scheme.medium}) paper in this set",
        })

    return papers, orphan_schemes, issues


# ── reporting ────────────────────────────────────────────────────────────────


def print_plan(papers: list[Item], orphans: list[Item], issues: list[dict]) -> None:
    paired = [p for p in papers if p.scheme is not None]
    total_bytes = sum(p.size_bytes for p in papers) + sum(
        p.scheme.size_bytes for p in papers if p.scheme
    )
    print()
    print("=" * 74)
    print("  TheStudent · past-paper import plan")
    print("=" * 74)
    print(f"  papers            {len(papers)}")
    print(f"  marking schemes   {len(paired)} paired · {len(orphans)} orphaned")
    if any(p.size_bytes for p in papers):
        print(f"  payload           {human_size(total_bytes)}")
        no_pages = [p for p in papers if p.pages == 0]
        counted = len(papers) - len(no_pages)
        print(f"  page counts       {counted}/{len(papers)} read from the PDFs")
        if no_pages:
            print(f"  ⚠ {len(no_pages)} file(s) whose page count could not be "
                  f"read — they upload without one:")
            for paper in no_pages[:6]:
                print(f"    {paper.rel_path}")
            if len(no_pages) > 6:
                print(f"    … and {len(no_pages) - 6} more")
    else:
        print("  payload           unknown — planning from a listing; run with "
              "--scan for real sizes and page counts")
    print(f"  upload limit      {MAX_PDF_BYTES // 1048576} MB per file")

    by_medium: dict[str, int] = {}
    for paper in papers:
        by_medium[paper.medium] = by_medium.get(paper.medium, 0) + 1
    print("\n  by medium")
    for medium, count in sorted(by_medium.items(), key=lambda kv: -kv[1]):
        print(f"    {medium:<12} {count:>4}")

    subjects = sorted({p.subject for p in papers})
    print(f"\n  by subject ({len(subjects)})")
    for subject in subjects:
        rows = [p for p in papers if p.subject == subject]
        years = sorted({p.year for p in rows if p.year})
        span = f"{years[0]}–{years[-1]}" if years else "?"
        print(f"    {subject:<52} {len(rows):>3}  ({span})")

    unknown = sorted({p.subject for p in papers if not p.subject_known})
    if unknown:
        print("\n  ⚠ subject folders the map does not know (uploaded verbatim):")
        for subject in unknown:
            print(f"    {subject}")

    unmapped = sorted({p.subject for p in papers if p.subject not in SUBJECT_STREAM})
    if unmapped:
        print(f"\n  note: {len(unmapped)} subject(s) fall back to the "
              f"'{DEFAULT_STREAM}' stream")

    no_year = [p for p in papers if p.year is None]
    if no_year:
        print(f"\n  ⚠ {len(no_year)} file(s) with no year — they are uploaded with "
              f"this year, please check:")
        for paper in no_year[:10]:
            print(f"    {paper.rel_path}")

    tiny = [
        p for p in papers
        if p.size_bytes and (p.size_bytes < 900_000 or (0 < p.pages < 6))
    ]
    if tiny:
        print(f"\n  ⚠ {len(tiny)} paper(s) look unusually small for an exam paper "
              f"(under 900 KB or under 6 pages) — check they downloaded fully:")
        for paper in tiny[:10]:
            print(f"    {human_size(paper.size_bytes):>9} {paper.pages:>3}p  {paper.rel_path}")

    oversize = [p for p in papers if p.size_bytes > MAX_PDF_BYTES]
    if oversize:
        print(f"\n  ⚠ {len(oversize)} file(s) over the upload limit — skipped:")
        for paper in oversize[:10]:
            print(f"    {paper.rel_path}  {human_size(paper.size_bytes)}")

    if orphans:
        print(f"\n  orphaned schemes (kept out of the upload, listed for you):")
        for scheme in orphans[:15]:
            print(f"    {scheme.rel_path}")
        if len(orphans) > 15:
            print(f"    … and {len(orphans) - 15} more (see the issues CSV)")

    missing_schemes = len([p for p in papers if p.scheme is None])
    print(f"\n  papers without a scheme: {missing_schemes}")
    print(f"  issues logged: {len(issues)}")
    print("=" * 74)
    print()


def write_manifest(path: Path, papers: list[Item], orphans: list[Item]) -> None:
    fields = [
        "kind", "subject", "year", "medium", "stream", "grade", "category",
        "exam_type", "title", "pages", "file_size", "status", "source_path",
        "scheme_path", "notes",
    ]
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for paper in papers:
            writer.writerow({
                "kind": "paper",
                "subject": paper.subject,
                "year": paper.year or "",
                "medium": paper.medium,
                "stream": paper.stream,
                "grade": GRADE,
                "category": CATEGORY,
                "exam_type": EXAM_TYPE,
                "title": paper.title,
                "pages": paper.pages or "",
                "file_size": human_size(paper.size_bytes) if paper.size_bytes else "",
                "status": "published",
                "source_path": paper.rel_path,
                "scheme_path": paper.scheme.rel_path if paper.scheme else "",
                "notes": "; ".join(paper.notes),
            })
        for scheme in orphans:
            writer.writerow({
                "kind": "orphan-scheme",
                "subject": scheme.subject,
                "year": scheme.year or "",
                "medium": scheme.medium,
                "stream": scheme.stream,
                "source_path": scheme.rel_path,
                "notes": "; ".join(scheme.notes),
            })
    print(f"  manifest → {path}")


def write_issues(path: Path, issues: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["kind", "path", "detail"])
        writer.writeheader()
        writer.writerows(issues)
    print(f"  issues   → {path}")



# ── direct-to-server route (no HTTP upload) ──────────────────────────────────


def slug(text: str) -> str:
    """A URL-safe spelling of a subject: "Communication & Media Studies" →
    "communication-and-media-studies"."""
    text = text.replace("&", " and ")
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    text = re.sub(r"[^A-Za-z0-9]+", "-", text).strip("-").lower()
    return re.sub(r"-{2,}", "-", text)


def served_path(paper: Item, kind: str) -> str:
    """
    Where the file must live under the web root, relative to `--upload-prefix`.

    Names are built from the parsed fields rather than copied from the archive,
    so they are plain ASCII, unique per subject/year/medium, and readable in a
    URL: papers/physics/2024-physics-sinhala-paper.pdf
    """
    folder = slug(paper.subject) or "subject"
    medium = slug(paper.medium)
    stem = f"{paper.year}-{folder}-{medium}-{kind}.pdf"
    return f"{folder}/{stem}"


def stage_files(papers: list[Item], root: Path, stage_dir: Path) -> list[dict]:
    """
    Copy every paper and scheme into `stage_dir` under its served name.

    The staged tree is exactly what has to be uploaded: hand its contents to
    FTP or DirectAdmin's File Manager and the URLs in the SQL will resolve.
    """
    import shutil

    mapping: list[dict] = []
    for index, paper in enumerate(papers, start=1):
        paper_rel = served_path(paper, "paper")
        target = stage_dir / paper_rel
        target.parent.mkdir(parents=True, exist_ok=True)
        if root is not None:
            shutil.copy2(root / paper.rel_path, target)
        mapping.append({
            "kind": "paper",
            "title": paper.title,
            "source": paper.rel_path,
            "served": paper_rel,
        })
        if paper.scheme is not None:
            scheme_rel = served_path(paper, "scheme")
            scheme_target = stage_dir / scheme_rel
            scheme_target.parent.mkdir(parents=True, exist_ok=True)
            if root is not None:
                shutil.copy2(root / paper.scheme.rel_path, scheme_target)
            mapping.append({
                "kind": "scheme",
                "title": paper.title,
                "source": paper.scheme.rel_path,
                "served": scheme_rel,
            })
        if index % 100 == 0:
            print(f"  staged {index}/{len(papers)}")
    return mapping


def sql_quote(value) -> str:
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def write_sql(
    path: Path,
    papers: list[Item],
    base_url: str,
    upload_prefix: str,
) -> None:
    """
    INSERT statements for phpMyAdmin, one per paper.

    Each is guarded with NOT EXISTS on subject + year + medium, so importing the
    file twice adds nothing — the same rule the HTTP importer uses to skip papers
    that are already on the server.
    """
    base = base_url.rstrip("/")
    prefix = upload_prefix.strip("/")
    header = (
        "-- TheStudent · direct import of the past-paper archive\n"
        f"-- {len(papers)} papers. Upload the files to {prefix}/ first, then import\n"
        "-- this file in phpMyAdmin. Safe to run twice: every row is guarded by\n"
        "-- subject + year + medium.\n"
        "SET NAMES utf8mb4;"
    )
    parts: list[str] = [header]

    for paper in papers:
        pdf_url = f"{base}/{prefix}/{served_path(paper, 'paper')}"
        scheme_url = (
            f"{base}/{prefix}/{served_path(paper, 'scheme')}"
            if paper.scheme is not None
            else None
        )
        size = human_size(paper.size_bytes) if paper.size_bytes else ""
        parts.append(
            "INSERT INTO `past_papers`\n"
            "  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,\n"
            "   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,\n"
            "   `file_size`, `pages`, `status`)\n"
            "SELECT "
            f"{sql_quote(GRADE)}, {sql_quote(CATEGORY)}, {sql_quote(paper.subject)},\n"
            f"       {sql_quote(paper.stream)}, {int(paper.year or 0)}, "
            f"{sql_quote(EXAM_TYPE)}, {sql_quote(paper.medium)},\n"
            f"       {sql_quote(paper.title)}, {sql_quote(pdf_url)},\n"
            f"       {sql_quote(scheme_url)}, {sql_quote(paper.description)},\n"
            f"       {sql_quote(size)}, {int(paper.pages or 0)}, 'published'\n"
            "WHERE NOT EXISTS (\n"
            "  SELECT 1 FROM `past_papers`\n"
            f"  WHERE `subject` = {sql_quote(paper.subject)} "
            f"AND `year` = {int(paper.year or 0)}\n"
            f"    AND `medium` = {sql_quote(paper.medium)}\n"
            ");"
        )

    if any(not paper.size_bytes for paper in papers):
        print("  note: some sizes/pages are unknown (planned from a listing) — "
              "run with --scan for exact values")
    path.write_text("\n\n".join(parts) + "\n", encoding="utf-8")
    print(f"  sql      → {path}")


def write_copy_map(path: Path, mapping: list[dict], upload_prefix: str) -> None:
    prefix = upload_prefix.strip("/")
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle, fieldnames=["kind", "title", "source", "served", "url_path"]
        )
        writer.writeheader()
        for row in mapping:
            writer.writerow({**row, "url_path": f"{prefix}/{row['served']}"})
    print(f"  map      → {path}")


# ── backend ──────────────────────────────────────────────────────────────────


class BodyTooLarge(RuntimeError):
    """
    PHP discarded the request body, so `$_POST` arrived empty.

    When a multipart body exceeds `post_max_size`, PHP drops the whole form and
    `upload_paper.php` sees no `title` — it answers "Paper title is required."
    even though a title was sent. Retrying can never help.
    """


class OtpRequired(RuntimeError):
    """
    Sign-in was accepted but the server wants a mailed code for this device.

    The code cannot be supplied from an earlier run: every login attempt issues
    a fresh code and marks the previous one used, so the prompt has to happen
    right after this login response.
    """

    def __init__(self, email: str, reason: str | None = None):
        self.email = email
        self.reason = reason
        super().__init__("Verification code required.")


class Backend:
    def __init__(
        self,
        base_url: str,
        token: str | None = None,
        insecure: bool = False,
        user_agent: str = USER_AGENT,
    ):
        self.base_url = base_url.rstrip("/") + "/"
        self.token = token
        self.headers = {
            "User-Agent": user_agent,
            "Accept": "application/json, text/plain, */*",
            "Accept-Language": "en-US,en;q=0.9",
            "X-Requested-With": "XMLHttpRequest",
            "Connection": "close",
        }
        self.context = ssl.create_default_context()
        if insecure:
            self.context.check_hostname = False
            self.context.verify_mode = ssl.CERT_NONE

    # -- plumbing ---------------------------------------------------------
    def _request(self, url: str, *, data: bytes | None = None,
                 headers: dict | None = None, method: str | None = None,
                 timeout: int = 300) -> tuple[int, bytes]:
        merged = dict(self.headers)
        merged.update(headers or {})
        request = urllib.request.Request(url, data=data, headers=merged, method=method)
        try:
            with urllib.request.urlopen(request, context=self.context, timeout=timeout) as reply:
                return reply.status, reply.read()
        except urllib.error.HTTPError as error:
            return error.code, error.read() or b""
        except urllib.error.URLError as error:
            raise ConnectionError(f"{url}: {error.reason}") from error

    def _json(self, url: str, payload: dict | None = None,
              headers: dict | None = None) -> dict:
        body = None
        hdrs = dict(headers or {})
        if payload is not None:
            body = json.dumps(payload).encode("utf-8")
            hdrs["Content-Type"] = "application/json"
        status, raw = self._request(url, data=body, headers=hdrs)
        try:
            return json.loads(raw.decode("utf-8", "replace") or "{}")
        except json.JSONDecodeError:
            raise RuntimeError(_describe_non_json(url, status, raw)) from None

    def _auth_headers(self) -> dict:
        if not self.token:
            raise RuntimeError("No admin token — sign in first.")
        return {"Authorization": f"Bearer {self.token}"}

    # -- auth -------------------------------------------------------------
    def login(self, email: str, password: str, otp: str | None = None) -> dict:
        url = self.base_url + AUTH_PATH + "?action=login"
        reply = self._json(url, {"email": email, "password": password})
        if reply.get("status") == "requires_verification":
            if not otp:
                raise OtpRequired(email, reply.get("message"))
            self.verify_otp(email, otp)
            return reply
        if reply.get("status") != "success":
            raise RuntimeError(reply.get("message") or "Sign-in failed.")
        self.token = payload_field(reply, "token")
        if not self.token:
            raise RuntimeError("Sign-in did not return a session token.")
        return reply

    def verify_otp(self, email: str, code: str) -> dict:
        reply = self._json(
            self.base_url + AUTH_PATH + "?action=verify_otp",
            {"email": email, "otp_code": code},
        )
        if reply.get("status") != "success":
            raise RuntimeError(reply.get("message") or "Verification failed.")
        self.token = payload_field(reply, "token")
        if not self.token:
            raise RuntimeError("Verification did not return a session token.")
        return reply

    def whoami(self) -> dict:
        reply = self._json(
            self.base_url + AUTH_PATH + "?action=me", headers=self._auth_headers()
        )
        if reply.get("status") != "success":
            raise RuntimeError(reply.get("message") or "The session was refused.")
        user = payload_field(reply, "user") or {}
        if (user.get("role") or "") != "admin":
            raise RuntimeError(
                f"This account is not an administrator (role: {user.get('role') or 'user'})."
            )
        return user

    # -- data -------------------------------------------------------------
    def existing_keys(self) -> set[tuple]:
        """subject|year|medium of every paper already on the server."""
        url = self.base_url + PAPERS_PATH + "?status=all"
        status, raw = self._request(url, headers=self._auth_headers())
        try:
            reply = json.loads(raw.decode("utf-8", "replace") or "{}")
        except json.JSONDecodeError:
            raise RuntimeError(_describe_non_json(url, status, raw)) from None
        keys: set[tuple] = set()
        for row in reply.get("data") or []:
            try:
                year = int(row.get("year"))
            except (TypeError, ValueError):
                year = None
            keys.add((
                norm(str(row.get("subject") or "")),
                year,
                norm(str(row.get("medium") or "")),
            ))
        return keys

    def upload(
        self,
        paper: Item,
        root: Path,
        status: str,
        with_scheme: bool = True,
    ) -> dict:
        fields = {
            "title": paper.title,
            "grade": GRADE,
            "category": CATEGORY,
            "subject": paper.subject,
            "stream": paper.stream,
            "year": str(paper.year),
            "exam_type": EXAM_TYPE,
            "medium": paper.medium,
            "description": paper.description,
            "file_size": human_size(paper.size_bytes),
            "status": status,
        }
        # The endpoint also re-measures the stored file, so file_size is right
        # either way; pages are ours to count, so send them whenever known.
        if paper.pages > 0:
            fields["pages"] = str(paper.pages)

        files = {"pdf_file": root / paper.rel_path}
        if with_scheme and paper.scheme is not None:
            files["scheme_file"] = root / paper.scheme.rel_path

        body, content_type = _multipart(fields, files)
        headers = self._auth_headers()
        headers["Content-Type"] = content_type
        status_code, raw = self._request(
            self.base_url + UPLOAD_PATH, data=body, headers=headers
        )
        try:
            reply = json.loads(raw.decode("utf-8", "replace") or "{}")
        except json.JSONDecodeError:
            raise RuntimeError(
                _describe_non_json(self.base_url + UPLOAD_PATH, status_code, raw)
            ) from None
        if status_code == 413:
            raise RuntimeError(
                "The server refused the request as too large (HTTP 413) — check "
                "PHP's post_max_size / upload_max_filesize."
            )
        message = str(reply.get("message") or "")
        if reply.get("status") != "success":
            # An empty $_POST is the signature of a dropped body, not of a
            # missing title — the title was in the request we just wrote.
            if fields.get("title") and "title is required" in message.lower():
                raise BodyTooLarge(
                    f"the server dropped the {human_size(len(body))} body "
                    f"(post_max_size) — it saw no fields at all"
                )
            raise RuntimeError(message or f"HTTP {status_code}")
        return reply


def payload_field(reply: dict, key: str, default=None):
    """
    Read a field from a PHP response.

    `sendJsonResponse()` merges its payload into the top level, so auth.php
    answers `{"status": …, "token": …}` — there is no `data` envelope. Older
    handlers (and get_papers.php) do use one, so accept both shapes.
    """
    if key in reply:
        return reply[key]
    data = reply.get("data")
    if isinstance(data, dict) and key in data:
        return data[key]
    return default


def _describe_non_json(url: str, status: int, raw: bytes) -> str:
    """Turn a proxy/CDN rejection into something actionable."""
    body = raw[:200].decode("utf-8", "replace").strip()
    host = urllib.parse.urlparse(url).netloc
    if "1010" in body or "Cloudflare" in body or "cloudflare" in body:
        return (
            f"{url} was blocked by Cloudflare ({body!r}), so the request never "
            f"reached PHP. Retry with a different --user-agent, and if it "
            f"persists ask the host to allow this IP: Security → WAF → "
            f"Bot Fight Mode on {host}."
        )
    if status == 403 and body.startswith("<"):
        return (
            f"{url} answered 403 with an HTML page instead of JSON — usually a "
            f"WAF/CDN rule on {host}, not an authentication failure. Retry with "
            f"--user-agent set to a browser string."
        )
    if status == 429:
        return f"{url} rate-limited this machine (HTTP 429). Wait a minute and re-run."
    return f"{url} answered {status} with non-JSON: {body[:160]!r}"


def _multipart(fields: dict[str, str], files: dict[str, Path]) -> tuple[bytes, str]:
    """PHP-friendly multipart body (only stdlib, no requests dependency)."""
    boundary = "----TheStudentImport" + uuid.uuid4().hex
    chunks: list[bytes] = []
    for name, value in fields.items():
        chunks.append(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n".encode("utf-8")
        )
    for name, path in files.items():
        filename = path.name.replace('"', "_")
        chunks.append(
            f'--{boundary}\r\n'
            f'Content-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
            f"Content-Type: application/pdf\r\n\r\n".encode("utf-8")
        )
        chunks.append(path.read_bytes())
        chunks.append(b"\r\n")
    chunks.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


# ── upload loop ──────────────────────────────────────────────────────────────


def upload_all(
    papers: list[Item],
    backend: Backend,
    root: Path,
    *,
    status: str,
    state_path: Path,
    skip_existing: bool,
    delay: float,
    retries: int,
    limit: int | None,
    workers: int = 1,
    scheme_mode: str = "attach",
    max_body_mb: float = 8.0,
) -> None:
    done = load_state(state_path)
    existing = backend.existing_keys() if skip_existing else set()
    if existing:
        print(f"  {len(existing)} paper(s) already on the server will be skipped")

    queue = [
        paper for paper in papers
        if paper.rel_path not in done
        and not (skip_existing and paper.key in existing)
        and paper.size_bytes <= MAX_PDF_BYTES
    ]
    skipped_limit = [p for p in papers if p.size_bytes > MAX_PDF_BYTES]
    if skipped_limit:
        print(f"  {len(skipped_limit)} paper(s) skipped — over the "
              f"{MAX_PDF_BYTES // 1048576} MB limit:")
        for paper in skipped_limit:
            print(f"    {human_size(paper.size_bytes):>9}  {paper.rel_path}")

    if limit:
        queue = queue[:limit]

    total_bytes = sum(p.size_bytes for p in queue) + sum(
        p.scheme.size_bytes for p in queue if p.scheme
    )
    print(f"  queued {len(queue)} paper(s), {human_size(total_bytes)} to send")
    print(f"  {workers} parallel upload(s)\n" if workers > 1 else "")

    failures: list[dict] = []
    deferred: list[dict] = []
    fatal: list[str] = []
    started = time.time()
    counters = {"sent_bytes": 0, "done": 0, "ok": 0}
    lock = threading.Lock()

    def send(paper: Item) -> None:
        """One paper, with retries. Runs on a worker thread."""
        if fatal:
            return

        # Shared hosts commonly cap the request body at 8 MB; sending a big
        # paper plus its scheme in one request gets the whole form discarded.
        budget = max_body_mb * 1048576
        combined = paper.size_bytes + (
            paper.scheme.size_bytes if paper.scheme else 0
        )
        with_scheme = scheme_mode != "skip" and paper.scheme is not None
        if with_scheme and scheme_mode == "auto" and combined > budget:
            with_scheme = False
            with lock:
                deferred.append({
                    "path": paper.rel_path,
                    "scheme_path": paper.scheme.rel_path if paper.scheme else "",
                    "paper_size": human_size(paper.size_bytes),
                    "combined": human_size(combined),
                    "reason": f"paper + scheme is {human_size(combined)}, over "
                              f"the {max_body_mb:g} MB request budget",
                })
                print(f"{paper.label():<56} … scheme deferred "
                      f"({human_size(combined)})")

        attempt = 0
        while True:
            attempt += 1
            try:
                reply = backend.upload(paper, root, status, with_scheme=with_scheme)
                with lock:
                    append_state(state_path, paper, reply.get("paper_id"))
                    counters["sent_bytes"] += paper.size_bytes + (
                        paper.scheme.size_bytes if paper.scheme else 0
                    )
                    counters["done"] += 1
                    counters["ok"] += 1
                    elapsed = time.time() - started
                    rate = counters["sent_bytes"] / elapsed / 1024 if elapsed else 0
                    pages = f"{paper.pages}p" if paper.pages else "?p"
                    print(f"[{counters['done']:>4}/{len(queue)}] "
                          f"{paper.label():<56} ✓ "
                          f"{human_size(paper.size_bytes):>9} {pages:>5}  "
                          f"id={reply.get('paper_id')}  {rate:4.0f} KB/s")
                return
            except BodyTooLarge as error:
                with lock:
                    fatal.append(str(error))
                    append_state(state_path, paper, None, error=str(error))
                return
            except Exception as error:  # noqa: BLE001 - reported, not raised
                if attempt <= retries:
                    with lock:
                        print(f"{paper.label():<56} ↻ {error} — retrying "
                              f"in {attempt * 2}s")
                    time.sleep(attempt * 2)
                    continue
                with lock:
                    counters["done"] += 1
                    print(f"[{counters['done']:>4}/{len(queue)}] "
                          f"{paper.label():<56} ✗ {error}")
                    failures.append({
                        "path": paper.rel_path,
                        "subject": paper.subject,
                        "year": paper.year,
                        "medium": paper.medium,
                        "error": str(error),
                    })
                    append_state(state_path, paper, None, error=str(error))
                return

    if workers <= 1:
        # Sequential keeps the log in manifest order and lets a single stream
        # use the whole uplink, which is usually all a shared host will take.
        for paper in queue:
            send(paper)
            if delay:
                time.sleep(delay)
    else:
        with ThreadPoolExecutor(max_workers=workers) as pool:
            list(pool.map(send, queue))

    elapsed = time.time() - started
    print()
    print(f"  finished in {elapsed / 60:.1f} min · "
          f"{human_size(counters['sent_bytes'])} sent")
    print(f"  uploaded {counters['ok']} · failed {len(failures)} · "
          f"schemes deferred {len(deferred)}")

    if fatal:
        print()
        print("  STOPPED — the server refused the request body:")
        print(f"    {fatal[0]}")
        print()
        print("  PHP's post_max_size is smaller than the file being sent, so it")
        print("  throws the whole form away and upload_paper.php sees no fields.")
        print("  Raise it on the host (DirectAdmin → PHP settings, or a .user.ini")
        print("  next to the API):  post_max_size = 64M · upload_max_filesize = 40M")
        print("  then re-run this command — finished papers are skipped, so it")
        print("  picks up exactly here.")

    if deferred:
        pending_path = state_path.with_name("schemes_pending.csv")
        with pending_path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(
                handle, fieldnames=["path", "scheme_path", "paper_size",
                                    "combined", "reason"]
            )
            writer.writeheader()
            writer.writerows(deferred)
        print(f"  deferred → {pending_path} "
              f"(send those with --scheme-mode attach once the limit is raised)")
    if failures:
        failed_path = state_path.with_name("upload_failed.csv")
        with failed_path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(
                handle, fieldnames=["path", "subject", "year", "medium", "error"]
            )
            writer.writeheader()
            writer.writerows(failures)
        print(f"  failures → {failed_path}")
    print(f"  state    → {state_path} (re-run the same command to resume)")


def load_state(state_path: Path) -> set[str]:
    if not state_path.exists():
        return set()
    done: set[str] = set()
    for line in state_path.read_text(encoding="utf-8").splitlines():
        try:
            row = json.loads(line)
        except json.JSONDecodeError:
            continue
        if row.get("ok"):
            done.add(row["path"])
    return done


def append_state(state_path: Path, paper: Item, paper_id, error: str | None = None) -> None:
    row = {
        "path": paper.rel_path,
        "ok": error is None,
        "paper_id": paper_id,
        "at": time.strftime("%Y-%m-%dT%H:%M:%S"),
    }
    if error:
        row["error"] = error
    with state_path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(row, ensure_ascii=False) + "\n")


# ── entry point ──────────────────────────────────────────────────────────────


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Bulk-upload TheStudent past papers from the branded archive.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--scan", type=Path, metavar="DIR",
                        help="root of the Branded_Past_Papers folder")
    source.add_argument("--tree", type=Path, metavar="FILE",
                        help="a directory-structure text dump (plan only)")

    parser.add_argument("--upload", action="store_true",
                        help="actually send the files (default: plan only)")
    parser.add_argument("--manifest", type=Path, metavar="CSV",
                        help="write the generated manifest here")
    parser.add_argument("--report", type=Path, metavar="TXT",
                        help="write a plain-text summary here")
    parser.add_argument("--issues", type=Path, metavar="CSV",
                        help="write per-file warnings here")
    parser.add_argument("--stage", type=Path, metavar="DIR",
                        help="copy every paper and scheme into DIR under its "
                             "served file name — upload that tree to the host, "
                             "then import the SQL")
    parser.add_argument("--emit-sql", type=Path, metavar="FILE",
                        help="write INSERT statements for phpMyAdmin (no HTTP)")
    parser.add_argument("--copy-map", type=Path, metavar="CSV",
                        help="write source → served path for every staged file")
    parser.add_argument("--upload-prefix", default="uploads/papers",
                        help="folder under the web root holding the files "
                             "(default: uploads/papers)")
    parser.add_argument("--serve-base-url",
                        default=os.environ.get("TS_SERVE_URL", DEFAULT_BASE_URL),
                        help="public root used inside pdf_url "
                             f"(default: {DEFAULT_BASE_URL})")
    parser.add_argument("--state", type=Path, default=Path("import_state.jsonl"),
                        help="progress log for resuming (default: ./import_state.jsonl)")
    parser.add_argument("--status", default="published",
                        choices=["published", "draft", "archived"],
                        help="row status to write (default: published)")
    parser.add_argument("--limit", type=int, help="upload at most N papers this run")
    parser.add_argument("--delay", type=float, default=0.0,
                        help="seconds to sleep between uploads (workers=1)")
    parser.add_argument("--scheme-mode", default="attach",
                        choices=["attach", "auto", "skip"],
                        help="attach = paper + scheme in one request (default); "
                             "auto = attach only when both fit --max-body-mb, "
                             "otherwise upload the paper and defer the scheme; "
                             "skip = papers only")
    parser.add_argument("--max-body-mb", type=float, default=8.0,
                        help="request body budget for --scheme-mode auto "
                             "(default 8, the usual shared-hosting post_max_size)")
    parser.add_argument("--workers", type=int, default=1, metavar="N",
                        help="upload N papers at once (4–6 usually saturates a "
                             "home uplink; more just queues on the server)")
    parser.add_argument("--retries", type=int, default=2, help="retries per paper")
    parser.add_argument("--no-skip-existing", action="store_true",
                        help="upload even when the paper is already on the server")
    parser.add_argument("--languages-medium", default="language",
                        choices=["language", "english", "sinhala", "tamil"],
                        help="medium for papers in the Languages folder "
                             "(default: the language itself, e.g. Japanese)")
    parser.add_argument("--no-pages", action="store_true",
                        help="skip reading page counts from the PDFs")
    parser.add_argument("--base-url", default=os.environ.get(
        "TS_BASE_URL", DEFAULT_BASE_URL), help=f"backend root (default: {DEFAULT_BASE_URL})")
    parser.add_argument("--token", default=os.environ.get("TS_ADMIN_TOKEN"),
                        help="admin session token (or set TS_ADMIN_TOKEN)")
    parser.add_argument("--email", help="admin email — signs in for a fresh token")
    parser.add_argument("--password", help="admin password (prompted if omitted)")
    parser.add_argument("--otp", help="email verification code, when the server asks")
    parser.add_argument("--user-agent", default=USER_AGENT,
                        help="User-Agent to send (a CDN may reject the default)")
    parser.add_argument("--insecure", action="store_true",
                        help="skip TLS verification (self-signed test hosts only)")
    args = parser.parse_args(argv)

    # 1. discover
    if args.tree:
        entries: list[tuple[str, Path | None]] = walk_tree_dump(args.tree)
        root = None
        if not entries:
            print("No PDFs found in that tree dump.", file=sys.stderr)
            return 2
    else:
        root = args.scan.expanduser().resolve()
        if not root.is_dir():
            print(f"{root} is not a directory.", file=sys.stderr)
            return 2
        entries = walk_directory(root)
        if not entries:
            print(f"No PDFs under {root}.", file=sys.stderr)
            return 2

    print(f"  found {len(entries)} PDF(s) in "
          f"{'the tree dump' if args.tree else root}")

    # 2. classify + pair
    papers, orphans, issues = build_items(
        entries,
        languages_medium=args.languages_medium,
        read_pages=not args.no_pages,
    )
    print_plan(papers, orphans, issues)

    if args.manifest:
        write_manifest(args.manifest, papers, orphans)
    if args.issues:
        write_issues(args.issues, issues)
    if args.report:
        with args.report.open("w", encoding="utf-8") as handle:
            with _Tee(handle):
                print_plan(papers, orphans, issues)
        print(f"  report   → {args.report}")

    # ── direct-to-server route: no HTTP upload at all ────────────────────
    if args.emit_sql or args.stage:
        if args.stage:
            if args.tree:
                print("--stage needs the real files: use --scan.", file=sys.stderr)
                return 2
            if args.stage.exists() and any(args.stage.iterdir()):
                print(f"  note: {args.stage} is not empty — existing files are "
                      f"kept and overwritten one by one")
            args.stage.mkdir(parents=True, exist_ok=True)
            print(f"  staging {len(papers)} paper(s) into {args.stage} …")
            mapping = stage_files(papers, root, args.stage)
            print(f"  staged  {len(mapping)} file(s)")
            if args.copy_map:
                write_copy_map(args.copy_map, mapping, args.upload_prefix)
        if args.emit_sql:
            write_sql(args.emit_sql, papers, args.serve_base_url, args.upload_prefix)
        print()
        print(f"  Next: upload the contents of {args.stage or '<staged folder>'} to")
        print(f"  the web root so they land under {args.upload_prefix.strip('/')}/,")
        print("  then import the .sql file in phpMyAdmin.")
        print()
        return 0

    if not args.upload:
        print("  plan only — add --upload to send these to the backend,")
        print("  or --stage/--emit-sql for the direct-to-server route.\n")
        return 0

    if args.tree:
        print("Uploading needs the real files: use --scan, not --tree.", file=sys.stderr)
        return 2

    # 3. upload
    backend = Backend(
        args.base_url,
        token=args.token,
        insecure=args.insecure,
        user_agent=args.user_agent,
    )
    try:
        if not backend.token:
            if not args.email:
                print("Provide --email (to sign in) or --token.", file=sys.stderr)
                return 2
            password = args.password or getpass.getpass(f"Password for {args.email}: ")
            try:
                backend.login(args.email, password, otp=args.otp)
            except OtpRequired as challenge:
                # The code has just been mailed by this very login call, so ask
                # for it now: a code from an earlier run has already been
                # invalidated by the attempt above.
                print(f"  {challenge.reason or 'A verification code was emailed.'}")
                try:
                    code = input(
                        f"  Code sent to {challenge.email} (6 digits): "
                    ).strip()
                except EOFError:
                    raise RuntimeError(
                        "A verification code is needed but this shell cannot ask "
                        "for it. Re-run in a terminal, or pass --token with a "
                        "session from the app."
                    ) from None
                if not code:
                    raise RuntimeError("No code entered — nothing was uploaded.")
                backend.verify_otp(challenge.email, code)
        user = backend.whoami()
        print(f"  signed in as {user.get('email')} ({user.get('role')})")

        upload_all(
            papers,
            backend,
            root,
            status=args.status,
            state_path=args.state,
            skip_existing=not args.no_skip_existing,
            delay=args.delay,
            retries=args.retries,
            limit=args.limit,
            workers=max(1, args.workers),
            scheme_mode=args.scheme_mode,
            max_body_mb=args.max_body_mb,
        )
    except KeyboardInterrupt:
        print("\n  stopped — progress is saved, re-run to continue.")
        return 130
    except Exception as error:  # noqa: BLE001
        print(f"\n  {error}", file=sys.stderr)
        return 1
    return 0


class _Tee:
    """Write stdout to the console and a file at the same time."""

    def __init__(self, handle):
        self.handle = handle
        self.stdout = sys.stdout

    def __enter__(self):
        sys.stdout = self

    def __exit__(self, *exc_info):
        sys.stdout = self.stdout

    def write(self, text):
        self.stdout.write(text)
        self.handle.write(text)

    def flush(self):
        self.stdout.flush()
        self.handle.flush()


if __name__ == "__main__":
    raise SystemExit(main())
