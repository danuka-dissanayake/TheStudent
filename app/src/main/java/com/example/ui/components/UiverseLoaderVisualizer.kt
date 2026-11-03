package com.example.ui.components

import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * EXACT Uiverse.io loader by mrhyddenn — the dual rotating orbit rings with
 * gold and lime glowing particles the app uses for assistant loading states.
 *
 * It is local HTML/CSS only: JavaScript is disabled, no network base URL is
 * used, and no file/content access is granted. The WebView is destroyed when the
 * composable leaves the tree, so repeatedly showing it cannot leak WebViews.
 */
@Composable
fun UiverseLoaderVisualizer(
    modifier: Modifier = Modifier,
    size: Dp = 170.dp
) {
    val exactHtmlAndCss = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body {
              width: 100%; height: 100%;
              background: transparent !important;
              background-color: transparent !important;
              display: flex; align-items: center; justify-content: center;
              overflow: hidden;
            }

            /* From Uiverse.io by mrhyddenn */
            .loader {
              width: 6em; height: 6em; font-size: 13px;
              position: relative; display: flex;
              align-items: center; justify-content: center;
            }

            .loader .face {
              position: absolute; border-radius: 50%; border-style: solid;
              animation: animate023845 3s linear infinite;
            }

            .loader .face:nth-child(1) {
              width: 100%; height: 100%; color: gold;
              border-color: currentColor transparent transparent currentColor;
              border-width: 0.2em 0.2em 0em 0em;
              --deg: -45deg; animation-direction: normal;
            }

            .loader .face:nth-child(2) {
              width: 70%; height: 70%; color: lime;
              border-color: currentColor currentColor transparent transparent;
              border-width: 0.2em 0em 0em 0.2em;
              --deg: -135deg; animation-direction: reverse;
            }

            .loader .face .circle {
              position: absolute; width: 50%; height: 0.1em;
              top: 50%; left: 50%; background-color: transparent;
              transform: rotate(var(--deg)); transform-origin: left;
            }

            .loader .face .circle::before {
              position: absolute; top: -0.5em; right: -0.5em; content: '';
              width: 1em; height: 1em; background-color: currentColor;
              border-radius: 50%;
              box-shadow: 0 0 2em, 0 0 4em, 0 0 6em, 0 0 8em, 0 0 10em,
                          0 0 0 0.5em rgba(255, 255, 0, 0.1);
            }

            @keyframes animate023845 { to { transform: rotate(1turn); } }
          </style>
        </head>
        <body>
          <div class="loader">
            <div class="face"><div class="circle"></div></div>
            <div class="face"><div class="circle"></div></div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    val ownedWebView = remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("uiverse_loader_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.size(size),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = false          // pure CSS animation
                        domStorageEnabled = false
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        allowFileAccess = false
                        allowContentAccess = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    // Loading state is not interactive; let touches pass through.
                    setOnTouchListener { _, _ -> false }
                    loadDataWithBaseURL(null, exactHtmlAndCss, "text/html", "UTF-8", null)
                    ownedWebView.value = this
                }
            },
            update = {}
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            ownedWebView.value?.let { view ->
                view.stopLoading()
                view.destroy()
            }
            ownedWebView.value = null
        }
    }
}
