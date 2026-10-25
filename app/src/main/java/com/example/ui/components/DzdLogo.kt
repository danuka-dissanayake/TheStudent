package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.TealDark

@Composable
fun DzdLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showText: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Geometric Emerald/Lime Crest Badge
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(
                    Brush.linearGradient(
                        colors = listOf(TealDark, EmeraldPrimary, EmeraldLight)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "DZD Learn Logo",
                tint = Color.White,
                modifier = Modifier.size(size * 0.62f)
            )

            // Lime accent badge dot
            Box(
                modifier = Modifier
                    .size(size * 0.22f)
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LimeAccent)
            )
        }

        if (showText) {
            Column(verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DZD",
                        fontSize = (size.value * 0.46f).sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = EmeraldPrimary)) {
                                append("L")
                            }
                            append("EARN")
                        },
                        fontSize = (size.value * 0.46f).sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Sri Lanka Education Hub",
                    fontSize = (size.value * 0.24f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
