package com.example.data.model

import java.util.Locale

/**
 * The short code printed on a subject's papers and timetables.
 *
 * Sri Lankan school subjects do not have a single official abbreviation set:
 * the Department of Examinations numbers its papers, while every paper library,
 * tuition class and short-note site prints a short code instead. The codes below
 * are the ones students actually recognise — the technology stream trio
 * (**SFT / ET / BST**), **ICT**, **AGRI** and **CM** — with the rest derived from
 * the same convention. Unknown subjects fall back to initials, so an admin can
 * upload a paper under any subject name and still get a sensible card.
 */
object SubjectCodes {

    /** Exact matches, keyed by the canonicalised subject name. */
    private val CODES: Map<String, String> = mapOf(
        // ── A/L science stream ───────────────────────────────────────────────
        "combined mathematics" to "CM",
        "combined maths" to "CM",
        "physics" to "PHY",
        "chemistry" to "CHEM",
        "biology" to "BIO",
        "agriculture" to "AGRI",
        "agricultural science" to "AGRI",
        "agriculture and food technology" to "AGRI",
        "botany" to "BOT",
        "zoology" to "ZOO",

        // ── A/L technology stream ────────────────────────────────────────────
        "science for technology" to "SFT",
        "engineering technology" to "ET",
        "biosystems technology" to "BST",
        "bio systems technology" to "BST",
        "bio resource technology" to "BRT",
        "civil technology" to "CIV",
        "mechanical technology" to "MECH",
        "electrical electronic and information technology" to "EEIT",

        // ── shared / common ──────────────────────────────────────────────────
        "information and communication technology" to "ICT",
        "ict" to "ICT",
        "general information technology" to "GIT",
        "general english" to "GEN ENG",
        "english" to "ENG",
        "english literature" to "ELIT",
        "sinhala" to "SIN",
        "sinhala literature" to "SLIT",
        "tamil" to "TAM",
        "mathematics" to "MATH",
        "science" to "SCI",
        "computer science" to "CS",
        "computer science and engineering" to "CSE",
        "information systems" to "IS",

        // ── commerce ─────────────────────────────────────────────────────────
        "accounting" to "ACC",
        "business studies" to "BS",
        "business statistics" to "BSTAT",
        "economics" to "ECON",
        "business and accounting studies" to "BAS",
        "entrepreneurship" to "ENT",
        "management" to "MGT",
        "management studies" to "MGT",
        "commerce" to "COM",

        // ── arts ─────────────────────────────────────────────────────────────
        "history" to "HIST",
        "geography" to "GEO",
        "political science" to "POL",
        "media and communication studies" to "MCS",
        "media" to "MCS",
        "logic and scientific method" to "LSM",
        "buddhist civilization" to "BC",
        "buddhism" to "BC",
        "christianity" to "CHR",
        "hinduism" to "HIN",
        "islam" to "ISL",
        "religion" to "REL",
        "art" to "ART",
        "dancing" to "DAN",
        "drama and theatre" to "DRA",
        "music" to "MUS",
        "eastern music" to "EMUS",
        "western music" to "WMUS",
        "civic education" to "CIV",
        "health and physical education" to "HPE",
        "study skills" to "SS",

        // ── other / vocational ───────────────────────────────────────────────
        "engineering" to "ENGR",
        "communication" to "COMM",
        "academic writing" to "AW",
        "digital marketing" to "DM"
    )

    /** Words that never contribute an initial when a code has to be derived. */
    private val STOP_WORDS = setOf("for", "and", "of", "in", "the", "to", "a", "an")

    /**
     * The code for [subject]. Never blank: an unknown name is reduced to its
     * initials (or its first four letters when it is a single word).
     */
    fun of(subject: String): String {
        val key = canonical(subject)
        if (key.isEmpty()) return "PAPER"
        // A trailing qualifier ("Physics 2023", "Physics (Paper II)") should not
        // change the code of the subject it belongs to, so trailing tokens are
        // dropped one at a time until the name matches.
        var probe = key
        while (true) {
            CODES[probe]?.let { return it }
            if (!probe.contains(' ')) break
            probe = probe.substringBeforeLast(' ')
        }
        return derive(subject)
    }

    /** "Information & Communication Technology" → "information and communication technology". */
    private fun canonical(subject: String): String = subject
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Initials of the meaningful words, capped at four characters. */
    private fun derive(subject: String): String {
        val words = canonical(subject)
            .split(' ')
            .filter { it.isNotBlank() && it !in STOP_WORDS }
        if (words.isEmpty()) return "PAPER"
        if (words.size == 1) return words.first().take(4).uppercase(Locale.ROOT)
        return words.take(4).joinToString("") { it.take(1) }.uppercase(Locale.ROOT)
    }
}
