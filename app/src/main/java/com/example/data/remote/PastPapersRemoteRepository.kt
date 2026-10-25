package com.example.data.remote

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.example.data.repository.SampleEducationalData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class PastPapersRemoteRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("dzd_backend_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PastPapersRemoteRepo"
        const val KEY_BACKEND_URL = "custom_infinityfree_url"
        const val DEFAULT_BACKEND_URL = "https://dzdlearn.infinityfreeapp.com/"
    }

    var backendBaseUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        set(value) {
            val sanitized = if (!value.endsWith("/")) "$value/" else value
            prefs.edit().putString(KEY_BACKEND_URL, sanitized).apply()
            cachedService = null
        }

    private var cachedService: PastPapersApiService? = null

    private fun getApiService(): PastPapersApiService {
        cachedService?.let { return it }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(backendBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val service = retrofit.create(PastPapersApiService::class.java)
        cachedService = service
        return service
    }

    /**
     * Fetch papers dynamically from the InfinityFree MySQL database via PHP API.
     * If the remote call fails (e.g. offline or pending user upload), falls back
     * to rich seed papers matching the database schema so the UI remains fully functional.
     */
    suspend fun fetchPapers(
        category: String? = null,
        grade: String? = null,
        stream: String? = null,
        subject: String? = null,
        year: Int? = null,
        search: String? = null
    ): Pair<List<PastPaper>, Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = getApiService()
            val cleanCategory = if (category == "All" || category == "All Categories") null else category
            val cleanGrade = if (grade == "All" || grade == "All Grades") null else grade
            val cleanStream = if (stream == "All" || stream == "All Streams") null else stream
            val cleanSubject = if (subject == "All" || subject == "All Subjects") null else subject

            val response = service.getPapers(
                category = cleanCategory,
                grade = cleanGrade,
                stream = cleanStream,
                subject = cleanSubject,
                year = year,
                search = if (search.isNullOrBlank()) null else search
            )

            if (response.status == "success" && !response.data.isNullOrEmpty()) {
                val mapped = response.data.map { it.toDomain() }
                Log.d(TAG, "Successfully loaded ${mapped.size} papers from MySQL API at $backendBaseUrl")
                return@withContext Pair(mapped, true) // isLiveDatabase = true
            } else {
                Log.w(TAG, "Empty response from remote database, falling back to local dataset")
                return@withContext Pair(getFallbackPapers(cleanCategory, cleanGrade, cleanStream, cleanSubject, year, search), false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error connecting to InfinityFree MySQL API ($backendBaseUrl): ${e.message}")
            return@withContext Pair(getFallbackPapers(category, grade, stream, subject, year, search), false)
        }
    }

    /**
     * Triggers dynamic PDF download from the pdf_url stored in the MySQL database.
     * Does NOT store the PDF hardcoded in the app; downloads dynamically via DownloadManager.
     */
    fun downloadPaperPdf(paper: PastPaper) {
        val targetUrl = paper.pdfUrl.ifBlank {
            "https://archive.org/download/gce-al-pastpapers/${paper.year}-${paper.category.replace("/", "")}-${paper.subject.replace(" ", "-")}.pdf"
        }

        try {
            val request = DownloadManager.Request(Uri.parse(targetUrl)).apply {
                setTitle("${paper.title}.pdf")
                setDescription("Downloading ${paper.subject} ${paper.year} Examination Paper...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${paper.title.replace(" ", "_")}.pdf")
                setMimeType("application/pdf")
                setAllowedOverMetered(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                manager.enqueue(request)
                Toast.makeText(context, "Downloading PDF from server...", Toast.LENGTH_SHORT).show()
            } else {
                openPdfInBrowser(targetUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed: ${e.message}, opening in external viewer", e)
            openPdfInBrowser(targetUrl)
        }
    }

    /**
     * Triggers dynamic download of the Marking Scheme PDF from server.
     */
    fun downloadMarkingSchemePdf(paper: PastPaper) {
        val schemeUrl = paper.markingSchemeUrl ?: return
        try {
            val request = DownloadManager.Request(Uri.parse(schemeUrl)).apply {
                setTitle("${paper.title}_Marking_Scheme.pdf")
                setDescription("Downloading Marking Scheme...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${paper.title.replace(" ", "_")}_Scheme.pdf")
                setMimeType("application/pdf")
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            manager?.enqueue(request)
            Toast.makeText(context, "Downloading Marking Scheme from server...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            openPdfInBrowser(schemeUrl)
        }
    }

    fun openPdfInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open PDF link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Comprehensive seed papers covering Grade 6 through Grade 13 (A/L & O/L)
     * matching the exact MySQL database schema for seamless testing.
     */
    fun getFallbackPapers(
        categoryFilter: String? = null,
        gradeFilter: String? = null,
        streamFilter: String? = null,
        subjectFilter: String? = null,
        yearFilter: Int? = null,
        searchQuery: String? = null
    ): List<PastPaper> {
        val baseList = DefaultPastPapersSeed.allPapers
        return baseList.filter { paper ->
            val matchesCategory = categoryFilter.isNullOrBlank() || categoryFilter == "All" || categoryFilter == "All Categories" ||
                    paper.category.equals(categoryFilter, ignoreCase = true) ||
                    paper.grade.contains(categoryFilter, ignoreCase = true)

            val matchesGrade = gradeFilter.isNullOrBlank() || gradeFilter == "All" || gradeFilter == "All Grades" ||
                    paper.grade.equals(gradeFilter, ignoreCase = true)

            val matchesStream = streamFilter.isNullOrBlank() || streamFilter == "All" || streamFilter == "All Streams" ||
                    paper.stream.equals(streamFilter, ignoreCase = true)

            val matchesSubject = subjectFilter.isNullOrBlank() || subjectFilter == "All" || subjectFilter == "All Subjects" ||
                    paper.subject.contains(subjectFilter, ignoreCase = true)

            val matchesYear = yearFilter == null || paper.year == yearFilter

            val matchesSearch = searchQuery.isNullOrBlank() ||
                    paper.title.contains(searchQuery, ignoreCase = true) ||
                    paper.subject.contains(searchQuery, ignoreCase = true) ||
                    paper.grade.contains(searchQuery, ignoreCase = true) ||
                    (paper.stream?.contains(searchQuery, ignoreCase = true) == true) ||
                    paper.year.toString().contains(searchQuery) ||
                    paper.description.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesGrade && matchesStream && matchesSubject && matchesYear && matchesSearch
        }
    }
}
