package com.example.data.model

object GradeCurriculum {

    val allGrades = listOf(
        "Grade 6",
        "Grade 7",
        "Grade 8",
        "Grade 9",
        "Grade 10",
        "Grade 11 – O/L",
        "Grade 12 – A/L",
        "Grade 13 – A/L"
    )

    val mediumOptions = listOf("All Mediums", "Sinhala", "English", "Tamil")

    val examTypeOptions = listOf("All Types", "O/L", "A/L", "School Term Test", "Model Paper", "Provincial Exam")

    val yearOptions = listOf("All Years", "2025", "2024", "2023", "2022", "2021", "2020")

    fun getDefaultSubjectsForGrade(grade: String): List<String> {
        return when {
            grade.contains("12") || grade.contains("13") -> listOf(
                "Physics",
                "Chemistry",
                "Combined Mathematics",
                "Biology",
                "ICT",
                "Accounting",
                "Economics",
                "Business Studies",
                "Agricultural Science",
                "Engineering Technology"
            )
            grade.contains("11") -> listOf(
                "Mathematics",
                "Science",
                "English",
                "Sinhala",
                "History",
                "ICT",
                "Religion",
                "Commerce",
                "Geography",
                "Health Science"
            )
            grade.contains("10") -> listOf(
                "Mathematics",
                "Science",
                "English",
                "Sinhala",
                "History",
                "ICT",
                "Commerce",
                "Geography",
                "Health Science"
            )
            else -> listOf(
                "Mathematics",
                "Science",
                "English",
                "Sinhala",
                "History",
                "Geography",
                "ICT",
                "Health & Physical Education"
            )
        }
    }
}
