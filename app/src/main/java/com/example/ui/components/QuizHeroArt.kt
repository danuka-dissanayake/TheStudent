package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizSummary
import com.example.data.model.SubjectCodes
import com.example.ui.theme.MaisonNeueFontFamily
import kotlin.math.PI
import kotlin.math.sin

/**
 * The picture at the top of a quiz's detail page.
 *
 * There is no image column in the quiz table and no licensed photo library
 * behind this app, so the hero is **drawn** from the quiz's own metadata. That
 * has three consequences worth stating plainly:
 *
 * 1. It never 404s, never needs a download and never costs a licence.
 * 2. It cannot be wrong or generic — the scene is chosen from the subject and
 *    title, so a physics quiz shows a swinging cradle and a chemistry quiz shows
 *    a flask.
 * 3. It is deterministic: the same quiz always draws the same picture, so the
 *    card in the list, the detail page and the result page agree.
 *
 * If the database ever gains a `hero_image_url`, `QuizHeroArt` should prefer that
 * image and fall back to this drawing — the call site would not change.
 */
enum class QuizHeroScene {
    MECHANICS,
    WAVES,
    CIRCUIT,
    CHEMISTRY,
    BIOLOGY,
    MATHS,
    LANGUAGE,
    COMMERCE,
    GENERAL
}

/** The colours and scene a quiz earns, derived once from its text. */
data class QuizHeroTheme(
    val scene: QuizHeroScene,
    val top: Color,
    val bottom: Color,
    val ink: Color
)

/**
 * Keyword → scene. The first group that matches wins, so the order here is the
 * order of specificity: a quiz called "Biology: genetics" is biology, not
 * whatever "genetics" might otherwise fall through to.
 */
private val SCENE_KEYWORDS: List<Pair<QuizHeroScene, List<String>>> = listOf(
    QuizHeroScene.WAVES to listOf(
        "wave", "sound", "light", "optic", "oscillat", "vibration", "resonan", "electromagnetic"
    ),
    QuizHeroScene.MECHANICS to listOf(
        "mechanic", "motion", "kinematic", "force", "newton", "momentum", "energy", "gravit",
        "friction", "projectile", "physics"
    ),
    QuizHeroScene.CIRCUIT to listOf(
        "circuit", "current", "electric", "electronic", "voltage", "ict", "computer",
        "information", "network", "logic gate"
    ),
    QuizHeroScene.CHEMISTRY to listOf(
        "chem", "reaction", "acid", "base", "mole", "organic", "inorganic", "bond", "solution"
    ),
    QuizHeroScene.BIOLOGY to listOf(
        "bio", "cell", "genetic", "plant", "animal", "ecolog", "photosynth", "human body",
        "agri", "agricultur", "food"
    ),
    QuizHeroScene.MATHS to listOf(
        "math", "algebra", "geometry", "trigon", "calculus", "statistic", "probability",
        "vector", "matri"
    ),
    QuizHeroScene.COMMERCE to listOf(
        "account", "commerce", "business", "economic", "finance", "marketing", "trade",
        "management"
    ),
    QuizHeroScene.LANGUAGE to listOf(
        "english", "sinhala", "tamil", "language", "literature", "grammar", "essay", "poem"
    )
)

/** Subject families get a palette, so physics is teal and commerce is amber. */
private val SUBJECT_PALETTES: List<Pair<List<String>, Pair<Color, Color>>> = listOf(
    listOf("physics", "mechanic", "wave") to (Color(0xFF059669) to Color(0xFF34D399)),
    listOf("chem") to (Color(0xFF0E7490) to Color(0xFF22D3EE)),
    listOf("bio", "agri", "science") to (Color(0xFF15803D) to Color(0xFF86EFAC)),
    listOf("math") to (Color(0xFF1D4ED8) to Color(0xFF60A5FA)),
    listOf("ict", "computer", "information") to (Color(0xFF6D28D9) to Color(0xFFA78BFA)),
    listOf("account", "commerce", "business", "econom") to (Color(0xFFB45309) to Color(0xFFFCD34D)),
    listOf("english", "sinhala", "tamil", "language") to (Color(0xFFBE185D) to Color(0xFFF9A8D4))
)

