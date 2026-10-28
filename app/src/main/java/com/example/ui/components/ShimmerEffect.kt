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
import com.example.ui.theme.EmeraldPrimary

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
 * Skeleton for the dashboard.
 *
 * It mirrors the real layout block for block — greeting, hero report card, four
 * quick-action tiles, the weekly report card and the first rail — so the
 * shimmer that plays while content loads resolves into the same shapes instead
 * of jumping to a different design.
 */
@Composable
fun HomeSkeletonLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Greeting row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 118.dp, height = 14.dp))
                ShimmerBox(modifier = Modifier.size(width = 196.dp, height = 32.dp))
            }
        }

        // Hero report card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(98.dp), shapeRadius = 49.dp)
                Spacer(Modifier.size(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            ShimmerBox(modifier = Modifier.size(width = 74.dp, height = 14.dp))
                            ShimmerBox(modifier = Modifier.size(width = 96.dp, height = 9.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(38.dp), shapeRadius = 19.dp)
        }

        // Quick actions.
        ShimmerBox(modifier = Modifier.size(width = 104.dp, height = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ShimmerBox(modifier = Modifier.size(34.dp), shapeRadius = 11.dp)
                    Spacer(Modifier.size(7.dp))
                    ShimmerBox(modifier = Modifier.size(width = 44.dp, height = 10.dp))
                }
            }
        }

        // Weekly report.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(width = 88.dp, height = 16.dp))
                Spacer(Modifier.weight(1f))
                ShimmerBox(modifier = Modifier.size(width = 54.dp, height = 14.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    repeat(3) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp)) }
                }
                ShimmerBox(modifier = Modifier.weight(1f).height(76.dp), shapeRadius = 6.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) {
                    ShimmerBox(modifier = Modifier.size(width = 72.dp, height = 20.dp), shapeRadius = 10.dp)
                }
            }
        }

        // First rail.
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(width = 128.dp, height = 18.dp))
            Spacer(Modifier.weight(1f))
            ShimmerBox(modifier = Modifier.size(width = 48.dp, height = 14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // The real rail cards are a fixed 178dp wide, so these are too.
            repeat(2) {
                Box(modifier = Modifier.width(178.dp)) { CourseCardSkeleton() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AUTH SCREEN SKELETONS
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
//  AUTH SCREEN SKELETONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Skeleton shared by the sign-in and sign-up screens.
 *
 * It mirrors the redesigned auth layout 1:1 — slim top bar with the back circle
 * and the logo mark, the big headline block, the segmented mode switch and the
 * single floating white card that holds the form — so the shimmer that plays
 * during a mode switch looks like the real screen loading in, not a different
 * design.
 */
@Composable
private fun AuthFormSkeleton(
    isSignUp: Boolean,
    modifier: Modifier = Modifier
) {
    // Opaque, so the real form underneath never shows through while the
    // shimmer cross-fades in. The wash is reproduced here so the skeleton and
    // the screen it replaces share exactly the same backdrop.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to EmeraldPrimary.copy(alpha = 0.12f),
                        0.35f to EmeraldPrimary.copy(alpha = 0.02f),
                        1f to Color.Transparent
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── top bar: back circle + logo mark ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.size(42.dp), shapeRadius = 21.dp)
                Spacer(Modifier.weight(1f))
                ShimmerBox(
                    modifier = Modifier.size(width = 94.dp, height = 26.dp),
                    shapeRadius = 8.dp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(10.dp))

                // ── headline + subheadline ───────────────────────────────────────
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(if (isSignUp) 0.72f else 0.52f)
                        .height(30.dp),
                    shapeRadius = 10.dp
                )
                Spacer(Modifier.height(10.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(if (isSignUp) 0.88f else 0.62f)
                        .height(14.dp),
                    shapeRadius = 7.dp
                )

                Spacer(Modifier.height(22.dp))

                // ── segmented sign in / sign up switch ───────────────────────────
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shapeRadius = 16.dp
                )

                Spacer(Modifier.height(18.dp))

                // ── floating form card ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSignUp) {
                        // First + last name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ShimmerBox(modifier = Modifier.weight(1f).height(56.dp), shapeRadius = 14.dp)
                            ShimmerBox(modifier = Modifier.weight(1f).height(56.dp), shapeRadius = 14.dp)
                        }
                    }

                    // Email address
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 14.dp)

                    // Password
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 14.dp)

                    if (!isSignUp) {
                        // Remember me + forgot password row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShimmerBox(modifier = Modifier.size(18.dp), shapeRadius = 4.dp)
                                ShimmerBox(
                                    modifier = Modifier.size(width = 86.dp, height = 12.dp),
                                    shapeRadius = 6.dp
                                )
                            }
                            ShimmerBox(
                                modifier = Modifier.size(width = 104.dp, height = 12.dp),
                                shapeRadius = 6.dp
                            )
                        }
                    }

                    if (isSignUp) {
                        // Confirm password
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 14.dp)

                        // Terms + privacy consent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ShimmerBox(modifier = Modifier.size(18.dp), shapeRadius = 4.dp)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), shapeRadius = 6.dp)
                                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp), shapeRadius = 6.dp)
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // Primary button
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp), shapeRadius = 26.dp)

                    // "or continue with" divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBox(modifier = Modifier.weight(1f).height(1.dp), shapeRadius = 1.dp)
                        Spacer(Modifier.width(10.dp))
                        ShimmerBox(
                            modifier = Modifier.size(width = 96.dp, height = 11.dp),
                            shapeRadius = 6.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        ShimmerBox(modifier = Modifier.weight(1f).height(1.dp), shapeRadius = 1.dp)
                    }

                    // Google button
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp), shapeRadius = 26.dp)
                }

                Spacer(Modifier.height(18.dp))

                // ── footer mode switch ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(width = 140.dp, height = 13.dp),
                        shapeRadius = 7.dp
                    )
                }
            }
        }
    }
}

/**
 * Shimmer shown while the app switches into the sign-in form.
 */
@Composable
fun AuthSignInSkeleton(modifier: Modifier = Modifier) {
    AuthFormSkeleton(isSignUp = false, modifier = modifier)
}

/**
 * Shimmer shown while the app switches into the sign-up form.
 */
@Composable
fun AuthSignUpSkeleton(modifier: Modifier = Modifier) {
    AuthFormSkeleton(isSignUp = true, modifier = modifier)
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
