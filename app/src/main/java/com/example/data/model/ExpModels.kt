package com.example.data.model

/**
 * The student's EXP, as the server keeps it.
 *
 * EXP is a ledger on the server: every point names the thing that earned it
 * (`backend/api/_exp.php`). The app never adds points up itself — it shows what
 * the server returned — because a total the client can invent is a total that can
 * drift from the rows underneath it.
 */
data class UserExp(
    val total: Int = 0,
    val today: Int = 0,
    val dailyCap: Int = 120,
    val tier: ExpTier = ExpTier.forTotal(0),
    val recent: List<ExpAward> = emptyList()
) {
    /** What is left of today's allowance. */
    val todayRemaining: Int get() = (dailyCap - today).coerceAtLeast(0)

    /** 0…1 across today's allowance, for the daily bar. */
    val todayProgress: Float
        get() = if (dailyCap <= 0) 0f else (today.toFloat() / dailyCap).coerceIn(0f, 1f)

    /** How many of the five tier badges are earned. */
    val badgesEarned: Int get() = ExpTier.LADDER.count { total >= it.min }

    companion object {
        /** Parse the `exp` object from `auth.php` (`me`) or `chat.php` (`exp_state`). */
        fun from(obj: org.json.JSONObject?): UserExp {
            if (obj == null) return UserExp()
            val tierObj = obj.optJSONObject("tier")
            val recent = ArrayList<ExpAward>()
            obj.optJSONArray("recent")?.let { array ->
                for (index in 0 until array.length()) {
                    val row = array.optJSONObject(index) ?: continue
                    recent += ExpAward(
                        kind = row.optString("kind"),
                        amount = row.optInt("amount", 0),
                        note = row.optString("note"),
                        createdEpoch = row.optLong("created_epoch", 0L)
                    )
                }
            }
            val total = obj.optInt("total", 0)
            val local = ExpTier.forTotal(total)
            return UserExp(
                total = total,
                today = obj.optInt("today", 0),
                dailyCap = obj.optInt("daily_cap", local.cap),
                tier = if (tierObj == null) {
                    local
                } else {
                    ExpTier(
                        code = tierObj.optString("code").orEmpty().ifBlank { local.code },
                        name = tierObj.optString("name").orEmpty().ifBlank { local.name },
                        level = tierObj.optInt("level", local.level),
                        nextName = tierObj.optString("next_name").orEmpty()
                            .takeIf { it.isNotBlank() },
                        nextLevel = if (tierObj.has("next_level") && !tierObj.isNull("next_level")) {
                            tierObj.optInt("next_level", 0)
                        } else {
                            local.nextLevel
                        },
                        nextAt = if (tierObj.has("next_min") && !tierObj.isNull("next_min")) {
                            tierObj.optInt("next_min", 0)
                        } else {
                            null
                        },
                        toNext = tierObj.optInt("to_next", 0),
                        progress = tierObj.optDouble("progress", 0.0).toFloat(),
                        min = tierObj.optInt("min", local.min),
                        cap = tierObj.optInt("cap", local.cap),
                        nextCap = if (tierObj.has("next_cap") && !tierObj.isNull("next_cap")) {
                            tierObj.optInt("next_cap", 0)
                        } else {
                            null
                        }
                    )
                },
                recent = recent
            )
        }
    }
}

/** One award, for the history list. */
data class ExpAward(
    val kind: String,
    val amount: Int,
    val note: String,
    val createdEpoch: Long
) {
    /**
     * A label for the kind of award. Unknown kinds (a newer server) fall back to
     * the note the server sent rather than showing a raw code.
     */
    val label: String
        get() = when (kind) {
            "help_given" -> "Help given"
            "help_closed" -> "Request closed"
            "rated" -> "Rated a student"
            "rating_received" -> "Rating received"
            "rating_five" -> "Five-star rating"
            "paper_uploaded" -> "Paper uploaded"
            "profile_complete" -> "Profile complete"
            else -> note.ifBlank { "EXP" }
        }
}

/**
 * A rung of the EXP ladder.
 *
 * `min` is the total needed to reach it, `cap` the daily ceiling it unlocks. The
 * thresholds mirror `TS_EXP_TIERS` in `backend/api/_exp.php`; the server is the
 * authority (it sends level, progress and both caps), and the local table exists
 * only so the screen can draw the badge row and name a tier before the first
 * network call lands.
 */
data class ExpTier(
    val code: String = "learner",
    val name: String = "Learner",
    val level: Int = 1,
    val nextName: String? = null,
    val nextLevel: Int? = null,
    val nextAt: Int? = null,
    val toNext: Int = 0,
    val progress: Float = 0f,
    val min: Int = 0,
    val cap: Int = 120,
    val nextCap: Int? = null
) {
    /**
     * "Advanced" style epithet shown under the level number, derived from the
     * tier itself rather than invented per-screen.
     */
    val epithet: String
        get() = when (code) {
            "learner" -> "Just getting started"
            "helper" -> "Students count on you"
            "guide" -> "You explain things well"
            "mentor" -> "A trusted name here"
            else -> "Top of the ladder"
        }

    companion object {
        /** One rung: what it is called, where it starts, what it unlocks. */
        data class Step(val code: String, val name: String, val min: Int, val cap: Int)

        /** Local mirror of `TS_EXP_TIERS` — never used for awarding. */
        val LADDER: List<Step> = listOf(
            Step("learner", "Learner", 0, 120),
            Step("helper", "Helper", 200, 160),
            Step("guide", "Guide", 600, 200),
            Step("mentor", "Mentor", 1500, 260),
            Step("legend", "Legend", 3000, 320)
        )

        fun forTotal(total: Int): ExpTier {
            var index = 0
            LADDER.forEachIndexed { i, step -> if (total >= step.min) index = i }
            val current = LADDER[index]
            val next = LADDER.getOrNull(index + 1)
            val span = if (next != null) (next.min - current.min).coerceAtLeast(1) else 0
            return ExpTier(
                code = current.code,
                name = current.name,
                level = index + 1,
                nextName = next?.name,
                nextLevel = next?.let { index + 2 },
                nextAt = next?.min,
                toNext = next?.let { (it.min - total).coerceAtLeast(0) } ?: 0,
                progress = if (span == 0) 1f else ((total - current.min).toFloat() / span).coerceIn(0f, 1f),
                min = current.min,
                cap = current.cap,
                nextCap = next?.cap
            )
        }
    }
}
