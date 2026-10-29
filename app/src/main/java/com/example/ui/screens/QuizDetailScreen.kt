package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizSummary
import com.example.ui.components.QuizHeroArt
import com.example.ui.components.shimmerEffect
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay

private val DetailInk = Color(0xFF0F172A)
private val DetailMuted = Color(0xFF64748B)
private val DetailFaint = Color(0xFF94A3B8)
private val DetailHairline = Color(0xFFE8EDF3)
private val DetailBg = Color(0xFFF7F9FC)
private val DetailGreen = Color(0xFF059669)
private val DetailGreenSoft = Color(0xFFECFDF5)

/** Same roles and sizes as `PT` on the papers screen and `QT` on quizzes. */
private object DQ {
    val title: TextUnit = 28.sp
    val section: TextUnit = 18.sp
    val cardTitle: TextUnit = 15.sp
    val body: TextUnit = 14.sp
    val meta: TextUnit = 13.sp
    val micro: TextUnit = 12.sp
    val overline: TextUnit = 11.sp

    val titleHeight: TextUnit = 34.sp
    val bodyHeight: TextUnit = 20.sp
}

/**
 * The page a quiz opens into, before any attempt starts.
 *
 * Everything on it is derived from the quiz the server sent — the counts, the
 * time limit, the reward, the attempts left, the pass mark — so a quiz with four
 * questions and no timer reads honestly instead of borrowing another quiz's
 * numbers. The rules are generated from the same values, which also means the
 * page can never promise something the runner will not do.
 */
