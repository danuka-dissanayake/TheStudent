package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.example.data.model.QuizSummary
import com.example.data.model.SubjectCodes
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * The picture at the top of a quiz's detail page.
 *
 * It is a **real photograph from Pexels**, chosen by the quiz's own subject and
 * title, and loaded with Coil. Pexels' CDN needs no API key, permits commercial
 * use without attribution, and serves a cropped, compressed size on request, so
 * the app can fetch a 1200×675 hero in one URL:
 *
 * ```
 * https://images.pexels.com/photos/<id>/pexels-photo-<id>.jpeg?auto=compress&cs=tinysrgb&w=1200&h=675&fit=crop
 * ```
 *
 * While the photo downloads nothing is blank: a gradient in the subject's colour
 * plus a shimmer holds the space, the photo crossfades in on top, and if the
 * request fails the gradient stays with the subject's short code on it — so the
 * card always reads as finished, never as broken.
 *
 * The photo is chosen the same way every time for the same quiz (keyword match on
 * subject → title → description), so the list, the detail page and the result
 * page always agree. To use a real photo per quiz instead, add a `hero_image_url`
 * column and pass it in [QuizHeroArt]; the curated map is only the fallback.
 */

/** One entry per topic family: the photo and the colours it sits on. */
private data class HeroLook(
    val photoId: String,
    val top: Color,
    val bottom: Color
)

/**
 * Topic → look. The first group that matches wins, so the order is the order of
 * specificity: a quiz called "Biology: genetics" is biology, not whatever
 * "genetics" might otherwise fall through to.
 */
private val HERO_LOOKS: List<Pair<List<String>, HeroLook>> = listOf(
    // Newton's cradle — mechanics, forces, motion.
    listOf(
        "mechanic", "motion", "kinematic", "force", "newton", "momentum", "energy",
        "gravit", "friction", "projectile", "physics"
    ) to HeroLook("10424708", Color(0xFF047857), Color(0xFF34D399)),

    // Waves, sound, light, optics.
    listOf(
        "wave", "sound", "light", "optic", "oscillat", "vibration", "resonan",
        "electromagnetic"
    ) to HeroLook("9785612", Color(0xFF4338CA), Color(0xFFA78BFA)),

    // Circuits, electronics, ICT.
    listOf(
        "circuit", "current", "electric", "electronic", "voltage", "ict", "computer",
        "information", "network", "logic gate"
    ) to HeroLook("34526423", Color(0xFF5B21B6), Color(0xFFC4B5FD)),

    // Chemistry: glassware and reactions.
    listOf(
        "chem", "reaction", "acid", "base", "mole", "organic", "inorganic", "bond",
        "solution"
    ) to HeroLook("9243559", Color(0xFF0E7490), Color(0xFF67E8F9)),

    // Biology, agriculture and food.
    listOf(
        "bio", "cell", "genetic", "plant", "animal", "ecolog", "photosynth",
        "human body", "agri", "agricultur", "food"
    ) to HeroLook("5842066", Color(0xFF15803D), Color(0xFF86EFAC)),

    // Mathematics: numbers, graphs, geometry.
    listOf(
        "math", "algebra", "geometry", "trigon", "calculus", "statistic",
        "probability", "vector", "matri"
    ) to HeroLook("5412108", Color(0xFF1D4ED8), Color(0xFF93C5FD)),

    // Commerce, accounting, economics.
    listOf(
        "account", "commerce", "business", "economic", "finance", "marketing",
        "trade", "management"
    ) to HeroLook("5816298", Color(0xFFB45309), Color(0xFFFCD34D)),

    // Languages and literature.
    listOf(
        "english", "sinhala", "tamil", "language", "literature", "grammar", "essay",
        "poem", "novel"
    ) to HeroLook("7935224", Color(0xFFBE185D), Color(0xFFF9A8D4))
)

/** Anything that matched nothing: a quiet study photograph on emerald. */
private val DEFAULT_LOOK = HeroLook("8199558", Color(0xFF065F46), Color(0xFF6EE7B7))

/**
 * The look a quiz earns, from its own words. Deterministic by design.
 *
 * Private because [HeroLook] is private to this file — a public function cannot
 * hand out a type nobody outside can name. If another screen ever needs the
 * theme, promote both to internal rather than just this one.
 */
private fun quizHeroLook(quiz: QuizSummary): HeroLook {
    val text = "${quiz.subject} ${quiz.title} ${quiz.description}".lowercase()
    return HERO_LOOKS.firstOrNull { (words, _) -> words.any { text.contains(it) } }?.second
        ?: DEFAULT_LOOK
}

/** The CDN URL for a look, at hero size and cropped to 16:9. */
private fun heroUrl(photoId: String): String =
    "https://images.pexels.com/photos/$photoId/pexels-photo-$photoId.jpeg" +
        "?auto=compress&cs=tinysrgb&w=1200&h=675&fit=crop"

/**
 * The hero card: the topic photo over a gradient of the same family, with the
 * subject's short code in a glass medallion.
 */
@Composable
fun QuizHeroArt(
    quiz: QuizSummary,
    modifier: Modifier = Modifier
) {
    val look = remember(quiz.id, quiz.subject, quiz.title) { quizHeroLook(quiz) }
    var isLoaded by remember(quiz.id, quiz.heroImageUrl) { mutableStateOf(false) }

    // An image the admin uploaded wins; the curated photo is the fallback. The
    // key includes the uploaded URL, so swapping it in the admin panel replaces
    // the picture immediately instead of showing a cached old one.
    val uploaded = quiz.heroImageUrl
    val model: Any = uploaded?.takeIf { it.isNotBlank() } ?: heroUrl(look.photoId)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(look.top, look.bottom))
            ),
        contentAlignment = Alignment.Center
    ) {
        // No blank grey box while the photo is in flight: the gradient is already
        // the right colour, and the shimmer says "loading" without moving layout.
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect(shapeRadius = 20.dp)
            )
        }

        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state ->
                isLoaded = state is AsyncImagePainter.State.Success
            }
        )

        // A scrim keeps the medallion legible over any photograph.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color(0x66000000)
                    )
                )
        )

        // Where the mockup had a play button. There is no video behind a quiz, so
        // the medallion carries the subject code instead of pretending.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.24f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = SubjectCodes.of(quiz.subject),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.6.sp
            )
        }
    }
}
