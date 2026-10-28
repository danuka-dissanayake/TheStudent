package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ProfileSetupSubmission
import com.example.data.model.UserProfile
import com.example.data.model.AcademicCatalog
import com.example.data.model.AcademicProfile
import com.example.data.remote.AvatarPreparer
import com.example.ui.components.StudentLoadingSpinner
import com.example.ui.components.TheStudentAppBackground
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.RedError
import kotlinx.coroutines.launch

/** Aspect ratio of `res/drawable-nodpi/thestudent_logo_full.png` (1959 × 541). */
private const val BRAND_LOGO_ASPECT = 3.6211f

/** Spacing used by every card so the column reads as one rhythm. */
private val SectionGap = 14.dp

/**
 * Mandatory profile-setup screen.
 * ==============================
 *
 * Shown straight after a successful sign-up / sign-in and on every cold start
 * until the server reports `profile_completed: true`. It collects what an email
 * sign-up or a Google account cannot provide: phone number, academic details,
 * school/stream, a profile photo, and (for Google-only accounts) a password.
 *
 * Layout notes
 * ------------
 * • Every card is `fillMaxWidth()` inside ONE column that owns the horizontal
 *   padding (16 dp) and the vertical rhythm ([SectionGap]). Cards never add
 *   their own outer margins, which is what stopped edges from bleeding into the
 *   neighbouring section.
 * • No `animateContentSize` on the cards: animating a card's height inside a
 *   scrollable column makes the next section overlap for a frame.
 * • Nothing is pre-filled. Values the server does not have are shown as
 *   placeholders, never as defaults such as "Physical Science" or a literal
 *   "null" — this screen asks, it does not assume.
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
    // Only the full name (from Google / the e-mail address) is pre-filled: it is
    // the one value we genuinely know. Everything else starts empty so the
    // placeholder is visible and the student makes a real choice.
    var fullName by remember(initialProfile.email) {
        // "Student" is the server's placeholder for an account without a name
        // (rare, Google always sends one) - treat it as "no value".
        mutableStateOf(
            initialProfile.name.cleanValue()
                .takeUnless { it.equals("Student", ignoreCase = true) }
                .orEmpty()
        )
    }
    var phone by remember(initialProfile.email) { mutableStateOf("") }
    // The whole academic form is collected here now, not just a curriculum chip
    // and a free-text stream: the stage decides the subjects, the exam year and
    // the university, and the matcher needs all of it. A/L is only the default
    // selection, not an assumption.
    var academic by remember(initialProfile.email) {
        mutableStateOf(
            AcademicProfile(examLevel = AcademicCatalog.LEVEL_AL)
        )
    }
    var institution by remember(initialProfile.email) { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val needsPassword = !initialProfile.hasPassword
    val existingAvatarUrl = initialProfile.avatarUrl.cleanValue()

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

    val academicError = if (academic.isComplete) null else academicTodo(academic)

    val institutionError = when {
        institution.isBlank() -> "Please enter your school or institution."
        institution.trim().length < 2 -> "That looks too short."
        institution.trim().length > 120 -> "Please use at most 120 characters."
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

    val hasPhoto = avatarBytes != null || existingAvatarUrl.isNotBlank()
    val photoErrorShown = photoError ?: if (!hasPhoto) "Add a profile photo to continue." else null

    val isFormValid = nameError == null && phoneError == null && academicError == null &&
        institutionError == null &&
        passwordError == null && confirmError == null &&
        photoErrorShown == null

    // ── completion progress ──────────────────────────────────────────────────
    val checks = listOf(
        nameError == null,
        phoneError == null,
        academicError == null,
        institutionError == null,
        hasPhoto,
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
                // The legacy columns are derived from the structured profile:
                // complete_profile still validates them, and the admin panel and
                // older builds still read them.
                targetExam = targetExamFor(academic),
                institution = institution.trim(),
                streamOrDegree = streamOrDegreeFor(academic),
                password = if (needsPassword) password else null,
                avatarJpeg = avatarBytes,
                academic = academic
            )
        )
    }

    // Layout contract
    // --------------
    // The "Finish setup" button is the LAST item inside the scroll container, so
    // it sits directly under the "Where you study" card and is only reachable by
    // scrolling - it is deliberately NOT pinned to the screen bottom. The header
    // stays fixed, the form scrolls, and the bottom padding is applied on the
    // scroll content (never on the container) so the button can leave the screen.
    // One shared background for the whole app (see TheStudentAppBackground).
    TheStudentAppBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // ── brand bar: the real logo artwork, no text drawn by the app ────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.thestudent_logo_full),
                    contentDescription = "TheStudent — Everything a Student Needs",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(38.dp)
                        .aspectRatio(BRAND_LOGO_ASPECT)
                        .testTag("profile_brand_logo")
                )
                Spacer(Modifier.weight(1f))
                if (onSignOut != null) {
                    TextButton(onClick = onSignOut, enabled = !isSaving) {
                        Text(
                            "Sign out",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            // ── scrollable form: header stays, everything else scrolls, and the
            //    submit button lives at the very end of this column ────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(SectionGap)
            ) {
                // avatar + heading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
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

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Finish setting up",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "A few details so we can personalise your papers, " +
                            "progress and study plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    if (submitted && photoErrorShown != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = photoErrorShown,
                            style = MaterialTheme.typography.bodySmall,
                            color = RedError,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (completed == checks.size) EmeraldContainer
                                else EmeraldPrimary.copy(alpha = 0.10f)
                            )
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    ) {
                        if (completed == checks.size) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (completed == checks.size) "Everything looks good"
                            else "$completed of ${checks.size} details added",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    }
                }

                // server error banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ErrorBanner(text = errorMessage.orEmpty())
                }

                // ── personal details ─────────────────────────────────────────
                ProfileSectionCard(
                    title = "Personal details",
                    subtitle = "Shown on your progress reports",
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

                // ── academic details ─────────────────────────────────────────
                // The full academic step, live in the setup form: stage, grade,
                // stream, subjects, medium and — for undergraduates — the
                // university and the year. Asking here means the student never
                // lands on a dashboard the matcher cannot read.
                ProfileSectionCard(
                    title = "Academic details",
                    subtitle = "What you study decides who can help you",
                    icon = Icons.Filled.School
                ) {
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

                    AcademicStageForm(
                        profile = academic,
                        onChange = { academic = it },
                        flat = true
                    )

                    if (submitted && academicError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = academicError,
                            style = MaterialTheme.typography.bodySmall,
                            color = RedError
                        )
                    }
                }

                // ── password (Google-only accounts) ──────────────────────────
                if (needsPassword) {
                    ProfileSectionCard(
                        title = "Create a password",
                        subtitle = "You signed in with Google — add a password so you can also " +
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

                // ── submit (end of the form, not pinned to the screen) ───────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    if (submitted && !isFormValid && !isSaving) {
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
                            .height(52.dp)
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
                            Text("Finish setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = EmeraldPrimary,
                        trackColor = EmeraldPrimary.copy(alpha = 0.15f)
                    )
                }

                // Clears the navigation bar when the button is scrolled to the end.
                Spacer(Modifier.navigationBarsPadding().height(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

/**
 * `null`, "null" and blank all mean "the server has no value". Showing them is
 * how a field ended up printing the word "null" — never let that reach the UI.
 */
