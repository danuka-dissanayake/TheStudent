package com.example.data.model

/**
 * Sri Lankan National Curriculum Configuration
 *
 * Configurable curriculum structure for Grade 5 to Grade 13:
 * - Scholarship & Junior Secondary: Grade 5, Grade 6, Grade 7, Grade 8, Grade 9
 * - Ordinary Level (O/L): Grade 10, Grade 11
 * - Advanced Level (A/L): Grade 12, Grade 13 with Streams:
 *     Physical Science, Biological Science, Commerce, Arts, Technology
 */
object SriLankanCurriculumConfig {

    const val CAT_ALL = "All Categories"
    const val CAT_JUNIOR = "Scholarship & Junior"
    const val CAT_OL = "G.C.E. O/L"
    const val CAT_AL = "G.C.E. A/L"
    const val CAT_PROVINCIAL = "Provincial & Term Test"
    const val CAT_PROTOTYPE = "Prototype & Model Papers"

    val educationCategories = listOf(
        "All Categories",
        "G.C.E. A/L",
        "G.C.E. O/L",
        "Provincial & Term Test",
        "Prototype & Model Papers",
        "Scholarship & Junior",
        "National Model Exams",
        "Target & Revision Papers"
    )

    val allGrades = listOf(
        "All Grades",
        "Grade 13", "Grade 12", "Grade 11", "Grade 10",
        "Grade 9", "Grade 8", "Grade 7", "Grade 6", "Grade 5"
    )

    // A/L Streams and configurable subjects
    val alStreams = listOf(
        "All Streams",
        "Physical Science",
        "Biological Science",
        "Commerce",
        "Arts",
        "Technology"
    )

    // Configurable stream subjects mapping
    val alStreamSubjects: Map<String, List<String>> = mapOf(
        "Physical Science" to listOf(
            "Combined Mathematics",
            "Physics",
            "Chemistry",
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
        "Technology" to listOf(
            "Engineering Technology",
            "Science for Technology",
            "Biosystems Technology",
            "Information & Communication Technology"
        ),
        "Arts" to listOf(
            "Sinhala",
            "Tamil",
            "English Literature",
            "Political Science",
            "Geography",
            "History",
            "Logic & Scientific Method",
            "Buddhist Civilization",
            "Media & Communication Studies",
            "Economics"
        )
    )

    // Configurable O/L Subjects (Grades 10 and 11)
    val olSubjects: List<String> = listOf(
        "Mathematics",
        "Science",
        "English",
        "Sinhala",
        "Tamil",
        "History",
        "Information & Communication Technology",
        "Religion (Buddhism / Christianity / Islam / Hinduism)",
        "Business & Accounting Studies",
        "Geography",
        "Civic Education",
        "Health & Physical Education",
        "Eastern Music",
        "Western Music",
        "Art",
        "Dancing",
        "Drama & Theatre",
        "English Literature",
        "Sinhala Literature",
        "Agriculture & Food Technology",
        "Design & Construction Technology",
        "Home Economics"
    )

    // Configurable Junior Secondary Subjects (Grades 5 to 9)
    val juniorSecondarySubjects: List<String> = listOf(
        "Mathematics",
        "Science",
        "English",
        "Sinhala",
        "Tamil",
        "History",
        "Geography",
        "Information & Communication Technology",
        "Health & Physical Education",
        "Civic Education",
        "Religion (Buddhism / Christianity / Islam / Hinduism)",
        "Practical & Technical Skills",
        "Art",
        "Music",
        "Dancing"
    )

    fun isAlGrade(grade: String): Boolean = grade == "Grade 12" || grade == "Grade 13"

    fun isOlGrade(grade: String): Boolean = grade == "Grade 10" || grade == "Grade 11"

    fun isJuniorGrade(grade: String): Boolean = grade in listOf("Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9")

    fun getCategoryForGrade(grade: String): String {
        return when {
            isAlGrade(grade) -> CAT_AL
            isOlGrade(grade) -> CAT_OL
            isJuniorGrade(grade) -> CAT_JUNIOR
            else -> CAT_ALL
        }
    }

    /**
     * Resolves default subject list for a grade & optional stream,
     * merging with dynamic subjects found in the database.
     */
    fun getSubjectsForSelection(
        grade: String,
        stream: String,
        databaseSubjects: List<String> = emptyList()
    ): List<String> {
        val baseList = when {
            isAlGrade(grade) -> {
                if (stream != "All Streams" && alStreamSubjects.containsKey(stream)) {
                    alStreamSubjects[stream] ?: emptyList()
                } else {
                    alStreamSubjects.values.flatten().distinct()
                }
            }
            isOlGrade(grade) -> olSubjects
            isJuniorGrade(grade) -> juniorSecondarySubjects
            else -> (olSubjects + alStreamSubjects.values.flatten() + juniorSecondarySubjects).distinct()
        }

        // Dynamically find any subjects present in db papers matching this selection
        val combined = (listOf("All Subjects") + baseList + databaseSubjects)
            .distinct()
            .filter { it.isNotBlank() }

        return combined
    }
}
