package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TheStudentPreferences
import com.example.data.remote.ApiOutcome
import com.example.data.remote.AuthApiResult
import com.example.data.remote.TheStudentAuthClient
import com.example.ui.theme.MaisonNeueFontFamily
import com.example.ui.theme.RedError
import kotlinx.coroutines.launch

private val SecurityGreen = Color(0xFF047857)
private val SecurityInk = Color(0xFF0F172A)
private val SecurityMuted = Color(0xFF64748B)
private val SecurityLine = Color(0xFFE2E8F0)

/** Mirror of the server policy, for immediate feedback only. */
private fun securityPasswordError(password: String): String? = when {
    password.isEmpty() -> "Please enter your new password."
    password.trim().isEmpty() -> "Please enter your new password."
    password.length < 8 -> "Use at least 8 characters."
    password.length > 72 -> "Password is too long (maximum 72 characters)."
    !password.any { it.isLetter() } -> "Include at least one letter."
    !password.any { it.isDigit() } -> "Include at least one number."
    else -> null
}

/**
 * Account & security sheet.
 *
 * Gives the student the controls a production app is expected to have:
 *  • change password (server revokes every other device and returns a new token)
 *  • sign out of all devices
 *  • delete the account and its data (self-service, required by Play policy)
 *
 * All networking happens here; the caller only owns session persistence.
 */
