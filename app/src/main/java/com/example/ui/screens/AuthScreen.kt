package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.util.Patterns
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.data.remote.ApiOutcome
import com.example.data.local.TheStudentPreferences
import com.example.R
import com.example.ui.components.StudentLoadingSpinner
import com.example.ui.components.TheStudentAppBackground
import com.example.ui.components.AuthSignInSkeleton
import com.example.ui.components.AuthSignUpSkeleton
import com.example.ui.components.TheStudentSnackbarHost
import com.example.ui.components.AuthOtpSkeleton
import com.example.ui.theme.*

// Modern Darkened Emerald Palette for Modern White, Black & Green UI (Emerald 700)
private val AuthModernDarkGreen = Color(0xFF047857)

/** Public pages referenced by the sign-up consent row. */
private const val TERMS_URL = "https://app.quizontal.site/legal/terms.html"
private const val PRIVACY_URL = "https://app.quizontal.site/legal/privacy.html"

/** "5:00" style countdown instead of the broken "0:300". */
internal fun formatCooldown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    VERIFY_OTP,
    FORGOT_PASSWORD,
    RESET_PASSWORD
}

/**
 * Client-side mirror of the server password policy. The server remains the
 * source of truth - this only gives immediate feedback.
 */
private fun passwordPolicyError(password: String): String? = when {
    password.isEmpty() -> "Please enter a password."
    password.trim().isEmpty() -> "Please enter a password."
    password.length < 8 -> "Use at least 8 characters."
    password.length > 72 -> "Password is too long (maximum 72 characters)."
    !password.any { it.isLetter() } -> "Include at least one letter."
    !password.any { it.isDigit() } -> "Include at least one number."
    else -> null
}

private fun isEmailValid(value: String): Boolean =
    value.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

/**
 * True when repeating the same request can plausibly succeed: the network
 * failed, the server throttled us, or it had a temporary problem. A 400/401
 * ("wrong password", "bad email") is the user's to fix, not a retry.
 */
private fun AuthApiResult.Error.isRetryable(): Boolean =
    isNetworkError || statusCode == 429 || statusCode >= 500