@Composable
fun QuizDetailScreen(
    quiz: QuizSummary,
    isStarting: Boolean = false,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outOfAttempts = quiz.isEnrolled

    // The phone's back gesture leaves the page, never the app.
    BackHandler { onBack() }

    // The page is built from data the list already holds, so the only thing that
    // can still be missing is the hero photograph. The skeleton covers that first
    // moment so the page never appears half-drawn.
    var isEntering by remember(quiz.id) { mutableStateOf(true) }
    LaunchedEffect(quiz.id) {
        delay(320)
        isEntering = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DetailBg)
    ) {
        // ── top bar: the only fixed part, same rule as the list ─────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DetailBg)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, DetailHairline),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("quiz_detail_back")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to quizzes",
                            tint = DetailInk,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Quizzes  ›  " + (quiz.code.ifBlank { "Details" }),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.meta,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DetailHairline)
            )
        }

        if (isEntering) {
            QuizDetailSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quiz_detail_skeleton")
            )
        } else {
            // ── everything else scrolls ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 18.dp, bottom = 16.dp)
                    .testTag("quiz_detail_content")
            ) {
                Text(
                    text = listOf(quiz.subject, quiz.grade, "${quiz.medium} medium")
                        .filter { it.isNotBlank() }
                        .joinToString("  ·  ")
                        .uppercase(),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.overline,
                    fontWeight = FontWeight.Bold,
                    color = DetailGreen,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = quiz.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.title,
                    lineHeight = DQ.titleHeight,
                    fontWeight = FontWeight.ExtraBold,
                    color = DetailInk,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listOf(quiz.category, quiz.stream)
                        .filterNotNull()
                        .filter { it.isNotBlank() }
                        .joinToString(" · ") + " revision quiz",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.cardTitle,
                    color = DetailMuted
                )

                Spacer(Modifier.height(16.dp))
                QuizHeroArt(
                    quiz = quiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .testTag("quiz_detail_hero")
                )

                Spacer(Modifier.height(14.dp))
                // Two by two, not four across: a phone gives four tiles about 78dp of
                // text each, which is not enough for "No timer" or "2 attempts left"
                // next to an icon — that is what was clipping behind the cards.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailStat(
                            icon = Icons.Default.Quiz,
                            value = "${quiz.questionCount} questions",
                            label = "one answer each",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStat(
                            icon = Icons.Default.Schedule,
                            value = if (quiz.isTimed) "${quiz.minutes} minutes" else "No timer",
                            label = if (quiz.isTimed) "to finish" else "your own pace",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailStat(
                            icon = Icons.Default.EmojiEvents,
                            value = "+${quiz.expReward} EXP",
                            label = if (quiz.expReward > 0) "on a pass" else "no reward",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStat(
                            icon = Icons.Default.RestartAlt,
                            value = "${quiz.attemptsLeft.coerceAtLeast(0)} attempts left",
                            label = "of ${quiz.maxAttempts}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (quiz.description.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = quiz.description,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = DQ.meta,
                        lineHeight = DQ.bodyHeight,
                        color = DetailMuted
                    )
                }

                Spacer(Modifier.height(16.dp))
                DetailCard(title = "Rules & guidance", icon = Icons.Default.CheckCircle) {
                    quizRules(quiz).forEach { rule ->
                        DetailBullet(text = rule, leading = true)
                    }
                }

                Spacer(Modifier.height(12.dp))
                DetailCard(title = "Before you start", icon = Icons.Default.Info) {
                    DetailBullet(
                        text = "Find a quiet place where you can focus for " +
                            if (quiz.isTimed) "${quiz.minutes} minutes." else "a while.",
                        leading = false
                    )
                    DetailBullet(
                        text = "Keep a stable connection — your answers are saved as you go.",
                        leading = false
                    )
                    if (quiz.bestPercent != null) {
                        DetailBullet(
                            text = "Your best so far is ${quiz.bestPercent}% across " +
                                "${quiz.attempts} attempt${if (quiz.attempts == 1) "" else "s"}.",
                            leading = false
                        )
                    }
                }
            }
        }

        // ── the two actions, always reachable ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DetailBg)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            if (outOfAttempts) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "No attempts left — this quiz is in your collection",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = DQ.body,
                            fontWeight = FontWeight.Bold,
                            color = DetailMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                Surface(
                    onClick = onStart,
                    enabled = !isStarting,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isStarting) DetailGreen.copy(alpha = 0.6f) else DetailGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("quiz_detail_start")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when {
                                isStarting -> "Preparing your questions…"
                                quiz.attemptsLeft < quiz.maxAttempts -> "Start attempt ${quiz.attempts + 1}"
                                else -> "Start quiz"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = DQ.cardTitle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, DetailHairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("quiz_detail_secondary_back")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Back",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = DQ.cardTitle,
                        fontWeight = FontWeight.Bold,
                        color = DetailInk
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your attempt is saved automatically",
                fontFamily = MaisonNeueFontFamily,
                fontSize = DQ.overline,
                color = DetailFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The rules, written from the quiz's own settings.
 *
 * A quiz without a timer must not claim one, a one-attempt quiz must not say
 * "each attempt", and a quiz that awards nothing must not promise EXP — so each
 * line is built from a real field rather than pasted.
 */
private fun quizRules(quiz: QuizSummary): List<String> {
    val rules = mutableListOf<String>()
    if (quiz.isTimed) {
        rules += "You get ${quiz.minutes} minutes; the clock keeps running if you leave the quiz."
    } else {
        rules += "No time limit — answer at your own pace."
    }
    rules += "${quiz.questionCount} multiple-choice questions, one answer each. " +
        "You can move back before submitting."
    rules += "You need ${quiz.passPercent}% or more to pass."
    if (quiz.expReward > 0) {
        rules += "Passing banks +${quiz.expReward} EXP, once per quiz."
    }
    rules += if (quiz.attemptsLeft < quiz.maxAttempts) {
        "You have ${quiz.attemptsLeft} of ${quiz.maxAttempts} attempts left."
    } else {
        "You get ${quiz.maxAttempts} attempt${if (quiz.maxAttempts == 1) "" else "s"} on this quiz."
    }
    rules += "Correct answers and a short explanation appear after you submit."
    return rules
}

/**
 * One stat tile.
 *
 * Four tiles across a phone leaves roughly 78dp of text per tile, which is not
 * enough for "15 min" at body size next to an icon — that is what was clipping.
 * So the tile holds a **single line**: the value, with the label underneath only
 * when it fits, and the whole tile scales its text down instead of truncating.
 * Nothing is ever hidden behind the card edge.
 */
/**
 * One stat tile: a soft icon disc on the left, the value and its label beside it.
 *
 * Laying the text out horizontally (rather than stacked and centred under an
 * icon) is what makes it survive a narrow card: the value gets all the remaining
 * width and still fits "2 attempts left" at body size. Both lines are single-line
 * with ellipsis as a last resort, never `Visible` overflow.
 */
@Composable
private fun DetailStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DetailHairline),
        modifier = modifier.height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DetailGreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = DetailGreen,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.body,
                    fontWeight = FontWeight.Bold,
                    color = DetailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.overline,
                    color = DetailFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DetailGreen, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = DQ.cardTitle,
                    fontWeight = FontWeight.ExtraBold,
                    color = DetailInk
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailBullet(text: String, leading: Boolean) {
    Row(
        modifier = Modifier.padding(bottom = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (leading) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(DetailGreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = DetailGreen,
                    modifier = Modifier.size(11.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(DetailFaint)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = DQ.meta,
            lineHeight = DQ.bodyHeight,
            color = DetailMuted
        )
    }
}

/**
 * What the detail page shows for the first moment: the same shapes it is about
 * to fill in, shimmering. Laid out with the identical spacing and rhythm as the
 * real content, so nothing jumps when it swaps over.
 */
@Composable
private fun QuizDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 16.dp)
    ) {
        SkeletonLine(widthFraction = 0.42f, height = 11.dp)
        Spacer(Modifier.height(12.dp))
        SkeletonLine(widthFraction = 0.86f, height = 26.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonLine(widthFraction = 0.52f, height = 18.dp)
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shimmerEffect(shapeRadius = 20.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shimmerEffect(shapeRadius = 14.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shimmerEffect(shapeRadius = 14.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shimmerEffect(shapeRadius = 14.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shimmerEffect(shapeRadius = 14.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        SkeletonLine(widthFraction = 1f, height = 12.dp)
        Spacer(Modifier.height(7.dp))
        SkeletonLine(widthFraction = 0.92f, height = 12.dp)
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .shimmerEffect(shapeRadius = 18.dp)
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .shimmerEffect(shapeRadius = 18.dp)
        )
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .shimmerEffect(shapeRadius = 6.dp)
    )
}
