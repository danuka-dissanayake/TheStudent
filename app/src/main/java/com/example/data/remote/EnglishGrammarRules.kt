package com.example.data.remote

import com.example.data.model.GrammarFix

/**
 * A local grammar checker for spoken English.
 *
 * The tutor explains mistakes, but it should never be the *only* thing that
 * notices them: the model can be sleepy, offline, or too busy keeping the
 * conversation going. These rules run on every turn, on the phone, in
 * microseconds, and they never invent a correction for a sentence they do not
 * understand.
 *
 * The rules are deliberately narrow. Each one targets an error Sri Lankan
 * learners actually make (past tense after "did", "she don't", uncountable
 * plurals, "explain me"), because a broad rule that fires on correct English
 * teaches the wrong thing — a student who is told they are wrong when they are
 * right stops trusting the corrections entirely.
 */
object EnglishGrammarRules {

    private val irregularPastToBase = mapOf(
        "went" to "go", "saw" to "see", "had" to "have", "took" to "take",
        "came" to "come", "made" to "make", "got" to "get", "said" to "say",
        "knew" to "know", "thought" to "think", "found" to "find", "gave" to "give",
        "told" to "tell", "felt" to "feel", "brought" to "bring", "bought" to "buy",
        "caught" to "catch", "taught" to "teach", "wrote" to "write", "spoke" to "speak",
        "broke" to "break", "ate" to "eat", "drank" to "drink", "ran" to "run",
        "slept" to "sleep", "met" to "meet", "paid" to "pay", "sent" to "send",
        "built" to "build", "chose" to "choose", "drove" to "drive", "flew" to "fly",
        "grew" to "grow", "heard" to "hear", "kept" to "keep", "left" to "leave",
        "lost" to "lose", "meant" to "mean", "sold" to "sell", "spent" to "spend",
        "stood" to "stand", "swam" to "swim", "understood" to "understand",
        "wore" to "wear", "won" to "win", "began" to "begin", "sang" to "sing",
        "drove" to "drive", "threw" to "throw", "rode" to "ride", "rang" to "ring"
    )

    private val irregularParticiple = mapOf(
        "went" to "gone", "saw" to "seen", "ate" to "eaten", "did" to "done",
        "wrote" to "written", "took" to "taken", "spoke" to "spoken", "broke" to "broken",
        "drove" to "driven", "flew" to "flown", "grew" to "grown", "knew" to "known",
        "threw" to "thrown", "wore" to "worn", "chose" to "chosen", "forgot" to "forgotten",
        "got" to "got", "drank" to "drunk", "began" to "begun", "rang" to "rung",
        "sang" to "sung", "swam" to "swum", "ran" to "run", "became" to "become",
        "came" to "come", "gave" to "given", "stole" to "stolen", "hid" to "hidden",
        "bit" to "bitten", "rode" to "ridden", "rose" to "risen", "shook" to "shaken",
        "tore" to "torn", "woke" to "woken"
    )

    private val thirdPersonVerbs = listOf(
        "go", "do", "have", "come", "like", "want", "need", "know", "think", "say",
        "make", "take", "get", "give", "look", "feel", "seem", "find", "tell", "ask",
        "work", "study", "play", "live", "eat", "drink", "read", "write", "speak",
        "watch", "teach", "walk", "talk", "help", "try", "use", "learn", "wake",
        "start", "finish", "travel", "listen", "buy", "bring", "call", "visit"
    )

    /** Verbs whose -s form is spelled -es. */
    private val esVerbs = setOf(
        "go", "do", "watch", "teach", "finish", "brush", "wash", "fix", "catch", "pass"
    )