/** Everything the drawing needs, computed from the quiz's own words. */
fun quizHeroTheme(quiz: QuizSummary): QuizHeroTheme {
    val text = "${quiz.subject} ${quiz.title} ${quiz.description}".lowercase()
    val scene = SCENE_KEYWORDS.firstOrNull { (_, words) -> words.any { text.contains(it) } }
        ?.first
        ?: QuizHeroScene.GENERAL
    val palette = SUBJECT_PALETTES.firstOrNull { (words, _) -> words.any { text.contains(it) } }
        ?.second
        ?: (Color(0xFF059669) to Color(0xFF34D399))
    return QuizHeroTheme(scene = scene, top = palette.first, bottom = palette.second, ink = Color.White)
}

/**
 * The hero card: a gradient in the subject's colour, one drawn scene, and the
 * subject's short code in a glass medallion where a video would put play.
 */
@Composable
fun QuizHeroArt(
    quiz: QuizSummary,
    modifier: Modifier = Modifier
) {
    val theme = quizHeroTheme(quiz)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(theme.top, theme.bottom),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            // A faint grid keeps the flat colour from looking like a mistake.
            val step = size.width / 9f
            var x = step
            while (x < size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }

            when (theme.scene) {
                QuizHeroScene.MECHANICS -> drawMechanics()
                QuizHeroScene.WAVES -> drawWaves()
                QuizHeroScene.CIRCUIT -> drawCircuit()
                QuizHeroScene.CHEMISTRY -> drawFlask()
                QuizHeroScene.BIOLOGY -> drawLeaf()
                QuizHeroScene.MATHS -> drawGraph()
                QuizHeroScene.COMMERCE -> drawBars()
                QuizHeroScene.LANGUAGE -> drawTextLines()
                QuizHeroScene.GENERAL -> drawArcs()
            }
        }

        // Where the mockup had a play button. There is no video behind a quiz, so
        // the medallion carries the subject code instead of pretending.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = SubjectCodes.of(quiz.subject),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = theme.ink
            )
        }
    }
}

// ── the scenes ──────────────────────────────────────────────────────────────

/** Newton's cradle: a frame, four hanging spheres, one mid-swing. */
private fun DrawScope.drawMechanics() {
    val ink = Color.White
    val frameTop = size.height * 0.22f
    val frameBottom = size.height * 0.74f
    val left = size.width * 0.16f
    val right = size.width * 0.58f
    val stroke = size.minDimension * 0.012f

    drawLine(ink, Offset(left, frameTop), Offset(right, frameTop), strokeWidth = stroke)
    drawLine(ink, Offset(left, frameTop), Offset(left, frameBottom), strokeWidth = stroke)
    drawLine(ink, Offset(right, frameTop), Offset(right, frameBottom), strokeWidth = stroke)
    drawLine(ink, Offset(left, frameBottom), Offset(right, frameBottom), strokeWidth = stroke)

    val balls = 4
    val radius = (right - left) / (balls * 2.6f)
    for (index in 0 until balls) {
        val restX = left + radius + index * radius * 2.1f
        val swing = if (index == balls - 1) 0.42f else 0f
        val pivot = Offset(restX, frameTop)
        val ballX = restX + sin(swing) * (frameBottom - frameTop) * 0.55f
        val ballY = frameTop + (frameBottom - frameTop) * 0.74f
        drawLine(
            color = ink.copy(alpha = 0.85f),
            start = pivot,
            end = Offset(ballX, ballY - radius),
            strokeWidth = stroke * 0.6f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0.55f)),
                center = Offset(ballX - radius * 0.3f, ballY - radius * 0.35f),
                radius = radius * 1.6f
            ),
            radius = radius,
            center = Offset(ballX, ballY)
        )
    }
}

/** Three interfering sine waves, one dashed. */
private fun DrawScope.drawWaves() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val baseY = size.height * 0.62f
    repeat(3) { wave ->
        val amplitude = size.height * (0.14f - wave * 0.025f)
        val phase = wave * PI.toFloat() / 3f
        val path = Path()
        var x = 0f
        while (x <= size.width) {
            val y = baseY + sin((x / size.width) * 4f * PI.toFloat() * 1.3f + phase) * amplitude
            if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            x += size.width / 90f
        }
        drawPath(
            path = path,
            color = ink.copy(alpha = 0.9f - wave * 0.22f),
            style = Stroke(width = stroke)
        )
    }
    drawLine(
        color = ink.copy(alpha = 0.35f),
        start = Offset(0f, baseY),
        end = Offset(size.width, baseY),
        strokeWidth = stroke * 0.5f
    )
}

