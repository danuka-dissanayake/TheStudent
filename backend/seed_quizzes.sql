-- ====================================================================
-- TheStudent — starter quizzes
-- --------------------------------------------------------------------
-- Three real, curriculum-aligned quizzes for a fresh install:
--
--   1. Current Electricity        — O/L Science, Grade 11
--   2. Magnetism & Induction      — A/L Physics, Grade 13, Physical Science
--   3. Nutrition & Digestion      — Grade 9 Science
--
-- Questions follow the topics in the NIE Grade 10/11 Science syllabus
-- (current electricity: Ohm's law, factors affecting resistance, series and
-- parallel circuits, household wiring and kW h) and the A/L Physics
-- magnetism/induction unit. Every question carries a worked explanation, which
-- the app shows after the attempt is graded.
--
-- Run this AFTER `database_quizzes.sql`.
--
-- Safe to re-run: each quiz is removed by its stable `code` first and the
-- foreign keys cascade, so its questions and options go with it. Only run it on
-- a server where these starter quizzes are not yet in use by students.
--
-- Edit them afterwards in the app: Admin panel -> Quizzes -> Edit.
-- ====================================================================

SET NAMES utf8mb4;


-- --------------------------------------------------------------------
-- Current Electricity — O/L Science
-- --------------------------------------------------------------------
DELETE FROM `quizzes` WHERE `code` = 'ol-science-current-electricity';
INSERT INTO `quizzes` (`code`, `title`, `description`, `subject`, `grade`, `category`,
  `stream`, `medium`, `time_limit_seconds`, `pass_percent`, `shuffle_questions`,
  `shuffle_options`, `exp_reward`, `status`) VALUES
( 'ol-science-current-electricity', 'Current Electricity — O/L Science',
  'Ten questions on the O/L Science electricity unit: current, potential difference, Ohm''s law, series and parallel resistors, power and safe household wiring.',
  'Science', 'Grade 11', 'O/L', NULL, 'English', 900, 60, 1, 1, 20, 'published');