private fun ApiOutcome.Failure.isRetryable(): Boolean =
    isNetworkError || statusCode == 429 || statusCode >= 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    initialIsRegister: Boolean = false,
    initialIsOtp: Boolean = false,
    initialPendingProfile: UserProfile? = null,
    initialOtpCode: String? = null,
    onAuthSuccess: (UserProfile) -> Unit,
    backendBaseUrl: String = "https://app.quizontal.site/",
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
    var isRequestingReset by remember { mutableStateOf(false) }
    var isResettingPassword by remember { mutableStateOf(false) }
    var resetToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    // Retry hook for recoverable (network / rate-limit) failures. Null when the
    // error is a validation problem the user has to fix by editing the form.
    var retryAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var acceptedTerms by remember { mutableStateOf(false) }
    val isAnyLoading = isGoogleSigningIn || isSigningIn || isRegistering || isVerifyingOtp ||
        isRequestingReset || isResettingPassword

    /** Show an error, optionally offering a one-tap retry. */
    fun showError(message: String, retry: (() -> Unit)? = null) {
        errorMessage = message
        retryAction = retry
    }

    fun clearError() {
        errorMessage = null
        retryAction = null
    }

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
            AuthMode.VERIFY_OTP,
            AuthMode.RESET_PASSWORD,
            AuthMode.FORGOT_PASSWORD,
            AuthMode.SIGN_UP -> {
                clearError()
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
            clearError()
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
                                rememberMe = true,
                                role = authResult.role,
                                expiresAt = authResult.expiresAt
                            )
                            onAuthSuccess(authResult.profile)
                        }
                        is AuthApiResult.ResetRequired -> {
                            errorMessage = "Unexpected response from the server. Please try again."
                        }
                        is AuthApiResult.Error -> {
                            showError(
                                authResult.message,
                                retry = ({ handleGoogleSignIn() }).takeIf { authResult.isRetryable() }
                            )
                        }
                    }
                }
                is GoogleAuthResult.Cancelled -> {
                    // Show ONLY native Android Toast with app icon
                    android.widget.Toast.makeText(context, "Sign in with Google was cancelled", android.widget.Toast.LENGTH_SHORT).show()
                }
                is GoogleAuthResult.Error -> {
                    showError(result.message)
                }
            }
            isGoogleSigningIn = false
        }
    }

    fun handleEmailSignIn() {
        if (isAnyLoading) return
        if (!isEmailValid(email)) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (password.isBlank()) {
            errorMessage = "Please enter your password."
            return
        }

        // NOTE: passwords are never trimmed or normalised - the exact characters
        // entered are the exact characters hashed, so spaces are significant.
        coroutineScope.launch {
            isSigningIn = true
            clearError()
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
                        rememberMe = rememberMe,
                        role = result.role,
                        expiresAt = result.expiresAt
                    )
                    onAuthSuccess(result.profile)
                }
                is AuthApiResult.ResetRequired -> {
                    errorMessage = "Unexpected response from the server. Please try again."
                }
                is AuthApiResult.Error -> {
                    showError(
                        result.message,
                        retry = ({ handleEmailSignIn() }).takeIf { result.isRetryable() }
                    )
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
        if (!isEmailValid(email)) {
            errorMessage = "Please enter a valid email address."
            return
        }
        passwordPolicyError(password)?.let { policyError ->
            errorMessage = policyError
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        // Consent is required before an account is created (and before any
        // personal data leaves the device).
        if (!acceptedTerms) {
            errorMessage = "Please accept the Terms of Service and Privacy Policy to create an account."
            return
        }
        coroutineScope.launch {
            isRegistering = true
            clearError()
            val fullName = "${firstName.trim()} ${lastName.trim()}"
            when (val result = TheStudentAuthClient.register(backendBaseUrl, fullName, email.trim(), password, selectedStream)) {
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
                        rememberMe = true,
                        role = result.role,
                        expiresAt = result.expiresAt
                    )
                    onAuthSuccess(result.profile)
                }
                is AuthApiResult.ResetRequired -> {
                    errorMessage = "Unexpected response from the server. Please try again."
                }
                is AuthApiResult.Error -> {
                    showError(
                        result.message,
                        retry = ({ handleEmailRegister() }).takeIf { result.isRetryable() }
                    )
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
            clearError()

            // Strictly verify against DirectAdmin MySQL backend to update is_email_verified
            when (val result = TheStudentAuthClient.verifyOtp(backendBaseUrl, targetEmail, enteredCode)) {
                is AuthApiResult.Success -> {
                    TheStudentPreferences.setSession(
                        context = context,
                        token = result.token,
                        userEmail = result.profile.email,
                        userName = result.profile.name,
                        rememberMe = true,
                        role = result.role,
                        expiresAt = result.expiresAt
                    )
                    android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    onAuthSuccess(result.profile)
                }
                is AuthApiResult.ResetRequired -> {
                    // The code was valid but it belongs to a password reset:
                    // no session is created, the user must choose a new password.
                    resetToken = result.resetToken
                    clearError()
                    otpDigits = listOf("", "", "", "", "", "")
                    isVerifyingOtp = false
                    switchModeWithShimmer(AuthMode.RESET_PASSWORD)
                }
                is AuthApiResult.RequiresOtp -> {
                    showError(result.message)
                }
                is AuthApiResult.Error -> {
                    showError(
                        result.message,
                        retry = ({ handleVerifyOtp() }).takeIf { result.isRetryable() }
                    )
                    result.retryAfterSeconds?.let { wait ->
                        if (wait in 1..300) resendCooldownSeconds = maxOf(resendCooldownSeconds, wait)
                    }
                }
            }
            isVerifyingOtp = false
        }
    }

    fun handleResendOtp() {
        if (isAnyLoading || resendCooldownSeconds > 0) return
        val targetEmail = pendingVerificationEmail.ifBlank { email }
        if (!isEmailValid(targetEmail)) {
            errorMessage = "Please enter a valid email address."
            return
        }
        coroutineScope.launch {
            clearError()
            when (val outcome = TheStudentAuthClient.resendOtp(backendBaseUrl, targetEmail.trim())) {
                is ApiOutcome.Success -> {
                    resendCooldownSeconds = outcome.cooldownSeconds.coerceIn(30, 300)
                    android.widget.Toast.makeText(context, outcome.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is ApiOutcome.Failure -> {
                    showError(
                        outcome.message,
                        retry = ({ handleResendOtp() }).takeIf { outcome.isRetryable() }
                    )
                    outcome.retryAfterSeconds?.let { wait ->
                        if (wait in 1..300) resendCooldownSeconds = wait
                    }
                }
            }
        }
    }

    /**
     * Step 1 of the password reset flow: ask the server to email a reset code.
     * The server intentionally returns the same response for unknown emails.
     */
    fun handleForgotPassword() {
        if (isAnyLoading) return
        if (!isEmailValid(email)) {
            errorMessage = "Please enter a valid email address."
            return
        }
        coroutineScope.launch {
            isRequestingReset = true
            clearError()
            when (val outcome = TheStudentAuthClient.requestPasswordReset(backendBaseUrl, email.trim())) {
                is ApiOutcome.Success -> {
                    pendingVerificationEmail = email.trim()
                    pendingVerificationReason = "password_reset"
                    otpDigits = listOf("", "", "", "", "", "")
                    resendCooldownSeconds = outcome.cooldownSeconds.coerceIn(30, 300)
                    android.widget.Toast.makeText(context, outcome.message, android.widget.Toast.LENGTH_LONG).show()
                    isRequestingReset = false
                    switchModeWithShimmer(AuthMode.VERIFY_OTP)
                }
                is ApiOutcome.Failure -> {
                    showError(
                        outcome.message,
                        retry = ({ handleForgotPassword() }).takeIf { outcome.isRetryable() }
                    )
                    outcome.retryAfterSeconds?.let { wait ->
                        if (wait in 1..300) resendCooldownSeconds = wait
                    }
                }
            }
            isRequestingReset = false
        }
    }

    /** Step 3 of the reset flow: submit the new password with the reset token. */
    fun handleResetPassword() {
        if (isAnyLoading) return
        if (resetToken.isBlank()) {
            errorMessage = "Your reset session expired. Please request a new code."
            return
        }
        passwordPolicyError(newPassword)?.let { policyError ->
            errorMessage = policyError
            return
        }
        if (newPassword != confirmNewPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        coroutineScope.launch {
            isResettingPassword = true
            clearError()
            when (val outcome = TheStudentAuthClient.resetPassword(backendBaseUrl, resetToken, newPassword)) {
                is ApiOutcome.Success -> {
                    resetToken = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    otpDigits = listOf("", "", "", "", "", "")
                    password = ""
                    pendingVerificationReason = ""
                    android.widget.Toast.makeText(context, outcome.message, android.widget.Toast.LENGTH_LONG).show()
                    isResettingPassword = false
                    switchModeWithShimmer(AuthMode.SIGN_IN)
                }
                is ApiOutcome.Failure -> {
                    showError(
                        outcome.message,
                        retry = ({ handleResetPassword() }).takeIf { outcome.isRetryable() }
                    )
                }
            }
            isResettingPassword = false
        }
    }

    // Every auth step (sign in, sign up, verification, forgot and reset password)
    // now shares the app-wide background from onboarding / profile setup, so the
    // status bar blends into the same colour as the rest of the app.

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen"),
        snackbarHost = {
            TheStudentSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .imePadding()
            )
        },
        // Transparent on purpose: the backgrounds below are drawn edge-to-edge
        // so the status bar shows the same colour as the screen.
        containerColor = Color.Transparent
    ) { innerPadding ->
        // ── Box overlay: real content underneath, shimmer fades on top ──────
        Box(modifier = Modifier.fillMaxSize()) {

        // Shared app background, drawn edge-to-edge so the system bars blend in.
        // TheStudentAppBackground also flips the status-bar icons to dark.
        TheStudentAppBackground(
            baseColor = Color(0xFFF8FAFC),
            modifier = Modifier.fillMaxSize()
        ) {}

        if (authMode == AuthMode.VERIFY_OTP) {
            ModernEmailVerificationScreen(
                targetEmail = pendingVerificationEmail.ifBlank { email }.ifBlank { "your email" },
                reason = pendingVerificationReason,
                isSecureConnection = backendBaseUrl.trim().startsWith("https", ignoreCase = true),
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
                    clearError()
                    switchModeWithShimmer(AuthMode.SIGN_IN)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // ═════════════════════════════════════════════════════════════════
            // MODERN AUTH FORM  (sign in · sign up · forgot · reset)
            // =================================================================
            // Light wash background (same as onboarding / profile setup), a
            // slim brand bar, big headline, animated mode switch and one
            // floating white card that holds the whole form.
            // ═════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── slim top bar: back + brand mark ──────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Scaffold already applies the status-bar inset through
                        // innerPadding, so no extra status-bars padding here.
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            if (!isAnyLoading) {
                                if (authMode == AuthMode.SIGN_IN) {
                                    onBack?.invoke()
                                } else {
                                    clearError()
                                    switchModeWithShimmer(AuthMode.SIGN_IN)
                                }
                            }
                        },
                        enabled = !isAnyLoading,
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("auth_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AuthModernDarkGreen,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Image(
                        painter = painterResource(id = R.drawable.thestudent_logo_full),
                        contentDescription = "TheStudent",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(26.dp)
                            .aspectRatio(3.6211f)
                    )
                }

                // ── scrollable content ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(10.dp))

                    val headline = when (authMode) {
                        AuthMode.SIGN_IN -> "Welcome back"
                        AuthMode.SIGN_UP -> "Create your account"
                        AuthMode.FORGOT_PASSWORD -> "Forgot password?"
                        AuthMode.RESET_PASSWORD -> "Choose a new password"
                        AuthMode.VERIFY_OTP -> "Verify it's you"
                    }
                    val subheadline = when (authMode) {
                        AuthMode.SIGN_IN -> "Pick up right where you left off."
                        AuthMode.SIGN_UP -> "Free forever, built for the Sri Lankan syllabus."
                        AuthMode.FORGOT_PASSWORD -> "We'll email a 6-digit code to your inbox."
                        AuthMode.RESET_PASSWORD -> "At least 8 characters, with a letter and a number."
                        AuthMode.VERIFY_OTP -> "Enter the code we emailed you."
                    }

                    Text(
                        text = headline,
                        fontFamily = MaisonNeueFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        color = Color(0xFF0B1220),
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subheadline,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(22.dp))

                    // ── sign in / sign up segmented switch ───────────────────
                    if (authMode == AuthMode.SIGN_IN || authMode == AuthMode.SIGN_UP) {
                        ModernAuthToggle(
                            isSignIn = authMode == AuthMode.SIGN_IN,
                            enabled = !isAnyLoading,
                            onSelectSignIn = {
                                if (!isAnyLoading) {
                                    switchModeWithShimmer(AuthMode.SIGN_IN)
                                    clearError()
                                }
                            },
                            onSelectSignUp = {
                                if (!isAnyLoading) {
                                    switchModeWithShimmer(AuthMode.SIGN_UP)
                                    clearError()
                                }
                            }
                        )
                        Spacer(Modifier.height(18.dp))
                    }

                    // ── error banner ─────────────────────────────────────────
                    AnimatedVisibility(visible = errorMessage != null) {
                        Column {
                            AuthErrorBanner(
                                message = errorMessage ?: "",
                                retry = retryAction?.let { retry ->
                                    {
                                        clearError()
                                        retry()
                                    }
                                }
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    // ── the form card ────────────────────────────────────────
                    // Flat white card: no outline and no shadow, so the form
                    // sits quietly on the wash instead of glowing around it.
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AnimatedContent(
                                targetState = authMode,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "auth_screen_transition"
                            ) { mode ->
                                when (mode) {

                                    // ─────────────────────────────── SIGN IN ──
                                    AuthMode.SIGN_IN -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        CleanLabeledTextField(
                                            label = "Email address",
                                            value = email,
                                            placeholder = "you@example.com",
                                            keyboardType = KeyboardType.Email,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.AlternateEmail,
                                            onValueChange = {
                                                email = it
                                                clearError()
                                            },
                                            autofillType = ContentType.EmailAddress,
                                            testTag = "signin_email_field"
                                        )

                                        CleanLabeledTextField(
                                            label = "Password",
                                            value = password,
                                            placeholder = "Enter your password",
                                            keyboardType = KeyboardType.Password,
                                            isPassword = true,
                                            passwordVisible = passwordVisible,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.Lock,
                                            onTogglePassword = { passwordVisible = !passwordVisible },
                                            onValueChange = {
                                                password = it
                                                clearError()
                                            },
                                            autofillType = ContentType.Password,
                                            imeAction = ImeAction.Done,
                                            onSubmit = { handleEmailSignIn() },
                                            testTag = "signin_password_field"
                                        )

                                        // Remember me + forgot password
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable(enabled = !isAnyLoading) {
                                                    rememberMe = !rememberMe
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = rememberMe,
                                                    onCheckedChange = null,
                                                    enabled = !isAnyLoading,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = AuthModernDarkGreen,
                                                        checkmarkColor = Color.White,
                                                        uncheckedColor = Color(0xFFCBD5E1)
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Remember me",
                                                    fontFamily = MaisonNeueFontFamily,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF475569)
                                                )
                                            }

                                            Text(
                                                text = "Forgot password?",
                                                fontFamily = MaisonNeueFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary,
                                                modifier = Modifier
                                                    .clickable(enabled = !isAnyLoading) {
                                                        clearError()
                                                        switchModeWithShimmer(AuthMode.FORGOT_PASSWORD)
                                                    }
                                                    .testTag("forget_password_button")
                                            )
                                        }

                                        Spacer(Modifier.height(2.dp))

                                        SolidPillButton(
                                            text = if (isSigningIn) "Signing in..." else "Sign in",
                                            buttonColor = AuthModernDarkGreen,
                                            isLoading = isSigningIn,
                                            enabled = !isAnyLoading,
                                            onClick = { handleEmailSignIn() },
                                            testTag = "btn_sign_in"
                                        )

                                        AuthOrDivider(text = "or continue with")

                                        OfficialGoogleButton(
                                            text = "Continue with Google",
                                            isLoading = isGoogleSigningIn,
                                            enabled = !isAnyLoading,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { handleGoogleSignIn() }
                                        )
                                    }

                                    // ─────────────────────────────── SIGN UP ──
                                    AuthMode.SIGN_UP -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CleanLabeledTextField(
                                                label = "First name",
                                                value = firstName,
                                                placeholder = "Danuka",
                                                modifier = Modifier.weight(1f),
                                                enabled = !isAnyLoading,
                                                onValueChange = { firstName = it },
                                                autofillType = ContentType.PersonFirstName,
                                                testTag = "signup_firstname_field"
                                            )
                                            CleanLabeledTextField(
                                                label = "Last name",
                                                value = lastName,
                                                placeholder = "Dissanayake",
                                                modifier = Modifier.weight(1f),
                                                enabled = !isAnyLoading,
                                                onValueChange = { lastName = it },
                                                autofillType = ContentType.PersonLastName,
                                                testTag = "signup_lastname_field"
                                            )
                                        }

                                        CleanLabeledTextField(
                                            label = "Email address",
                                            value = email,
                                            placeholder = "you@example.com",
                                            keyboardType = KeyboardType.Email,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.AlternateEmail,
                                            onValueChange = {
                                                email = it
                                                clearError()
                                            },
                                            autofillType = ContentType.EmailAddress,
                                            testTag = "signup_email_field"
                                        )

                                        CleanLabeledTextField(
                                            label = "Password",
                                            value = password,
                                            placeholder = "At least 8 characters",
                                            keyboardType = KeyboardType.Password,
                                            isPassword = true,
                                            passwordVisible = passwordVisible,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.Lock,
                                            onTogglePassword = { passwordVisible = !passwordVisible },
                                            onValueChange = {
                                                password = it
                                                clearError()
                                            },
                                            autofillType = ContentType.NewPassword,
                                            testTag = "signup_password_field"
                                        )
                                        AnimatedVisibility(visible = password.isNotEmpty()) {
                                            PasswordStrengthHint(password = password)
                                        }

                                        CleanLabeledTextField(
                                            label = "Confirm password",
                                            value = confirmPassword,
                                            placeholder = "Repeat your password",
                                            keyboardType = KeyboardType.Password,
                                            isPassword = true,
                                            passwordVisible = confirmPasswordVisible,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.LockReset,
                                            onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible },
                                            onValueChange = {
                                                confirmPassword = it
                                                clearError()
                                            },
                                            autofillType = ContentType.NewPassword,
                                            imeAction = ImeAction.Done,
                                            onSubmit = { handleEmailRegister() },
                                            testTag = "signup_confirm_password_field"
                                        )

                                        SignUpConsentRow(
                                            accepted = acceptedTerms,
                                            enabled = !isAnyLoading,
                                            onToggle = { acceptedTerms = !acceptedTerms }
                                        )

                                        SolidPillButton(
                                            text = if (isRegistering) "Creating account..." else "Create account",
                                            buttonColor = AuthModernDarkGreen,
                                            isLoading = isRegistering,
                                            enabled = !isAnyLoading,
                                            onClick = { handleEmailRegister() },
                                            testTag = "btn_register"
                                        )

                                        AuthOrDivider(text = "or sign up with")

                                        OfficialGoogleButton(
                                            text = "Sign up with Google",
                                            isLoading = isGoogleSigningIn,
                                            enabled = !isAnyLoading,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { handleGoogleSignIn() }
                                        )
                                    }

                                    // ───────────────────────── FORGOT PASSWORD ──
                                    AuthMode.FORGOT_PASSWORD -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "Enter the email linked to your account and we'll send " +
                                                "you a 6-digit reset code.",
                                            fontFamily = MaisonNeueFontFamily,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        CleanLabeledTextField(
                                            label = "Email address",
                                            value = email,
                                            placeholder = "you@example.com",
                                            keyboardType = KeyboardType.Email,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.AlternateEmail,
                                            onValueChange = {
                                                email = it
                                                clearError()
                                            },
                                            autofillType = ContentType.EmailAddress,
                                            imeAction = ImeAction.Done,
                                            onSubmit = { handleForgotPassword() },
                                            testTag = "forgot_email_field"
                                        )
                                        SolidPillButton(
                                            text = if (isRequestingReset) "Sending code..." else "Send reset code",
                                            buttonColor = AuthModernDarkGreen,
                                            isLoading = isRequestingReset,
                                            enabled = !isAnyLoading,
                                            onClick = { handleForgotPassword() },
                                            testTag = "btn_send_reset_code"
                                        )
                                        Text(
                                            text = "Back to sign in",
                                            fontFamily = MaisonNeueFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AuthModernDarkGreen,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isAnyLoading) {
                                                    clearError()
                                                    switchModeWithShimmer(AuthMode.SIGN_IN)
                                                }
                                                .padding(vertical = 6.dp)
                                        )
                                    }

                                    // ────────────────────────── RESET PASSWORD ──
                                    AuthMode.RESET_PASSWORD -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "New password for " +
                                                pendingVerificationEmail.ifBlank { email }.ifBlank { "your email" } +
                                                ". All other devices will be signed out.",
                                            fontFamily = MaisonNeueFontFamily,
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        CleanLabeledTextField(
                                            label = "New password",
                                            value = newPassword,
                                            placeholder = "At least 8 characters",
                                            keyboardType = KeyboardType.Password,
                                            isPassword = true,
                                            passwordVisible = newPasswordVisible,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.Lock,
                                            onTogglePassword = { newPasswordVisible = !newPasswordVisible },
                                            onValueChange = {
                                                newPassword = it
                                                clearError()
                                            },
                                            autofillType = ContentType.NewPassword,
                                            testTag = "reset_password_field"
                                        )
                                        CleanLabeledTextField(
                                            label = "Confirm new password",
                                            value = confirmNewPassword,
                                            placeholder = "Repeat your new password",
                                            keyboardType = KeyboardType.Password,
                                            isPassword = true,
                                            passwordVisible = confirmNewPasswordVisible,
                                            enabled = !isAnyLoading,
                                            leadingIcon = Icons.Default.LockReset,
                                            onTogglePassword = { confirmNewPasswordVisible = !confirmNewPasswordVisible },
                                            onValueChange = {
                                                confirmNewPassword = it
                                                clearError()
                                            },
                                            autofillType = ContentType.NewPassword,
                                            imeAction = ImeAction.Done,
                                            onSubmit = { handleResetPassword() },
                                            testTag = "reset_confirm_password_field"
                                        )
                                        SolidPillButton(
                                            text = if (isResettingPassword) "Updating..." else "Update password",
                                            buttonColor = AuthModernDarkGreen,
                                            isLoading = isResettingPassword,
                                            enabled = !isAnyLoading,
                                            onClick = { handleResetPassword() },
                                            testTag = "btn_update_password"
                                        )
                                    }

                                    AuthMode.VERIFY_OTP -> {}
                                }
                            }
                        }
                    }

                    // ── footer switch ────────────────────────────────────────
                    Spacer(Modifier.height(18.dp))
                    if (authMode == AuthMode.SIGN_IN || authMode == AuthMode.SIGN_UP) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (authMode == AuthMode.SIGN_IN) {
                                    "New to TheStudent?"
                                } else {
                                    "Already have an account?"
                                },
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (authMode == AuthMode.SIGN_IN) "Create account" else "Sign in",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .clickable(enabled = !isAnyLoading) {
                                        clearError()
                                        switchModeWithShimmer(
                                            if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP
                                            else AuthMode.SIGN_IN
                                        )
                                    }
                                    .testTag("btn_switch_auth_mode")
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Secure connection · email verification required",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(Modifier.height(26.dp))
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
                    AuthMode.SIGN_IN -> AuthSignInSkeleton()
                    AuthMode.SIGN_UP -> AuthSignUpSkeleton()
                    AuthMode.FORGOT_PASSWORD -> AuthSignInSkeleton()
                    AuthMode.RESET_PASSWORD -> AuthSignUpSkeleton()
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
 * Live password strength meter.
 *
 * Shows a 4-segment bar plus the requirement the user is still missing, so the
 * policy is visible *while typing* instead of only after a rejected submit.
 * (The server remains the authority; this is guidance only.)
 */
