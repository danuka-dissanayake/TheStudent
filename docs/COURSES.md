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

28 courses across 16 providers, chosen for three audiences:

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

Ten videos, all **verified against YouTube's oEmbed endpoint** before shipping,
so an embed cannot 404: three MIT lectures, two 3Blue1Brown explainers, five
freeCodeCamp full courses, one StatQuest video.

## 4. The screen

`ui/screens/CourseCollectionScreen.kt` — full-screen route (`isCourseCollectionOpen`),
opened from the **Courses collection** card on Home. No bottom bar, no app header,
own back button, system back closes the detail before the screen.

**The top bar is fixed.** Header, search and every filter row sit in a `Column`
above the list, so nothing you need disappears when you scroll. Only the
content — slider, video rail, provider rail, course rows, credit block — scrolls.

Sections, top to bottom:

1. header — back, title, course and provider counts, "credited to their owners";
2. search — title, subject, provider and skills;
3. **filters** (three fixed rows):
   - *certificate*: All · Free cert · Paid cert · Participation · No cert ·
     Sri Lanka;
   - *stage and level*: A/L, Undergraduate, Postgraduate, Beginner,
     Intermediate, Advanced;
   - *subject*: every subject in the catalog, derived from the courses.
   Selecting a filter resets the list to the top, which is what stops the old
   scroll offset from dropping you at the bottom of a suddenly shorter page;
4. **Editor's picks** — a sliding rail that advances itself every 4.5s, pausing
   while a finger is on it (`isScrollInProgress`), with a dot indicator that
   follows the first visible card;
5. **Watch first** — YouTube rail: real thumbnails, runtime badge, channel name;
6. **Where these come from** — provider tiles with their own logo, name, kind
   and course count; tapping one filters the list;
7. the list — course cards with subject, title, "Provided by …", certificate
   pill, level and hours;
8. the credit block.

Slider, video rail and provider rail hide while a filter is active, so results
are never pushed below three screens of browsing furniture. Any filter adds a
**Clear filters** action and a "No courses match that" state.

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

`YouTubeEmbedDialog` is a real embed: a `WebView` loading an HTML page whose
base URL is `https://www.youtube.com/`, with a `youtube-nocookie.com` iframe.

The base URL is the fix for the "video player configuration" error: a bare
`loadUrl("…/embed/…")` reaches YouTube **without a referer**, and the embed
refuses to configure a player. Serving the iframe from a page based at
youtube.com gives the WebView that referer.

`onRelease` stops loading, blanks the page and destroys the `WebView`, so audio
stops when the dialog closes. Under the player: the title, the channel ("… on
YouTube") and a **Watch on YouTube** exit.

### Provider logos

Each provider shows **its own icon**, fetched at runtime from Google's public
favicon endpoint (`https://www.google.com/s2/favicons?domain=…&sz=128`).
Nothing is bundled, nothing is redrawn, and initials appear while it loads or
offline — so the credit still reads correctly on a bad connection. Using a
provider's mark to identify the provider you are crediting is exactly what the
mark is for; it is never used as TheStudent's own branding.

## 5. Test tags

`courses_collection_content`, `courses_back`, `courses_search`,
`courses_chip_all`, `courses_chip_free`, `courses_chip_paid`,
`courses_chip_participation`, `courses_chip_none`, `courses_chip_local`,
`courses_stage_*`, `courses_level_*`, `courses_subject_*`,
`courses_featured_{id}`, `courses_video_{videoId}`,
`courses_provider_{name}`, `courses_card_{id}`, `courses_clear_filters`,
`courses_empty`, `courses_credit_block`, `course_detail_{id}`,
`course_detail_back`, `course_detail_video`, `course_detail_open`,
`course_video_player`, `course_video_close`, `course_video_open_youtube`,
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