/** A loop of wires with nodes and a battery. */
private fun DrawScope.drawCircuit() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val path = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.32f)
        lineTo(size.width * 0.5f, size.height * 0.32f)
        lineTo(size.width * 0.5f, size.height * 0.68f)
        lineTo(size.width * 0.82f, size.height * 0.68f)
        lineTo(size.width * 0.82f, size.height * 0.32f)
        lineTo(size.width * 0.66f, size.height * 0.32f)
    }
    drawPath(path, ink.copy(alpha = 0.9f), style = Stroke(width = stroke))
    listOf(
        Offset(size.width * 0.18f, size.height * 0.32f),
        Offset(size.width * 0.5f, size.height * 0.5f),
        Offset(size.width * 0.82f, size.height * 0.68f)
    ).forEach { centre ->
        drawCircle(ink, radius = stroke * 3.2f, center = centre)
        drawCircle(ink.copy(alpha = 0.25f), radius = stroke * 7f, center = centre)
    }
    // battery: two short parallel lines
    drawLine(
        ink,
        Offset(size.width * 0.66f, size.height * 0.24f),
        Offset(size.width * 0.66f, size.height * 0.40f),
        strokeWidth = stroke
    )
    drawLine(
        ink.copy(alpha = 0.7f),
        Offset(size.width * 0.70f, size.height * 0.28f),
        Offset(size.width * 0.70f, size.height * 0.36f),
        strokeWidth = stroke
    )
}

/** A conical flask with two bubbles rising. */
private fun DrawScope.drawFlask() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val cx = size.width * 0.5f
    val neckTop = size.height * 0.2f
    val neckHalf = size.width * 0.055f
    val bodyTop = size.height * 0.46f
    val bodyHalf = size.width * 0.2f
    val bottom = size.height * 0.78f

    val path = Path().apply {
        moveTo(cx - neckHalf, neckTop)
        lineTo(cx - neckHalf, bodyTop)
        lineTo(cx - bodyHalf, bottom - size.height * 0.05f)
        quadraticTo(cx, bottom + size.height * 0.04f, cx + bodyHalf, bottom - size.height * 0.05f)
        lineTo(cx + neckHalf, bodyTop)
        lineTo(cx + neckHalf, neckTop)
    }
    drawPath(path, ink.copy(alpha = 0.92f), style = Stroke(width = stroke))
    drawLine(
        ink,
        Offset(cx - neckHalf * 1.6f, neckTop),
        Offset(cx + neckHalf * 1.6f, neckTop),
        strokeWidth = stroke
    )
    // liquid line
    drawLine(
        ink.copy(alpha = 0.6f),
        Offset(cx - bodyHalf * 0.72f, size.height * 0.62f),
        Offset(cx + bodyHalf * 0.72f, size.height * 0.62f),
        strokeWidth = stroke * 0.8f
    )
    drawCircle(ink.copy(alpha = 0.55f), radius = stroke * 2.4f, center = Offset(cx - bodyHalf * 0.3f, size.height * 0.56f))
    drawCircle(ink.copy(alpha = 0.45f), radius = stroke * 1.6f, center = Offset(cx + bodyHalf * 0.16f, size.height * 0.48f))
    drawCircle(ink.copy(alpha = 0.35f), radius = stroke * 1.2f, center = Offset(cx + bodyHalf * 0.32f, size.height * 0.40f))
}

/** A leaf with a midrib and veins. */
private fun DrawScope.drawLeaf() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val left = size.width * 0.24f
    val right = size.width * 0.76f
    val top = size.height * 0.22f
    val bottom = size.height * 0.78f

    val leaf = Path().apply {
        moveTo(left, bottom)
        quadraticTo(left - size.width * 0.02f, top, (left + right) / 2f, top)
        quadraticTo(right + size.width * 0.02f, top, right, bottom)
        quadraticTo((left + right) / 2f, bottom + size.height * 0.06f, left, bottom)
    }
    drawPath(leaf, ink.copy(alpha = 0.9f), style = Stroke(width = stroke))
    drawLine(
        ink.copy(alpha = 0.8f),
        Offset(left, bottom),
        Offset((left + right) / 2f, top + size.height * 0.02f),
        strokeWidth = stroke * 0.8f
    )
    repeat(4) { vein ->
        val t = 0.2f + vein * 0.18f
        val x = left + (right - left) * t
        val y = bottom - (bottom - top) * t
        drawLine(
            ink.copy(alpha = 0.5f),
            Offset(x, y),
            Offset(x + size.width * 0.1f, y - size.height * 0.1f),
            strokeWidth = stroke * 0.55f
        )
        drawLine(
            ink.copy(alpha = 0.5f),
            Offset(x, y),
            Offset(x - size.width * 0.1f, y - size.height * 0.1f),
            strokeWidth = stroke * 0.55f
        )
    }
}