@Composable
fun PasswordStrengthHint(
    password: String,
    modifier: Modifier = Modifier
) {
    val hasLength = password.length >= 8
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    val score = listOf(hasLength, hasLetter, hasDigit, hasSymbol).count { it }

    val label = when {
        score <= 1 -> "Weak"
        score <= 3 -> "Fair"
        else -> "Strong"
    }
    val color = when {
        score <= 1 -> RedError
        score <= 3 -> Color(0xFFD97706)
        else -> AuthModernDarkGreen
    }
    val missing = buildList {
        if (!hasLength) add("8+ characters")
        if (!hasLetter) add("a letter")
        if (!hasDigit) add("a number")
        if (!hasSymbol) add("a symbol")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < score.coerceAtLeast(1)) color else Color(0xFFE2E8F0))
                )
                if (index < 3) Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (missing.isEmpty()) {
                "Great password. Keep it somewhere safe."
            } else {
                "Add " + missing.joinToString(", ")
            },
            fontSize = 11.5.sp,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Terms of Service / Privacy Policy consent.
 *
 * Publishing an app that creates accounts on Google Play requires the user to
 * actively agree to both, and the links must be reachable before sign-up.
 */
@Composable
fun SignUpConsentRow(
    accepted: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = enabled) { onToggle() }
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = null,
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = AuthModernDarkGreen,
                    checkmarkColor = Color.White
                )
            )
            Text(
                text = "I agree to TheStudent's Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConsentLink(text = "Read terms", enabled = enabled) { uriHandler.openUri(TERMS_URL) }
            Text(text = "·", color = Color(0xFF94A3B8), fontSize = 12.sp)
            ConsentLink(text = "Read privacy policy", enabled = enabled) { uriHandler.openUri(PRIVACY_URL) }
        }
    }
}

