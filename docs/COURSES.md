# Courses collection

A curated index of **free courses that other people made**. Not ours, not hosted
by us, not sold by us. The screen exists because a Sri Lankan undergraduate can
go through four years without anyone telling them that Harvard's CS50, the IITs'
NPTEL courses and Moratuwa's own open learning platform are free.

Design references: `designs/20-courses-collection.png` (index),
`designs/20-course-detail.png` (detail). The generated text in the mockups is
placeholder; the shipped screen carries the real titles in `CourseCatalog`.

---

## 1. The two rules

1. **Every card names its owner.** The provider is a line of text on the card
   ("Provided by edX"), not a logo in a corner. The detail page repeats it above
   the button that leaves the app, and every entry is credited again in the
   block at the end of the list.
2. **Nothing is re-hosted.** The app never mirrors a video, copies a syllabus or
   proxies an enrolment. Tapping opens the provider's own page, so the student
   enrols under *their* terms, and their prices and certificate rules stay their
   business. Videos are embedded from `youtube-nocookie.com` and credited to the
   channel that made them, with a "Watch on YouTube" exit.

The catalog is deliberately honest about certificates, because that is where
list articles usually cheat. Every course carries one of four facts:

| Value | Means |
| --- | --- |
| `FREE` | Free to take **and** a free certificate/badge on completion |
| `PAID` | Learn free, certificate paid (or via financial aid) |
| `PARTICIPATION` | A statement of participation, not an accredited award |
| `NONE` | Genuinely nothing at the end — good to learn from, no paper |

The footer adds the sentence that keeps us accurate: certificate terms and
prices belong to the provider and can change, so check the course page.

## 2. Where the data lives

Bundled in `data/model/CourseCatalog.kt` — one `object`, no database, no server
call. Reasons:

- it is static editorial content that changes a few times a year, not per user;
- it must work on a fresh install before the relay is configured (the app's
  PHP/SQL deploy is a separate, manual step);
- there is no personal data, so there is nothing to sync.

Cost: adding a course needs an app update. If that becomes annoying, the same
shape moves to `api/courses.php` + a seed table with the bundled list as the
offline fallback — the models already separate provider, course and video.

```
CatalogProvider  name, url, kind            → the owner
CatalogCourse    provider, url, subject, stages, level, hours,
                 certificate, priceNote, summary, skills, videoId,
                 featured, local
CatalogVideo     videoId, title, channel, minutes, topic
```

`provider` is a **name**, resolved through `CourseCatalog.provider(name)`; a
course whose provider is missing from `providers` simply shows its initials and
still credits the name, so a typo can never silently drop the credit.

## 3. What is in it now

42 courses across 16 providers, chosen for three audiences:

- **Sri Lanka** — University of Moratuwa Open Learning (Python for Beginners,
  Web Design for Beginners, Project Management, the six-module Trainee Full
  Stack Developer path), free with UoM certificates, sponsored by DP Education.
- **Undergraduates** — CS50x / CS50P / CS50AI (Harvard via edX), freeCodeCamp
  certifications, MIT 18.06, NPTEL, Saylor, Cisco Networking Academy, Kaggle
  Learn, Microsoft Learn.
- **Graduates and researchers** — Writing in the Sciences (Stanford), Learning
  How to Learn, OpenLearn's Jamovi statistics course, How to Speak.

Plus business/career: IBM SkillsBuild, Google Skillshop, HP LIFE, Alison, and
the OpenLearn business-writing course.

Second wave (2026-10-29): Moratuwa's Python Programmer, Data Scientist and Mobile
Developer career paths; CS50W and CS50SQL; MIT 6.006 algorithms; freeCodeCamp's
Data Analysis with Python certification; Google Data Analytics and Google Project
Management; IBM Data Science; Kaggle's Intro to Machine Learning; Cisco's
Introduction to Cybersecurity; Khan Academy statistics; Microsoft's Azure
fundamentals path.

Ten videos, all **verified against YouTube's oEmbed endpoint** before shipping,
so an embed cannot 404: three MIT lectures, two 3Blue1Brown explainers, five
freeCodeCamp full courses, one StatQuest video.

