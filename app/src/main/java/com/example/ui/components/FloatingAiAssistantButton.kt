package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class RadialActionIcon {
    AI_CHAT,
    DOWNLOAD_PAPERS,
    CREATE_QUIZ,
    SOLVE_STEPS
}

private data class RadialSpeedDialItem(
    val index: Int,
    val label: String,
    val prompt: String,
    val icon: RadialActionIcon
)

private val DefaultRadialItems = listOf(
    RadialSpeedDialItem(
        index = 0,
        label = "Ask AI Chat",
        prompt = "",
        icon = RadialActionIcon.AI_CHAT
    ),
    RadialSpeedDialItem(
        index = 1,
        label = "Download papers",
        prompt = "Find and download official Sri Lankan past papers and marking schemes.",
        icon = RadialActionIcon.DOWNLOAD_PAPERS
    ),
    RadialSpeedDialItem(
        index = 2,
        label = "Create quiz",
        prompt = "Create an interactive MCQ practice quiz to test my knowledge.",
        icon = RadialActionIcon.CREATE_QUIZ
    ),
    RadialSpeedDialItem(
        index = 3,
        label = "Solve step by step",
        prompt = "Solve this math or science problem step by step with clear explanations.",
        icon = RadialActionIcon.SOLVE_STEPS
    )
)

/**
 * Speed Dial Radial Menu FAB with Backdrop (`fb-07` design) + Draggable Left/Right Docking
 * + Exact Dual Speech-Bubble "Ai" Icon.
 *
 * - **FAB Icon**: Renders the overlapping dual speech-bubble `"Ai"` icon from the user's screenshot.
 * - **Draggable on Left & Right Sides**: Can be dragged anywhere and smoothly snaps to the nearest
 *   horizontal edge (left or right side of the screen).
 * - **Quarter-Circle Radial Speed Dial**: Tapping the FAB opens a dimmed backdrop scrim and fans out
 *   4 actions along a quarter-circle (`cos(i * 30°)`, `sin(i * 30°)`) with staggered spring delays,
 *   and delayed floating labels that arrive a beat after their icons. Automatically flips the
 *   quarter-circle inward depending on whether the FAB is docked on the right or left side.
 */
