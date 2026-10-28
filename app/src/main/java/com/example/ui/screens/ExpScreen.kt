package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpAward
import com.example.data.model.ExpTier
import com.example.data.model.UserExp
import com.example.ui.components.ExpBadgeLadder
import com.example.ui.components.ExpHeroCard
import com.example.ui.components.TierMedallion
import com.example.ui.components.formatExp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ExpInk = Color(0xFF0F172A)
private val ExpMuted = Color(0xFF64748B)
private val ExpFaint = Color(0xFF94A3B8)
private val ExpHairline = Color(0xFFE8EDF3)
private val ExpBg = Color(0xFFF6F8FB)
private val ExpMint = Color(0xFFECFDF5)

/**
 * "Your EXP" — the reward page.
 *
 * Four movements, top to bottom: who you are right now (a medallion and the
 * total), how close the next level is (one bar, one number), what that level
 * unlocks (badges and the higher daily ceiling), and what earned it (the ledger
 * rows). Everything shown comes from the server's ledger; the screen adds
 * nothing up itself.
 */
@Composable
fun ExpScreen(
    exp: UserExp,
    isLoading: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelProgress by animateFloatAsState(
        targetValue = exp.tier.progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "exp_level_progress"
    )
    val dayProgress by animateFloatAsState(
        targetValue = exp.todayProgress,
        animationSpec = tween(900),
        label = "exp_day_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ExpBg)
            .testTag("exp_screen")
    ) {
        ExpHeader(
            tier = exp.tier,
            isLoading = isLoading,
            onClose = onClose,
            onRefresh = onRefresh
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "hero") {
                ExpHeroCard(total = exp.total, tier = exp.tier)
            }

            item(key = "level") {
                LevelProgressCard(
                    tier = exp.tier,
                    total = exp.total,
                    progress = levelProgress
                )
            }

            item(key = "today") {
                TodayCard(
                    today = exp.today,
                    cap = exp.dailyCap,
                    remaining = exp.todayRemaining,
                    progress = dayProgress
                )
            }

            if (exp.tier.nextName != null) {
                item(key = "rewards") {
                    NextLevelRewards(tier = exp.tier)
                }
            }

            item(key = "badges_title") {
                ExpSectionTitle(
                    title = "Badges",
                    trailing = "${exp.badgesEarned} of ${ExpTier.LADDER.size} earned"
                )
            }

            item(key = "badges") {
                ExpBadgeLadder(total = exp.total)
            }

            item(key = "activity_title") {
                ExpSectionTitle(
                    title = "Recent activity",
                    trailing = if (exp.recent.isEmpty()) null else "${exp.recent.size} awards"
                )
            }

            if (exp.recent.isEmpty()) {
                item(key = "activity_empty") { EmptyActivityCard() }
            } else {
                itemsIndexed(exp.recent, key = { index, award -> "$index-${award.kind}-${award.createdEpoch}" }) { _, award ->
                    ActivityRow(award)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpHeader(
    tier: ExpTier,
    isLoading: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = ExpInk,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClose)
                .padding(6.dp)
                .size(22.dp)
                .testTag("exp_back")
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "EXP",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ExpInk,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "Your learning journey",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                color = ExpMuted
            )
        }

        Surface(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            color = ExpMint,
            modifier = Modifier.testTag("exp_refresh")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = EmeraldPrimary
                    )
                } else {
                    TierMedallion(code = tier.code, size = 15.dp, glow = false)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isLoading) "Updating…" else "EXP Log",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
//  Level + today
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LevelProgressCard(tier: ExpTier, total: Int, progress: Float) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Level ${tier.level}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpInk
                    )
                    Text(
                        text = tier.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
                Text(
                    text = if (tier.nextName != null) {
                        "${formatExp(tier.toNext)} EXP to Level ${tier.nextLevel ?: (tier.level + 1)}"
                    } else {
                        "Top level"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = ExpMuted
                )
            }

            Spacer(Modifier.height(12.dp))
            ProgressTrack(progress = progress, height = 10.dp, gradient = true)
            Spacer(Modifier.height(8.dp))
            val nextAt = tier.nextAt
            Text(
                text = if (nextAt != null) {
                    "${formatExp(total)} / ${formatExp(nextAt)} EXP"
                } else {
                    "${formatExp(total)} EXP"
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = ExpMuted
            )
        }
    }
}

