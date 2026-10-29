# Past papers screen

The library of real, uploaded papers — 741 papers and their marking schemes,
read straight from the server. Design reference: `designs/29-papers-screen-v2.png`.

File: `app/src/main/java/com/example/ui/screens/PapersScreen.kt`.

---

## 1. Searching by the name a student actually uses

A paper is called two things: its long name on the timetable ("Combined
Mathematics") and the short code printed on the paper itself ("CM"). Students
use both, so the search field matches both.

`matchesQuery()` checks, case-insensitively:

| Field | Example |
|---|---|
| Title | "2023 Combined Mathematics Paper I" |
| Subject long name | "Combined Mathematics" |
| Subject short code, via `SubjectCodes.of()` | "CM" |
| Year | "2023" |
| Medium | "Sinhala" |
| Grade, exam type, category, stream | "Grade 13", "A/L" |

Two passes are made: a plain `contains`, then a punctuation- and space-insensitive
one, so `combinedmaths` finds Combined Mathematics as well. A code chip row
(PHY, CM, CHEM, BIO, ICT) sits under the search field for the common subjects;
tapping one fills the query, tapping it again clears it.

`SubjectCodes`' fallback code `PAPER` is never used as a search term — otherwise
every paper would answer the word "paper".

---

## 2. Paper and scheme download separately

Each row offers **two** downloads, because they are two files on the server:

- **Paper** — filled button, tag `papers_download_<id>`, `onDownloadPaper`.
- **Scheme** — outlined button, tag `papers_download_scheme_<id>`,
  `onDownloadScheme`. Renders as a disabled "No scheme" when
  `hasMarkingScheme` is false.

Wiring:

```
PapersScreen.onDownloadScheme
  → MainActivity (PastPapersTab)
  → TheStudentViewModel.downloadSchemeOffline(paper)
  → PastPapersRemoteRepository.downloadMarkingSchemePdf(paper)   // markingSchemeUrl
```

`downloadMarkingSchemePdf` saves as `<title>_Scheme.pdf` in Downloads; it returns
early when the paper has no scheme, so a missing file can never look like a
successful download.

---

## 3. Two layouts, fifteen to a page

The sort row carries a layout toggle (list ↔ cards) — same list, two densities:

- **List** (`PaperRow`) — 40×50 cover, two-line title, `CODE · subject · year ·
  medium`, badges, bookmark, two small download pills.
- **Cards** (`PaperCardRow`) — 74×94 cover, three-line title, two wide buttons
  labelled "Download paper" / "Marking scheme".

Pagination is fixed at `PAPERS_PER_PAGE = 15`. The bar shows `‹ 1 2 3 … N ›` with
a windowed number set around the current page, plus
"Page x of y · 15 per page · N papers". Changing the query, chip, filter or sort
returns to page 1; changing page scrolls the list back to the top.

---

## 4. Shimmer on every change, not just the first load

The old gate (`isLoading && papers.isEmpty()`) only ever shimmered on a cold
start. Now `showSkeletons` is true when:

- `isLoading` — the view model is fetching (covers the refresh button), or
- `isSettling` — 320 ms after a query, chip, filter or sort change, or
- `isRefreshing` — the header refresh button was just tapped.

The refresh shimmer caps itself after two seconds so a request that never
reports back cannot leave the screen shimmering forever; the previous list
returns instead.

The refresh button sits in the header next to the filter button, and only on the
Papers segment.

---

## 5. Empty and offline states

Order of dispatch in the body: skeletons → no papers at all
(`LibraryOfflineState`, which offers retry and the server URL) → no results for
the current filters (`EmptyResultsState`, which clears them) → the paged list.