@Composable
fun FloatingAiAssistantButton(
    onClick: () -> Unit,
    onSelectPrompt: (String) -> Unit = { onClick() },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var isDockedRight by rememberSaveable { mutableStateOf(true) }
    var verticalFraction by rememberSaveable { mutableFloatStateOf(0.76f) }
    var isDragging by remember { mutableStateOf(false) }
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isMenuOpen) {
        isMenuOpen = false
    }

    val fabSizeDp = 62.dp
    val actSizeDp = 52.dp
    val radialDistanceDp = 128.dp
    val edgeMarginDp = 18.dp
    val topSafeMarginDp = 96.dp
    val bottomSafeMarginDp = 112.dp

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isMenuOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "fb07_scrim_alpha"
    )

    val fabScale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.08f
            isMenuOpen -> 0.96f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fb07_fab_scale"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        val fabSizePx = with(density) { fabSizeDp.toPx() }
        val actSizePx = with(density) { actSizeDp.toPx() }
        val radialRadiusPx = with(density) { radialDistanceDp.toPx() }
        val edgeMarginPx = with(density) { edgeMarginDp.toPx() }
        val topSafePx = with(density) { topSafeMarginDp.toPx() }
        val bottomSafePx = with(density) { bottomSafeMarginDp.toPx() }

        val minX = edgeMarginPx
        val maxX = (screenWidthPx - fabSizePx - edgeMarginPx).coerceAtLeast(minX)
        val minY = topSafePx
        val maxY = (screenHeightPx - fabSizePx - bottomSafePx).coerceAtLeast(minY)

        val targetDockedX = if (isDockedRight) maxX else minX
        val targetDockedY = (minY + (maxY - minY) * verticalFraction.coerceIn(0f, 1f)).coerceIn(minY, maxY)

        val offsetX = remember { Animatable(targetDockedX) }
        val offsetY = remember { Animatable(targetDockedY) }

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!isDragging && screenWidthPx > 0f && screenHeightPx > 0f) {
                val snappedX = if (isDockedRight) maxX else minX
                val clampedY = (minY + (maxY - minY) * verticalFraction.coerceIn(0f, 1f)).coerceIn(minY, maxY)
                offsetX.snapTo(snappedX)
                offsetY.snapTo(clampedY)
            }
        }

        // 1. Backdrop Scrim (.fb-07__scrim)
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(scrimAlpha)
                    .background(Color(0x99120D1D))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isMenuOpen = false }
                    )
                    .testTag("floating_ai_scrim")
            )
        }

        // Determine whether the quarter-circle should fan upward (-1f) or downward (+1f)
        // so radial items never clip off the top of the screen if the FAB is dragged high up.
        val fanUpward = (offsetY.value - radialRadiusPx - actSizePx * 0.5f) >= with(density) { 48.dp.toPx() }
        val horizontalDir = if (isDockedRight) -1f else 1f
        val verticalDir = if (fanUpward) -1f else 1f

        // 2. Radial Action Buttons (.fb-07__acts -> 4 items on a quarter-circle: i * 30deg)
        val actBaseInsetPx = (fabSizePx - actSizePx) / 2f

        DefaultRadialItems.forEach { item ->
            val itemProgress = remember(item.index) { Animatable(0f) }
            val labelProgress = remember(item.index) { Animatable(0f) }

            LaunchedEffect(isMenuOpen) {
                if (isMenuOpen) {
                    // Open staggered delay: calc(var(--fb-07-i) * 55ms)
                    val openDelayMs = item.index * 55L
                    launch {
                        delay(openDelayMs)
                        itemProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.68f,
                                stiffness = 380f
                            )
                        )
                    }
                    // Label arrives a beat after its icon: calc(var(--fb-07-i) * 45ms + 120ms)
                    launch {
                        delay(item.index * 45L + 120L)
                        labelProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                    }
                } else {
                    // Close staggered reverse delay: calc((3 - var(--fb-07-i)) * 45ms)
                    val closeDelayMs = (DefaultRadialItems.lastIndex - item.index) * 45L
                    launch {
                        labelProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 120)
                        )
                    }
                    launch {
                        delay(closeDelayMs)
                        itemProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }

            if (itemProgress.value > 0.01f) {
                val angleDeg = item.index * 30.0
                val angleRad = angleDeg * (PI / 180.0)
                val tx = (cos(angleRad).toFloat() * radialRadiusPx * horizontalDir) * itemProgress.value
                val ty = (sin(angleRad).toFloat() * radialRadiusPx * verticalDir) * itemProgress.value

                val actCenterX = offsetX.value + actBaseInsetPx + tx
                val actCenterY = offsetY.value + actBaseInsetPx + ty
                val currentScale = 0.3f + 0.7f * itemProgress.value
                val currentAlpha = itemProgress.value.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = actCenterX.roundToInt(),
                                y = actCenterY.roundToInt()
                            )
                        }
                        .wrapContentSize(align = Alignment.Center, unbounded = true)
                ) {
                    // Floating Label Pill (.fb-07__lab) placed beside the circular button
                    val labelSlidePx = with(density) { 8.dp.toPx() } * (1f - labelProgress.value) * (if (isDockedRight) 1f else -1f)
                    val labelGapPx = with(density) { 12.dp.toPx() }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE01B1626),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(if (isDockedRight) Alignment.CenterStart else Alignment.CenterEnd)
                            .offset {
                                val xShift = if (isDockedRight) {
                                    -(actSizePx / 2f + labelGapPx) + labelSlidePx
                                } else {
                                    (actSizePx / 2f + labelGapPx) + labelSlidePx
                                }
                                IntOffset(x = xShift.roundToInt(), y = 0)
                            }
                            .graphicsLayer {
                                alpha = labelProgress.value.coerceIn(0f, 1f)
                                // Shift by 50% of own width so it sits cleanly outside the 52dp button
                                translationX = if (isDockedRight) -size.width / 2f else size.width / 2f
                            }
                            .clickable(
                                enabled = isMenuOpen,
                                onClick = {
                                    isMenuOpen = false
                                    if (item.prompt.isBlank()) onClick() else onSelectPrompt(item.prompt)
                                }
                            )
                    ) {
                        Text(
                            text = item.label,
                            color = Color(0xFFF7F5FA),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }

                    // Circular Radial Action Button (.fb-07__act: 52dp circle, white surface, dark icon)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFAF9FC),
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(actSizeDp)
                            .scale(currentScale)
                            .alpha(currentAlpha)
                            .clip(CircleShape)
                            .clickable(
                                enabled = isMenuOpen,
                                onClick = {
                                    isMenuOpen = false
                                    if (item.prompt.isBlank()) onClick() else onSelectPrompt(item.prompt)
                                }
                            )
                            .testTag("radial_ai_action_${item.index}")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            RadialActionSvgIcon(
                                icon = item.icon,
                                tint = Color(0xFF1E1B2E),
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Main Floating Button (.fb-07__fab) with the Dual Speech-Bubble "Ai" Icon
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offsetX.value.roundToInt(),
                        y = offsetY.value.roundToInt()
                    )
                }
                .size(fabSizeDp)
                .scale(fabScale)
                .pointerInput(screenWidthPx, screenHeightPx, isMenuOpen) {
                    if (isMenuOpen) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val centerX = offsetX.value + fabSizePx / 2f
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
                                (screenWidthPx - fabSizePx - edgeMarginPx * 0.35f).coerceAtLeast(edgeMarginPx)
                            )
                            val nextY = (offsetY.value + dragAmount.y).coerceIn(minY, maxY)
                            coroutineScope.launch {
                                offsetX.snapTo(nextX)
                                offsetY.snapTo(nextY)
                            }
                        }
                    )
                }
                .shadow(
                    elevation = if (isDragging || isMenuOpen) 16.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF4F4F6)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        isMenuOpen = !isMenuOpen
                    }
                )
                .testTag("floating_ai_assistant_button"),
            contentAlignment = Alignment.Center
        ) {
            val closeRotation by animateFloatAsState(
                targetValue = if (isMenuOpen) 135f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 340f
                ),
                label = "fb07_icon_rotation"
            )

            if (isMenuOpen) {
                // Close (+) rotated 135deg into an (×) just like .fb-07__fab[aria-expanded="true"] svg
                Canvas(
                    modifier = Modifier
                        .size(27.dp)
                        .graphicsLayer { rotationZ = closeRotation }
                ) {
                    val w = size.width
                    val h = size.height
                    val strokeW = w * 0.11f
                    drawLine(
                        color = Color(0xFF0A0A0D),
                        start = Offset(w * 0.5f, h * 0.18f),
                        end = Offset(w * 0.5f, h * 0.82f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF0A0A0D),
                        start = Offset(w * 0.18f, h * 0.5f),
                        end = Offset(w * 0.82f, h * 0.5f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                // Exact Dual Speech-Bubble "Ai" Icon matching the user's attached image
                AiDualChatBubblesIcon(
                    bubbleColor = Color(0xFF0A0A0D),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * Exact vector reproduction of the user's attached "Ai" Dual Speech-Bubbles Icon (`image-2.png`):
 * - Back lower-left circular speech bubble with sharp bottom-left tail, cleanly crescent-cut
 *   around the front bubble.
 * - Front upper-right circular speech bubble with sharp bottom-right tail and bold geometric
 *   "Ai" cut out inside.
 */
@Composable
fun AiDualChatBubblesIcon(
    bubbleColor: Color = Color(0xFF0A0A0D),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Original artwork coordinate system: 260 x 250 (x: 34..294, y: 11..261)
        val scaleX = size.width / 260f
        val scaleY = size.height / 250f
        val s = minOf(scaleX, scaleY)
        val ox = (size.width - 260f * s) / 2f - 34f * s
        val oy = (size.height - 250f * s) / 2f - 11f * s

        fun px(x: Float) = ox + x * s
        fun py(y: Float) = oy + y * s

        // 1. Front circle (upper-right) + bottom-right tail
        val frontCenter = Offset(px(199f), py(105f))
        val frontRadius = 92f * s

        val frontBubblePath = Path().apply {
            addOval(
                Rect(
                    left = frontCenter.x - frontRadius,
                    top = frontCenter.y - frontRadius,
                    right = frontCenter.x + frontRadius,
                    bottom = frontCenter.y + frontRadius
                )
            )
            // Sharp bottom-right speech tail
            moveTo(px(236f), py(188f))
            lineTo(px(280f), py(199f))
            lineTo(px(267f), py(163f))
            close()
        }

        // Cutout gap around the front bubble to carve the crescent back bubble
        val gapRadius = 108f * s
        val frontGapPath = Path().apply {
            addOval(
                Rect(
                    left = frontCenter.x - gapRadius,
                    top = frontCenter.y - gapRadius,
                    right = frontCenter.x + gapRadius,
                    bottom = frontCenter.y + gapRadius
                )
            )
        }

        // 2. Back circle (lower-left) + bottom-left tail
        val backCenter = Offset(px(128f), py(163f))
        val backRadius = 92f * s

        val backRawPath = Path().apply {
            addOval(
                Rect(
                    left = backCenter.x - backRadius,
                    top = backCenter.y - backRadius,
                    right = backCenter.x + backRadius,
                    bottom = backCenter.y + backRadius
                )
            )
            // Sharp bottom-left speech tail
            moveTo(px(62f), py(220f))
            lineTo(px(47f), py(258f))
            lineTo(px(92f), py(242f))
            close()
        }

        val backCrescentPath = Path().apply {
            op(backRawPath, frontGapPath, PathOperation.Difference)
        }

        // 3. Bold "A" and "i" cutout inside the front bubble
        val letterCutoutPath = Path().apply {
            // Outer contour of "A" (with horizontal crossbar)
            moveTo(px(171f), py(55f))
            lineTo(px(185f), py(55f))
            lineTo(px(214f), py(137f))
            lineTo(px(198f), py(137f))
            lineTo(px(193f), py(122f))
            lineTo(px(163f), py(122f))
            lineTo(px(158f), py(137f))
            lineTo(px(142f), py(137f))
            close()

            // Dot of "i"
            addRect(
                Rect(
                    left = px(235f),
                    top = py(67f),
                    right = px(250f),
                    bottom = py(81f)
                )
            )

            // Stem of "i"
            addRect(
                Rect(
                    left = px(235f),
                    top = py(95f),
                    right = px(250f),
                    bottom = py(137f)
                )
            )
        }

        // Inner triangle hole of "A" (kept solid)
        val aInnerHolePath = Path().apply {
            moveTo(px(178f), py(73f))
            lineTo(px(189f), py(109f))
            lineTo(px(167f), py(109f))
            close()
        }

        val netCutoutPath = Path().apply {
            op(letterCutoutPath, aInnerHolePath, PathOperation.Difference)
        }

        val finalFrontBubblePath = Path().apply {
            op(frontBubblePath, netCutoutPath, PathOperation.Difference)
        }

        drawPath(path = backCrescentPath, color = bubbleColor, style = Fill)
        drawPath(path = finalFrontBubblePath, color = bubbleColor, style = Fill)
    }
}

/**
 * Crisp 24x24 vector icons for the 4 radial speed-dial buttons matching the `.fb-07__act svg` style.
 */
@Composable
private fun RadialActionSvgIcon(
    icon: RadialActionIcon,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = w * 0.08f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (icon) {
            RadialActionIcon.AI_CHAT -> {
                // Clean chat bubble + AI star
                val path = Path().apply {
                    moveTo(w * 0.20f, h * 0.24f)
                    lineTo(w * 0.80f, h * 0.24f)
                    quadraticTo(w * 0.88f, h * 0.24f, w * 0.88f, h * 0.32f)
                    lineTo(w * 0.88f, h * 0.66f)
                    quadraticTo(w * 0.88f, h * 0.74f, w * 0.80f, h * 0.74f)
                    lineTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.22f, h * 0.88f)
                    lineTo(w * 0.26f, h * 0.74f)
                    lineTo(w * 0.20f, h * 0.74f)
                    quadraticTo(w * 0.12f, h * 0.74f, w * 0.12f, h * 0.66f)
                    lineTo(w * 0.12f, h * 0.32f)
                    quadraticTo(w * 0.12f, h * 0.24f, w * 0.20f, h * 0.24f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.34f, h * 0.44f),
                    end = Offset(w * 0.66f, h * 0.44f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.34f, h * 0.56f),
                    end = Offset(w * 0.54f, h * 0.56f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }

            RadialActionIcon.DOWNLOAD_PAPERS -> {
                // Document with folded corner (<path d="M6 4h9l4 4v12H6z"/><path d="M15 4v5h4"/>)
                val docPath = Path().apply {
                    moveTo(w * 0.25f, h * 0.16f)
                    lineTo(w * 0.625f, h * 0.16f)
                    lineTo(w * 0.79f, h * 0.33f)
                    lineTo(w * 0.79f, h * 0.84f)
                    lineTo(w * 0.25f, h * 0.84f)
                    close()
                }
                val foldPath = Path().apply {
                    moveTo(w * 0.625f, h * 0.16f)
                    lineTo(w * 0.625f, h * 0.35f)
                    lineTo(w * 0.79f, h * 0.35f)
                }
                drawPath(path = docPath, color = tint, style = stroke)
                drawPath(path = foldPath, color = tint, style = stroke)
            }

            RadialActionIcon.CREATE_QUIZ -> {
                // Checklist / Quiz lines
                drawLine(
                    color = tint,
                    start = Offset(w * 0.42f, h * 0.30f),
                    end = Offset(w * 0.82f, h * 0.30f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.42f, h * 0.50f),
                    end = Offset(w * 0.82f, h * 0.50f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.42f, h * 0.70f),
                    end = Offset(w * 0.82f, h * 0.70f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawCircle(color = tint, radius = w * 0.055f, center = Offset(w * 0.24f, h * 0.30f))
                drawCircle(color = tint, radius = w * 0.055f, center = Offset(w * 0.24f, h * 0.50f))
                drawCircle(color = tint, radius = w * 0.055f, center = Offset(w * 0.24f, h * 0.70f))
            }

            RadialActionIcon.SOLVE_STEPS -> {
                // Sigma (∑) step-by-step math symbol
                val sigmaPath = Path().apply {
                    moveTo(w * 0.76f, h * 0.22f)
                    lineTo(w * 0.26f, h * 0.22f)
                    lineTo(w * 0.56f, h * 0.50f)
                    lineTo(w * 0.26f, h * 0.78f)
                    lineTo(w * 0.76f, h * 0.78f)
                }
                drawPath(path = sigmaPath, color = tint, style = stroke)
            }
        }
    }
}
