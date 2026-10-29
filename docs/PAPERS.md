# Past papers screen

The library of real, uploaded papers — 741 papers and their marking schemes,
read straight from the server. Design reference: `designs/29-papers-screen-v2.png`.

File: `app/src/main/java/com/example/ui/screens/PapersScreen.kt`, with the detail
sheet in `app/src/main/java/com/example/ui/components/PaperDetailDialog.kt`.

---

## 1. Searching by the name a student actually uses

A paper is called two things: its long name on the timetable ("Combined
Mathematics") and the short code printed on the paper itself ("CM"). Students
use both, so the search field matches both.

`matchesQuery()` checks, case-insensitively:

| Field | Example |
|---|---|
| Row title (paper, or paper + "Marking scheme") | "2023 Combined Mathematics — Marking scheme" |
| Subject long name | "Combined Mathematics" |
| Subject short code, via `SubjectCodes.of()` | "CM" |
| Year | "2023" |
| Medium | "Sinhala" |
| Grade, exam type, category, stream | "Grade 13", "A/L" |

Two passes are made: a plain `contains`, then a punctuation- and space-insensitive
one, so `combinedmaths` finds Combined Mathematics as well. `SubjectCodes`'
fallback code `PAPER` is never used as a search term — otherwise every paper
would answer the word "paper".

### Typing is debounced

The field holds `queryInput`; the list filters on `query`, which only catches up
after 400 ms without a keystroke (`SEARCH_SETTLE_MS`). Filtering ~1,200 rows on
every keystroke is what made the field feel laggy. Clearing the field clears
both.

---

## 2. Marking schemes are rows too

Each paper with a scheme contributes **two** rows to the library:

- the question paper, titled as stored;
- the scheme, titled `<paper title> — Marking scheme`, id `<paper id>#scheme`.

So searching "physics marking scheme" — or "scheme" — lists schemes directly
instead of hiding them behind a detail screen. Within a year the paper sorts
before its own scheme. Pages count entries, so the 741 papers show as ~989 rows
and paginate accordingly.

Tapping a scheme row opens the same paper detail sheet, where the scheme is one
tap away.

---

## 3. Downloading lives in the detail sheet

The rows are for finding and opening; the sheet is for acting. Each row shows
only a bookmark. Tapping it opens `PaperDetailDialog`, which carries:

- **Download PDF** — `onDownloadPaper`.
- **Ask AI** — same `onAskAi` path as before.
- **Download marking scheme** — outlined button, tag `paper_detail_scheme`,
  rendered only when `paper.hasMarkingScheme`.

Wiring:

```
PaperDetailDialog.onDownloadScheme
  → MainActivity (active paper detail)
  → TheStudentViewModel.downloadSchemeOffline(paper)
  → PastPapersRemoteRepository.downloadMarkingSchemePdf(paper)   // markingSchemeUrl
```

`downloadMarkingSchemePdf` saves as `<title>_Scheme.pdf` in Downloads and returns
early when the paper has no scheme, so a missing file can never look like a
successful download.

The sheet shows the **full title**, wrapped over as many lines as it needs — no
ellipsis — with the real `subject · year · medium` underneath. The old
"Paper I · answers included" line was invented text and is gone.

---

## 4. Two layouts, fifteen to a page, page bar at the end

The sort row carries a layout toggle (list ↔ cards) — same list, two densities:

- **List** (`PaperRow`) — 40×50 cover, two-line title, `CODE · subject · year ·
  medium`, badges, bookmark.
- **Cards** (`PaperCardRow`) — 74×94 cover, three-line title, same metadata.

Pagination is fixed at `PAPERS_PER_PAGE = 15`. The bar is the **last item of the
LazyColumn**, not an overlay: it scrolls up with the list and comes to rest under
the final row, showing `‹ 1 2 3 … N ›` (a windowed number set around the current
page) plus "Page x of y · 15 per page · N papers". Changing the query, chip,
filter or sort returns to page 1; changing page scrolls the list to the top under
a short shimmer.

---

## 5. Only the top bar is pinned

`Column`: fixed top bar (back, title, subtitle, refresh, filters) with
`statusBarsPadding()`, then the scrolling surface.

For the Papers segment everything else — the Papers/Quizzes pills, the search
field, the quick chips and the result/sort row — is the first item of the
LazyColumn, so it scrolls away with the results. The Quizzes segment keeps its
pills above its own list.

---

## 6. Shimmer on every change, not just the first load

The old gate (`isLoading && papers.isEmpty()`) only ever shimmered on a cold
start. Now `showSkeletons` is true when:

- `isLoading` — the view model is fetching (covers the refresh button), or
- `isSettling` — 320 ms after a query settles, or any chip, filter, sort or page
  change, or
- `isRefreshing` — the header refresh button was just tapped.

The refresh shimmer caps itself after two seconds so a request that never
reports back cannot leave the screen shimmering forever; the previous list
returns instead.

---

## 7. Empty and offline states

Dispatch order: skeletons → no papers at all (`LibraryOfflineState`, which offers
retry and the server URL) → no results for the current filters
(`EmptyResultsState`, which clears them) → the paged list. Both empty states are
LazyColumn items sized with `fillParentMaxHeight`, so they scroll too.
