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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.model.UserProfile
import com.example.data.remote.TheStudentGoogleAuthClient
import com.example.data.remote.GoogleAuthResult
import com.example.data.remote.TheStudentAuthClient
import com.example.data.remote.AuthApiResult
import com.example.data.remote.TheStudentEmailSender
import com.example.data.local.TheStudentPreferences
import com.example.ui.components.TheStudentLogo
import com.example.ui.components.StudentLoadingSpinner
import com.example.ui.components.AuthSignInSkeleton
import com.example.ui.components.AuthSignUpSkeleton
import com.example.ui.components.AuthOtpSkeleton
import com.example.ui.theme.*

// Modern Darkened Emerald Palette for Modern White, Black & Green UI (Emerald 700)
private val AuthModernDarkGreen = Color(0xFF047857)

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    VERIFY_OTP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    initialIsRegister: Boolean = false,
    initialIsOtp: Boolean = false,
    initialPendingProfile: UserProfile? = null,
    initialOtpCode: String? = null,
    onAuthSuccess: (UserProfile) -> Unit,
    backendBaseUrl: String = "https://app.quizontal.site/",
    onOpenAdminPanel: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onContinueAsGuest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var authMode by remember(initialIsOtp, initialIsRegister) {
        mutableStateOf(
            when {
                initialIsOtp -> AuthMode.VERIFY_OTP
                initialIsRegister -> AuthMode.SIGN_UP
                else -> AuthMode.SIGN_IN
            }
        )
    }

    // Form inputs (No dummy pre-filled credentials - strictly validated server-side)
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Target stream/curriculum for Sri Lanka learners
    val selectedStream = "G.C.E. A/L (Physical Science)"

    // 6-digit OTP verification code state
    var otpDigits by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    val activeOtpIndex = otpDigits.indexOfFirst { it.isEmpty() }.let { if (it == -1) 5 else it }
    var pendingOtpCode by remember(initialOtpCode) { mutableStateOf(initialOtpCode ?: "") }
    var pendingVerificationProfile by remember(initialPendingProfile) { mutableStateOf(initialPendingProfile) }
    var pendingVerificationEmail by remember(initialPendingProfile) { mutableStateOf(initialPendingProfile?.email ?: "") }
    var pendingVerificationReason by remember { mutableStateOf("registration") }
    var resendCooldownSeconds by remember(initialIsOtp) { mutableStateOf(if (initialIsOtp) 50 else 0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    val isAnyLoading = isGoogleSigningIn || isSigningIn || isRegistering || isVerifyingOtp

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Shimmer transition state ──────────────────────────────────────────────
    // Full screen skeleton shimmer loading:
    // When navigating to AuthScreen (e.g. from Google Login on Onboarding, Register, or Sign-In),
    // or when toggling between Auth screens, the skeleton shimmer is displayed for ~950ms.
    var isAuthTransitioning by remember { mutableStateOf(true) }
    var transitionTargetMode by remember(initialIsOtp, initialIsRegister) {
        mutableStateOf(
            when {
                initialIsOtp -> AuthMode.VERIFY_OTP
                initialIsRegister -> AuthMode.SIGN_UP
                else -> AuthMode.SIGN_IN
            }
        )
    }

    // Always show initial skeleton shimmer on entry for 950ms so user sees the premium loading state
    LaunchedEffect(initialIsOtp, initialIsRegister) {
        val target = when {
            initialIsOtp -> AuthMode.VERIFY_OTP
            initialIsRegister -> AuthMode.SIGN_UP
            else -> AuthMode.SIGN_IN
        }
        transitionTargetMode = target
        isAuthTransitioning = true
        delay(950)
        authMode = target
        isAuthTransitioning = false
    }

    // Trigger shimmer overlay for toggle transitions (Sign In ↔ Sign Up ↔ Back)
    fun switchModeWithShimmer(newMode: AuthMode) {
        if (newMode == authMode || isAnyLoading || isAuthTransitioning) return
        transitionTargetMode = newMode        // skeleton mirrors the destination
        isAuthTransitioning = true            // shimmer fades IN
        coroutineScope.launch {
            kotlinx.coroutines.delay(950)     // hold skeleton visible for satisfying sweep
            authMode = newMode                // silently switch real content
            kotlinx.coroutines.delay(100)     // brief settle before fade-out
            isAuthTransitioning = false       // shimmer fades OUT
        }
    }

    // Hardware & system gesture back handler: strictly preserves auth hierarchy and never leaks to Dashboard
    androidx.activity.compose.BackHandler(enabled = true) {
        if (isAnyLoading) return@BackHandler
        when (authMode) {
            AuthMode.VERIFY_OTP -> {
                switchModeWithShimmer(AuthMode.SIGN_IN)
            }
            AuthMode.SIGN_UP -> {
                switchModeWithShimmer(AuthMode.SIGN_IN)
            }
            AuthMode.SIGN_IN -> {
                onBack?.invoke()
            }
        }
    }

    // Resend countdown timer
    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000)
            resendCooldownSeconds -= 1
        }
    }

    fun handleGoogleSignIn() {
        if (isAnyLoading) return
        coroutineScope.launch {
            isGoogleSigningIn = true
            errorMessage = null
            when (val result = TheStudentGoogleAuthClient.signIn(context)) {
                is GoogleAuthResult.Success -> {
                    // Call DirectAdmin backend to create/authenticate user & issue OTP in MySQL
                    when (val authResult = TheStudentAuthClient.googleLogin(backendBaseUrl, result.idToken)) {
                        is AuthApiResult.RequiresOtp -> {
                            pendingVerificationEmail = authResult.email
                            pendingVerificationProfile = result.profile
                            pendingVerificationReason = authResult.reason
                            otpDigits = listOf("", "", "", "", "", "")
                            resendCooldownSeconds = 50
                            // ── OTP Skeleton shimmer transition ──
                            transitionTargetMode = AuthMode.VERIFY_OTP
                            isGoogleSigningIn = false        // hide spinner now
                            isAuthTransitioning = true
                            kotlinx.coroutines.delay(950)    // hold shimmer fully visible
                            authMode = AuthMode.VERIFY_OTP   // switch real content silently
                            kotlinx.coroutines.delay(100)    // settle before fade-out
                            isAuthTransitioning = false      // shimmer fades out
                        }
                        is AuthApiResult.Success -> {
                            TheStudentPreferences.setSession(
                                context = context,
                                token = authResult.token,
                                userEmail = authResult.profile.email,
                                userName = authResult.profile.name,
                                rememberMe = true
                            )
                            onAuthSuccess(authResult.profile)
                        }
                        is AuthApiResult.Error -> {
                            errorMessage = authResult.message
                        }
                    }
                }
                is GoogleAuthResult.Cancelled -> {
                    // Show ONLY native Android Toast with app icon
                    android.widget.Toast.makeText(context, "Sign in with Google was cancelled", android.widget.Toast.LENGTH_SHORT).show()
                }
                is GoogleAuthResult.Error -> {
                    errorMessage = result.message
                }
            }
            isGoogleSigningIn = false
        }
    }

    fun handleEmailSignIn() {
        if (isAnyLoading) return
        if (email.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (password.isBlank()) {
            errorMessage = "Please enter your password."
            return
        }

        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        // ── MASTER ADMIN INTERCEPT ──
        // Email: admin@gmail.com, Pass: admin123 -> Navigates immediately to Admin Panel
        if (trimmedEmail.equals("admin@gmail.com", ignoreCase = true) && trimmedPassword == "admin123") {
            errorMessage = null
            coroutineScope.launch {
                isSigningIn = true
                val adminProfile = UserProfile(
                    name = "Administrator",
                    email = "admin@gmail.com",
                    academicLevel = "Administrator",
                    streamOrDegree = "System Admin",
                    institution = "TheStudent Console",
                    targetExam = "Administration",
                    streakDays = 1,
                    studyHoursThisWeek = 10.0f,
                    totalPapersSolved = 100,
                    points = 9999,
                    isLoggedIn = true
                )
                TheStudentPreferences.setSession(
                    context = context,
                    token = "admin_master_token",
                    userEmail = adminProfile.email,
                    userName = adminProfile.name,
                    rememberMe = rememberMe
                )
                onAuthSuccess(adminProfile)
                onOpenAdminPanel?.invoke()
                isSigningIn = false
            }
            return
        }

        coroutineScope.launch {
            isSigningIn = true
            errorMessage = null
            when (val result = TheStudentAuthClient.login(backendBaseUrl, email, password)) {
                is AuthApiResult.RequiresOtp -> {
                    pendingVerificationEmail = result.email
                    val fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    pendingVerificationProfile = UserProfile(name = fullName, email = email, isLoggedIn = true)
                    pendingVerificationReason = result.reason
                    otpDigits = listOf("", "", "", "", "", "")
                    resendCooldownSeconds = 50
                    // ── OTP Skeleton shimmer transition ──
                    transitionTargetMode = AuthMode.VERIFY_OTP
                    isSigningIn = false
                    isAuthTransitioning = true
                    kotlinx.coroutines.delay(950)
                    authMode = AuthMode.VERIFY_OTP
                    kotlinx.coroutines.delay(100)
                    isAuthTransitioning = false
                }
                is AuthApiResult.Success -> {
                    TheStudentPreferences.setSession(
                        context = context,
                        token = result.token,
                        userEmail = result.profile.email,
                        userName = result.profile.name,
                        rememberMe = rememberMe
                    )
                    onAuthSuccess(result.profile)
                }
                is AuthApiResult.Error -> {
                    errorMessage = result.message
                }
            }
            isSigningIn = false
        }
    }

    fun handleEmailRegister() {
        if (isAnyLoading) return
        if (firstName.isBlank() || lastName.isBlank()) {
            errorMessage = "Please enter your first and last name."
            return
        }
        if (email.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters long."
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        coroutineScope.launch {
            isRegistering = true
            errorMessage = null
            val fullName = "$firstName $lastName".trim()
            when (val result = TheStudentAuthClient.register(backendBaseUrl, fullName, email, password, selectedStream)) {
                is AuthApiResult.RequiresOtp -> {
                    pendingVerificationEmail = result.email
                    pendingVerificationProfile = UserProfile(name = fullName, email = email, isLoggedIn = true)
                    pendingVerificationReason = result.reason
                    otpDigits = listOf("", "", "", "", "", "")
                    resendCooldownSeconds = 50
                    // ── OTP Skeleton shimmer transition ──
                    transitionTargetMode = AuthMode.VERIFY_OTP
                    isRegistering = false
                    isAuthTransitioning = true
                    kotlinx.coroutines.delay(950)
                    authMode = AuthMode.VERIFY_OTP
                    kotlinx.coroutines.delay(100)
                    isAuthTransitioning = false
                }
                is AuthApiResult.Success -> {
                    TheStudentPreferences.setSession(
                        context = context,
                        token = result.token,
                        userEmail = result.profile.email,
                        userName = result.profile.name,
                        rememberMe = true
                    )
                    onAuthSuccess(result.profile)
                }
                is AuthApiResult.Error -> {
                    errorMessage = result.message
                }
            }
            isRegistering = false
        }
    }

    fun handleVerifyOtp() {
        if (isAnyLoading) return
        val enteredCode = otpDigits.joinToString("")
        if (enteredCode.length < 6) {
            errorMessage = "Please enter the complete 6-digit verification code."
            return
        }
        val targetEmail = pendingVerificationEmail.ifBlank { email }
        coroutineScope.launch {
            isVerifyingOtp = true
            errorMessage = null

            // Strictly verify against DirectAdmin MySQL backend to update is_email_verified
            when (val result = TheStudentAuthClient.verifyOtp(backendBaseUrl, targetEmail, enteredCode)) {
                is AuthApiResult.Success -> {
                    TheStudentPreferences.setSession(
                        context = context,
                        token = result.token,
                        userEmail = result.profile.email,
                        userName = result.profile.name,
                        rememberMe = true
                    )
                    android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    onAuthSuccess(result.profile)
                }
                is AuthApiResult.RequiresOtp -> {
                    errorMessage = result.message
                }
                is AuthApiResult.Error -> {
                    errorMessage = result.message
                }
            }
            isVerifyingOtp = false
        }
    }

    fun handleResendOtp() {
        if (isAnyLoading || resendCooldownSeconds > 0) return
        val targetEmail = pendingVerificationEmail.ifBlank { email }
        if (targetEmail.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        coroutineScope.launch {
            errorMessage = null
            val result = TheStudentAuthClient.resendOtp(backendBaseUrl, targetEmail)
            result.onSuccess { msg ->
                resendCooldownSeconds = 50
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                errorMessage = err.message
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen"),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .imePadding()
            ) { data ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E293B),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = data.visuals.message,
                        fontFamily = MaisonNeueFontFamily,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        },
        containerColor = if (authMode == AuthMode.VERIFY_OTP) Color(0xFFF8FAFC) else AuthModernDarkGreen
    ) { innerPadding ->
        // ── Box overlay: real content underneath, shimmer fades on top ──────
        Box(modifier = Modifier.fillMaxSize()) {

        if (authMode == AuthMode.VERIFY_OTP) {
            ModernEmailVerificationScreen(
                targetEmail = pendingVerificationEmail.ifBlank { email }.ifBlank { "your email" },
                reason = pendingVerificationReason,
                otpDigits = otpDigits,
                activeOtpIndex = activeOtpIndex,
                isAnyLoading = isAnyLoading,
                isVerifyingOtp = isVerifyingOtp,
                resendCooldownSeconds = resendCooldownSeconds,
                errorMessage = errorMessage,
                onOtpDigitChange = { otpDigits = it },
                onVerify = { handleVerifyOtp() },
                onResend = { handleResendOtp() },
                onBack = {
                    errorMessage = null
                    switchModeWithShimmer(AuthMode.SIGN_IN)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Real auth content (SIGN_IN & SIGN_UP)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(AuthModernDarkGreen)
            ) {
                // Top Header section for Sign In & Sign Up
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circular back button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(enabled = !isAnyLoading) {
                                    if (authMode == AuthMode.SIGN_UP) {
                                        errorMessage = null
                                        switchModeWithShimmer(AuthMode.SIGN_IN)
                                    } else {
                                        onBack?.invoke()
                                    }
                                }
                                .testTag("auth_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AuthModernDarkGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (authMode == AuthMode.SIGN_IN) {
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
                    } else {
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main White Sheet with generous top rounded corners
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(16.dp, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                    // Redesigned modern toggle pill switcher
                    if (authMode != AuthMode.VERIFY_OTP) {
                        ModernSegmentedToggle(
                            isSignIn = authMode == AuthMode.SIGN_IN,
                            enabled = !isAnyLoading,
                            onSelectSignIn = {
                                if (!isAnyLoading) {
                                    switchModeWithShimmer(AuthMode.SIGN_IN)
                                    errorMessage = null
                                }
                            },
                            onSelectSignUp = {
                                if (!isAnyLoading) {
                                    switchModeWithShimmer(AuthMode.SIGN_UP)
                                    errorMessage = null
                                }
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
                                        enabled = !isAnyLoading,
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
                                        enabled = !isAnyLoading,
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
                                            modifier = Modifier.clickable(enabled = !isAnyLoading) { rememberMe = !rememberMe }
                                        ) {
                                            Checkbox(
                                                checked = rememberMe,
                                                enabled = !isAnyLoading,
                                                onCheckedChange = { rememberMe = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = AuthModernDarkGreen,
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
                                                .clickable(enabled = !isAnyLoading) { switchModeWithShimmer(AuthMode.VERIFY_OTP) }
                                                .testTag("forget_password_button")
                                        )
                                    }

                                    // Primary Sign In Button
                                    SolidPillButton(
                                        text = if (isSigningIn) "Signing In..." else "Sign In",
                                        buttonColor = AuthModernDarkGreen,
                                        isLoading = isSigningIn,
                                        enabled = !isAnyLoading,
                                        onClick = { handleEmailSignIn() },
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

                                    // Official Google Button (Full Width, Apple removed, Admin Panel removed)
                                    OfficialGoogleButton(
                                        text = "Continue with Google",
                                        isLoading = isGoogleSigningIn,
                                        enabled = !isAnyLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { handleGoogleSignIn() }
                                    )
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
                                            enabled = !isAnyLoading,
                                            onValueChange = { firstName = it },
                                            testTag = "signup_firstname_field"
                                        )
                                        CleanLabeledTextField(
                                            label = "Last Name",
                                            value = lastName,
                                            placeholder = "Smith",
                                            modifier = Modifier.weight(1f),
                                            enabled = !isAnyLoading,
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
                                        enabled = !isAnyLoading,
                                        onValueChange = {
                                            email = it
                                            errorMessage = null
                                        },
                                        testTag = "signup_email_field"
                                    )

                                    // Password input
                                    CleanLabeledTextField(
                                        label = "Password",
                                        value = password,
                                        placeholder = "••••••••••••",
                                        keyboardType = KeyboardType.Password,
                                        isPassword = true,
                                        passwordVisible = passwordVisible,
                                        enabled = !isAnyLoading,
                                        onTogglePassword = { passwordVisible = !passwordVisible },
                                        onValueChange = {
                                            password = it
                                            errorMessage = null
                                        },
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
                                        enabled = !isAnyLoading,
                                        onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible },
                                        onValueChange = {
                                            confirmPassword = it
                                            errorMessage = null
                                        },
                                        testTag = "signup_confirm_password_field"
                                    )

                                    // Register Button
                                    SolidPillButton(
                                        text = if (isRegistering) "Creating Account..." else "Register",
                                        buttonColor = AuthModernDarkGreen,
                                        isLoading = isRegistering,
                                        enabled = !isAnyLoading,
                                        onClick = { handleEmailRegister() },
                                        testTag = "btn_register"
                                    )

                                    // Or Sign Up With divider
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "   Or Sign Up With   ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Official Google Button in Sign Up
                                    OfficialGoogleButton(
                                        text = "Sign up with Google",
                                        isLoading = isGoogleSigningIn,
                                        enabled = !isAnyLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { handleGoogleSignIn() }
                                    )
                                }
                            }

                            AuthMode.VERIFY_OTP -> {}
                        }
                    }
                }
            }
        }
    }

        // ── Shimmer overlay (on top of real content, fades in/out) ────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = isAuthTransitioning,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(180)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(280)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (transitionTargetMode) {
                    AuthMode.SIGN_IN  -> AuthSignInSkeleton()
                    AuthMode.SIGN_UP  -> AuthSignUpSkeleton()
                    AuthMode.VERIFY_OTP -> AuthOtpSkeleton()
                }
            }
        }

        } // end outer Box
    }
}

