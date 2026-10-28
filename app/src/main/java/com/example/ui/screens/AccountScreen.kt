package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TheStudentPreferences
import com.example.data.model.UserProfile
import com.example.data.remote.AvatarPreparer
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.launch

private val AccountInk = Color(0xFF0F172A)
private val AccountMuted = Color(0xFF64748B)
private val AccountFaint = Color(0xFF94A3B8)
private val AccountLine = Color(0xFFE8EDF3)
private val AccountBg = Color(0xFFF6F8FB)
private val AccountGreen = Color(0xFF059669)
private val AccountDeep = Color(0xFF053B2B)

/** Mirror of the server rules, so a bad value never needs a round trip. */
private fun nameError(name: String): String? = when {
    name.trim().isEmpty() -> "Please enter your name."
    name.trim().length < 3 -> "Your name is too short."
    name.trim().length > 80 -> "Your name is too long."
    else -> null
}

private fun phoneError(phone: String): String? {
    val digits = phone.filter { it.isDigit() }
    return when {
        phone.trim().isEmpty() -> "Please enter your phone number."
        digits.length < 9 -> "That number looks too short."
        digits.length > 15 -> "That number looks too long."
        else -> null
    }
}

private fun institutionError(school: String): String? = when {
    school.trim().isEmpty() -> "Please enter your school."
    school.trim().length < 2 -> "That name is too short."
    school.trim().length > 120 -> "That name is too long."
    else -> null
}

/**
 * Account details — the page behind "Account" and "Account & profile".
 *
 * Everything a student can change about *themselves* is editable here: the photo,
 * the name, the phone number and the school. The academic profile (level, stream,
 * subjects) deliberately lives on the Profile tab instead — two screens editing
 * the same subjects was exactly the confusion this page replaces.
 *
 * Reading your own account must never sign you out, so nothing here touches the
 * session except the one explicit "Sign out" row.
 */
