package com.soumit.mine.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soumit.mine.home.CreateScreen
import com.soumit.mine.home.CreateStudentScreen
import com.soumit.mine.home.HomeScreen
import com.soumit.mine.home.HomeViewModel
import com.soumit.mine.ui.theme.MineTheme

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

enum class MainScreenState {
    HOME,
    CREATE,
    CREATE_STUDENT
}

/**
 * Stateful composable wired to AuthViewModel.
 */
@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by rememberSaveable { mutableStateOf(MainScreenState.HOME) }
    val homeViewModel: HomeViewModel = viewModel()
    val tuitionUiState by homeViewModel.uiState.collectAsState()

    if (uiState.currentUser != null) {
        when (currentScreen) {
            MainScreenState.HOME -> {
                HomeScreen(
                    userName = uiState.currentUser?.displayName,
                    userEmail = uiState.currentUser?.email,
                    students = tuitionUiState.students,
                    payments = tuitionUiState.payments,
                    onNavigateToCreate = { currentScreen = MainScreenState.CREATE },
                    onRescheduleStudent = { studentId, oldDay, newDay, newTime ->
                        homeViewModel.updateStudentSchedule(studentId, oldDay, newDay, newTime)
                    },
                    onDeleteScheduleSlot = { studentId, day, startTime ->
                        homeViewModel.deleteScheduleSlot(studentId, day, startTime)
                    },
                    onDeleteStudent = { id -> homeViewModel.deleteStudent(id) },
                    onRecordPayment = { studentId, studentName, amount, date, notes ->
                        homeViewModel.recordPayment(studentId, studentName, amount, date, notes)
                    },
                    onDeletePayment = { paymentId -> homeViewModel.deletePayment(paymentId) },
                    onSignOut = { viewModel.signOut() },
                    modifier = modifier
                )
            }
            MainScreenState.CREATE -> {
                CreateScreen(
                    students = tuitionUiState.students,
                    onBack = { currentScreen = MainScreenState.HOME },
                    onNavigateToCreateStudent = { currentScreen = MainScreenState.CREATE_STUDENT },
                    onRecordPayment = { studentId, studentName, amount, date, notes ->
                        homeViewModel.recordPayment(studentId, studentName, amount, date, notes)
                    },
                    onAddScheduleSlot = { studentId, day, time, duration ->
                        val student = tuitionUiState.students.find { it.id == studentId }
                        if (student != null) {
                            val updated = student.weeklySchedule + com.soumit.mine.home.DaySchedule(day = day, startTime = time, duration = duration)
                            homeViewModel.addStudent(student.copy(weeklySchedule = updated))
                        }
                    },
                    modifier = modifier
                )
            }
            MainScreenState.CREATE_STUDENT -> {
                CreateStudentScreen(
                    onBack = { currentScreen = MainScreenState.CREATE },
                    onSaveStudent = { student ->
                        homeViewModel.addStudent(
                            student = student,
                            onSuccess = { currentScreen = MainScreenState.HOME }
                        )
                    },
                    isLoading = tuitionUiState.isLoading,
                    modifier = modifier
                )
            }
        }
    } else {
        AuthScreen(
            uiState = uiState,
            onSignInEmail = { email, pass -> viewModel.signInWithEmail(email, pass) },
            onSignUpEmail = { name, email, pass, confirm ->
                viewModel.signUpWithEmail(name, email, pass, confirm)
            },
            onForgotPassword = { email -> viewModel.sendPasswordReset(email) },
            onDismissError = { viewModel.clearMessages() },
            modifier = modifier
        )
    }
}

