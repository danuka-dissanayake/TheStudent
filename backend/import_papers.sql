-- TheStudent · direct import of the past-paper archive
-- 730 papers. Upload the files to uploads/Papers/ first, then import
-- this file in phpMyAdmin. Safe to run twice: every row is guarded by
-- subject + year + medium.
SET NAMES utf8mb4;

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Accounting English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2015-accounting-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Accounting English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2016-accounting-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Accounting English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2017-accounting-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Accounting English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2021-accounting-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2021-accounting-english-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Accounting English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2024-accounting-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2016-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2017-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2018-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2019-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2020-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2021-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Agricultural Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2024-agricultural-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Agro Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2015-agro-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Agro Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2017-agro-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Agro Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2019-agro-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Agro Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2021-agro-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Art English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2015-art-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Art English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2017-art-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Art English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2019-art-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Art English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2020-art-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/art/2020-art-english-scheme.pdf', 'Official G.C.E. Advanced Level Art paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Art English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2024-art-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Art English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2025-art-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2016-biology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Biology paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2017-biology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Biology paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2018-biology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Biology paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2019-biology-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2019-biology-english-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2019, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2020-biology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Biology paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2023-biology-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2023-biology-english-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2023, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2024-biology-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2024-biology-english-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2024, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Biology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2025-biology-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2025-biology-english-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2025, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Bio Resource Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2015-bio-resource-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Bio Systems Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2016-bio-systems-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Bio Systems Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2018-bio-systems-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Bio Systems Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2019-bio-systems-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Bio Systems Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2021-bio-systems-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Buddhism English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2017-buddhism-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Buddhism English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2021-buddhism-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Buddhism English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2024-buddhism-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2015-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2016-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2017-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2018-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2019-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2020-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2021-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Buddhist Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2024-buddhist-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2015-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2016-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2017-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2018-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2019-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2021-business-studies-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2021-business-studies-english-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2022-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2022, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2024-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Business Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2025-business-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2015-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2015-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2015, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2016-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2016-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2016, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2017-chemistry-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2018-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2018-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2018, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2019-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2019-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2019, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2020-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2020-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2021-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2021-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2022-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2022-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2022, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2024-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2024-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2024, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Chemistry English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2025-chemistry-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2025-chemistry-english-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2025, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Christian Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2020-christian-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2015-christianity-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2016-christianity-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2017-christianity-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2019-christianity-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2020-christianity-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2021-christianity-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Christianity English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2024-christianity-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christianity/2024-christianity-english-scheme.pdf', 'Official G.C.E. Advanced Level Christianity paper, 2024, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Combined Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2016-combined-mathematics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2016-combined-mathematics-english-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2016, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Combined Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2017-combined-mathematics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2017-combined-mathematics-english-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2017, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Combined Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2018-combined-mathematics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2018-combined-mathematics-english-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2018, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Combined Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2019-combined-mathematics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2019-combined-mathematics-english-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2019, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Combined Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2020-combined-mathematics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2020-combined-mathematics-english-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Combined Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2025-combined-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Combined Mathematics paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Common General Test English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2016-common-general-test-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Common General Test English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2018-common-general-test-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Common General Test English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2019-common-general-test-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2019-common-general-test-english-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2019, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Common General Test English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2020-common-general-test-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2020-common-general-test-english-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Common General Test English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2021-common-general-test-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2021-common-general-test-english-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Common General Test English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2025-common-general-test-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2015-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2016-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2019-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2020-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2021-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2022-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2022, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2023-communication-and-media-studies-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2023-communication-and-media-studies-english-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2023, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2024-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Communication & Media Studies English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2025-communication-and-media-studies-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Drama and Theatre English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2015-drama-and-theatre-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Drama and Theatre English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2016-drama-and-theatre-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Drama and Theatre English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2017-drama-and-theatre-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Drama and Theatre English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2019-drama-and-theatre-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Drama and Theatre English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2020-drama-and-theatre-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Drama and Theatre English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2021-drama-and-theatre-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2015-economics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2016-economics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2018-economics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2019-economics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2021-economics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2021-economics-english-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2022-economics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2022-economics-english-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2022, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2024-economics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2024-economics-english-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2024, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Economics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2025-economics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2015-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2016-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2017-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2018-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2019-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2020-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2024-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Electric, Electronic and Information Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2025-electric-electronic-and-information-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2015-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2016-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2017-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2018-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2019-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2020-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2021-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2024-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Engineering Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2025-engineering-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2016-general-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level General English paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2017-general-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level General English paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2019-general-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/general-english/2019-general-english-english-scheme.pdf', 'Official G.C.E. Advanced Level General English paper, 2019, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2020-general-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level General English paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2021-general-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level General English paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2022-general-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/general-english/2022-general-english-english-scheme.pdf', 'Official G.C.E. Advanced Level General English paper, 2022, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2023-general-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/general-english/2023-general-english-english-scheme.pdf', 'Official G.C.E. Advanced Level General English paper, 2023, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'General English',
       'Common', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 General English English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/general-english/2025-general-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/general-english/2025-general-english-english-scheme.pdf', 'Official G.C.E. Advanced Level General English paper, 2025, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'General English' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Geography English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2017-geography-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Geography English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2018-geography-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2018-geography-english-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2018, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Geography English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2019-geography-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Geography English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2025-geography-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2016-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2017-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2018-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2019-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2020-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2021-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2024-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Higher Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2025-higher-mathematics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Hindu Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2016-hindu-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2016-ict-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2016-ict-english-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2016, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2017-ict-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2017-ict-english-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2017, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2018-ict-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2020-ict-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2020-ict-english-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2021-ict-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2021-ict-english-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2022-ict-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2022-ict-english-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2022, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2023-ict-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2023, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 ICT English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2025-ict-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2025-ict-english-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2025, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2015-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2016-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2017-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2018-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2019-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2020-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Islam English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2021-islam-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2015-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2016-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2017-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2018-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2019-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2020-islam-civilization-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2020-islam-civilization-english-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2021-islam-civilization-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2021-islam-civilization-english-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2024-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Islam Civilization English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2025-islam-civilization-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Mathematics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2024-mathematics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/mathematics/2024-mathematics-english-scheme.pdf', 'Official G.C.E. Advanced Level Mathematics paper, 2024, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 Physics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2016-physics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Physics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2018-physics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2018-physics-english-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2018, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Physics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2020-physics-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2020-physics-english-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Physics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2021-physics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2021, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 Physics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2023-physics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2023, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Physics English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2025-physics-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2017-political-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2018-political-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2019-political-science-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2019-political-science-english-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2019, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2020-political-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2020, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2021-political-science-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2021-political-science-english-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2023-political-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2023, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2024-political-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Political Science English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2025-political-science-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2015-science-for-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'English',
       '2017 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2017-science-for-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2017, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2017
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2018-science-for-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2018, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2019-science-for-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2021-science-for-technology-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2021-science-for-technology-english-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'English',
       '2024 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2024-science-for-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2024, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2024
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'English',
       '2025 Science for Technology English Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2025-science-for-technology-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2025, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2025
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Arabic',
       '2015 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2015-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2015, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2015
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Arabic',
       '2016 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2016-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2016, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2016
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Arabic',
       '2017 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2017-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2017, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2017
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Arabic',
       '2018 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2018-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2018, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2018
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Arabic',
       '2019 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2019-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2019, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2019
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Arabic',
       '2021 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2021-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2021, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2021
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Arabic',
       '2024 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2024-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2024, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2024
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Arabic',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Arabic',
       '2025 Arabic Past Paper', 'https://app.quizontal.site/uploads/Papers/arabic/2025-arabic-arabic-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Arabic paper, 2025, Arabic medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Arabic' AND `year` = 2025
    AND `medium` = 'Arabic'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chinese',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Chinese',
       '2016 Chinese Past Paper', 'https://app.quizontal.site/uploads/Papers/chinese/2016-chinese-chinese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chinese paper, 2016, Chinese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chinese' AND `year` = 2016
    AND `medium` = 'Chinese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chinese',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Chinese',
       '2017 Chinese Past Paper', 'https://app.quizontal.site/uploads/Papers/chinese/2017-chinese-chinese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chinese paper, 2017, Chinese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chinese' AND `year` = 2017
    AND `medium` = 'Chinese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chinese',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Chinese',
       '2018 Chinese Past Paper', 'https://app.quizontal.site/uploads/Papers/chinese/2018-chinese-chinese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chinese paper, 2018, Chinese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chinese' AND `year` = 2018
    AND `medium` = 'Chinese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chinese',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Chinese',
       '2019 Chinese Past Paper', 'https://app.quizontal.site/uploads/Papers/chinese/2019-chinese-chinese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chinese paper, 2019, Chinese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chinese' AND `year` = 2019
    AND `medium` = 'Chinese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2015, 'G.C.E. Advanced Level', 'English',
       '2015 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2015-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level English paper, 2015, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2015
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2016, 'G.C.E. Advanced Level', 'English',
       '2016 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2016-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level English paper, 2016, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2016
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2018, 'G.C.E. Advanced Level', 'English',
       '2018 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2018-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/english/2018-english-english-scheme.pdf', 'Official G.C.E. Advanced Level English paper, 2018, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2018
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2019, 'G.C.E. Advanced Level', 'English',
       '2019 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2019-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level English paper, 2019, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2019
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2020, 'G.C.E. Advanced Level', 'English',
       '2020 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2020-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/english/2020-english-english-scheme.pdf', 'Official G.C.E. Advanced Level English paper, 2020, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2020
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2021, 'G.C.E. Advanced Level', 'English',
       '2021 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2021-english-english-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/english/2021-english-english-scheme.pdf', 'Official G.C.E. Advanced Level English paper, 2021, English medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2021
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2022, 'G.C.E. Advanced Level', 'English',
       '2022 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2022-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level English paper, 2022, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2022
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'English',
       'Arts', 2023, 'G.C.E. Advanced Level', 'English',
       '2023 English Past Paper', 'https://app.quizontal.site/uploads/Papers/english/2023-english-english-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level English paper, 2023, English medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'English' AND `year` = 2023
    AND `medium` = 'English'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2016, 'G.C.E. Advanced Level', 'French',
       '2016 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2016-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2016, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2016
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2017, 'G.C.E. Advanced Level', 'French',
       '2017 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2017-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2017, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2017
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2018, 'G.C.E. Advanced Level', 'French',
       '2018 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2018-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2018, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2018
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2019, 'G.C.E. Advanced Level', 'French',
       '2019 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2019-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2019, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2019
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2020, 'G.C.E. Advanced Level', 'French',
       '2020 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2020-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2020, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2020
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2021, 'G.C.E. Advanced Level', 'French',
       '2021 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2021-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2021, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2021
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2023, 'G.C.E. Advanced Level', 'French',
       '2023 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2023-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2023, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2023
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'French',
       'Arts', 2024, 'G.C.E. Advanced Level', 'French',
       '2024 French Past Paper', 'https://app.quizontal.site/uploads/Papers/french/2024-french-french-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level French paper, 2024, French medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'French' AND `year` = 2024
    AND `medium` = 'French'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2015, 'G.C.E. Advanced Level', 'German',
       '2015 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2015-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2015, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2015
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2016, 'G.C.E. Advanced Level', 'German',
       '2016 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2016-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2016, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2016
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2018, 'G.C.E. Advanced Level', 'German',
       '2018 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2018-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2018, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2018
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2019, 'G.C.E. Advanced Level', 'German',
       '2019 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2019-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2019, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2019
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2020, 'G.C.E. Advanced Level', 'German',
       '2020 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2020-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2020, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2020
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2021, 'G.C.E. Advanced Level', 'German',
       '2021 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2021-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2021, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2021
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2022, 'G.C.E. Advanced Level', 'German',
       '2022 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2022-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2022, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2022
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2023, 'G.C.E. Advanced Level', 'German',
       '2023 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2023-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2023, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2023
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2024, 'G.C.E. Advanced Level', 'German',
       '2024 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2024-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2024, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2024
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'German',
       'Arts', 2025, 'G.C.E. Advanced Level', 'German',
       '2025 German Past Paper', 'https://app.quizontal.site/uploads/Papers/german/2025-german-german-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level German paper, 2025, German medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'German' AND `year` = 2025
    AND `medium` = 'German'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Japanese',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Japanese',
       '2015 Japanese Past Paper', 'https://app.quizontal.site/uploads/Papers/japanese/2015-japanese-japanese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Japanese paper, 2015, Japanese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Japanese' AND `year` = 2015
    AND `medium` = 'Japanese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Japanese',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Japanese',
       '2019 Japanese Past Paper', 'https://app.quizontal.site/uploads/Papers/japanese/2019-japanese-japanese-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/japanese/2019-japanese-japanese-scheme.pdf', 'Official G.C.E. Advanced Level Japanese paper, 2019, Japanese medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Japanese' AND `year` = 2019
    AND `medium` = 'Japanese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Japanese',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Japanese',
       '2021 Japanese Past Paper', 'https://app.quizontal.site/uploads/Papers/japanese/2021-japanese-japanese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Japanese paper, 2021, Japanese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Japanese' AND `year` = 2021
    AND `medium` = 'Japanese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Japanese',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Japanese',
       '2022 Japanese Past Paper', 'https://app.quizontal.site/uploads/Papers/japanese/2022-japanese-japanese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Japanese paper, 2022, Japanese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Japanese' AND `year` = 2022
    AND `medium` = 'Japanese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Japanese',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Japanese',
       '2023 Japanese Past Paper', 'https://app.quizontal.site/uploads/Papers/japanese/2023-japanese-japanese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Japanese paper, 2023, Japanese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Japanese' AND `year` = 2023
    AND `medium` = 'Japanese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Japanese',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Japanese',
       '2025 Japanese Past Paper', 'https://app.quizontal.site/uploads/Papers/japanese/2025-japanese-japanese-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Japanese paper, 2025, Japanese medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Japanese' AND `year` = 2025
    AND `medium` = 'Japanese'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Korean',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Korean',
       '2024 Korean Past Paper', 'https://app.quizontal.site/uploads/Papers/korean/2024-korean-korean-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Korean paper, 2024, Korean medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Korean' AND `year` = 2024
    AND `medium` = 'Korean'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Korean',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Korean',
       '2025 Korean Past Paper', 'https://app.quizontal.site/uploads/Papers/korean/2025-korean-korean-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Korean paper, 2025, Korean medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Korean' AND `year` = 2025
    AND `medium` = 'Korean'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Russian',
       '2015 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2015-russian-russian-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Russian paper, 2015, Russian medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2015
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Russian',
       '2017 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2017-russian-russian-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Russian paper, 2017, Russian medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2017
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Russian',
       '2018 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2018-russian-russian-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/russian/2018-russian-russian-scheme.pdf', 'Official G.C.E. Advanced Level Russian paper, 2018, Russian medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2018
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Russian',
       '2019 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2019-russian-russian-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/russian/2019-russian-russian-scheme.pdf', 'Official G.C.E. Advanced Level Russian paper, 2019, Russian medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2019
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Russian',
       '2020 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2020-russian-russian-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Russian paper, 2020, Russian medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2020
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Russian',
       '2021 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2021-russian-russian-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Russian paper, 2021, Russian medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2021
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Russian',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Russian',
       '2022 Russian Past Paper', 'https://app.quizontal.site/uploads/Papers/russian/2022-russian-russian-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Russian paper, 2022, Russian medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Russian' AND `year` = 2022
    AND `medium` = 'Russian'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2015-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2016-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2017-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2018-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2019-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2021-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2021, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2023-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2023, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2024-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2024, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Sinhala',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Sinhala Past Paper', 'https://app.quizontal.site/uploads/Papers/sinhala/2025-sinhala-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Sinhala paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Sinhala' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2015-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2016-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2017-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2020-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2021-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2022-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Tamil',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Tamil Past Paper', 'https://app.quizontal.site/uploads/Papers/tamil/2023-tamil-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Tamil paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Tamil' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2015-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2015-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2015, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2016-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2016-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2017-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2017-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2018-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2018-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2019-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2019-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2021-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2021-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2023-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2023-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2024-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2024-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Accounting Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2025-accounting-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2025-accounting-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2015-agricultural-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2016-agricultural-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2017-agricultural-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2019-agricultural-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2021-agricultural-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/agricultural-science/2021-agricultural-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Agricultural Science paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2022-agricultural-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/agricultural-science/2022-agricultural-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Agricultural Science paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2024-agricultural-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/agricultural-science/2024-agricultural-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Agricultural Science paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Agricultural Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2025-agricultural-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2016-agro-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2017-agro-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2018-agro-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2019-agro-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2020-agro-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/agro-technology/2020-agro-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Agro Technology paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2022-agro-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Agro Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2024-agro-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/agro-technology/2024-agro-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Agro Technology paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2016-art-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2018-art-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2020-art-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/art/2020-art-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Art paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2021-art-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/art/2021-art-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Art paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2022-art-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2023-art-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2023, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Art Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2025-art-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/art/2025-art-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Art paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2016-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2016-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2018-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2018-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2019-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2019-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2020-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2020-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2021-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2021-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2022-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2022-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Biology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2023-biology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2023-biology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2015-bio-resource-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2016-bio-resource-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2017-bio-resource-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2018-bio-resource-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2019-bio-resource-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2021-bio-resource-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2021-bio-resource-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Bio Resource Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2022-bio-resource-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2015-bio-systems-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2016-bio-systems-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2017-bio-systems-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2019-bio-systems-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2019-bio-systems-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2020-bio-systems-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2020-bio-systems-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2021-bio-systems-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2021-bio-systems-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2023-bio-systems-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2023-bio-systems-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2024-bio-systems-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2024-bio-systems-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Bio Systems Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2025-bio-systems-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2015-buddhism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2016-buddhism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2018-buddhism-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhism/2018-buddhism-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhism paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2019-buddhism-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhism/2019-buddhism-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhism paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2020-buddhism-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhism/2020-buddhism-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhism paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2022-buddhism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Buddhism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2025-buddhism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2016-buddhist-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2017-buddhist-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2018-buddhist-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2018-buddhist-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2019-buddhist-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2019-buddhist-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2020-buddhist-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2021-buddhist-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2021-buddhist-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2024-buddhist-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2024-buddhist-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Buddhist Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2025-buddhist-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2015-business-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2015-business-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2015, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2016-business-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2016-business-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2017-business-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2018-business-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2019-business-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2019-business-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2020-business-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2023-business-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2023-business-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Business Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2025-business-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2016-chemistry-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2017-chemistry-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2017-chemistry-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2018-chemistry-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2018-chemistry-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2019-chemistry-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2020-chemistry-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2020-chemistry-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2021-chemistry-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2021, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2022-chemistry-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2023-chemistry-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2023-chemistry-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2024-chemistry-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2024-chemistry-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2026, 'G.C.E. Advanced Level', 'Sinhala',
       '2026 Chemistry Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2026-chemistry-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2026, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2026
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Christian Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2015-christian-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Christian Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2016-christian-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Christian Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2017-christian-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Christian Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2020-christian-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Christian Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2024-christian-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christian-civilization/2024-christian-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Christian Civilization paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Christian Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2025-christian-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2015-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2016-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2017-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2018-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2019-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2020-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2021-christianity-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christianity/2021-christianity-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Christianity paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2023-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2023, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2024-christianity-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christianity/2024-christianity-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Christianity paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Christianity Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2025-christianity-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Combined Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2018-combined-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Combined Mathematics paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Combined Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2019-combined-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Combined Mathematics paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Combined Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2020-combined-mathematics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2020-combined-mathematics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Combined Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2021-combined-mathematics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2021-combined-mathematics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Combined Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2024-combined-mathematics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2024-combined-mathematics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Combined Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2025-combined-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Combined Mathematics paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Common General Test Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2016-common-general-test-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Common General Test Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2017-common-general-test-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Common General Test Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2019-common-general-test-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2019-common-general-test-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Common General Test Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2021-common-general-test-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2021-common-general-test-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Common General Test Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2022-common-general-test-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2022-common-general-test-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Common General Test Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2023-common-general-test-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2023-common-general-test-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2015-communication-and-media-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2016-communication-and-media-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2017-communication-and-media-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2018-communication-and-media-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2019-communication-and-media-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2019-communication-and-media-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2020-communication-and-media-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2020-communication-and-media-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2021-communication-and-media-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2021, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2022-communication-and-media-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2022-communication-and-media-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2023-communication-and-media-studies-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2023-communication-and-media-studies-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Communication & Media Studies Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2025-communication-and-media-studies-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2015-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2016-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2018-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2019-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2020-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2021-drama-and-theatre-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2021-drama-and-theatre-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Drama and Theatre paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2022-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Drama and Theatre Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2024-drama-and-theatre-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2024, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2015-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2015-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2015, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2016-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2016-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2017-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2017-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2018-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2018-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2021-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2021-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2022-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2022-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2023-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2023-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Economics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2025-economics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2025-economics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2016-electric-electronic-and-information-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2017-electric-electronic-and-information-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2018-electric-electronic-and-information-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2019-electric-electronic-and-information-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2019-electric-electronic-and-information-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2022-electric-electronic-and-information-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2024-electric-electronic-and-information-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2024, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Electric, Electronic and Information Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2025-electric-electronic-and-information-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2015-engineering-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2016-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2016-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2017-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2017-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2018-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2018-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2019-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2019-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2020-engineering-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2021-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2021-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2022-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2022-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2024-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2024-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Engineering Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2025-engineering-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2025-engineering-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2015-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2016-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2017-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2018-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2019-geography-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2019-geography-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2020-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2021-geography-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2021-geography-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2022-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2023-geography-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2023, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Geography Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2024-geography-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2024-geography-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Higher Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2020-higher-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2020, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Higher Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2021-higher-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2021, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Higher Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2022-higher-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Higher Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2024-higher-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2024, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Hindu Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2019-hindu-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Hinduism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2017-hinduism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Hinduism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2018-hinduism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Hinduism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2019-hinduism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Hinduism Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2025-hinduism-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2016-ict-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2017-ict-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2017-ict-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2020-ict-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2020-ict-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2021-ict-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2021, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2022-ict-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2022-ict-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2023-ict-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2023-ict-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2024-ict-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2024, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 ICT Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2025-ict-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2025-ict-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2017-islam-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2018-islam-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2019-islam-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2020-islam-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam/2020-islam-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Islam paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2021-islam-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam/2021-islam-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Islam paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2024-islam-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam/2024-islam-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Islam paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Islam Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2025-islam-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2016-islam-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2018-islam-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2019-islam-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2020-islam-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2020-islam-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2021-islam-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2021-islam-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2024-islam-civilization-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2024-islam-civilization-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Islam Civilization Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2025-islam-civilization-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2025, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2016-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2017-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2018-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2019-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Mathematics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2024-mathematics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2024, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2016-physics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2016-physics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2017-physics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2017-physics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2018-physics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2019-physics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2019, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2020-physics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2020-physics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2021-physics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2021-physics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2022-physics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2022, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2023-physics-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2023, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2024-physics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2024-physics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Physics Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2025-physics-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2025-physics-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2015-political-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2015, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2016-political-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2016, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2017-political-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2017, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2018-political-science-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2018, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2019-political-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2019-political-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2020-political-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2020-political-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2021-political-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2021-political-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2021, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Political Science Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2024-political-science-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2024-political-science-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Sinhala',
       '2015 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2015-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2015-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2015, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2015
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Sinhala',
       '2016 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2016-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2016-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2016, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2016
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Sinhala',
       '2017 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2017-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2017-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2017, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2017
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Sinhala',
       '2018 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2018-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2018-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2018, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2018
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Sinhala',
       '2019 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2019-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2019-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2019, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2019
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Sinhala',
       '2020 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2020-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2020-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2020, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2020
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Sinhala',
       '2021 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2021-science-for-technology-sinhala-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2021, Sinhala medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2021
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Sinhala',
       '2022 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2022-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2022-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2022, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2022
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Sinhala',
       '2023 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2023-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2023-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2023, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2023
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Sinhala',
       '2024 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2024-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2024-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2024, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2024
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Sinhala',
       '2025 Science for Technology Sinhala Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2025-science-for-technology-sinhala-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2025-science-for-technology-sinhala-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2025, Sinhala medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2025
    AND `medium` = 'Sinhala'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2015-accounting-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2016-accounting-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2017-accounting-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2018-accounting-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Accounting paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2019-accounting-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2019-accounting-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2021-accounting-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2021-accounting-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2022-accounting-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2022-accounting-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2023-accounting-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2023-accounting-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2024-accounting-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2024-accounting-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Accounting',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Accounting Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/accounting/2025-accounting-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/accounting/2025-accounting-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Accounting paper, 2025, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Accounting' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Agricultural Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2016-agricultural-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Agricultural Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2017-agricultural-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Agricultural Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2018-agricultural-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Agricultural Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2022-agricultural-science-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/agricultural-science/2022-agricultural-science-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Agricultural Science paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agricultural Science',
       'Biological Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Agricultural Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agricultural-science/2023-agricultural-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agricultural Science paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agricultural Science' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Agro Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2015-agro-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Agro Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2016-agro-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Agro Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2017-agro-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Agro Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2020-agro-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Agro Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Agro Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/agro-technology/2023-agro-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Agro Technology paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Agro Technology' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Art Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2016-art-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Art Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2017-art-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Art Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2018-art-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Art Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2019-art-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Art Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2023-art-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/art/2023-art-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Art paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Art',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Art Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/art/2025-art-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Art paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Art' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2018-biology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2018-biology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2018, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2019-biology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2019-biology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2020-biology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2020-biology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2020, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2021-biology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Biology paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2022-biology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2022-biology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2023-biology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2023-biology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Biology',
       'Biological Science', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Biology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/biology/2024-biology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/biology/2024-biology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Biology paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Biology' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2016-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2017-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2018-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2019-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2021-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2022-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2023-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Resource Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Bio Resource Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-resource-technology/2025-bio-resource-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Resource Technology paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Resource Technology' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2015-bio-systems-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2016-bio-systems-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2016-bio-systems-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2016, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2017-bio-systems-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2018-bio-systems-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2018-bio-systems-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2018, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2020-bio-systems-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2021-bio-systems-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2021-bio-systems-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Bio Systems Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Bio Systems Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/bio-systems-technology/2023-bio-systems-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Bio Systems Technology paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Bio Systems Technology' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Buddhism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2015-buddhism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Buddhism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2019-buddhism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhism',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Buddhism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhism/2022-buddhism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhism paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhism' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2015-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2016-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2017-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2018-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2019-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2020-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2022-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Buddhist Civilization',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Buddhist Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/buddhist-civilization/2023-buddhist-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Buddhist Civilization paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Buddhist Civilization' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2016-business-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2018-business-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2019-business-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2020-business-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2021-business-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2021-business-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2022-business-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Business Studies paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2023-business-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2023-business-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Business Studies',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Business Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/business-studies/2025-business-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/business-studies/2025-business-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Business Studies paper, 2025, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Business Studies' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2016-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2016-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2016, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2017-chemistry-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2018-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2018-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2018, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2019-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2019-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2020-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2020-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2020, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2021-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2021-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2022-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2022-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2023-chemistry-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/chemistry/2023-chemistry-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Chemistry paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Chemistry',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Chemistry Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/chemistry/2024-chemistry-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Chemistry paper, 2024, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Chemistry' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2015-christian-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2016-christian-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2018-christian-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2019-christian-civilization-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christian-civilization/2019-christian-civilization-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Christian Civilization paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2021-christian-civilization-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christian-civilization/2021-christian-civilization-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Christian Civilization paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2023-christian-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christian Civilization',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Christian Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christian-civilization/2025-christian-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christian Civilization paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christian Civilization' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2016-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2017-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2018-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2019-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2020-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2021-christianity-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christianity/2021-christianity-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Christianity paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2022-christianity-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/christianity/2022-christianity-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Christianity paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2023-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Christianity',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Christianity Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/christianity/2025-christianity-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Christianity paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Christianity' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Combined Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2016-combined-mathematics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2016-combined-mathematics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2016, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Combined Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2017-combined-mathematics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2017-combined-mathematics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2017, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Combined Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2018-combined-mathematics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2018-combined-mathematics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2018, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Combined Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2023-combined-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Combined Mathematics paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Combined Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2024-combined-mathematics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/combined-mathematics/2024-combined-mathematics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Combined Mathematics paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Combined Mathematics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Combined Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/combined-mathematics/2025-combined-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Combined Mathematics paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Combined Mathematics' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2016-common-general-test-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2017-common-general-test-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2018-common-general-test-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2019-common-general-test-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Common General Test paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2020-common-general-test-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2020-common-general-test-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2020, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2021-common-general-test-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2021-common-general-test-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Common General Test',
       'Common', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Common General Test Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/common-general-test/2022-common-general-test-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/common-general-test/2022-common-general-test-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Common General Test paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Common General Test' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2015-communication-and-media-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2016-communication-and-media-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2017-communication-and-media-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2018-communication-and-media-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2018-communication-and-media-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2018, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2019-communication-and-media-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2019-communication-and-media-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2021-communication-and-media-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2021-communication-and-media-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2022-communication-and-media-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2022-communication-and-media-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2023-communication-and-media-studies-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2023-communication-and-media-studies-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Communication & Media Studies',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Communication & Media Studies Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/communication-and-media-studies/2025-communication-and-media-studies-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Communication & Media Studies paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Communication & Media Studies' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2015-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2016-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2018-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2019-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2020-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2021-drama-and-theatre-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2021-drama-and-theatre-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Drama and Theatre paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2022-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2023-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Drama and Theatre',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Drama and Theatre Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/drama-and-theatre/2025-drama-and-theatre-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Drama and Theatre paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Drama and Theatre' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2015-economics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2016-economics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2017-economics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2018-economics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2020-economics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2022-economics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Economics paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2023-economics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2023-economics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2024-economics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2024-economics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Economics',
       'Commerce', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Economics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/economics/2025-economics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/economics/2025-economics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Economics paper, 2025, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Economics' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2015-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2016-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2017-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2018-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2019-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2020-electric-electronic-and-information-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2020-electric-electronic-and-information-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2020, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2021-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2023-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Electric, Electronic and Information Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Electric, Electronic and Information Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/electric-electronic-and-information-technology/2025-electric-electronic-and-information-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Electric, Electronic and Information Technology paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Electric, Electronic and Information Technology' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2016-engineering-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2016-engineering-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2016, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2017-engineering-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2018-engineering-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2020-engineering-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2020-engineering-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2020, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2021-engineering-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2021-engineering-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2022-engineering-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2023-engineering-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2023-engineering-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2024-engineering-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/engineering-technology/2024-engineering-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Engineering Technology paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Engineering Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Engineering Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/engineering-technology/2025-engineering-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Engineering Technology paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Engineering Technology' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2015-geography-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2016-geography-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2018-geography-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2020-geography-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2021-geography-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2021-geography-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2022-geography-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2022-geography-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2023-geography-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/geography/2023-geography-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Geography paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Geography',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Geography Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/geography/2025-geography-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Geography paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Geography' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2016-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2017-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2018-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2020-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2021-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2022-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2023-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Higher Mathematics',
       'Physical Science', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Higher Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/higher-mathematics/2025-higher-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Higher Mathematics paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Higher Mathematics' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2015-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2016-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2017-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2018-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2019-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2020-hindu-civilization-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/hindu-civilization/2020-hindu-civilization-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Hindu Civilization paper, 2020, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2022-hindu-civilization-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/hindu-civilization/2022-hindu-civilization-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Hindu Civilization paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2023-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hindu Civilization',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Hindu Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hindu-civilization/2025-hindu-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hindu Civilization paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hindu Civilization' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2015-hinduism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2016-hinduism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2017-hinduism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2018-hinduism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2019-hinduism-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/hinduism/2019-hinduism-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Hinduism paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2021-hinduism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2022-hinduism-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/hinduism/2022-hinduism-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Hinduism paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Hinduism',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Hinduism Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/hinduism/2023-hinduism-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Hinduism paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Hinduism' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2015-ict-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2016-ict-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2017-ict-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2018-ict-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level ICT paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2019-ict-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2019-ict-tamil-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2023-ict-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2023-ict-tamil-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2023, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2024-ict-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2024-ict-tamil-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'ICT',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 ICT Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/ict/2025-ict-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/ict/2025-ict-tamil-scheme.pdf', 'Official G.C.E. Advanced Level ICT paper, 2025, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'ICT' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2015-islam-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2017-islam-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2018-islam-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2019-islam-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2021-islam-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2024-islam-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam/2024-islam-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Islam paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Islam Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam/2025-islam-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2015-islam-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2015, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2017-islam-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2018-islam-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2019-islam-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2020-islam-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2021-islam-civilization-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2021-islam-civilization-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2022-islam-civilization-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/islam-civilization/2022-islam-civilization-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Islam Civilization paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Islam Civilization',
       'Arts', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Islam Civilization Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/islam-civilization/2023-islam-civilization-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Islam Civilization paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Islam Civilization' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2016-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2017-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2018-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2020-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2021-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2021, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2022-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2022, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Mathematics',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Mathematics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/mathematics/2023-mathematics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Mathematics paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Mathematics' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2016-physics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2017-physics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2017-physics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2017, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2018-physics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2020-physics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2021-physics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2021-physics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2022-physics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2022-physics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2023, 'G.C.E. Advanced Level', 'Tamil',
       '2023 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2023-physics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2023, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2023
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2024, 'G.C.E. Advanced Level', 'Tamil',
       '2024 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2024-physics-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/physics/2024-physics-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Physics paper, 2024, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2024
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Physics',
       'Physical Science', 2026, 'G.C.E. Advanced Level', 'Tamil',
       '2026 Physics Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/physics/2026-physics-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Physics paper, 2026, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Physics' AND `year` = 2026
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2016, 'G.C.E. Advanced Level', 'Tamil',
       '2016 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2016-political-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2016, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2016
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2018, 'G.C.E. Advanced Level', 'Tamil',
       '2018 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2018-political-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2018, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2018
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2019-political-science-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2019-political-science-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2019, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2020-political-science-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Political Science paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2021-political-science-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2021-political-science-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2022-political-science-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2022-political-science-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Political Science',
       'Arts', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Political Science Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/political-science/2025-political-science-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/political-science/2025-political-science-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Political Science paper, 2025, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Political Science' AND `year` = 2025
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2015, 'G.C.E. Advanced Level', 'Tamil',
       '2015 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2015-science-for-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2015-science-for-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2015, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2015
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2017, 'G.C.E. Advanced Level', 'Tamil',
       '2017 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2017-science-for-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2017, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2017
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2019, 'G.C.E. Advanced Level', 'Tamil',
       '2019 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2019-science-for-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2019, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2019
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2020, 'G.C.E. Advanced Level', 'Tamil',
       '2020 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2020-science-for-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2020, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2020
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2021, 'G.C.E. Advanced Level', 'Tamil',
       '2021 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2021-science-for-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2021-science-for-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2021, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2021
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2022, 'G.C.E. Advanced Level', 'Tamil',
       '2022 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2022-science-for-technology-tamil-paper.pdf',
       'https://app.quizontal.site/uploads/Papers/science-for-technology/2022-science-for-technology-tamil-scheme.pdf', 'Official G.C.E. Advanced Level Science for Technology paper, 2022, Tamil medium. Marking scheme included.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2022
    AND `medium` = 'Tamil'
);

INSERT INTO `past_papers`
  (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`,
   `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`,
   `file_size`, `pages`, `status`)
SELECT 'Grade 13', 'A/L', 'Science for Technology',
       'Technology', 2025, 'G.C.E. Advanced Level', 'Tamil',
       '2025 Science for Technology Tamil Medium Past Paper', 'https://app.quizontal.site/uploads/Papers/science-for-technology/2025-science-for-technology-tamil-paper.pdf',
       NULL, 'Official G.C.E. Advanced Level Science for Technology paper, 2025, Tamil medium.',
       '', 0, 'published'
WHERE NOT EXISTS (
  SELECT 1 FROM `past_papers`
  WHERE `subject` = 'Science for Technology' AND `year` = 2025
    AND `medium` = 'Tamil'
);
