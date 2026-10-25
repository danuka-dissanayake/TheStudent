-- ====================================================================
-- DZD Learn - Past Papers MySQL Database Schema (InfinityFree Compatible)
-- Table: past_papers
-- ====================================================================

-- MIGRATION NOTE FOR EXISTING DATABASE:
-- If you already imported this table previously, run this single query in phpMyAdmin to add the stream column without losing any data:
-- ALTER TABLE `past_papers` ADD COLUMN `stream` VARCHAR(100) NULL AFTER `subject`;
-- ALTER TABLE `past_papers` ADD INDEX `idx_stream` (`stream`);

CREATE TABLE IF NOT EXISTS `past_papers` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `grade` VARCHAR(50) NOT NULL COMMENT 'e.g. Grade 6, Grade 7, Grade 8, Grade 9, Grade 10, Grade 11, Grade 12, Grade 13',
  `category` VARCHAR(50) NOT NULL COMMENT 'e.g. Junior Secondary, O/L, A/L, Grade 6, etc.',
  `subject` VARCHAR(100) NOT NULL COMMENT 'e.g. Combined Mathematics, Physics, Chemistry, Biology, Science, English',
  `stream` VARCHAR(100) DEFAULT NULL COMMENT 'A/L Stream: Physical Science, Biological Science, Commerce, Arts, Technology',
  `year` INT NOT NULL COMMENT 'e.g. 2024, 2023, 2022',
  `exam_type` VARCHAR(100) NOT NULL COMMENT 'e.g. G.C.E. Advanced Level, G.C.E. Ordinary Level, Term Test',
  `medium` VARCHAR(50) NOT NULL DEFAULT 'English' COMMENT 'English, Sinhala, Tamil',
  `title` VARCHAR(255) NOT NULL COMMENT 'Full title of the paper',
  `pdf_url` VARCHAR(500) NOT NULL COMMENT 'URL to download or view the PDF paper',
  `marking_scheme_url` VARCHAR(500) DEFAULT NULL COMMENT 'URL to the marking scheme PDF',
  `description` TEXT DEFAULT NULL COMMENT 'Syllabus coverage and paper breakdown',
  `file_size` VARCHAR(50) DEFAULT '3.5 MB' COMMENT 'Display file size',
  `pages` INT DEFAULT 16 COMMENT 'Total pages',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT 'published, draft, archived',
  INDEX `idx_category` (`category`),
  INDEX `idx_grade` (`grade`),
  INDEX `idx_stream` (`stream`),
  INDEX `idx_subject` (`subject`),
  INDEX `idx_year` (`year`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================================
-- Initial Sample Papers for Testing (InfinityFree MySQL Database)
-- ====================================================================

-- 1. Grade 13 - A/L Combined Mathematics 2024 (Physical Science)
INSERT INTO `past_papers` (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`, `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`, `file_size`, `pages`, `status`)
VALUES
('Grade 13', 'A/L', 'Combined Mathematics', 'Physical Science', 2024, 'G.C.E. Advanced Level', 'English', 
 'Combined Mathematics 2024 (English Medium)', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Combined-Maths-English.pdf', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Combined-Maths-Scheme-English.pdf', 
 'Official G.C.E. A/L 2024 Combined Mathematics Paper I (Pure Mathematics) and Paper II (Mechanics/Applied). Includes comprehensive step-by-step marking scheme.', 
 '3.8 MB', 16, 'published'),

-- 2. Grade 13 - A/L Combined Mathematics 2023 (Physical Science)
('Grade 13', 'A/L', 'Combined Mathematics', 'Physical Science', 2023, 'G.C.E. Advanced Level', 'Sinhala', 
 'Combined Mathematics 2023 (සිංහල මාධ්‍යය)', 
 'https://archive.org/download/gce-al-pastpapers/2023-AL-Combined-Maths-Sinhala.pdf', 
 'https://archive.org/download/gce-al-pastpapers/2023-AL-Combined-Maths-Scheme-Sinhala.pdf', 
 '2023 උසස් පෙළ සංයුක්ත ගණිතය I සහ II සම්පූර්ණ ප්‍රශ්න පත්‍රය සහ නිල ලකුණු දීමේ පටිපාටිය.', 
 '3.9 MB', 16, 'published'),

-- 3. Grade 13 - A/L Biology 2024 (Biological Science)
('Grade 13', 'A/L', 'Biology', 'Biological Science', 2024, 'G.C.E. Advanced Level', 'English', 
 'Biology 2024 (English Medium)', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Biology-English.pdf', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Biology-Scheme-English.pdf', 
 'Cell Biology, Genetics, Plant & Animal Physiology, and Ecology examination paper with scheme.', 
 '4.5 MB', 22, 'published'),

-- 4. Grade 13 - A/L Accounting 2024 (Commerce)
('Grade 13', 'A/L', 'Accounting', 'Commerce', 2024, 'G.C.E. Advanced Level', 'English', 
 'Accounting 2024 (English Medium)', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Accounting-English.pdf', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Accounting-Scheme-English.pdf', 
 'Financial Accounting, Costing, Company Accounts and ratios with complete schemes.', 
 '3.4 MB', 16, 'published'),

-- 5. Grade 13 - A/L ICT 2024 (Technology)
('Grade 13', 'A/L', 'ICT', 'Technology', 2024, 'G.C.E. Advanced Level', 'English', 
 'ICT 2024 (English Medium)', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-ICT-English.pdf', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-ICT-Scheme-English.pdf', 
 'Algorithms, Python programming, Database Design, Networking and Web Technologies.', 
 '3.2 MB', 14, 'published'),

-- 6. Grade 12 - A/L Physics 2024 (Physical Science)
('Grade 12', 'A/L', 'Physics', 'Physical Science', 2024, 'G.C.E. Advanced Level', 'English', 
 'Physics 2024 - Part I & II (English Medium)', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Physics-English.pdf', 
 'https://archive.org/download/gce-al-pastpapers/2024-AL-Physics-Scheme-English.pdf', 
 'Unit 1-10 complete Physics syllabus: Mechanics, Fields, Waves & Oscillations, Thermal Physics, Electronics.', 
 '4.2 MB', 20, 'published'),

-- 7. Grade 11 - O/L Mathematics 2024
('Grade 11', 'O/L', 'Mathematics', NULL, 2024, 'G.C.E. Ordinary Level', 'English', 
 'Mathematics 2024 - Paper I & II', 
 'https://archive.org/download/gce-ol-pastpapers/2024-OL-Maths-English.pdf', 
 'https://archive.org/download/gce-ol-pastpapers/2024-OL-Maths-Scheme-English.pdf', 
 'Official G.C.E. Ordinary Level Mathematics examination papers including structured part A and essay part B.', 
 '2.9 MB', 12, 'published'),

-- 8. Grade 11 - O/L Science 2024
('Grade 11', 'O/L', 'Science', NULL, 2024, 'G.C.E. Ordinary Level', 'Sinhala', 
 'Science 2024 - විද්‍යාව I & II (සිංහල මාධ්‍යය)', 
 'https://archive.org/download/gce-ol-pastpapers/2024-OL-Science-Sinhala.pdf', 
 'https://archive.org/download/gce-ol-pastpapers/2024-OL-Science-Scheme-Sinhala.pdf', 
 '2024 සාමාන්‍ය පෙළ විද්‍යාව I බහුවරණ සහ II ව්‍යුහගත හා රචනා ප්‍රශ්න පත්‍රය සමඟ ලකුණු දීමේ පටිපාටිය.', 
 '3.1 MB', 14, 'published'),

-- 9. Grade 10 - O/L Mathematics 2024
('Grade 10', 'O/L', 'Mathematics', NULL, 2024, 'Year End Evaluation / Term Test', 'English', 
 'Grade 10 Mathematics 2024 Provincial Paper', 
 'https://archive.org/download/grade-papers/Grade10-Maths-2024-English.pdf', 
 'https://archive.org/download/grade-papers/Grade10-Maths-Scheme-2024-English.pdf', 
 'Grade 10 Provincial evaluation paper covering Quadratic Equations, Indices, Geometry & Volume.', 
 '2.1 MB', 10, 'published'),

-- 10. Grade 9 - Junior Secondary Science 2024
('Grade 9', 'Junior Secondary', 'Science', NULL, 2024, 'Year End Evaluation', 'English', 
 'Grade 9 Science 2024 Paper', 
 'https://archive.org/download/grade-papers/Grade9-Science-2024-English.pdf', 
 'https://archive.org/download/grade-papers/Grade9-Science-Scheme-2024-English.pdf', 
 'Complete Grade 9 Science term examination paper covering Human Systems, Force & Energy.', 
 '1.8 MB', 8, 'published'),

-- 11. Grade 8 - Junior Secondary Mathematics 2024
('Grade 8', 'Junior Secondary', 'Mathematics', NULL, 2024, 'Term Test 3', 'English', 
 'Grade 8 Mathematics 2024 Paper', 
 'https://archive.org/download/grade-papers/Grade8-Maths-2024-English.pdf', 
 'https://archive.org/download/grade-papers/Grade8-Maths-Scheme-2024-English.pdf', 
 'Grade 8 3rd Term test with full mark allocations and answer key.', 
 '1.5 MB', 6, 'published'),

-- 12. Grade 7 - Junior Secondary English 2024
('Grade 7', 'Junior Secondary', 'English', NULL, 2024, 'Term Test 3', 'English', 
 'Grade 7 English Language & Literature 2024', 
 'https://archive.org/download/grade-papers/Grade7-English-2024.pdf', 
 'https://archive.org/download/grade-papers/Grade7-English-Scheme-2024.pdf', 
 'Grade 7 English reading comprehension, grammar tests, vocabulary and guided writing.', 
 '1.4 MB', 6, 'published'),

-- 13. Grade 6 - Junior Secondary Mathematics 2024
('Grade 6', 'Junior Secondary', 'Mathematics', NULL, 2024, 'Term Test 3', 'English', 
 'Grade 6 Mathematics 2024 Paper', 
 'https://archive.org/download/grade-papers/Grade6-Maths-2024-English.pdf', 
 'https://archive.org/download/grade-papers/Grade6-Maths-Scheme-2024-English.pdf', 
 'Foundational junior secondary mathematics paper with numbers, geometry, fractions, and measurements.', 
 '1.2 MB', 6, 'published');
