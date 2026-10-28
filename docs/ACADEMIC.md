# Academic details — the four stages the app models

The academic step (`ui/screens/AcademicSetupScreen.kt`) is the data behind help
matching. It is not a free-text "what do you study" box: the matcher needs the
stage, the subjects and the medium, because two students both typing
"Technology" tells you nothing about whether they share Science for Technology,
and a Sinhala-medium student cannot help a Tamil-medium one.

Sources for the structure below: the Department of Examinations subject lists,
the National Institute of Education curriculum reform report (O/L and A/L
structure), and the University Grants Commission handbook (degree programmes and
durations). Re-check the bucket and stream lists when the Department publishes a
new syllabus — the catalog lives in `AcademicProfile.kt`, not on the server.

## The system, stage by stage

| Stage | Grades | Typical age | Ends with |
|---|---|---|---|
| Primary | 1–5 | 5–9 | Grade 5 Scholarship (placement/scholarship, not a qualification) |
| **Junior secondary** | **6–9** | 10–13 | No national exam; school term tests |
| **Senior secondary (O/L)** | **10–11** | 14–15 | G.C.E. Ordinary Level |
| **Collegiate (A/L)** | **12–13** | 16–18 | G.C.E. Advanced Level |
| **Tertiary** | — | 19+ | Degree, higher diploma, NVQ |

Schooling is compulsory to Grade 9. A/L is a two-year programme and the only
route into a state university, selected on Z-score with district and course
quotas.

## Grades 6–9 — junior secondary

A common curriculum for everyone, in the student's medium. The subjects are:

Mathematics · Science · English · Sinhala · Tamil · History · Geography · Life
Competencies & Citizenship Education · Information & Communication Technology ·
Health & Physical Education · Practical & Technical Skills · Art · Music ·
Dancing · Drama & Theatre · Religion

There is no national exam, so the profile keeps **the grade, the medium and up to
three subjects the student wants help with**. That is what a tutor or an older
student can actually act on.

## O/L — Grades 10–11

Nine subjects: **six compulsory plus one from each of three optional buckets.**

Compulsory (identical for every candidate, so never stored — they carry no
matching signal): Religion · First Language (Sinhala/Tamil) · English ·
Mathematics · Science · History.

Optional buckets (Department of Examinations names):

1. **Humanities & languages** — Geography, Civic Education, Business &
   Accounting Studies, Entrepreneurship Studies, Second Language, Pali, Sanskrit,
   French, German, Hindi, Japanese, Arabic, Korean, Chinese, Russian
2. **Aesthetic** — Art, Oriental/Carnatic/Western Music, Dancing (Oriental,
   Bharatha), Drama & Theatre, Appreciation of English/Sinhala/Tamil/Arabic
   Literary Texts
3. **Technical & practical** — ICT, Communication & Media Studies, Agriculture &
   Food Technology, Aquatic Bioresources Technology, Arts & Crafts, Home
   Economics, Health & Physical Education, Design & Construction Technology,
   Design & Mechanical Technology, Design, Electrical & Electronic Technology,
   Electronic Writing & Shorthand

Grading A/B/C/S/W. Qualifying for A/L needs at least six passes (S or better)
including the first language, Mathematics and English.

## A/L — Grades 12–13

A stream and **three subjects**. Every candidate also sits **General English** and
the **Common General Test**; they are on the admission card but are not part of
the counted three, so the screen states them once and does not ask for them.

| Stream | Usual core | The third subject |
|---|---|---|
| Physical Science | Combined Mathematics, Physics | Chemistry, Higher Mathematics or ICT |
| Biological Science | Biology, Chemistry | Physics or Agricultural Science |
| Commerce | Accounting / Business Studies / Economics | Business Statistics or ICT |
| Technology | Science for Technology | Engineering Technology or Bio-systems Technology, plus a category-C subject |
| Arts | — | any of the ~37 Arts subjects (languages, social sciences, aesthetics, religions) |

Grading A/B/C/S/F; university admission is by Z-score.

## University — undergraduate

Seventeen state universities under the UGC (Colombo, Peradeniya, Sri
Jayewardenepura, Kelaniya, Moratuwa, Jaffna, Ruhuna, Eastern, South Eastern,
Rajarata, Sabaragamuwa, Wayamba, Uva Wellassa, Open University, KDU, Bhikshu,
Buddhist & Pali) plus private, foreign and higher-diploma routes in "Other".

Durations: Arts and Science general degrees 3 years (special 4), Engineering and
most applied sciences 4, Medicine 5, Dental Surgery 4, Veterinary Science 4,
Ayurveda/Siddha/Unani 5–6. The profile therefore keeps the **university, the
field of study and the year of study** (1–6) and no subjects.

## What the app stores

`users` columns (all nullable, added with `ts_ensure_column`):

`exam_level` (`JUNIOR` | `OL` | `AL` | `UNI`) · `stream_code` · `exam_year` ·
`study_year` (school grade, or year of study at university) · `medium` ·
`university` · `degree_programme`

plus one row per subject in `user_subjects` (`bucket_no` is set for O/L only).

Endpoint: `POST api/auth.php?action=academic_profile` (Bearer). It validates the
shape per stage:

| Level | study_year | subjects | other |
|---|---|---|---|
| JUNIOR | 6–9 | 1–3, no buckets | no exam year |
| OL | 10/11 or 0 | exactly 3, one per bucket | exam year required |
| AL | 12/13 or 0 | exactly 3, no buckets | stream + exam year required |
| UNI | 1–6 | none | university + field required |

`ts_academic_complete()` mirrors the same rules, so "Complete" on the profile
card cannot disagree with what the server accepted.

The Android cache lives in `user_profile` (Room v7: `studyYear`, `university`,
`degreeProgramme`), encoded by `AcademicProfile.encodeSubjects`.
