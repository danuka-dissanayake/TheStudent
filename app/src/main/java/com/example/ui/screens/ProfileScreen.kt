package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.LearningActivityEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SavedPaperEntity
import com.example.data.model.ChatMyRatings
import com.example.data.model.ExpTier
import com.example.data.model.UserExp
import com.example.data.model.UserProfile
import com.example.ui.components.ExpMedalsCard
import com.example.ui.components.TierMedallion
import com.example.ui.components.formatExp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Palette — shared by every card on this screen
// ─────────────────────────────────────────────────────────────────────────────
private val ProfInk = Color(0xFF0F172A)
private val ProfMuted = Color(0xFF64748B)
private val ProfFaint = Color(0xFF94A3B8)
private val ProfHairline = Color(0xFFE8EDF3)
private val ProfBg = Color(0xFFF6F8FB)
private val ProfDeep = Color(0xFF053B2B)
private val ProfMint = Color(0xFFECFDF5)
private val ProfAmber = Color(0xFFFBBF24)

/**
 * The student's own profile.
 *
 * Four questions, answered in order: who am I (identity + level), what have I
 * done (stats, badges, reputation), what am I studying (the academic profile the
 * matching engine reads), and what have I saved (papers, courses, history). Then
 * the account controls, which are the only destructive things on the screen and
 * therefore live at the bottom, behind a confirmation.
 *
 * Everything shown is real: no placeholder streaks, no invented school names, no
 * "0d 🔥". A value the student has not set renders as a prompt to set it, never as
 * a made-up number.
 */
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    exp: UserExp,
    myRatings: ChatMyRatings,
    isRatingsLoading: Boolean,
    savedPapers: List<SavedPaperEntity>,
    savedCourses: List<SavedCourseEntity>,
    recentActivities: List<LearningActivityEntity>,
    isDarkTheme: Boolean,
    isAdmin: Boolean,
    onToggleTheme: () -> Unit,
    onOpenAccount: () -> Unit,
    onBack: () -> Unit = {},
    onPaperClick: (String) -> Unit,
    onCourseClick: (String) -> Unit = {},
    onEditAcademic: () -> Unit,
    onOpenExp: () -> Unit = {},
    onOpenProgress: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onSignOut: () -> Unit = {},
    accountEmail: String = "",
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var confirmSignOut by remember { mutableStateOf(false) }

    // A Column, not a Box: a Box would let the list measure itself against an
    // infinite height and crash. The dialog below is composed by the caller's
    // window, so it does not need the Box to sit in.
    Column(modifier = modifier.fillMaxSize().background(ProfBg)) {
        // Pinned above the list, not an item in it: the title and the two buttons
        // must stay put while the record scrolls under them.
        ProfileHeader(
            onBack = onBack,
            onOpenSecurity = onOpenSecurity,
            onOpenAccount = onOpenAccount
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("profile_screen_content"),
            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "hero") {
                IdentityHero(
                    userProfile = userProfile,
                    exp = exp,
                    isAdmin = isAdmin,
                    onOpenExp = onOpenExp,
                    onOpenAccount = onOpenAccount
                )
            }

            item(key = "stats") {
                StatsStrip(
                    profile = userProfile,
                    savedPapers = savedPapers.size,
                    savedCourses = savedCourses.size,
                    exp = exp
                )
            }

            item(key = "level") {
                // Same 16dp gutter as the cards above and below it.
                ExpMedalsCard(
                    exp = exp,
                    onOpenExp = onOpenExp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item(key = "reputation") {
                ReputationCard(
                    myRatings = myRatings,
                    isLoading = isRatingsLoading
                )
            }

            item(key = "academic") {
                AcademicCard(profile = userProfile, onEdit = onEditAcademic)
            }

            item(key = "library_title") {
                SectionHeading(
                    title = "My library",
                    trailing = "${savedPapers.size + savedCourses.size} items"
                )
            }

            item(key = "library_tabs") {
                LibraryTabs(
                    selected = selectedTab,
                    papers = savedPapers.size,
                    courses = savedCourses.size,
                    history = recentActivities.size,
                    onSelect = { selectedTab = it }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (savedPapers.isEmpty()) {
                        item(key = "papers_empty") {
                            EmptyLibraryCard(
                                icon = Icons.Default.Description,
                                title = "Nothing saved yet",
                                body = "Save a paper from the Papers tab and it lands here, " +
                                    "ready offline."
                            )
                        }
                    } else {
                        items(savedPapers, key = { "paper-${it.id}" }) { paper ->
                            SavedPaperRow(paper = paper, onClick = { onPaperClick(paper.id) })
                        }
                    }
                }

                1 -> {
                    if (savedCourses.isEmpty()) {
                        item(key = "courses_empty") {
                            EmptyLibraryCard(
                                icon = Icons.Default.School,
                                title = "No courses yet",
                                body = "Enrol in a course from the Learn tab to track your progress here."
                            )
                        }
                    } else {
                        items(savedCourses, key = { "course-${it.id}" }) { course ->
                            SavedCourseRow(course = course, onClick = { onCourseClick(course.id) })
                        }
                    }
                }

                else -> {
                    if (recentActivities.isEmpty()) {
                        item(key = "history_empty") {
                            EmptyLibraryCard(
                                icon = Icons.Default.History,
                                title = "No activity yet",
                                body = "Papers you open, lessons you finish and questions you ask " +
                                    "are listed here."
                            )
                        }
                    } else {
                        items(recentActivities, key = { "activity-${it.id}" }) { activity ->
                            ActivityRow(activity)
                        }
                    }
                }
            }

            item(key = "settings_title") {
                SectionHeading(title = "Account", trailing = null)
            }

            item(key = "settings") {
                AccountCard(
                    userProfile = userProfile,
                    accountEmail = accountEmail,
                    isDarkTheme = isDarkTheme,
                    isAdmin = isAdmin,
                    onToggleTheme = onToggleTheme,
                    onOpenSecurity = onOpenSecurity,
                    onOpenAdmin = onOpenAdmin,
                    onOpenAccount = onOpenAccount,
                    onOpenProgress = onOpenProgress,
                    onSignOut = { confirmSignOut = true }
                )
            }

        }
    }

    // `Dialog` draws in its own window, so it is composed next to the column
    // rather than inside it — a dialog nested in a sized parent still renders,
    // but this keeps the layout tree honest.
    if (confirmSignOut) {
        SignOutDialog(
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                onSignOut()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    onBack: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProfBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleGhostButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = "Back",
                onClick = onBack,
                tag = "profile_back"
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Profile",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfInk,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Your record",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = ProfMuted
                )
            }
            CircleGhostButton(
                icon = Icons.Default.Shield,
                description = "Security and sessions",
                onClick = onOpenSecurity,
                tag = "profile_security"
            )
            Spacer(Modifier.width(8.dp))
            CircleGhostButton(
                icon = Icons.Default.ManageAccounts,
                description = "Account settings",
                onClick = onOpenAccount,
                tag = "profile_account"
            )
        }
        // Hairline: without it the cards slide under the title with nothing
        // separating them, which reads as a rendering glitch.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ProfHairline)
        )
    }
}

