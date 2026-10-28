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
            return UserExp(
                total = total,
                today = obj.optInt("today", 0),
                dailyCap = obj.optInt("daily_cap", 120),
                tier = ExpTier(
                    code = tierObj?.optString("code").orEmpty().ifBlank { "learner" },
                    name = tierObj?.optString("name").orEmpty().ifBlank { "Learner" },
                    nextName = tierObj?.optString("next_name").orEmpty().ifBlank { null },
                    nextAt = if (tierObj != null && tierObj.has("next_min") &&
                        !tierObj.isNull("next_min")
                    ) {
                        tierObj.optInt("next_min", 0)
                    } else {
                        null
                    },
                    toNext = tierObj?.optInt("to_next", 0) ?: 0,
                    progress = (tierObj?.optDouble("progress", 0.0) ?: 0.0).toFloat(),
                    min = tierObj?.optInt("min", 0) ?: 0
                ),
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
 * A tier of the EXP ladder.
 *
 * The thresholds mirror `TS_EXP_TIERS` in `backend/api/_exp.php`; the server is
 * the authority (`progress` arrives computed), and [forTotal] exists only so the
 * home screen can name the tier before the first network call lands.
 */
data class ExpTier(
    val code: String = "learner",
    val name: String = "Learner",
    val nextName: String? = null,
    val nextAt: Int? = null,
    val toNext: Int = 0,
    val progress: Float = 0f,
    val min: Int = 0
) {
    companion object {
        /** Local mirror of the server ladder — never used for awarding. */
        private val LADDER = listOf(
            "learner" to ("Learner" to 0),
            "helper" to ("Helper" to 200),
            "guide" to ("Guide" to 600),
            "mentor" to ("Mentor" to 1500),
            "legend" to ("Legend" to 3000)
        )

        fun forTotal(total: Int): ExpTier {
            var currentCode = LADDER.first().first
            var currentName = LADDER.first().second.first
            var currentMin = 0
            var nextName: String? = null
            var nextAt: Int? = null
            LADDER.forEachIndexed { index, entry ->
                if (total >= entry.second.second) {
                    currentCode = entry.first
                    currentName = entry.second.first
                    currentMin = entry.second.second
                    nextName = LADDER.getOrNull(index + 1)?.second?.first
                    nextAt = LADDER.getOrNull(index + 1)?.second?.second
                }
            }
            val span = if (nextAt != null) (nextAt - currentMin).coerceAtLeast(1) else 0
            return ExpTier(
                code = currentCode,
                name = currentName,
                nextName = nextName,
                nextAt = nextAt,
                toNext = nextAt?.let { (it - total).coerceAtLeast(0) } ?: 0,
                progress = if (span == 0) 1f else ((total - currentMin).toFloat() / span).coerceIn(0f, 1f),
                min = currentMin
            )
        }
    }
}
