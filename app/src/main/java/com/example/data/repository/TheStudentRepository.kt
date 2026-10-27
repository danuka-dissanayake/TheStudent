package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TheStudentRepository(private val dao: TheStudentDao) {

    // Saved papers from Room
    val savedPapers: Flow<List<SavedPaperEntity>> = dao.getAllSavedPapers().flowOn(Dispatchers.IO)
    val savedCourses: Flow<List<SavedCourseEntity>> = dao.getAllSavedCourses().flowOn(Dispatchers.IO)
    val recentActivities: Flow<List<LearningActivityEntity>> = dao.getRecentActivities().flowOn(Dispatchers.IO)
    val userProfileFlow: Flow<UserProfileEntity?> = dao.getUserProfile().flowOn(Dispatchers.IO)

    // Check if saved
    fun isPaperSaved(paperId: String): Flow<Boolean> = dao.isPaperSaved(paperId).flowOn(Dispatchers.IO)
    fun isCourseSaved(courseId: String): Flow<Boolean> = dao.isCourseSaved(courseId).flowOn(Dispatchers.IO)

    suspend fun toggleSavePaper(paper: PastPaper) = withContext(Dispatchers.IO) {
        val entity = SavedPaperEntity(
            id = paper.id,
            title = paper.title,
            examination = paper.examination,
            subject = paper.subject,
            year = paper.year,
            medium = paper.medium,
            fileSize = paper.fileSize,
            isDownloaded = paper.isDownloaded
        )
        // If already exists, delete, else insert
        dao.insertSavedPaper(entity)
        dao.insertActivity(
            LearningActivityEntity(
                title = paper.title,
                category = "Past Paper",
                description = "Saved ${paper.examination} ${paper.year} to offline collection",
                scoreOrDuration = paper.fileSize
            )
        )
    }

    suspend fun removeSavedPaper(paperId: String) = withContext(Dispatchers.IO) {
        dao.deleteSavedPaper(paperId)
    }

    suspend fun markPaperDownloaded(paperId: String) = withContext(Dispatchers.IO) {
        val paper = SampleEducationalData.pastPapers.find { it.id == paperId }
        if (paper != null) {
            val entity = SavedPaperEntity(
                id = paper.id,
                title = paper.title,
                examination = paper.examination,
                subject = paper.subject,
                year = paper.year,
                medium = paper.medium,
                fileSize = paper.fileSize,
                isDownloaded = true
            )
            dao.insertSavedPaper(entity)
            dao.insertActivity(
                LearningActivityEntity(
                    title = "Downloaded: ${paper.title}",
                    category = "Download",
                    description = "Offline PDF & Marking Scheme cached successfully",
                    scoreOrDuration = paper.fileSize
                )
            )
        }
    }

    suspend fun toggleSaveCourse(course: Course) = withContext(Dispatchers.IO) {
        val entity = SavedCourseEntity(
            id = course.id,
            title = course.title,
            instructor = course.instructor,
            institution = course.institution,
            level = course.level.displayName,
            progressPercent = if (course.totalLessons > 0) (course.completedLessons * 100 / course.totalLessons) else 0,
            totalLessons = course.totalLessons,
            completedLessons = course.completedLessons
        )
        dao.insertSavedCourse(entity)
        dao.insertActivity(
            LearningActivityEntity(
                title = "Enrolled in ${course.title}",
                category = "Course",
                description = "Started syllabus tracking with ${course.instructor}",
                scoreOrDuration = "${course.durationHours} hrs"
            )
        )
    }

    suspend fun removeSavedCourse(courseId: String) = withContext(Dispatchers.IO) {
        dao.deleteSavedCourse(courseId)
    }

    suspend fun completeLesson(courseId: String, lessonId: String, lessonTitle: String) = withContext(Dispatchers.IO) {
        dao.insertActivity(
            LearningActivityEntity(
                title = "Lesson Completed: $lessonTitle",
                category = "Study Note",
                description = "Completed topic revision & practice quiz",
                scoreOrDuration = "+25 pts"
            )
        )
    }

    suspend fun recordAiSession(topic: String, questionsCount: Int) = withContext(Dispatchers.IO) {
        dao.insertActivity(
            LearningActivityEntity(
                title = "AI Tutor: $topic",
                category = "AI Tutor",
                description = "Generated explanations & $questionsCount practice questions",
                scoreOrDuration = "15 min"
            )
        )
    }

    suspend fun recordLanguagePractice(mode: String, score: String) = withContext(Dispatchers.IO) {
        dao.insertActivity(
            LearningActivityEntity(
                title = "Language Lab: $mode",
                category = "Language",
                description = "Completed AI pronunciation & conversational exercise",
                scoreOrDuration = score
            )
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        dao.saveUserProfile(
            UserProfileEntity(
                name = profile.name,
                email = profile.email,
                academicLevel = profile.academicLevel,
                streamOrDegree = profile.streamOrDegree,
                institution = profile.institution,
                targetExam = profile.targetExam,
                streakDays = profile.streakDays,
                studyHoursThisWeek = profile.studyHoursThisWeek,
                totalPapersSolved = profile.totalPapersSolved,
                points = profile.points,
                isLoggedIn = profile.isLoggedIn,
                phone = profile.phone,
                avatarUrl = profile.avatarUrl,
                profileCompleted = profile.profileCompleted,
                hasPassword = profile.hasPassword
            )
        )
    }

    suspend fun getUserProfileSync(): UserProfileEntity? = withContext(Dispatchers.IO) {
        dao.getUserProfileSync()
    }

    suspend fun updateLoginStatus(isLoggedIn: Boolean) = withContext(Dispatchers.IO) {
        dao.updateLoginStatus(isLoggedIn)
    }

    suspend fun clearUserProfile() = withContext(Dispatchers.IO) {
        dao.clearUserProfile()
    }

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        // Seed educational content only - strictly NO dummy user profiles or credentials
        // Profiles are populated solely from verified server-side authentication

        // Seed an initial saved course and paper for realistic experience
        val initialPaper = SampleEducationalData.pastPapers.first()
        dao.insertSavedPaper(
            SavedPaperEntity(
                id = initialPaper.id,
                title = initialPaper.title,
                examination = initialPaper.examination,
                subject = initialPaper.subject,
                year = initialPaper.year,
                medium = initialPaper.medium,
                fileSize = initialPaper.fileSize,
                isDownloaded = true
            )
        )

        val initialCourse = SampleEducationalData.courses.first()
        dao.insertSavedCourse(
            SavedCourseEntity(
                id = initialCourse.id,
                title = initialCourse.title,
                instructor = initialCourse.instructor,
                institution = initialCourse.institution,
                level = initialCourse.level.displayName,
                progressPercent = 31,
                totalLessons = initialCourse.totalLessons,
                completedLessons = initialCourse.completedLessons
            )
        )

        dao.insertActivity(
            LearningActivityEntity(
                title = "Solved A/L Combined Maths 2024 (Part A)",
                category = "Past Paper",
                description = "Scored 88% in Trigonometry & Calculus questions",
                scoreOrDuration = "88%"
            )
        )
        dao.insertActivity(
            LearningActivityEntity(
                title = "Reviewed A/L Physics Mechanics",
                category = "Course",
                description = "Completed Projectile Motion lecture & formula review",
                scoreOrDuration = "45 min"
            )
        )
    }

    fun generateAiTutorResponse(prompt: String): AiChatMessage = Companion.generateAiTutorResponse(prompt)

    companion object {
        fun generateAiTutorResponse(prompt: String): AiChatMessage {
            val lower = prompt.lowercase()
            val content: String
            val suggestions: List<String>

        when {
            lower.contains("bernoulli") || lower.contains("fluid") -> {
                content = """
                    **Bernoulli's Principle & Equation (A/L Physics Sri Lanka):**
                    
                    **1. Statement:**
                    For an incompressible, non-viscous fluid undergoing streamline (laminar) flow along a streamline, the sum of pressure energy, kinetic energy per unit volume, and potential energy per unit volume remains constant.
                    
                    **2. Mathematical Expression:**
                    P + (1/2)ρv² + ρgh = Constant
                    
                    * P = Static pressure of the fluid
                    * (1/2)ρv² = Dynamic pressure (kinetic energy density)
                    * ρgh = Hydrostatic potential pressure
                    
                    **3. Key Examination Applications:**
                    * **Venturi Meter:** Measuring fluid flow rates through a pipe constriction.
                    * **Pitot Tube:** Measuring air speed of airplanes.
                    * **Atomizer / Spray gun:** High velocity air drops local pressure, drawing up liquid.
                    * **Aerodynamic Lift on Aerofoil:** Faster airflow over curved top creates low pressure above.
                    
                    *Common A/L Exam Trap:* Remember the continuity equation A1·v1 = A2·v2. When pipe cross-section narrows, velocity v increases, meaning pressure P MUST decrease!
                """.trimIndent()
                suggestions = listOf(
                    "Solve a Venturi Meter numerical problem",
                    "Explain Torricelli's Law of Efflux",
                    "Generate 3 A/L Physics MCQs on Fluid Dynamics"
                )
            }
            lower.contains("calculus") || lower.contains("integral") || lower.contains("differentiat") -> {
                content = """
                    **A/L Combined Mathematics: Calculus Mastery Guide**
                    
                    **1. Essential Standard Integrals:**
                    * ∫ 1/√(a² - x²) dx = sin⁻¹(x/a) + C
                    * ∫ 1/(a² + x²) dx = (1/a)tan⁻¹(x/a) + C
                    * ∫ f'(x)/f(x) dx = ln|f(x)| + C
                    * ∫ e^(ax) cos(bx) dx (Solved using integration by parts twice!)
                    
                    **2. Integration by Parts Formula:**
                    ∫ u (dv/dx) dx = uv - ∫ v (du/dx) dx
                    
                    *Prioritize u using the **LIATE** rule:*
                    * **L** - Logarithmic functions (ln x)
                    * **I** - Inverse trigonometric (tan⁻¹ x, sin⁻¹ x)
                    * **A** - Algebraic polynomials (x², 3x)
                    * **T** - Trigonometric (sin x, cos x)
                    * **E** - Exponential (e²ˣ)
                """.trimIndent()
                suggestions = listOf(
                    "Solve ∫ x² e^(3x) dx step-by-step",
                    "Show partial fractions decomposition for quadratic denominator",
                    "Summarize limits using L'Hôpital's rule"
                )
            }
            lower.contains("practice question") || lower.contains("mcq") || lower.contains("quiz") -> {
                content = """
                    **Generated Sri Lankan A/L Practice Questions:**
                    
                    **Question 1 (Combined Maths - Mechanics):**
                    A particle of mass m is projected vertically upwards with initial speed u in a resisting medium where air resistance is mkv². 
                    Find the maximum height H reached in terms of g, k, and u.
                    
                    *Hint:* Use v(dv/dx) = -(g + kv²) and integrate with limits v: u → 0 and x: 0 → H.
                    *Answer:* H = (1/2k) · ln(1 + ku²/g).
                    
                    ---
                    **Question 2 (A/L Physics - Electricity):**
                    A potentiometer wire of length 100 cm has resistance 5 Ω. It is connected in series with a 2V accumulator and a resistor R. An unknown emf cell of 1.2V balances at 60 cm. 
                    Calculate the series resistance R.
                    
                    *Answer:* R = 5 Ω.
                """.trimIndent()
                suggestions = listOf(
                    "Show full working for Question 1",
                    "Show full working for Question 2",
                    "Generate 3 more Chemistry questions"
                )
            }
            lower.contains("summar") || lower.contains("lesson") -> {
                content = """
                    **Lesson Summary: Chemical Equilibrium (Le Chatelier's Principle)**
                    
                    **Core Concept:**
                    When a chemical system in dynamic equilibrium is disturbed by changes in concentration, temperature, or pressure, the system counteracts the change to re-establish equilibrium.
                    
                    **1. Effect of Temperature:**
                    * Endothermic (ΔH > 0): Heating favors forward reaction (Kc increases).
                    * Exothermic (ΔH < 0): Heating favors reverse reaction (Kc decreases).
                    
                    **2. Effect of Pressure (Gaseous Systems):**
                    * Increasing pressure shifts equilibrium towards side with fewer moles of gas.
                    * Adding an inert gas at constant volume has NO effect on equilibrium position!
                    
                    **3. Catalysts:**
                    * Accelerates both forward and reverse reaction equally.
                    * Reduces time to reach equilibrium, but DOES NOT alter Kc or yield!
                """.trimIndent()
                suggestions = listOf(
                    "Explain Haber process optimal conditions",
                    "Calculate Kp from partial pressures",
                    "Distinguish between Qc and Kc"
                )
            }
            else -> {
                content = """
                    **TheStudent Academic Tutor (Sri Lankan Curricula):**
                    
                    I can assist you across all educational levels:
                    * **School (G.C.E. O/L & A/L):** Combined Maths, Physics, Chemistry, Biology, ICT, Accounting, Economics.
                    * **Undergraduate:** Data Structures, Algorithms, Calculus, Microeconomics, Electrical Circuits.
                    * **Graduate:** Research Methodology, Statistical Modeling, Dissertation writing.
                    
                    How would you like to explore this topic? You can ask me to:
                    1. Break down a complex theoretical concept step-by-step
                    2. Provide past paper question walkthroughs and marking schemes
                    3. Generate timed practice MCQs with explanations
                    4. Summarize full lesson syllabus chapters for quick revision
                """.trimIndent()
                suggestions = listOf(
                    "Explain Bernoulli's Principle & Venturi Tube",
                    "Master A/L Combined Maths Integration techniques",
                    "Generate 3 exam practice questions with marking schemes",
                    "Summarize Le Chatelier's Principle in Chemistry"
                )
            }
        }

        return AiChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = MessageSender.AI,
            content = content,
            suggestedFollowUps = suggestions
        )
    }
    }
}