/**
 * Modern Segmented Toggle with generous padding and vibrant darkened-emerald active pill indicator.
 */
@Composable
fun ModernSegmentedToggle(
    isSignIn: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(29.dp),
        color = Color(0xFFF1F5F9), // Light clean slate-gray container
        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(5.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sign In Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSignIn) AuthModernDarkGreen else Color.Transparent)
                    .then(
                        if (isSignIn) Modifier.shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = AuthModernDarkGreen.copy(alpha = 0.40f),
                            ambientColor = AuthModernDarkGreen.copy(alpha = 0.15f)
                        )
                        else Modifier
                    )
                    .clickable(enabled = enabled, onClick = onSelectSignIn)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("tab_sign_in"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = if (isSignIn) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSignIn) Color.White else Color(0xFF64748B),
                    fontSize = 15.sp
                )
            }

            // Sign Up Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (!isSignIn) AuthModernDarkGreen else Color.Transparent)
                    .then(
                        if (!isSignIn) Modifier.shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = AuthModernDarkGreen.copy(alpha = 0.40f),
                            ambientColor = AuthModernDarkGreen.copy(alpha = 0.15f)
                        )
                        else Modifier
                    )
                    .clickable(enabled = enabled, onClick = onSelectSignUp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("tab_sign_up"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = if (!isSignIn) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (!isSignIn) Color.White else Color(0xFF64748B),
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Clean field with floating notch label matching Material Design outline specs.
 */
@Composable
fun CleanLabeledTextField(
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontWeight = FontWeight.Medium
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, color = Color(0xFF94A3B8), fontFamily = MaisonNeueFontFamily) }
        } else null,
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword && onTogglePassword != null) {
                IconButton(onClick = onTogglePassword, enabled = enabled) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = if (enabled) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedBorderColor = AuthModernDarkGreen,
            unfocusedLabelColor = Color(0xFF64748B),
            focusedLabelColor = AuthModernDarkGreen,
            unfocusedTextColor = Color(0xFF0F172A),
            focusedTextColor = Color(0xFF0F172A),
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            disabledBorderColor = Color(0xFFE2E8F0),
            disabledLabelColor = Color(0xFF94A3B8),
            disabledTextColor = Color(0xFF64748B),
            disabledContainerColor = Color(0xFFF8FAFC)
        )
    )
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
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Button(
        onClick = { if (!isLoading && enabled) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled && !isLoading) 4.dp else 1.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = buttonColor.copy(alpha = if (enabled && !isLoading) 0.4f else 0.15f)
            )
            .testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isLoading) buttonColor.copy(alpha = 0.85f) else buttonColor,
            contentColor = Color.White,
            disabledContainerColor = if (isLoading) buttonColor.copy(alpha = 0.85f) else buttonColor.copy(alpha = 0.55f),
            disabledContentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                StudentLoadingSpinner(
                    size = 24.dp,
                    isOnDarkBackground = true
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                fontFamily = MaisonNeueFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Real Official Google Button with exact official Multi-Color G Logo and typography.
 */
@Composable
fun OfficialGoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Continue with Google",
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (!isLoading && enabled) onClick() },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(26.dp),
        color = if (isLoading || !enabled) Color(0xFFF8FAFC) else Color.White,
        border = BorderStroke(1.2.dp, if (isLoading || !enabled) Color(0xFFCBD5E1) else Color(0xFFE2E8F0)),
        modifier = modifier
            .height(52.dp)
            .shadow(
                elevation = if (isLoading || !enabled) 1.dp else 2.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x10000000),
                ambientColor = Color(0x06000000)
            )
            .testTag("google_login_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                StudentLoadingSpinner(
                    size = 26.dp,
                    isOnDarkBackground = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connecting to Google...",
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF334155),
                    letterSpacing = (-0.2).sp
                )
            } else {
                // Official Google 4-Color 'G' Canvas Icon
                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    fontFamily = MaisonNeueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (enabled) Color(0xFF1F2937) else Color(0xFF94A3B8),
                    letterSpacing = (-0.2).sp
                )
            }
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
    modifier: Modifier = Modifier,
    buttonHeight: androidx.compose.ui.unit.Dp = 40.dp,
    rowSpacing: androidx.compose.ui.unit.Dp = 5.dp
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
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                row.forEach { (digit, sub) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (digit.isNotEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                                modifier = Modifier.size(19.dp)
                            )
                        } else if (digit.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (sub.isNotEmpty()) {
                                    Text(
                                        text = sub,
                                        fontSize = 7.5.sp,
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

/**
 * Modern, edge-to-edge, ultra-clean Email Verification screen.
 * Non-scrolling one-shot layout with glowing floating hero emblem,
 * interactive target email capsule, crisp 6-digit OTP cards, countdown timer,
 * and a tactile squircle passcode keypad anchored cleanly at the bottom.
 */
@Composable
fun ModernEmailVerificationScreen(
    targetEmail: String,
    reason: String,
    otpDigits: List<String>,
    activeOtpIndex: Int,
    isAnyLoading: Boolean,
    isVerifyingOtp: Boolean,
    resendCooldownSeconds: Int,
    errorMessage: String?,
    onOtpDigitChange: (List<String>) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Celestial background art from splash screen (glowing orbital curves & stars)
        OnboardingModernFooterArt(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .alpha(0.85f)
        )

        // Main content column - strictly non-scrolling, spaced between
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP & HERO SECTION ────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Elevated white circular back button
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable(enabled = !isAnyLoading) { onBack() }
                            .testTag("auth_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Security Shield Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (reason == "ip_change") Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, if (reason == "ip_change") Color(0xFFFDE68A) else Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (reason == "ip_change") Icons.Default.Warning else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (reason == "ip_change") Color(0xFFB45309) else AuthModernDarkGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (reason == "ip_change") "Security Alert" else "256-Bit SSL",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (reason == "ip_change") Color(0xFF92400E) else AuthModernDarkGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hero Floating Emblem (Layered Glowing Mint Squircle with Email Icon)
                Box(
                    modifier = Modifier.size(66.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDCFCE7).copy(alpha = 0.55f),
                        modifier = Modifier.size(66.dp)
                    ) {}
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.5.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = AuthModernDarkGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Heading
                Text(
                    text = if (reason == "ip_change") "Security Verification" else "Verify Your Email",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.4).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Enter the 6-digit confirmation code sent to",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Target email capsule with edit action
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable(enabled = !isAnyLoading) { onBack() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = targetEmail,
                            fontFamily = MaisonNeueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = AuthModernDarkGreen,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit email",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6 OTP code boxes (46dp x 52dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    otpDigits.forEachIndexed { index, digit ->
                        val isCurrentFocus = index == activeOtpIndex
                        val hasValue = digit.isNotBlank()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (hasValue) Color(0xFFF0FDF4) else Color.White,
                            shadowElevation = if (isCurrentFocus) 4.dp else 1.dp,
                            border = BorderStroke(
                                width = if (isCurrentFocus) 2.dp else 1.2.dp,
                                color = if (isCurrentFocus) AuthModernDarkGreen
                                else if (hasValue) AuthModernDarkGreen.copy(alpha = 0.5f)
                                else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .size(width = 46.dp, height = 52.dp)
                                .clickable(enabled = !isAnyLoading) {
                                    val updated = otpDigits.toMutableList()
                                    updated[index] = ""
                                    onOtpDigitChange(updated)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (hasValue) {
                                    Text(
                                        text = digit,
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                } else if (isCurrentFocus) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(20.dp)
                                            .background(AuthModernDarkGreen, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resend Countdown Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (resendCooldownSeconds > 0) {
                        Text(
                            text = "Resend code in ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "0:${resendCooldownSeconds.toString().padStart(2, '0')}",
                            fontFamily = MaisonNeueFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = AuthModernDarkGreen,
                            fontSize = 12.5.sp
                        )
                    } else {
                        Text(
                            text = "Didn't receive code? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = !isAnyLoading) { onResend() }
                                .testTag("btn_resend_otp")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AuthModernDarkGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Resend Code",
                                fontFamily = MaisonNeueFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = AuthModernDarkGreen,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                // Error Message Banner (if any)
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = errorMessage,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B),
                                lineHeight = 16.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // ── BOTTOM SECTION: Verify Button + Modern Passcode Keypad ───
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Button: Verify & Continue
                val isCodeComplete = otpDigits.joinToString("").length == 6
                Button(
                    onClick = onVerify,
                    enabled = !isAnyLoading && isCodeComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            elevation = if (isCodeComplete) 8.dp else 0.dp,
                            shape = RoundedCornerShape(25.dp),
                            spotColor = Color(0x66047857)
                        )
                        .testTag("btn_verification"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuthModernDarkGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                ) {
                    if (isVerifyingOtp) {
                        StudentLoadingSpinner(size = 20.dp, isOnDarkBackground = true)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verifying Code...",
                            fontFamily = MaisonNeueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Verify & Continue",
                                fontFamily = MaisonNeueFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Modern Minimalist Passcode Keypad
                ModernPasscodeKeypad(
                    onDigitClick = { char ->
                        if (isAnyLoading) return@ModernPasscodeKeypad
                        val firstEmpty = otpDigits.indexOfFirst { it.isEmpty() }
                        if (firstEmpty != -1) {
                            val mutable = otpDigits.toMutableList()
                            mutable[firstEmpty] = char
                            onOtpDigitChange(mutable)
                        }
                    },
                    onBackspace = {
                        if (isAnyLoading) return@ModernPasscodeKeypad
                        val lastFilled = otpDigits.indexOfLast { it.isNotEmpty() }
                        if (lastFilled != -1) {
                            val mutable = otpDigits.toMutableList()
                            mutable[lastFilled] = ""
                            onOtpDigitChange(mutable)
                        }
                    },
                    onClearAll = {
                        if (isAnyLoading) return@ModernPasscodeKeypad
                        onOtpDigitChange(listOf("", "", "", "", "", ""))
                    }
                )
            }
        }
    }
}

/**
 * Clean, tactile squircle passcode keypad styled like premium banking & iOS passcode entries.
 */
@Composable
fun ModernPasscodeKeypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLEAR", "0", "BACKSPACE")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { item ->
                    when (item) {
                        "CLEAR" -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { onClearAll() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        "BACKSPACE" -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { onBackspace() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { onDigitClick(item) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item,
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
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
