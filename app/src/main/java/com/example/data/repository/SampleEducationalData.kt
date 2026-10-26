package com.example.data.repository

import com.example.data.model.*

object SampleEducationalData {

    val pastPapers = listOf(
        PastPaper(
            id = "al-cm-2024",
            title = "G.C.E. A/L Combined Mathematics 2024",
            examination = "G.C.E. Advanced Level",
            subject = "Combined Mathematics",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            downloadCount = 14250,
            pages = 16,
            fileSize = "3.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Part A: 10 structured questions covering Trigonometry, Polynomials, Limits, Vectors, Integration. Part B: Mechanics & Pure Maths essays.",
            sampleQuestions = listOf(
                "Q1 (Pure): Solve 2sin²θ + 3cosθ = 0 for 0 ≤ θ ≤ 2π.",
                "Q2 (Pure): Using mathematical induction, prove that 1² + 2² + ... + n² = n(n+1)(2n+1)/6.",
                "Q3 (Applied): A projectile is fired with velocity u at angle α to horizontal. Find the maximum height and time of flight."
            ),
            markingSchemeNotes = listOf(
                "Mark distribution: Part A (250 marks), Part B (750 marks), scaled to 100.",
                "Method marks allocated for proper vector resolution diagrams.",
                "Complete step-by-step model answers included for all 5 Mechanics problems."
            )
        ),
        PastPaper(
            id = "al-cm-2023-sin",
            title = "G.C.E. A/L Combined Mathematics 2023 (සිංහල මාධ්‍යය)",
            examination = "G.C.E. Advanced Level",
            subject = "Combined Mathematics",
            year = 2023,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 29400,
            pages = 16,
            fileSize = "3.9 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2023 උසස් පෙළ සංයුක්ත ගණිතය I (ශුද්ධ ගණිතය) සහ II (ව්‍යවහාරික ගණිතය) සම්පූර්ණ ප්‍රශ්න පත්‍රය සහ නිල ලකුණු දීමේ පටිපාටිය.",
            sampleQuestions = listOf(
                "ප්‍රශ්නය 01 (ශුද්ධ ගණිතය): f(x) = x³ - 3x² + 2x ශ්‍රිතයේ ප්‍රස්ථාරයේ හැරවුම් ලක්ෂ්‍ය සහ අවනති ලක්ෂ්‍ය නිර්ණය කර ප්‍රස්ථාරය අඳින්න.",
                "ප්‍රශ්නය 02 (ශුද්ධ ගණිතය): ∫ (2x + 1) / (x² + 4x + 5) dx අනුකලනය ආංශික භාග හෝ ත්‍රිකෝණමිතික ආදේශය භාවිතයෙන් සොයන්න.",
                "ප්‍රශ්නය 03 (ව්‍යවහාරික ගණිතය): තිරසට α කෝණයකින් ආනත සුමට තලයක u ප්‍රවේගයෙන් ඉහළට ප්‍රක්ෂේපණය කරන අංශුවක චලිත සමීකරණ ලියා උපරිම උස ගණනය කරන්න.",
                "ප්‍රශ්නය 04 (ව්‍යවහාරික ගණිතය): m1 සහ m2 ස්කන්ධ සහිත අංශු දෙකක් සුමට කප්පියක් මතින් ගමන් කරන අවිතන්‍ය තන්තුවකින් සම්බන්ධ කර ඇත. පද්ධතියේ ත්වරණය සහ තන්තුවේ ආතතිය T සොයන්න."
            ),
            markingSchemeNotes = listOf(
                "I පත්‍රය (ශුද්ධ ගණිතය) ලකුණු 100 + II පත්‍රය (ව්‍යවහාරික ගණිතය) ලකුණු 100, සාමාන්‍යය 100 දක්වා පරිමාණය කෙරේ.",
                "අවකලනය හා අනුකලන ගැටළු සඳහා පියවර ලකුණු සහ ආදේශන ලකුණු නිවැරදිව පිරිනැමේ.",
                "චලිත ප්‍රස්ථාර සහ දෛශික සටහන් සඳහා සම්පූර්ණ ලකුණු ලබාදීමේ ක්‍රමවේදය."
            )
        ),
        PastPaper(
            id = "al-cm-2023-eng",
            title = "G.C.E. A/L Combined Mathematics 2023 (English Medium)",
            examination = "G.C.E. Advanced Level",
            subject = "Combined Mathematics",
            year = 2023,
            medium = "English",
            level = EducationLevel.SCHOOL,
            downloadCount = 21100,
            pages = 16,
            fileSize = "3.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "G.C.E. A/L 2023 Combined Mathematics Paper I (Pure Mathematics) and Paper II (Applied Mathematics) complete examination paper and marking criteria.",
            sampleQuestions = listOf(
                "Q1 (Pure Maths): Find stationary points of f(x) = x³ - 3x² + 2x and sketch the curve.",
                "Q2 (Pure Maths): Evaluate ∫ (2x + 1) / (x² + 4x + 5) dx using substitution.",
                "Q3 (Applied Maths): Projectile motion on an inclined plane with velocity u at angle α to the horizontal.",
                "Q4 (Applied Maths): Two masses connected by a light inextensible string over a smooth fixed pulley."
            ),
            markingSchemeNotes = listOf(
                "Full marks allocated for structured method steps and clear vector resolution diagrams.",
                "Calculus solutions: explicit substitution checks and boundary conditions evaluated."
            )
        ),
        PastPaper(
            id = "al-phy-2024",
            title = "G.C.E. A/L Physics 2024",
            examination = "G.C.E. Advanced Level",
            subject = "Physics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 18900,
            pages = 22,
            fileSize = "4.2 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Measurement, Mechanics, Oscillations & Waves, Thermal Physics, Gravitational & Electric Fields, Electronics & Modern Physics.",
            sampleQuestions = listOf(
                "Q1 (MCQ): A satellite orbits Earth at radius r with speed v. If radius is quadrupled, the new orbital speed is?",
                "Q2 (Structured): Calibration of a sonometer wire using tuning forks of standard frequencies.",
                "Q3 (Essay): Bernoulli's equation derivation and application to Venturi meter."
            ),
            markingSchemeNotes = listOf(
                "Detailed MCQs key with explanations for tricky distractor answers.",
                "Structured essay step marks: 2 marks for graph slope calculation, 3 marks for percentage uncertainty estimation."
            )
        ),
        PastPaper(
            id = "al-chem-2024",
            title = "G.C.E. A/L Chemistry 2024",
            examination = "G.C.E. Advanced Level",
            subject = "Chemistry",
            year = 2024,
            medium = "Tamil",
            level = EducationLevel.SCHOOL,
            downloadCount = 11200,
            pages = 18,
            fileSize = "3.4 MB",
            hasMarkingScheme = true,
            syllabusSummary = "General Chemistry, Inorganic Chemistry (s, p, d blocks), Organic Reaction Mechanisms, Physical Chemistry (Equilibrium, Kinetics).",
            sampleQuestions = listOf(
                "Q1: Write the balanced ionic equation for redox reaction between KMnO₄ and FeSO₄ in acidic medium.",
                "Q2: Explain why boiling point of ethanol is substantially higher than dimethyl ether.",
                "Q3: Calculate pH of buffer formed by 0.1M CH₃COOH and 0.1M CH₃COONa (Ka = 1.8 × 10⁻⁵)."
            ),
            markingSchemeNotes = listOf(
                "Complete IUPAC nomenclature rules applied for organic conversions.",
                "Step-by-step buffer calculation showing Henderson-Hasselbalch substitution."
            )
        ),
        PastPaper(
            id = "al-bio-2023",
            title = "G.C.E. A/L Biology 2023",
            examination = "G.C.E. Advanced Level",
            subject = "Biology",
            year = 2023,
            medium = "English",
            level = EducationLevel.SCHOOL,
            downloadCount = 16300,
            pages = 20,
            fileSize = "4.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Cell Biology, Plant Diversity & Physiology, Animal Form & Function, Genetics & Recombinant DNA, Ecology & Environmental Biology.",
            sampleQuestions = listOf(
                "Q1: Describe the light-dependent reactions of photosynthesis and ATP synthesis via chemiosmosis.",
                "Q2: Explain the regulatory feedback mechanism of thyroid hormones in human metabolism."
            ),
            markingSchemeNotes = listOf(
                "Key points checklist: Calvin cycle intermediates, stomatal mechanism."
            )
        ),
        PastPaper(
            id = "ol-math-2023",
            title = "G.C.E. O/L Mathematics 2023",
            examination = "G.C.E. Ordinary Level",
            subject = "Mathematics",
            year = 2023,
            medium = "English",
            level = EducationLevel.SCHOOL,
            downloadCount = 28400,
            pages = 12,
            fileSize = "2.9 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Paper I (Part A & B: Percentages, Fractions, Simple Interest, Geometry, Probability), Paper II (Algebraic graphs, Statistics, Trigonometry).",
            sampleQuestions = listOf(
                "Q1: Calculate annual compound interest for Rs. 50,000 at 8% per annum for 2 years.",
                "Q2: Draw the graph of y = x² - 4x + 3 and find the roots when y = 0."
            ),
            markingSchemeNotes = listOf(
                "Step marks for quadratic factoring and axis labeling."
            )
        ),
        PastPaper(
            id = "uom-dsa-2023",
            title = "CS2010 Data Structures & Algorithms 2023",
            examination = "University Semester Exam",
            subject = "Computer Science & Engineering",
            year = 2023,
            medium = "English",
            level = EducationLevel.UNDERGRADUATE,
            downloadCount = 8750,
            pages = 10,
            fileSize = "1.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Asymptotic Analysis, Balanced Trees (AVL, Red-Black), Graph Algorithms (Dijkstra, Prim, Kruskal), Dynamic Programming.",
            sampleQuestions = listOf(
                "Q1: Trace the Dijkstra algorithm on given 8-node directed weighted graph.",
                "Q2: Design an O(n log k) algorithm to merge k sorted linked lists of size n."
            ),
            markingSchemeNotes = listOf(
                "Full pseudocode and Big-O runtime breakdown."
            )
        ),
        PastPaper(
            id = "ucsc-db-2023",
            title = "SCS2201 Database Management Systems",
            examination = "University Semester Exam",
            subject = "Information Systems",
            year = 2023,
            medium = "English",
            level = EducationLevel.UNDERGRADUATE,
            downloadCount = 6400,
            pages = 8,
            fileSize = "1.4 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Relational Algebra, SQL queries, Normalization (1NF to BCNF), ACID transactions, Concurrency control.",
            sampleQuestions = listOf(
                "Q1: Decompose the given relation R(A,B,C,D,E) with functional dependencies into BCNF.",
                "Q2: Write complex SQL query with subqueries and window functions."
            ),
            markingSchemeNotes = listOf(
                "Lossless join decomposition proof and dependency preservation evaluation."
            )
        ),
        PastPaper(
            id = "pim-res-2024",
            title = "MBA 610 Advanced Research Methodology",
            examination = "Postgraduate Exam",
            subject = "Management Studies",
            year = 2024,
            medium = "English",
            level = EducationLevel.GRADUATE,
            downloadCount = 3900,
            pages = 6,
            fileSize = "1.1 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Epistemology, Quantitative vs Qualitative paradigms, Sampling designs, Structural Equation Modeling, Validity and Reliability.",
            sampleQuestions = listOf(
                "Q1: Critically evaluate the use of PLS-SEM versus CB-SEM in consumer behavior empirical studies.",
                "Q2: Formulate hypotheses and design a cross-sectional survey instrument."
            ),
            markingSchemeNotes = listOf(
                "Rubric criteria: Conceptual framework rigor, empirical construct justification."
            )
        ),
        PastPaper(
            id = "al-sft-2019-sin",
            title = "G.C.E. A/L Science for Technology (SFT) 2019 (සිංහල මාධ්‍යය)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2019,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 22400,
            pages = 14,
            fileSize = "3.2 MB",
            hasMarkingScheme = true,
            syllabusSummary = "ජෛව පද්ධති තාක්ෂණවේදය, භෞතික විද්‍යාව සහ මිනුම්, ඉලෙක්ට්‍රොනික විද්‍යාව, තාක්ෂණික ද්‍රව්‍ය හා පරිගණක මූලධර්ම ආවරණය වන සම්පූර්ණ ප්‍රශ්න පත්‍රය සහ ලකුණු දීමේ පටිපාටිය.",
            sampleQuestions = listOf(
                "ප්‍රශ්නය 01 (ව්‍යුහගත): ශාකවල ප්‍රභාසංස්ලේෂණයේ ආලෝක ප්‍රතික්‍රියාවේ දී ATP සහ NADPH නිපදවීමේ ක්‍රියාවලිය පැහැදිලි කරන්න. Photolysis ක්‍රියාවලිය ශාකයක පැවැත්මට බලපාන්නේ කෙසේද?",
                "ප්‍රශ්නය 02 (ව්‍යුහගත): ද්‍රවයක සාපේක්ෂ ඝනත්වය මැනීම සඳහා හයිඩ්‍රොමීටරයක් ක්‍රමාංකනය කිරීමේ පියවර සහ ආකිමිඩීස් මූලධර්මය ගණනය කිරීම් සඳහා යොදා ගන්නා ආකාරය දක්වන්න.",
                "ප්‍රශ්නය 03 (රචනා): සරල අර්ධ තරංග හා පූර්ණ තරංග ඩයෝඩ සෘජුකාරක පරිපථයක (Rectifier Circuit) ක්‍රියාකාරිත්වය සන්සන්දනය කරන්න. එහි ප්‍රතිදාන තරංග හැඩය වඩාත් සුමට කිරීම සඳහා පෙරහන් ධාරිත්‍රකයක් (Filter Capacitor) ක්‍රියාකරන ආකාරය පැහැදිලි කරන්න.",
                "ප්‍රශ්නය 04 (රචනා): ද්වීමය (Binary) සංඛ්‍යා පද්ධතිය සහ බූලීය වීජ ගණිතය යොදා ගනිමින් කාර්මික සංවේදක පාලන පද්ධතියක් සැලසුම් කරන ආකාරය තාර්කික ද්වාර (AND, OR, NOT) පරිපථ ඇසුරින් විස්තර කරන්න."
            ),
            markingSchemeNotes = listOf(
                "ලකුණු ලබාදීමේ ක්‍රමවේදය: I කොටස - බහුවරණ ප්‍රශ්න 50 (ලකුණු 50), II කොටස - ව්‍යුහගත රචනා (ලකුණු 50).",
                "ආලෝක ප්‍රතික්‍රියාවේ photolysis ක්‍රියාවලියේ දී ජල අණුව වියෝජනය වී ඉලෙක්ට්‍රෝන, ප්‍රෝටෝන සහ O2 නිදහස් වීම දැක්වීමට ලකුණු 3ක් හිමිවේ.",
                "සෘජුකාරක පරිපථයේ තරංග හැඩය (Waveform) සහ සෘජුකාරක සාධකය (Ripple Factor) සූත්‍රය ලිවීම සඳහා පියවර ලකුණු 4ක්.",
                "ද්වීමය පරිවර්තනය සහ සත්‍යතා වගුව (Truth Table) නිවැරදිව පිරීම සඳහා ලකුණු 4ක්."
            )
        ),
        PastPaper(
            id = "al-sft-2019-eng",
            title = "G.C.E. A/L Science for Technology (SFT) 2019 (English Medium)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2019,
            medium = "English",
            level = EducationLevel.SCHOOL,
            downloadCount = 17800,
            pages = 14,
            fileSize = "3.1 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Biosystems Technology, Applied Physics & Measurement, Electronics & Instrumentation, Material Science, and Computing Fundamentals.",
            sampleQuestions = listOf(
                "Q1 (Structured): Describe the generation of ATP and NADPH in the light-dependent reactions of photosynthesis. Explain the significance of photolysis in sustaining electron flow through Photosystem II.",
                "Q2 (Structured): Explain the working principle of a hydrometer based on Archimedes' Principle. Calculate the submerged depth of a hydrometer stem in a liquid of relative density 1.25.",
                "Q3 (Essay): Contrast half-wave versus full-wave bridge rectifier circuits with schematic diagrams. Explain the role of a reservoir capacitor filter in reducing voltage ripple factor.",
                "Q4 (Essay): Design a combinatorial logic circuit for an automated industrial tank level controller using basic logic gates (AND, OR, NOT). Construct the complete truth table and Karnaugh map simplification."
            ),
            markingSchemeNotes = listOf(
                "Mark allocation: Paper I (50 MCQs = 50 marks), Paper II (Structured & Essay = 50 marks, scaled).",
                "Full marks awarded for correct photolysis equation (2H2O -> 4H+ + 4e- + O2) and Z-scheme energy diagram.",
                "Waveform rectification: 2 marks for diode conduction cycle, 2 marks for capacitor discharge slope calculation.",
                "Digital logic: 2 marks for truth table verification, 3 marks for minimal Boolean SOP expression."
            )
        ),
        PastPaper(
            id = "al-sft-2019-tam",
            title = "G.C.E. A/L Science for Technology (SFT) 2019 (தமிழ் மொழி)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2019,
            medium = "Tamil",
            level = EducationLevel.SCHOOL,
            downloadCount = 14300,
            pages = 14,
            fileSize = "3.3 MB",
            hasMarkingScheme = true,
            syllabusSummary = "உயிரியல் அமைப்புகள் தொழில்நுட்பம், பிரயோக பௌதீகவியல், மின்னணுவியல் மற்றும் தொழினுட்ப பொருட்கள் பற்றிய முழுமையான பரீட்சை வினாத்தாள் மற்றும் புள்ளி வழங்கும் திட்டம்.",
            sampleQuestions = listOf(
                "வினா 01 (அமைப்புக்கட்டுரை): தாவரங்களின் ஒளிச்சேர்க்கையின் ஒளித்தாக்கத்தின் போது ATP மற்றும் NADPH தொகுக்கப்படும் பொறிமுறையை விளக்குக.",
                "வினா 02 (அமைப்புக்கட்டுரை): பாய்பொருட்களின் சாரடர்த்தியை அளவிட நீரியல்மானி (Hydrometer) எவ்வாறு ஆர்க்கிமிடிஸின் தத்துவத்தின் அடிப்படையில் தொழிற்படுகின்றது என்பதை விளக்குக.",
                "வினா 03 (கட்டுரை): அரை அலை மற்றும் பூரண அலைத் திருத்திகளின் (Rectifiers) தொழிற்பாட்டை சுற்று வரிப்படங்களுடன் ஒப்பிடுக. மின்தேக்கி வடிப்பட்டியின் பங்களிப்பை தெளிவுபடுத்துக.",
                "வினா 04 (கட்டுரை): எண்முறை தர்க்க வாயில்களை (AND, OR, NOT) பயன்படுத்தி தானியங்கி தொழில்துறை கட்டுப்பாட்டு அமைப்பை வடிவமைத்து உண்மை அட்டவணையை வரைக."
            ),
            markingSchemeNotes = listOf(
                "புள்ளி ஒதுக்கீடு: பகுதி I (50 பல்தேர்வு வினாக்கள்), பகுதி II (அமைப்பு மற்றும் கட்டுரைகள்).",
                "ஒளித்தாக்கத்தின் போது நீர் மூலக்கூறு பிரிகையடைதலை சரியாக குறிப்பிடுவதற்கு 3 புள்ளிகள்.",
                "அலைத் திருத்தியின் அலையுருவத்தை (Waveform) துல்லியமாக வரைவதற்கு 3 புள்ளிகள்."
            )
        ),
        PastPaper(
            id = "al-sft-2020-sin",
            title = "G.C.E. A/L Science for Technology (SFT) 2020 (සිංහල මාධ්‍යය)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2020,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 28900,
            pages = 16,
            fileSize = "3.6 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2020 අධ්‍යයන පොදු සහතික පත්‍ර උසස් පෙළ තාක්ෂණවේදය සඳහා විද්‍යාව (SFT) ප්‍රශ්න පත්‍රය. ජෛව පද්ධති තාක්ෂණය, පාංශු විද්‍යාව, තාප ගති විද්‍යාව, ධාරා විද්‍යුතය සහ ක්ෂුද්‍ර ජීව විද්‍යාත්මක මූලධර්ම සහ නිල ලකුණු දීමේ පටිපාටිය.",
            sampleQuestions = listOf(
                "ප්‍රශ්නය 01 (ව්‍යුහගත): ශාක පෝෂක ඌනතා හඳුනාගැනීම සඳහා ජල වගා (Hydroponics) මාධ්‍යයක pH අගය සහ විද්‍යුත් සන්නායකතාව (EC) නියාමනය කරන ආකාරය පැහැදිලි කරන්න.",
                "ප්‍රශ්නය 02 (ව්‍යුහගත): පරිපථයක අර්ධ සන්නායක තාප ප්‍රතිරෝධකයක (Thermistor) උෂ්ණත්වය අනුව ප්‍රතිරෝධය වෙනස් වීම ප්‍රස්තාරගත කර, ස්වයංක්‍රීය සිසිලන පද්ධතියක් සැලසුම් කරන්න.",
                "ප්‍රශ්නය 03 (රචනා): සත්ත්ව හා ශාක අපද්‍රව්‍ය මඟින් ජෛව වායුව (Biogas) නිපදවීමේ නිර්වායු ජීර්ණ ක්‍රියාවලියේ ප්‍රධාන අවධි 4 (ජලවිච්ඡේදනය, අම්ලජනනය, ඇසිටොජනනය, මෙතනොජනනය) සවිස්තරව විස්තර කරන්න.",
                "ප්‍රශ්නය 04 (රචනා): ද්‍රවස්ථිති පීඩනය P = hρg සූත්‍රය ව්‍යුත්පන්න කර, හයිඩ්‍රොලික් ජැක් එකක (Hydraulic Jack) බල වර්ධන අනුපාතය ගණනය කරන ආකාරය පැහැදිලි කරන්න."
            ),
            markingSchemeNotes = listOf(
                "I කොටස බහුවරණ ප්‍රශ්න 50 (ලකුණු 50) + II කොටස ව්‍යුහගත රචනා (ලකුණු 50).",
                "ජෛව වායු නිර්වායු ජීර්ණයේ මෙතනොජනක බැක්ටීරියා (Methanogenic bacteria) මඟින් CH4 සහ CO2 නිපදවීම විස්තර කිරීමට ලකුණු 4ක්.",
                "හයිඩ්‍රොලික් ජැක් ගණනය කිරීමේදී පැස්කල් නියමය (Pascal's Principle) F1/A1 = F2/A2 ආදේශ කිරීම සඳහා පියවර ලකුණු 3ක්."
            )
        ),
        PastPaper(
            id = "al-sft-2020-eng",
            title = "G.C.E. A/L Science for Technology (SFT) 2020 (English Medium)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2020,
            medium = "English",
            level = EducationLevel.SCHOOL,
            downloadCount = 21400,
            pages = 16,
            fileSize = "3.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "G.C.E. A/L 2020 Science for Technology examination paper covering Biosystems Technology, Soil Physics, Applied Thermodynamics, Sensors & Transducers, and Industrial Biotechnology.",
            sampleQuestions = listOf(
                "Q1 (Structured): Describe the regulation of pH and Electrical Conductivity (EC) in hydroponic nutrient solution management.",
                "Q2 (Structured): Plot the resistance-temperature characteristics of an NTC Thermistor and design a temperature-triggered transistor switching circuit.",
                "Q3 (Essay): Explain the 4 stages of anaerobic digestion in biogas generation (Hydrolysis, Acidogenesis, Acetogenesis, Methanogenesis).",
                "Q4 (Essay): Derive hydrostatic pressure P = hρg and calculate mechanical advantage in a hydraulic press system applying Pascal's Principle."
            ),
            markingSchemeNotes = listOf(
                "50 MCQs (50 marks) + Structured & Essay (50 marks).",
                "Full marks for identifying methanogenic bacterial role in converting acetic acid to methane and carbon dioxide.",
                "Correct application of Pascal's principle F1/A1 = F2/A2 and force multiplication derivation."
            )
        ),
        PastPaper(
            id = "al-sft-2020-tam",
            title = "G.C.E. A/L Science for Technology (SFT) 2020 (தமிழ் மொழி)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2020,
            medium = "Tamil",
            level = EducationLevel.SCHOOL,
            downloadCount = 18200,
            pages = 16,
            fileSize = "3.7 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2020 க.பொ.த உயர்தர தொழினுட்பவியல் பாடப்பரப்பிற்கான விஞ்ஞானம் வினாத்தாள் மற்றும் புள்ளி வழங்கும் திட்டம்.",
            sampleQuestions = listOf(
                "வினா 01 (அமைப்பு): நீர் ஊடக பயிர்ச்செய்கையில் (Hydroponics) pH மற்றும் மின்கடத்துதிறனை (EC) ஒழுங்குபடுத்தும் முறை.",
                "வினா 02 (அமைப்பு): NTC வெப்பமாறுமின்தடையின் வெப்பநிலை மாற்றங்களை வரைந்து விவரிக்கவும்.",
                "வினா 03 (கட்டுரை): உயிர்வாயு (Biogas) உற்பத்தியின் 4 நிலைகளை (நீர்ப்பகுப்பு, அமிலமாக்கல், அசிற்றோவாக்கம், மெத்தேனாக்கம்) விரிவாக விளக்குக."
            ),
            markingSchemeNotes = listOf(
                "பகுதி I பல்தேர்வு வினாக்கள் (50 புள்ளிகள்), பகுதி II அமைப்பு மற்றும் கட்டுரை (50 புள்ளிகள்)."
            )
        ),
        PastPaper(
            id = "al-sft-2021-sin",
            title = "G.C.E. A/L Science for Technology (SFT) 2021 (සිංහල මාධ්‍යය)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2021,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 26500,
            pages = 16,
            fileSize = "3.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2021 උසස් පෙළ තාක්ෂණවේදය සඳහා විද්‍යාව සම්පූර්ණ ප්‍රශ්න පත්‍රය සහ පිළිතුරු පත්‍රය.",
            sampleQuestions = listOf(
                "ප්‍රශ්නය 01: ආහාර කල්තබා ගැනීමේ තාක්ෂණික ක්‍රම (Pasteurization, Sterilization, Dehydration) සන්සන්දනය කරන්න.",
                "ප්‍රශ්නය 02: සූර්ය කෝෂ (Solar Photovoltaic) පද්ධතියක කාර්යක්ෂමතාව ගණනය කර උපරිම බල ලක්ෂ්‍යය (MPPT) පැහැදිලි කරන්න."
            ),
            markingSchemeNotes = listOf(
                "ආහාර විෂබීජහරණය සහ සූර්ය කෝෂ ගණනය කිරීම් සඳහා සම්පූර්ණ ලකුණු ලබාදීමේ උපදෙස්."
            )
        ),
        PastPaper(
            id = "al-sft-2022-sin",
            title = "G.C.E. A/L Science for Technology (SFT) 2022 (සිංහල මාධ්‍යය)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2022,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 27800,
            pages = 16,
            fileSize = "3.9 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2022 උසස් පෙළ SFT ප්‍රශ්න පත්‍රය, ව්‍යුහගත රචනා ප්‍රශ්න සහ සම්පූර්ණ ලකුණු පටිපාටිය.",
            sampleQuestions = listOf(
                "ප්‍රශ්නය 01: ස්වයංක්‍රීය වාරිමාර්ග පද්ධතියක පසෙහි තෙතමනය මනින සංවේදක පරිපථයක ක්‍රියාකාරිත්වය.",
                "ප්‍රශ්නය 02: එන්සයිම ප්‍රතික්‍රියා සීඝ්‍රතාව කෙරෙහි උෂ්ණත්වය සහ උපස්ථර සාන්ද්‍රණය බලපාන ආකාරය."
            ),
            markingSchemeNotes = listOf(
                "එන්සයිම kinetics සහ සංවේදක ක්‍රමාංකන ප්‍රස්ථාර සඳහා පියවර ලකුණු."
            )
        ),
        PastPaper(
            id = "al-sft-2023-sin",
            title = "G.C.E. A/L Science for Technology (SFT) 2023 (සිංහල මාධ්‍යය)",
            examination = "G.C.E. Advanced Level",
            subject = "Science for Technology",
            year = 2023,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            downloadCount = 31200,
            pages = 16,
            fileSize = "4.0 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2023 උසස් පෙළ SFT ප්‍රශ්න පත්‍රය සහ සම්පූර්ණ ලකුණු ලබාදීමේ නිර්ණායක.",
            sampleQuestions = listOf(
                "ප්‍රශ්නය 01: නැනෝ තාක්ෂණික යෙදවුම් කෘෂිකර්මාන්තය සහ පරිසර සංරක්ෂණය සඳහා යොදාගන්නා ආකාරය.",
                "ප්‍රශ්නය 02: ක්ෂුද්‍ර පාලක (Microcontroller) සහ IoT පද්ධති මඟින් හරිතාගාර පාලනය කිරීම."
            ),
            markingSchemeNotes = listOf(
                "IoT හරිතාගාර පාලන පරිපථය සහ නැනෝ යෙදවුම් සඳහා සම්පූර්ණ විවරණ ලකුණු."
            )
        ),
        // Grade 6
        PastPaper(
            id = "g6-math-2024",
            title = "Grade 6 Mathematics 3rd Term Test 2024 (ගණිතය)",
            examination = "School Term Test",
            subject = "Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 6",
            examType = "School Term Test",
            downloadCount = 8400,
            pages = 6,
            fileSize = "1.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Whole numbers, basic operations, fractions, decimals, perimeter, and area.",
            sampleQuestions = listOf("Q1: Simplify 3/4 + 1/2", "Q2: Find perimeter of rectangle 8cm x 5cm"),
            markingSchemeNotes = listOf("Complete step marks for calculation and units.")
        ),
        PastPaper(
            id = "g6-sci-2024",
            title = "Grade 6 Science Term Evaluation 2024 (විද්‍යාව)",
            examination = "School Term Test",
            subject = "Science",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 6",
            examType = "School Term Test",
            downloadCount = 6200,
            pages = 6,
            fileSize = "1.4 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Living world, matter, energy, states of water, and plant diversity.",
            sampleQuestions = listOf("Q1: Name 3 differences between living and non-living things."),
            markingSchemeNotes = listOf("Model answers with diagram labeling keys.")
        ),
        // Grade 7
        PastPaper(
            id = "g7-math-2024",
            title = "Grade 7 Mathematics Year-End Paper 2024",
            examination = "School Term Test",
            subject = "Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 7",
            examType = "School Term Test",
            downloadCount = 7800,
            pages = 8,
            fileSize = "1.6 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Directed numbers, simple equations, angles, percentages, and volume.",
            sampleQuestions = listOf("Q1: Solve 2x + 5 = 19", "Q2: Calculate 15% of 600."),
            markingSchemeNotes = listOf("Step-by-step mark distribution included.")
        ),
        PastPaper(
            id = "g7-sci-2024",
            title = "Grade 7 Science 2nd Term Evaluation 2024",
            examination = "School Term Test",
            subject = "Science",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 7",
            examType = "School Term Test",
            downloadCount = 5900,
            pages = 7,
            fileSize = "1.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Photosynthesis, heat transfer, electric circuits, and animal groups.",
            sampleQuestions = listOf("Q1: Explain conduction, convection, and radiation with examples."),
            markingSchemeNotes = listOf("Full structured marking breakdown.")
        ),
        // Grade 8
        PastPaper(
            id = "g8-math-2024",
            title = "Grade 8 Mathematics Provincial Evaluation 2024",
            examination = "School Term Test",
            subject = "Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 8",
            examType = "School Term Test",
            downloadCount = 9200,
            pages = 8,
            fileSize = "1.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Algebraic expressions, linear graphs, Pythagoras theorem, ratios and rates.",
            sampleQuestions = listOf("Q1: Using Pythagoras theorem, find hypotenuse of triangle with legs 6cm & 8cm."),
            markingSchemeNotes = listOf("Geometry reasoning criteria and final unit marks.")
        ),
        PastPaper(
            id = "g8-eng-2024",
            title = "Grade 8 English Language Assessment 2024",
            examination = "School Term Test",
            subject = "English",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 8",
            examType = "School Term Test",
            downloadCount = 7400,
            pages = 6,
            fileSize = "1.2 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Grammar, reading comprehension, vocabulary, essay and informal letter writing.",
            sampleQuestions = listOf("Q1: Write an essay on 'My Favorite Hobby' (120 words)."),
            markingSchemeNotes = listOf("Grammar (5), vocabulary (5), organization (5) rubric.")
        ),
        // Grade 9
        PastPaper(
            id = "g9-math-2024",
            title = "Grade 9 Mathematics Final Assessment 2024",
            examination = "School Term Test",
            subject = "Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 9",
            examType = "School Term Test",
            downloadCount = 10500,
            pages = 10,
            fileSize = "2.0 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Simultaneous equations, circle theorems, quadratic expressions, probability.",
            sampleQuestions = listOf("Q1: Solve simultaneous equations: 3x + y = 11 and x - y = 1."),
            markingSchemeNotes = listOf("Full algebraic method marks.")
        ),
        PastPaper(
            id = "g9-sci-2024",
            title = "Grade 9 Science 3rd Term Paper 2024",
            examination = "School Term Test",
            subject = "Science",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 9",
            examType = "School Term Test",
            downloadCount = 8800,
            pages = 8,
            fileSize = "1.7 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Chemical bonding, acids & bases, Newton's laws of motion, human organ systems.",
            sampleQuestions = listOf("Q1: State Newton's second law and derive F = ma."),
            markingSchemeNotes = listOf("Full marking scheme with essay rubric.")
        ),
        // Grade 10
        PastPaper(
            id = "g10-math-2024",
            title = "Grade 10 Mathematics Term Test 2024 (ගණිතය)",
            examination = "School Term Test",
            subject = "Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 10",
            examType = "School Term Test",
            downloadCount = 14800,
            pages = 12,
            fileSize = "2.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Logarithms, quadratic equations, circle theorems, statistics, indices.",
            sampleQuestions = listOf("Q1: Solve log₂(x + 3) = 4", "Q2: Find roots of 2x² - 5x + 2 = 0."),
            markingSchemeNotes = listOf("Step-by-step marking scheme for quadratic formula.")
        ),
        PastPaper(
            id = "g10-sci-2024",
            title = "Grade 10 Science Provincial Model Paper 2024",
            examination = "Model Paper",
            subject = "Science",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 10",
            examType = "Model Paper",
            downloadCount = 12900,
            pages = 12,
            fileSize = "2.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Periodic table, chemical calculations, current electricity, genetics, friction.",
            sampleQuestions = listOf("Q1: Calculate equivalent resistance of two 6Ω resistors in parallel."),
            markingSchemeNotes = listOf("Circuit diagram analysis and point allocations.")
        ),
        // Grade 11 – O/L
        PastPaper(
            id = "ol-math-2025-model",
            title = "2025 O/L Mathematics Model Examination Paper (ගණිතය)",
            examination = "G.C.E. Ordinary Level",
            subject = "Mathematics",
            year = 2025,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 34500,
            pages = 14,
            fileSize = "3.2 MB",
            hasMarkingScheme = true,
            syllabusSummary = "2025 G.C.E. O/L Model Examination Paper I & II with complete answer schemes.",
            sampleQuestions = listOf("Q1: A cone of radius 7cm and height 24cm. Calculate its curved surface area."),
            markingSchemeNotes = listOf("Official O/L marking scheme model answers.")
        ),
        PastPaper(
            id = "ol-math-2024-real",
            title = "2024 O/L Mathematics Past Paper (ගණිතය)",
            examination = "G.C.E. Ordinary Level",
            subject = "Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 42100,
            pages = 14,
            fileSize = "3.4 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Official 2024 G.C.E. O/L Mathematics Paper I and Paper II with marking scheme.",
            sampleQuestions = listOf("Q1: Solve 3x² + 7x - 6 = 0 using formula.", "Q2: Calculate income tax for annual salary."),
            markingSchemeNotes = listOf("Complete marking scheme steps with method and accuracy marks.")
        ),
        PastPaper(
            id = "ol-math-2022-real",
            title = "2022 O/L Mathematics Past Paper",
            examination = "G.C.E. Ordinary Level",
            subject = "Mathematics",
            year = 2022,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 31000,
            pages = 12,
            fileSize = "2.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "G.C.E. O/L 2022 Mathematics Paper I & II in English medium.",
            sampleQuestions = listOf("Q1: Trigonometry angle of elevation calculation.", "Q2: Probability tree diagram problem."),
            markingSchemeNotes = listOf("Full English medium answer key and marking guidelines.")
        ),
        PastPaper(
            id = "ol-sci-2024",
            title = "2024 O/L Science Past Paper & Scheme (විද්‍යාව)",
            examination = "G.C.E. Ordinary Level",
            subject = "Science",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 38900,
            pages = 14,
            fileSize = "3.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Complete 2024 O/L Science Paper I (MCQ 40) and Paper II (Structured & Essay).",
            sampleQuestions = listOf("Q1: Write the chemical equation for extraction of iron in the blast furnace."),
            markingSchemeNotes = listOf("Comprehensive breakdown of marks for all 40 MCQs and essays.")
        ),
        PastPaper(
            id = "ol-eng-2024",
            title = "2024 O/L English Language Examination Paper",
            examination = "G.C.E. Ordinary Level",
            subject = "English",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 31200,
            pages = 10,
            fileSize = "2.1 MB",
            hasMarkingScheme = true,
            syllabusSummary = "G.C.E. O/L English Paper I & II (Reading, Writing, Vocabulary, Grammar).",
            sampleQuestions = listOf("Q1: Write a formal letter of complaint to the municipal council regarding road repairs."),
            markingSchemeNotes = listOf("Official 15-mark essay and letter writing rubric.")
        ),
        PastPaper(
            id = "ol-ict-2024",
            title = "2024 O/L Information & Communication Technology (ICT)",
            examination = "G.C.E. Ordinary Level",
            subject = "ICT",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 26700,
            pages = 12,
            fileSize = "2.6 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Data representation, logic gates, operating systems, spreadsheets, scratch programming.",
            sampleQuestions = listOf("Q1: Convert decimal number 45 to 8-bit binary.", "Q2: Draw logic circuit for (A AND B) OR NOT C."),
            markingSchemeNotes = listOf("Truth table verification and binary conversion marks.")
        ),
        PastPaper(
            id = "ol-his-2024",
            title = "2024 O/L History Past Paper & Answers (ඉතිහාසය)",
            examination = "G.C.E. Ordinary Level",
            subject = "History",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 24100,
            pages = 12,
            fileSize = "2.7 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Ancient hydraulic civilizations, colonial era, constitutional reforms, and modern Sri Lanka.",
            sampleQuestions = listOf("Q1: Describe King Parakramabahu I's contributions to agriculture and foreign policy."),
            markingSchemeNotes = listOf("Structured historical essay evaluation marks.")
        ),
        PastPaper(
            id = "ol-rel-2024",
            title = "2024 O/L Buddhism Paper (බුද්ධ ධර්මය)",
            examination = "G.C.E. Ordinary Level",
            subject = "Religion",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 11 – O/L",
            examType = "O/L",
            downloadCount = 19500,
            pages = 10,
            fileSize = "2.2 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Four Noble Truths, Buddhist culture, history of the Sasana in Sri Lanka.",
            sampleQuestions = listOf("Q1: Explain the practical value of the Eightfold Path in contemporary society."),
            markingSchemeNotes = listOf("Doctrinal and practical application scoring points.")
        ),
        // Grade 12 – A/L
        PastPaper(
            id = "al-g12-comb-math-2025",
            title = "2025 Grade 12 A/L Combined Mathematics Term Paper",
            examination = "School Term Test",
            subject = "Combined Mathematics",
            year = 2025,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 12 – A/L",
            examType = "School Term Test",
            downloadCount = 21000,
            pages = 12,
            fileSize = "3.1 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Trigonometric functions, polynomials, limits, differentiation, vector statics.",
            sampleQuestions = listOf("Q1: Differentiate y = (sin x) / (1 + cos x) with respect to x."),
            markingSchemeNotes = listOf("First principles and derivative formula steps.")
        ),
        PastPaper(
            id = "al-g12-phy-2024",
            title = "2024 Grade 12 A/L Physics Mid-Year Assessment",
            examination = "School Term Test",
            subject = "Physics",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 12 – A/L",
            examType = "School Term Test",
            downloadCount = 19400,
            pages = 14,
            fileSize = "3.2 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Units and measurements, kinematics, circular motion, work, energy and power.",
            sampleQuestions = listOf("Q1: A car rounds a banked curve of radius 50m at 20 m/s. Find the ideal banking angle."),
            markingSchemeNotes = listOf("Free body diagram allocation (3 marks) and trigonometry resolution.")
        ),
        PastPaper(
            id = "al-g12-chem-2024",
            title = "2024 Grade 12 A/L Chemistry School Evaluation",
            examination = "School Term Test",
            subject = "Chemistry",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 12 – A/L",
            examType = "School Term Test",
            downloadCount = 18100,
            pages = 12,
            fileSize = "2.9 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Atomic structure, chemical bonding, s and p block chemistry, stoichiometry.",
            sampleQuestions = listOf("Q1: Explain anomalous properties of lithium compared to other group 1 elements."),
            markingSchemeNotes = listOf("Electronegativity, polarization power, and lattice energy points.")
        ),
        // Grade 13 – A/L
        PastPaper(
            id = "al-g13-cm-2024",
            title = "2024 A/L Combined Mathematics Past Paper & Scheme",
            examination = "G.C.E. Advanced Level",
            subject = "Combined Mathematics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 45200,
            pages = 18,
            fileSize = "4.1 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Complete 2024 G.C.E. A/L Combined Maths Paper I (Pure) and Paper II (Applied).",
            sampleQuestions = listOf("Q1: Complex numbers loci problem.", "Q2: Work-energy theorem for particle on rough sphere."),
            markingSchemeNotes = listOf("Official Department of Examinations mark allocation guide.")
        ),
        PastPaper(
            id = "al-g13-phy-2024-real",
            title = "2024 A/L Physics Past Paper & Marking Scheme",
            examination = "G.C.E. Advanced Level",
            subject = "Physics",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 41300,
            pages = 22,
            fileSize = "4.4 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Complete 2024 A/L Physics Paper I & II with marking scheme breakdown.",
            sampleQuestions = listOf("Q1: Optical fiber total internal reflection.", "Q2: Radioactive decay rate law derivation."),
            markingSchemeNotes = listOf("Structured question marking points and MCQ explanations.")
        ),
        PastPaper(
            id = "al-g13-chem-2024",
            title = "2024 A/L Chemistry National Past Paper & Scheme",
            examination = "G.C.E. Advanced Level",
            subject = "Chemistry",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 37200,
            pages = 20,
            fileSize = "4.5 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Organic synthesis pathways, chemical equilibria, d-block transition metals, electrochemistry.",
            sampleQuestions = listOf("Q1: Synthesize benzoic acid starting from benzene using Grignard reagent."),
            markingSchemeNotes = listOf("Full reaction step reagents and conditions.")
        ),
        PastPaper(
            id = "al-g13-bio-2024",
            title = "2024 A/L Biology Examination Paper (English Medium)",
            examination = "G.C.E. Advanced Level",
            subject = "Biology",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 28900,
            pages = 22,
            fileSize = "4.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Genetics, human reproduction, neurobiology, recombinant DNA technology, biosphere.",
            sampleQuestions = listOf("Q1: Describe PCR (Polymerase Chain Reaction) steps and importance in medical diagnostics."),
            markingSchemeNotes = listOf("Step-by-step marking guide: Denaturation, Annealing, Extension.")
        ),
        PastPaper(
            id = "al-g13-acc-2024",
            title = "2024 A/L Accounting Past Paper & Scheme (ගිණුම්කරණය)",
            examination = "G.C.E. Advanced Level",
            subject = "Accounting",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 24600,
            pages = 18,
            fileSize = "3.8 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Company financial statements, cash flow statements, ratio analysis, cost accounting.",
            sampleQuestions = listOf("Q1: Prepare Statement of Profit or Loss and Other Comprehensive Income for LK plc."),
            markingSchemeNotes = listOf("Comprehensive double-entry adjustment markings.")
        ),
        PastPaper(
            id = "al-g13-econ-2024",
            title = "2024 A/L Economics Past Paper & Marking Scheme",
            examination = "G.C.E. Advanced Level",
            subject = "Economics",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 21800,
            pages = 16,
            fileSize = "3.3 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Macroeconomic policy, inflation, exchange rate regimes, international trade, fiscal policy.",
            sampleQuestions = listOf("Q1: Explain how contractionary monetary policy controls demand-pull inflation using diagrams."),
            markingSchemeNotes = listOf("Diagrammatic clarity (4 marks) and policy transmission mechanisms.")
        ),
        PastPaper(
            id = "al-g13-bs-2024",
            title = "2024 A/L Business Studies Examination Paper (ව්‍යාපාර අධ්‍යයනය)",
            examination = "G.C.E. Advanced Level",
            subject = "Business Studies",
            year = 2024,
            medium = "Sinhala",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 22300,
            pages = 16,
            fileSize = "3.4 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Business environment, entrepreneurship, marketing management, human resource management.",
            sampleQuestions = listOf("Q1: Analyze the marketing mix (4Ps) strategy for a new Sri Lankan consumer product."),
            markingSchemeNotes = listOf("Strategic analysis rubric and real-world case evaluation marks.")
        ),
        PastPaper(
            id = "al-g13-ict-2024",
            title = "2024 A/L Information & Communication Technology (ICT)",
            examination = "G.C.E. Advanced Level",
            subject = "ICT",
            year = 2024,
            medium = "English",
            level = EducationLevel.SCHOOL,
            grade = "Grade 13 – A/L",
            examType = "A/L",
            downloadCount = 33100,
            pages = 18,
            fileSize = "3.9 MB",
            hasMarkingScheme = true,
            syllabusSummary = "Python programming, SQL databases, networking, IoT, web technologies.",
            sampleQuestions = listOf("Q1: Write Python script to read CSV file, compute average student marks, and output sorted rankings."),
            markingSchemeNotes = listOf("Clean code syntax and time complexity evaluations.")
        ),
        PastPaper(
            id = "al-timetable-2026",
            title = "Official G.C.E. A/L Examination Timetable 2026 (විභාග කාලසටහන)",
            examination = "G.C.E. Advanced Level",
            subject = "All Streams Timetable",
            year = 2026,
            medium = "Sinhala & English",
            level = EducationLevel.SCHOOL,
            downloadCount = 38500,
            pages = 4,
            fileSize = "1.2 MB",
            hasMarkingScheme = false,
            syllabusSummary = "Official National Examination Schedule for G.C.E. A/L 2026. Morning Session (08:30 AM - 11:40 AM) & Afternoon Session (01:00 PM - 04:10 PM) across Physical, Biological, Technology, Commerce, and Arts streams.",
            sampleQuestions = listOf(
                "Day 01 (Mon, Nov 23): Morning - Combined Mathematics I (Pure) / Biology I (MCQ) | Afternoon - General English / Sinhala / Tamil Paper I",
                "Day 02 (Tue, Nov 24): Morning - Combined Mathematics II (Applied) / Biology II | Afternoon - Agricultural Science Paper I",
                "Day 03 (Wed, Nov 25): Morning - Physics I (MCQ) / Accounting I | Afternoon - Information & Communication Technology (ICT) I",
                "Day 04 (Thu, Nov 26): Morning - Physics II / Accounting II | Afternoon - Information & Communication Technology (ICT) II",
                "Day 05 (Fri, Nov 27): Morning - Science for Technology (SFT) I (MCQ) | Afternoon - Engineering Technology (ET) I / Biosystems I",
                "Day 06 (Mon, Nov 30): Morning - Science for Technology (SFT) II (Structured & Essay) | Afternoon - Engineering Technology (ET) II / Biosystems II",
                "Day 07 (Tue, Dec 01): Morning - Chemistry I (MCQ) / Business Studies I | Afternoon - Political Science / Geography",
                "Day 08 (Wed, Dec 02): Morning - Chemistry II / Business Studies II | Afternoon - Logic & Scientific Method / Media Studies",
                "Day 09 (Thu, Dec 03): Morning - Economics I (MCQ) / Higher Mathematics I | Afternoon - Home Economics / Art",
                "Day 10 (Fri, Dec 04): Morning - Economics II / Common General Test | Afternoon - Examination Conclusion & Practical Briefing"
            ),
            markingSchemeNotes = listOf(
                "Morning sessions commence sharply at 08:30 AM (Candidates must be seated by 08:15 AM).",
                "10 minutes additional reading time is allocated for all question papers prior to writing.",
                "Admission cards and National Identity Cards (NIC) are strictly mandatory at examination centers."
            )
        )
    )

