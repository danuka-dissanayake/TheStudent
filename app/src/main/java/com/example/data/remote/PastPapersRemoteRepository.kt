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

    private val prefs = context.getSharedPreferences("thestudent_backend_prefs", Context.MODE_PRIVATE)
    private val sessionPrefs by lazy { com.example.data.local.TheStudentPreferences(context) }

    /** Bearer header for admin-only write endpoints, or null when signed out. */
    private fun adminAuthorizationHeader(): String? {
        val token = sessionPrefs.sessionToken
        return if (token.isNullOrBlank()) null else "Bearer $token"
    }

    companion object {
        private const val TAG = "PastPapersRemoteRepo"

        /** RFC 1918 private ranges — the only addresses allowed to use cleartext HTTP. */
        private val PRIVATE_IPV4 = Regex(
            "^(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}" +
                "|192\\.168\\.\\d{1,3}\\.\\d{1,3}" +
                "|172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3})$"
        )
        const val KEY_BACKEND_URL = "custom_database_api_url"
        const val DEFAULT_BACKEND_URL = "https://app.quizontal.site/"
    }

    var backendBaseUrl: String
        get() {
            val stored = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
            if (stored == "https://api.thestudent.lk/") return DEFAULT_BACKEND_URL
            return enforceTransportSecurity(stored)
        }
        set(value) {
            var sanitized = value.trim()
            if (sanitized.contains(".php")) {
                val lastSlash = sanitized.lastIndexOf('/')
                if (lastSlash != -1) {
                    sanitized = sanitized.substring(0, lastSlash + 1)
                }
            }
            if (!sanitized.endsWith("/")) {
                sanitized = "$sanitized/"
            }
            sanitized = enforceTransportSecurity(sanitized)
            prefs.edit().putString(KEY_BACKEND_URL, sanitized).apply()
            cachedService = null
        }

    /**
     * Session tokens and passwords must never travel in clear text.
     *
     * Any plain `http://` URL is upgraded to `https://` unless it points at a
     * local development host (emulator loopback / private LAN range), which is
     * also the only cleartext allowed by res/xml/network_security_config.xml.
     */
    private fun enforceTransportSecurity(url: String): String {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true)) return trimmed

        val host = runCatching {
            java.net.URI(trimmed).host?.lowercase()
        }.getOrNull()

        val isLocalDevHost = host != null && (
            host == "localhost" ||
                host == "127.0.0.1" ||
                host == "10.0.2.2" ||            // Android emulator loopback alias
                host == "::1" ||
                host.endsWith(".local") ||
                PRIVATE_IPV4.matches(host)
            )

        if (isLocalDevHost) return trimmed
        Log.w(TAG, "Refusing cleartext backend URL; upgrading to HTTPS.")
        return "https://" + trimmed.substring("http://".length)
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
                // An empty catalog is a real answer: either the library has
                // nothing yet or the filters exclude everything. Never invent
                // papers to fill the screen.
                Log.w(TAG, "Empty response from remote database")
                return@withContext Pair(emptyList(), false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error connecting to the papers API ($backendBaseUrl): ${e.message}")
            return@withContext Pair(emptyList(), false)
        }
    }

    /**
     * Test connection to the DirectAdmin MySQL API and return verification status.
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val service = getApiService()
            val response = service.getPapers()
            if (response.status == "success") {
                Pair(true, "Connection Verified Successfully! Connected to MySQL API at $backendBaseUrl (${response.data?.size ?: 0} papers loaded from DirectAdmin database).")
            } else {
                Pair(false, "Connected to server, but API response status was '${response.status}'. Please check your PHP scripts in public_html/api/.")
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: ""
            if (msg.contains("Unable to resolve host") || msg.contains("No address associated")) {
                Pair(false, "DNS Resolution Failed: '$backendBaseUrl' is a mock or unregistered domain name. Android cannot resolve it on the public internet. If testing locally, use http://10.0.2.2/api/ or your local server IP. Otherwise, the app will use its built-in offline demo database.")
            } else {
                Pair(false, "Connection Failed to $backendBaseUrl: $msg")
            }
        }
    }

    /**
     * Upload paper details to the DirectAdmin / MySQL server API.
     */
    suspend fun uploadPaperRemote(paper: PastPaper): Boolean = withContext(Dispatchers.IO) {
        val authorization = adminAuthorizationHeader() ?: run {
            Log.w(TAG, "Upload blocked: an administrator session is required")
            return@withContext false
        }
        try {
            val service = getApiService()
            val req = PaperUploadRequest(
                title = paper.title,
                year = paper.year,
                duration = "3 Hours",
                pages = paper.pages,
                medium = paper.medium,
                hasMarkingScheme = paper.hasMarkingScheme,
                thumbnailUrl = paper.thumbnailUrl,
                category = paper.category,
                grade = paper.grade,
                stream = paper.stream,
                subject = paper.subject,
                pdfUrl = paper.pdfUrl,
                markingSchemeUrl = paper.markingSchemeUrl,
                description = paper.description
            )
            val res = service.uploadPaper(authorization, req)
            res.status == "success"
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading paper to remote API ($backendBaseUrl): ${e.message}")
            false
        }
    }

    /**
     * Delete paper from the remote server database.
     */
    suspend fun deletePaperRemote(paperId: String): Boolean = withContext(Dispatchers.IO) {
        val authorization = adminAuthorizationHeader() ?: run {
            Log.w(TAG, "Delete blocked: an administrator session is required")
            return@withContext false
        }
        try {
            val service = getApiService()
            val res = service.deletePaper(authorization, PaperDeleteRequest(paperId))
            res.status == "success"
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting paper from remote API: ${e.message}")
            false
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

}
