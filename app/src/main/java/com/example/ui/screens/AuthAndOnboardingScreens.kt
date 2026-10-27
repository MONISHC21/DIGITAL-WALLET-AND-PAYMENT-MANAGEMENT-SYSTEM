package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.CustomTextField
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandIndigoDark
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.SemanticSuccess
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF191847), Color(0xFF0E0D26))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(26.dp),
                color = BrandIndigo,
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "DigiWallet Emblem",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "DigiWallet",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your Money. Your Control.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

private data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val badge: String
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "One App for\nAll Your Payments",
                subtitle = "Send, Receive, Track and Manage your money easily with full clarity.",
                badge = "Smart Digital Wallet"
            ),
            OnboardingPageData(
                title = "Track Expenses &\nMonthly Budgets",
                subtitle = "Categorize every rupee, set monthly spending limits, and receive proactive alerts.",
                badge = "Intelligent Insights"
            ),
            OnboardingPageData(
                title = "Safe Demo Mode &\nCloud Sync Ready",
                subtitle = "Test simulated INR transactions safely or sign in with Google to sync across devices.",
                badge = "Bank-Grade Security"
            )
        )
    }
    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Fintech vector illustration canvas matching the reference poster
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape)
                .background(BrandIndigo.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                // Soft purple glow
                drawCircle(
                    color = Color(0xFF7768F8).copy(alpha = 0.16f),
                    radius = size.minDimension * 0.44f
                )
                // Phone frame
                drawRoundRect(
                    color = Color(0xFF232738),
                    topLeft = Offset(size.width * 0.22f, size.height * 0.12f),
                    size = Size(size.width * 0.44f, size.height * 0.72f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                // Screen inside phone
                drawRoundRect(
                    color = Color(0xFF5146E5),
                    topLeft = Offset(size.width * 0.25f, size.height * 0.16f),
                    size = Size(size.width * 0.38f, size.height * 0.64f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                // Floating wallet card
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(size.width * 0.48f, size.height * 0.44f),
                    size = Size(size.width * 0.36f, size.height * 0.28f),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                // Coin circle
                drawCircle(
                    color = Color(0xFF16845B),
                    radius = 24f,
                    center = Offset(size.width * 0.52f, size.height * 0.76f)
                )
            }
        }

        val page = pages[currentPage]
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = BrandIndigo.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = page.badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandIndigo,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onFinishOnboarding,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (idx == currentPage) 22.dp else 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (idx == currentPage) BrandIndigo
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        currentPage++
                    } else {
                        onFinishOnboarding()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                modifier = Modifier.testTag("onboarding_next_button")
            ) {
                Text(text = if (currentPage < pages.lastIndex) "Next" else "Get Started")
            }
        }
    }
}

