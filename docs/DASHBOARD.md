# The dashboard (Home)

`ui/screens/HomeScreen.kt` — the landing screen after sign-in. It was rebuilt
from scratch in v2.0.3: the old "welcome card + stat row + three rails" layout is
gone, replaced by a report-first dashboard.

The reference renders are `designs/21-home-dashboard-top.png` and
`designs/21-home-dashboard-lower.png`.

## Layout, top to bottom

| Block | What it shows | Where the data comes from |
|---|---|---|
| Greeting | time-of-day greeting, first name, amber streak pill, avatar | `UserProfile.name`, `max(summary.streakDays, userProfile.streakDays)` |
| Hero report card | white accuracy ring, `quizzes passed`, `studied`, `EXP earned`, CTA, `Full report` | `QuizProgressSummary.averagePercent / quizzesPassed / quizzesPublished / durationSeconds / expFromQuizzes` |
| Quick actions | Ask AI · Courses · Papers · Report | callbacks (`onOpenAiAssistant`, `onOpenCourseCollection`, `onNavigateTab(PAPERS)`, `onOpenProgress`) |
| This week | up to four subject accuracy bars, seven weekday bars, `best` / `attempts` / `time` chips | `QuizProgress.subjects[].accuracy`, `QuizProgress.recent[].submittedAt`, `summary.bestPercent / attempts` |
| Practise next | subject chip, quiz title, accuracy bar, attempts left, Practise button | `QuizProgress.practice` (server picks it); the button calls `onContinueLearning` |
| Promos | dark `N free courses` card with three provider logos · Language lab card | `CourseCatalog.courses.size`, `CourseCatalog.provider()` |
| Rails | Popular courses (178dp cards), Past papers (`PaperCover`), High-yield notes, Saved courses chips | `popularCourses`, `recentPapers`, `recentNotes`, `savedCourses` |
| Footer | `Last attempt 27 Oct` | `QuizProgress.recent.first().submittedAt` |

## Rules baked into it

- **Nothing is invented.** No placeholder percentages, no fake streaks. With no
  attempts the ring reads `0%`, the weekly card says what a quiz would produce,
  and the CTA becomes *Take your first quiz*.
- **The dashboard never writes stats.** `points`, `studyHoursThisWeek` and
  `totalPapersSolved` on the profile are not trusted for display; every figure is
  derived from submitted attempts, exactly as on `ProgressScreen`.
- **`java.time` is not available** (minSdk 24, no desugaring). Weekday bucketing
  and date formatting use `Calendar` + `SimpleDateFormat("yyyy-MM-dd")`, with
  weekday index `(Calendar.DAY_OF_WEEK + 5) % 7` for a Monday-first week.
- **Scroll performance.** The screen is one `LazyColumn` of pre-composed sections;
  rails are `LazyRow`s with `key =` on every item, no `SubcomposeAsyncImage`, no
  per-row infinite animations, and all palettes / catalogue lookups are hoisted
  to top level or `by lazy`. See `docs/PERFORMANCE.md`.
- **Skeleton parity.** `HomeSkeletonLoading` (in `ui/components/ShimmerEffect.kt`)
  mirrors this layout block for block — greeting, 232dp hero, four 86dp tiles,
  the report card and the first rail — so the shimmer resolves instead of jumping.

## Testing

No Compose test targets the dashboard, so the tags are the contract for new ones:

`home_screen_content`, `welcome_card`, `home_streak_pill`, `home_exp_pill`,
`hero_cta`, `home_open_progress`, `home_ai_assistant_card`,
`home_course_collection_card`, `home_quick_papers`, `home_quick_report`,
`home_weekly_report`, `continue_learning_card`, `resume_lesson_button`,
`home_course_<id>`, `home_paper_<id>`.
