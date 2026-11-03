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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    val icon: RadialActionIcon,
    val cosAngle: Float,
    val sinAngle: Float
)

private val DefaultRadialItems = listOf(
    RadialSpeedDialItem(
        index = 0,
        label = "Ask AI Chat",
        prompt = "",
        icon = RadialActionIcon.AI_CHAT,
        cosAngle = cos(0.0).toFloat(),
        sinAngle = sin(0.0).toFloat()
    ),
    RadialSpeedDialItem(
        index = 1,
        label = "Download papers",
        prompt = "Find and download official Sri Lankan past papers and marking schemes.",
        icon = RadialActionIcon.DOWNLOAD_PAPERS,
        cosAngle = cos(30.0 * PI / 180.0).toFloat(),
        sinAngle = sin(30.0 * PI / 180.0).toFloat()
    ),
    RadialSpeedDialItem(
        index = 2,
        label = "Create quiz",
        prompt = "Create an interactive MCQ practice quiz to test my knowledge.",
        icon = RadialActionIcon.CREATE_QUIZ,
        cosAngle = cos(60.0 * PI / 180.0).toFloat(),
        sinAngle = sin(60.0 * PI / 180.0).toFloat()
    ),
    RadialSpeedDialItem(
        index = 3,
        label = "Solve step by step",
        prompt = "Solve this math or science problem step by step with clear explanations.",
        icon = RadialActionIcon.SOLVE_STEPS,
        cosAngle = cos(90.0 * PI / 180.0).toFloat(),
        sinAngle = sin(90.0 * PI / 180.0).toFloat()
    )
)

