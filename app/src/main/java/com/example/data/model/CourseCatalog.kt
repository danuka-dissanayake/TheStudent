package com.example.data.model

/**
 * The courses collection — a curated index of **other people's** courses.
 *
 * Nothing here is ours. Every entry belongs to the university or platform that
 * wrote it, links straight to their page, and names its owner on the card. The
 * app never hosts, mirrors, re-uploads or sells a course; it points at it. That
 * is why [CatalogProvider] is a first-class type and why the screen ends with a
 * credit block instead of a sign-up form.
 *
 * The catalog is bundled with the app on purpose: it is static, small, and must
 * work before the student has ever reached the relay server. Editing it means
 * editing [CourseCatalog.courses] — no deploy, no schema.
 */
data class CatalogProvider(
    val name: String,
    /** The provider's own site — where we send people when they ask "who?". */
    val url: String,
    /** "University", "Open university", "MOOC platform", "Non-profit"… */
    val kind: String,
    /** Hostname used to fetch the provider's own favicon for the avatar. */
    val domain: String
) {
    /** Up to two letters, shown only if the logo cannot be fetched. */
    val initials: String
        get() = name.split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    /**
     * The provider's favicon, served by Google's public favicon endpoint.
     *
     * We point at their published icon instead of redrawing one: it identifies
     * the owner (the whole point of the credit) without implying that we made
     * the course, and nothing is bundled in the APK.
     */
    val logoUrl: String
        get() = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128"
}

/**
 * What a student actually gets at the end — the single most important fact
 * about a free course, and the one listicles usually fudge.
 */
enum class CatalogCertificate(val label: String, val short: String) {
    /** Free to take *and* a free certificate/badge on completion. */
    FREE("Free certificate", "Free cert"),

    /** Learn free, but the certificate is paid (or needs financial aid). */
    PAID("Certificate is paid", "Paid cert"),

    /** A statement of participation / completion, not an accredited award. */
    PARTICIPATION("Statement of participation", "Participation"),

    /** Genuinely nothing at the end — great to learn from, don't expect paper. */
    NONE("No certificate", "No cert")
}

data class CatalogCourse(
    val id: String,
    val title: String,
    /** Must match a [CatalogProvider.name] — that is how credits resolve. */
    val provider: String,
    val url: String,
    val subject: String,
    /** Who it is for: "School", "A/L", "Undergraduate", "Postgraduate". */
    val stages: List<String>,
    val level: String,
    /** Approximate guided hours; 0 means open-ended / self-paced. */
    val hours: Int,
    val certificate: CatalogCertificate,
    /** One honest line about cost and how the certificate works. */
    val priceNote: String,
    val summary: String,
    val skills: List<String> = emptyList(),
    /** A related talk on YouTube — embedded in the app, credited to its channel. */
    val videoId: String? = null,
    /** Shown in the "Editor's picks" slider. */
    val featured: Boolean = false,
    /** Sri Lankan provider — flagged so local students see a familiar option. */
    val local: Boolean = false
) {
    val durationLabel: String
        get() = when {
            hours <= 0 -> "Self-paced"
            hours < 10 -> "${hours} hrs"
            hours < 40 -> "${hours} hrs"
            else -> "${hours} hrs"
        }

    val isFreeCertificate: Boolean get() = certificate == CatalogCertificate.FREE
}

/** A YouTube talk in the "Watch first" rail. Owned by its channel, not by us. */
data class CatalogVideo(
    val videoId: String,
    val title: String,
    val channel: String,
    /** Rough watch time, for the badge on the thumbnail. */
    val minutes: Int,
    val topic: String
) {
    val thumbnailUrl: String get() = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
}

object CourseCatalog {

