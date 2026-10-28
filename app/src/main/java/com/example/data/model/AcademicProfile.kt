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
 * The structured academic profile: what a student is studying, when, in which
 * medium and with which three subjects.
 *
 * This exists because `stream_or_degree` is free text — two Technology students
 * both storing "Technology" tells the matcher nothing about whether they share
 * Science for Technology, and a Sinhala-medium student can't help a Tamil-medium
 * one. Every field here is optional so a half-filled profile never blocks the
 * app; [isComplete] is what the UI nudges toward.
 */
data class AcademicProfile(
    /** [AcademicCatalog.LEVEL_AL] or [AcademicCatalog.LEVEL_OL]; empty = not set. */
    val examLevel: String = "",
    /** A/L stream label (e.g. "Technology"); empty for O/L. */
    val streamCode: String = "",
    val examYear: Int? = null,
    /** Sinhala / Tamil / English. */
    val medium: String = "",
    val subjects: List<AcademicSubject> = emptyList()
) {
    val isAl: Boolean get() = examLevel == AcademicCatalog.LEVEL_AL
    val isOl: Boolean get() = examLevel == AcademicCatalog.LEVEL_OL

    /** True when the whole step is done and matching can use every signal. */
    val isComplete: Boolean
        get() = examLevel.isNotBlank() &&
            examYear != null &&
            medium.isNotBlank() &&
            subjects.size == 3 &&
            (!isAl || streamCode.isNotBlank())

    /** How far through the academic step this student is (0f…1f). */
    val completion: Float
        get() {
            var filled = 0
            if (examLevel.isNotBlank()) filled++
            if (examYear != null) filled++
            if (medium.isNotBlank()) filled++
            if (!isAl || streamCode.isNotBlank()) filled++ else if (isOl) filled++
            filled += subjects.size.coerceAtMost(3)
            return (filled.toFloat() / 7f).coerceIn(0f, 1f)
        }

    /** "Technology · 2026 · Sinhala" — the one-line summary under the name. */
    fun summary(): String = buildList {
        when {
            isAl && streamCode.isNotBlank() -> add(streamCode)
            isOl -> add("O/L")
        }
        examYear?.let { add(it.toString()) }
        if (medium.isNotBlank()) add(medium)
    }.joinToString(" · ")

    /** The three subject labels, or an empty list. */
    fun subjectLabels(): List<String> = subjects.map { it.label }

    companion object {
        /**
         * Tolerant parser for the server payload. Unknown shapes degrade to an
         * empty profile rather than throwing, because this runs on sign-in.
         */
        fun from(
            examLevel: String?,
            streamCode: String?,
            examYear: Int?,
            medium: String?,
            subjects: List<AcademicSubject>
        ): AcademicProfile = AcademicProfile(
            examLevel = (examLevel ?: "").trim().uppercase(Locale.US),
            streamCode = (streamCode ?: "").trim(),
            examYear = examYear?.takeIf { it > 0 },
            medium = (medium ?: "").trim(),
            subjects = subjects.take(3)
        )
    }
}

/**
 * The app's copy of the Sri Lankan subject catalog.
 *
 * Deliberately client-side: the syllabus and the O/L bucket list change faster
 * than a server deploy cycle, and the server only validates the *shape* (three
 * distinct subjects, one per bucket). It reuses [SriLankanCurriculumConfig] so
 * the picker here can never disagree with the past-papers filters.
 */
object AcademicCatalog {

    const val LEVEL_AL = "AL"
    const val LEVEL_OL = "OL"

    val levels: List<Pair<String, String>> = listOf(
        LEVEL_AL to "A/L",
        LEVEL_OL to "O/L"
    )

    val mediums: List<String> = listOf("Sinhala", "Tamil", "English")

    /** The five A/L streams. "All Streams" is a filter, not a stream. */
    val alStreams: List<String> = SriLankanCurriculumConfig.alStreams
        .filter { it != SriLankanCurriculumConfig.CAT_ALL && it != "All Streams" }

    fun alSubjects(stream: String): List<String> =
        SriLankanCurriculumConfig.alStreamSubjects[stream].orEmpty()

    /** An O/L optional-subject bucket; one subject may be chosen from each. */
    data class OlBucket(
        val number: Int,
        val title: String,
        val subjects: List<String>
    )

    /**
     * The three O/L optional buckets. The six compulsory subjects (first
     * language, English, Mathematics, Science, Religion, History) are assumed
     * and not stored — they are identical for everyone, so they carry no
     * matching signal.
     */
    val olBuckets: List<OlBucket> = listOf(
        OlBucket(
            number = 1,
            title = "Bucket 1",
            subjects = listOf(
                "Geography",
                "Civic Education",
                "Business & Accounting Studies"
            )
        ),
        OlBucket(
            number = 2,
            title = "Bucket 2",
            subjects = listOf(
                "Art",
                "Eastern Music",
                "Western Music",
                "Dancing",
                "Drama & Theatre",
                "English Literature",
                "Sinhala Literature"
            )
        ),
        OlBucket(
            number = 3,
            title = "Bucket 3",
            subjects = listOf(
                "Information & Communication Technology",
                "Agriculture & Food Technology",
                "Design & Construction Technology",
                "Home Economics",
                "Health & Physical Education"
            )
        )
    )

    /** O/L compulsory subjects, shown (read-only) so students know what is assumed. */
    val olCompulsorySubjects: List<String> = listOf(
        "First Language (Sinhala / Tamil)",
        "English",
        "Mathematics",
        "Science",
        "Religion",
        "History"
    )

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