    val courses = listOf(
        Course(
            id = "c-al-math-mechanics",
            title = "A/L Mechanics Mastery: Vectors to Projectiles",
            instructor = "Prof. Senaka Jayatillake",
            institution = "University of Colombo / TheStudent Academy",
            level = EducationLevel.SCHOOL,
            category = "Physical Science",
            durationHours = 24,
            totalLessons = 16,
            completedLessons = 5,
            rating = 4.9f,
            studentsCount = 5420,
            description = "A comprehensive deep dive into Sri Lankan A/L Combined Mathematics Mechanics syllabus with past paper model breakdowns.",
            lessons = listOf(
                Lesson("l1", 1, "Vector Algebra & Relative Velocity", 45, true, "Unit vectors, dot and cross products, river-crossing problems."),
                Lesson("l2", 2, "Rectilinear Motion with Uniform Acceleration", 50, true, "Motion graphs (v-t curves), calculus approach to kinematic equations."),
                Lesson("l3", 3, "Projectile Motion on Inclined Planes", 60, true, "Trajectory equations, maximum range up and down the incline."),
                Lesson("l4", 4, "Newton's Laws of Motion & Connected Particles", 55, true, "Pulleys, friction, inclined blocks, system constraint relations."),
                Lesson("l5", 5, "Work, Energy, Power & Impulse", 45, true, "Conservation of mechanical energy, coefficient of restitution e."),
                Lesson("l6", 6, "Circular Motion & Conical Pendulum", 50, false, "Centripetal acceleration, banking of curved roads and tracks."),
                Lesson("l7", 7, "Equilibrium of Rigid Bodies", 60, false, "Taking moments, three-force concurrent system, limiting friction."),
                Lesson("l8", 8, "Frameworks & Jointed Rods", 45, false, "Method of joints, Bow's notation, stress in structural members.")
            )
        ),
        Course(
            id = "c-al-organic-chem",
            title = "Organic Chemistry: Complete Reaction Roadmap",
            instructor = "Dr. Manjula Wickramasinghe",
            institution = "Institute of Chemistry Ceylon",
            level = EducationLevel.SCHOOL,
            category = "Chemistry",
            durationHours = 18,
            totalLessons = 12,
            completedLessons = 2,
            rating = 4.8f,
            studentsCount = 6800,
            description = "Master all 30+ organic conversions, electrophilic additions, nucleophilic substitutions, and Grignard reagent synthesis for A/L Chemistry.",
            lessons = listOf(
                Lesson("oc1", 1, "Hydrocarbons: Alkanes, Alkenes & Alkynes", 40, true, "Markovnikov addition, ozonolysis, hydrogenation."),
                Lesson("oc2", 2, "Alkyl Halides & Nucleophilic Substitution", 45, true, "SN1 vs SN2 mechanisms, elimination reactions."),
                Lesson("oc3", 3, "Alcohols, Phenols & Ethers", 50, false, "Lucas reagent test, oxidation pathways, esterification."),
                Lesson("oc4", 4, "Carbonyl Compounds: Aldehydes & Ketones", 55, false, "2,4-DNP test, Tollens & Fehling tests, aldol condensation."),
                Lesson("oc5", 5, "Carboxylic Acids & Derivatives", 50, false, "Acyl chlorides, amides, saponification, decarboxylation.")
            )
        ),
        Course(
            id = "c-cs-dsa-modern",
            title = "Data Structures & Modern Algorithms in Kotlin",
            instructor = "Eng. Dilshan Silva",
            institution = "University of Moratuwa",
            level = EducationLevel.UNDERGRADUATE,
            category = "Computer Science",
            durationHours = 32,
            totalLessons = 20,
            completedLessons = 8,
            rating = 4.95f,
            studentsCount = 4150,
            description = "Designed for CSE and IT undergraduates in Sri Lanka preparing for top tech internships and competitive programming.",
            lessons = listOf(
                Lesson("dsa1", 1, "Big-O Notation & Memory Analysis", 35, true, "Time and space complexity profiling, amortized analysis."),
                Lesson("dsa2", 2, "Dynamic Arrays, Linked Lists & Stacks", 45, true, "Cache locality, pointer manipulation, monotonic stacks."),
                Lesson("dsa3", 3, "Trees & Binary Search Trees (BST)", 50, true, "Tree traversals (inorder, preorder, postorder, level order)."),
                Lesson("dsa4", 4, "Graph Representations & BFS/DFS", 55, false, "Adjacency list, cycle detection, topological sorting."),
                Lesson("dsa5", 5, "Dynamic Programming on Grids & Strings", 60, false, "Memoization, tabulations, Longest Common Subsequence.")
            )
        ),
        Course(
            id = "c-mba-financial-strategy",
            title = "Corporate Financial Strategy & Valuation",
            instructor = "Prof. Rohana Kumara",
            institution = "Postgraduate Institute of Management",
            level = EducationLevel.GRADUATE,
            category = "Business & Finance",
            durationHours = 20,
            totalLessons = 10,
            completedLessons = 0,
            rating = 4.85f,
            studentsCount = 1890,
            description = "Advanced financial modeling, DCF valuations, capital budgeting, and Colombo Stock Exchange (CSE) case studies.",
            lessons = listOf(
                Lesson("fin1", 1, "Time Value of Money & Discounted Cash Flows", 40, false, "WACC calculation, Net Present Value, IRR criteria."),
                Lesson("fin2", 2, "Financial Statement Analysis & Ratio Modeling", 50, false, "DuPont analysis, cash conversion cycle, debt covenants."),
                Lesson("fin3", 3, "Company Valuation & M&A Strategy", 60, false, "Comparable company analysis, terminal value estimation.")
            )
        )
    )