    // ── Providers (the owners of everything below) ───────────────────────────
    val providers: List<CatalogProvider> = listOf(
        CatalogProvider(
            "University of Moratuwa",
            "https://open.uom.lk/",
            "Sri Lankan university · free",
            "open.uom.lk"
        ),
        CatalogProvider("edX", "https://www.edx.org/", "MOOC platform", "edx.org"),
        CatalogProvider("Coursera", "https://www.coursera.org/", "MOOC platform", "coursera.org"),
        CatalogProvider("OpenLearn", "https://www.open.edu/openlearn/", "The Open University, UK", "open.edu"),
        CatalogProvider("MIT OpenCourseWare", "https://ocw.mit.edu/", "University · open courseware", "ocw.mit.edu"),
        CatalogProvider("NPTEL", "https://nptel.ac.in/", "Indian Institutes of Technology", "nptel.ac.in"),
        CatalogProvider("freeCodeCamp", "https://www.freecodecamp.org/", "Non-profit", "freecodecamp.org"),
        CatalogProvider("Saylor Academy", "https://learn.saylor.org/", "Non-profit", "saylor.org"),
        CatalogProvider("Alison", "https://alison.com/", "MOOC platform", "alison.com"),
        CatalogProvider("IBM SkillsBuild", "https://skillsbuild.org/", "Industry programme", "skillsbuild.org"),
        CatalogProvider("Cisco Networking Academy", "https://www.netacad.com/", "Industry programme", "netacad.com"),
        CatalogProvider("Google", "https://grow.google/certificates/", "Industry programme", "grow.google"),
        CatalogProvider("HP LIFE", "https://www.life-global.org/", "Non-profit · HP Foundation", "life-global.org"),
        CatalogProvider("Kaggle", "https://www.kaggle.com/learn", "Data science community", "kaggle.com"),
        CatalogProvider("Microsoft Learn", "https://learn.microsoft.com/training/", "Industry programme", "microsoft.com"),
        CatalogProvider("Khan Academy", "https://www.khanacademy.org/", "Non-profit", "khanacademy.org")
    )