/**
 * Speed Dial Radial Menu FAB with Backdrop (`fb-07` design) + Draggable Left/Right Docking
 * + Exact Dual Speech-Bubble "Ai" Icon.
 *
 * Performance & Layout Optimizations:
 * - Compact `52.dp` main FAB and `44.dp` radial action buttons, positioned lower on initial load
 *   just above the bottom navigation bar (`verticalFraction = 0.92f`).
 * - Zero-recomposition 60/120fps animations: all `Animatable` values are read strictly inside
 *   GPU `graphicsLayer` lambdas instead of during composition.
 * - Non-overlapping labels: labels are placed via custom `Modifier.layout` outside the full width
 *   of each action icon with a guaranteed `10.dp` gap, and rendered in a dedicated top z-index layer
 *   above all radial icons so no icon ever hides any part of the label text.
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
    // Lower initial vertical position on first load (just above the bottom nav bar)
    var verticalFraction by rememberSaveable { mutableFloatStateOf(0.92f) }
    var isDragging by remember { mutableStateOf(false) }
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    var keepRadialComposed by remember { mutableStateOf(false) }

    BackHandler(enabled = isMenuOpen) {
        isMenuOpen = false
    }

    // Reduced, sleeker button dimensions
    val fabSizeDp = 52.dp
    val actSizeDp = 44.dp
    val radialDistanceDp = 114.dp
    val edgeMarginDp = 16.dp
    val topSafeMarginDp = 88.dp
    val bottomSafeMarginDp = 90.dp

    val scrimAnim = remember { Animatable(0f) }
    val itemAnims = remember { List(DefaultRadialItems.size) { Animatable(0f) } }
    val labelAnims = remember { List(DefaultRadialItems.size) { Animatable(0f) } }

    LaunchedEffect(isMenuOpen) {
        if (isMenuOpen) {
            keepRadialComposed = true
            launch {
                scrimAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
            DefaultRadialItems.forEach { item ->
                val idx = item.index
                launch {
                    delay(idx * 45L)
                    itemAnims[idx].animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = 420f
                        )
                    )
                }
                launch {
                    delay(idx * 40L + 95L)
                    labelAnims[idx].animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                }
            }
        } else {
            launch {
                scrimAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                )
            }
            DefaultRadialItems.forEach { item ->
                val idx = item.index
                val reverseDelay = (DefaultRadialItems.lastIndex - idx) * 35L
                launch {
                    labelAnims[idx].animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 100)
                    )
                }
                launch {
                    delay(reverseDelay)
                    itemAnims[idx].animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
                    )
                }
            }
            delay(DefaultRadialItems.size * 35L + 200L)
            if (!isMenuOpen) {
                keepRadialComposed = false
            }
        }
    }

    val fabScale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.08f
            isMenuOpen -> 0.95f
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
        val labelGapPx = with(density) { 10.dp.roundToPx() }
        val labelSlidePx = with(density) { 8.dp.toPx() }

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

        // 1. Backdrop Scrim (.fb-07__scrim) — animated via graphicsLayer for 0 recompositions
        if (keepRadialComposed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .graphicsLayer {
                        alpha = scrimAnim.value
                    }
                    .background(Color(0x99120D1D))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = isMenuOpen,
                        onClick = { isMenuOpen = false }
                    )
                    .testTag("floating_ai_scrim")
            )
        }

        val fanUpward = (offsetY.value - radialRadiusPx - actSizePx * 0.5f) >= with(density) { 48.dp.toPx() }
        val horizontalDir = if (isDockedRight) -1f else 1f
        val verticalDir = if (fanUpward) -1f else 1f
        val actBaseInsetPx = (fabSizePx - actSizePx) / 2f

        if (keepRadialComposed) {
            // 2A. Radial Action Icon Circles Layer (zIndex = 2f)
            DefaultRadialItems.forEach { item ->
                val itemAnim = itemAnims[item.index]

                Box(
                    modifier = Modifier
                        .zIndex(2f)
                        .offset {
                            IntOffset(
                                x = (offsetX.value + actBaseInsetPx).roundToInt(),
                                y = (offsetY.value + actBaseInsetPx).roundToInt()
                            )
                        }
                        .size(actSizeDp)
                        .graphicsLayer {
                            val p = itemAnim.value
                            val clamped = p.coerceIn(0f, 1f)
                            translationX = item.cosAngle * radialRadiusPx * horizontalDir * p
                            translationY = item.sinAngle * radialRadiusPx * verticalDir * p
                            val s = 0.3f + 0.7f * clamped
                            scaleX = s
                            scaleY = s
                            alpha = clamped
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFAF9FC),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxSize()
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2B. Floating Label Pills Layer (zIndex = 3f, rendered above ALL icons so no icon ever hides text)
            DefaultRadialItems.forEach { item ->
                val itemAnim = itemAnims[item.index]
                val labelAnim = labelAnims[item.index]

                Box(
                    modifier = Modifier
                        .zIndex(3f)
                        .offset {
                            IntOffset(
                                x = (offsetX.value + actBaseInsetPx).roundToInt(),
                                y = (offsetY.value + actBaseInsetPx).roundToInt()
                            )
                        }
                        .size(actSizeDp)
                        .graphicsLayer {
                            val p = itemAnim.value
                            translationX = item.cosAngle * radialRadiusPx * horizontalDir * p
                            translationY = item.sinAngle * radialRadiusPx * verticalDir * p
                        }
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE81B1626),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                // Measure label at its natural width without being constrained by the 44dp icon box
                                val looseConstraints = constraints.copy(
                                    minWidth = 0,
                                    maxWidth = Int.MAX_VALUE,
                                    minHeight = 0
                                )
                                val placeable = measurable.measure(looseConstraints)
                                val iconBoxPx = actSizeDp.roundToPx()
                                layout(0, 0) {
                                    // Place completely outside the 44dp circular icon with a 10dp gap
                                    val xPos = if (isDockedRight) {
                                        -placeable.width - labelGapPx
                                    } else {
                                        iconBoxPx + labelGapPx
                                    }
                                    val yPos = (iconBoxPx - placeable.height) / 2
                                    placeable.place(xPos, yPos)
                                }
                            }
                            .graphicsLayer {
                                val lp = labelAnim.value.coerceIn(0f, 1f)
                                alpha = lp
                                translationX = labelSlidePx * (1f - lp) * (if (isDockedRight) 1f else -1f)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 3. Main Floating Button (.fb-07__fab, 52.dp) with the Dual Speech-Bubble "Ai" Icon
        val closeRotation by animateFloatAsState(
            targetValue = if (isMenuOpen) 135f else 0f,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = 360f
            ),
            label = "fb07_icon_rotation"
        )

        Box(
            modifier = Modifier
                .zIndex(4f)
                .offset {
                    IntOffset(
                        x = offsetX.value.roundToInt(),
                        y = offsetY.value.roundToInt()
                    )
                }
                .size(fabSizeDp)
                .graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                }
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
                    elevation = 10.dp,
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
            if (isMenuOpen) {
                // Close (+) rotated 135deg into an (×) just like .fb-07__fab[aria-expanded="true"] svg
                Canvas(
                    modifier = Modifier
                        .size(22.dp)
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
                    modifier = Modifier.size(33.dp)
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
