package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TheStudentPreferences
import com.example.data.model.UserProfile
import com.example.data.remote.AvatarPreparer
import com.example.ui.components.shimmerEffect
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AccountInk = Color(0xFF0F172A)
private val AccountMuted = Color(0xFF64748B)
private val AccountFaint = Color(0xFF94A3B8)
private val AccountLine = Color(0xFFE8EDF3)
private val AccountBg = Color(0xFFF6F8FB)
private val AccountGreen = Color(0xFF059669)
private val AccountDeep = Color(0xFF053B2B)
private val AccountDanger = Color(0xFFDC2626)

/** The three fields a student may edit on this page. */
private enum class EditField { Name, Phone, School }

private fun labelOf(field: EditField): String = when (field) {
    EditField.Name -> "Full name"
    EditField.Phone -> "Phone"
    EditField.School -> "School"
}

/** Mirror of the server rules, so a bad value never needs a round trip. */
private fun fieldError(field: EditField, value: String): String? {
    val text = value.trim()
    return when (field) {
        EditField.Name -> when {
            text.isEmpty() -> "Please enter your name."
            text.length < 3 -> "Your name is too short."
            text.length > 80 -> "Your name is too long."
            else -> null
        }
        EditField.Phone -> {
            val digits = text.filter { it.isDigit() }
            when {
                text.isEmpty() -> "Please enter your phone number."
                digits.length < 9 -> "That number looks too short."
                digits.length > 15 -> "That number looks too long."
                else -> null
            }
        }
        EditField.School -> when {
            text.isEmpty() -> "Please enter your school."
            text.length < 2 -> "That name is too short."
            text.length > 120 -> "That name is too long."
            else -> null
        }
    }
}

