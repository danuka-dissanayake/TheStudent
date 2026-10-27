package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.EmeraldPrimary

/**
 * The single app-wide screen background.
 *
 * A flat surface colour with a very soft emerald wash that starts at the top and
 * fades to nothing by the bottom third — the same treatment used by the profile
 * setup screen, the onboarding slides and the "Welcome to TheStudent" screen, so
 * moving between them feels like one app instead of three.
 *
 * Usage:
 * ```
 * TheStudentAppBackground {
 *     Column(Modifier.fillMaxSize()) { … }
 * }
 * ```
 *
 * The status bar blends in automatically: the composable also makes the system
 * bars transparent and flips the icon colour to suit [baseColor] (dark icons on
 * a light background, white icons on a dark one).
 *
 * @param baseColor flat colour underneath the wash. Defaults to the theme
 *   background (`#F8FAFC` light, dark surface in dark theme). Pass an explicit
 *   colour for screens that pin their own palette.
 */
@Composable
fun TheStudentAppBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    // Icons must contrast with the colour actually under the status bar.
    TheStudentSystemBars(darkIcons = baseColor.luminance() > 0.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        // The wash itself: 12% emerald at the very top, 2% past a third of the
        // way down, fully transparent at the bottom. Subtle on purpose — it must
        // never fight with content or tint white cards.
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
        content()
    }
}