/**
 * Pure Stateless AuthScreen composable, easily previewable and testable.
 */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    modifier: Modifier = Modifier,
    initialMode: AuthMode = AuthMode.SIGN_IN,
    onSignInEmail: (email: String, pass: String) -> Unit,
    onSignUpEmail: (name: String, email: String, pass: String, confirm: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onDismissError: () -> Unit
) {
    var authMode by rememberSaveable { mutableStateOf(initialMode) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Brand Emblem & Header
                AuthHeader(authMode = authMode)

                Spacer(modifier = Modifier.height(28.dp))

                // Mode Selector Tab (Sign In vs Sign Up)
                AuthModeSelector(
                    selectedMode = authMode,
                    onModeSelected = {
                        authMode = it
                        onDismissError()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error / Success Banners
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    uiState.errorMessage?.let { msg ->
                        AuthMessageBanner(
                            message = msg,
                            isError = true,
                            onDismiss = onDismissError,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.successMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    uiState.successMessage?.let { msg ->
                        AuthMessageBanner(
                            message = msg,
                            isError = false,
                            onDismiss = onDismissError,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                // Input Fields: Name (Sign Up only)
                AnimatedVisibility(
                    visible = authMode == AuthMode.SIGN_UP,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("John Doe") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("you@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (authMode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = {
                            focusManager.clearFocus()
                            if (authMode == AuthMode.SIGN_IN) {
                                onSignInEmail(email, password)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Confirm Password (Sign Up only)
                AnimatedVisibility(
                    visible = authMode == AuthMode.SIGN_UP,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSignUpEmail(name, email, password, confirmPassword)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                // Forgot Password link for Sign In
                if (authMode == AuthMode.SIGN_IN) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showForgotPasswordDialog = true }
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Action Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (authMode == AuthMode.SIGN_IN) {
                            onSignInEmail(email, password)
                        } else {
                            onSignUpEmail(name, email, password, confirmPassword)
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (authMode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Footer Prompt
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (authMode == AuthMode.SIGN_IN) "Don't have an account? " else "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (authMode == AuthMode.SIGN_IN) "Sign Up" else "Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable {
                            authMode = if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
                            onDismissError()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onSendReset = { resetEmail ->
                onForgotPassword(resetEmail)
                showForgotPasswordDialog = false
            }
        )
    }
}

/**
 * Top brand header with modern glowing emblem.
 */
@Composable
private fun AuthHeader(
    authMode: AuthMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing brand emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = authMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        expandVertically(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            label = "AuthHeaderTitle"
        ) { mode ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (mode == AuthMode.SIGN_IN) "Welcome Back" else "Create Account",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (mode == AuthMode.SIGN_IN)
                        "Enter your credentials to access your account"
                    else
                        "Join Mine today to unlock all your features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Modern Segmented Pill selector between Sign In and Sign Up.
 */
@Composable
private fun AuthModeSelector(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthSelectorTab(
                text = "Sign In",
                isSelected = selectedMode == AuthMode.SIGN_IN,
                onClick = { onModeSelected(AuthMode.SIGN_IN) },
                modifier = Modifier.weight(1f)
            )
            AuthSelectorTab(
                text = "Sign Up",
                isSelected = selectedMode == AuthMode.SIGN_UP,
                onClick = { onModeSelected(AuthMode.SIGN_UP) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthSelectorTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "TabBackground"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "TabTextColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = textColor
        )
    }
}

/**
 * Elegant Error / Success Banner.
 */
@Composable
private fun AuthMessageBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }

    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Dialog for sending password reset link.
 */
@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your registered email address and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendReset(email) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send Reset Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Sign In - Light", showBackground = true)
@Composable
fun AuthScreenSignInLightPreview() {
    MineTheme(darkTheme = false) {
        AuthScreen(
            uiState = AuthUiState(),
            initialMode = AuthMode.SIGN_IN,
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}

@Preview(name = "Sign In - Dark", showBackground = true)
@Composable
fun AuthScreenSignInDarkPreview() {
    MineTheme(darkTheme = true) {
        AuthScreen(
            uiState = AuthUiState(),
            initialMode = AuthMode.SIGN_IN,
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}

@Preview(name = "Sign Up - Light", showBackground = true)
@Composable
fun AuthScreenSignUpLightPreview() {
    MineTheme(darkTheme = false) {
        AuthScreen(
            uiState = AuthUiState(),
            initialMode = AuthMode.SIGN_UP,
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}

@Preview(name = "Sign Up - Dark", showBackground = true)
@Composable
fun AuthScreenSignUpDarkPreview() {
    MineTheme(darkTheme = true) {
        AuthScreen(
            uiState = AuthUiState(),
            initialMode = AuthMode.SIGN_UP,
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}

@Preview(name = "Sign In - Loading", showBackground = true)
@Composable
fun AuthScreenLoadingPreview() {
    MineTheme {
        AuthScreen(
            uiState = AuthUiState(isLoading = true),
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}

@Preview(name = "Sign In - Error State", showBackground = true)
@Composable
fun AuthScreenErrorPreview() {
    MineTheme {
        AuthScreen(
            uiState = AuthUiState(
                errorMessage = "Incorrect email or password. Please try again."
            ),
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}

@Preview(name = "Forgot Password Dialog", showBackground = true)
@Composable
fun ForgotPasswordDialogPreview() {
    MineTheme {
        ForgotPasswordDialog(
            initialEmail = "alex@example.com",
            onDismiss = {},
            onSendReset = {}
        )
    }
}
