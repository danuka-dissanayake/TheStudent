package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.*

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    VERIFY_OTP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    initialIsRegister: Boolean = false,
    onAuthSuccess: (UserProfile) -> Unit,
    onContinueAsGuest: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var authMode by remember {
        mutableStateOf(if (initialIsRegister) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
    }

    // Form inputs
    var firstName by remember { mutableStateOf("Jonathan") }
    var lastName by remember { mutableStateOf("Smith") }
    var email by remember { mutableStateOf("jonathansmith76@gmail.com") }
    var password by remember { mutableStateOf("••••••••••••") }
    var confirmPassword by remember { mutableStateOf("••••••••••••") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Target stream/curriculum for Sri Lanka learners
    var selectedStream by remember { mutableStateOf("G.C.E. A/L (Physical Science)") }
    var isStreamMenuOpen by remember { mutableStateOf(false) }
    val streamList = listOf(
        "G.C.E. A/L (Physical Science)",
        "G.C.E. A/L (Biological Science)",
        "G.C.E. A/L (Commerce & Arts)",
        "G.C.E. A/L (Technology Stream)",
        "G.C.E. O/L Examination",
        "Undergraduate (BSc / BEng / BIT)",
        "Postgraduate (MSc / MBA)"
    )

    // OTP verification code state (5 digits)
    var otpDigits by remember { mutableStateOf(listOf("7", "6", "9", "5", "")) }
    val activeOtpIndex = otpDigits.indexOfFirst { it.isEmpty() }.let { if (it == -1) 4 else it }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen"),
        containerColor = PureLightGreenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureLightGreenBg)
        ) {
            // Top Header section with pure light green
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular back button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            if (authMode == AuthMode.VERIFY_OTP) {
                                authMode = AuthMode.SIGN_IN
                            } else if (onBack != null) {
                                onBack()
                            } else {
                                onContinueAsGuest()
                            }
                        }
                        .testTag("auth_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureLightGreenButton,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Dynamic Titles
                when (authMode) {
                    AuthMode.SIGN_IN -> {
                        Text(
                            text = "Go ahead and complete your\naccount and setup",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = "Create your account and simplify your workflow instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                    AuthMode.SIGN_UP -> {
                        Text(
                            text = "Sign up now to access your\npersonal account",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = "Sign up to access your account and exclusive features.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                    AuthMode.VERIFY_OTP -> {
                        Text(
                            text = "Use phone or email to reset your\npassword",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = "Enter your email or phone to reset your password easily.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main White Sheet with generous top rounded corners
            Surface(
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(16.dp, shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Redesigned modern toggle pill switcher
                    if (authMode != AuthMode.VERIFY_OTP) {
                        ModernSegmentedToggle(
                            isSignIn = authMode == AuthMode.SIGN_IN,
                            onSelectSignIn = {
                                authMode = AuthMode.SIGN_IN
                                errorMessage = null
                            },
                            onSelectSignUp = {
                                authMode = AuthMode.SIGN_UP
                                errorMessage = null
                            }
                        )
                    }

                    // Error message banner if any
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RedError.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = RedError, modifier = Modifier.size(18.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RedError,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Screen Body Content
                    AnimatedContent(
                        targetState = authMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "auth_screen_transition"
                    ) { mode ->
                        when (mode) {
                            AuthMode.SIGN_IN -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Email input
                                    CleanLabeledTextField(
                                        label = "Email",
                                        value = email,
                                        placeholder = "jonathansmith76@gmail.com",
                                        keyboardType = KeyboardType.Email,
                                        onValueChange = {
                                            email = it
                                            errorMessage = null
                                        },
                                        testTag = "signin_email_field"
                                    )

                                    // Password input
                                    CleanLabeledTextField(
                                        label = "Password",
                                        value = password,
                                        placeholder = "Enter your password",
                                        keyboardType = KeyboardType.Password,
                                        isPassword = true,
                                        passwordVisible = passwordVisible,
                                        onTogglePassword = { passwordVisible = !passwordVisible },
                                        onValueChange = {
                                            password = it
                                            errorMessage = null
                                        },
                                        testTag = "signin_password_field"
                                    )

                                    // Remember me & Forget password row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                                        ) {
                                            Checkbox(
                                                checked = rememberMe,
                                                onCheckedChange = { rememberMe = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = PureLightGreenButton,
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Text(
                                                text = "Remember me",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "Forget password?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RedError,
                                            modifier = Modifier
                                                .clickable { authMode = AuthMode.VERIFY_OTP }
                                                .testTag("forget_password_button")
                                        )
                                    }

                                    // Primary Sign In Button
                                    SolidPillButton(
                                        text = "Sign In",
                                        buttonColor = PureLightGreenButton,
                                        onClick = {
                                            if (email.isBlank()) {
                                                errorMessage = "Please enter an email address."
                                                return@SolidPillButton
                                            }
                                            val displayName = if (email.contains("@")) {
                                                email.substringBefore("@").replace(".", " ").capitalizeWords()
                                            } else {
                                                "Jonathan Smith"
                                            }
                                            onAuthSuccess(
                                                UserProfile(
                                                    name = displayName,
                                                    email = email,
                                                    institution = "Royal College, Colombo / UoM",
                                                    targetExam = selectedStream,
                                                    streamOrDegree = "Combined Maths, Physics, Chemistry",
                                                    studyHoursThisWeek = 14.5f,
                                                    totalPapersSolved = 28,
                                                    streakDays = 12,
                                                    points = 1850,
                                                    isLoggedIn = true
                                                )
                                            )
                                        },
                                        testTag = "btn_sign_in"
                                    )

                                    // Or Login With divider
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "   Or Login With   ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Modern Official Social Login Buttons: Google and Apple
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        OfficialGoogleButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                onAuthSuccess(
                                                    UserProfile(
                                                        name = "Jonathan Smith",
                                                        email = "jonathansmith76@gmail.com",
                                                        institution = "University of Moratuwa / Colombo",
                                                        targetExam = "G.C.E. A/L (Physical Science)",
                                                        streamOrDegree = "Combined Maths, Physics, Chemistry",
                                                        studyHoursThisWeek = 16.0f,
                                                        totalPapersSolved = 34,
                                                        streakDays = 15,
                                                        points = 2100,
                                                        isLoggedIn = true
                                                    )
                                                )
                                            }
                                        )

                                        OfficialAppleButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                onAuthSuccess(
                                                    UserProfile(
                                                        name = "Jonathan Smith",
                                                        email = "jonathan.smith@icloud.com",
                                                        institution = "Sri Lanka Institute of IT (SLIIT)",
                                                        targetExam = "Undergraduate (BSc / BEng / BIT)",
                                                        streamOrDegree = "Software Engineering",
                                                        studyHoursThisWeek = 12.0f,
                                                        totalPapersSolved = 20,
                                                        streakDays = 8,
                                                        points = 1450,
                                                        isLoggedIn = true
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            AuthMode.SIGN_UP -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // First Name & Last Name 2-column row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CleanLabeledTextField(
                                            label = "First Name",
                                            value = firstName,
                                            placeholder = "Jonathan",
                                            modifier = Modifier.weight(1f),
                                            onValueChange = { firstName = it },
                                            testTag = "signup_firstname_field"
                                        )
                                        CleanLabeledTextField(
                                            label = "Last Name",
                                            value = lastName,
                                            placeholder = "Smith",
                                            modifier = Modifier.weight(1f),
                                            onValueChange = { lastName = it },
                                            testTag = "signup_lastname_field"
                                        )
                                    }

                                    // Email input
                                    CleanLabeledTextField(
                                        label = "Email",
                                        value = email,
                                        placeholder = "jonathansmith76@gmail.com",
                                        keyboardType = KeyboardType.Email,
                                        onValueChange = { email = it },
                                        testTag = "signup_email_field"
                                    )

                                    // Academic target curriculum
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Study Stream / Target Exam",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ExposedDropdownMenuBox(
                                            expanded = isStreamMenuOpen,
                                            onExpandedChange = { isStreamMenuOpen = it }
                                        ) {
                                            OutlinedTextField(
                                                value = selectedStream,
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStreamMenuOpen) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                    focusedBorderColor = PureLightGreenButton
                                                )
                                            )
                                            ExposedDropdownMenu(
                                                expanded = isStreamMenuOpen,
                                                onDismissRequest = { isStreamMenuOpen = false }
                                            ) {
                                                streamList.forEach { streamItem ->
                                                    DropdownMenuItem(
                                                        text = { Text(streamItem, fontSize = 13.sp) },
                                                        onClick = {
                                                            selectedStream = streamItem
                                                            isStreamMenuOpen = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Password input
                                    CleanLabeledTextField(
                                        label = "Password",
                                        value = password,
                                        placeholder = "••••••••••••",
                                        keyboardType = KeyboardType.Password,
                                        isPassword = true,
                                        passwordVisible = passwordVisible,
                                        onTogglePassword = { passwordVisible = !passwordVisible },
                                        onValueChange = { password = it },
                                        testTag = "signup_password_field"
                                    )

                                    // Confirm Password input
                                    CleanLabeledTextField(
                                        label = "Confirm Password",
                                        value = confirmPassword,
                                        placeholder = "••••••••••••",
                                        keyboardType = KeyboardType.Password,
                                        isPassword = true,
                                        passwordVisible = confirmPasswordVisible,
                                        onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible },
                                        onValueChange = { confirmPassword = it },
                                        testTag = "signup_confirm_password_field"
                                    )

                                    // Register Button
                                    SolidPillButton(
                                        text = "Register",
                                        buttonColor = PureLightGreenButton,
                                        onClick = {
                                            val fullName = "$firstName $lastName".trim().ifBlank { "Jonathan Smith" }
                                            onAuthSuccess(
                                                UserProfile(
                                                    name = fullName,
                                                    email = email,
                                                    institution = "Sri Lanka Education System",
                                                    targetExam = selectedStream,
                                                    streamOrDegree = "Combined Maths, Physics & Tech",
                                                    studyHoursThisWeek = 6.0f,
                                                    totalPapersSolved = 10,
                                                    streakDays = 1,
                                                    points = 500,
                                                    isLoggedIn = true
                                                )
                                            )
                                        },
                                        testTag = "btn_register"
                                    )
                                }
                            }

                            AuthMode.VERIFY_OTP -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Text(
                                        text = "Verify your account",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Please enter the 5-digit verification code we sent to your registered email to proceed securely.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    // 5 OTP code boxes
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        otpDigits.forEachIndexed { index, digit ->
                                            val isCurrentFocus = index == activeOtpIndex
                                            val hasValue = digit.isNotBlank()

                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(
                                                        width = if (isCurrentFocus) 2.dp else 1.dp,
                                                        color = if (isCurrentFocus) PureLightGreenButton else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        val updated = otpDigits.toMutableList()
                                                        updated[index] = ""
                                                        otpDigits = updated
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (hasValue) digit else if (isCurrentFocus) "-" else "",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }

                                    // Verification Button
                                    SolidPillButton(
                                        text = "Verification",
                                        buttonColor = PureLightGreenButton,
                                        onClick = {
                                            onAuthSuccess(
                                                UserProfile(
                                                    name = "Jonathan Smith",
                                                    email = email,
                                                    institution = "Royal College / University of Colombo",
                                                    targetExam = selectedStream,
                                                    streamOrDegree = "Combined Maths & ICT",
                                                    studyHoursThisWeek = 14.5f,
                                                    totalPapersSolved = 28,
                                                    streakDays = 12,
                                                    points = 1850,
                                                    isLoggedIn = true
                                                )
                                            )
                                        },
                                        testTag = "btn_verification"
                                    )

                                    // Don't received OTP? RESEND
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Don't received OTP? ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "RESEND",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RedError,
                                            modifier = Modifier
                                                .clickable {
                                                    otpDigits = listOf("8", "2", "4", "1", "")
                                                    errorMessage = null
                                                }
                                                .testTag("btn_resend_otp")
                                        )
                                    }

                                    // On-screen dial pad
                                    KeypadGrid(
                                        onDigitClick = { char ->
                                            val firstEmpty = otpDigits.indexOfFirst { it.isEmpty() }
                                            if (firstEmpty != -1) {
                                                val mutable = otpDigits.toMutableList()
                                                mutable[firstEmpty] = char
                                                otpDigits = mutable
                                            }
                                        },
                                        onBackspace = {
                                            val lastFilled = otpDigits.indexOfLast { it.isNotEmpty() }
                                            if (lastFilled != -1) {
                                                val mutable = otpDigits.toMutableList()
                                                mutable[lastFilled] = ""
                                                otpDigits = mutable
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Segmented Toggle with clean, high-contrast active pill indicator.
 */
@Composable
fun ModernSegmentedToggle(
    isSignIn: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF1F5F9), // Light clean slate-gray container
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sign In Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSignIn) Color.White else Color.Transparent)
                    .then(
                        if (isSignIn) Modifier.shadow(3.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x20000000), spotColor = Color(0x20000000))
                        else Modifier
                    )
                    .clickable(onClick = onSelectSignIn)
                    .testTag("tab_sign_in"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = if (isSignIn) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSignIn) PureLightGreenButton else Color(0xFF64748B),
                    fontSize = 15.sp
                )
            }

            // Sign Up Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (!isSignIn) Color.White else Color.Transparent)
                    .then(
                        if (!isSignIn) Modifier.shadow(3.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x20000000), spotColor = Color(0x20000000))
                        else Modifier
                    )
                    .clickable(onClick = onSelectSignUp)
                    .testTag("tab_sign_up"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    fontWeight = if (!isSignIn) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isSignIn) PureLightGreenButton else Color(0xFF64748B),
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Clean field with top label matching the mockup format.
 */
@Composable
fun CleanLabeledTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    testTag: String = ""
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (isPassword && onTogglePassword != null) {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = PureLightGreenButton,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

/**
 * Full-width Modern Pill button with crisp solid green color and soft elevation.
 */
@Composable
fun SolidPillButton(
    text: String,
    buttonColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(4.dp, RoundedCornerShape(26.dp), spotColor = buttonColor.copy(alpha = 0.4f))
            .testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

/**
 * Real Official Google Button with exact official Multi-Color G Logo and typography.
 */
@Composable
fun OfficialGoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = modifier
            .height(50.dp)
            .testTag("google_login_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Official Google 4-Color 'G' Canvas Icon
            GoogleLogoIcon(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Google",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF1F2937)
            )
        }
    }
}

/**
 * Exact Google 4-Color vector G logo drawn programmatically on Canvas.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = w * 0.22f
        val radius = (w - stroke) / 2f

        // Blue right segment & horizontal bar
        val blueColor = Color(0xFF4285F4)
        val redColor = Color(0xFFEA4335)
        val yellowColor = Color(0xFFFBBC05)
        val greenColor = Color(0xFF34A853)

        // Draw outer arc sections with accurate official Google colors
        drawArc(
            color = redColor,
            startAngle = 180f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawArc(
            color = yellowColor,
            startAngle = 120f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawArc(
            color = greenColor,
            startAngle = 45f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawArc(
            color = blueColor,
            startAngle = 320f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )

        // Center crossbar for the G
        drawRect(
            color = blueColor,
            topLeft = Offset(cx - (stroke * 0.1f), cy - (stroke / 2)),
            size = Size(cx + (stroke * 0.45f) - (cx - stroke * 0.1f), stroke)
        )
    }
}

/**
 * Real Official Apple Button with authentic Apple silhouette logo and typography.
 */
@Composable
fun OfficialAppleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFF0F172A), // Official Apple black / obsidian
        shadowElevation = 2.dp,
        modifier = modifier
            .height(50.dp)
            .testTag("apple_login_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AppleSilhouetteIcon(modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Apple",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Apple Silhouette vector logo drawn via Canvas Path.
 */
@Composable
fun AppleSilhouetteIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Apple leaf
        val leafPath = Path().apply {
            moveTo(w * 0.52f, h * 0.22f)
            cubicTo(w * 0.52f, h * 0.10f, w * 0.62f, 0f, w * 0.72f, 0f)
            cubicTo(w * 0.73f, h * 0.12f, w * 0.63f, h * 0.24f, w * 0.52f, h * 0.22f)
            close()
        }
        drawPath(leafPath, color = tint, style = Fill)

        // Apple Body with bite cut-out
        val bodyPath = Path().apply {
            moveTo(w * 0.75f, h * 0.32f)
            // right top curve
            cubicTo(w * 0.88f, h * 0.40f, w * 0.94f, h * 0.56f, w * 0.88f, h * 0.68f)
            cubicTo(w * 0.82f, h * 0.78f, w * 0.76f, h * 0.88f, w * 0.64f, h * 0.88f)
            cubicTo(w * 0.54f, h * 0.88f, w * 0.50f, h * 0.82f, w * 0.38f, h * 0.82f)
            cubicTo(w * 0.26f, h * 0.82f, w * 0.22f, h * 0.88f, w * 0.12f, h * 0.88f)
            cubicTo(0f, h * 0.88f, -0.04f, h * 0.70f, 0.04f, h * 0.54f)
            cubicTo(0.12f, h * 0.38f, 0.26f, h * 0.28f, 0.40f, h * 0.28f)
            cubicTo(0.50f, h * 0.28f, 0.56f, h * 0.34f, 0.64f, h * 0.34f)
            cubicTo(0.70f, h * 0.34f, 0.72f, h * 0.30f, 0.75f, h * 0.32f)
            close()
        }
        drawPath(bodyPath, color = tint, style = Fill)
    }
}

/**
 * Numeric Keypad Grid matching Mockup Screen 3.
 */
@Composable
fun KeypadGrid(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("" to "", "0" to "", "BACKSPACE" to "")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (digit, sub) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (digit.isNotEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable(enabled = digit.isNotEmpty()) {
                                if (digit == "BACKSPACE") {
                                    onBackspace()
                                } else {
                                    onDigitClick(digit)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit == "BACKSPACE") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (digit.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (sub.isNotEmpty()) {
                                    Text(
                                        text = sub,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