@Composable
private fun ConsentLink(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) AuthModernDarkGreen else Color(0xFF94A3B8),
        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    )
}

/**
 * Segmented sign-in / sign-up switch used above the auth form card.
 */
@Composable
fun ModernAuthToggle(
    isSignIn: Boolean,
    enabled: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE6ECF3)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentedAuthOption(
                text = "Sign in",
                selected = isSignIn,
                enabled = enabled,
                onClick = onSelectSignIn,
                modifier = Modifier.weight(1f)
            )
            SegmentedAuthOption(
                text = "Sign up",
                selected = !isSignIn,
                enabled = enabled,
                onClick = onSelectSignUp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentedAuthOption(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AuthModernDarkGreen else Color.Transparent)
            .clickable(enabled = enabled && !selected) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> Color.White
                enabled -> Color(0xFF475569)
                else -> Color(0xFF94A3B8)
            }
        )
    }
}

/**
 * Soft red banner shown above the form card whenever the view model reports an error.
 */
@Composable
fun AuthErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    retry: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFECACA)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Color(0xFFB91C1C),
                modifier = Modifier.weight(1f)
            )
            if (retry != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Retry",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { retry() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * "─── or continue with ───" hairline divider used before the Google button.
 */
@Composable
fun AuthOrDivider(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFE8EDF3))
        )
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFE8EDF3))
        )
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
    autofillType: ContentType? = null,
    imeAction: ImeAction = ImeAction.Next,
    onSubmit: (() -> Unit)? = null,
    testTag: String = "",
    leadingIcon: ImageVector? = null
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
            .testTag(testTag)
            // Tells Google Password Manager / autofill services what this field
            // holds, so credentials can be suggested and saved.
            .then(
                if (autofillType != null) {
                    Modifier.semantics { contentType = autofillType }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onSubmit?.invoke() },
            onDone = { onSubmit?.invoke() },
            onGo = { onSubmit?.invoke() }
        ),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(19.dp)
                )
            }
        },
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
    isSecureConnection: Boolean = true,
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
    // Transparent: the caller paints TheStudentAppBackground edge-to-edge
    // behind the status bar, so the wash stays continuous.
    Box(
        modifier = modifier
            .fillMaxSize()
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

                    // Security pill. The label reflects the REAL transport the app
                    // is talking to - it used to say "256-Bit SSL" unconditionally,
                    // which was a false claim whenever the server URL was http://.
                    val infoIsWarning = reason == "ip_change" || !isSecureConnection
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (infoIsWarning) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, if (infoIsWarning) Color(0xFFFDE68A) else Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (infoIsWarning) Icons.Default.Warning else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (infoIsWarning) Color(0xFFB45309) else AuthModernDarkGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = when {
                                    !isSecureConnection -> "Insecure connection"
                                    reason == "ip_change" -> "Security Alert"
                                    else -> "Secure connection"
                                },
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (infoIsWarning) Color(0xFF92400E) else AuthModernDarkGreen
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
                    text = when (reason) {
                        "ip_change" -> "Security Verification"
                        "password_reset" -> "Reset Your Password"
                        else -> "Verify Your Email"
                    },
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

                // 6 OTP code boxes (46dp x 52dp) with a real - but invisible -
                // text field on top, so paste, a hardware keyboard and Android's
                // one-time-code autofill all work. It auto-submits on the 6th digit.
                val otpText = otpDigits.joinToString("")
                Box(contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = otpText,
                        onValueChange = { raw ->
                            if (!isAnyLoading) {
                                val digits = raw.filter { it.isDigit() }.take(6)
                                val updated = MutableList(6) { "" }
                                digits.forEachIndexed { i, character -> updated[i] = character.toString() }
                                onOtpDigitChange(updated)
                                if (digits.length == 6) onVerify()
                            }
                        },
                        enabled = !isAnyLoading,
                        modifier = Modifier
                            .matchParentSize()
                            .semantics { contentType = ContentType.SmsOtpCode }
                            .testTag("otp_input_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.Transparent,
                            fontSize = 22.sp
                        ),
                        cursorBrush = SolidColor(Color.Transparent),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onVerify() })
                    )
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
                            text = formatCooldown(resendCooldownSeconds),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
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
                                maxLines = 3
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
