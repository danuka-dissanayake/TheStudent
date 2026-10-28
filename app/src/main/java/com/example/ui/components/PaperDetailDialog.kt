package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PastPaper
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * Paper Detail Bottom Sheet Card matching exact reference image (image_e6edf588.png):
 * - Top drag handle bar
 * - Mint subject initial avatar + Paper Title + "Marking scheme included" subtitle + Close button (X)
 * - 4 Stat Cards Row: YEAR (2024), DURATION (3 hrs), PAGES (24), MEDIUM (Sinhala/English in mint badge)
 * - Main Action Row:
 *   - Solid Black "Download PDF" button displaying paper file size (e.g., 3.1 MB)
 *   - Mint Green "Ask AI" button
 */
@Composable
fun PaperDetailDialog(
    paper: PastPaper,
    isSaved: Boolean = false,
    isDownloaded: Boolean = false,
    onToggleSave: () -> Unit = {},
    onDownload: () -> Unit = {},
    onAskAi: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = false

    // Determine subject initial letter
    val subjectInitial = remember(paper.subject, paper.title) {
        when {
            paper.subject.contains("Math", ignoreCase = true) -> "M"
            paper.subject.contains("Physics", ignoreCase = true) -> "P"
            paper.subject.contains("Science", ignoreCase = true) -> "S"
            paper.subject.contains("Scholarship", ignoreCase = true) -> "E"
            paper.subject.contains("Chem", ignoreCase = true) -> "C"
            paper.subject.contains("Bio", ignoreCase = true) -> "B"
            paper.subject.contains("English", ignoreCase = true) -> "E"
            paper.subject.contains("Accounting", ignoreCase = true) -> "A"
            paper.subject.contains("Commerce", ignoreCase = true) -> "C"
            paper.subject.contains("History", ignoreCase = true) -> "H"
            paper.subject.contains("Sinhala", ignoreCase = true) -> "S"
            paper.subject.contains("Tamil", ignoreCase = true) -> "T"
            else -> paper.subject.take(1).uppercase().ifEmpty { "P" }
        }
    }

    // Dynamic duration hint
    val durationHint = remember(paper.grade, paper.examType) {
        when {
            paper.grade.contains("12") || paper.grade.contains("13") -> "3 hrs"
            paper.grade.contains("10") || paper.grade.contains("11") -> "3 hrs"
            paper.grade.contains("5") -> "2 hrs"
            else -> "2.5 hrs"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.06f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false, onClick = {})
                    .testTag("paper_detail_dialog"),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag Handle Line at top
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )

                    // Top Info Row: Avatar + Title/Subtitle + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // The paper's own cover — drawn from its metadata when
                        // the API has no thumbnail image for it.
                        PaperCover(
                            paper = paper,
                            modifier = Modifier.size(52.dp),
                            corner = 14.dp
                        )

                        // Title & Subtitle Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = paper.title,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "Paper I · ${if (paper.hasMarkingScheme) "Marking scheme included" else "Answers included"}",
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Light Rounded Close Button (X)
                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 4 Stat Cards Row: YEAR, DURATION, PAGES, MEDIUM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // YEAR Stat Card
                        PaperStatCard(
                            label = "YEAR",
                            value = "${paper.year}",
                            isDark = false,
                            modifier = Modifier.weight(1f)
                        )

                        // DURATION Stat Card
                        PaperStatCard(
                            label = "DURATION",
                            value = durationHint,
                            isDark = false,
                            modifier = Modifier.weight(1.1f)
                        )

                        // PAGES Stat Card
                        PaperStatCard(
                            label = "PAGES",
                            value = "${paper.pages}",
                            isDark = false,
                            modifier = Modifier.weight(0.9f)
                        )

                        // MEDIUM Stat Card (Pastel Mint Highlight Box with Green Border)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(58.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "MEDIUM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = paper.medium,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Bottom Action Row: Black "Download PDF" Button (with Icon) + Vibrant Green "Ask AI" Button (with Spark Icon)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Solid ink Download PDF Button with Icon & Subtitle File Size.
                        // Sized by its own content (min 56dp) and given a generous
                        // share of the row, so the label never wraps or gets clipped
                        // on a 320dp phone the way a fixed 54dp box did.
                        Surface(
                            onClick = {
                                onDownload()
                                if (paper.pdfUrl.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paper.pdfUrl))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDownloaded) Color(0xFF059669) else Color(0xFF0F172A),
                            modifier = Modifier
                                .weight(1.35f)
                                .heightIn(min = 56.dp)
                                .testTag("paper_download_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = if (isDownloaded) "Downloaded" else "Download PDF",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = paper.fileSize,
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Green "Ask AI" button: same height as its neighbour, and
                        // only as wide as its label needs.
                        Surface(
                            onClick = onAskAi,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF059669),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .testTag("paper_ask_ai_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Ask AI",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperStatCard(
    label: String,
    value: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
