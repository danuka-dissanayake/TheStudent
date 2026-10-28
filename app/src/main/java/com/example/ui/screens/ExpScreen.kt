package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpAward
import com.example.data.model.UserExp
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

/**
 * "Your EXP" — the ledger, made legible.
 *
 * The number is never just a badge: this page says what the total is made of
 * (the newest awards, each with the thing that earned it), how much of today's
 * allowance is used, and how far the next tier is. Everything shown here came
 * from the server's ledger; nothing is added up on the phone.
 */
@Composable
fun ExpScreen(
    exp: UserExp,
    isLoading: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierProgress by animateFloatAsState(
        targetValue = exp.tier.progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "exp_tier_progress"
    )
    val dayProgress by animateFloatAsState(
        targetValue = exp.todayProgress,
        animationSpec = tween(700),
        label = "exp_day_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("exp_screen")
    ) {
        // ── header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, ExpHairline),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ExpInk,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your EXP",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ExpInk
                )
                Text(
                    text = exp.tier.name,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EmeraldDark
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = EmeraldPrimary
                )
            } else {
                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.testTag("exp_refresh")
                ) {
                    Text(
                        text = "Refresh",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── the total ────────────────────────────────────────────────────
            item(key = "exp_total") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "${exp.total}",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "EXP earned · ${exp.tier.name}",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.28f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(tierProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = if (exp.tier.nextName != null) {
                                "${exp.tier.toNext} EXP to ${exp.tier.nextName}"
                            } else {
                                "Top tier — nothing left to climb"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // ── today's allowance ────────────────────────────────────────────
            item(key = "exp_today") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ExpHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Today",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpInk,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${exp.today} / ${exp.dailyCap} EXP",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpMuted
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE9F8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(dayProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(EmeraldPrimary)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (exp.todayRemaining == 0) {
                                "That is today's ceiling. It resets after midnight."
                            } else {
                                "${exp.todayRemaining} EXP still available today."
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            color = ExpFaint
                        )
                    }
                }
            }

            // ── the ledger ───────────────────────────────────────────────────
            item(key = "exp_ledger_title") {
                Text(
                    text = "Recent awards",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpInk,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (exp.recent.isEmpty()) {
                item(key = "exp_empty") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, ExpHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nothing here yet",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpInk
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Help someone with a question and it shows up here.",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                color = ExpMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(exp.recent, key = { "${it.kind}-${it.createdEpoch}-${it.amount}" }) { award ->
                    ExpAwardRow(award)
                }
            }
        }
    }
}

@Composable
private fun ExpAwardRow(award: ExpAward) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ExpHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${award.amount}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = award.label,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ExpInk
                )
                Text(
                    text = award.note.ifBlank { award.label },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = ExpMuted,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = awardDate(award.createdEpoch),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = ExpFaint
            )
        }
    }
}

/** "Today", "Yesterday", or a short date — enough to place an award in time. */
private fun awardDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val now = System.currentTimeMillis() / 1000L
    val diff = now - epochSeconds
    return when {
        diff < 60L -> "Just now"
        diff < 3600L -> "${diff / 60L} min ago"
        diff < 86_400L -> "${diff / 3600L} h ago"
        diff < 172_800L -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochSeconds * 1000L))
    }
}
