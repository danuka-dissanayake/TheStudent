package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpTier
import com.example.data.model.UserExp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

private val ExpInk = Color(0xFF0F172A)
private val ExpMuted = Color(0xFF64748B)
private val ExpFaint = Color(0xFF94A3B8)

/**
 * The EXP medals card.
 *
 * One design, used by both the profile and the dashboard: the current medallion,
 * the level name, what is left to the next rung, the gradient progress bar, and
 * the five-rung ladder with the earned medals in colour and the rest greyed.
 * Keeping it here means the two screens can never drift apart.
 */
@Composable
fun ExpMedalsCard(
    exp: UserExp,
    onOpenExp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = exp.tier.progress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "exp_medals_progress"
    )

    // One clock for the whole card. Each earned medal reads it inside its draw
    // scope with its own offset, so the sheen ripples along the ladder without a
    // single recomposition.
    val shine = rememberInfiniteTransition(label = "exp_medals_shine")
    val shinePhase by shine.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "exp_medals_shine_phase"
    )
    val shinePhaseProvider: () -> Float = { shinePhase }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenExp)
            .testTag("exp_medals_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TierMedallion(
                    code = exp.tier.code,
                    size = 40.dp,
                    glow = true,
                    earned = true,
                    shinePhase = shinePhaseProvider
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Level ${exp.tier.level} · ${exp.tier.name}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpInk
                    )
                    Text(
                        text = if (exp.tier.nextName != null) {
                            "${formatExp(exp.tier.toNext)} EXP to ${exp.tier.nextName}"
                        } else {
                            "Top level reached"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        color = ExpMuted
                    )
                }
                Text(
                    text = "${formatExp(exp.total)} XP",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open EXP",
                    tint = ExpFaint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEAF0F5))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF34D399), EmeraldPrimary)
                            )
                        )
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExpTier.LADDER.forEachIndexed { index, step ->
                    val earned = exp.total >= step.min
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TierMedallion(
                            code = step.code,
                            size = 32.dp,
                            glow = false,
                            earned = earned,
                            // Offset per rung so the light travels along the row.
                            shinePhase = if (earned) {
                                { (shinePhase + index * 0.16f) % 1f }
                            } else {
                                null
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = step.name,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = if (earned) FontWeight.Bold else FontWeight.Medium,
                            color = if (earned) ExpInk else ExpFaint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