## 4. The screen

`ui/screens/CourseCollectionScreen.kt` — full-screen route (`isCourseCollectionOpen`),
opened from the **Courses collection** card on Home. No bottom bar, no app header,
own back button, system back closes the detail before the screen.

**Only the title bar is fixed.** Header (back, title, counts) stays put; search
and filters scroll with the page, so content gets the screen back.

Sections, top to bottom:

1. **fixed header** — back, title, course and provider counts;
2. search (scrolls) — title, subject, provider and skills;
3. a single **filter row** (scrolls): a `Filters (n)` button plus the currently
   applied filters as chips, with a **Clear** action;
4. **Editor's picks** — a rail that slides itself every 4.2s with a 700ms eased
   scroll, pausing while a finger is on it. It **loops forward**: the card list is
   laid out three times over and the rail starts in the middle copy, so after the
   last card it slides straight into the first instead of reversing back through
   everything. When it drifts into the third copy it silently slides back one
   copy — the pixels are identical, so the jump is invisible. Dots follow the
   card;
5. **Watch first** — YouTube rail: real thumbnails, runtime badge, channel name;
6. **Where these come from** — provider tiles with their own logo, name, kind
   and course count; tapping one filters the list;
7. the results — **15 courses per page**, with a numbered pager underneath
   ("Showing 16–30 of 42", Prev / 1 2 3 / Next). Tapping a page scrolls back up
   to the results heading and shows a brief row skeleton (~0.38s) while the new
   page resolves, so a page change looks like one. Any filter resets to page 1.
   Tags: `courses_pager`, `courses_page_1…n`, `courses_page_prev`,
   `courses_page_next`, `courses_page_skeleton`;
8. the credit block.

### Filters, pulled up from the bottom

Everything lives in one `ModalBottomSheet` opened by the filter button, grouped
into **Certificate** (free, paid, statement of participation, none), **Who it's
for** (A/L, Undergraduate, Postgraduate), **Level** (beginner → advanced),
**Subject** and **Provider** — all 16. Chips apply immediately and the button at
the bottom reads "Show N courses", so the count always describes the list behind
the sheet. Tapping a selected chip removes it; **Clear all** empties everything.

**The sheet is compact, not a page.** Header and the apply button are fixed; the
chips scroll inside a `heightIn(max = 356.dp)` area, so the sheet tops out at
roughly 60% of the screen however many subjects are added later. Section labels
are 10.5sp caps, chips are 32dp, and the drag handle is a 4dp pill — the default
handle plus its padding was most of the reason the earlier version opened at
nearly full height.

Slider, video rail and provider rail hide while a filter is active, so results
are never pushed below three screens of browsing furniture.

### Shimmer

The catalog is bundled, so nothing is actually fetched — but a screen that pops
in fully formed feels unfinished, and a filter that changes instantly feels like
nothing happened. So:

- **on arrival**, ~0.7s of shimmer that mirrors the real layout (slider, video
  cards, provider tiles, course rows), so the content lands without a jump;
- **on a filter change**, ~0.4s of shimmer over the results;
- **when the pager changes page**, ~0.38s of row shimmer (`courses_page_skeleton`)
  in place of the results, right after scrolling up to the heading;
- **when a course card is opened**, ~0.4s of detail shimmer — coloured hero with
  white sweeps, facts card, certificate row, about lines, video block and the
  sticky button. The back button on that skeleton is real, so a loading state
  can never trap you (tag `course_detail_skeleton`).

Typing in the search field does **not** shimmer — flashing the list on every
keystroke would be worse than no animation. The shimmer is one animated
`Brush.linearGradient` shared by every placeholder (tag `courses_skeleton`).

### Typography

One local scale (`private object CT`) drives the whole screen, with an **11sp
floor**: `screen 21`, `hero 22`, `section 16`, `cardTitle 14.5`, `body 13`,
`meta 12`, `micro 11`, `chip 12`, plus matching line heights. Nothing is 9sp to
make it fit; long strings wrap (`maxLines` 2–3) instead of being clipped.

### Detail

