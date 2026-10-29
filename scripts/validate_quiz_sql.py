#!/usr/bin/env python3
"""
Validate a quiz .sql file before it reaches the database.

    python3 scripts/validate_quiz_sql.py quizzes.sql [--fix fixes.sql]

Catches the things that silently break a quiz in the app rather than erroring
in MySQL: a grade or category the app's chips never match, a quiz nobody can
find, questions with two correct answers, duplicate prompts, missing
explanations. Exits 1 if anything blocking is found, so it can gate an import.
"""

from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path

# ── what the app and the schema actually accept ──────────────────────────────

# QuizzesScreen.matchesChip() only ever matches these, and QuizAdminScreen's
# CATEGORY_OPTIONS is exactly this list.
CATEGORIES = {"A/L", "O/L", "Junior", "Provincial", "University"}
GRADES = {
    "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11",
    "Grade 12", "Grade 13", "Undergraduate",
}
MEDIUMS = {"English", "Sinhala", "Tamil"}
STATUSES = {"draft", "published", "archived"}

# Subject names the papers library already uses, so quizzes and papers agree.
SUBJECTS = {
    "Accounting", "Agricultural Science", "Agro Technology", "Art",
    "Bio Resource Technology", "Bio Systems Technology", "Biology", "Buddhism",
    "Buddhist Civilization", "Business Statistics", "Business Studies",
    "Chemistry", "Christian Civilization", "Christianity", "Combined Mathematics",
    "Common General Test", "Communication & Media Studies", "Drama and Theatre",
    "Economics", "Electric, Electronic and Information Technology",
    "Engineering Technology", "General English", "Geography",
    "Higher Mathematics", "Hindu Civilization", "Hinduism", "ICT", "Islam",
    "Islam Civilization", "Mathematics", "Physics", "Political Science",
    "Science", "Science for Technology", "Sinhala", "Tamil", "English",
    "History", "GIT", "Health Science", "Commerce", "Religion",
    "Home Economics", "Arabic", "Chinese", "French", "German", "Japanese",
    "Korean", "Russian",
}

# A quiz needs enough questions to be worth a time limit and an EXP reward.
MIN_QUESTIONS = 5
MIN_EXPLANATION = 20


def split_values(blob: str) -> list[str]:
    """Split a SQL VALUES tuple body into its values, respecting quotes."""
    values: list[str] = []
    current: list[str] = []
    in_quote = False
    index = 0
    while index < len(blob):
        char = blob[index]
        if in_quote:
            if char == "'" and index + 1 < len(blob) and blob[index + 1] == "'":
                current.append("'")
                index += 2
                continue
            if char == "'":
                in_quote = False
            else:
                current.append(char)
        else:
            if char == "'":
                in_quote = True
            elif char == ",":
                values.append("".join(current).strip())
                current = []
            else:
                current.append(char)
        index += 1
    if "".join(current).strip():
        values.append("".join(current).strip())
    return values


def norm(text: str) -> str:
    """Letters and digits only, case-folded — keeps Sinhala intact for dedupe."""
    return "".join(ch for ch in unicodedata.normalize("NFKC", text).casefold()
                   if ch.isalnum())


STATEMENT = re.compile(
    r"INSERT\s+INTO\s+`?(quizzes|quiz_questions|quiz_options)`?\s*\([^)]*\)"
    r"\s*VALUES\s*(.*?);",
    re.IGNORECASE | re.DOTALL,
)
TUPLE = re.compile(r"\((?:[^()']|'(?:[^']|'')*')*\)")


