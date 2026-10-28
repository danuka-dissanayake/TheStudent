package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TheStudentPreferences
import com.example.data.model.UserProfile
import com.example.ui.theme.MaisonNeueFontFamily

private val AccountInk = Color(0xFF0F172A)
private val AccountMuted = Color(0xFF64748B)
private val AccountFaint = Color(0xFF94A3B8)
private val AccountLine = Color(0xFFE8EDF3)
private val AccountBg = Color(0xFFF6F8FB)
private val AccountGreen = Color(0xFF059669)
private val AccountDeep = Color(0xFF053B2B)

/**
 * Account details — the page behind "Account" and "Account & profile".
 *
 * Reading your own account must never sign you out, so this screen is entirely
 * local: it shows what the server already told us (name, e-mail, phone, provider,
 * verification, session) and offers the two things a student actually came for —
 * manage the password / sessions, or sign out on purpose.
 */
@Composable
fun AccountScreen(
    userProfile: UserProfile,
    onBack: () -> Unit,
    onOpenSecurity: () -> Unit,
    onEditAcademic: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Read once: these are session facts, not state the screen can change.
    val preferences = remember { TheStudentPreferences(context) }
    val role = remember { preferences.userRole }
    val expiresAt = remember { preferences.sessionExpiresAt }
    val isAdmin = role == TheStudentPreferences.ROLE_ADMIN

    var confirmSignOut by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AccountBg)
            .testTag("account_screen")
    ) {
        // ── header ───────────────────────────────────────────────────────────
        // statusBarsPadding first: without it the back button and the title slide
        // under the device clock, which is exactly the bug the security screen had.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, AccountLine),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("account_back")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AccountInk,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Account",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccountInk
                )
                Text(
                    text = "Your details and session",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = AccountMuted
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp)
        ) {
            // ── identity ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AccountDeep, Color(0xFF075E45), AccountGreen)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF047857)),
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
                                    text = accountInitials(userProfile.name),
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 21.sp,
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userProfile.email.ifBlank { "No e-mail on this account" },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AccountChip(if (isAdmin) "Administrator" else "Student")
                            AccountChip(
                                when (userProfile.authProvider) {
                                    "google" -> "Google sign-in"
                                    else -> "Email sign-in"
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── details ──────────────────────────────────────────────────────
            AccountSection(title = "Details") {
                DetailRow(
                    icon = Icons.Default.Person,
                    tint = AccountGreen,
                    label = "Full name",
                    value = userProfile.name.ifBlank { "Not set" }
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Default.Email,
                    tint = Color(0xFF3B82F6),
                    label = "E-mail",
                    value = userProfile.email.ifBlank { "Not set" }
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Default.Phone,
                    tint = Color(0xFF14B8A6),
                    label = "Phone",
                    value = userProfile.phone.ifBlank { "Not added" }
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Default.Lock,
                    tint = Color(0xFF8B5CF6),
                    label = "Password",
                    value = if (userProfile.hasPassword) "Set" else "Not set — sign in with Google"
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Default.Badge,
                    tint = Color(0xFFF59E0B),
                    label = "Role",
                    value = if (isAdmin) "Administrator" else "Student"
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── studies ──────────────────────────────────────────────────────
            AccountSection(title = "Studies") {
                DetailRow(
                    icon = Icons.Default.School,
                    tint = Color(0xFF0EA5E9),
                    label = "Level",
                    value = when {
                        userProfile.academic.isAl -> "A/L"
                        userProfile.academic.isOl -> "O/L"
                        else -> "Not set"
                    }
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Default.MenuBook,
                    tint = Color(0xFF059669),
                    label = "Subjects",
                    value = userProfile.academic.subjectLabels()
                        .joinToString(" · ")
                        .ifBlank { "Not set" }
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── session ──────────────────────────────────────────────────────
            AccountSection(title = "Session") {
                DetailRow(
                    icon = Icons.Default.CheckCircle,
                    tint = AccountGreen,
                    label = "Status",
                    value = "Signed in on this device"
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Default.Shield,
                    tint = Color(0xFF3B82F6),
                    label = "Valid until",
                    value = expiresAt?.take(16) ?: "Active"
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── actions ──────────────────────────────────────────────────────
            AccountAction(
                icon = Icons.Default.Shield,
                tint = Color(0xFF3B82F6),
                title = "Password & devices",
                subtitle = "Change your password, sign out everywhere",
                onClick = onOpenSecurity,
                tag = "account_security"
            )
            Spacer(Modifier.height(10.dp))
            AccountAction(
                icon = Icons.Default.MenuBook,
                tint = AccountGreen,
                title = "Study details",
                subtitle = "Level, stream, subjects and medium",
                onClick = onEditAcademic,
                tag = "account_studies"
            )
            Spacer(Modifier.height(10.dp))
            AccountAction(
                icon = Icons.Default.Logout,
                tint = Color(0xFFDC2626),
                title = "Sign out",
                subtitle = "End this session on this phone",
                onClick = { confirmSignOut = true },
                tag = "account_sign_out",
                destructive = true
            )
        }
    }

    if (confirmSignOut) {
        SignOutConfirm(
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                onSignOut()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pieces
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AccountSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AccountFaint,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            color = AccountMuted,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccountInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 14.dp)
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}

@Composable
private fun AccountAction(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String,
    destructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = if (destructive) BorderStroke(1.dp, Color(0xFFFBD5D5)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (destructive) tint else AccountInk
                )
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = AccountMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!destructive) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AccountFaint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SignOutConfirm(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                    color = AccountInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your saved papers, downloads and chat history stay on this phone. " +
                        "You can sign back in any time.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = AccountMuted
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = AccountBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("account_signout_cancel")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Stay",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccountInk
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
                            .testTag("account_signout_confirm")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sign out",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
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

/** "Tharindu Perera" → "TP". */
private fun accountInitials(name: String): String {
    val letters = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    return letters.ifBlank { "S" }
}