    val studyNotes = listOf(
        StudyNote(
            id = "n-al-phys-formula",
            title = "A/L Physics Complete Formula & Unit Sheet",
            subject = "Physics",
            level = EducationLevel.SCHOOL,
            author = "Kavindu Perera (Island 4th Rank)",
            institutionOrSchool = "Royal College",
            downloads = 21400,
            likes = 1850,
            readTimeMinutes = 15,
            tags = listOf("Physics", "Quick Revision", "Formulas", "SI Units"),
            contentSummary = "Concise summary of all formula relations across Mechanics, Waves, Fields, and Electronics with SI units and common traps."
        ),
        StudyNote(
            id = "n-al-math-integration",
            title = "Pure Maths 30 Standard Integration Techniques",
            subject = "Combined Mathematics",
            level = EducationLevel.SCHOOL,
            author = "Ashan De Silva",
            institutionOrSchool = "Ananda College",
            downloads = 17800,
            likes = 1420,
            readTimeMinutes = 20,
            tags = listOf("Maths", "Calculus", "Integration", "Part B"),
            contentSummary = "Integration by parts, partial fractions, trigonometric substitutions, and reduction formulas with solved Sri Lankan past paper steps."
        ),
        StudyNote(
            id = "n-ug-operating-systems",
            title = "Operating Systems: Concurrency & Virtual Memory",
            subject = "Computer Science",
            level = EducationLevel.UNDERGRADUATE,
            author = "Chathura Bandara",
            institutionOrSchool = "University of Moratuwa",
            downloads = 6300,
            likes = 780,
            readTimeMinutes = 25,
            tags = listOf("CSE", "Deadlocks", "Paging", "Semaphores"),
            contentSummary = "Clear diagrams on virtual memory page translation, TLB hits/misses, dining philosophers problem, and mutex implementation."
        )
    )