    /**
     * Action verbs a learner says in the present tense about yesterday.
     *
     * Mental verbs are deliberately absent: "I think that yesterday was busy" is
     * correct English, and a rule that fires on it would teach the wrong thing.
     */
    private val baseToPast = mapOf(
        "go" to "went", "come" to "came", "see" to "saw", "eat" to "ate",
        "drink" to "drank", "have" to "had", "do" to "did", "make" to "made",
        "take" to "took", "get" to "got", "give" to "gave", "buy" to "bought",
        "bring" to "brought", "meet" to "met", "watch" to "watched",
        "play" to "played", "study" to "studied", "visit" to "visited",
        "walk" to "walked", "talk" to "talked", "work" to "worked",
        "live" to "lived", "travel" to "travelled", "finish" to "finished",
        "start" to "started", "read" to "read", "write" to "wrote",
        "speak" to "spoke", "sleep" to "slept", "run" to "ran", "swim" to "swam",
        "sit" to "sat", "stand" to "stood",
        "leave" to "left", "keep" to "kept", "lose" to "lost",
        "send" to "sent", "pay" to "paid", "spend" to "spent", "learn" to "learned"
    )

    /** Words that put a sentence firmly in the past. */
    private val pastMarkers = listOf(
        "yesterday", "last night", "last week", "last month", "last year",
        "last weekend", "ago", "this morning", "in 2019", "in 2020", "in 2021",
        "in 2022", "in 2023", "in 2024", "in 2025"
    )

    private val uncountable = listOf(
        "information", "advice", "furniture", "luggage", "equipment",
        "homework", "news", "money", "research", "vocabulary"
    )

    private val countablePluralForms = mapOf(
        "informations" to "information",
        "advices" to "advice",
        "furnitures" to "furniture",
        "luggages" to "luggage",
        "equipments" to "equipment",
        "homeworks" to "homework",
        "vocabularies" to "vocabulary"
    )

    private val fixedPhrases = listOf(
        Triple("""\bdiscuss about\b""", "discuss", "discuss about → discuss"),
        Triple("""\bexplain me\b""", "explain to me", "explain me → explain to me"),
        Triple("""\bmarried with\b""", "married to", "married with → married to"),
        Triple("""\bdepends? of\b""", "depends on", "depend of → depend on"),
        Triple("""\bafraid from\b""", "afraid of", "afraid from → afraid of"),
        Triple("""\bgood in\b""", "good at", "good in → good at"),
        Triple("""\blisten music\b""", "listen to music", "listen music → listen to music"),
        Triple("""\bmore better\b""", "better", "more better → better"),
        Triple("""\bmost best\b""", "best", "most best → best"),
        Triple("""\bI am agree\b""", "I agree", "I am agree → I agree"),
        Triple("""\bI very like\b""", "I really like", "I very like → I really like"),
        Triple("""\bmy cousin (brother|sister)\b""", "my cousin", "cousin brother → cousin"),
        Triple("""\bwhat is your good name\b""", "what is your name", "good name → name"),
        Triple("""\bI am having a doubt\b""", "I have a question", "doubt → question"),
        Triple("""\bdo you know about\b""", "do you know", "know about → know")
    )

