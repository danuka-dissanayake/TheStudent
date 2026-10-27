package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProfileSetupSubmission
import com.example.data.model.UserProfile
import com.example.data.remote.AvatarPreparer
import com.example.ui.components.StudentLoadingSpinner
import com.example.ui.components.TheStudentLogo
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.RedError
import kotlinx.coroutines.launch

/**
 * Mandatory profile-setup screen.
 * ==============================
 *
 * Shown straight after a successful sign-up / sign-in and on every cold start
 * until the server reports `profile_completed: true`. Its whole job is to
 * collect the details an email sign-up or a Google account cannot provide:
 *
 *   • full name           (pre-filled, still editable)
 *   • phone number        (normalised to +94… by the server)
 *   • curriculum          (O/L, A/L, University, Professional)
 *   • school / institution
 *   • stream, degree or programme
 *   • profile photo       (validated + re-encoded, never trusted as sent)
 *   • password            (Google-only accounts, so email sign-in also works)
 *
 * Everything is validated twice: here for instant feedback, and again on the
 * server, which is the only source of truth. The screen is deliberately the
 * only way forward — there is no skip button — but signing out is always one tap
 * away so nobody is trapped.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    initialProfile: UserProfile,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (ProfileSetupSubmission) -> Unit,
    onDismissError: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── form state ────────────────────────────────────────────────────────────
    var fullName by remember(initialProfile.email) {
        mutableStateOf(initialProfile.name.takeUnless { it == "Student" || it == "Student </>" }.orEmpty())
    }
    // Stored as E.164 (+94…). Shown as-is: the field accepts a local
    // 07X XXX XXXX number or the full +94 form, and the server normalises it.
    //
    // Every field is keyed on the account e-mail so a cached profile that
    // arrives a moment later (Room emits asynchronously) fills the form once,
    // without ever overwriting what the student has typed since.
    var phone by remember(initialProfile.email) { mutableStateOf(initialProfile.phone) }
    var targetExam by remember(initialProfile.email) {
        mutableStateOf(initialProfile.targetExam.ifBlank { "G.C.E. A/L" })
    }
    var institution by remember(initialProfile.email) { mutableStateOf(initialProfile.institution) }
    var streamOrDegree by remember(initialProfile.email) { mutableStateOf(initialProfile.streamOrDegree) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val needsPassword = !initialProfile.hasPassword
    val existingAvatarUrl = initialProfile.avatarUrl

    // ── photo picker (system Photo Picker; no storage permission needed) ─────
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingPhoto = true
            photoError = null
            onDismissError()
            when (val result = AvatarPreparer.prepare(context, uri)) {
                is AvatarPreparer.Result.Success -> {
                    avatarBytes = result.jpegBytes
                    previewUri = uri
                }
                is AvatarPreparer.Result.Failure -> {
                    avatarBytes = null
                    previewUri = null
                    photoError = result.message
                }
            }
            isProcessingPhoto = false
        }
    }

    // Block back: the profile step cannot be skipped.
    BackHandler(enabled = true) { /* consumed */ }

    // ── validation (mirrors the server, for instant feedback) ────────────────
    val nameError = when {
        fullName.isBlank() -> "Please enter your full name."
        fullName.trim().length < 3 -> "That name looks too short."
        fullName.trim().length > 80 -> "Please use at most 80 characters."
        !fullName.trim().all { it.isLetter() || it in " .'-’" } ->
            "Letters, spaces, apostrophes and hyphens only."
        else -> null
    }

    val phoneDigits = phone.filter { it.isDigit() }
    val phoneError = when {
        phone.isBlank() -> "Please enter your phone number."
        phone.trim().startsWith("+") && phoneDigits.length in 8..15 -> null
        phoneDigits.length == 10 && phoneDigits.startsWith("0") -> null
        phoneDigits.length == 11 && phoneDigits.startsWith("94") -> null
        else -> "Enter a valid number, e.g. 071 234 5678."
    }

    val institutionError = when {
        institution.isBlank() -> "Please enter your school or institution."
        institution.trim().length < 2 -> "That looks too short."
        institution.trim().length > 120 -> "Please use at most 120 characters."
        else -> null
    }

    val streamError = when {
        streamOrDegree.isBlank() -> "Please choose or type your stream."
        streamOrDegree.trim().length < 2 -> "That looks too short."
        streamOrDegree.trim().length > 150 -> "Please use at most 150 characters."
        else -> null
    }

    val passwordError = if (!needsPassword) null else when {
        password.isEmpty() -> "Choose a password."
        password.length < 8 -> "At least 8 characters."
        password.length > 72 -> "Maximum 72 characters."
        !password.any { it.isLetter() } || !password.any { it.isDigit() } ->
            "Include a letter and a number."
        else -> null
    }

    val confirmError = if (!needsPassword) null else when {
        confirmPassword.isEmpty() -> "Repeat your password."
        confirmPassword != password -> "Passwords do not match."
        else -> null
    }

    val photoErrorShown = photoError ?: if (avatarBytes == null && existingAvatarUrl.isBlank()) {
        "Add a profile photo to continue."
    } else null

    val isFormValid = nameError == null && phoneError == null &&
        institutionError == null && streamError == null &&
        passwordError == null && confirmError == null &&
        photoErrorShown == null

    // ── completion progress ──────────────────────────────────────────────────
    val checks = listOf(
        nameError == null,
        phoneError == null,
        institutionError == null,
        streamError == null,
        avatarBytes != null || existingAvatarUrl.isNotBlank(),
        !needsPassword || passwordError == null
    )
    val completed = checks.count { it }
    val progress by animateFloatAsState(
        targetValue = completed.toFloat() / checks.size,
        animationSpec = tween(420),
        label = "profile_progress"
    )

    fun submit() {
        submitted = true
        onDismissError()
        if (!isFormValid) {
            if (photoErrorShown != null) photoError = photoErrorShown
            return
        }
        onSubmit(
            ProfileSetupSubmission(
                fullName = fullName.trim(),
                phone = phone.trim(),
                targetExam = targetExam,
                institution = institution.trim(),
                streamOrDegree = streamOrDegree.trim(),
                password = if (needsPassword) password else null,
                avatarJpeg = avatarBytes
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient emerald glow (subtle, keeps the screen from feeling flat).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            EmeraldPrimary.copy(alpha = 0.16f),
                            EmeraldPrimary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TheStudentLogo(size = 32.dp)
                Spacer(Modifier.weight(1f))
                if (onSignOut != null) {
                    TextButton(onClick = onSignOut, enabled = !isSaving) {
                        Text(
                            "Sign out",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .imePadding()
            ) {
                // ── avatar + headline ────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarPicker(
                        previewUri = previewUri,
                        existingUrl = existingAvatarUrl,
                        isProcessing = isProcessingPhoto,
                        hasError = submitted && photoErrorShown != null,
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Finish setting up",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Just a few details so we can personalise your papers, " +
                            "progress and study plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    Spacer(Modifier.height(14.dp))

                    // progress pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldContainer.copy(alpha = if (completed == checks.size) 0.9f else 0.45f))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        if (completed == checks.size) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (completed == checks.size) {
                                "Everything looks good"
                            } else {
                                "$completed of ${checks.size} details added"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── server error banner ──────────────────────────────────────
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ErrorBanner(text = errorMessage.orEmpty())
                }
                AnimatedVisibility(visible = errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                }

                // ── personal details ─────────────────────────────────────────
                ProfileSectionCard(
                    title = "Personal details",
                    subtitle = "Shown on your certificates and progress reports",
                    icon = Icons.Filled.Badge
                ) {
                    ProfileField(
                        value = fullName,
                        onValueChange = { fullName = it.take(80) },
                        label = "Full name",
                        placeholder = "e.g. Danuka Dissanayake",
                        error = if (submitted) nameError else null,
                        keyboardType = KeyboardType.Text,
                        testTag = "profile_full_name"
                    )
                    Spacer(Modifier.height(12.dp))
                    ProfileField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter { it.isDigit() || it == '+' || it == ' ' }.take(18)
                        },
                        label = "Phone number",
                        placeholder = "071 234 5678",
                        error = if (submitted) phoneError else null,
                        keyboardType = KeyboardType.Phone,
                        testTag = "profile_phone"
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── study details ────────────────────────────────────────────
                ProfileSectionCard(
                    title = "Where you study",
                    subtitle = "We use this to show the right syllabus and papers",
                    icon = Icons.Filled.School
                ) {
                    Text(
                        text = "Curriculum",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    ExamSelector(
                        selected = targetExam,
                        onSelect = { exam ->
                            targetExam = exam
                            // Reset the stream when it does not belong to the new curriculum.
                            if (streamOrDegree !in streamsFor(exam) && streamOrDegree.isNotBlank()) {
                                streamOrDegree = ""
                            }
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                    ProfileField(
                        value = institution,
                        onValueChange = { institution = it.take(120) },
                        label = "School / university",
                        placeholder = "e.g. Ananda College, Colombo",
                        error = if (submitted) institutionError else null,
                        keyboardType = KeyboardType.Text,
                        testTag = "profile_institution"
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (targetExam == "University" || targetExam == "Professional") {
                            "Degree / programme"
                        } else {
                            "Stream"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        streamsFor(targetExam).forEach { option ->
                            FilterChip(
                                selected = streamOrDegree == option,
                                onClick = { streamOrDegree = option },
                                label = { Text(option, fontSize = 12.5.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = streamOrDegree == option,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    selectedBorderColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ProfileField(
                        value = streamOrDegree,
                        onValueChange = { streamOrDegree = it.take(150) },
                        label = "Or type it",
                        placeholder = "e.g. Physical Science",
                        error = if (submitted) streamError else null,
                        keyboardType = KeyboardType.Text,
                        testTag = "profile_stream"
                    )
                }

                // ── password (Google-only accounts) ──────────────────────────
                AnimatedVisibility(visible = needsPassword) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        ProfileSectionCard(
                            title = "Create a password",
                            subtitle = "You signed in with Google. Add a password so you can also " +
                                "sign in with your email if you ever lose access.",
                            icon = Icons.Filled.Lock
                        ) {
                            ProfileField(
                                value = password,
                                onValueChange = { password = it.take(72) },
                                label = "Password",
                                placeholder = "At least 8 characters",
                                error = if (submitted) passwordError else null,
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                testTag = "profile_password"
                            )
                            if (password.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                PasswordStrengthBar(password)
                            }
                            Spacer(Modifier.height(12.dp))
                            ProfileField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it.take(72) },
                                label = "Confirm password",
                                placeholder = "Repeat the password",
                                error = if (submitted) confirmError else null,
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                testTag = "profile_confirm_password"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── photo hint + privacy note ────────────────────────────────
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Your photo is checked and resized on your phone before upload, " +
                                "and only JPG, PNG or WEBP images under 5 MB are accepted. " +
                                "We never share your phone number.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            // ── sticky action bar ────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    AnimatedVisibility(visible = submitted && !isFormValid && !isSaving) {
                        Text(
                            text = "Please fix the highlighted fields above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RedError,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = { submit() },
                        enabled = !isSaving && !isProcessingPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("profile_finish_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = EmeraldPrimary.copy(alpha = 0.45f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Saving your profile…", fontWeight = FontWeight.SemiBold)
                        } else if (isProcessingPhoto) {
                            Text("Preparing photo…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(
                                "Finish setup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(CircleShape),
                            color = EmeraldPrimary,
                            trackColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

private fun streamsFor(exam: String): List<String> = when (exam) {
    "G.C.E. O/L" -> listOf("Grade 10", "Grade 11", "Commerce", "Arts", "Science")
    "Primary" -> listOf("Grade 1-5", "Grade 6-9")
    "University" -> listOf(
        "Engineering", "IT / Computing", "Medicine", "Science",
        "Business / Management", "Arts", "Law", "Other"
    )
    "Professional" -> listOf("BIT", "BCS", "CIMA", "ACCA", "Other")
    else -> listOf(
        "Physical Science", "Biological Science", "Commerce",
        "Arts", "Technology", "Common"
    )
}

private val examOptions = listOf(
    Triple("G.C.E. O/L", "Grade 10–11", Icons.Filled.School),
    Triple("G.C.E. A/L", "Grade 12–13", Icons.Filled.School),
    Triple("University", "Undergraduate", Icons.Filled.AccountCircle),
    Triple("Professional", "BIT · BCS · CIMA", Icons.Filled.Badge)
)

@Composable
private fun AvatarPicker(
    previewUri: Uri?,
    existingUrl: String,
    isProcessing: Boolean,
    hasError: Boolean,
    onClick: () -> Unit
) {
    val ringBrush = remember {
        Brush.linearGradient(listOf(EmeraldPrimary, LimeAccent, EmeraldDark))
    }
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(ringBrush)
                .clickable(enabled = !isProcessing, onClick = onClick)
                .testTag("profile_avatar_picker"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessing -> StudentLoadingSpinner(size = 34.dp)

                    previewUri != null -> AsyncImage(
                        model = previewUri,
                        contentDescription = "Selected profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                    )

                    existingUrl.isNotBlank() -> AsyncImage(
                        model = existingUrl,
                        contentDescription = "Current profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                    )

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            if (hasError) {
                // Thin red ring communicates the error without hiding the avatar.
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .border(3.dp, RedError, CircleShape)
                )
            }
        }

        // camera badge
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RedError.copy(alpha = 0.10f))
            .border(1.dp, RedError.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = RedError,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(19.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    keyboardType: KeyboardType,
    testTag: String,
    leadingText: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    val showError = error != null

    val leadingLabel = leadingText
    val leadingContent: (@Composable () -> Unit)? = if (leadingLabel != null) {
        @Composable { Text(leadingLabel, fontWeight = FontWeight.SemiBold, color = EmeraldDark) }
    } else {
        null
    }

    val trailingContent: (@Composable () -> Unit)? = if (isPassword && onTogglePasswordVisibility != null) {
        @Composable {
            Icon(
                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onTogglePasswordVisibility() }
            )
        }
    } else {
        null
    }

    val errorText = error
    val supportingContent: (@Composable () -> Unit)? = if (showError && errorText != null) {
        @Composable { Text(errorText, color = RedError) }
    } else {
        null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
        singleLine = true,
        isError = showError,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        leadingIcon = leadingContent,
        trailingIcon = trailingContent,
        supportingText = supportingContent,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldPrimary,
            focusedLabelColor = EmeraldPrimary,
            cursorColor = EmeraldPrimary,
            errorBorderColor = RedError
        )
    )
}

@Composable
private fun ExamSelector(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        examOptions.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (exam, hint, icon) ->
                    val isSelected = selected == exam
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) EmeraldPrimary.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(exam) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Check else icon,
                                contentDescription = null,
                                tint = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = exam,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    if (password.any { it.isUpperCase() }) score++

    val (label, color) = when {
        score <= 2 -> "Weak" to RedError
        score == 3 -> "Fair" to Color(0xFFF59E0B)
        score == 4 -> "Good" to Color(0xFF10B981)
        else -> "Strong" to EmeraldDark
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < score) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