Gradient hero (subject colour), a facts card (level and time side by side, the
audience list on its own full-width row so it wraps instead of clipping), the
certificate fact in plain words, summary, skills, an inline video card, an
**Ownership** box, and a sticky **Open on {provider}** button that launches the
system browser through `LocalUriHandler` (wrapped in `runCatching`, so a device
with no browser cannot crash the screen).

### Video player

`YouTubeEmbedDialog` is a `WebView` that loads the embed URL **with an explicit
`Referer` header**:

```kotlin
val origin = "https://" + context.packageName          // e.g. https://com.thestudent.sl
loadUrl(
    "https://www.youtube.com/embed/$id?autoplay=1&playsinline=1&rel=0&origin=$origin",
    mapOf("Referer" to origin)
)
```

**Why exactly this.** YouTube now refuses to configure a player — error 153, and
the 152 family — for an embed that arrives without a `Referer`, because the
iframe must be able to say which app is hosting it. The history of this screen is
that rule being hit three times:

| Attempt | Result |
| --- | --- |
| `loadUrl("…/embed/…")` from the app WebView | black box (no referer) |
| iframe inside `loadDataWithBaseURL("https://www.youtube.com/")` | still error 152-4 |
| `android-youtube-player`'s `YouTubePlayerView` | still error 152-4 — it cannot set the header either |
| **embed URL + `Referer` header + matching `origin=`** | plays |

The header and the `origin` parameter must agree, which is why both come from
one `origin` value derived from the package name. `playsinline=1` keeps it inside
the dialog, `rel=0` stops related videos from other channels appearing, and the
channel is still credited under the player with a **Watch on YouTube** exit.

**The white screen after that.** With the header in place the player loaded and
played — audio, scrubbing, controls — but painted a blank white rectangle. Three
things cause exactly that, and all three are now handled:

| Cause | Fix |
| --- | --- |
| the view sits in a **dialog window**, a second window that composites everything but the video frames | the player is an in-composition overlay (`course_video_overlay`), not a `Dialog` |
| something **clips** the `WebView`, forcing it into an offscreen layer | no `clip()` above it; the card's corners are a rounded *background*, not a clip |
| the app has no **hardware acceleration** (sound plays, picture is blank) | `android:hardwareAccelerated="true"` on `<application>`, plus `setLayerType(LAYER_TYPE_HARDWARE)` on the WebView |

The overlay **swallows every gesture** through a `pointerInput` consumer, so the
catalogue behind it cannot scroll, fling or open another course while a video is
playing — Compose hit-tests siblings top-down and stops at the first taker.

The player shows a spinner until `onPageFinished` and, if the *main frame* fails
to load, swaps to the thumbnail with an **Open in YouTube** button — never an
empty black rectangle. `onRelease` stops the load, blanks the page and destroys
the WebView so audio cannot keep playing after the dialog closes.

No third-party player dependency is needed for this; the `core` library added in
the previous round was removed again.

### Provider logos

Each provider shows **its own icon**, fetched at runtime from Google's public
favicon endpoint (`https://www.google.com/s2/favicons?domain=…&sz=128`).
Nothing is bundled, nothing is redrawn, and initials appear while it loads or
offline — so the credit still reads correctly on a bad connection. Using a
provider's mark to identify the provider you are crediting is exactly what the
mark is for; it is never used as TheStudent's own branding.

## 5. Test tags

`courses_collection_content`, `courses_back`, `courses_search`,
`courses_filters_button`, `courses_filter_sheet`, `courses_filters_apply`,
`courses_chip_all`, `courses_chip_free`, `courses_chip_paid`,
`courses_chip_participation`, `courses_chip_none`,
`courses_chip_provider_{name}`, `courses_stage_*`, `courses_level_*`,
`courses_subject_*`, `courses_featured_{id}`, `courses_video_{videoId}`,
`courses_provider_{name}`, `courses_card_{id}`, `courses_clear_filters`,
`courses_empty`, `courses_empty_clear`, `courses_skeleton`,
`course_detail_skeleton`,
`courses_credit_block`, `course_detail_{id}`, `course_detail_back`,
`course_detail_video`, `course_detail_open`, `course_video_player`,
`course_video_close`, `course_video_open_youtube`, `course_video_fallback`,
`home_course_collection_card`.