    /**
     * Every independent correction this sentence deserves, best first.
     *
     * Returns an empty list for a sentence that is already fine — which is the
     * common case, and the one that matters most: silence is the correct
     * feedback for correct English.
     */
    fun checkAll(sentence: String): List<GrammarFix> {
        val clean = sentence.trim()
        if (clean.length < 3) return emptyList()
        val fixes = LinkedHashMap<String, GrammarFix>()

        fun record(rule: String, corrected: String) {
            if (corrected.trim() == clean) return
            val fix = GrammarFix(original = clean, corrected = corrected.trim(), rule = rule)
            if (fix.isUseful) fixes.putIfAbsent(corrected.trim(), fix)
        }

        // 0. "I go to the beach yesterday" — a past time marker with a present
        //    verb. This is the single most common spoken error in a lesson about
        //    the past, and it is only applied to action verbs, never to thinking
        //    or feeling verbs, so a correct sentence like "I think that yesterday
        //    was busy" is left alone.
        val lower = clean.lowercase()
        if (pastMarkers.any { lower.contains(it) }) {
            Regex("""\b(I|you|we|they|he|she|it)\s+([a-zA-Z]+)\b""", RegexOption.IGNORE_CASE)
                .find(clean)
                ?.let { match ->
                    val subject = match.groupValues[1]
                    val verb = match.groupValues[2].lowercase()
                    val base = when {
                        verb in baseToPast -> verb
                        verb.endsWith("ies") -> verb.dropLast(3) + "y"
                        verb.endsWith("es") && verb.dropLast(2) in baseToPast -> verb.dropLast(2)
                        verb.endsWith("s") && verb.dropLast(1) in baseToPast -> verb.dropLast(1)
                        else -> verb
                    }
                    // "I say that yesterday was hard" is a reporting clause, not a
                    // mistake — a verb followed by "that" is never converted.
                    val followedByThat = clean
                        .substring(match.range.last + 1)
                        .trimStart()
                        .startsWith("that", ignoreCase = true)
                    val past = baseToPast[base]
                    if (past != null && past != verb && !followedByThat) {
                        val fixed = clean.substring(0, match.range.first) +
                            "$subject $past" +
                            clean.substring(match.range.last + 1)
                        record("past tense after \"${pastMarkers.first { lower.contains(it) }}\"", fixed)
                    }
                }
        }

        // 1. "did you went" / "didn't saw" — the auxiliary already carries the
        //    tense. A subject or a short adverb may sit between the two words,
        //    which is exactly how the mistake usually sounds.
        replaceMap(
            clean,
            """\b(did|didn't|did not|does|doesn't|does not)\s+""" +
                """(?:(?:you|he|she|it|they|we|i|me|your|the)\s+)?""" +
                """([a-zA-Z']+)""",
            irregularPastToBase
        )?.let { record("after \"did\", use the base verb", it) }

        // 2. "I have went" — the perfect tense needs the past participle.
        replaceMap(
            clean,
            """\b(have|has|had|haven't|hasn't|hadn't)\s+""" +
                """(?:(?:you|he|she|it|they|we|i)\s+)?""" +
                """(?:(?:ever|already|just|never)\s+)?""" +
                """([a-zA-Z']+)""",
            irregularParticiple
        )?.let { record("past participle after \"have\"", it) }

        // 3. "She don't like tea" — third person takes "doesn't".
        Regex("""\b(he|she|it)\s+don't\b""", RegexOption.IGNORE_CASE)
            .replace(clean) { m -> "${m.groupValues[1]} doesn't" }
            .takeIf { it != clean }
            ?.let { record("third person: he/she/it + doesn't", it) }

        // 4. "He go to school" — third person singular adds -s.
        val thirdPerson = Regex(
            """\b(he|she|it)\s+(${thirdPersonVerbs.joinToString("|")})\b""",
            RegexOption.IGNORE_CASE
        ).replace(clean) { m ->
            val verb = m.groupValues[2].lowercase()
            val conjugated = when {
                verb == "have" -> "has"
                verb in esVerbs -> verb + "es"
                verb.endsWith("y") && verb.length > 1 &&
                    verb[verb.length - 2] !in "aeiou" -> verb.dropLast(1) + "ies"
                else -> verb + "s"
            }
            "${m.groupValues[1]} $conjugated"
        }
        if (thirdPerson != clean) record("third person: he/she/it + verb-s", thirdPerson)

        // 5. a / an before a vowel or consonant sound.
        Regex("""\ba\s+([aeiou]\w+)""", RegexOption.IGNORE_CASE)
            .replace(clean) { m ->
                val word = m.groupValues[1]
                if (word.lowercase() in setOf("one", "once", "uniform", "university", "user", "useful", "european", "unit")) {
                    "a $word"
                } else {
                    "an $word"
                }
            }
            .takeIf { it != clean }
            ?.let { record("a / an", it) }

        Regex("""\ban\s+([bcdfgjklmnpqrstvwxz]\w+)""", RegexOption.IGNORE_CASE)
            .replace(clean) { m ->
                val word = m.groupValues[1]
                if (word.lowercase() in setOf("hour", "honest", "honour", "honor", "heir")) {
                    "an $word"
                } else {
                    "a $word"
                }
            }
            .takeIf { it != clean }
            ?.let { record("a / an", it) }

        // 6. Fixed phrases (prepositions, common Sri Lankan English).
        fixedPhrases.forEach { (pattern, replacement, rule) ->
            Regex(pattern, RegexOption.IGNORE_CASE)
                .replace(clean, replacement)
                .takeIf { it != clean }
                ?.let { record(rule, it) }
        }

        // 7. "many information" / "informations".
        Regex("""\bmany\s+(${uncountable.joinToString("|")})\b""", RegexOption.IGNORE_CASE)
            .replace(clean) { m -> "much ${m.groupValues[1].lowercase()}" }
            .takeIf { it != clean }
            ?.let { record("uncountable nouns take \"much\"", it) }

        countablePluralForms.forEach { (wrong, right) ->
            Regex("""\b$wrong\b""", RegexOption.IGNORE_CASE)
                .replace(clean, right)
                .takeIf { it != clean }
                ?.let { record("$wrong is uncountable", it) }
        }

        // 8. for + a period, since + a point in time.
        Regex("""\bsince\s+(\d+|one|two|three|four|five|six)\s+(years?|months?|weeks?|days?|hours?)""", RegexOption.IGNORE_CASE)
            .replace(clean) { m -> "for ${m.groupValues[1]} ${m.groupValues[2]}" }
            .takeIf { it != clean }
            ?.let { record("since + duration → for", it) }

        Regex("""\bfor\s+(yesterday|last night|last week|last month|last year|2018|2019|2020|2021|2022|2023|2024|2025)""", RegexOption.IGNORE_CASE)
            .replace(clean) { m -> "since ${m.groupValues[1]}" }
            .takeIf { it != clean }
            ?.let { record("for + a point in time → since", it) }

        return fixes.values.toList()
    }

