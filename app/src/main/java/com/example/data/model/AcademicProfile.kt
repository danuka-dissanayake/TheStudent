package com.example.data.model

import java.util.Calendar
import java.util.Locale

/**
 * One subject a student actually sits for.
 *
 * [code] is the stable join key used by the matching algorithm (the server
 * re-derives the same slug from the label, so an older client and a newer
 * catalog still line up). [bucket] is only set for O/L optional subjects, where
 * the exam requires exactly one subject from each of the three buckets.
 */
data class AcademicSubject(
    val code: String,
    val label: String,
    val bucket: Int? = null
)

/**
 * The structured academic profile.
 *
 * It covers every stage of the Sri Lankan system that a student using this app
 * can be at:
 *
 *  • **JUNIOR** — Grades 6–9, the junior secondary curriculum. No national exam;
 *    the useful signals are the grade and the medium.
 *  • **OL** — Grades 10–11, ending in the G.C.E. Ordinary Level: six compulsory
 *    subjects plus one from each of three optional buckets.
 *  • **AL** — Grades 12–13, the collegiate level: a stream and three subjects,
 *    plus General English and the Common General Test which every candidate sits
 *    but which are not part of the three.
 *  • **UNI** — undergraduate study: university, field of study and year.
 *
 * The free-text `stream_or_degree` on the account can't carry any of this: two
 * Technology students both storing "Technology" tells the matcher nothing about
 * whether they share Science for Technology, and a Sinhala-medium student can't
 * help a Tamil-medium one. Every field is optional so a half-filled profile never
 * blocks the app; [isComplete] is what the UI nudges toward.
 */
