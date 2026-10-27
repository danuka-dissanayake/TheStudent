package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Make the system bars blend into the screen background.
 *
 * The app draws edge-to-edge (`enableEdgeToEdge()` in `MainActivity`), so the
 * status bar is really "whatever the screen paints behind it". This helper
 * guarantees the two things that make that look deliberate instead of broken:
 *
 *  1. the status and navigation bars are fully transparent, and
 *  2. the status/navigation icons are dark on light backgrounds and white on
 *     dark ones — otherwise white icons vanish on the cream background.
 *
 * Called by [TheStudentAppBackground], and directly by screens that paint their
 * own dark background (the sign-in / sign-up header).
 */
@Composable
@Suppress("DEPRECATION") // statusBarColor/navigationBarColor are legacy APIs on API 35
fun TheStudentSystemBars(darkIcons: Boolean = true) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val window = view.context.findActivity()?.window ?: return

    // SideEffect (not LaunchedEffect): the bars must already match on the very
    // first frame, before the screenshot/splash hand-off is visible.
    SideEffect {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }
}

/** Walk `ContextWrapper`s until the hosting Activity is found. */
private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
