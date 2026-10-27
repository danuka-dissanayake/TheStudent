package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.screens.OnboardingModernFooterArt

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
        initialValue = -350f,
        targetValue = 1350f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF3B4D66) else Color(0xFFFFFFFF)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim.value - 350f, y = translateAnim.value - 350f),
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    this
        .clip(RoundedCornerShape(shapeRadius))
        .background(brush)
}

/**
 * Shimmer modifier specifically tuned for dark-emerald green headers.
 * Sweeps a delicate translucent-white glow across the emerald surface.
 */
fun Modifier.shimmerEffectOnGreen(
    shapeRadius: Dp = 12.dp
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_green_transition")
    val translateAnim = transition.animateFloat(
        initialValue = -350f,
        targetValue = 1350f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_green_translate"
    )

    val baseColor = Color.White.copy(alpha = 0.16f)
    val highlightColor = Color.White.copy(alpha = 0.44f)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim.value - 350f, y = translateAnim.value - 350f),
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

@Composable
fun ShimmerBoxOnGreen(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier.shimmerEffectOnGreen(shapeRadius = shapeRadius)
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

// ─────────────────────────────────────────────────────────────────────────────
//  AUTH SCREEN SKELETONS
// ─────────────────────────────────────────────────────────────────────────────

private val AuthSkeletonGreen = Color(0xFF047857)

/**
 * Full-screen skeleton that mirrors the Sign-In / Sign-Up layout.
 * Shows the green header strip + white bottom sheet with shimmer placeholders.
 */
@Composable
fun AuthSignInSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuthSkeletonGreen)
    ) {
        // ── Top green header ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button circle
            ShimmerBoxOnGreen(
                modifier = Modifier.size(44.dp),
                shapeRadius = 22.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Title line 1
            ShimmerBoxOnGreen(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(26.dp),
                shapeRadius = 8.dp
            )
            // Title line 2
            ShimmerBoxOnGreen(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(26.dp),
                shapeRadius = 8.dp
            )
            // Subtitle
            ShimmerBoxOnGreen(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .height(14.dp),
                shapeRadius = 6.dp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── White bottom sheet ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .shadow(16.dp, RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Segment toggle pill
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shapeRadius = 29.dp
                )

                // Email field
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shapeRadius = 14.dp
                )

                // Password field
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shapeRadius = 14.dp
                )

                // Remember me + Forget row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.size(width = 130.dp, height = 18.dp), shapeRadius = 6.dp)
                    ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 16.dp), shapeRadius = 6.dp)
                }

                // Primary sign-in button
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shapeRadius = 26.dp
                )

                // "Or Login With" divider shimmer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.weight(1f).height(1.dp), shapeRadius = 1.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    ShimmerBox(modifier = Modifier.size(width = 100.dp, height = 14.dp), shapeRadius = 6.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    ShimmerBox(modifier = Modifier.weight(1f).height(1.dp), shapeRadius = 1.dp)
                }

                // Google button
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shapeRadius = 26.dp
                )
            }
        }
    }
}

/**
 * Full-screen skeleton that mirrors the Sign-Up (Register) layout.
 */
@Composable
fun AuthSignUpSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuthSkeletonGreen)
    ) {
        // ── Green header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBoxOnGreen(modifier = Modifier.size(44.dp), shapeRadius = 22.dp)
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBoxOnGreen(modifier = Modifier.fillMaxWidth(0.82f).height(26.dp), shapeRadius = 8.dp)
            ShimmerBoxOnGreen(modifier = Modifier.fillMaxWidth(0.60f).height(26.dp), shapeRadius = 8.dp)
            ShimmerBoxOnGreen(modifier = Modifier.fillMaxWidth(0.88f).height(14.dp), shapeRadius = 6.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── White sheet ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                // Segmented toggle
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(58.dp), shapeRadius = 29.dp)

                // First + Last name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(modifier = Modifier.weight(1f).height(56.dp), shapeRadius = 14.dp)
                    ShimmerBox(modifier = Modifier.weight(1f).height(56.dp), shapeRadius = 14.dp)
                }

                // Email field
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 14.dp)
                // Password field
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 14.dp)
                // Confirm password field
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 14.dp)

                // Register button
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp), shapeRadius = 26.dp)

                // Divider
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(modifier = Modifier.weight(1f).height(1.dp), shapeRadius = 1.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 14.dp), shapeRadius = 6.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    ShimmerBox(modifier = Modifier.weight(1f).height(1.dp), shapeRadius = 1.dp)
                }

                // Google button
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp), shapeRadius = 26.dp)
            }
        }
    }
}

/**
 * Full-screen skeleton that mirrors the OTP / Email Verification screen layout.
 * Perfectly calibrated with the modern light edge-to-edge layout, hero emblem,
 * 6 OTP cards, and tactile squircle keypad.
 */
@Composable
fun AuthOtpSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Celestial background art from splash screen
        OnboardingModernFooterArt(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .alpha(0.85f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Section Skeleton ──────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar: Back button + Security Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.size(42.dp), shapeRadius = 21.dp)
                    ShimmerBox(modifier = Modifier.size(width = 110.dp, height = 28.dp), shapeRadius = 14.dp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hero Emblem Skeleton
                ShimmerBox(modifier = Modifier.size(52.dp), shapeRadius = 18.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Heading Skeleton
                ShimmerBox(modifier = Modifier.size(width = 200.dp, height = 24.dp), shapeRadius = 8.dp)

                Spacer(modifier = Modifier.height(5.dp))

                // Subtitle Skeleton
                ShimmerBox(modifier = Modifier.size(width = 240.dp, height = 13.dp), shapeRadius = 6.dp)

                Spacer(modifier = Modifier.height(6.dp))

                // Target email capsule Skeleton
                ShimmerBox(modifier = Modifier.size(width = 210.dp, height = 28.dp), shapeRadius = 12.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // 6 OTP code boxes Skeleton (46dp x 52dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) {
                        ShimmerBox(
                            modifier = Modifier.size(width = 46.dp, height = 52.dp),
                            shapeRadius = 14.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resend Countdown Row Skeleton
                ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 14.dp), shapeRadius = 6.dp)
            }

            // ── Bottom Section Skeleton: Verify Button + Squircle Keypad ───────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Verify button Skeleton
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(50.dp), shapeRadius = 25.dp)

                // Keypad Skeleton (4 rows × 3 cols, 46dp squircles)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    repeat(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(3) {
                                ShimmerBox(
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shapeRadius = 14.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen shimmer shown briefly when navigating from Auth → Dashboard.
 * Mirrors the main dashboard home screen layout skeleton so the transition
 * feels intentional rather than abrupt.
 */
@Composable
fun NavigationTransitionShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top app bar skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(40.dp), shapeRadius = 20.dp)
            ShimmerBox(modifier = Modifier.size(width = 120.dp, height = 22.dp), shapeRadius = 8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.size(36.dp), shapeRadius = 18.dp)
                ShimmerBox(modifier = Modifier.size(36.dp), shapeRadius = 18.dp)
            }
        }

        // Main content shimmer (HomeSkeletonLoading)
        HomeSkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Bottom nav bar skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShimmerBox(modifier = Modifier.size(24.dp), shapeRadius = 12.dp)
                    ShimmerBox(modifier = Modifier.size(width = 40.dp, height = 8.dp), shapeRadius = 4.dp)
                }
            }
        }
    }
}
