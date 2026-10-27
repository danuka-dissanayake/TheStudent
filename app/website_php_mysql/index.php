<?php
/**
 * TheStudent - Past Papers Portal (Public Facing Web Application)
 * Modern, responsive, card-based interface with Grade -> Subject -> Filtered Papers flow.
 */
require_once __DIR__ . '/config.php';
$pdo = getDbConnection();

// Fetch all grades
$gradesStmt = $pdo->query("SELECT * FROM grades ORDER BY sort_order ASC");
$grades = $gradesStmt->fetchAll();

// Default selected grade is Grade 11 – O/L (or first grade)
$defaultGrade = $grades[5] ?? $grades[0];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Past Papers | TheStudent - Official Examination Portal</title>
    <!-- Tailwind CSS CDN for high-end modern styling -->
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: {
                            50: '#ecfdf5',
                            100: '#d1fae5',
                            500: '#10b981',
                            600: '#059669',
                            700: '#047857',
                            800: '#065f46',
                            900: '#064e3b',
                        }
                    }
                }
            }
        }
    </script>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f8fafc; }
        .glass-card { background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); }
    </style>
</head>
<body class="text-slate-800 antialiased min-h-screen flex flex-col">

    <!-- Top Navigation Bar -->
    <header class="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-sm">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 rounded-xl bg-brand-600 flex items-center justify-center text-white shadow-md shadow-brand-500/20">
                    <i class="fa-solid fa-graduation-cap text-lg"></i>
                </div>
                <div>
                    <span class="text-xl font-extrabold tracking-tight text-slate-900"><span class="text-brand-600">The</span>Student.</span>
                    <span class="hidden sm:inline-block ml-2 px-2 py-0.5 text-xs font-semibold bg-brand-100 text-brand-800 rounded-md">Past Papers Portal</span>
                </div>
            </div>

            <div class="flex items-center space-x-3">
                <a href="admin/index.php" class="inline-flex items-center px-3.5 py-2 text-xs sm:text-sm font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition">
                    <i class="fa-solid fa-lock mr-2 text-slate-500"></i> Admin Panel
                </a>
            </div>
        </div>
    </header>

    <!-- Hero Header -->
    <section class="bg-white border-b border-slate-200 pt-8 pb-10">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="max-w-3xl">
                <h1 class="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">Past Papers</h1>
                <p class="mt-2 text-base sm:text-lg text-slate-600">Find and download past examination papers easily.</p>
            </div>

            <!-- Instant Search Bar -->
            <div class="mt-6 max-w-2xl relative">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400">
                    <i class="fa-solid fa-magnifying-glass text-brand-600"></i>
                </div>
                <input type="text" id="searchInput" placeholder="Search by subject, year (e.g. 2024, 2025), medium or examination..." 
                       class="w-full pl-11 pr-4 py-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition text-sm sm:text-base shadow-sm">
            </div>

            <!-- Grade Categories Navigation: Grade 6 to Grade 13 -->
            <div class="mt-8">
                <div class="text-xs font-extrabold tracking-wider text-slate-500 uppercase mb-3">SELECT YOUR GRADE</div>
                <div class="flex items-center space-x-2 sm:space-x-3 overflow-x-auto pb-2 scrollbar-none" id="gradesPillsContainer">
                    <?php foreach ($grades as $g): ?>
                        <button onclick="selectGrade(<?= $g['id'] ?>, '<?= e($g['name']) ?>')" 
                                id="grade-btn-<?= $g['id'] ?>"
                                class="grade-pill whitespace-nowrap px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-bold border transition shadow-sm
                                <?= $g['id'] == $defaultGrade['id'] ? 'bg-brand-600 text-white border-brand-600' : 'bg-white text-slate-700 border-slate-200 hover:border-brand-500 hover:bg-slate-50' ?>">
                            <?= e($g['name']) ?>
                        </button>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>
    </section>

    <!-- Main Content Area -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full">
        
        <!-- Breadcrumb / Selected Grade & Subject Display -->
        <div class="flex flex-wrap items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-200">
            <div class="flex items-center space-x-2 text-sm font-semibold text-slate-600">
                <span id="currentGradeLabel" class="text-brand-700 font-bold text-base"><?= e($defaultGrade['name']) ?></span>
                <span id="breadcrumbArrow" class="hidden text-slate-400">›</span>
                <span id="currentSubjectLabel" class="hidden text-slate-900 font-bold text-base"></span>
            </div>

            <!-- Clear Subject Button -->
            <button id="viewAllSubjectsBtn" onclick="selectSubject(null, '')" class="hidden text-xs sm:text-sm font-bold text-brand-600 hover:text-brand-700 flex items-center">
                <i class="fa-solid fa-arrow-left mr-1.5"></i> Back to All Subjects
            </button>
        </div>

        <!-- 1. SUBJECTS GRID VIEW (Shows when no subject is selected) -->
        <section id="subjectsSection" class="mb-10">
            <h2 class="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">Available Subjects for <span id="subjectsGradeTitle"><?= e($defaultGrade['name']) ?></span></h2>
            
            <div id="subjectsGrid" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-4">
                <!-- Dynamically populated via JS -->
            </div>
        </section>

        <!-- 2. FILTER CONTROLS BAR (Year | Medium | Examination Type) -->
        <section id="filterControlsBar" class="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-sm mb-6">
            <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div class="flex flex-wrap items-center gap-3">
                    <span class="text-xs font-bold text-slate-500 uppercase tracking-wider">Filters:</span>

                    <!-- Year Dropdown -->
                    <select id="yearFilter" onchange="applyFilters()" class="text-xs sm:text-sm font-medium bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500">
                        <option value="All Years">All Years</option>
                        <option value="2025">2025</option>
                        <option value="2024">2024</option>
                        <option value="2023">2023</option>
                        <option value="2022">2022</option>
                        <option value="2021">2021</option>
                        <option value="2020">2020</option>
                    </select>

                    <!-- Medium Dropdown -->
                    <select id="mediumFilter" onchange="applyFilters()" class="text-xs sm:text-sm font-medium bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500">
                        <option value="All Mediums">All Mediums</option>
                        <option value="Sinhala">Sinhala (සිංහල)</option>
                        <option value="English">English</option>
                        <option value="Tamil">Tamil (தமிழ்)</option>
                    </select>

                    <!-- Examination Type Dropdown -->
                    <select id="examTypeFilter" onchange="applyFilters()" class="text-xs sm:text-sm font-medium bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500">
                        <option value="All Types">All Exam Types</option>
                        <option value="O/L">G.C.E. O/L</option>
                        <option value="A/L">G.C.E. A/L</option>
                        <option value="School Term Test">School Term Test</option>
                        <option value="Model Paper">Model Paper</option>
                    </select>
                </div>

                <div class="flex items-center justify-between md:justify-end space-x-3 text-xs sm:text-sm text-slate-500 font-medium">
                    <span id="resultsCount">Loading papers...</span>
                    <button onclick="resetFilters()" class="text-brand-600 hover:text-brand-700 font-bold hover:underline">Reset</button>
                </div>
            </div>
        </section>

        <!-- 3. PAPERS CARDS GRID (Modern, clean, card-based layout) -->
        <section id="papersListContainer" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
            <!-- Dynamically populated via JS -->
        </section>

        <!-- Empty State Message -->
        <div id="noPapersMessage" class="hidden bg-white rounded-2xl border border-slate-200 p-12 text-center max-w-lg mx-auto shadow-sm">
            <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400 text-2xl mb-4">
                <i class="fa-solid fa-file-circle-question"></i>
            </div>
            <h3 class="text-lg font-bold text-slate-900">No examination papers found</h3>
            <p class="text-sm text-slate-500 mt-1">Try resetting your filters or selecting a different subject or grade.</p>
            <button onclick="resetFilters()" class="mt-5 px-5 py-2.5 bg-brand-600 hover:bg-brand-700 text-white rounded-xl text-xs sm:text-sm font-bold shadow-md shadow-brand-500/20 transition">
                Reset All Filters
            </button>
        </div>

    </main>

    <!-- Footer -->
    <footer class="bg-white border-t border-slate-200 mt-16 py-8 text-center text-xs text-slate-500">
        <div class="max-w-7xl mx-auto px-4">
            <p class="font-semibold text-slate-700">TheStudent — Everything a Student Needs (Sri Lanka's Premier Digital Examination & Learning Platform)</p>
            <p class="mt-1">Providing syllabus-aligned G.C.E. A/L, O/L, and Junior Secondary past papers with marking schemes.</p>
        </div>
    </footer>

    <!-- Client-side Interactive Script -->
    <script>
        let currentGradeId = <?= (int)$defaultGrade['id'] ?>;
        let currentGradeName = "<?= addslashes($defaultGrade['name']) ?>";
        let currentSubjectId = null;
        let currentSubjectName = "";

        // Icon mapper helper
        function getSubjectIconClass(name) {
            const lower = name.toLowerCase();
            if (lower.includes('math')) return 'fa-calculator text-blue-600 bg-blue-50';
            if (lower.includes('sci') || lower.includes('bio')) return 'fa-dna text-emerald-600 bg-emerald-50';
            if (lower.includes('phy')) return 'fa-bolt text-amber-600 bg-amber-50';
            if (lower.includes('chem')) return 'fa-flask text-red-600 bg-red-50';
            if (lower.includes('ict')) return 'fa-microchip text-purple-600 bg-purple-50';
            if (lower.includes('eng')) return 'fa-language text-sky-600 bg-sky-50';
            if (lower.includes('acc') || lower.includes('com')) return 'fa-chart-line text-green-600 bg-green-50';
            if (lower.includes('econ')) return 'fa-chart-pie text-teal-600 bg-teal-50';
            if (lower.includes('his')) return 'fa-landmark text-orange-600 bg-orange-50';
            return 'fa-book text-brand-600 bg-brand-50';
        }

        // 1. Grade Selection Handler
        function selectGrade(gradeId, gradeName) {
            currentGradeId = gradeId;
            currentGradeName = gradeName;
            currentSubjectId = null;
            currentSubjectName = "";

            // Update pills UI
            document.querySelectorAll('.grade-pill').forEach(btn => {
                btn.className = "grade-pill whitespace-nowrap px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-bold border transition shadow-sm bg-white text-slate-700 border-slate-200 hover:border-brand-500 hover:bg-slate-50";
            });
            const activeBtn = document.getElementById('grade-btn-' + gradeId);
            if (activeBtn) {
                activeBtn.className = "grade-pill whitespace-nowrap px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-bold border transition shadow-sm bg-brand-600 text-white border-brand-600";
            }

            // Update Labels
            document.getElementById('currentGradeLabel').innerText = gradeName;
            document.getElementById('subjectsGradeTitle').innerText = gradeName;
            document.getElementById('breadcrumbArrow').classList.add('hidden');
            document.getElementById('currentSubjectLabel').classList.add('hidden');
            document.getElementById('viewAllSubjectsBtn').classList.add('hidden');

            loadSubjects();
            loadPapers();
        }

        // 2. Load Subjects for current Grade
        async function loadSubjects() {
            try {
                const res = await fetch(`api/papers.php?action=get_subjects&grade_id=${currentGradeId}`);
                const result = await res.json();
                const grid = document.getElementById('subjectsGrid');
                grid.innerHTML = '';

                if (result.status === 'success' && result.data.length > 0) {
                    result.data.forEach(subj => {
                        const iconData = getSubjectIconClass(subj.name).split(' ');
                        const iconClass = iconData[0];
                        const iconColor = iconData[1];
                        const iconBg = iconData[2];

                        const card = document.createElement('div');
                        card.className = "bg-white p-3.5 sm:p-4 rounded-xl border border-slate-200 hover:border-brand-500 hover:shadow-md cursor-pointer transition flex items-center space-x-3";
                        card.onclick = () => selectSubject(subj.id, subj.name);
                        card.innerHTML = `
                            <div class="w-10 h-10 rounded-xl ${iconBg} flex items-center justify-center ${iconColor} text-base flex-shrink-0">
                                <i class="fa-solid ${iconClass}"></i>
                            </div>
                            <div class="min-w-0">
                                <h4 class="text-xs sm:text-sm font-bold text-slate-900 truncate">${subj.name}</h4>
                                <p class="text-[11px] text-slate-400 font-medium">${subj.paper_count} papers</p>
                            </div>
                        `;
                        grid.appendChild(card);
                    });
                }
            } catch (err) {
                console.error("Error loading subjects:", err);
            }
        }

        // 3. Subject Selection Handler
        function selectSubject(subjectId, subjectName) {
            currentSubjectId = subjectId;
            currentSubjectName = subjectName;

            const arrow = document.getElementById('breadcrumbArrow');
            const subjLabel = document.getElementById('currentSubjectLabel');
            const backBtn = document.getElementById('viewAllSubjectsBtn');

            if (subjectId) {
                arrow.classList.remove('hidden');
                subjLabel.classList.remove('hidden');
                subjLabel.innerText = subjectName;
                backBtn.classList.remove('hidden');
            } else {
                arrow.classList.add('hidden');
                subjLabel.classList.add('hidden');
                backBtn.classList.add('hidden');
            }

            loadPapers();
        }

        // 4. Load Papers matching filters
        async function loadPapers() {
            const container = document.getElementById('papersListContainer');
            const noMessage = document.getElementById('noPapersMessage');
            const countLabel = document.getElementById('resultsCount');

            const year = document.getElementById('yearFilter').value;
            const medium = document.getElementById('mediumFilter').value;
            const examType = document.getElementById('examTypeFilter').value;
            const search = document.getElementById('searchInput').value;

            container.innerHTML = `
                <div class="col-span-full py-12 text-center text-slate-400">
                    <i class="fa-solid fa-circle-notch fa-spin text-2xl text-brand-600 mb-2"></i>
                    <p class="text-sm font-medium">Fetching examination papers...</p>
                </div>
            `;
            noMessage.classList.add('hidden');

            try {
                let url = `api/papers.php?action=get_papers&grade_id=${currentGradeId}`;
                if (currentSubjectId) url += `&subject_id=${currentSubjectId}`;
                if (year !== 'All Years') url += `&year=${encodeURIComponent(year)}`;
                if (medium !== 'All Mediums') url += `&medium=${encodeURIComponent(medium)}`;
                if (examType !== 'All Types') url += `&exam_type=${encodeURIComponent(examType)}`;
                if (search.trim() !== '') url += `&search=${encodeURIComponent(search.trim())}`;

                const res = await fetch(url);
                const result = await res.json();

                container.innerHTML = '';

                if (result.status === 'success' && result.data.length > 0) {
                    countLabel.innerText = `${result.data.length} papers available`;

                    result.data.forEach(paper => {
                        const card = document.createElement('div');
                        card.className = "bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between";
                        
                        card.innerHTML = `
                            <div>
                                <!-- Badges Row -->
                                <div class="flex items-center justify-between mb-3.5">
                                    <div class="flex items-center space-x-2">
                                        <span class="px-2.5 py-1 text-xs font-extrabold bg-brand-100 text-brand-800 rounded-lg">${paper.year}</span>
                                        <span class="px-2.5 py-1 text-xs font-semibold bg-slate-100 text-slate-700 rounded-lg">${paper.exam_type}</span>
                                        <span class="px-2 py-1 text-xs font-medium text-slate-500">${paper.medium} Medium</span>
                                    </div>
                                    <span class="text-xs font-bold text-slate-400">${paper.pages || 14} pgs</span>
                                </div>

                                <!-- Title and PDF Icon -->
                                <div class="flex items-start space-x-3.5 mt-2">
                                    <div class="w-11 h-11 rounded-xl bg-red-50 text-red-600 flex items-center justify-center text-xl flex-shrink-0">
                                        <i class="fa-solid fa-file-pdf"></i>
                                    </div>
                                    <div>
                                        <span class="text-xs font-bold text-brand-600 uppercase tracking-wide">${paper.subject_name}</span>
                                        <h3 class="text-base font-bold text-slate-900 leading-snug mt-0.5 line-clamp-2">${paper.title}</h3>
                                        <p class="text-xs text-slate-500 mt-1">${paper.file_size || '3.2 MB'} • Official Curriculum Paper</p>
                                    </div>
                                </div>
                            </div>

                            <!-- Buttons (View & Download) -->
                            <div class="grid grid-cols-2 gap-2.5 mt-5 pt-4 border-t border-slate-100">
                                <a href="api/download.php?id=${paper.id}&mode=view" target="_blank"
                                   class="inline-flex items-center justify-center px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 font-bold text-xs sm:text-sm hover:bg-slate-50 transition">
                                    <i class="fa-solid fa-eye mr-2 text-slate-400"></i> View
                                </a>
                                <a href="api/download.php?id=${paper.id}&mode=download" 
                                   class="inline-flex items-center justify-center px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-bold text-xs sm:text-sm shadow-md shadow-brand-500/20 transition">
                                    <i class="fa-solid fa-download mr-2"></i> Download
                                </a>
                            </div>
                        `;
                        container.appendChild(card);
                    });
                } else {
                    countLabel.innerText = "0 papers found";
                    noMessage.classList.remove('hidden');
                }
            } catch (err) {
                console.error("Error loading papers:", err);
                container.innerHTML = `<div class="col-span-full py-8 text-center text-red-500 text-sm">Failed to connect to database. Please check config.php.</div>`;
            }
        }

        function applyFilters() {
            loadPapers();
        }

        function resetFilters() {
            document.getElementById('yearFilter').value = 'All Years';
            document.getElementById('mediumFilter').value = 'All Mediums';
            document.getElementById('examTypeFilter').value = 'All Types';
            document.getElementById('searchInput').value = '';
            loadPapers();
        }

        // Live search debounce
        let searchTimer;
        document.getElementById('searchInput').addEventListener('input', () => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                loadPapers();
            }, 300);
        });

        // Initialize on page load
        document.addEventListener('DOMContentLoaded', () => {
            loadSubjects();
            loadPapers();
        });
    </script>
</body>
</html>