SET @quiz = LAST_INSERT_ID();

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 0, 'Which is the SI unit of electric current?',
  'Current is measured in amperes (A). The coulomb is the unit of charge, the volt the unit of potential difference and the ohm the unit of resistance.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Ampere', 1),
  (@q, 1, 'Coulomb', 0),
  (@q, 2, 'Volt', 0),
  (@q, 3, 'Ohm', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 1, 'A potential difference of 6 V is applied across a resistor of 3 Ω. What current flows?',
  'From Ohm''s law, I = V / R = 6 V / 3 Ω = 2 A.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '0.5 A', 0),
  (@q, 1, '2 A', 1),
  (@q, 2, '3 A', 0),
  (@q, 3, '18 A', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 2, 'Two 4 Ω resistors are connected in series. What is the total resistance?',
  'In series the resistances add: R = 4 Ω + 4 Ω = 8 Ω.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '2 Ω', 0),
  (@q, 1, '4 Ω', 0),
  (@q, 2, '8 Ω', 1),
  (@q, 3, '16 Ω', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 3, 'Two 4 Ω resistors are connected in parallel. What is the total resistance?',
  'In parallel 1/R = 1/4 + 1/4 = 1/2, so R = 2 Ω. The combined resistance is always less than the smallest resistor.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '2 Ω', 1),
  (@q, 1, '4 Ω', 0),
  (@q, 2, '8 Ω', 0),
  (@q, 3, '16 Ω', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 4, 'Which statement about the resistance of a metal wire is correct?',
  'For a wire of the same material and cross-section, doubling the length doubles the resistance. A thicker wire (larger cross-sectional area) has a smaller resistance.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'It increases when the wire is made longer', 1),
  (@q, 1, 'It increases when the wire is made thicker', 0),
  (@q, 2, 'It does not depend on the material', 0),
  (@q, 3, 'It is measured in volts', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 5, 'In which direction does conventional current flow in an external circuit?',
  'Conventional current is taken from the positive terminal to the negative terminal through the external circuit, which is opposite to the direction of electron flow.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'From the positive terminal to the negative terminal', 1),
  (@q, 1, 'From the negative terminal to the positive terminal', 0),
  (@q, 2, 'In the same direction as the electrons', 0),
  (@q, 3, 'In both directions at once', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 6, 'Which unit is used on a household electricity bill to measure energy consumed?',
  'Electrical energy is billed in kilowatt-hours (kW h). One kW h is the energy used by a 1 kW appliance running for one hour.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Kilowatt-hour (kW h)', 1),
  (@q, 1, 'Watt (W)', 0),
  (@q, 2, 'Volt (V)', 0),
  (@q, 3, 'Ampere (A)', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 7, 'A 60 W lamp is connected to a 240 V supply. What current does it draw?',
  'From P = VI, I = P / V = 60 W / 240 V = 0.25 A.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '0.25 A', 1),
  (@q, 1, '4 A', 0),
  (@q, 2, '60 A', 0),
  (@q, 3, '240 A', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 8, 'How are an ammeter and a voltmeter connected in a circuit?',
  'An ammeter is connected in series so the current passes through it; a voltmeter is connected in parallel across the component whose potential difference is being measured.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Ammeter in series, voltmeter in parallel', 1),
  (@q, 1, 'Ammeter in parallel, voltmeter in series', 0),
  (@q, 2, 'Both in series', 0),
  (@q, 3, 'Both in parallel', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 9, 'What is the purpose of a fuse in a household circuit?',
  'A fuse contains a thin wire that melts and breaks the circuit when the current becomes too large, protecting the wiring and appliances from overheating.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'It melts and breaks the circuit when the current is too large', 1),
  (@q, 1, 'It stores electrical energy for later use', 0),
  (@q, 2, 'It increases the current in the circuit', 0),
  (@q, 3, 'It changes alternating current into direct current', 0);

-- --------------------------------------------------------------------
-- Magnetism & Electromagnetic Induction — A/L Physics
-- --------------------------------------------------------------------
DELETE FROM `quizzes` WHERE `code` = 'al-physics-magnetism-induction';
INSERT INTO `quizzes` (`code`, `title`, `description`, `subject`, `grade`, `category`,
  `stream`, `medium`, `time_limit_seconds`, `pass_percent`, `shuffle_questions`,
  `shuffle_options`, `exp_reward`, `status`) VALUES
( 'al-physics-magnetism-induction', 'Magnetism & Electromagnetic Induction — A/L Physics',
  'Ten questions on the A/L Physics magnetism unit: flux and flux density, force on a current-carrying conductor, Faraday''s and Lenz''s laws, eddy currents and the ideal transformer.',
  'Physics', 'Grade 13', 'A/L', 'Physical Science', 'English', 1200, 60, 1, 1, 20, 'published');
SET @quiz = LAST_INSERT_ID();

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 0, 'Which is the SI unit of magnetic flux density?',
  'Magnetic flux density B is measured in tesla (T), where 1 T = 1 Wb m^-2. The weber is the unit of magnetic flux.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Tesla', 1),
  (@q, 1, 'Weber', 0),
  (@q, 2, 'Henry', 0),
  (@q, 3, 'Gauss', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 1, 'A straight wire of length l carrying current I is placed at right angles to a uniform magnetic field of flux density B. What is the magnitude of the force on the wire?',
  'For a conductor perpendicular to the field, F = BIl. If the wire were parallel to the field the force would be zero.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'F = BIl', 1),
  (@q, 1, 'F = BI / l', 0),
  (@q, 2, 'F = B l / I', 0),
  (@q, 3, 'F = B I^2 l', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 2, 'Faraday''s law states that the magnitude of the induced electromotive force is proportional to what?',
  'The induced emf equals the rate of change of magnetic flux linkage, so a faster change of flux gives a larger emf.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'The rate of change of magnetic flux linkage', 1),
  (@q, 1, 'The total magnetic flux only', 0),
  (@q, 2, 'The resistance of the circuit', 0),
  (@q, 3, 'The current already flowing in the coil', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 3, 'Lenz''s law gives the direction of an induced current. Which statement is correct?',
  'The induced current always flows in a direction that opposes the change producing it. This is a statement of conservation of energy.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'It opposes the change that produced it', 1),
  (@q, 1, 'It aids the change that produced it', 0),
  (@q, 2, 'It is always clockwise', 0),
  (@q, 3, 'It depends only on the resistance of the circuit', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 4, 'A bar magnet falls through a vertical copper tube. Why does it fall more slowly than in free air?',
  'The changing flux induces eddy currents in the copper. By Lenz''s law these currents produce a magnetic field that opposes the motion of the magnet.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Eddy currents in the copper oppose the motion of the magnet', 1),
  (@q, 1, 'Copper is magnetic and attracts the magnet upwards', 0),
  (@q, 2, 'The magnet loses its magnetism inside the tube', 0),
  (@q, 3, 'Air resistance is greater inside the tube', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 5, 'Which is the SI unit of magnetic flux?',
  'Magnetic flux is measured in webers (Wb). One weber equals one tesla square metre.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Weber', 1),
  (@q, 1, 'Tesla', 0),
  (@q, 2, 'Newton per metre', 0),
  (@q, 3, 'Volt per metre', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 6, 'A uniform magnetic field of flux density 0.5 T passes at right angles through an area of 0.2 m^2. What is the magnetic flux through the area?',
  'With the field perpendicular to the area, flux = B x A = 0.5 T x 0.2 m^2 = 0.1 Wb.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '0.1 Wb', 1),
  (@q, 1, '0.4 Wb', 0),
  (@q, 2, '2.5 Wb', 0),
  (@q, 3, '10 Wb', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 7, 'An ideal transformer has 400 turns in the primary and 40 turns in the secondary. The primary is connected to 240 V. What is the secondary voltage?',
  'For an ideal transformer Vs / Vp = Ns / Np, so Vs = 240 V x (40 / 400) = 24 V. This is a step-down transformer.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '24 V', 1),
  (@q, 1, '240 V', 0),
  (@q, 2, '2400 V', 0),
  (@q, 3, '2.4 V', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 8, 'Which rule gives the direction of the force on a current-carrying conductor in a magnetic field?',
  'Fleming''s left-hand rule is the motor rule: the first finger points along the field, the second along the current, and the thumb gives the direction of the force.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Fleming''s left-hand rule', 1),
  (@q, 1, 'Fleming''s right-hand rule', 0),
  (@q, 2, 'The right-hand grip rule', 0),
  (@q, 3, 'Snell''s law', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 9, 'A coil of 100 turns experiences a change of magnetic flux of 0.02 Wb in 0.1 s. What is the average induced emf?',
  'Average emf = N x (change in flux / time) = 100 x (0.02 / 0.1) = 20 V.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, '20 V', 1),
  (@q, 1, '2 V', 0),
  (@q, 2, '200 V', 0),
  (@q, 3, '0.2 V', 0);

-- --------------------------------------------------------------------
-- Nutrition & Digestion — Grade 9 Science
-- --------------------------------------------------------------------
DELETE FROM `quizzes` WHERE `code` = 'grade-9-nutrition-digestion';
INSERT INTO `quizzes` (`code`, `title`, `description`, `subject`, `grade`, `category`,
  `stream`, `medium`, `time_limit_seconds`, `pass_percent`, `shuffle_questions`,
  `shuffle_options`, `exp_reward`, `status`) VALUES
( 'grade-9-nutrition-digestion', 'Nutrition & Digestion — Grade 9 Science',
  'Ten questions on the junior secondary nutrition unit: digestive organs and enzymes, absorption, and the deficiency diseases caused by missing nutrients.',
  'Science', 'Grade 9', 'Junior', NULL, 'English', 720, 60, 1, 1, 20, 'published');
SET @quiz = LAST_INSERT_ID();

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 0, 'In which organ is most of the digested food absorbed into the blood?',
  'The small intestine has a long, folded lining covered with villi, which give a very large surface area for absorption.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Small intestine', 1),
  (@q, 1, 'Stomach', 0),
  (@q, 2, 'Large intestine', 0),
  (@q, 3, 'Oesophagus', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 1, 'Which enzyme in saliva begins the digestion of starch?',
  'Salivary amylase (ptyalin) breaks starch down into simpler sugars such as maltose while the food is still in the mouth.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Amylase', 1),
  (@q, 1, 'Pepsin', 0),
  (@q, 2, 'Lipase', 0),
  (@q, 3, 'Trypsin', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 2, 'Which food component is mainly digested in the stomach?',
  'The stomach secretes hydrochloric acid and pepsin, which begin the digestion of proteins.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Proteins', 1),
  (@q, 1, 'Carbohydrates', 0),
  (@q, 2, 'Fats', 0),
  (@q, 3, 'Vitamins', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 3, 'Where is bile produced?',
  'Bile is produced by the liver, stored in the gall bladder and released into the small intestine, where it emulsifies fats into small droplets.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Liver', 1),
  (@q, 1, 'Gall bladder', 0),
  (@q, 2, 'Pancreas', 0),
  (@q, 3, 'Stomach', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 4, 'Which deficiency disease is caused by a lack of vitamin C?',
  'Vitamin C is needed for healthy gums, skin and blood vessels. A lack of it causes scurvy, with bleeding gums and slow healing of wounds.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Scurvy', 1),
  (@q, 1, 'Rickets', 0),
  (@q, 2, 'Beri-beri', 0),
  (@q, 3, 'Night blindness', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 5, 'A person whose diet lacks iodine is most likely to develop which condition?',
  'Iodine is needed by the thyroid gland to make thyroxine. A lack of iodine causes the thyroid to enlarge, a condition called goitre.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Goitre', 1),
  (@q, 1, 'Anaemia', 0),
  (@q, 2, 'Scurvy', 0),
  (@q, 3, 'Rickets', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 6, 'Which of these is a good source of protein?',
  'Eggs, fish, milk and pulses are rich in protein, which is needed for growth and for repairing body tissues.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Eggs and fish', 1),
  (@q, 1, 'Rice and potatoes', 0),
  (@q, 2, 'Coconut oil and butter', 0),
  (@q, 3, 'Sugar and honey', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 7, 'What is the main function of roughage (dietary fibre) in the diet?',
  'Fibre cannot be digested, but it adds bulk to the food and helps the muscles of the gut move it along, preventing constipation.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'It helps food move through the gut and prevents constipation', 1),
  (@q, 1, 'It supplies most of the energy in the diet', 0),
  (@q, 2, 'It is absorbed directly into the blood', 0),
  (@q, 3, 'It builds muscle tissue', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 8, 'In which part of the digestive system is most of the water from undigested food absorbed?',
  'By the time food reaches the large intestine most nutrients have been absorbed; the large intestine absorbs much of the remaining water, forming semi-solid faeces.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Large intestine', 1),
  (@q, 1, 'Stomach', 0),
  (@q, 2, 'Small intestine', 0),
  (@q, 3, 'Mouth', 0);

INSERT INTO `quiz_questions` (`quiz_id`, `position`, `prompt`, `explanation`, `marks`) VALUES
(@quiz, 9, 'Kwashiorkor in young children is caused mainly by a deficiency of which nutrient?',
  'Kwashiorkor results from a diet with too little protein, even when enough energy food is available. It shows as swelling of the body, particularly the legs and face.', 1);
SET @q = LAST_INSERT_ID();
INSERT INTO `quiz_options` (`question_id`, `position`, `label`, `is_correct`) VALUES
  (@q, 0, 'Protein', 1),
  (@q, 1, 'Vitamin A', 0),
  (@q, 2, 'Iron', 0),
  (@q, 3, 'Calcium', 0);

-- ====================================================================
-- Verify (optional):
--   SELECT q.id, q.title, q.status, COUNT(qq.id) AS questions
--   FROM quizzes q LEFT JOIN quiz_questions qq ON qq.quiz_id = q.id
--   GROUP BY q.id ORDER BY q.id;
-- ====================================================================