## 6. Adding a course

1. Append a `CatalogCourse` to `CourseCatalog.courses` — copy `priceNote` wording
   from the provider's own page, never from an aggregator.
2. If the provider is new, add a `CatalogProvider` first; the credits block and
   the provider rail both read from it.
3. For a video, take the id from the watch URL and check it resolves:
   `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=<id>&format=json`
   — a title back means the embed will work.
4. Keep `featured = true` to at most ~6 courses; the slider is an argument, not
   a catalogue.
5. Never add a course whose certificate claim you have not read on its own page.

## 7. Fixes after first review (2026-10-28)

| Report | Cause | Fix |
| --- | --- | --- |
| "Level / Time / For" clipped on small phones | three weighted columns inside one row | two columns plus a full-width audience row that wraps |
| Top bar scrolled away | header lived inside the `LazyColumn` | header, search and filters moved to a fixed `Column` above it |
| Editor's picks didn't move | no timer | auto-advance every 4.5s, paused while dragging, dots follow the card |
| YouTube "video player configuration" error | WebView embed had no referer | iframe served from an HTML page based at `https://www.youtube.com/` |
| Tapping a provider jumped to the bottom | shorter filtered content kept the old scroll offset | filter changes animate the list back to the top |
| Provider cards felt empty | initials avatar, small | real provider favicon at 46dp, name, kind, course count, taller card |
| Wanted free/paid and more filters | only four certificate chips | certificate + stage + level + subject + provider filters, with a clear action |
| Some text too small | sizes drifted to 9–10.5sp | single `CT` type scale with an 11sp floor |

## 8. Second review (2026-10-28)

| Report | Cause | Fix |
| --- | --- | --- |
| Search and filters should scroll too | everything above the list was pinned | only the title bar is fixed now |
| All filters in one place | three fixed chip rows ate the screen | one `Filters` button + a bottom sheet with grouped chips, applied live |
| Video showed an empty black card | WebView embed without a proper origin/referer | replaced with `YouTubePlayerView` (android-youtube-player 12.1.2) |
| Wanted shimmer on filters and entry | no loading state | animated shimmer on arrival (0.7s) and on facet changes (0.4s) |
| Course detail still popped in | only the list had a loading state | detail gets its own 0.4s shimmer with a working back button |
| Filter sheet opened at nearly full height | default handle + uncapped content | fixed header and button, chips scroll inside 356dp, slim handle |
| YouTube error 152-4 | embed arrived without a `Referer`; the library could not set it either | WebView embed with a `Referer` header and a matching `origin=`, spinner + fallback |
| Editor's picks snapped back at the end | `(index + 1) % size` animated backwards from the last card | three copies, start in the middle, eased `animateScrollBy`, silent recentre |
| Video played but painted white | dialog window + clipped WebView | inline overlay, no clip |
| Video *still* white, with sound and controls | **the web core's video view was never attached** — Chromium hands it back through `WebChromeClient.onShowCustomView`, and if nobody adds it to the hierarchy the page keeps rendering over a blank rectangle | the WebView lives in a `FrameLayout` host that attaches the custom view on `onShowCustomView` and removes it on `onHideCustomView` (also gives fullscreen for free) |
| — | `android:hardwareAccelerated="true"` on `<application>` kept, so the window can composite video at all | an explicit `WebView.setLayerType(LAYER_TYPE_HARDWARE)` was removed again: on some Samsung builds it decouples the compositing layer and produces a solid black screen |
| Detail skeleton vanished twice | both player rewrites replaced the *end* of the file, where it lived | moved above the player section, with a note saying why |
| Background scrolled under the video card | the overlay drew on top but took no gestures | the overlay consumes pointer events, so nothing behind receives them |
| List of 40+ cards made the page very tall | every course rendered at once | 15 per page with a numbered pager, and filters reset to page 1 |
| Page change snapped | new rows replaced the old instantly | pager scrolls to the heading, then row skeletons for ~0.38s |
