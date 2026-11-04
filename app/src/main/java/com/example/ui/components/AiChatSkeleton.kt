package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Loading state for the AI Assistant screen.
 *
 * A bare spinner over a flat colour is the single strongest "this is a WebView"
 * signal in the app: natives show the shape of the screen immediately and fill
 * it in. This draws the chat screen's silhouette — header, a few bubbles, the
 * composer — in the exact geometry of the web app, so the hand-off when the page
 * commits its first frame is a cross-fade rather than a reveal.
 *
 * One infinite transition drives every block, so the cost is a single animated
 * float per frame no matter how many placeholders there are.
 */
@Composable
fun AiChatSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ai_chat_skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_chat_skeleton_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkeletonBackground)
            .testTag("ai_chat_skeleton")
    ) {
        // Header: apps icon, title pill, trailing action.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBlock(progress, Modifier.size(28.dp), CircleShape)
            Spacer(Modifier.width(12.dp))
            SkeletonBlock(progress, Modifier.width(104.dp).height(13.dp), RoundedCornerShape(7.dp))
            Spacer(Modifier.width(1.dp).weight(1f))
            SkeletonBlock(progress, Modifier.size(26.dp), CircleShape)
            Spacer(Modifier.width(10.dp))
            SkeletonBlock(progress, Modifier.size(26.dp), CircleShape)
        }

        // Conversation: two incoming answers, one outgoing question.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Top) {
                SkeletonBlock(progress, Modifier.size(24.dp), CircleShape)
                Spacer(Modifier.width(10.dp))
                SkeletonBlock(
                    progress,
                    Modifier.fillMaxWidth(0.78f).height(64.dp),
                    RoundedCornerShape(16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                SkeletonBlock(
                    progress,
                    Modifier.fillMaxWidth(0.55f).height(40.dp),
                    RoundedCornerShape(16.dp)
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                SkeletonBlock(progress, Modifier.size(24.dp), CircleShape)
                Spacer(Modifier.width(10.dp))
                SkeletonBlock(
                    progress,
                    Modifier.fillMaxWidth(0.86f).height(96.dp),
                    RoundedCornerShape(16.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                SkeletonBlock(
                    progress,
                    Modifier.fillMaxWidth(0.36f).height(28.dp),
                    RoundedCornerShape(14.dp)
                )
            }
        }

        // Composer: the shape the keyboard will attach to.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBlock(progress, Modifier.size(30.dp), CircleShape)
            Spacer(Modifier.width(10.dp))
            SkeletonBlock(
                progress,
                Modifier.weight(1f).height(46.dp),
                RoundedCornerShape(23.dp)
            )
            Spacer(Modifier.width(10.dp))
            SkeletonBlock(progress, Modifier.size(40.dp), CircleShape)
        }
    }
}

/**
 * A single shimmering placeholder. The sweep runs across the block's own width so
 * blocks of different sizes stay in phase visually without extra transitions.
 */
@Composable
private fun SkeletonBlock(
    progress: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(SkeletonBase)
            .drawBehind {
                val width = size.width
                if (width <= 0f) return@drawBehind
                val x = -width + progress * (width * 2f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            SkeletonHighlight,
                            Color.Transparent
                        ),
                        start = Offset(x, 0f),
                        end = Offset(x + width, 0f)
                    )
                )
            }
    )
}

private val SkeletonBackground = Color(0xFF121016)
private val SkeletonBase = Color(0xFF1E1B24)
private val SkeletonHighlight = Color(0x14FFFFFF)