@Composable
fun SecurityScreen(
    backendBaseUrl: String,
    onBack: () -> Unit,
    onSignOutEverywhere: () -> Unit,
    onPasswordChanged: (newToken: String?, expiresAt: String?) -> Unit,
    onAccountDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val email = remember { TheStudentPreferences(context).userEmail.orEmpty() }
    val role = remember { TheStudentPreferences(context).userRole }
    val expiresAt = remember { TheStudentPreferences(context).sessionExpiresAt }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf<String?>(null) }

    var showSignOutAllDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isSigningOutAll by remember { mutableStateOf(false) }

    val isBusy = isChangingPassword || isDeleting || isSigningOutAll

    fun submitPasswordChange() {
        if (isBusy) return
        if (currentPassword.isEmpty()) {
            passwordError = "Please enter your current password."
            return
        }
        securityPasswordError(newPassword)?.let { message ->
            passwordError = message
            return
        }
        if (newPassword != confirmPassword) {
            passwordError = "The two passwords do not match."
            return
        }
        if (newPassword == currentPassword) {
            passwordError = "Choose a password you have not used before."
            return
        }
        val token = TheStudentPreferences(context).sessionToken
        if (token.isNullOrBlank()) {
            passwordError = "Your session expired. Please sign in again."
            return
        }
        scope.launch {
            isChangingPassword = true
            passwordError = null
            passwordSuccess = null
            when (val outcome = TheStudentAuthClient.changePassword(backendBaseUrl, token, currentPassword, newPassword)) {
                is AuthApiResult.Success -> {
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    passwordSuccess = outcome.message
                    onPasswordChanged(outcome.token, outcome.expiresAt)
                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                }
                is AuthApiResult.RequiresOtp -> passwordError = "Please finish verifying your email first."
                is AuthApiResult.ResetRequired -> passwordError = "Please sign in again to change your password."
                is AuthApiResult.Error -> passwordError = outcome.message
            }
            isChangingPassword = false
        }
    }

    fun confirmDelete() {
        if (isDeleting) return
        val token = TheStudentPreferences(context).sessionToken
        if (token.isNullOrBlank()) {
            deleteError = "Your session expired. Please sign in again."
            return
        }
        scope.launch {
            isDeleting = true
            deleteError = null
            when (val outcome = TheStudentAuthClient.deleteAccount(backendBaseUrl, token, deletePassword)) {
                is ApiOutcome.Success -> {
                    showDeleteDialog = false
                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                    onAccountDeleted()
                }
                is ApiOutcome.Failure -> deleteError = outcome.message
            }
            isDeleting = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // statusBarsPadding before the scroll container: the back button
                // and the title were sliding under the clock, because the screen
                // is drawn full-bleed with no app bar above it.
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, SecurityLine),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(enabled = !isBusy) { onBack() }
                        .testTag("security_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SecurityGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Account & security",
                        fontFamily = MaisonNeueFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = SecurityInk
                    )
                    Text(
                        text = "Manage your password and devices",
                        fontSize = 13.5.sp,
                        color = SecurityMuted
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Account card ──────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SecurityLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = if (role == TheStudentPreferences.ROLE_ADMIN) "Administrator account" else "Student account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SecurityInk
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoRow(label = "Email", value = email.ifBlank { "—" })
                    InfoRow(label = "Signed in until", value = expiresAt?.take(16) ?: "Session active")
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── Change password ───────────────────────────────────────────
            SectionTitle(icon = Icons.Default.Lock, text = "Change password")
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CleanLabeledTextField(
                    label = "Current password",
                    value = currentPassword,
                    placeholder = "Your current password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    enabled = !isBusy,
                    onValueChange = {
                        currentPassword = it
                        passwordError = null
                        passwordSuccess = null
                    },
                    testTag = "security_current_password"
                )
                CleanLabeledTextField(
                    label = "New password",
                    value = newPassword,
                    placeholder = "At least 8 characters",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    passwordVisible = newPasswordVisible,
                    enabled = !isBusy,
                    onTogglePassword = { newPasswordVisible = !newPasswordVisible },
                    onValueChange = {
                        newPassword = it
                        passwordError = null
                        passwordSuccess = null
                    },
                    testTag = "security_new_password"
                )
                if (newPassword.isNotEmpty()) {
                    PasswordStrengthHint(password = newPassword)
                }
                CleanLabeledTextField(
                    label = "Confirm new password",
                    value = confirmPassword,
                    placeholder = "Repeat the new password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    enabled = !isBusy,
                    onValueChange = {
                        confirmPassword = it
                        passwordError = null
                        passwordSuccess = null
                    },
                    testTag = "security_confirm_password"
                )

                passwordError?.let { message ->
                    Text(text = message, color = RedError, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                }
                passwordSuccess?.let { message ->
                    Text(text = message, color = SecurityGreen, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                }

                SolidPillButton(
                    text = if (isChangingPassword) "Updating..." else "Update password",
                    buttonColor = SecurityGreen,
                    isLoading = isChangingPassword,
                    enabled = !isBusy,
                    onClick = { submitPasswordChange() },
                    testTag = "btn_change_password"
                )
                Text(
                    text = "Changing your password signs out every other device.",
                    fontSize = 12.5.sp,
                    color = SecurityMuted
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = SecurityLine)
            Spacer(Modifier.height(20.dp))

            // ── Sessions ──────────────────────────────────────────────────
            OutlinedButton(
                onClick = { showSignOutAllDialog = true },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, SecurityLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_sign_out_everywhere")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = SecurityInk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (isSigningOutAll) "Signing out..." else "Sign out of all devices",
                    color = SecurityInk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }

            Spacer(Modifier.height(26.dp))

            // ── Danger zone ───────────────────────────────────────────────
            SectionTitle(icon = Icons.Default.DeleteForever, text = "Delete account")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This permanently removes your account, saved papers and progress. " +
                    "It cannot be undone.",
                fontSize = 13.5.sp,
                color = SecurityMuted
            )
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = {
                    deletePassword = ""
                    deleteError = null
                    showDeleteDialog = true
                },
                enabled = !isBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = RedError),
                modifier = Modifier.testTag("btn_delete_account")
            ) {
                Text("Delete my account", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }

            Spacer(Modifier.height(28.dp))
        }

        // ── Dialogs ───────────────────────────────────────────────────────
        if (showSignOutAllDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSigningOutAll) showSignOutAllDialog = false },
                title = { Text("Sign out everywhere?") },
                text = {
                    Text(
                        "Every device signed in to this account will be signed out, " +
                            "including this one. You can sign back in at any time."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isSigningOutAll = true
                            showSignOutAllDialog = false
                            onSignOutEverywhere()
                        }
                    ) { Text("Sign out everywhere", color = RedError, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutAllDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = { Text("Delete your account?") },
                text = {
                    Column {
                        Text(
                            "Your account and all of its data will be erased. This cannot be undone.",
                            fontSize = 13.5.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        CleanLabeledTextField(
                            label = "Password (leave blank for Google sign-in)",
                            value = deletePassword,
                            placeholder = "Your password",
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            enabled = !isDeleting,
                            onValueChange = {
                                deletePassword = it
                                deleteError = null
                            },
                            testTag = "delete_account_password"
                        )
                        deleteError?.let { message ->
                            Spacer(Modifier.height(8.dp))
                            Text(text = message, color = RedError, fontSize = 13.5.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { confirmDelete() }, enabled = !isDeleting) {
                        Text(
                            text = if (isDeleting) "Deleting..." else "Delete permanently",
                            color = RedError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SecurityInk, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = SecurityInk
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.5.sp, color = SecurityMuted)
        Text(
            text = value,
            fontSize = 13.5.sp,
            color = SecurityInk,
            fontWeight = FontWeight.SemiBold
        )
    }
}
