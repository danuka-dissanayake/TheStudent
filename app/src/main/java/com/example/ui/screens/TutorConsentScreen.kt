package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  The service disclosure shown once, before the tutor is ever used
//
//  The reference is a "Terms & Conditions" screen: a deep gradient head with a
//  back arrow and a help button, then a white sheet that slides up over it,
//  numbered clauses, and a Decline / Accept pair pinned at the bottom. The
//  colours are the app's emerald and teal instead of the reference's violet —
//  this is a page of TheStudent, not a different product.
//
//  Everything it says is about *this screen*: the microphone, the transcript,
//  the one-time voice download. Nothing about how the backend is built.
// ─────────────────────────────────────────────────────────────────────────────

private val ConsentInk = Color(0xFF0F172A)
private val ConsentMuted = Color(0xFF5B6B7C)
private val ConsentBrand = Color(0xFF059669)
private val ConsentBrandDeep = Color(0xFF047857)
private val ConsentTeal = Color(0xFF0F766E)
private val ConsentSheet = Color(0xFFFFFFFF)

/** One numbered clause. */
private data class Clause(val title: String, val body: String)

private val CLAUSES = listOf(
    Clause(
        title = "The microphone",
        body = "Tapping the microphone opens it for that call only. The orb is a " +
            "picture, not a camera — nothing is recorded and no audio file is ever " +
            "saved on your phone or anywhere else."
    ),
    Clause(
        title = "Your words, not your voice",
        body = "Your phone turns what you say into text. Miss Emma reads that text; " +
            "the sound of your voice stays on your device."
    ),
    Clause(
        title = "What leaves your phone",
        body = "Each sentence you say is sent to the app's AI service so she can reply " +
            "and point out one correction. Your turns, call length and speaking " +
            "minutes are saved with your account, exactly like any other practice " +
            "record in TheStudent."
    ),
    Clause(
        title = "The one-time download",
        body = "The first call downloads Miss Emma's natural voice — about 20 MB — " +
            "so she can speak without an internet connection afterwards. It lives " +
            "in this app's storage and is removed if you clear the app's data."
    ),
    Clause(
        title = "What we never do",
        body = "No listening in the background. No recordings of your calls. No " +
            "selling or sharing what you say. The microphone is off the moment the " +
            "call ends."
    ),
    Clause(
        title = "Your choices",
        body = "Mute or end a call at any time. Decline below and nothing changes: " +
            "this screen simply waits for you to come back."
    )
)

@Composable
fun TutorConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF04201A))) {
        // ── the head: deep emerald to teal, like the app's own gradient ──────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(258.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF03302A), ConsentBrandDeep, ConsentTeal)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp)
            ) {
                CircleGlass(onClick = onDecline) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                CircleGlass(
                    onClick = {
                        // "?" is not decoration: it jumps to the plain-language
                        // "what we never do" clause.
                        scope.launch { scroll.animateScrollTo(scroll.maxValue) }
                    }
                ) {
                    Text(
                        text = "?",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp)) {
                Text(
                    text = "Hello 👋",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Before you start speaking, here is exactly what " +
                        "happens inside this screen.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            // ── the sheet ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(y = (-18).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(ConsentSheet)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(horizontal = 24.dp)
                        .padding(top = 26.dp)
                ) {
                    Text(
                        text = "How the AI tutor works",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ConsentInk
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Last updated: 30 October 2026",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Miss Emma is a speaking partner, not a recording app. " +
                            "Please read this once — it is short, and it explains the " +
                            "only four things this screen does with your data.",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                        color = ConsentMuted
                    )
                    Spacer(Modifier.height(22.dp))

                    CLAUSES.forEachIndexed { index, clause ->
                        ClauseBlock(number = index + 1, clause = clause)
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        text = "You can revisit this at any time from the ⋯ menu.",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(18.dp))
                }

                // ── the two choices, always reachable ────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ConsentSheet.copy(alpha = 0f),
                                    ConsentSheet,
                                    ConsentSheet
                                )
                            )
                        )
                        .padding(horizontal = 24.dp)
                        .padding(top = 14.dp, bottom = 16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ButtonPill(
                            label = "Decline",
                            filled = false,
                            onClick = onDecline,
                            modifier = Modifier.weight(1f).testTag("tutor_consent_decline")
                        )
                        ButtonPill(
                            label = "Accept",
                            filled = true,
                            onClick = onAccept,
                            modifier = Modifier.weight(1f).testTag("tutor_consent_accept")
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "By accepting you agree to the points above.",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ClauseBlock(number: Int, clause: Clause) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(23.dp)
                .clip(CircleShape)
                .background(ConsentBrand.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ConsentBrandDeep
            )
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = clause.title,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = ConsentInk
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = clause.body,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ConsentMuted
            )
        }
    }
}

@Composable
private fun CircleGlass(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ButtonPill(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(if (filled) ConsentBrand else Color(0xFFF1F5F4))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) Color.White else Color(0xFF64748B)
        )
    }
}