    val vocabularyWords = listOf(
        VocabularyWord(
            word = "Hypothesis",
            phonetic = "/haɪˈpɒθ.ə.sɪs/",
            translationSinhala = "ප්‍රවාදය / උපකල්පනය",
            translationTamil = "கருதுகோள்",
            definition = "A proposed explanation made on the basis of limited evidence as a starting point for further investigation.",
            exampleSentence = "The researchers formulated a clear hypothesis before conducting the laboratory experiment.",
            category = "Academic & Research"
        ),
        VocabularyWord(
            word = "Synthesize",
            phonetic = "/ˈsɪn.θə.saɪz/",
            translationSinhala = "සංස්ලේෂණය කරනවා",
            translationTamil = "தொகுத்து உருவாக்குதல்",
            definition = "To combine various components or concepts into a coherent, structured whole.",
            exampleSentence = "In your university literature review, synthesize arguments from diverse peer-reviewed papers.",
            category = "Academic Writing"
        ),
        VocabularyWord(
            word = "Pivotal",
            phonetic = "/ˈpɪv.ə.təl/",
            translationSinhala = "ඉතා වැදගත් / තීරණාත්මක",
            translationTamil = "மிக முக்கியமான",
            definition = "Of crucial importance in relation to the development or success of something else.",
            exampleSentence = "Securing good marks in A/L Physics was pivotal to getting selected for Engineering.",
            category = "General English"
        ),
        VocabularyWord(
            word = "Methodology",
            phonetic = "/ˌmeθ.əˈdɒl.ə.dʒi/",
            translationSinhala = "ක්‍රමවේදය",
            translationTamil = "முறைமை / ஆய்வு நெறிமுறை",
            definition = "A system of methods used in a particular area of study or activity.",
            exampleSentence = "The research methodology outlined rigorous quantitative data collection procedures.",
            category = "Academic & Research"
        ),
        VocabularyWord(
            word = "Articulate",
            phonetic = "/ɑːˈtɪk.jə.lət/",
            translationSinhala = "පැහැදිලිව ප්‍රකාශ කරනවා",
            translationTamil = "தெளிவாக வெளிப்படுத்துதல்",
            definition = "Having or showing the ability to speak fluently and coherently.",
            exampleSentence = "During the graduate viva voce, she was able to articulate her thesis findings with great clarity.",
            category = "Speaking & Presentation"
        )
    )

