# Scrolling performance

Why the app felt "laggy while scrolling" — the audit, what was fixed, and how to
check it again. Written 2026-10-29 after the courses screen shipped.

## What was actually wrong

Four things, in rough order of how much they cost.

### 1. `SubcomposeAsyncImage` inside lazy lists (the big one)

`SubcomposeAsyncImage` runs a **subcomposition** for every instance so it can
offer `loading = {}` and `error = {}` slots. In a recycled list that means a
nested composition per visible row, rebuilt as rows scroll in and out. It is the
single most expensive thing you can put in a `LazyColumn` item, and it was in
both of the app's main scrolling surfaces:

- `PaperCover.kt` — every paper tile;
- `CourseCollectionScreen.kt` — provider logos, video thumbnails, credit block.

**Fixed** by drawing the fallback *underneath* a plain `AsyncImage`:

```kotlin
CoverArt(paper, tint)                       // drawn sheet, always there
if (paper.thumbnailUrl.isNotBlank()) {
    AsyncImage(model = paper.thumbnailUrl, …) // paints over it when loaded
}
```

Coil draws nothing on loading or error, so the fallback shows through — the same
appearance, one composition per row instead of two.

### 2. A dead state collection at the root of the UI tree

`MainActivity` collected `viewModel.chatMessages` at the top level and never used
it (ChatScreen collects its own). Because it sat above every branch, **every
chat database write recomposed the entire app tree** — and the chat layer writes
on a 2–10s background poll, so this fired while the student was scrolling.

**Fixed**: the collection is gone. Chat state is now read only inside the chat
tab, where it belongs.

### 3. Reading a scroll position straight in composition

The Editor's-picks dots read `firstVisibleItemIndex` directly, which changes on
every pixel of every auto-advance and fling, recomposing the rail's parent each
time. It is now a `derivedStateOf` that only changes when the *card* changes.

### 4. Small per-frame waste on the scroll path

- `paperCoverTint()` built a list of eight colours on every call, a few times per
  row. The palette is now a top-level `val`.
- `CourseCatalog.provider()/course()/video()` were linear scans called several
  times per row. They are `by lazy` maps now.
- Missing `key =` on Home's course rail and Learn's lists, and no `contentType`
  on the course rows — both make Compose rebuild slots it could have reused.
  Added.

## What that means in practice

Scrolling cost is now about drawing, not composing: one composition per row, one
image request per row (Coil's memory cache handles the repeats), and no periodic
full-tree recomposition from chat polling.

## Debug builds are slow — verify with a release build

A debug APK runs Compose through the Kotlin compiler without inlining or R8, and
`isDebuggable = true` disables several optimisations. **A Compose debug build
routinely scrolls noticeably worse than release on the same device**, and this
repo's `release` block currently has `isMinifyEnabled = false`, so even release
is unoptimised. To judge the real feel:

```bash
./gradlew :app:assembleRelease        # needs my-upload-key.jks + env vars
# or, quicker, profile a debug build with the numbers below
```

## How to measure, instead of guessing

```bash
# frame timings for the last minute of scrolling
adb shell dumpsys gfxinfo com.thestudent.sl reset
# … scroll the screen by hand for ~20 seconds …
adb shell dumpsys gfxinfo com.thestudent.sl
```

Read the histogram: the `95th percentile` line should be under 16ms on a 60Hz
device for a smooth scroll. If `Janky frames` is high *and* `Number Missed
Vsync` grows while nothing is animating, it is composition; if it only happens
while images load, it is image decoding.

Faster loop for a specific fix:

```bash
adb shell am profile start --sampling 1000 com.thestudent.sl /data/local/tmp/trace.trace
# … interact …
adb shell am profile stop com.thestudent.sl
adb pull /data/local/tmp/trace.trace && open in Android Studio Profiler
```

## Rules to keep

1. **Never `SubcomposeAsyncImage` in a list item.** Draw the placeholder
   underneath and let `AsyncImage` paint over it.
2. **No state collection at the root** unless every screen needs it.
3. **`derivedStateOf` for anything derived from `scrollState`** that is read in
   composition.
4. **Always `key =` and a `contentType =`** on lazy items.
5. Nothing that allocates per frame — hoist palettes, maps and brushes.

## Still on the table (not done)

- `isMinifyEnabled = true` + R8 for release (test the Firebase/Moshi/Retrofit
  paths first).
- A baseline profile (`androidx.profileinstaller` + a macrobenchmark module) —
  the biggest remaining win for cold start and first-scroll jank.
- `Crossfade` between tabs keeps both tabs composed for ~300ms; switching to a
  plain switch would remove that, at the cost of the fade.