/** Axes, a parabola and the tangent that touches it. */
private fun DrawScope.drawGraph() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val originX = size.width * 0.22f
    val originY = size.height * 0.74f
    drawLine(ink.copy(alpha = 0.8f), Offset(originX, size.height * 0.16f), Offset(originX, originY), strokeWidth = stroke)
    drawLine(ink.copy(alpha = 0.8f), Offset(originX, originY), Offset(size.width * 0.86f, originY), strokeWidth = stroke)

    val curve = Path()
    var x = originX
    while (x <= size.width * 0.84f) {
        val t = (x - originX) / (size.width * 0.62f)
        val y = originY - (sin(t * 1.6f) * size.height * 0.5f)
        if (x == originX) curve.moveTo(x, y) else curve.lineTo(x, y)
        x += size.width / 80f
    }
    drawPath(curve, ink, style = Stroke(width = stroke * 1.2f))
    val tangentX = originX + (size.width * 0.62f) * 0.42f
    val tangentY = originY - (sin(0.42f * 1.6f) * size.height * 0.5f)
    drawLine(
        ink.copy(alpha = 0.5f),
        Offset(tangentX - size.width * 0.14f, tangentY + size.height * 0.1f),
        Offset(tangentX + size.width * 0.14f, tangentY - size.height * 0.1f),
        strokeWidth = stroke * 0.7f
    )
    drawCircle(ink, radius = stroke * 3f, center = Offset(tangentX, tangentY))
}

/** Four bars and a trend line. */
private fun DrawScope.drawBars() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val baseline = size.height * 0.76f
    val heights = listOf(0.26f, 0.44f, 0.34f, 0.58f)
    val barWidth = size.width * 0.1f
    heights.forEachIndexed { index, height ->
        val left = size.width * (0.18f + index * 0.17f)
        drawRoundRect(
            color = ink.copy(alpha = 0.35f + index * 0.15f),
            topLeft = Offset(left, baseline - size.height * height),
            size = Size(barWidth, size.height * height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 1.4f)
        )
    }
    val trend = Path().apply {
        moveTo(size.width * 0.18f + barWidth / 2f, baseline - size.height * 0.26f)
        lineTo(size.width * 0.35f + barWidth / 2f, baseline - size.height * 0.44f)
        lineTo(size.width * 0.52f + barWidth / 2f, baseline - size.height * 0.34f)
        lineTo(size.width * 0.69f + barWidth / 2f, baseline - size.height * 0.58f)
    }
    drawPath(trend, ink, style = Stroke(width = stroke))
}

/** Lines of text with a quotation mark, for language papers. */
private fun DrawScope.drawTextLines() {
    val ink = Color.White
    val stroke = size.minDimension * 0.011f
    val left = size.width * 0.2f
    repeat(5) { line ->
        val widthFactor = if (line % 2 == 0) 0.62f else 0.48f
        drawLine(
            color = ink.copy(alpha = 0.75f - line * 0.08f),
            start = Offset(left, size.height * (0.3f + line * 0.1f)),
            end = Offset(left + size.width * widthFactor, size.height * (0.3f + line * 0.1f)),
            strokeWidth = stroke
        )
    }
    // opening quotation marks
    drawCircle(ink.copy(alpha = 0.85f), radius = stroke * 2.6f, center = Offset(size.width * 0.2f, size.height * 0.2f))
    drawCircle(ink.copy(alpha = 0.85f), radius = stroke * 2.6f, center = Offset(size.width * 0.26f, size.height * 0.2f))
}

/** The fallback: concentric arcs and a slow rotation, subject-agnostic. */
private fun DrawScope.drawArcs() {
    val ink = Color.White
    rotate(degrees = -18f, pivot = Offset(size.width * 0.5f, size.height * 0.5f)) {
        repeat(4) { ring ->
            val inset = size.minDimension * (0.12f + ring * 0.1f)
            drawArc(
                color = ink.copy(alpha = 0.7f - ring * 0.13f),
                startAngle = 200f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                style = Stroke(width = size.minDimension * 0.012f)
            )
        }
    }
}
