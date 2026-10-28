package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpAward
import com.example.data.model.ExpTier
import com.example.data.model.UserExp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ExpInk = Color(0xFF0F172A)
private val ExpMuted = Color(0xFF64748B)
private val ExpFaint = Color(0xFF94A3B8)
private val ExpHairline = Color(0xFFE8EDF3)
private val ExpBg = Color(0xFFF6F8FB)
private val ExpMint = Color(0xFFECFDF5)
private val ExpDeep = Color(0xFF053B2B)

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
                BadgeLadder(total = exp.total)
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

@Composable
private fun ExpHeroCard(total: Int, tier: ExpTier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ExpDeep,
                            Color(0xFF075E45),
                            Color(0xFF059669)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TierMedallion(code = tier.code, size = 96.dp, glow = true)

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total EXP",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        text = formatExp(total),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tier.epithet,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

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
                        "${formatExp(tier.toNext)} EXP to Level ${tier.nextLevel}"
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

@Composable
private fun BadgeLadder(total: Int) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            ExpTier.LADDER.forEach { step ->
                val earned = total >= step.min
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TierMedallion(
                        code = step.code,
                        size = 44.dp,
                        glow = false,
                        earned = earned
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = step.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.sp,
                        fontWeight = if (earned) FontWeight.Bold else FontWeight.Medium,
                        color = if (earned) ExpInk else ExpFaint,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (earned) "Earned" else formatExp(step.min),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.sp,
                        color = if (earned) EmeraldPrimary else ExpFaint,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
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
//  The medallion
// ─────────────────────────────────────────────────────────────────────────────

/** One colour per rung, so a badge is recognisable at a glance. */
private fun badgeColor(code: String): Color = when (code.lowercase()) {
    "helper" -> Color(0xFF10B981)
    "guide" -> Color(0xFF0EA5E9)
    "mentor" -> Color(0xFF8B5CF6)
    "legend" -> Color(0xFFF59E0B)
    else -> Color(0xFF059669)
}

/**
 * A hexagon badge with a star, drawn rather than an icon so it can glow, carry a
 * tier colour and grey out when it is still locked.
 */
@Composable
private fun TierMedallion(
    code: String,
    size: Dp,
    glow: Boolean,
    earned: Boolean = true
) {
    val base = if (earned) badgeColor(code) else Color(0xFFCBD5E1)
    val light = if (earned) lerp(base, Color.White, 0.42f) else Color(0xFFE7EDF3)

    Canvas(modifier = Modifier.size(size)) {
        val maxRadius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val hexRadius = maxRadius * 0.62f

        if (glow) {
            // Three soft rings, widest first: a halo, not a drop shadow.
            drawCircle(base.copy(alpha = 0.10f), maxRadius, center)
            drawCircle(base.copy(alpha = 0.14f), maxRadius * 0.84f, center)
            drawCircle(base.copy(alpha = 0.18f), maxRadius * 0.70f, center)
        }

        // Ribbon tails behind the hexagon.
        val tail = maxRadius * 0.42f
        val tailTop = hexRadius * 0.52f
        val tailBottom = maxRadius * 0.94f
        listOf(-1f, 1f).forEach { side ->
            val path = Path().apply {
                moveTo(center.x + side * tailTop, center.y + hexRadius * 0.40f)
                lineTo(center.x + side * tail, center.y + tailTop)
                lineTo(center.x + side * tail * 0.62f, center.y + tailBottom)
                lineTo(center.x + side * tail * 0.10f, center.y + hexRadius * 0.66f)
                close()
            }
            drawPath(path, color = lerp(base, Color.Black, 0.16f))
        }

        // Hexagon face.
        val hex = Path().apply {
            for (index in 0 until 6) {
                val angle = (-PI / 2 + index * PI / 3).toFloat()
                val x = center.x + hexRadius * cos(angle)
                val y = center.y + hexRadius * sin(angle)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = hex,
            brush = Brush.linearGradient(
                colors = listOf(light, base),
                start = Offset(center.x - hexRadius, center.y - hexRadius),
                end = Offset(center.x + hexRadius, center.y + hexRadius)
            )
        )
        drawPath(
            path = hex,
            color = Color.White.copy(alpha = if (glow) 0.55f else 0.35f),
            style = Stroke(width = maxRadius * 0.05f)
        )

        // Star.
        drawStar(
            center = center,
            outer = hexRadius * 0.46f,
            color = if (earned) Color.White.copy(alpha = 0.96f) else Color(0xFFF8FAFC)
        )
    }
}

/** A five-point star, filled. */
private fun DrawScope.drawStar(center: Offset, outer: Float, color: Color) {
    val inner = outer * 0.46f
    val path = Path()
    for (index in 0 until 10) {
        val angle = (-PI / 2 + index * PI / 5).toFloat()
        val radius = if (index % 2 == 0) outer else inner
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** 1240 → "1,240": big numbers read better with the separator. */
private fun formatExp(value: Int): String {
    if (value < 1000) return value.toString()
    return String.format(Locale.US, "%,d", value)
}

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