/**
 * Account details — the page behind "Account" and "Account & profile".
 *
 * Every row is read-only until its pencil is tapped: editing one field at a time
 * keeps the page calm and makes it obvious what changed. Saving shows the usual
 * spinner plus a shimmer skeleton of the page, then the record comes back fresh
 * and the page returns to the top.
 *
 * The academic profile (level, stream, subjects) deliberately lives on the Profile
 * tab instead — two screens editing the same subjects was the confusion this page
 * replaces. Reading your own account never touches the session.
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
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val latestProfile by rememberUpdatedState(userProfile)

    val preferences = remember { TheStudentPreferences(context) }
    val role = remember { preferences.userRole }
    val expiresAt = remember { preferences.sessionExpiresAt }
    val isAdmin = role == TheStudentPreferences.ROLE_ADMIN

    // Committed values. Seeded from the profile, then owned here so a background
    // refresh cannot yank text out from under a keyboard.
    var name by remember { mutableStateOf(userProfile.name) }
    var phone by remember { mutableStateOf(userProfile.phone) }
    var school by remember { mutableStateOf(userProfile.institution) }

    var editing by remember { mutableStateOf<EditField?>(null) }
    var draft by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    var pickedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pickedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var showSignOut by remember { mutableStateOf(false) }

    fun valueOf(field: EditField): String = when (field) {
        EditField.Name -> name
        EditField.Phone -> phone
        EditField.School -> school
    }

    fun commit(field: EditField, value: String) {
        when (field) {
            EditField.Name -> name = value
            EditField.Phone -> phone = value
            EditField.School -> school = value
        }
    }

    val isDirty = pickedBytes != null ||
        name.trim() != userProfile.name.trim() ||
        phone.trim() != userProfile.phone.trim() ||
        school.trim() != userProfile.institution.trim()

    // ── after a successful save: reseed, drop the photo draft, go to the top ──
    var wasSaving by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving, saveError) {
        if (isSaving) {
            wasSaving = true
            return@LaunchedEffect
        }
        if (wasSaving && saveError == null) {
            wasSaving = false
            // One frame for the freshly written profile to reach us, so the page
            // settles on the saved values instead of the old ones.
            delay(120)
            val fresh = latestProfile
            name = fresh.name
            phone = fresh.phone
            school = fresh.institution
            editing = null
            draft = ""
            pickedBytes = null
            pickedBitmap = null
            scrollState.animateScrollTo(0)
        }
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
        AccountHeader(onBack = onBack)

        // Saving swaps the whole body for the shimmer skeleton: the values on
        // screen are about to change, so showing the old ones invites a second
        // edit of something that is already on its way to the server.
        if (isSaving) {
            AccountSavingSkeleton(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp)
            ) {
                IdentityCard(
                    name = name,
                    email = userProfile.email,
                    avatarUrl = userProfile.avatarUrl,
                    pickedBitmap = pickedBitmap,
                    isAdmin = isAdmin,
                    photoError = photoError,
                    onChangePhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(Modifier.height(14.dp))

                AccountSectionTitle("Your details")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        EditField.entries.forEachIndexed { index, field ->
                            if (index > 0) FieldDivider()
                            val isEditing = editing == field
                            if (isEditing) {
                                EditingRow(
                                    field = field,
                                    draft = draft,
                                    error = localError,
                                    onDraftChange = {
                                        draft = it
                                        localError = null
                                    },
                                    onConfirm = {
                                        val problem = fieldError(field, draft)
                                        if (problem != null) {
                                            localError = problem
                                        } else {
                                            commit(field, draft.trim())
                                            editing = null
                                            draft = ""
                                            localError = null
                                            focusManager.clearFocus()
                                        }
                                    },
                                    onCancel = {
                                        editing = null
                                        draft = ""
                                        localError = null
                                        focusManager.clearFocus()
                                    }
                                )
                            } else {
                                DetailRow(
                                    field = field,
                                    value = valueOf(field),
                                    enabled = editing == null,
                                    onEdit = {
                                        localError = null
                                        draft = valueOf(field)
                                        editing = field
                                    }
                                )
                            }
                        }
                        FieldDivider()
                        ReadOnlyField(
                            icon = Icons.Default.Email,
                            tint = Color(0xFF3B82F6),
                            label = "E-mail",
                            value = userProfile.email.ifBlank { "Not set" }
                        )
                        FieldDivider()
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
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccountDanger,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("account_error")
                    )
                }

                Spacer(Modifier.height(14.dp))

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
                    tint = AccountDanger,
                    title = "Sign out",
                    subtitle = "End this session on this phone",
                    onClick = { showSignOut = true },
                    tag = "account_sign_out",
                    destructive = true
                )
            }
        }

        // ── save bar ────────────────────────────────────────────────────────
        if (isDirty || isSaving) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    onClick = {
                        if (isSaving) return@Surface
                        val problem = fieldError(EditField.Name, name)
                            ?: fieldError(EditField.Phone, phone)
                            ?: fieldError(EditField.School, school)
                        if (problem != null) {
                            localError = problem
                            return@Surface
                        }
                        focusManager.clearFocus()
                        editing = null
                        onSave(name.trim(), phone.trim(), school.trim(), pickedBytes)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSaving) AccountGreen.copy(alpha = 0.65f) else AccountGreen,
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
                                modifier = Modifier
                                    .size(15.dp)
                                    .testTag("account_save_spinner")
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(
                            text = if (isSaving) "Saving your details…" else "Save changes",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.5.sp,
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
//  Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountHeader(onBack: () -> Unit) {
    Row(
        // statusBarsPadding first: without it the back button and the title slide
        // under the device clock.
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
                text = "Your details, one field at a time",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                color = AccountMuted
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Identity
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdentityCard(
    name: String,
    email: String,
    avatarUrl: String,
    pickedBitmap: ImageBitmap?,
    isAdmin: Boolean,
    photoError: String?,
    onChangePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(AccountDeep, Color(0xFF075E45), AccountGreen)))
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
                            when {
                                pickedBitmap != null -> Image(
                                    bitmap = pickedBitmap,
                                    contentDescription = "New profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                avatarUrl.isNotBlank() -> coil.compose.AsyncImage(
                                    model = avatarUrl,
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
                    Surface(
                        onClick = onChangePhoto,
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
                        text = email.ifBlank { "No e-mail on this account" },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccountChip(if (isAdmin) "Administrator" else "Student")
                        if (pickedBitmap != null) AccountChip("New photo")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tap the camera to change your photo.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.72f)
            )
            if (photoError != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = photoError,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFDE68A)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Detail rows
// ─────────────────────────────────────────────────────────────────────────────

private fun iconOf(field: EditField): ImageVector = when (field) {
    EditField.Name -> Icons.Default.Person
    EditField.Phone -> Icons.Default.Phone
    EditField.School -> Icons.Default.School
}

private fun tintOf(field: EditField): Color = when (field) {
    EditField.Name -> AccountGreen
    EditField.Phone -> Color(0xFF14B8A6)
    EditField.School -> Color(0xFF0EA5E9)
}

/**
 * Read-only row. The pencil lives at the end of the row, so the value stays a
 * value until someone deliberately asks to change it.
 */