    val grammarExercises = listOf(
        GrammarPractice(
            id = "g1",
            title = "Conditionals: Real vs Unreal Hypotheses",
            ruleExplanation = "Third Conditional: Use 'If + had + past participle, would have + past participle' to talk about past unreal situations.",
            question = "If she ______ earlier for the A/L exam, she would have secured a higher island rank.",
            options = listOf("revised", "had revised", "would revise", "has revised"),
            correctIndex = 1,
            explanation = "In the third conditional, the 'if' clause requires the past perfect ('had revised')."
        ),
        GrammarPractice(
            id = "g2",
            title = "Academic Prepositions: Research Contexts",
            ruleExplanation = "Use 'conduct research into/on', 'concur with', 'comply with'.",
            question = "The university committee concurred ______ the department's revised curriculum recommendations.",
            options = listOf("with", "to", "at", "for"),
            correctIndex = 0,
            explanation = "'Concur' takes the preposition 'with' when agreeing with a person, opinion, or proposal."
        ),
        GrammarPractice(
            id = "g3",
            title = "Subject-Verb Agreement with Collective Nouns",
            ruleExplanation = "Phrases starting with 'A panel of experts' or 'The team of researchers' take a singular verb when treated as a single unit.",
            question = "A panel of senior lecturers ______ currently evaluating the examination papers.",
            options = listOf("are", "is", "were", "have been"),
            correctIndex = 1,
            explanation = "'A panel' is a singular collective noun subject, so the singular auxiliary 'is' is correct."
        )
    )