    /** The single correction worth showing for this sentence, or null. */
    fun check(sentence: String): GrammarFix? = checkAll(sentence).firstOrNull()

    /**
     * True when the whole utterance is a backchannel ("mm-hmm", "okay", "yeah").
     *
     * Real calls are full of these. Treating one as a question makes the tutor
     * answer something the student never asked, so turn-taking skips them.
     */
    fun isBackchannel(sentence: String): Boolean {
        val clean = sentence.trim().lowercase().trim('.', '!', ',', '?', ' ')
        return clean in setOf(
            "mm", "mmm", "mhm", "mm hmm", "mm-hmm", "uh huh", "uh-huh", "hmm",
            "ok", "okay", "yeah", "yes", "yep", "ha", "aha", "right", "sure", "oh"
        )
    }

    /** True when the student is asking to stop or wait, not to answer. */
    fun isStopRequest(sentence: String): Boolean {
        val clean = sentence.trim().lowercase()
        return clean in setOf("stop", "wait", "hold on", "one second", "give me a second") ||
            clean.startsWith("stop ") || clean.startsWith("wait ")
    }

    /**
     * Replace the verb at the end of each match, keeping everything before it.
     *
     * The pattern may contain optional groups (a subject, an adverb) between the
     * auxiliary and the verb, so the verb is always the last group and the head of
     * the match is copied verbatim — case, spacing and all.
     */
    private fun replaceMap(text: String, pattern: String, map: Map<String, String>): String? {
        var changed = false
        val result = Regex(pattern, RegexOption.IGNORE_CASE).replace(text) { match ->
            val rawWord = match.groupValues.last()
            val replacement = map[rawWord.lowercase()]
            if (replacement == null || rawWord.isBlank()) {
                match.value
            } else {
                changed = true
                match.value.dropLast(rawWord.length) + replacement
            }
        }
        return if (changed) result else null
    }
}