@Composable
private fun CircleGhostButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, ProfHairline),
        modifier = Modifier.size(40.dp).testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = ProfInk, modifier = Modifier.size(19.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Identity hero
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdentityHero(
    userProfile: UserProfile,
    exp: UserExp,
    isAdmin: Boolean,
    onOpenExp: () -> Unit,
    onOpenAccount: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ProfDeep, Color(0xFF075E45), EmeraldPrimary)
                )
            )
            .padding(18.dp)
            .testTag("user_profile_card")
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with a white ring, so a photo never bleeds into the card.
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(EmeraldDark)
                            .testTag("profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatar = userProfile.avatarUrl
                        if (avatar.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = avatar,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = initialsOf(userProfile.name),
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userProfile.name.ifBlank { "Student" },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = userProfile.email.ifBlank { "No e-mail on this account" },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeroChip(
                            text = "Level ${exp.tier.level} · ${exp.tier.name}",
                            onClick = onOpenExp
                        )
                        if (isAdmin) {
                            Spacer(Modifier.width(6.dp))
                            HeroChip(text = "Admin")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // The two things this page is actually *for*: manage the account, and
            // look at the record.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroAction(
                    icon = Icons.Default.ManageAccounts,
                    label = "Account",
                    onClick = onOpenAccount,
                    modifier = Modifier.weight(1f)
                )
                HeroAction(
                    icon = Icons.Default.MilitaryTech,
                    label = "My EXP",
                    onClick = onOpenExp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroChip(text: String, onClick: (() -> Unit)? = null) {
    val base = Modifier
        .clip(RoundedCornerShape(50))
        .background(Color.White.copy(alpha = 0.16f))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 9.dp, vertical = 4.dp)
    Box(modifier = base) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun HeroAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stats
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsStrip(
    profile: UserProfile,
    savedPapers: Int,
    savedCourses: Int,
    exp: UserExp
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell("${profile.streakDays}", "Day streak", Modifier.weight(1f))
            StatDivider()
            StatCell("${profile.totalPapersSolved}", "Papers solved", Modifier.weight(1f))
            StatDivider()
            StatCell(formatExp(exp.total), "Total EXP", Modifier.weight(1f))
            StatDivider()
            StatCell("${savedPapers + savedCourses}", "Saved", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ProfInk
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            color = ProfMuted,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(ProfHairline)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Level + badges
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
//  Reputation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReputationCard(myRatings: ChatMyRatings, isLoading: Boolean) {
    val summary = myRatings.summary

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("profile_reputation_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reputation",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfInk,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = EmeraldPrimary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (!summary.hasRatings) {
                // A new student is "New", never "0.0". The prompt says how the
                // number appears rather than pretending one exists.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ProfMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = ProfFaint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "No ratings yet",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfInk
                        )
                        Text(
                            text = "Help someone close a request and they can rate you.",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            color = ProfMuted
                        )
                    }
                }
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary.averageLine,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfInk,
                    letterSpacing = (-0.8).sp
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val full = summary.average?.let { kotlin.math.round(it).toInt() } ?: 0
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < full) ProfAmber else Color(0xFFE2E8F0),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${summary.count} rating" + (if (summary.count == 1) "" else "s") +
                            " from students",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = ProfMuted
                    )
                }
            }

            if (summary.breakdown.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                for (star in 5 downTo 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "$star",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfFaint,
                            modifier = Modifier.width(10.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(summary.shareOf(star))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(ProfAmber)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${summary.breakdown[star] ?: 0}",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            color = ProfFaint,
                            modifier = Modifier.width(16.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // The private notes: the only place in the app they are ever shown,
            // and only to the student they were written for.
            myRatings.notes.take(2).forEach { note ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "“${note.note}”",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            color = ProfInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = buildString {
                                append("— ${note.from}")
                                if (note.subjectLabel.isNotBlank()) append(" · ${note.subjectLabel}")
                                if (note.createdEpoch > 0) append(" · ${shortDate(note.createdEpoch)}")
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            color = ProfMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Academic
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AcademicCard(profile: UserProfile, onEdit: () -> Unit) {
    val academic = profile.academic
    val subjects = academic.subjectLabels()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("profile_academic_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Studies",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProfInk
                    )
                    Text(
                        text = if (academic.isComplete) {
                            "What the help matching reads"
                        } else {
                            "Finish this to get matched with the right students"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = if (academic.isComplete) ProfMuted else EmeraldDark
                    )
                }
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(50),
                    color = if (academic.isComplete) ProfMint else EmeraldPrimary,
                    modifier = Modifier.testTag("profile_edit_academic")
                ) {
                    Text(
                        text = if (academic.isComplete) "Edit" else "Complete",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (academic.isComplete) EmeraldDark else Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!academic.isComplete && subjects.isEmpty()) {
                Text(
                    text = "Pick your stage — Grades 6–9, O/L, A/L or university — and the " +
                        "details that go with it, so the chat can introduce you to someone " +
                        "who already sits the same papers.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = ProfMuted
                )
            } else {
                InfoLine(
                    label = "Stage",
                    value = academic.stageLabel.ifBlank { "—" }
                )
                when {
                    academic.isUni -> {
                        InfoLine(
                            label = "University",
                            value = academic.university.ifBlank { "—" }
                        )
                        InfoLine(
                            label = "Field",
                            value = academic.programme.ifBlank { "—" }
                        )
                        InfoLine(
                            label = "Year",
                            value = academic.studyYear?.let { "Year $it" } ?: "—"
                        )
                    }
                    academic.isAl -> {
                        InfoLine(label = "Stream", value = academic.streamCode.ifBlank { "—" })
                        InfoLine(
                            label = "Grade",
                            value = academic.studyYear?.let { "Grade $it" } ?: "—"
                        )
                        InfoLine(
                            label = "Exam year",
                            value = academic.examYear?.toString() ?: "—"
                        )
                        InfoLine(label = "Medium", value = academic.medium.ifBlank { "—" })
                    }
                    academic.isOl -> {
                        InfoLine(
                            label = "Grade",
                            value = academic.studyYear?.let { "Grade $it" } ?: "—"
                        )
                        InfoLine(
                            label = "Exam year",
                            value = academic.examYear?.toString() ?: "—"
                        )
                        InfoLine(label = "Medium", value = academic.medium.ifBlank { "—" })
                    }
                    else -> {
                        InfoLine(
                            label = "Grade",
                            value = academic.studyYear?.let { "Grade $it" } ?: "—"
                        )
                        InfoLine(label = "Medium", value = academic.medium.ifBlank { "—" })
                    }
                }

                if (subjects.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Subjects",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfFaint,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    // Chips, one per subject: they are three separate things and
                    // should not read as one sentence.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEachIndexed { index, subject ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("profile_subject_$index")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(EmeraldPrimary, CircleShape)
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    text = subject,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ProfInk
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
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = ProfMuted,
            modifier = Modifier.width(84.dp)
        )
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Library
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeading(title: String, trailing: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp)
    ) {
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ProfInk,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = ProfMuted
            )
        }
    }
}

@Composable
private fun LibraryTabs(
    selected: Int,
    papers: Int,
    courses: Int,
    history: Int,
    onSelect: (Int) -> Unit
) {
    val labels = listOf("Saved" to papers, "Courses" to courses, "History" to history)

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFEDF2F7),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, (label, count) ->
                val isSelected = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 9.dp)
                        .testTag("profile_tab_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 0) "$label · $count" else label,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ProfInk else ProfMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPaperRow(paper: SavedPaperEntity, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            IconTile(icon = Icons.Default.Description, tint = EmeraldPrimary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${paper.subject} · ${paper.examination} · ${paper.year}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = ProfMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (paper.isDownloaded) {
                Pill(text = "Offline", bg = Color(0xFFE9F8F0), fg = EmeraldDark)
            }
        }
    }
}