private enum class AuthViewMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueInDemoMode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var authMode by remember { mutableStateOf(AuthViewMode.LOGIN) }
    var fullName by remember { mutableStateOf("Monish") }
    var email by remember { mutableStateOf("monish@gmail.com") }
    var password by remember { mutableStateOf("password123") }
    var showPassword by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        AnimatedContent(targetState = authMode, label = "auth_mode_switch") { mode ->
            Column(modifier = Modifier.fillMaxWidth()) {
                when (mode) {
                    AuthViewMode.LOGIN -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in to your DigiWallet account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CustomTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = "Email",
                            placeholder = "monish@gmail.com",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "login_email_input"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CustomTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = "Password",
                            placeholder = "••••••••••",
                            leadingIcon = Icons.Default.Lock,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingContent = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            testTag = "login_password_input"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    errorMessage = null
                                    statusMessage = null
                                    authMode = AuthViewMode.FORGOT_PASSWORD
                                }
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PrimaryButton(
                            text = "Login",
                            onClick = {
                                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    errorMessage = "Please enter a valid email address."
                                } else if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                } else {
                                    errorMessage = null
                                    statusMessage = "AI Studio provisions Google Sign-In for live Cloud Firestore security rules. Tap 'Sign in with Google' below for live cloud sync, or 'Explore Demo Prototype' for offline Room persistence."
                                }
                            },
                            testTag = "login_submit_button"
                        )
                    }

                    AuthViewMode.REGISTER -> {
                        IconButton(
                            onClick = { authMode = AuthViewMode.LOGIN },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Login"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Join DigiWallet today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        CustomTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                errorMessage = null
                            },
                            label = "Full Name",
                            placeholder = "Monish",
                            leadingIcon = Icons.Default.Person,
                            testTag = "register_name_input"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CustomTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = "Email",
                            placeholder = "monish@gmail.com",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "register_email_input"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CustomTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = "Password",
                            placeholder = "••••••••••",
                            leadingIcon = Icons.Default.Lock,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingContent = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            testTag = "register_password_input"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = BrandIndigo)
                            )
                            Text(
                                text = "I agree to the Terms & Conditions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        PrimaryButton(
                            text = "Sign Up",
                            onClick = {
                                if (fullName.isBlank()) {
                                    errorMessage = "Please enter your full name."
                                } else if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    errorMessage = "Please enter a valid email address."
                                } else if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                } else if (!termsAccepted) {
                                    errorMessage = "Please accept the Terms & Conditions."
                                } else {
                                    errorMessage = null
                                    statusMessage = "Account details validated! Use 'Sign in with Google' below to provision your authenticated Cloud Firestore profile, or continue in Demo Prototype mode."
                                }
                            },
                            testTag = "register_submit_button"
                        )
                    }

                    AuthViewMode.FORGOT_PASSWORD -> {
                        IconButton(
                            onClick = { authMode = AuthViewMode.LOGIN },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Login"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(92.dp),
                                shape = CircleShape,
                                color = BrandIndigo.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Password Recovery",
                                        tint = BrandIndigo,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your email to reset password",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CustomTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = "Email",
                            placeholder = "monish@gmail.com",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "forgot_email_input"
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryButton(
                            text = "Send Reset Link",
                            onClick = {
                                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    errorMessage = "Please enter a valid email address."
                                } else {
                                    errorMessage = null
                                    Firebase.auth.sendPasswordResetEmail(email.trim())
                                        .addOnSuccessListener {
                                            statusMessage = "Password reset instructions sent to ${email.trim()}."
                                        }
                                        .addOnFailureListener {
                                            statusMessage = "Password reset checked for ${email.trim()}. Note: Sign in with Google uses your Google account security directly."
                                        }
                                }
                            },
                            testTag = "forgot_submit_button"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { authMode = AuthViewMode.LOGIN },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Back to Login", color = BrandIndigo)
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandIndigo.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Info",
                        tint = BrandIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  Or continue with  ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mandatory Google Sign-In button via Jetpack Credential Manager
        Button(
            onClick = {
                errorMessage = null
                isGoogleLoading = true
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isGoogleLoading = false
                        onAuthSuccess()
                    },
                    onAuthError = { msg ->
                        isGoogleLoading = false
                        errorMessage = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        isGoogleLoading = false
                    }
                )
            },
            enabled = !isGoogleLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BrandIndigo,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = BrandIndigo
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onContinueInDemoMode,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("continue_demo_mode_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Explore Interactive Demo Prototype (₹12,450)",
                style = MaterialTheme.typography.labelLarge,
                color = BrandIndigo
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (authMode == AuthViewMode.LOGIN) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandIndigo,
                    modifier = Modifier
                        .clickable {
                            errorMessage = null
                            statusMessage = null
                            authMode = AuthViewMode.REGISTER
                        }
                        .padding(4.dp)
                        .testTag("switch_to_register_button")
                )
            } else {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandIndigo,
                    modifier = Modifier
                        .clickable {
                            errorMessage = null
                            statusMessage = null
                            authMode = AuthViewMode.LOGIN
                        }
                        .padding(4.dp)
                        .testTag("switch_to_login_button")
                )
            }
        }
    }
}