@Composable
fun AccountScreen(
    userProfile: UserProfile,
    isSaving: Boolean,
    saveError: String?,
    onBack: () -> Unit,
    onOpenSecurity: () -> Unit,
    onSave: (name: String, phone: String, institution: String, avatarJpeg: ByteArray?) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { TheStudentPreferences(context) }
    val role = remember { preferences.userRole }
    val expiresAt = remember { preferences.sessionExpiresAt }
    val isAdmin = role == TheStudentPreferences.ROLE_ADMIN

    // Draft values. Seeded from the profile once, then owned by the fields, so a
    // background profile refresh cannot yank the text out from under a keyboard.
    var name by remember { mutableStateOf(userProfile.name) }
    var phone by remember { mutableStateOf(userProfile.phone) }
    var school by remember { mutableStateOf(userProfile.institution) }

    var pickedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pickedBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var showSignOut by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val isDirty = pickedBytes != null ||
        name.trim() != userProfile.name.trim() ||
        phone.trim() != userProfile.phone.trim() ||
        school.trim() != userProfile.institution.trim()

    // Keep the fields in step with a fresh profile — but only while untouched.
    LaunchedEffect(userProfile.name) {
        if (!isDirty) name = userProfile.name
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            photoError = null
            when (val result = AvatarPreparer.prepare(context, uri)) {
                is AvatarPreparer.Result.Success -> {
                    pickedBytes = result.jpegBytes
                    pickedBitmap = runCatching {
                        android.graphics.BitmapFactory
                            .decodeByteArray(result.jpegBytes, 0, result.jpegBytes.size)
                            ?.asImageBitmap()
                    }.getOrNull()
                }
                is AvatarPreparer.Result.Failure -> photoError = result.message
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AccountBg)
            .testTag("account_screen")
    ) {
        // ── header ───────────────────────────────────────────────────────────
        // statusBarsPadding first: without it the back button and the title slide
        // under the device clock (the bug the security screen had).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
                    text = "Edit your details",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = AccountMuted
                )
            }
        }

        Column(
            modifier = Modifier
                // weight(1f), not fillMaxSize: the save bar below must keep its
                // own space instead of being pushed past the bottom edge.
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            // ── photo + identity ────────────────────────────────────────────
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF047857)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = pickedBitmap
                                    val remote = userProfile.avatarUrl
                                    when {
                                        bitmap != null -> androidx.compose.foundation.Image(
                                            bitmap = bitmap,
                                            contentDescription = "New profile photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        remote.isNotBlank() -> coil.compose.AsyncImage(
                                            model = remote,
                                            contentDescription = "Profile photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        else -> Text(
                                            text = accountInitials(name),
                                            fontFamily = MaisonNeueFontFamily,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            // Camera badge — the tap target for a new photo.
                            Surface(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("account_change_photo")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Change profile photo",
                                        tint = AccountGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { "Student" },
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 17.sp,
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
                                if (pickedBytes != null) AccountChip("New photo")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Tap the camera to change your photo.",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    if (photoError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = photoError!!,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── editable details ────────────────────────────────────────────
            AccountSectionTitle("Your details")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    EditField(
                        icon = Icons.Default.Person,
                        tint = AccountGreen,
                        label = "Full name",
                        value = name,
                        placeholder = "Your full name",
                        enabled = !isSaving,
                        keyboardType = KeyboardType.Text,
                        maxLength = 80,
                        onValueChange = { name = it; localError = null },
                        tag = "account_name"
                    )
                    Spacer(Modifier.height(12.dp))
                    EditField(
                        icon = Icons.Default.Phone,
                        tint = Color(0xFF14B8A6),
                        label = "Phone",
                        value = phone,
                        placeholder = "07X XXX XXXX",
                        enabled = !isSaving,
                        keyboardType = KeyboardType.Phone,
                        maxLength = 20,
                        onValueChange = { phone = it; localError = null },
                        tag = "account_phone"
                    )
                    Spacer(Modifier.height(12.dp))
                    EditField(
                        icon = Icons.Default.School,
                        tint = Color(0xFF0EA5E9),
                        label = "School",
                        value = school,
                        placeholder = "Your school or institute",
                        enabled = !isSaving,
                        keyboardType = KeyboardType.Text,
                        maxLength = 120,
                        onValueChange = { school = it; localError = null },
                        tag = "account_school"
                    )
                    Spacer(Modifier.height(12.dp))
                    // E-mail is the sign-in identity: readable, never editable here.
                    ReadOnlyField(
                        icon = Icons.Default.Email,
                        tint = Color(0xFF3B82F6),
                        label = "E-mail",
                        value = userProfile.email.ifBlank { "Not set" }
                    )
                    Spacer(Modifier.height(12.dp))
                    ReadOnlyField(
                        icon = Icons.Default.Lock,
                        tint = Color(0xFF8B5CF6),
                        label = "Password",
                        value = if (userProfile.hasPassword) "Set" else "Not set — sign in with Google"
                    )
                }
            }

            val visibleError = localError ?: saveError
            if (visibleError != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = visibleError,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("account_error")
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── session ────────────────────────────────────────────────────
            AccountSectionTitle("Session")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ReadOnlyRow(
                        icon = Icons.Default.CheckCircle,
                        tint = AccountGreen,
                        label = "Status",
                        value = "Signed in on this device"
                    )
                    Spacer(Modifier.height(10.dp))
                    ReadOnlyRow(
                        icon = Icons.Default.Shield,
                        tint = Color(0xFF3B82F6),
                        label = "Valid until",
                        value = expiresAt?.take(16) ?: "Active"
                    )
                    Spacer(Modifier.height(10.dp))
                    ReadOnlyRow(
                        icon = Icons.Default.Badge,
                        tint = Color(0xFFF59E0B),
                        label = "Role",
                        value = if (isAdmin) "Administrator" else "Student"
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

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
                icon = Icons.Default.Logout,
                tint = Color(0xFFDC2626),
                title = "Sign out",
                subtitle = "End this session on this phone",
                onClick = { showSignOut = true },
                tag = "account_sign_out",
                destructive = true
            )
        }

        // ── save bar (only while there is something to save) ────────────────
        if (isDirty) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    onClick = {
                        if (isSaving) return@Surface
                        val problem = nameError(name)
                            ?: phoneError(phone)
                            ?: institutionError(school)
                        if (problem != null) {
                            localError = problem
                            return@Surface
                        }
                        onSave(name.trim(), phone.trim(), school.trim(), pickedBytes)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSaving) AccountGreen.copy(alpha = 0.6f) else AccountGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("account_save")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(
                            text = if (isSaving) "Saving…" else "Save changes",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showSignOut) {
        SignOutConfirm(
            onDismiss = { showSignOut = false },
            onConfirm = {
                showSignOut = false
                onSignOut()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pieces
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = MaisonNeueFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = AccountFaint,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

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
private fun EditField(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    tag: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FieldIcon(icon, tint)
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            enabled = enabled,
            singleLine = true,
            label = {
                Text(
                    text = label,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = AccountMuted
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    color = AccountFaint
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccountInk
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccountGreen,
                unfocusedBorderColor = AccountLine,
                focusedContainerColor = Color(0xFFFAFBFC),
                unfocusedContainerColor = Color(0xFFFAFBFC),
                disabledContainerColor = Color(0xFFF8FAFC),
                disabledBorderColor = AccountLine
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
        )
    }
}

@Composable
private fun ReadOnlyField(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FieldIcon(icon, tint)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = AccountMuted
            )
            Text(
                text = value,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccountInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "Locked",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = AccountFaint
        )
    }
}

@Composable
private fun ReadOnlyRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FieldIcon(icon, tint)
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            color = AccountMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccountInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FieldIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
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
            FieldIcon(icon, tint)
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
