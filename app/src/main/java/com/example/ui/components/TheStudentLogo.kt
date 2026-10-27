package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimary

@Composable
fun TheStudentLogo(
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
        // Crisp TheStudent Emblem Mark (High-res, perfectly sharp at any DP)
        Image(
            painter = painterResource(id = R.drawable.the_student_icon),
            contentDescription = "TheStudent Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )

        if (showText) {
            Column(verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = EmeraldPrimary)) {
                                append("The")
                            }
                            withStyle(SpanStyle(color = textColor)) {
                                append("Student.")
                            }
                        },
                        fontSize = (size.value * 0.46f).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Everything a Student Needs",
                    fontSize = (size.value * 0.24f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Deprecated("Use TheStudentLogo instead", ReplaceWith("TheStudentLogo(modifier, size, showText, textColor)"))
@Composable
fun DzdLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showText: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    TheStudentLogo(modifier, size, showText, textColor)
}