data class AcademicProfile(
    /** [AcademicCatalog.LEVEL_JUNIOR], [AcademicCatalog.LEVEL_OL], [AcademicCatalog.LEVEL_AL] or [AcademicCatalog.LEVEL_UNI]; empty = not set. */
    val examLevel: String = "",
    /** A/L stream label (e.g. "Technology"); empty for every other level. */
    val streamCode: String = "",
    /** The year the student sits O/L or A/L; null for Grades 6–9 and for university. */
    val examYear: Int? = null,
    /** Sinhala / Tamil / English. */
    val medium: String = "",
    val subjects: List<AcademicSubject> = emptyList(),
    /**
     * School grade (6–9 for JUNIOR, 10–11 for O/L, 12–13 for A/L) or, for
     * university, the year of study (1–6). One column, because it always means
     * "where in this stage are you".
     */
    val studyYear: Int? = null,
    /** Undergraduate only: the university, from [AcademicCatalog.universities]. */
    val university: String = "",
    /** Undergraduate only: the field of study, from [AcademicCatalog.degreeFields]. */
    val programme: String = ""
) {
    val isJunior: Boolean get() = examLevel == AcademicCatalog.LEVEL_JUNIOR
    val isAl: Boolean get() = examLevel == AcademicCatalog.LEVEL_AL
    val isOl: Boolean get() = examLevel == AcademicCatalog.LEVEL_OL
    val isUni: Boolean get() = examLevel == AcademicCatalog.LEVEL_UNI

    /** "Advanced Level", "University"... — for a heading. */
    val stageLabel: String get() = AcademicCatalog.labelFor(examLevel)

    /** True when the whole step is done and matching can use every signal. */
    val isComplete: Boolean
        get() = when (examLevel) {
            AcademicCatalog.LEVEL_JUNIOR -> studyYear != null && medium.isNotBlank() && subjects.isNotEmpty()
            AcademicCatalog.LEVEL_OL -> examYear != null && medium.isNotBlank() && subjects.size == 3
            AcademicCatalog.LEVEL_AL -> streamCode.isNotBlank() && examYear != null &&
                medium.isNotBlank() && subjects.size == 3
            AcademicCatalog.LEVEL_UNI -> university.isNotBlank() && programme.isNotBlank() && studyYear != null
            else -> false
        }

    /** How far through the academic step this student is (0f…1f). */
    val completion: Float
        get() {
            var filled = 0
            var total = 0
            fun step(done: Boolean) {
                total++
                if (done) filled++
            }
            step(examLevel.isNotBlank())
            when (examLevel) {
                AcademicCatalog.LEVEL_JUNIOR -> {
                    step(studyYear != null)
                    step(medium.isNotBlank())
                    step(subjects.isNotEmpty())
                }
                AcademicCatalog.LEVEL_OL -> {
                    step(examYear != null)
                    step(medium.isNotBlank())
                    step(subjects.size == 3)
                }
                AcademicCatalog.LEVEL_AL -> {
                    step(streamCode.isNotBlank())
                    step(examYear != null)
                    step(medium.isNotBlank())
                    step(subjects.size == 3)
                }
                AcademicCatalog.LEVEL_UNI -> {
                    step(university.isNotBlank())
                    step(programme.isNotBlank())
                    step(studyYear != null)
                }
                else -> Unit
            }
            if (total == 0) return 0f
            return (filled.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

    /** "Physical Science · 2027 · Sinhala" — the one-line summary under the name. */
    fun summary(): String = buildList {
        when {
            isAl && streamCode.isNotBlank() -> add(streamCode)
            isOl -> add("O/L")
            isJunior -> studyYear?.let { add("Grade $it") } ?: add("Grades 6–9")
            isUni -> {
                if (university.isNotBlank()) add(university)
                if (programme.isNotBlank()) add(programme)
                studyYear?.let { add("Year $it") }
            }
        }
        if (!isUni) {
            examYear?.let { add(it.toString()) }
            if (medium.isNotBlank()) add(medium)
        }
    }.joinToString(" · ")

    /** The subject labels, or an empty list. */
    fun subjectLabels(): List<String> = subjects.map { it.label }

    companion object {
        // ── on-device encoding ───────────────────────────────────────────────
        // The academic profile is cached in the single user_profile row so the
        // subjects survive an app restart without a second network call. Room
        // has no list column, so the subjects travel as one string:
        //   "label|code|bucket~label|code|bucket"
        // '|' and '~' never appear in a subject label chosen from the catalog.
        private const val SUBJECT_SEPARATOR = "~"
        private const val FIELD_SEPARATOR = "|"

        fun encodeSubjects(subjects: List<AcademicSubject>): String =
            subjects.joinToString(SUBJECT_SEPARATOR) { subject ->
                listOf(
                    subject.label.replace(FIELD_SEPARATOR, "/").replace(SUBJECT_SEPARATOR, "/"),
                    subject.code.replace(FIELD_SEPARATOR, "/").replace(SUBJECT_SEPARATOR, "/"),
                    subject.bucket?.toString().orEmpty()
                ).joinToString(FIELD_SEPARATOR)
            }

        fun decodeSubjects(encoded: String): List<AcademicSubject> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(SUBJECT_SEPARATOR).mapNotNull { chunk ->
                if (chunk.isBlank()) return@mapNotNull null
                val parts = chunk.split(FIELD_SEPARATOR)
                val label = parts.getOrNull(0)?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                val code = parts.getOrNull(1)?.trim().orEmpty().ifBlank {
                    AcademicCatalog.codeFor(label)
                }
                val bucket = parts.getOrNull(2)?.trim()?.toIntOrNull()?.takeIf { it in 1..3 }
                AcademicSubject(code = code, label = label, bucket = bucket)
            }
        }

        /**
         * Tolerant parser for the server payload. Unknown shapes degrade to an
         * empty profile rather than throwing, because this runs on sign-in.
         */
        fun from(
            examLevel: String?,
            streamCode: String?,
            examYear: Int?,
            medium: String?,
            subjects: List<AcademicSubject>,
            studyYear: Int? = null,
            university: String? = null,
            programme: String? = null
        ): AcademicProfile = AcademicProfile(
            examLevel = (examLevel ?: "").trim().uppercase(Locale.US),
            streamCode = (streamCode ?: "").trim(),
            examYear = examYear?.takeIf { it > 0 },
            medium = (medium ?: "").trim(),
            // A/L is exactly three; O/L is exactly three (one per bucket);
            // Grades 6–9 have no fixed count; university has none at all.
            subjects = subjects.take(3),
            studyYear = studyYear?.takeIf { it in 1..13 },
            university = (university ?: "").trim(),
            programme = (programme ?: "").trim()
        )
    }
}

/**
 * The app's copy of the Sri Lankan curriculum catalog.
 *
 * Deliberately client-side: the syllabus, the O/L bucket list and the university
 * list change faster than a server deploy cycle, and the server only validates
 * the *shape*. Subject and stream names follow the Department of Examinations
 * naming so a student can match what is printed on their admission card.
 */
object AcademicCatalog {

    // ── levels ──────────────────────────────────────────────────────────────
    const val LEVEL_JUNIOR = "JUNIOR"
    const val LEVEL_OL = "OL"
    const val LEVEL_AL = "AL"
    const val LEVEL_UNI = "UNI"

    data class LevelInfo(
        val code: String,
        val label: String,
        val range: String,
        val blurb: String
    )

    val levelInfos: List<LevelInfo> = listOf(
        LevelInfo(
            LEVEL_JUNIOR,
            "Grades 6–9",
            "Junior secondary",
            "Common curriculum, no national exam yet"
        ),
        LevelInfo(
            LEVEL_OL,
            "O/L",
            "Grades 10–11",
            "6 compulsory + 3 optional subjects"
        ),
        LevelInfo(
            LEVEL_AL,
            "A/L",
            "Grades 12–13",
            "A stream and three subjects"
        ),
        LevelInfo(
            LEVEL_UNI,
            "University",
            "Undergraduate",
            "Degree programme and year of study"
        )
    )

    /** Kept for callers that only need code → label. */
    val levels: List<Pair<String, String>> = levelInfos.map { it.code to it.label }

    fun labelFor(code: String): String = when (code) {
        LEVEL_JUNIOR -> "Grades 6–9"
        LEVEL_OL -> "O/L"
        LEVEL_AL -> "A/L"
        LEVEL_UNI -> "University"
        else -> ""
    }

    val mediums: List<String> = listOf("Sinhala", "Tamil", "English")

    // ── Junior secondary: Grades 6–9 ─────────────────────────────────────────
    val juniorGrades: List<Int> = listOf(6, 7, 8, 9)

    /**
     * The junior secondary curriculum every school follows in Grades 6–9: the
     * core subjects plus the practical, aesthetic and second-language strands.
     * Nothing here is optional in the exam sense — there is no national exam —
     * so the picker asks which of them the student wants help with.
     */
    val juniorSubjects: List<String> = listOf(
        "Mathematics",
        "Science",
        "English",
        "Sinhala",
        "Tamil",
        "History",
        "Geography",
        "Life Competencies & Citizenship Education",
        "Information & Communication Technology",
        "Health & Physical Education",
        "Practical & Technical Skills",
        "Art",
        "Music (Oriental / Western / Carnatic)",
        "Dancing",
        "Drama & Theatre",
        "Religion (Buddhism / Hinduism / Islam / Christianity)"
    )

    // ── O/L: Grades 10–11 ────────────────────────────────────────────────────
    val olGrades: List<Int> = listOf(10, 11)

    /** The six compulsory subjects. Identical for everyone, so never stored. */
    val olCompulsorySubjects: List<String> = listOf(
        "Religion (Buddhism / Hinduism / Islam / Christianity)",
        "First Language (Sinhala / Tamil)",
        "English",
        "Mathematics",
        "Science",
        "History"
    )

    /**
     * The three optional buckets, one subject from each. Names follow the
     * Department of Examinations list, including the technology subjects added
     * with the 2018 syllabus revision.
     */
    val olBuckets: List<OlBucket> = listOf(
        OlBucket(
            number = 1,
            title = "Bucket 1 · Humanities & languages",
            hint = "Geography, civics, business or a language",
            subjects = listOf(
                "Geography",
                "Civic Education",
                "Business & Accounting Studies",
                "Entrepreneurship Studies",
                "Second Language (Sinhala / Tamil)",
                "Pali",
                "Sanskrit",
                "French",
                "German",
                "Hindi",
                "Japanese",
                "Arabic",
                "Korean",
                "Chinese",
                "Russian"
            )
        ),
        OlBucket(
            number = 2,
            title = "Bucket 2 · Aesthetic",
            hint = "Art, music, dancing, drama or literature",
            subjects = listOf(
                "Art",
                "Oriental Music",
                "Carnatic Music",
                "Western Music",
                "Dancing (Oriental)",
                "Dancing (Bharatha)",
                "Drama & Theatre",
                "Appreciation of English Literary Texts",
                "Appreciation of Sinhala Literary Texts",
                "Appreciation of Tamil Literary Texts",
                "Appreciation of Arabic Literary Texts"
            )
        ),
        OlBucket(
            number = 3,
            title = "Bucket 3 · Technical & practical",
            hint = "ICT, agriculture, design, home economics or health",
            subjects = listOf(
                "Information & Communication Technology",
                "Communication & Media Studies",
                "Agriculture & Food Technology",
                "Aquatic Bioresources Technology",
                "Arts & Crafts",
                "Home Economics",
                "Health & Physical Education",
                "Design & Construction Technology",
                "Design & Mechanical Technology",
                "Design, Electrical & Electronic Technology",
                "Electronic Writing & Shorthand"
            )
        )
    )

    // ── A/L: Grades 12–13 ────────────────────────────────────────────────────
    val alGrades: List<Int> = listOf(12, 13)

    /** The five A/L streams. "All Streams" is a filter, not a stream. */
    val alStreams: List<String> = SriLankanCurriculumConfig.alStreams
        .filter { it != SriLankanCurriculumConfig.CAT_ALL && it != "All Streams" }

    /**
     * Every subject a candidate may offer in each stream. Combinations are
     * governed by Ministry circulars, so the screen also shows [alTypicalCore]
     * and lets the student confirm their own three.
     */
    val alStreamSubjects: Map<String, List<String>> = mapOf(
        "Physical Science" to listOf(
            "Combined Mathematics",
            "Physics",
            "Chemistry",
            "Higher Mathematics",
            "Information & Communication Technology"
        ),
        "Biological Science" to listOf(
            "Biology",
            "Chemistry",
            "Physics",
            "Agricultural Science"
        ),
        "Commerce" to listOf(
            "Accounting",
            "Business Studies",
            "Economics",
            "Business Statistics",
            "Information & Communication Technology"
        ),
        "Arts" to listOf(
            "Sinhala",
            "Tamil",
            "English",
            "Pali",
            "Sanskrit",
            "Arabic",
            "Hindi",
            "Japanese",
            "Chinese",
            "Korean",
            "French",
            "German",
            "Russian",
            "Malay",
            "History",
            "Geography",
            "Political Science",
            "Economics",
            "Logic & Scientific Method",
            "Home Economics",
            "Communication & Media Studies",
            "Buddhism",
            "Hinduism",
            "Islam",
            "Christianity",
            "Buddhist Civilization",
            "Hindu Civilization",
            "Islamic Civilization",
            "Christian Civilization",
            "Greek & Roman Civilization",
            "Art",
            "Dancing (Indigenous)",
            "Dancing (Bharatha)",
            "Oriental Music",
            "Carnatic Music",
            "Western Music",
            "Drama & Theatre"
        ),
        "Technology" to listOf(
            "Engineering Technology",
            "Bio-systems Technology",
            "Science for Technology",
            "Civil Technology",
            "Mechanical Technology",
            "Electrical, Electronic & Information Technology",
            "Food Technology",
            "Agro Technology",
            "Bio-resource Technology",
            "Information & Communication Technology"
        )
    )

    /** The subjects that stream is normally built on, for the hint line. */
    fun alTypicalCore(stream: String): List<String> = when (stream) {
        "Physical Science" -> listOf("Combined Mathematics", "Physics")
        "Biological Science" -> listOf("Biology", "Chemistry")
        "Commerce" -> listOf("Accounting", "Business Studies", "Economics")
        "Technology" -> listOf("Science for Technology", "Engineering Technology / Bio-systems Technology")
        else -> emptyList()
    }

    /** Sat by every A/L candidate, but not part of the counted three. */
    val alCommonComponents: List<String> = listOf("General English", "Common General Test")

    fun alSubjects(stream: String): List<String> = alStreamSubjects[stream].orEmpty()

    // ── University ───────────────────────────────────────────────────────────
    /**
     * The state universities under the University Grants Commission, plus one
     * honest "other" for private, foreign and higher-diploma students.
     */
    val universities: List<String> = listOf(
        "University of Colombo",
        "University of Peradeniya",
        "University of Sri Jayewardenepura",
        "University of Kelaniya",
        "University of Moratuwa",
        "University of Jaffna",
        "University of Ruhuna",
        "Eastern University, Sri Lanka",
        "South Eastern University of Sri Lanka",
        "Rajarata University of Sri Lanka",
        "Sabaragamuwa University of Sri Lanka",
        "Wayamba University of Sri Lanka",
        "Uva Wellassa University",
        "Open University of Sri Lanka",
        "General Sir John Kotelawala Defence University",
        "Buddhasravaka Bhikshu University",
        "Buddhist and Pali University of Sri Lanka",
        "Other / private or foreign university"
    )

    /** Fields of study, matched against the faculties a university actually has. */
    val degreeFields: List<String> = listOf(
        "Engineering",
        "Medicine & Health Sciences",
        "Science",
        "Management & Commerce",
        "Arts & Social Sciences",
        "Information Technology & Computing",
        "Law",
        "Agriculture",
        "Education",
        "Architecture & Quantity Surveying",
        "Technology",
        "Allied Health Sciences",
        "Other"
    )

    val studyYears: List<Int> = listOf(1, 2, 3, 4, 5, 6)

    /** Current exam year and the realistic window around it. */
    fun examYears(): List<Int> {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        return ((thisYear - 1)..(thisYear + 4)).toList()
    }

    /** Stable slug used as the subject's join key (mirrors the server). */
    fun codeFor(label: String): String = label
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    fun subject(label: String, bucket: Int? = null): AcademicSubject =
        AcademicSubject(code = codeFor(label), label = label, bucket = bucket)
}

/** One O/L optional-subject bucket; one subject may be chosen from each. */
data class OlBucket(
    val number: Int,
    val title: String,
    val hint: String,
    val subjects: List<String>
)