private fun String?.cleanValue(): String {
    val value = this?.trim().orEmpty()
    return if (value.isEmpty() || value.equals("null", ignoreCase = true)) "" else value
}

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
                .size(136.dp)
                .clip(CircleShape)
                .background(ringBrush)
                .clickable(enabled = !isProcessing, onClick = onClick)
                .testTag("profile_avatar_picker"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        // Error ring drawn *inside* the disc so it can never
                        // repaint over the camera badge.
                        if (hasError) Modifier.border(2.dp, RedError, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessing -> StudentLoadingSpinner(size = 32.dp)

                    previewUri != null -> AsyncImage(
                        model = previewUri,
                        contentDescription = "Selected profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                    )

                    existingUrl.isNotBlank() -> AsyncImage(
                        model = existingUrl,
                        contentDescription = "Current profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                    )

                    // Empty state: the call to action lives *inside* the disc.
                    else -> Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Upload your\nprofile pic",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = EmeraldPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
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
            Spacer(Modifier.height(14.dp))
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
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    val errorText = error
    val supportingContent: (@Composable () -> Unit)? = if (errorText != null) {
        @Composable { Text(errorText, color = RedError) }
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

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        label = { Text(label) },
        placeholder = {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        },
        singleLine = true,
        isError = errorText != null,
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 16.dp),
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
                        if (index < score) color
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
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
