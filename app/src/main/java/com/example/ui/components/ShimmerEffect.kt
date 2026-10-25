package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance, smooth skeleton shimmer effect for loading states.
 * Uses an infinite gradient transition that sweeps diagonally across the surface.
 */
fun Modifier.shimmerEffect(
    shapeRadius: Dp = 12.dp,
    showShimmer: Boolean = true
): Modifier = composed {
    if (!showShimmer) return@composed this

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim.value - 300f, y = translateAnim.value - 300f),
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    this
        .clip(RoundedCornerShape(shapeRadius))
        .background(brush)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier.shimmerEffect(shapeRadius = shapeRadius)
    )
}

/**
 * Realistic Skeleton placeholder for Course cards.
 */
@Composable
fun CourseCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(modifier = Modifier.size(width = 80.dp, height = 22.dp), shapeRadius = 6.dp)
            ShimmerBox(modifier = Modifier.size(24.dp), shapeRadius = 12.dp)
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(20.dp), shapeRadius = 6.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp), shapeRadius = 6.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 16.dp), shapeRadius = 4.dp)
            ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 32.dp), shapeRadius = 8.dp)
        }
    }
}

/**
 * Realistic Skeleton placeholder for Past Papers list.
 */
@Composable
fun PaperTileSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(46.dp), shapeRadius = 10.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(18.dp), shapeRadius = 6.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 60.dp, height = 14.dp), shapeRadius = 4.dp)
                ShimmerBox(modifier = Modifier.size(width = 45.dp, height = 14.dp), shapeRadius = 4.dp)
                ShimmerBox(modifier = Modifier.size(width = 80.dp, height = 14.dp), shapeRadius = 4.dp)
            }
        }
    }
}

/**
 * Realistic Skeleton placeholder for Home Screen top header & continue learning card.
 */
@Composable
fun HomeSkeletonLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Card Skeleton
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 24.dp))
                    ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 16.dp))
                }
                ShimmerBox(modifier = Modifier.size(width = 65.dp, height = 28.dp), shapeRadius = 14.dp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(modifier = Modifier.size(width = 95.dp, height = 48.dp), shapeRadius = 12.dp)
                ShimmerBox(modifier = Modifier.size(width = 95.dp, height = 48.dp), shapeRadius = 12.dp)
                ShimmerBox(modifier = Modifier.size(width = 95.dp, height = 48.dp), shapeRadius = 12.dp)
            }
        }

        // Active Course Card Skeleton
        CourseCardSkeleton()

        // Carousel / Papers header Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(modifier = Modifier.size(width = 140.dp, height = 20.dp))
            ShimmerBox(modifier = Modifier.size(width = 60.dp, height = 20.dp))
        }

        PaperTileSkeleton()
        PaperTileSkeleton()
    }
}
