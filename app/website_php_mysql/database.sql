-- DZD Learn Past Papers Database Schema (MySQL 5.7 / 8.0 Compatible)
-- Designed for InfinityFree & standard cPanel / PHP hosting

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Grades Table
CREATE TABLE IF NOT EXISTS `grades` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL UNIQUE,
  `slug` VARCHAR(50) NOT NULL UNIQUE,
  `sort_order` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed default grades Grade 6 to Grade 13
INSERT INTO `grades` (`name`, `slug`, `sort_order`) VALUES
('Grade 6', 'grade-6', 1),
('Grade 7', 'grade-7', 2),
('Grade 8', 'grade-8', 3),
('Grade 9', 'grade-9', 4),
('Grade 10', 'grade-10', 5),
('Grade 11 – O/L', 'grade-11-ol', 6),
('Grade 12 – A/L', 'grade-12-al', 7),
('Grade 13 – A/L', 'grade-13-al', 8)
ON DUPLICATE KEY UPDATE `name`=`name`;

-- 2. Subjects Table
CREATE TABLE IF NOT EXISTS `subjects` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `slug` VARCHAR(100) NOT NULL UNIQUE,
  `icon` VARCHAR(50) DEFAULT 'book'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed default subjects
INSERT INTO `subjects` (`name`, `slug`, `icon`) VALUES
('Mathematics', 'mathematics', 'calculator'),
('Science', 'science', 'atom'),
('English', 'english', 'language'),
('Sinhala', 'sinhala', 'book-open'),
('Tamil', 'tamil', 'book-open'),
('History', 'history', 'landmark'),
('ICT', 'ict', 'cpu'),
('Religion', 'religion', 'sun'),
('Commerce', 'commerce', 'trending-up'),
('Geography', 'geography', 'globe'),
('Physics', 'physics', 'zap'),
('Chemistry', 'chemistry', 'flask-conical'),
('Combined Mathematics', 'combined-mathematics', 'binary'),
('Biology', 'biology', 'dna'),
('Accounting', 'accounting', 'calculator'),
('Economics', 'economics', 'bar-chart'),
('Business Studies', 'business-studies', 'briefcase'),
('Agricultural Science', 'agricultural-science', 'leaf'),
('Engineering Technology', 'engineering-technology', 'wrench')
ON DUPLICATE KEY UPDATE `name`=`name`;

