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
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
import uuid
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
        return f"{self.subject} {self.year} ({self.medium})"


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


# ── backend ──────────────────────────────────────────────────────────────────


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
        self.token = (reply.get("data") or {}).get("token")
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
        self.token = (reply.get("data") or {}).get("token")
        if not self.token:
            raise RuntimeError("Verification did not return a session token.")
        return reply

    def whoami(self) -> dict:
        reply = self._json(
            self.base_url + AUTH_PATH + "?action=me", headers=self._auth_headers()
        )
        if reply.get("status") != "success":
            raise RuntimeError(reply.get("message") or "The session was refused.")
        user = (reply.get("data") or {}).get("user") or {}
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

    def upload(self, paper: Item, root: Path, status: str) -> dict:
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
        if paper.scheme is not None:
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
        if reply.get("status") != "success":
            raise RuntimeError(reply.get("message") or f"HTTP {status_code}")
        return reply


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
    print(f"  queued {len(queue)} paper(s), {human_size(total_bytes)} to send\n")

    failures: list[dict] = []
    started = time.time()
    sent_bytes = 0
    previous_signature: tuple[str, float] | None = None

    for index, paper in enumerate(queue, start=1):
        # upload_paper.php names the stored file time() + original name, so two
        # uploads of same-named files inside one second would overwrite each
        # other. Serialise that rare case instead of losing a paper.
        signature = (Path(paper.rel_path).name, 0.0)
        if previous_signature and previous_signature[0] == signature[0]:
            waited = time.time() - previous_signature[1]
            if waited < 1.1:
                time.sleep(1.1 - waited)

        stamp = f"[{index:>4}/{len(queue)}] {paper.label():<46}"
        attempt = 0
        while True:
            attempt += 1
            try:
                reply = backend.upload(paper, root, status)
                sent_bytes += paper.size_bytes
                if paper.scheme is not None:
                    sent_bytes += paper.scheme.size_bytes
                append_state(state_path, paper, reply.get("paper_id"))
                elapsed = time.time() - started
                rate = sent_bytes / elapsed / 1024 if elapsed > 0 else 0
                pages = f"{paper.pages}p" if paper.pages else "?p"
                print(f"{stamp} ✓ {human_size(paper.size_bytes):>9} {pages:>5}  "
                      f"id={reply.get('paper_id')}  {rate:4.0f} KB/s")
                break
            except Exception as error:  # noqa: BLE001 - reported, not raised
                if attempt <= retries:
                    backoff = attempt * 2
                    print(f"{stamp} ↻ {error} — retrying in {backoff}s")
                    time.sleep(backoff)
                    continue
                print(f"{stamp} ✗ {error}")
                failures.append({
                    "path": paper.rel_path,
                    "subject": paper.subject,
                    "year": paper.year,
                    "medium": paper.medium,
                    "error": str(error),
                })
                append_state(state_path, paper, None, error=str(error))
                break
        previous_signature = (Path(paper.rel_path).name, time.time())
        if delay:
            time.sleep(delay)

    elapsed = time.time() - started
    print()
    print(f"  finished in {elapsed / 60:.1f} min · {human_size(sent_bytes)} sent")
    print(f"  uploaded {len(queue) - len(failures)} · failed {len(failures)}")
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
    parser.add_argument("--state", type=Path, default=Path("import_state.jsonl"),
                        help="progress log for resuming (default: ./import_state.jsonl)")
    parser.add_argument("--status", default="published",
                        choices=["published", "draft", "archived"],
                        help="row status to write (default: published)")
    parser.add_argument("--limit", type=int, help="upload at most N papers this run")
    parser.add_argument("--delay", type=float, default=0.0,
                        help="seconds to sleep between uploads")
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

    if not args.upload:
        print("  plan only — add --upload to send these to the backend.\n")
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
                if not sys.stdin.isatty():
                    raise RuntimeError(
                        "A verification code is needed but this shell cannot ask "
                        "for it. Re-run in a terminal, or pass --token with a "
                        "session from the app."
                    ) from None
                code = input(f"  Code sent to {challenge.email} (6 digits): ").strip()
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
