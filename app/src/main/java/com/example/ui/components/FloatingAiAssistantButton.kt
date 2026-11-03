package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Modern draggable floating AI Assistant launcher button.
 *
 * - Can be freely dragged anywhere vertically and horizontally across the screen.
 * - Smoothly snaps to either the **right side** or the **left side** of the screen
 *   when released, remembering its docked side and vertical position.
 * - Renders a sleek obsidian-emerald glass squircle with a custom vector AI constellation
 *   sparkle icon and glowing "AI" pill badge.
 */
@Composable
fun FloatingAiAssistantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var isDockedRight by rememberSaveable { mutableStateOf(true) }
    var verticalFraction by rememberSaveable { mutableFloatStateOf(0.72f) }
    var isDragging by remember { mutableStateOf(false) }

    val buttonSizeDp = 56.dp
    val edgeMarginDp = 14.dp
    val topSafeMarginDp = 88.dp
    val bottomSafeMarginDp = 112.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        val buttonSizePx = with(density) { buttonSizeDp.toPx() }
        val edgeMarginPx = with(density) { edgeMarginDp.toPx() }
        val topSafePx = with(density) { topSafeMarginDp.toPx() }
        val bottomSafePx = with(density) { bottomSafeMarginDp.toPx() }

        val minX = edgeMarginPx
        val maxX = (screenWidthPx - buttonSizePx - edgeMarginPx).coerceAtLeast(minX)
        val minY = topSafePx
        val maxY = (screenHeightPx - buttonSizePx - bottomSafePx).coerceAtLeast(minY)

        val targetDockedX = if (isDockedRight) maxX else minX
        val targetDockedY = (minY + (maxY - minY) * verticalFraction.coerceIn(0f, 1f)).coerceIn(minY, maxY)

        val offsetX = remember { Animatable(targetDockedX) }
        val offsetY = remember { Animatable(targetDockedY) }

        // Keep button within screen bounds if screen size / orientation changes
        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!isDragging && screenWidthPx > 0f && screenHeightPx > 0f) {
                val snappedX = if (isDockedRight) maxX else minX
                val clampedY = (minY + (maxY - minY) * verticalFraction.coerceIn(0f, 1f)).coerceIn(minY, maxY)
                offsetX.snapTo(snappedX)
                offsetY.snapTo(clampedY)
            }
        }

        val scale by animateFloatAsState(
            targetValue = if (isDragging) 1.10f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "ai_fab_drag_scale"
        )

        val infiniteTransition = rememberInfiniteTransition(label = "ai_fab_glow")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.62f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ai_fab_pulse_alpha"
        )

        val buttonShape = RoundedCornerShape(18.dp)

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offsetX.value.roundToInt(),
                        y = offsetY.value.roundToInt()
                    )
                }
                .size(buttonSizeDp)
                .scale(scale)
                .pointerInput(screenWidthPx, screenHeightPx) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val centerX = offsetX.value + buttonSizePx / 2f
                            val snapToRight = centerX >= screenWidthPx / 2f
                            isDockedRight = snapToRight

                            val finalX = if (snapToRight) maxX else minX
                            val finalY = offsetY.value.coerceIn(minY, maxY)
                            val usableHeight = (maxY - minY).coerceAtLeast(1f)
                            verticalFraction = ((finalY - minY) / usableHeight).coerceIn(0f, 1f)

                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = finalX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            coroutineScope.launch {
                                offsetY.animateTo(
                                    targetValue = finalY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            val finalX = if (isDockedRight) maxX else minX
                            val finalY = offsetY.value.coerceIn(minY, maxY)
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = finalX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            coroutineScope.launch {
                                offsetY.animateTo(targetValue = finalY)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextX = (offsetX.value + dragAmount.x).coerceIn(
                                edgeMarginPx * 0.35f,
                                (screenWidthPx - buttonSizePx - edgeMarginPx * 0.35f).coerceAtLeast(edgeMarginPx)
                            )
                            val nextY = (offsetY.value + dragAmount.y).coerceIn(minY, maxY)
                            coroutineScope.launch {
                                offsetX.snapTo(nextX)
                                offsetY.snapTo(nextY)
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("floating_ai_assistant_button"),
            contentAlignment = Alignment.Center
        ) {
            // 1. Soft breathing emerald-violet ambient halo behind the floating button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = pulseAlpha),
                                Color(0xFF8B5CF6).copy(alpha = pulseAlpha * 0.45f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 2. Main modern obsidian-emerald squircle button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(
                        elevation = if (isDragging) 16.dp else 10.dp,
                        shape = buttonShape,
                        ambientColor = Color(0xFF10B981),
                        spotColor = Color(0xFF059669)
                    )
                    .clip(buttonShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF064E3B),
                                Color(0xFF059669)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(140f, 140f)
                        )
                    )
                    .border(
                        width = 1.3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6EE7B7),
                                Color(0xFF10B981),
                                Color(0x44FFFFFF)
                            )
                        ),
                        shape = buttonShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                ModernAiAssistantIcon(
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3. Crisp "AI" mini pill badge at the bottom-right corner
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF10B981),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Custom vector AI Assistant icon featuring a luminous 4-point AI star constellation
 * inside a sleek futuristic portal ring.
 */
@Composable
private fun ModernAiAssistantIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Subtle inner glass portal frame
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            cornerRadius = CornerRadius(w * 0.24f, h * 0.24f),
            style = Stroke(width = w * 0.045f)
        )

        // Primary 4-point AI star sparkle (center-left)
        val cx = w * 0.46f
        val cy = h * 0.48f
        val rx = w * 0.34f
        val ry = h * 0.34f

        val mainStar = Path().apply {
            moveTo(cx, cy - ry)
            quadraticTo(cx, cy, cx + rx, cy)
            quadraticTo(cx, cy, cx, cy + ry)
            quadraticTo(cx, cy, cx - rx, cy)
            quadraticTo(cx, cy, cx, cy - ry)
            close()
        }
        drawPath(
            path = mainStar,
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFA7F3D0))
            ),
            style = Fill
        )

        // Secondary top-right AI sparkle star
        val sx = w * 0.77f
        val sy = h * 0.23f
        val sRad = w * 0.14f
        val subStar = Path().apply {
            moveTo(sx, sy - sRad)
            quadraticTo(sx, sy, sx + sRad, sy)
            quadraticTo(sx, sy, sx, sy + sRad)
            quadraticTo(sx, sy, sx - sRad, sy)
            quadraticTo(sx, sy, sx, sy - sRad)
            close()
        }
        drawPath(
            path = subStar,
            color = Color(0xFF6EE7B7),
            style = Fill
        )

        // Accent luminous dot (bottom-left)
        drawCircle(
            color = Color(0xFF34D399),
            radius = w * 0.048f,
            center = Offset(w * 0.22f, h * 0.76f),
            style = Fill
        )
    }
}