def iter_statements(sql: str):
    """Yield (table, [parsed value tuples]) in file order.

    Order matters: every `quiz_options` block belongs to the question inserted
    just before it, because that is what LAST_INSERT_ID() held.
    """
    for match in STATEMENT.finditer(sql):
        table = match.group(1).lower()
        rows = [split_values(t[1:-1]) for t in TUPLE.findall(match.group(2))]
        yield table, rows


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[1])
    parser.add_argument("sql", type=Path)
    parser.add_argument("--fix", type=Path, metavar="FILE",
                        help="write UPDATE statements correcting grade/category")
    args = parser.parse_args(argv)

    sql = args.sql.read_text(encoding="utf-8")
    if "SET NAMES utf8mb4" not in sql:
        print("warn: no `SET NAMES utf8mb4;` — Sinhala/Tamil may import as mojibake")

    blocking: list[str] = []
    warnings: list[str] = []
    fixes: list[str] = []

    # ── one ordered pass over every statement ────────────────────────────────
    quizzes: list[dict] = []
    questions: list[dict] = []
    pending_question: dict | None = None
    quiz_for_questions = -1

    for table, rows in iter_statements(sql):
        for values in rows:
            if table == "quizzes":
                # code, title, description, subject, grade, category, stream,
                # medium, time_limit, pass_percent, shuffle_q, shuffle_o,
                # exp_reward, status
                if len(values) < 14:
                    blocking.append(
                        f"quiz row has {len(values)} columns, expected 14"
                    )
                    continue
                quizzes.append({
                    "code": values[0], "title": values[1], "subject": values[3],
                    "grade": values[4], "category": values[5],
                    "medium": values[7], "exp": values[12], "status": values[13],
                    "questions": 0,
                })
                quiz_for_questions = len(quizzes) - 1
            elif table == "quiz_questions":
                # quiz ref, position, prompt, explanation, marks
                if len(values) < 5:
                    blocking.append(
                        f"question row has {len(values)} columns, expected 5"
                    )
                    continue
                pending_question = {
                    "quiz": quiz_for_questions,
                    "position": values[1],
                    "prompt": values[2],
                    "explanation": values[3],
                    "options": [],
                }
                questions.append(pending_question)
                if 0 <= quiz_for_questions < len(quizzes):
                    quizzes[quiz_for_questions]["questions"] += 1
            elif table == "quiz_options":
                # question ref, position, label, is_correct
                if len(values) < 4:
                    blocking.append(
                        f"option row has {len(values)} columns, expected 4"
                    )
                    continue
                if pending_question is None:
                    blocking.append("options appear before any question")
                    continue
                pending_question["options"].append(values[3])

    if not quizzes:
        print("error: no INSERT INTO `quizzes` found")
        return 1

    # ── quiz-level rules ────────────────────────────────────────────────────
    for quiz in quizzes:
        code = quiz["code"]
        if quiz["grade"] not in GRADES:
            blocking.append(
                f"{code}: grade '{quiz['grade']}' is one the app never matches "
                f"(use 'Grade 13' / 'Grade 12' / 'Grade 11' …)"
            )
            fixes.append(
                f"UPDATE `quizzes` SET `grade` = 'Grade 13' WHERE `code` = "
                f"'{code}' AND `grade` = '{quiz['grade']}';"
            )
        if quiz["category"] not in CATEGORIES:
            blocking.append(
                f"{code}: category '{quiz['category']}' is not a chip the app "
                f"shows ({', '.join(sorted(CATEGORIES))}) — the quiz would appear "
                f"under no filter at all"
            )
            fixes.append(
                f"UPDATE `quizzes` SET `category` = 'A/L' WHERE `code` = "
                f"'{code}' AND `category` = '{quiz['category']}';"
            )
        if quiz["medium"] not in MEDIUMS:
            blocking.append(
                f"{code}: medium '{quiz['medium']}' is not one of {MEDIUMS}"
            )
        if quiz["status"] not in STATUSES:
            blocking.append(
                f"{code}: status '{quiz['status']}' is not one of {STATUSES}"
            )
        if quiz["subject"] not in SUBJECTS:
            warnings.append(
                f"{code}: subject '{quiz['subject']}' is not used by any paper — "
                f"check the spelling so subject filters line up"
            )
        try:
            reward = int(quiz["exp"])
            if not 0 <= reward <= 40:
                warnings.append(
                    f"{code}: exp_reward {reward} is outside the 0–40 range"
                )
        except ValueError:
            blocking.append(f"{code}: exp_reward is not a number")
        if quiz["questions"] < MIN_QUESTIONS:
            warnings.append(
                f"{code}: only {quiz['questions']} question(s) — short for a "
                f"timed quiz"
            )

    for code, count in Counter(q["code"] for q in quizzes).items():
        if count > 1:
            blocking.append(f"quiz code '{code}' appears {count} times")

    # ── question + option rules ─────────────────────────────────────────────
    seen_prompts: dict[str, int] = {}
    for question in questions:
        label = f"{quizzes[question['quiz']]['code'] if 0 <= question['quiz'] < len(quizzes) else '?'} q{question['position']}"
        prompt = question["prompt"]
        if not prompt or prompt == "NULL":
            blocking.append(f"{label}: empty prompt")
        key = norm(prompt)
        if key in seen_prompts:
            blocking.append(f"{label}: duplicates an earlier question")
        else:
            seen_prompts[key] = 1
        explanation = question["explanation"]
        if not explanation or explanation == "NULL" or len(explanation) < MIN_EXPLANATION:
            warnings.append(
                f"{label}: explanation missing or under {MIN_EXPLANATION} chars"
            )
        try:
            if int(question["position"]) < 1:
                blocking.append(f"{label}: position is not 1-based")
        except ValueError:
            blocking.append(f"{label}: position '{question['position']}' is not a number")

        options = question["options"]
        correct = sum(1 for flag in options if flag.strip() == "1")
        if correct != 1:
            blocking.append(f"{label}: {correct} correct answers (need exactly 1)")
        if len(options) != 4:
            warnings.append(f"{label}: {len(options)} options (house style is 4)")

        # ── report ───────────────────────────────────────────────────────────────
    total_options = sum(len(q["options"]) for q in questions)
    print(f"\n  {args.sql.name}")
    print(f"  {len(quizzes)} quizzes · {len(questions)} questions · "
          f"{total_options} options")
    for quiz in quizzes:
        print(f"    {quiz['code']:<16} {quiz['grade']:<9} {quiz['category']:<11} "
              f"{quiz['medium']:<8} {quiz['questions']:>2} q")

    if warnings:
        print(f"\n  warnings ({len(warnings)})")
        for warning in warnings:
            print(f"    ! {warning}")
    if blocking:
        print(f"\n  BLOCKING ({len(blocking)})")
        for problem in blocking:
            print(f"    ✗ {problem}")
    else:
        print("\n  ✓ no blocking problems")

    if args.fix and fixes:
        unique = list(dict.fromkeys(fixes))
        args.fix.write_text(
            "-- Corrections for the blocking problems above\n"
            + "\n".join(unique) + "\n",
            encoding="utf-8",
        )
        print(f"\n  fixes → {args.fix} ({len(unique)} statement(s))")

    print()
    return 1 if blocking else 0


if __name__ == "__main__":
    raise SystemExit(main())
