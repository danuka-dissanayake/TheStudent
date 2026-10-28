# The dashboard (Home)

`ui/screens/HomeScreen.kt` — the landing screen after sign-in. It was rebuilt
from scratch in v2.0.3: the old "welcome card + stat row + three rails" layout is
gone, replaced by a report-first dashboard.

The reference renders are `designs/22-dashboard-top-new.png` (bar, greeting,
report card, quick actions), `designs/24-rails-new.png` (this week, courses rail
with dots, paper codes, quizzes rail) and `designs/23-sidebar-new.png` (drawer).

## Layout, top to bottom

| Block | What it shows | Where the data comes from |
|---|---|---|
| Greeting | 17sp emerald greeting, the student's **full name** at 36sp, amber streak pill, then `Tuesday, 28 Oct · A/L · Physical Science` | `UserProfile.name / academicLevel / streamOrDegree`, `StudyMetrics.today()`, `max(summary.streakDays, userProfile.streakDays)` |
| Report card | white card: 104dp emerald accuracy ring, three KPI rows (`4 of 6` passed · `2h 15m` studied · `240 XP`), a three-segment accuracy bar with legend, the emerald CTA and `Full report` | `QuizProgressSummary.averagePercent / quizzesPassed / quizzesPublished / durationSeconds / expFromQuizzes`; segments from `subjects[].accuracy` |
| Quick actions | Ask AI · Courses · Papers · **Quizzes** | `onOpenAiAssistant`, `onOpenCourseCollection`, `onNavigateTab(PAPERS)`, `onOpenQuizzes` |
| This week | up to four subject accuracy bars, seven weekday bars on pale tracks, `best` / `attempts` / `time` chips | `QuizProgress.subjects[].accuracy`, `QuizProgress.recent[].submittedAt`, `summary.bestPercent / attempts` |
| Practise next | subject **code** chip, quiz title, accuracy bar, attempts left, Practise button | `QuizProgress.practice` (server picks it); the button calls `onContinueLearning` |
| Promos | dark `N free courses` card with three provider logos · Language lab card | `CourseCatalog.courses.size`, `CourseCatalog.provider()` |
| Popular courses | 178dp cards in a self-paging `LazyRow` (one card every 4.2s, frozen while the student scrolls) with page dots; the cover prints the **category** in white with the level chip beside it | `popularCourses`, `Course.category`, `Course.level` |
| Past papers | 112dp `PaperCover` thumbnails — the coloured band carries the **subject name**, the body the **short code** (`CM`, `SFT`, `ICT`, `AGRI`) — then the paper title at 11.5sp and `CODE · Examination` | `recentPapers`, `SubjectCodes.of()` |
| Quizzes | 214×184dp **tickets**: a coloured spine down the left, a subject-code / grade stub line, a 15sp two-line title, a `12 questions · 20m · +20 XP` meta line and one full-width action pill at the foot (`3 attempts left` / `Best 82% · retake` / `All attempts used`) | `quizzes` (`QuizSummary`), `onQuizClick` |
| Notes / saved | compact note rows with the subject code, saved-course chips | `recentNotes`, `savedCourses` |
| Footer | `Last attempt 27 Oct` | `QuizProgress.recent.first().submittedAt` |

## Subject short codes

`data/model/SubjectCodes.kt` maps a subject name to the code printed on its
papers — the technology trio **SFT / ET / BST**, plus **ICT**, **AGRI**, **CM**,
**PHY**, **CHEM**, **BIO**, **MATH**, **SCI**, **ACC**, **BS**, **ECON**, **HIST**
and the rest of the school curriculum. The lookup canonicalises the name
(`&` → `and`, punctuation stripped), retries with trailing qualifiers dropped
(`"Physics 2023"` → `PHY`), and otherwise derives initials while skipping words
like *for / and / of*. It never returns a blank, so an admin can upload a paper
under any subject name and still get a readable cover.

## Rules baked into it

- **Nothing is invented.** No placeholder percentages, no fake streaks. With no
  attempts the ring reads `0%`, the weekly card says what a quiz would produce,
  and the CTA becomes *Take your first quiz*.
- **The dashboard never writes stats.** `points`, `studyHoursThisWeek` and
  `totalPapersSolved` on the profile are not trusted for display; every figure is
  derived from submitted attempts, exactly as on `ProgressScreen`.
- **`java.time` is not available** (minSdk 24, no desugaring). Weekday bucketing
  and date formatting go through `data/model/StudyMetrics.kt`
  (`Calendar` + `SimpleDateFormat("yyyy-MM-dd")`, weekday index
  `(Calendar.DAY_OF_WEEK + 5) % 7` for a Monday-first week), which the sidebar
  shares so the two never drift apart.
- **Scroll performance.** The screen is one `LazyColumn` of pre-composed sections;
  rails are `LazyRow`s with `key =` on every item, no `SubcomposeAsyncImage`, no
  per-row infinite animations, and all palettes / catalogue lookups are hoisted
  to top level or `by lazy`. See `docs/PERFORMANCE.md`.
- **Text scale.** Body copy never drops below 10sp and headings use one scale
  (`HT`): 34sp greeting, 16sp section, 14sp card title, 13sp body, 12sp meta,
  11sp micro. Cover chips are the only 9.5sp labels, and every card row uses
  weights rather than fixed widths so nothing clips on a narrow screen.
- **Skeleton parity.** `HomeSkeletonLoading` (in `ui/components/ShimmerEffect.kt`)
  mirrors this layout block for block — greeting, 232dp hero, four 86dp tiles,
  the report card and the first rail — so the shimmer resolves instead of jumping.

## Testing

No Compose test targets the dashboard, so the tags are the contract for new ones:

`home_screen_content`, `welcome_card`, `home_streak_pill`, `home_exp_pill`,
`home_report_card`, `hero_cta`, `home_open_progress`, `home_ai_assistant_card`,
`home_course_collection_card`, `home_quick_papers`, `home_quick_quizzes`,
`home_weekly_report`, `continue_learning_card`, `resume_lesson_button`,
`home_course_<id>`, `home_paper_<id>`, `home_quiz_<id>`.