-- 3. Grade-Subject association table (Scalable mapping)
CREATE TABLE IF NOT EXISTS `grade_subjects` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `grade_id` INT NOT NULL,
  `subject_id` INT NOT NULL,
  UNIQUE KEY `grade_subj_unique` (`grade_id`, `subject_id`),
  FOREIGN KEY (`grade_id`) REFERENCES `grades`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`subject_id`) REFERENCES `subjects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Examination Types Table
CREATE TABLE IF NOT EXISTS `exam_types` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `exam_types` (`name`) VALUES
('G.C.E. O/L'),
('G.C.E. A/L'),
('School Term Test'),
('Model Paper'),
('Provincial Examination')
ON DUPLICATE KEY UPDATE `name`=`name`;

-- 5. Past Papers Table
CREATE TABLE IF NOT EXISTS `papers` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `grade_id` INT NOT NULL,
  `subject_id` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `year` INT NOT NULL,
  `exam_type` VARCHAR(50) NOT NULL,
  `medium` VARCHAR(50) NOT NULL, -- Sinhala, English, Tamil
  `file_path` VARCHAR(255) NOT NULL,
  `file_size` VARCHAR(50) DEFAULT '2.5 MB',
  `pages` INT DEFAULT 12,
  `has_marking_scheme` TINYINT(1) DEFAULT 1,
  `syllabus_summary` TEXT,
  `download_count` INT DEFAULT 0,
  `status` ENUM('Published', 'Draft', 'Hidden') DEFAULT 'Published',
  `upload_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`grade_id`) REFERENCES `grades`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`subject_id`) REFERENCES `subjects`(`id`) ON DELETE CASCADE,
  INDEX `idx_grade_subject` (`grade_id`, `subject_id`),
  INDEX `idx_year` (`year`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Admins Table
CREATE TABLE IF NOT EXISTS `admins` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default admin account (Username: admin, Password: admin123password)
-- Password hash generated with password_hash('admin123password', PASSWORD_DEFAULT)
INSERT INTO `admins` (`username`, `password_hash`, `email`) VALUES
('admin', '$2y$10$O0.cQjFm.l1E0.xZ8P23Q.d2y0yQyT56Q8e.rE8y7b7j9z9a8b1qK', 'admin@dzdlearn.lk')
ON DUPLICATE KEY UPDATE `username`=`username`;

-- Seed starter papers matching Grade 6 to Grade 13
INSERT INTO `papers` (`grade_id`, `subject_id`, `title`, `year`, `exam_type`, `medium`, `file_path`, `file_size`, `pages`, `has_marking_scheme`, `status`) VALUES
(6, 1, '2024 Grade 6 Mathematics 3rd Term Test (ගණිතය)', 2024, 'School Term Test', 'Sinhala', 'uploads/papers/g6_math_2024.pdf', '1.5 MB', 6, 1, 'Published'),
(6, 2, '2024 Grade 6 Science Term Evaluation (විද්‍යාව)', 2024, 'School Term Test', 'English', 'uploads/papers/g6_sci_2024.pdf', '1.4 MB', 6, 1, 'Published'),
(7, 1, '2024 Grade 7 Mathematics Year-End Paper', 2024, 'School Term Test', 'Sinhala', 'uploads/papers/g7_math_2024.pdf', '1.6 MB', 8, 1, 'Published'),
(8, 1, '2024 Grade 8 Mathematics Provincial Evaluation', 2024, 'School Term Test', 'Sinhala', 'uploads/papers/g8_math_2024.pdf', '1.8 MB', 8, 1, 'Published'),
(9, 1, '2024 Grade 9 Mathematics Final Assessment', 2024, 'School Term Test', 'Sinhala', 'uploads/papers/g9_math_2024.pdf', '2.0 MB', 10, 1, 'Published'),
(10, 1, '2024 Grade 10 Mathematics Term Test', 2024, 'School Term Test', 'Sinhala', 'uploads/papers/g10_math_2024.pdf', '2.5 MB', 12, 1, 'Published'),
(6, 1, '2025 O/L Mathematics Model Examination Paper (ගණිතය)', 2025, 'G.C.E. O/L', 'Sinhala', 'uploads/papers/ol_math_2025.pdf', '3.2 MB', 14, 1, 'Published'),
(6, 1, '2024 O/L Mathematics Past Paper & Scheme', 2024, 'G.C.E. O/L', 'Sinhala', 'uploads/papers/ol_math_2024.pdf', '3.4 MB', 14, 1, 'Published'),
(6, 2, '2024 O/L Science Past Paper (විද්‍යාව)', 2024, 'G.C.E. O/L', 'Sinhala', 'uploads/papers/ol_sci_2024.pdf', '3.5 MB', 14, 1, 'Published'),
(6, 3, '2024 O/L English Language Examination Paper', 2024, 'G.C.E. O/L', 'English', 'uploads/papers/ol_eng_2024.pdf', '2.1 MB', 10, 1, 'Published'),
(6, 7, '2024 O/L ICT Past Paper & Marking Scheme', 2024, 'G.C.E. O/L', 'English', 'uploads/papers/ol_ict_2024.pdf', '2.6 MB', 12, 1, 'Published'),
(7, 13, '2025 Grade 12 A/L Combined Mathematics Term Paper', 2025, 'School Term Test', 'Sinhala', 'uploads/papers/al_g12_cm_2025.pdf', '3.1 MB', 12, 1, 'Published'),
(8, 13, '2024 A/L Combined Mathematics Past Paper & Scheme', 2024, 'G.C.E. A/L', 'Sinhala', 'uploads/papers/al_cm_2024.pdf', '4.1 MB', 18, 1, 'Published'),
(8, 11, '2024 A/L Physics Past Paper & Marking Scheme', 2024, 'G.C.E. A/L', 'Sinhala', 'uploads/papers/al_phy_2024.pdf', '4.4 MB', 22, 1, 'Published'),
(8, 12, '2024 A/L Chemistry National Past Paper & Scheme', 2024, 'G.C.E. A/L', 'Sinhala', 'uploads/papers/al_chem_2024.pdf', '4.5 MB', 20, 1, 'Published'),
(8, 14, '2024 A/L Biology Examination Paper', 2024, 'G.C.E. A/L', 'English', 'uploads/papers/al_bio_2024.pdf', '4.8 MB', 22, 1, 'Published'),
(8, 15, '2024 A/L Accounting Past Paper & Scheme', 2024, 'G.C.E. A/L', 'Sinhala', 'uploads/papers/al_acc_2024.pdf', '3.8 MB', 18, 1, 'Published'),
(8, 16, '2024 A/L Economics Past Paper & Scheme', 2024, 'G.C.E. A/L', 'English', 'uploads/papers/al_econ_2024.pdf', '3.3 MB', 16, 1, 'Published'),
(8, 17, '2024 A/L Business Studies Paper (ව්‍යාපාර අධ්‍යයනය)', 2024, 'G.C.E. A/L', 'Sinhala', 'uploads/papers/al_bs_2024.pdf', '3.4 MB', 16, 1, 'Published');

SET FOREIGN_KEY_CHECKS = 1;