    // ── Courses ─────────────────────────────────────────────────────────────
    // Ordered roughly by "how likely is this to change a Sri Lankan student's
    // next six months". Prices and certificate terms are the provider's call and
    // are written as they stood when this list was last checked.
    val courses: List<CatalogCourse> = listOf(

        // ── Sri Lanka ───────────────────────────────────────────────────────
        CatalogCourse(
            id = "uom-python",
            title = "Programming in Python 1 — Python for Beginners",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/lms/course/view.php?id=15",
            subject = "Programming",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 30,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free to study and free certificate from the University of Moratuwa.",
            summary = "Moratuwa's open learning platform, taught by its own academics and " +
                "industry engineers. Starts from zero and is built for Sri Lankan students " +
                "preparing for an IT career.",
            skills = listOf("Python", "Problem solving", "Automation"),
            featured = true,
            local = true
        ),
        CatalogCourse(
            id = "uom-fullstack",
            title = "Trainee Full Stack Developer — career path",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/fullstack-developer.html",
            subject = "Software Engineering",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 300,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free of charge; certificates issued by UoM's Centre for Open and Distance Learning.",
            summary = "Six modules that take a school leaver to industry-ready intern: web " +
                "fundamentals, databases, server-side code and version control. Sponsored by " +
                "DP Education and run with the Computer Society of Sri Lanka.",
            skills = listOf("HTML/CSS", "JavaScript", "SQL", "Git"),
            featured = true,
            local = true
        ),
        CatalogCourse(
            id = "uom-webdesign",
            title = "Web Development 1 — Web Design for Beginners",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/lms/course/view.php?id=16",
            subject = "Web Development",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 25,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free; certificate from the University of Moratuwa on completion.",
            summary = "Design-then-build: layout, colour and typography first, then the HTML " +
                "and CSS that make it real. The gentlest entry point into the UoM career paths.",
            skills = listOf("HTML", "CSS", "UI layout"),
            local = true
        ),
        CatalogCourse(
            id = "uom-pm",
            title = "Project Management Skills",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/lms/course/view.php?id=42",
            subject = "Management",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 40,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free; certificate from the University of Moratuwa.",
            summary = "A career-start programme aligned with the PMI's CAPM exam outline — " +
                "scoping, scheduling, risk and stakeholder communication in plain language.",
            skills = listOf("Project planning", "Risk", "Communication"),
            local = true
        ),

        // ── Computer science ────────────────────────────────────────────────
        CatalogCourse(
            id = "cs50x",
            title = "CS50x — Introduction to Computer Science",
            provider = "edX",
            url = "https://cs50.harvard.edu/x/",
            subject = "Computer Science",
            stages = listOf("A/L", "Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 100,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free on cs50.harvard.edu, with a free CS50 certificate; the edX verified certificate is paid.",
            summary = "Harvard's famous first course: C, Python, SQL, JavaScript and a final " +
                "project. The widest single foundation in computing you can get for nothing.",
            skills = listOf("C", "Python", "SQL", "Algorithms"),
            videoId = "rfscVS0vtbw",
        ),
        CatalogCourse(
            id = "cs50p",
            title = "CS50P — Introduction to Programming with Python",
            provider = "edX",
            url = "https://cs50.harvard.edu/python/",
            subject = "Programming",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 60,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free to follow and free CS50 certificate; edX verified certificate is paid.",
            summary = "The Python half of CS50, on its own. Starts at variables, ends at " +
                "regular expressions, testing and file I/O — with graded problem sets.",
            skills = listOf("Python", "Testing", "File I/O"),
            videoId = "rfscVS0vtbw"
        ),
        CatalogCourse(
            id = "cs50ai",
            title = "CS50AI — Introduction to AI with Python",
            provider = "edX",
            url = "https://cs50.harvard.edu/ai/",
            subject = "Artificial Intelligence",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 70,
            certificate = CatalogCertificate.PAID,
            priceNote = "Free to audit; the edX certificate is paid. Problem sets are free.",
            summary = "Search, knowledge, uncertainty, optimisation, learning, neural networks " +
                "and language — each with a project in Python. Assumes CS50x or equivalent.",
            skills = listOf("Search", "Probability", "Neural networks", "Python"),
            videoId = "aircAruvnKk"
        ),
        CatalogCourse(
            id = "fcc-curriculum",
            title = "freeCodeCamp — Responsive Web Design certification",
            provider = "freeCodeCamp",
            url = "https://www.freecodecamp.org/learn/",
            subject = "Web Development",
            stages = listOf("School", "A/L", "Undergraduate"),
            level = "Beginner",
            hours = 300,
            certificate = CatalogCertificate.FREE,
            priceNote = "Everything is free, including the certification — no account upgrade exists.",
            summary = "About 300 hours of hands-on HTML and CSS ending in five projects and a " +
                "verifiable certificate. The same curriculum continues through JavaScript, " +
                "front-end libraries, data analysis and machine learning.",
            skills = listOf("HTML", "CSS", "Accessibility", "Projects"),
            videoId = "bMknfKXIFA8",
        ),
        CatalogCourse(
            id = "fcc-js",
            title = "Learn JavaScript — full course for beginners",
            provider = "freeCodeCamp",
            url = "https://www.youtube.com/watch?v=PkZNo7MFNFg",
            subject = "Programming",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 40,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free course and free certification from freeCodeCamp.",
            summary = "One long, patient video walk-through of JavaScript from variables to " +
                "the DOM, paired with the free certification curriculum on the site.",
            skills = listOf("JavaScript", "DOM", "ES6"),
            videoId = "PkZNo7MFNFg"
        ),
        CatalogCourse(
            id = "fcc-sql",
            title = "SQL — full database course for beginners",
            provider = "freeCodeCamp",
            url = "https://www.youtube.com/watch?v=HXV3zeQKqGY",
            subject = "Databases",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 15,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free video course; no certificate — pair it with freeCodeCamp's relational database certification.",
            summary = "Databases from first principles: modelling, normalisation, queries, " +
                "joins, indexes and transactions, taught against a real MySQL schema.",
            skills = listOf("SQL", "Modelling", "Indexes"),
            videoId = "HXV3zeQKqGY"
        ),
        CatalogCourse(
            id = "git-basics",
            title = "Git and GitHub for Beginners — crash course",
            provider = "freeCodeCamp",
            url = "https://www.youtube.com/watch?v=RGOj5yH7evk",
            subject = "Software Engineering",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 2,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free video; no certificate.",
            summary = "The ninety minutes that stop you emailing zip files: commits, branches, " +
                "merging, pull requests and recovering from the mistakes everyone makes.",
            skills = listOf("Git", "GitHub", "Version control"),
            videoId = "RGOj5yH7evk"
        ),

        // ── Mathematics & statistics ────────────────────────────────────────
        CatalogCourse(
            id = "mit-1806",
            title = "18.06 Linear Algebra (full lectures)",
            provider = "MIT OpenCourseWare",
            url = "https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/",
            subject = "Mathematics",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 45,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free and open, no certificate — MIT OCW does not issue them.",
            summary = "Gilbert Strang's complete MIT course: elimination, vector spaces, " +
                "eigenvalues, SVD and the applications that make them matter. Notes, exams " +
                "and 35 lectures, all free.",
            skills = listOf("Linear algebra", "Eigenvalues", "SVD"),
            videoId = "ZK3O402wf1c",
        ),
        CatalogCourse(
            id = "essence-calculus",
            title = "Calculus 1 — limits, derivatives and integrals",
            provider = "Khan Academy",
            url = "https://www.khanacademy.org/math/calculus-1",
            subject = "Mathematics",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 10,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free forever; Khan Academy issues no certificates.",
            summary = "Calculus rebuilt from intuition rather than rules, then drilled on Khan " +
                "Academy's free practice sets. Best used the week before a calculus module " +
                "starts, not after it has gone wrong.",
            skills = listOf("Limits", "Derivatives", "Integrals"),
            videoId = "WUvTyaaNkzM"
        ),
        CatalogCourse(
            id = "jamovi",
            title = "Getting started with Jamovi",
            provider = "OpenLearn",
            url = "https://www.open.edu/openlearn/health-sports-psychology/getting-started-jamovi/content-section-0",
            subject = "Statistics",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 8,
            certificate = CatalogCertificate.PARTICIPATION,
            priceNote = "Free; a free statement of participation, no formal credit.",
            summary = "Quantitative analysis for a dissertation or thesis in one sitting: " +
                "descriptives, graphs and the tests most Sri Lankan undergraduates actually " +
                "need, in a free open-source package.",
            skills = listOf("Descriptive stats", "t-tests", "Data cleaning")
        ),
        CatalogCourse(
            id = "statquest-pca",
            title = "Kaggle Learn — Python, Pandas and intro to machine learning",
            provider = "Kaggle",
            url = "https://www.kaggle.com/learn/python",
            subject = "Data Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 20,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free; Kaggle awards skill badges, not certificates.",
            summary = "Kaggle's short hands-on tracks (Python, Pandas, data cleaning, " +
                "intro to machine learning) are the fastest way from theory to a working " +
                "notebook — pair them with StatQuest videos when the maths gets steep.",
            skills = listOf("Pandas", "Notebooks", "ML basics"),
            videoId = "FgakZw6K1QQ"
        ),

        // ── Study skills, writing & research ────────────────────────────────
        CatalogCourse(
            id = "learning-how-to-learn",
            title = "Learning How to Learn",
            provider = "Coursera",
            url = "https://www.coursera.org/learn/learning-how-to-learn",
            subject = "Study Skills",
            stages = listOf("A/L", "Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 15,
            certificate = CatalogCertificate.PAID,
            priceNote = "Free to audit; certificate is paid or through Coursera financial aid.",
            summary = "The most-taken course on the internet, from McMaster and UC San Diego: " +
                "spaced repetition, interleaving, chunking and beating procrastination — the " +
                "four things that decide whether your study hours count.",
            skills = listOf("Spaced repetition", "Focus", "Memory"),
            featured = true
        ),
        CatalogCourse(
            id = "writing-sciences",
            title = "Writing in the Sciences",
            provider = "Coursera",
            url = "https://www.coursera.org/learn/scientific-writing",
            subject = "Academic Writing",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 25,
            certificate = CatalogCertificate.PAID,
            priceNote = "Free to audit; certificate is paid or via financial aid.",
            summary = "Stanford's course on writing papers people finish reading: cutting " +
                "clutter, structuring a paragraph, presenting data honestly and surviving " +
                "peer review. Aimed squarely at final-year and postgraduate work.",
            skills = listOf("Academic writing", "Editing", "Data presentation")
        ),
        CatalogCourse(
            id = "python-everybody",
            title = "Python for Everybody (specialisation)",
            provider = "Coursera",
            url = "https://www.coursera.org/specializations/python",
            subject = "Programming",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 60,
            certificate = CatalogCertificate.PAID,
            priceNote = "Free to audit lesson by lesson; certificate per course is paid or via financial aid.",
            summary = "Charles Severance's five-course sequence from first script to scraping " +
                "and databases. The gentlest route for a non-CS undergraduate who needs code " +
                "for research.",
            skills = listOf("Python", "Web APIs", "SQLite")
        ),
        CatalogCourse(
            id = "how-to-speak",
            title = "How to Speak — Patrick Winston's MIT lecture",
            provider = "MIT OpenCourseWare",
            url = "https://www.youtube.com/watch?v=Unzc731iCUY",
            subject = "Communication",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 2,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free lecture; no certificate.",
            summary = "One hour that fixes presentations, vivas and job interviews. Winston's " +
                "rules — open with an empowerment promise, cycle back, stop on time — are " +
                "still the best advice given on the subject.",
            skills = listOf("Presenting", "Structure", "Q&A"),
            videoId = "Unzc731iCUY",
        ),

        // ── Business, careers & industry certificates ───────────────────────
        CatalogCourse(
            id = "openlearn-swot",
            title = "Business communication: writing a SWOT analysis",
            provider = "OpenLearn",
            url = "https://www.open.edu/openlearn/money-business/business-communication-writing-swot-analysis/content-section-0",
            subject = "Business",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 8,
            certificate = CatalogCertificate.PARTICIPATION,
            priceNote = "Free course and free statement of participation from The Open University.",
            summary = "Writing that gets marked well: picking the key facts out of a case " +
                "study, making concise notes, choosing a structure and writing the analysis " +
                "itself. Useful in every management module you will take.",
            skills = listOf("Business writing", "SWOT", "Case analysis")
        ),
        CatalogCourse(
            id = "ibm-skillsbuild",
            title = "IBM SkillsBuild — AI, cloud and cybersecurity foundations",
            provider = "IBM SkillsBuild",
            url = "https://skillsbuild.org/",
            subject = "Artificial Intelligence",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 40,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free courses with free IBM digital credentials, plus free career support.",
            summary = "Short, employer-branded modules in the skills job adverts keep asking " +
                "for, ending in IBM digital badges. The most direct free route to a credibly " +
                "signed certificate in this list.",
            skills = listOf("AI fundamentals", "Cloud", "Cybersecurity"),
            featured = true
        ),
        CatalogCourse(
            id = "cisco-networking",
            title = "Cisco Networking Academy — Networking Basics",
            provider = "Cisco Networking Academy",
            url = "https://www.netacad.com/courses",
            subject = "Networking",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 25,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free courses and free digital badges from Cisco Networking Academy.",
            summary = "How an internet actually works — addressing, routing, protocols and " +
                "wireless — from the company that builds the routers, with badge-backed " +
                "completion. A strong first step towards CCNA.",
            skills = listOf("TCP/IP", "Routing", "Wireless")
        ),
        CatalogCourse(
            id = "google-skillshop",
            title = "Google Skillshop — Analytics and Ads certifications",
            provider = "Google",
            url = "https://skillshop.withgoogle.com/",
            subject = "Digital Marketing",
            stages = listOf("A/L", "Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 12,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free training; Google certifications are free to take and pass.",
            summary = "The certificates that actually appear in Sri Lankan marketing job " +
                "adverts: Google Analytics 4 and Google Ads, with a timed assessment at the end.",
            skills = listOf("Analytics", "Ads", "Measurement")
        ),
        CatalogCourse(
            id = "hp-life",
            title = "HP LIFE — entrepreneurship and business skills",
            provider = "HP LIFE",
            url = "https://www.life-global.org/",
            subject = "Entrepreneurship",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 10,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free from the HP Foundation; free certificate of completion per course.",
            summary = "Short, practical modules on costing, cash flow, marketing and pitching — " +
                "written for people starting something small, not for MBA classrooms.",
            skills = listOf("Budgeting", "Marketing", "Pitching")
        ),
        CatalogCourse(
            id = "saylor-cs",
            title = "Saylor Academy — free college-level course sequences",
            provider = "Saylor Academy",
            url = "https://learn.saylor.org/",
            subject = "Computer Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 60,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free courses and free certificates; some are ACE credit-recommended.",
            summary = "Full semester-equivalent courses — CS101, programming, databases, " +
                "microeconomics — with free certificates and, uniquely, the possibility of " +
                "US college-credit recommendation.",
            skills = listOf("Programming", "Economics", "Databases")
        ),
        CatalogCourse(
            id = "alison-pm",
            title = "Alison — Project Management Diploma",
            provider = "Alison",
            url = "https://alison.com/courses",
            subject = "Management",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 15,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free to study; free digital certificate, printed or PDF copies cost a small fee.",
            summary = "One of Alison's largest free offerings — project lifecycle, scheduling, " +
                "budgeting and team roles — good for a first credential when a CV needs " +
                "something today.",
            skills = listOf("Planning", "Budgeting", "Teams")
        ),
        CatalogCourse(
            id = "nptel",
            title = "NPTEL — IIT engineering courses (certified exam optional)",
            provider = "NPTEL",
            url = "https://nptel.ac.in/courses",
            subject = "Engineering",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Advanced",
            hours = 40,
            certificate = CatalogCertificate.PAID,
            priceNote = "Lectures and assignments free; the proctored certificate exam is paid (a few thousand LKR).",
            summary = "Thousands of semester-length courses from the IITs — signals, machines, " +
                "structures, electronics — heavily used by South Asian engineering " +
                "undergraduates. The exam is optional and the fee is small.",
            skills = listOf("Engineering", "Problem sets", "Exams")
        ),
        CatalogCourse(
            id = "ms-learn",
            title = "Microsoft Learn — free training paths and exams prep",
            provider = "Microsoft Learn",
            url = "https://learn.microsoft.com/training/",
            subject = "Cloud Computing",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 30,
            certificate = CatalogCertificate.PAID,
            priceNote = "All training is free; the certification exam itself is paid.",
            summary = "Guided paths for Azure, Power BI, security and data engineering, with " +
                "free sandboxes. The exams cost money, but everything up to them does not.",
            skills = listOf("Azure", "Power BI", "Security")
        ),

        // ── More from Moratuwa ──────────────────────────────────────────────
        CatalogCourse(
            id = "uom-python-programmer",
            title = "Python Programmer — career path",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/python-programmer.html",
            subject = "Programming",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 120,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free, with certificates from the University of Moratuwa.",
            summary = "The structured route from the Python for Beginners module to " +
                "scripting, data handling and a first portfolio — laid out as a career path " +
                "rather than a pile of videos.",
            skills = listOf("Python", "Scripting", "Data handling"),
            local = true
        ),
        CatalogCourse(
            id = "uom-data-scientist",
            title = "Data Scientist — career path",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/data-scientist.html",
            subject = "Data Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 200,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free, with certificates from the University of Moratuwa.",
            summary = "Statistics, Python, SQL and machine learning in one sequence, built by " +
                "UoM academics with industry input. The most complete free data path aimed at " +
                "Sri Lankan students.",
            skills = listOf("Statistics", "Pandas", "SQL", "ML"),
            local = true
        ),
        CatalogCourse(
            id = "uom-mobile-developer",
            title = "Mobile Developer — career path",
            provider = "University of Moratuwa",
            url = "https://open.uom.lk/mobile-developer.html",
            subject = "Mobile Development",
            stages = listOf("Undergraduate"),
            level = "Beginner",
            hours = 120,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free, with certificates from the University of Moratuwa.",
            summary = "Android and cross-platform fundamentals: layouts, state, HTTP and " +
                "publishing — the skills a first app on the Play Store actually needs.",
            skills = listOf("Android", "Kotlin", "APIs"),
            local = true
        ),

        // ── Computer science, continued ─────────────────────────────────────
        CatalogCourse(
            id = "cs50w",
            title = "CS50W — Web Programming with Python and JavaScript",
            provider = "edX",
            url = "https://cs50.harvard.edu/web/",
            subject = "Web Development",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 80,
            certificate = CatalogCertificate.PAID,
            priceNote = "Free to follow on cs50.harvard.edu; the edX certificate is paid.",
            summary = "Django, SQL models, JavaScript and Git — then a final project of your " +
                "own. Assumes CS50x or a year of programming.",
            skills = listOf("Django", "SQL", "JavaScript", "Git"),
            featured = true
        ),
        CatalogCourse(
            id = "cs50sql",
            title = "CS50SQL — Introduction to Databases with SQL",
            provider = "edX",
            url = "https://cs50.harvard.edu/sql/",
            subject = "Databases",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 50,
            certificate = CatalogCertificate.PAID,
            priceNote = "Free to follow on cs50.harvard.edu; the edX certificate is paid.",
            summary = "Relational modelling, queries, indexes, transactions and NoSQL, with " +
                "problem sets against real SQLite and PostgreSQL databases.",
            skills = listOf("SQL", "Modelling", "PostgreSQL")
        ),
        CatalogCourse(
            id = "mit-6006",
            title = "6.006 Introduction to Algorithms (full lectures)",
            provider = "MIT OpenCourseWare",
            url = "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/",
            subject = "Computer Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Advanced",
            hours = 40,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free and open; MIT OCW issues no certificates.",
            summary = "Data structures and algorithm analysis taught properly: hashing, trees, " +
                "graphs, shortest paths and dynamic programming, with problem sets and exams.",
            skills = listOf("Algorithms", "Data structures", "Complexity"),
            featured = true
        ),
        CatalogCourse(
            id = "fcc-data-analysis",
            title = "freeCodeCamp — Data Analysis with Python certification",
            provider = "freeCodeCamp",
            url = "https://www.freecodecamp.org/learn/data-analysis-with-python/",
            subject = "Data Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Intermediate",
            hours = 60,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free, including the certification.",
            summary = "NumPy, Pandas, Matplotlib and Seaborn, ending in five projects and a " +
                "verifiable certificate. The practical half of a data analysis module.",
            skills = listOf("NumPy", "Pandas", "Visualisation")
        ),

        // ── Careers, industry certificates, statistics ──────────────────────
        CatalogCourse(
            id = "google-data-analytics",
            title = "Google Data Analytics Professional Certificate",
            provider = "Coursera",
            url = "https://www.coursera.org/professional-certificates/google-data-analytics",
            subject = "Data Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 180,
            certificate = CatalogCertificate.PAID,
            priceNote = "Monthly subscription, or free through Coursera financial aid.",
            summary = "Eight courses from spreadsheets to R and SQL, with a portfolio case " +
                "study at the end. The programme most often named in data job adverts.",
            skills = listOf("Spreadsheets", "SQL", "R", "Tableau"),
            featured = true
        ),
        CatalogCourse(
            id = "google-project-management",
            title = "Google Project Management Professional Certificate",
            provider = "Coursera",
            url = "https://www.coursera.org/professional-certificates/google-project-management",
            subject = "Management",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 150,
            certificate = CatalogCertificate.PAID,
            priceNote = "Monthly subscription, or free through Coursera financial aid.",
            summary = "Agile and waterfall from the inside: charters, schedules, risk registers " +
                "and stakeholder updates, taught on the tools teams actually use.",
            skills = listOf("Agile", "Scheduling", "Risk")
        ),
        CatalogCourse(
            id = "ibm-data-science",
            title = "IBM Data Science Professional Certificate",
            provider = "Coursera",
            url = "https://www.coursera.org/professional-certificates/ibm-data-science",
            subject = "Data Science",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 160,
            certificate = CatalogCertificate.PAID,
            priceNote = "Monthly subscription, or free through Coursera financial aid.",
            summary = "Python, SQL, Jupyter and machine learning, finished with a capstone " +
                "project on a real dataset and an IBM badge.",
            skills = listOf("Python", "Jupyter", "Machine learning")
        ),
        CatalogCourse(
            id = "kaggle-ml",
            title = "Kaggle Learn — Intro to Machine Learning",
            provider = "Kaggle",
            url = "https://www.kaggle.com/learn/intro-to-machine-learning",
            subject = "Artificial Intelligence",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 6,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free; Kaggle awards skill badges, not certificates.",
            summary = "Three hours of video and hands-on exercises that get you from a CSV to " +
                "a submitted model on a live leaderboard. The fastest way to demystify ML.",
            skills = listOf("scikit-learn", "Validation", "Features")
        ),
        CatalogCourse(
            id = "cisco-cybersecurity",
            title = "Cisco — Introduction to Cybersecurity",
            provider = "Cisco Networking Academy",
            url = "https://www.netacad.com/courses/cybersecurity/introduction-cybersecurity",
            subject = "Cybersecurity",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 15,
            certificate = CatalogCertificate.FREE,
            priceNote = "Free, with a free digital badge from Cisco Networking Academy.",
            summary = "Threats, vulnerabilities, cryptography and the roles that defend them — " +
                "an honest picture of the field before you commit a degree to it.",
            skills = listOf("Threats", "Cryptography", "Security roles")
        ),
        CatalogCourse(
            id = "khan-statistics",
            title = "Statistics and probability",
            provider = "Khan Academy",
            url = "https://www.khanacademy.org/math/statistics-probability",
            subject = "Statistics",
            stages = listOf("A/L", "Undergraduate"),
            level = "Beginner",
            hours = 25,
            certificate = CatalogCertificate.NONE,
            priceNote = "Free forever; no certificates.",
            summary = "Distributions, sampling, confidence intervals and hypothesis testing, " +
                "with unlimited practice. The foundation every research methods module assumes " +
                "you already have.",
            skills = listOf("Distributions", "Hypothesis testing", "Regression")
        ),
        CatalogCourse(
            id = "ms-azure-fundamentals",
            title = "Microsoft Learn — Azure fundamentals path",
            provider = "Microsoft Learn",
            url = "https://learn.microsoft.com/training/azure/",
            subject = "Cloud Computing",
            stages = listOf("Undergraduate", "Postgraduate"),
            level = "Beginner",
            hours = 20,
            certificate = CatalogCertificate.PAID,
            priceNote = "All training is free; the AZ-900 exam is paid.",
            summary = "Cloud concepts, core Azure services, pricing and governance — the " +
                "vocabulary half of any cloud interview, free up to the exam.",
            skills = listOf("Azure", "Governance", "Pricing")
        )
    )

    // ── Videos ──────────────────────────────────────────────────────────────
    // Verified against YouTube's own oEmbed endpoint, so an embed cannot 404.
    val videos: List<CatalogVideo> = listOf(
        CatalogVideo(
            "Unzc731iCUY",
            "How to Speak",
            "MIT OpenCourseWare",
            60,
            "Communication"
        ),
        CatalogVideo(
            "aircAruvnKk",
            "But what is a neural network?",
            "3Blue1Brown",
            19,
            "AI"
        ),
        CatalogVideo(
            "ZK3O402wf1c",
            "Lec 1 · MIT 18.06 Linear Algebra",
            "MIT OpenCourseWare",
            39,
            "Mathematics"
        ),
        CatalogVideo(
            "WUvTyaaNkzM",
            "The essence of calculus",
            "3Blue1Brown",
            17,
            "Mathematics"
        ),
        CatalogVideo(
            "rfscVS0vtbw",
            "Learn Python — full course for beginners",
            "freeCodeCamp.org",
            268,
            "Programming"
        ),
        CatalogVideo(
            "HXV3zeQKqGY",
            "SQL — full database course",
            "freeCodeCamp.org",
            263,
            "Databases"
        ),
        CatalogVideo(
            "RGOj5yH7evk",
            "Git and GitHub for Beginners",
            "freeCodeCamp.org",
            68,
            "Software Engineering"
        ),
        CatalogVideo(
            "FgakZw6K1QQ",
            "StatQuest: Principal Component Analysis",
            "StatQuest with Josh Starmer",
            20,
            "Statistics"
        ),
        CatalogVideo(
            "PkZNo7MFNFg",
            "Learn JavaScript — full course",
            "freeCodeCamp.org",
            207,
            "Programming"
        ),
        CatalogVideo(
            "bMknfKXIFA8",
            "React course — beginner's tutorial",
            "freeCodeCamp.org",
            720,
            "Web Development"
        )
    )

    /** Providers we actually link to, so the credit block lists only real ones. */
    val usedProviders: List<CatalogProvider>
        get() {
            val names = courses.map { it.provider }.toSet()
            return providers.filter { it.name in names }
        }

    // Looked up from every card, several times per row: a map, not a scan.
    private val providerIndex: Map<String, CatalogProvider> by lazy {
        providers.associateBy { it.name }
    }
    private val courseIndex: Map<String, CatalogCourse> by lazy {
        courses.associateBy { it.id }
    }
    private val videoIndex: Map<String, CatalogVideo> by lazy {
        videos.associateBy { it.videoId }
    }

    fun provider(name: String): CatalogProvider? = providerIndex[name]

    fun course(id: String): CatalogCourse? = courseIndex[id]

    fun video(id: String): CatalogVideo? = videoIndex[id]

    /** The four filters the screen offers, in order. */
    val subjects: List<String>
        get() = courses.map { it.subject }.distinct().sorted()
}