@Composable
private fun SavedCourseRow(course: SavedCourseEntity, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = Icons.Default.School, tint = Color(0xFF0EA5E9))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${course.instructor} · ${course.institution}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = ProfMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${course.progressPercent}%",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEAF0F5))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((course.progressPercent / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldPrimary)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${course.completedLessons} of ${course.totalLessons} lessons",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = ProfFaint
            )
        }
    }
}

@Composable
private fun ActivityRow(activity: LearningActivityEntity) {
    val (icon, tint) = when (activity.category) {
        "Past Paper" -> Icons.Default.Description to EmeraldPrimary
        "Course" -> Icons.Default.School to Color(0xFF0EA5E9)
        "AI Tutor" -> Icons.Default.AutoAwesome to Color(0xFF8B5CF6)
        else -> Icons.Default.Translate to Color(0xFF14B8A6)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            IconTile(icon = icon, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activity.description,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = ProfMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = activity.scoreOrDuration,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
                Text(
                    text = shortDate(activity.timestamp / 1000L),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = ProfFaint
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(icon: ImageVector, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ProfBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ProfFaint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = ProfInk
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                color = ProfMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Account
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountCard(
    userProfile: UserProfile,
    accountEmail: String,
    isDarkTheme: Boolean,
    isAdmin: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenProgress: () -> Unit = {},
    onSignOut: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingRow(
                icon = Icons.Default.Insights,
                tint = EmeraldPrimary,
                title = "Your progress",
                subtitle = "Subject accuracy, streak and what to practise next",
                onClick = onOpenProgress,
                tag = "profile_row_progress"
            )
            SettingDivider()
            SettingRow(
                icon = Icons.Default.ManageAccounts,
                tint = EmeraldPrimary,
                title = "Account & profile",
                subtitle = accountEmail.ifBlank { userProfile.email },
                onClick = onOpenAccount,
                tag = "profile_row_account"
            )
            SettingDivider()
            SettingRow(
                icon = Icons.Default.Shield,
                tint = Color(0xFF3B82F6),
                title = "Security & sessions",
                subtitle = "Password, devices, sign out everywhere",
                onClick = onOpenSecurity,
                tag = "profile_row_security"
            )
            SettingDivider()
            SettingRow(
                icon = Icons.Default.MilitaryTech,
                tint = Color(0xFFF59E0B),
                title = "Appearance",
                subtitle = "Dark mode",
                onClick = onToggleTheme,
                tag = "profile_row_theme",
                trailing = {
                    Text(
                        text = if (isDarkTheme) "On" else "Off",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) EmeraldPrimary else ProfMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkTheme) ProfMint else ProfBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            )
            if (isAdmin) {
                SettingDivider()
                SettingRow(
                    icon = Icons.Default.AdminPanelSettings,
                    tint = Color(0xFF8B5CF6),
                    title = "Admin panel",
                    subtitle = "Manage papers, students and reports",
                    onClick = onOpenAdmin,
                    tag = "profile_row_admin"
                )
            }
            SettingDivider()
            SettingRow(
                icon = Icons.Default.Logout,
                tint = Color(0xFFDC2626),
                title = "Sign out",
                subtitle = "You can sign back in any time",
                onClick = onSignOut,
                tag = "profile_row_signout",
                destructive = true
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag(tag)
    ) {
        IconTile(icon = icon, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (destructive) tint else ProfInk
            )
            Text(
                text = subtitle,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = ProfMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ProfFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 62.dp, end = 14.dp)
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}

/**
 * Sign out, behind a confirmation.
 *
 * It is the only action here that can lose someone their session, so it asks —
 * and the safe option is the wide, quiet one.
 */
@Composable
private fun SignOutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties()) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Sign out?",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your saved papers, downloads and chat history stay on this phone. " +
                        "You can sign back in any time.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    color = ProfMuted
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = ProfBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("profile_signout_cancel")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Stay",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfInk
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("profile_signout_confirm")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sign out",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** "Tharindu Perera" → "TP"; a single word gives one letter. */
private fun initialsOf(name: String): String {
    val letters = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    return letters.ifBlank { "S" }
}

private fun shortDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val now = System.currentTimeMillis() / 1000L
    val diff = now - epochSeconds
    return when {
        diff < 60L -> "Just now"
        diff < 3600L -> "${diff / 60L}m ago"
        diff < 86_400L -> "${diff / 3600L}h ago"
        diff < 172_800L -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochSeconds * 1000L))
    }
}