    val conversationScenarios = listOf(
        ConversationScenario(
            id = "conv-uni-interview",
            title = "University Admission & Viva Interview",
            contextDescription = "You are discussing your target degree program and academic background with the Dean of Admissions.",
            difficulty = "Intermediate to Advanced",
            speakerRole = "Dean of Admissions",
            userRole = "Undergraduate Candidate",
            sampleDialogue = listOf(
                "Dean" to "Good morning! Welcome to the faculty interview. Could you briefly introduce yourself and share why you chose Computer Engineering?",
                "Candidate" to "Good morning, Professor. Thank you for this opportunity. Having completed my A/L Physical Science stream with Combined Maths and Physics, I developed a strong passion for computational logic and hardware architectures.",
                "Dean" to "Excellent! What specific project or research area excites you the most in this upcoming semester?",
                "Candidate" to "I am particularly intrigued by artificial intelligence applied to local agricultural and healthcare problems in Sri Lanka."
            )
        ),
        ConversationScenario(
            id = "conv-tech-interview",
            title = "Software Internship Technical Discussion",
            contextDescription = "A Senior Engineering Lead in Colombo is asking about your experience with modern Android architecture and Room databases.",
            difficulty = "Advanced",
            speakerRole = "Engineering Lead",
            userRole = "Intern Applicant",
            sampleDialogue = listOf(
                "Lead" to "Could you walk me through how you organize data flow between your ViewModel and UI in Jetpack Compose?",
                "Applicant" to "Certainly. I follow MVVM architecture with Kotlin StateFlow, observing state in Composables using collectAsStateWithLifecycle.",
                "Lead" to "How do you ensure offline persistence without blocking the main UI thread?",
                "Applicant" to "I utilize Room with Kotlin Coroutines and Flow, delegating database queries to IO dispatchers."
            )
        ),
        ConversationScenario(
            id = "conv-library",
            title = "University Library Research Inquiry",
            contextDescription = "Inquiring about access to IEEE and ScienceDirect journal subscriptions at the library desk.",
            difficulty = "Beginner to Intermediate",
            speakerRole = "Chief Librarian",
            userRole = "Student Researcher",
            sampleDialogue = listOf(
                "Librarian" to "Hello, how can I assist your study research today?",
                "Student" to "Hello! I am preparing my final year project proposal and need remote access to international engineering journals."
            )
        )
    )

    val pronunciationSentences = listOf(
        "Sri Lankan universities provide world-class engineering and medical education.",
        "Thermodynamics explains how heat energy is converted into mechanical work.",
        "Practicing past examination papers under timed conditions builds immense confidence.",
        "Effective communication requires accurate articulation and active listening."
    )
}
