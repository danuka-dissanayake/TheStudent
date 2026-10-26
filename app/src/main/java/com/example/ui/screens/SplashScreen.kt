package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Concentric multi-ring spinner inspired by CSS tri-spinner with emerald green brand palette.
 * Rings:
 * - Outer ring (54dp): 3.6dp stroke, animated rotation 1s linear
 * - Middle ring (44dp): 3.0dp stroke, animated rotation 2s linear
 * - Inner ring (34dp): 2.4dp stroke, animated rotation 3s linear
 */
@Composable
fun TriSpinnerGreen(
    modifier: Modifier = Modifier,
    sizeDp: Int = 54,
    color: Color = Color(0xFF22C55E),
    secondaryColor: Color = Color(0xFF16A34A),
    innerColor: Color = Color(0xFF4ADE80)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tri_spinner_transition")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    val middleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "middle_rotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp.dp)
    ) {
        // Outer arc (1s rotation)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(outerRotation)
        ) {
            drawArc(
                color = color,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Middle arc (2s rotation)
        Canvas(
            modifier = Modifier
                .size((sizeDp - 10).dp)
                .rotate(middleRotation)
        ) {
            drawArc(
                color = secondaryColor,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner arc (3s rotation)
        Canvas(
            modifier = Modifier
                .size((sizeDp - 20).dp)
                .rotate(innerRotation)
        ) {
            drawArc(
                color = innerColor,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Animated cycling word loader with loading phase completion:
 * Features "loading" label and vertical animated transition through curated educational phases:
 * "papers", "quizzes", "ai tutor", "analytics".
 * Once all loading phases are completed, smoothly transitions into "Ready to go!" text.
 */
@Composable
fun CyclingWordLoader(
    modifier: Modifier = Modifier,
    words: List<String> = listOf("papers", "quizzes", "ai tutor", "analytics"),
    currentWordIndex: Int,
    isReady: Boolean
) {
    AnimatedContent(
        targetState = isReady,
        transitionSpec = {
            (slideInVertically(
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 2 }
            ) + fadeIn(animationSpec = tween(300))).togetherWith(
                slideOutVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it / 2 }
                ) + fadeOut(animationSpec = tween(250))
            )
        },
        label = "ready_word_container_transition"
    ) { ready ->
        if (!ready) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier.height(30.dp)
            ) {
                // "loading" prefix in clean Roboto Medium
                Text(
                    text = "loading",
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp,
                    color = Color(0xFF64748B),
                    letterSpacing = (-0.1).sp
                )

                Spacer(modifier = Modifier.width(7.dp))

                // Animated word transition without background box
                val safeIndex = currentWordIndex.coerceIn(0, words.size - 1)
                AnimatedContent(
                    targetState = words[safeIndex],
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetY = { fullHeight -> fullHeight }
                        ) + fadeIn(animationSpec = tween(280))).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                targetOffsetY = { fullHeight -> -fullHeight }
                            ) + fadeOut(animationSpec = tween(240))
                        )
                    },
                    label = "word_cycle_transition"
                ) { targetWord ->
                    Text(
                        text = targetWord,
                        fontFamily = RobotoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.5.sp,
                        color = Color(0xFF16A34A),
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Ready to go!",
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF16A34A),
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

/**
 * Feature chip representing one of the core pillars of DZD Learn.
 */
@Composable
private fun SplashFeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = Color(0xFF1E293B)
            )
        }
    }
}

/**
 * Redesigned Whole Splash Screen:
 * - Roboto typography across the entire screen.
 * - Crisp white canvas with glowing halo and real DZD Learn emblem.
 * - Structured, aesthetic 3-pillar feature badge layout replacing the plain single-line dot text.
 * - Pure, borderless/backgroundless Tri-spinner + smooth cycling word loader in emerald green.
 * - Seamless transition into onboarding flow.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth entrance scale & alpha animations matching onboarding flow
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0f) }

    val words = remember { listOf("papers", "quizzes", "ai tutor", "analytics") }
    var currentWordIndex by remember { mutableIntStateOf(0) }
    var isReadyToGo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
        // Step through each loading educational phase
        for (i in words.indices) {
            currentWordIndex = i
            delay(400)
        }
        // All loading texts are done! Display "Ready to go!"
        isReadyToGo = true
        delay(600) // Dwell time on "Ready to go!" so the user clearly sees it
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable { onSplashFinished() }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Modern decorative footer art matching onboarding flow
        OnboardingModernFooterArt(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
        )

        // Center content column with proper statusBarsPadding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Real DZD Learn Logo centered inside an elegant circular frame
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(174.dp)
                    .testTag("splash_logo_container")
            ) {
                // Outer soft glowing aura in brand green
                Box(
                    modifier = Modifier
                        .size(174.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF22C55E).copy(alpha = 0.22f),
                                    Color(0xFF16A34A).copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // White disc with pure light green boundary
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(2.8.dp, Color(0xFF22C55E)),
                    modifier = Modifier
                        .size(152.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color(0xFF22C55E).copy(alpha = 0.35f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.the_student_logo),
                            contentDescription = "TheStudent Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Brand Display Name with Roboto Black and highlighted green 'The'
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF16A34A))) { // Emerald green for distinctive branding
                        append("The")
                    }
                    append("Student.")
                },
                style = TextStyle(
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    letterSpacing = (-0.8).sp,
                    color = Color(0xFF0F172A)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Brand Tagline Pill Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0).copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "Everything a Student Needs",
                        fontFamily = RobotoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF16A34A),
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Redesigned Feature Blocks (Replaced the plain single-line text with polished feature chips)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SplashFeatureChip(
                        icon = Icons.Rounded.MenuBook,
                        label = "Smart Practice"
                    )
                    SplashFeatureChip(
                        icon = Icons.Rounded.Psychology,
                        label = "AI Mentor"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SplashFeatureChip(
                        icon = Icons.Rounded.TrendingUp,
                        label = "Rank Analytics"
                    )
                    SplashFeatureChip(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "24/7 Voice Tutor"
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // --- REDESIGNED DYNAMIC LOADER (Tri-spinner + Cycling words -> Ready to go!) ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("tri_spinner_container")
            ) {
                // Spinner to celebratory checkmark transition
                AnimatedContent(
                    targetState = isReadyToGo,
                    transitionSpec = {
                        (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                            .togetherWith(scaleOut() + fadeOut())
                    },
                    label = "spinner_to_ready_badge"
                ) { ready ->
                    if (!ready) {
                        // Multi-ring tri-spinner with custom rotation speeds in green palette
                        TriSpinnerGreen(
                            sizeDp = 50,
                            color = Color(0xFF22C55E),
                            secondaryColor = Color(0xFF16A34A),
                            innerColor = Color(0xFF4ADE80)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(2.dp, Color(0xFF22C55E)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Ready to go",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Cycling animated words transitioning into "Ready to go!"
                CyclingWordLoader(
                    words = words,
                    currentWordIndex = currentWordIndex,
                    isReady = isReadyToGo
                )
            }
        }
    }
}
