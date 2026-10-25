package com.example.data.remote

import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class PastPaperApiDto(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "grade") val grade: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "stream") val stream: String? = null,
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "year") val year: Any? = null,
    @Json(name = "exam_type") val examType: String? = null,
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
    @Json(name = "marking_scheme_url") val markingSchemeUrl: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "file_size") val fileSize: String? = null,
    @Json(name = "pages") val pages: Any? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "status") val status: String? = null
) {
    fun toDomain(): PastPaper {
        val parsedYear = when (year) {
            is Number -> year.toInt()
            is String -> year.toIntOrNull() ?: 2024
            else -> 2024
        }
        val parsedPages = when (pages) {
            is Number -> pages.toInt()
            is String -> pages.toIntOrNull() ?: 16
            else -> 16
        }
        val stringId = id?.toString() ?: "paper-$parsedYear-${title.hashCode()}"
        val resolvedCategory = category?.trim() ?: run {
            if (grade?.contains("13") == true || grade?.contains("12") == true) "A/L"
            else if (grade?.contains("11") == true || grade?.contains("10") == true) "O/L"
            else grade ?: "A/L"
        }
        val resolvedGrade = grade?.trim() ?: run {
            if (resolvedCategory == "A/L") "Grade 13"
            else if (resolvedCategory == "O/L") "Grade 11"
            else resolvedCategory
        }

        val resolvedStream = stream?.trim()?.takeIf { it.isNotBlank() } ?: run {
            if (resolvedCategory == "A/L" || resolvedGrade == "Grade 12" || resolvedGrade == "Grade 13") {
                val subjLower = (subject ?: "").lowercase()
                when {
                    subjLower.contains("math") -> "Physical Science"
                    subjLower.contains("bio") -> "Biological Science"
                    subjLower.contains("physics") || subjLower.contains("chem") -> "Physical Science"
                    subjLower.contains("account") || subjLower.contains("business") || subjLower.contains("econ") -> "Commerce"
                    subjLower.contains("tech") || subjLower.contains("sft") || subjLower.contains("ict") -> "Technology"
                    else -> "Arts"
                }
            } else null
        }

        return PastPaper(
            id = stringId,
            title = title ?: "$resolvedCategory ${subject ?: "Paper"} $parsedYear",
            examination = examType ?: "G.C.E. Examination",
            subject = subject ?: "General",
            year = parsedYear,
            medium = medium ?: "English",
            level = when {
                resolvedCategory.contains("A/L") || resolvedCategory.contains("O/L") || resolvedCategory.startsWith("Grade") -> EducationLevel.SCHOOL
                else -> EducationLevel.ALL
            },
            grade = resolvedGrade,
            category = resolvedCategory,
            stream = resolvedStream,
            examType = examType ?: "G.C.E. Examination",
            pdfUrl = pdfUrl ?: "",
            markingSchemeUrl = markingSchemeUrl?.takeIf { it.isNotBlank() },
            description = description ?: "",
            createdAt = createdAt,
            status = status ?: "published",
            pages = parsedPages,
            fileSize = fileSize ?: "3.5 MB",
            hasMarkingScheme = !markingSchemeUrl.isNullOrBlank(),
            syllabusSummary = description?.ifBlank { "Examination paper and marking guide." } ?: "Examination paper and marking guide.",
            sampleQuestions = emptyList(),
            markingSchemeNotes = if (!markingSchemeUrl.isNullOrBlank()) listOf("Official marking scheme included") else emptyList()
        )
    }
}

@JsonClass(generateAdapter = true)
data class PastPapersApiResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "available_categories") val availableCategories: List<String>? = null,
    @Json(name = "available_streams") val availableStreams: List<String>? = null,
    @Json(name = "available_subjects") val availableSubjects: List<String>? = null,
    @Json(name = "available_years") val availableYears: List<Any>? = null,
    @Json(name = "data") val data: List<PastPaperApiDto>? = null,
    @Json(name = "message") val message: String? = null
)

interface PastPapersApiService {
    @GET("api/get_papers.php")
    suspend fun getPapers(
        @Query("category") category: String? = null,
        @Query("grade") grade: String? = null,
        @Query("stream") stream: String? = null,
        @Query("subject") subject: String? = null,
        @Query("year") year: Int? = null,
        @Query("medium") medium: String? = null,
        @Query("search") search: String? = null,
        @Query("status") status: String? = "published"
    ): PastPapersApiResponse
}