@Composable
private fun DetailRow(
    field: EditField,
    value: String,
    enabled: Boolean,
    onEdit: () -> Unit
) {
    val tag = when (field) {
        EditField.Name -> "name"
        EditField.Phone -> "phone"
        EditField.School -> "school"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        FieldIcon(iconOf(field), tintOf(field))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = labelOf(field),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = AccountMuted
            )
            Text(
                text = value.ifBlank { "Not set" },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (value.isBlank()) AccountFaint else AccountInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onEdit,
            enabled = enabled,
            shape = CircleShape,
            color = AccountGreen.copy(alpha = if (enabled) 0.10f else 0.05f),
            modifier = Modifier
                .size(30.dp)
                .testTag("account_edit_$tag")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit ${labelOf(field).lowercase()}",
                    tint = if (enabled) AccountGreen else AccountFaint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** The same row, opened for editing: field, then confirm / cancel. */
@Composable
private fun EditingRow(
    field: EditField,
    draft: String,
    error: String?,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val tag = when (field) {
        EditField.Name -> "name"
        EditField.Phone -> "phone"
        EditField.School -> "school"
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(field) { runCatching { focusRequester.requestFocus() } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        FieldIcon(iconOf(field), tintOf(field))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = labelOf(field),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccountGreen
            )
            Spacer(Modifier.height(3.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    val limit = when (field) {
                        EditField.Name -> 80
                        EditField.Phone -> 20
                        EditField.School -> 120
                    }
                    if (it.length <= limit) onDraftChange(it)
                },
                singleLine = true,
                placeholder = {
                    Text(
                        text = when (field) {
                            EditField.Name -> "Your full name"
                            EditField.Phone -> "07X XXX XXXX"
                            EditField.School -> "Your school or institute"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        color = AccountFaint
                    )
                },
                textStyle = TextStyle(
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccountInk
                ),
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (field) {
                        EditField.Phone -> KeyboardType.Phone
                        else -> KeyboardType.Text
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccountGreen,
                    unfocusedBorderColor = AccountLine,
                    focusedContainerColor = Color(0xFFFAFBFC),
                    unfocusedContainerColor = Color(0xFFFAFBFC)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("account_field_$tag")
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                onClick = onConfirm,
                shape = CircleShape,
                color = AccountGreen,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("account_confirm_$tag")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Done editing ${labelOf(field).lowercase()}",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Surface(
                onClick = onCancel,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(30.dp)
                    .testTag("account_cancel_$tag")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = AccountMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private val AccountHairline = Color(0xFFF1F5F9)

@Composable
private fun FieldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 14.dp)
            .height(1.dp)
            .background(AccountHairline)
    )
}

@Composable
private fun ReadOnlyField(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        FieldIcon(icon, tint)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = AccountMuted
            )
            Text(
                text = value,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccountInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "Locked",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.sp,
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
            fontSize = 12.5.sp,
            color = AccountMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.sp,
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
private fun AccountSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = MaisonNeueFontFamily,
        fontSize = 11.5.sp,
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
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
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
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (destructive) tint else AccountInk
                )
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
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

// ─────────────────────────────────────────────────────────────────────────────
//  Saving skeleton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shimmer that mirrors this page: the emerald identity block, the three detail
 * rows and the bottom save bar. It is deliberately the same silhouette as the
 * real content, so the save reads as the page reloading rather than as a blank.
 */
@Composable
private fun AccountSavingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("account_saving_skeleton")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(AccountDeep, AccountGreen)))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                )
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 130.dp, height = 15.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 180.dp, height = 11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shimmerEffect(shapeRadius = 11.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 9.dp)
                                .shimmerEffect(shapeRadius = 5.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(12.dp)
                                .shimmerEffect(shapeRadius = 6.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .shimmerEffect(shapeRadius = 15.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(11.dp).shimmerEffect(shapeRadius = 6.dp))
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(11.dp).shimmerEffect(shapeRadius = 6.dp))
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
                    fontSize = 13.5.sp,
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
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccountInk
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        color = AccountDanger,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("account_signout_confirm")
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