@Composable
private fun TodayCard(today: Int, cap: Int, remaining: Int, progress: Float) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpInk
                )
                Spacer(Modifier.height(6.dp))
                ProgressTrack(progress = progress, height = 6.dp, gradient = false)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (remaining == 0) {
                        "Today's ceiling reached — it resets after midnight."
                    } else {
                        "$remaining EXP still available today."
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = ExpFaint
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "$today / $cap",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark
            )
        }
    }
}

/** The rounded emerald bar used in three places, so it always looks the same. */
@Composable
private fun ProgressTrack(progress: Float, height: Dp, gradient: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFEAF0F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(
                    if (gradient) {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF34D399), EmeraldPrimary)
                        )
                    } else {
                        Brush.horizontalGradient(listOf(EmeraldPrimary, EmeraldPrimary))
                    }
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rewards + badges
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NextLevelRewards(tier: ExpTier) {
    val nextName = tier.nextName ?: return
    val capGain = ((tier.nextCap ?: tier.cap) - tier.cap).coerceAtLeast(0)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ExpMint,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Next level rewards",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ExpInk
            )
            Text(
                text = "Reach $nextName to unlock these",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = ExpMuted
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Top) {
                RewardTile(
                    modifier = Modifier.weight(1f),
                    title = if (capGain > 0) "+$capGain" else "More",
                    subtitle = "Daily EXP limit"
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6EE7B7), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                RewardDivider()
                RewardTile(
                    modifier = Modifier.weight(1f),
                    title = nextName,
                    subtitle = "Tier badge"
                ) {
                    TierMedallion(code = tier.nextName.orEmpty().lowercase(), size = 46.dp, glow = false)
                }
                RewardDivider()
                RewardTile(
                    modifier = Modifier.weight(1f),
                    title = "Legend",
                    subtitle = "${formatExp(3000)} EXP"
                ) {
                    TierMedallion(code = "legend", size = 46.dp, glow = false)
                }
            }
        }
    }
}

@Composable
private fun RewardTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badge: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        badge()
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ExpInk,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            color = ExpMuted,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RewardDivider() {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .width(1.dp)
            .height(58.dp)
            .background(Color(0xFFD3EFE2))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Activity
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpSectionTitle(title: String, trailing: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ExpInk,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = EmeraldDark
            )
        }
    }
}

@Composable
private fun ActivityRow(award: ExpAward) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            AwardTile(award)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = award.label,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpInk,
                    maxLines = 1
                )
                Text(
                    text = award.note.ifBlank { award.label },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = ExpMuted,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${award.amount} EXP",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
                Text(
                    text = awardDate(award.createdEpoch),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
                    color = ExpFaint
                )
            }
        }
    }
}

/** A tinted circle with the kind of work inside it — the row's colour code. */
@Composable
private fun AwardTile(award: ExpAward) {
    val (tint, icon) = when (award.kind) {
        "help_given" -> Color(0xFF10B981) to Icons.Default.Forum
        "help_closed" -> Color(0xFF3B82F6) to Icons.Default.DoneAll
        "rating_five" -> Color(0xFFF59E0B) to Icons.Default.Star
        "rating_received" -> Color(0xFF8B5CF6) to Icons.Default.Star
        "rated" -> Color(0xFF8B5CF6) to Icons.Default.Star
        "paper_uploaded" -> Color(0xFF0EA5E9) to Icons.Default.Description
        else -> Color(0xFF64748B) to Icons.Default.Star
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.95f), tint.copy(alpha = 0.72f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyActivityCard() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            TierMedallion(code = "helper", size = 56.dp, glow = true, earned = false)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "No awards yet",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = ExpInk
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Answer someone's question and close the request — the first +25 lands here.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = ExpMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** "Today", "Yesterday", or a short date — enough to place an award in time. */
private fun awardDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val now = System.currentTimeMillis() / 1000L
    val diff = now - epochSeconds
    return when {
        diff < 60L -> "Just now"
        diff < 3600L -> "${diff / 60L}m ago"
        diff < 86_400L -> "${diff / 3600L}h ago"
        diff < 172_800L -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochSeconds * 1000L))
    }
}
