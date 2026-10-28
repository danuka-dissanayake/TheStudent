package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.remote.TheStudentGoogleAuthClient
import com.example.data.remote.GoogleAuthResult
import com.example.data.remote.TheStudentAuthClient
import com.example.data.remote.AuthApiResult
import com.example.data.local.TheStudentPreferences
import com.example.ui.components.StudentLoadingSpinner
import com.example.ui.components.TheStudentAppBackground
import com.example.ui.components.TheStudentSnackbarHost
import com.example.ui.theme.MaisonNeueFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldDark
import kotlinx.coroutines.launch

// Colors aligned with the vibrant emerald green primary palette
private val OnboardingLimeGreen = EmeraldPrimary // Vibrant Emerald Green
private val OnboardingLimeGreenButton = EmeraldDark // Vibrant Emerald Green button
private val OnboardingTrackInactive = Color(0xFFE2E8F0) // Subtle clean gray track
private val OnboardingDarkText = Color(0xFF0F172A) // Slate 900 for high-contrast typography
private val OnboardingMutedText = Color(0xFF64748B) // Slate 500 for secondary descriptions
private val OnboardingBg = Color(0xFFFCFDFD) // Clean light background

data class TrustBadge(
    val text: String,
    val icon: ImageVector
)

data class OnboardingCardData(
    val imageUrl: String,
    val badges: List<TrustBadge>,
    val headline: String,
    val description: String,
    val fallbackGradient: List<Color>
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: (openAuth: Boolean, isRegister: Boolean) -> Unit,
    onContinueAsGuest: () -> Unit,
    onStartGoogleVerification: ((UserProfile, String) -> Unit)? = null,
    onAuthSuccess: ((UserProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val snackbarHostState = remember { SnackbarHostState() }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf("🇱🇰 Sri Lanka") }
    var showRegionSelector by remember { mutableStateOf(false) }

    // 3 rich onboarding slides + 1 welcome landing screen (total 4 steps)
    val slides = remember {
        listOf(
            OnboardingCardData(
                imageUrl = "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&auto=format&fit=crop&q=80",
                badges = listOf(
                    TrustBadge("Verified resources", Icons.Default.CheckCircle),
                    TrustBadge("Fast answers", Icons.Default.AccessTime)
                ),
                headline = "Find verified resources\navailable to help you",
                description = "TheStudent connects you with syllabus-aligned past papers, marking schemes, and expert guidance ready for any exam.",
                fallbackGradient = listOf(Color(0xFFDCFCE7), Color(0xFFBBF7D0))
            ),
            OnboardingCardData(
                imageUrl = "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=900&auto=format&fit=crop&q=80",
                badges = listOf(
                    TrustBadge("24/7 AI tutor", Icons.Default.AutoAwesome),
                    TrustBadge("Spoken viva lab", Icons.Default.RecordVoiceOver)
                ),
                headline = "Every question has an answer\nLearn with AI",
                description = "Ask questions anytime, practice English viva interviews, and understand concepts at your own pace with 24/7 AI guidance.",
                fallbackGradient = listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE))
            ),
            OnboardingCardData(
                imageUrl = "https://images.unsplash.com/photo-1513258496099-48168024aec0?w=900&auto=format&fit=crop&q=80",
                badges = listOf(
                    TrustBadge("Rank analytics", Icons.Default.TrendingUp),
                    TrustBadge("Daily streaks", Icons.Default.Bolt)
                ),
                headline = "Track your progress\nAchieve your dream rank",
                description = "Measure your solved papers, follow curated revision paths, and build unstoppable daily study momentum with streak rewards.",
                fallbackGradient = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
            )
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        snackbarHost = {
            TheStudentSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .imePadding()
            )
        },
        // Transparent on purpose: the background below is drawn edge-to-edge, so
        // the status and navigation bars show the same colour as the screen
        // instead of a flat opaque strip. Inner padding is applied to the
        // content column, never to the background.
        containerColor = Color.Transparent
    ) { innerPadding ->
        // Same emerald-wash background as the profile setup screen. Onboarding
        // pins its own light palette, so the base colour is passed explicitly
        // (the wash itself comes from the shared composable, which also makes
        // the system bars transparent).
        TheStudentAppBackground(baseColor = OnboardingBg) {
            // Elegant modern geometric arch & starlight background art at footer
            OnboardingModernFooterArt(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .align(Alignment.BottomCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)      // keep content clear of the bars
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Progress Status Segmented Bar (4 segments synchronized with pagerState.currentPage)
                OnboardingProgressBar(
                    totalSteps = 4,
                    currentStep = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_progress_bar")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Horizontal Pager allowing natural left/right swiping across all 4 screens with switch & flash animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("onboarding_pager")
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                                    // Pinned translation cancels out horizontal conveyor sliding for an in-place switch effect
                                    translationX = pageOffset * size.width
                                    // Rapid switch crossfade
                                    alpha = (1f - absOffset * 1.4f).coerceIn(0f, 1f)
                                    // Subtle punchy switch scale
                                    val scale = lerp(1f, 0.94f, absOffset)
                                    scaleX = scale
                                    scaleY = scale
                                }
                        ) {
                            when (page) {
                                0, 1, 2 -> {
                                    val slide = slides[page]
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // High-quality Photo Card with increased height & Floating Trust Badges
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(380.dp) // Increased height for enhanced visual presence
                                                .clip(RoundedCornerShape(26.dp))
                                                .background(
                                                    Brush.verticalGradient(slide.fallbackGradient)
                                                )
                                                .shadow(8.dp, RoundedCornerShape(26.dp))
                                        ) {
                                            // Stock image from Unsplash
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(slide.imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = slide.headline,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Subtle dark-to-transparent scrim at bottom for badge contrast
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f))
                                                        )
                                                    )
                                            )

                                            // Floating Trust Badges over bottom of image
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                slide.badges.forEach { badge ->
                                                    Surface(
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = Color.White.copy(alpha = 0.94f),
                                                        shadowElevation = 3.dp,
                                                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = badge.icon,
                                                                contentDescription = null,
                                                                tint = OnboardingLimeGreenButton,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Text(
                                                                text = badge.text,
                                                                fontFamily = MaisonNeueFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = OnboardingDarkText
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Large ExtraBold Headline in Maison Neue
                                        Text(
                                            text = slide.headline,
                                            fontFamily = MaisonNeueFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 25.sp,
                                            lineHeight = 32.sp,
                                            color = OnboardingDarkText,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Subtitle description
                                        Text(
                                            text = slide.description,
                                            fontFamily = MaisonNeueFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.5.sp,
                                            lineHeight = 21.sp,
                                            color = OnboardingMutedText,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(30.dp))

                                        // Continue Button: snappy switch animation with fast easing
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(
                                                        page = page + 1,
                                                        animationSpec = tween(
                                                            durationMillis = 300,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .shadow(4.dp, RoundedCornerShape(26.dp), spotColor = OnboardingLimeGreenButton.copy(alpha = 0.35f))
                                                .testTag("btn_onboarding_continue"),
                                            shape = RoundedCornerShape(26.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = OnboardingLimeGreen,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                text = "Continue",
                                                fontFamily = MaisonNeueFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }

                                3 -> {
                                    // Screen 4: Redesigned Welcome and Auth Landing Screen with Circular Logo Frame & Skeleton Shimmer
                                    WelcomeAuthLandingContent(
                                        selectedRegion = selectedRegion,
                                        isGoogleLoading = isGoogleSigningIn,
                                        onSelectRegionClick = { if (!isGoogleSigningIn) showRegionSelector = true },
                                        onContinueWithApple = {
                                            if (isGoogleSigningIn) return@WelcomeAuthLandingContent
                                            onFinishOnboarding(true, false)
                                        },
                                        onContinueWithGoogle = {
                                            if (isGoogleSigningIn) return@WelcomeAuthLandingContent
                                            coroutineScope.launch {
                                                isGoogleSigningIn = true
                                                when (val result = TheStudentGoogleAuthClient.signIn(context)) {
                                                    is GoogleAuthResult.Success -> {
                                                        TheStudentPreferences.setIsOnboarded(context, true)
                                                        // Use the server URL configured on this device instead of a
                                                        // hard-coded host, so admin-configured endpoints are honoured.
                                                        val configuredBaseUrl = com.example.data.remote.PastPapersRemoteRepository(context).backendBaseUrl
                                                        when (val authResult = TheStudentAuthClient.googleLogin(configuredBaseUrl, result.idToken)) {
                                                            is AuthApiResult.RequiresOtp -> {
                                                                if (onStartGoogleVerification != null) {
                                                                    onStartGoogleVerification(result.profile, "")
                                                                } else {
                                                                    onFinishOnboarding(true, false)
                                                                }
                                                            }
                                                            is AuthApiResult.Success -> {
                                                                TheStudentPreferences.setSession(
                                                                    context = context,
                                                                    token = authResult.token,
                                                                    userEmail = authResult.profile.email,
                                                                    userName = authResult.profile.name,
                                                                    rememberMe = true,
                                                                    role = authResult.role,
                                                                    expiresAt = authResult.expiresAt
                                                                )
                                                                if (onAuthSuccess != null) {
                                                                    onAuthSuccess(authResult.profile)
                                                                } else {
                                                                    onFinishOnboarding(false, false)
                                                                }
                                                            }
                                                            is AuthApiResult.ResetRequired -> {
                                                                android.widget.Toast.makeText(context, "Please sign in again to continue.", android.widget.Toast.LENGTH_LONG).show()
                                                                onFinishOnboarding(true, false)
                                                            }
                                                            is AuthApiResult.Error -> {
                                                                android.widget.Toast.makeText(context, authResult.message, android.widget.Toast.LENGTH_LONG).show()
                                                                onFinishOnboarding(true, false)
                                                            }
                                                        }
                                                    }
                                                    is GoogleAuthResult.Cancelled -> {
                                                        android.widget.Toast.makeText(context, "Sign in with Google was cancelled", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    is GoogleAuthResult.Error -> {
                                                        android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                isGoogleSigningIn = false
                                            }
                                        },
                                        onSignUpWithEmail = {
                                            if (!isGoogleSigningIn) onFinishOnboarding(true, true)
                                        },
                                        onSignIn = {
                                            if (!isGoogleSigningIn) onFinishOnboarding(true, false)
                                        },
                                        onContinueAsGuest = {
                                            // Protected flow: Always require authentication
                                            if (!isGoogleSigningIn) onFinishOnboarding(true, false)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Luminous switch/flash overlay that peaks at the exact switch apex between slides
                    val offsetFraction = pagerState.currentPageOffsetFraction
                    val flashAlpha = (kotlin.math.sin(kotlin.math.abs(offsetFraction) * Math.PI.toFloat()) * 0.40f).coerceIn(0f, 1f)
                    if (flashAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = flashAlpha
                                }
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

        // Region / Stream selection dialog
        if (showRegionSelector) {
            AlertDialog(
                onDismissRequest = { showRegionSelector = false },
                title = {
                    Text(
                        text = "Select Academic Region / Stream",
                        fontFamily = MaisonNeueFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "🇱🇰 Sri Lanka (National Curriculum)",
                            "🇱🇰 G.C.E. A/L (Physical Science)",
                            "🇱🇰 G.C.E. A/L (Biological Science)",
                            "🇱🇰 G.C.E. A/L (Technology Stream)",
                            "🇱🇰 G.C.E. O/L Examination",
                            "🇱🇰 Undergraduate (Degree Level)"
                        ).forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedRegion in item) OnboardingLimeGreen.copy(alpha = 0.12f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRegion = if (item.startsWith("🇱🇰 Sri Lanka")) "🇱🇰 Sri Lanka" else item.substringBefore(" (")
                                        showRegionSelector = false
                                    }
                            ) {
                                Text(
                                    text = item,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (selectedRegion in item) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedRegion in item) OnboardingLimeGreenButton else OnboardingDarkText,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRegionSelector = false }) {
                        Text("Done", fontFamily = MaisonNeueFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
}

/**
 * Modern Segmented Progress Status Bar.
 */
@Composable
fun OnboardingProgressBar(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isFilled = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        if (isFilled) OnboardingLimeGreen else OnboardingTrackInactive
                    )
            )
        }
    }
}

/**
 * Newly Redesigned Modern Background Art for Footer.
 * Replaces previous simple wave curves with an elegant architectural arch gradient,
 * floating geometric isometric grid tiles, and soft light particles for a futuristic educational look.
 */
@Composable
fun OnboardingModernFooterArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Grand central elliptical dome aura in pure light green
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF22C55E).copy(alpha = 0.14f),
                    Color(0xFF16A34A).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h),
                radius = w * 0.70f
            ),
            topLeft = Offset(w * -0.20f, h * 0.20f),
            size = androidx.compose.ui.geometry.Size(w * 1.40f, h * 1.60f)
        )

        // 2. Modern architectural nested arch contour lines
        for (i in 1..4) {
            val progress = i / 4f
            val path = Path().apply {
                moveTo(w * (0.05f + progress * 0.08f), h)
                cubicTo(
                    w * (0.20f + progress * 0.05f), h * (0.85f - progress * 0.55f),
                    w * (0.80f - progress * 0.05f), h * (0.85f - progress * 0.55f),
                    w * (0.95f - progress * 0.08f), h
                )
            }
            drawPath(
                path = path,
                color = Color(0xFF22C55E).copy(alpha = 0.10f - i * 0.018f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 3. Subtle floating ambient star / sparkle coordinates
        val points = listOf(
            Offset(w * 0.15f, h * 0.45f) to 3.5.dp.toPx(),
            Offset(w * 0.28f, h * 0.70f) to 2.5.dp.toPx(),
            Offset(w * 0.50f, h * 0.32f) to 4.5.dp.toPx(),
            Offset(w * 0.72f, h * 0.65f) to 3.0.dp.toPx(),
            Offset(w * 0.86f, h * 0.40f) to 4.0.dp.toPx()
        )
        points.forEach { (point, radius) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22C55E).copy(alpha = 0.45f), Color.Transparent),
                    center = point,
                    radius = radius * 2f
                ),
                radius = radius,
                center = point
            )
        }
    }
}

/**
 * Screen 4: Redesigned Welcome Screen.
 * Features:
 * - Circular logo frame with adequate inner breathing room so TheStudent logo is never cropped.
 * - Bold display typography using high-contrast bold font styling.
 * - Clean action buttons stacked below.
 */
@Composable
fun WelcomeAuthLandingContent(
    selectedRegion: String,
    onSelectRegionClick: () -> Unit,
    onContinueWithApple: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onSignUpWithEmail: () -> Unit,
    onSignIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
    isGoogleLoading: Boolean = false
) {
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 76.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

        // --- CIRCULAR IMAGE FRAME FOR THESTUDENT LOGO ---
        // Generously sized circular container with ample breathing room so TheStudent logo is 100% visible and uncropped
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(174.dp)
                .testTag("circular_logo_container")
        ) {
            // 1. Soft glowing outer backdrop halo
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

            // 2. Main circular white disc with pure light green boundary
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(2.8.dp, Color(0xFF22C55E)),
                modifier = Modifier
                    .size(154.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color(0xFF22C55E).copy(alpha = 0.35f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Crisp TheStudent Emblem centered inside circular frame with ContentScale.Fit
                    Image(
                        painter = painterResource(id = R.drawable.the_student_icon),
                        contentDescription = "TheStudent Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("the_student_circular_logo"),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 3. Floating miniature sparkle badge pinned on circular rim
            Surface(
                shape = CircleShape,
                color = OnboardingLimeGreenButton,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- MODERN DISPLAY TYPOGRAPHY WITH TRULY BOLD THESTUDENT TEXT ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Welcome to",
                fontFamily = MaisonNeueFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                letterSpacing = 0.5.sp,
                color = OnboardingMutedText
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = OnboardingLimeGreenButton)) {
                        append("The")
                    }
                    append("Student.")
                },
                fontFamily = MaisonNeueFontFamily,
                fontWeight = FontWeight.Black, // Explicit Black weight for prominent bold look
                style = TextStyle(
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    letterSpacing = (-0.8).sp,
                    color = OnboardingDarkText
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle caption with clean modern tracking
            Text(
                text = "Everything a Student Needs • AI Study Suite",
                fontFamily = MaisonNeueFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                letterSpacing = 0.2.sp,
                color = OnboardingMutedText
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- COMMUNITY USER TRUST BADGE LABEL ---
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
            modifier = Modifier
                .clickable(enabled = !isGoogleLoading, onClick = onSelectRegionClick)
                .testTag("region_badge_label")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Small glowing emerald dot indicator
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(OnboardingLimeGreen)
                )
                Text(
                    text = "Join 50K+ learners in",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedRegion,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF14532D)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- AUTH ACTION BUTTONS STACKED CLEANLY BELOW ---

        // 1. Continue with Google Button (Premium styled white card with custom loading spinner)
        Surface(
            onClick = { if (!isGoogleLoading) onContinueWithGoogle() },
            enabled = !isGoogleLoading,
            shape = RoundedCornerShape(26.dp),
            color = if (isGoogleLoading) Color(0xFFF8FAFC) else Color.White,
            border = BorderStroke(1.2.dp, if (isGoogleLoading) Color(0xFFCBD5E1) else Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = if (isGoogleLoading) 1.dp else 3.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0x14000000),
                    ambientColor = Color(0x08000000)
                )
                .testTag("btn_continue_google")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isGoogleLoading) {
                    StudentLoadingSpinner(
                        size = 26.dp,
                        isOnDarkBackground = false
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Connecting to Google...",
                        fontFamily = MaisonNeueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF334155),
                        letterSpacing = (-0.2).sp
                    )
                } else {
                    GoogleLogoIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontFamily = MaisonNeueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1F2937),
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Sign up with email Button (Light styled pill button)
        Surface(
            onClick = { if (!isGoogleLoading) onSignUpWithEmail() },
            enabled = !isGoogleLoading,
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0x0F000000),
                    ambientColor = Color(0x06000000)
                )
                .testTag("btn_signup_email")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = if (!isGoogleLoading) OnboardingDarkText else Color(0xFF94A3B8),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign up with email",
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (!isGoogleLoading) OnboardingDarkText else Color(0xFF94A3B8),
                    letterSpacing = (-0.2).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. "Already have an account? Sign in"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clickable(enabled = !isGoogleLoading, onClick = onSignIn)
                .padding(vertical = 4.dp, horizontal = 12.dp)
                .testTag("btn_signin_link")
        ) {
            Text(
                text = "Already have an account? ",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.5.sp,
                color = OnboardingMutedText
            )
            Text(
                text = "Sign in",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isGoogleLoading) OnboardingLimeGreenButton else Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // --- ATTACHED DIRECTLY TO THE BOTTOM OF THE SCREEN ---
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Security & Student Privacy Trust Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.testTag("security_trust_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Secure",
                    tint = OnboardingLimeGreenButton,
                    modifier = Modifier.size(13.5.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% Free & Secure for Sri Lankan Students 🇱🇰",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnboardingMutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legal disclaimer with interactive Terms & Privacy links
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "By continuing, you agree to our ",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "Terms",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnboardingLimeGreenButton,
                modifier = Modifier
                    .clickable { showTermsDialog = true }
                    .padding(vertical = 2.dp)
            )
            Text(
                text = " & ",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "Privacy Policy",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnboardingLimeGreenButton,
                modifier = Modifier
                    .clickable { showPrivacyDialog = true }
                    .padding(vertical = 2.dp)
            )
        }
    }
}

    // Interactive Terms of Service dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = "Terms of Service",
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Welcome to TheStudent! By accessing our app, you agree to use the educational past papers, marking schemes, and AI tools for personal academic study and exam preparation. All materials are curated to support Sri Lankan students.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = OnboardingDarkText
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close", fontFamily = MaisonNeueFontFamily, fontWeight = FontWeight.Bold, color = OnboardingLimeGreenButton)
                }
            }
        )
    }

    // Interactive Privacy Policy dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Privacy Policy",
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your privacy is our priority. TheStudent does not sell or share your personal data. Your study progress, saved papers, and AI conversation history are kept secure and encrypted.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = OnboardingDarkText
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", fontFamily = MaisonNeueFontFamily, fontWeight = FontWeight.Bold, color = OnboardingLimeGreenButton)
                }
            }
        )
    }
}
