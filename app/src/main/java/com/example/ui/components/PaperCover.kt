package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PastPaper
import com.example.data.model.SubjectCodes
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * The thumbnail shown for a paper.
 *
 * The paper API has no thumbnail column, so most papers arrive with an empty
 * `thumbnailUrl` — showing one shared placeholder image made every row look the
 * same and told the user nothing. Instead each paper gets a cover drawn from its
 * own metadata: a sheet of paper with the examination colour, the subject's
 * short code (CM, SFT, ICT, AGRI …), the year and the medium. If a real
 * thumbnail URL ever arrives, that image wins.
 */
@Composable
fun PaperCover(
    paper: PastPaper,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp
) {
    val tint = paperCoverTint(paper.subject)
    Surface(
        shape = RoundedCornerShape(corner),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x140F172A)),
        modifier = modifier
    ) {
        // The drawn cover sits *underneath* the image and shows through until a
        // thumbnail paints over it. That is the same look as the old
        // `SubcomposeAsyncImage(loading = { CoverArt })`, without subcomposition:
        // subcomposing every row is one of the most expensive things a lazy list
        // can do, and this list is the app's main scrolling surface.
        CoverArt(paper, tint)
        if (paper.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = paper.thumbnailUrl,
                contentDescription = paper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
            )
        }
    }
}

/**
 * A miniature exam paper: a coloured header band carrying the subject name, then
 * the body with the subject's short code and ruled lines. Deliberately drawn
 * from primitives so it is crisp at any size and costs nothing to load.
 */
@Composable
private fun CoverArt(paper: PastPaper, tint: Color) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header band — the subject name, in white on the examination colour.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
                .background(tint)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = paper.subject,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                lineHeight = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Body — the short code ("SFT", "ICT", "AGRI") plus ruled lines.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f)
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SubjectCodes.of(paper.subject),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MiniLine(1f, tint.copy(alpha = 0.35f))
                MiniLine(0.75f, Color(0xFFCBD5E1))
                MiniLine(0.55f, Color(0xFFE2E8F0))
            }
        }
    }
}

@Composable
private fun MiniLine(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(1.5.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(color)
    )
}

/** Small pill with the paper year, used where the cover has room for it. */
@Composable
fun PaperYearPill(paper: PastPaper, modifier: Modifier = Modifier) {
    val tint = paperCoverTint(paper.subject)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = paper.year.toString(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

private val PAPER_COVER_PALETTE = listOf(
    Color(0xFF059669), Color(0xFF2563EB), Color(0xFF7C3AED),
    Color(0xFFDB2777), Color(0xFFEA580C), Color(0xFF0891B2),
    Color(0xFF65A30D), Color(0xFF9333EA)
)

/**
 * Stable colour per subject, so the same subject always looks the same.
 *
 * The palette is a top-level value: this is called a few times per row in every
 * list, and building the list of eight colours on each call was pure allocation
 * on the scrolling path.
 */
fun paperCoverTint(subject: String): Color {
    val size = PAPER_COVER_PALETTE.size
    val index = (subject.hashCode().rem(size) + size).rem(size)
    return PAPER_COVER_PALETTE[index]
}
