package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.DigiWalletDatabase
import com.example.data.model.ThemeMode
import com.example.data.repository.CloudFirestoreRepository
import com.example.data.repository.DigiWalletRepository
import com.example.ui.components.BottomNavTab
import com.example.ui.components.DigiWalletBottomNavigationBar
import com.example.ui.components.LoadingState
import com.example.ui.screens.AddMoneyScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BudgetManagementScreen
import com.example.ui.screens.ExpenseManagementScreen
import com.example.ui.screens.FinancialAnalyticsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SendMoneyScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionDetailsScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.DigiWalletTheme
import com.example.viewmodel.DigiWalletUiState
import com.example.viewmodel.DigiWalletViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

sealed interface DigiWalletDestination {
    data class PrimaryTab(val tab: BottomNavTab) : DigiWalletDestination
    data object AddMoney : DigiWalletDestination
    data object SendMoney : DigiWalletDestination
    data object QrScanner : DigiWalletDestination
    data class TransactionDetails(val transactionId: String) : DigiWalletDestination
    data object ExpenseManagement : DigiWalletDestination
    data object BudgetManagement : DigiWalletDestination
    data object Notifications : DigiWalletDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val appContext = context.applicationContext
            val viewModel: DigiWalletViewModel = viewModel(
                factory = remember {
                    val roomDb = DigiWalletDatabase.getInstance(appContext)
                    val databaseId = appContext.getString(R.string.firestore_database_id)
                    val firestore = FirebaseFirestore.getInstance(databaseId)
                    val cloudRepo = CloudFirestoreRepository(firestore)
                    val repository = DigiWalletRepository(
                        context = appContext,
                        dao = roomDb.digiWalletDao(),
                        cloudRepo = cloudRepo
                    )
                    DigiWalletViewModel.Factory(repository)
                }
            )

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (uiState.sessionPrefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DigiWalletTheme(darkTheme = useDarkTheme) {
                DigiWalletAppRoot(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
fun DigiWalletAppRoot(
    viewModel: DigiWalletViewModel,
    uiState: DigiWalletUiState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSplash by remember { mutableStateOf(true) }
    var firebaseUser by remember {
        mutableStateOf(
            try {
                Firebase.auth.currentUser
            } catch (e: Exception) {
                null
            }
        )
    }

    DisposableEffect(Unit) {
        val authInstance = try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
            auth.currentUser?.let { user ->
                viewModel.onGoogleSignInSuccess(
                    uid = user.uid,
                    displayName = user.displayName,
                    email = user.email
                )
            }
        }
        authInstance?.addAuthStateListener(listener)
        onDispose {
            authInstance?.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearMessages()
        }
    }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    if (uiState.isLoading) {
        LoadingState()
        return
    }

    if (!uiState.sessionPrefs.hasCompletedOnboarding) {
        OnboardingScreen(
            onFinishOnboarding = { viewModel.completeOnboarding() }
        )
        return
    }

    val isAuthenticatedOrDemo = (firebaseUser != null) || uiState.sessionPrefs.isGuestDemoSession

    if (!isAuthenticatedOrDemo) {
        AuthScreen(
            onAuthSuccess = {
                firebaseUser = try {
                    Firebase.auth.currentUser
                } catch (e: Exception) {
                    null
                }
                firebaseUser?.let { user ->
                    viewModel.onGoogleSignInSuccess(user.uid, user.displayName, user.email)
                }
            },
            onContinueInDemoMode = {
                viewModel.setGuestDemoSession(true)
            }
        )
        return
    }

    var currentDestination by remember {
        mutableStateOf<DigiWalletDestination>(DigiWalletDestination.PrimaryTab(BottomNavTab.HOME))
    }

    val activeTab = (currentDestination as? DigiWalletDestination.PrimaryTab)?.tab

    // Handle system back navigation cleanly across secondary screens and non-Home tabs
    if (currentDestination !is DigiWalletDestination.PrimaryTab) {
        BackHandler {
            currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME)
        }
    } else if (activeTab != null && activeTab != BottomNavTab.HOME) {
        BackHandler {
            currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (activeTab != null) {
                DigiWalletBottomNavigationBar(
                    selectedTab = activeTab,
                    onTabSelected = { tab ->
                        currentDestination = DigiWalletDestination.PrimaryTab(tab)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val dest = currentDestination) {
                is DigiWalletDestination.PrimaryTab -> {
                    when (dest.tab) {
                        BottomNavTab.HOME -> {
                            HomeDashboardScreen(
                                uiState = uiState,
                                onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                                onNavigateToAddMoney = { currentDestination = DigiWalletDestination.AddMoney },
                                onNavigateToSendMoney = { currentDestination = DigiWalletDestination.SendMoney },
                                onNavigateToQrScanner = { currentDestination = DigiWalletDestination.QrScanner },
                                onNavigateToExpenseManagement = { currentDestination = DigiWalletDestination.ExpenseManagement },
                                onNavigateToBudgetManagement = { currentDestination = DigiWalletDestination.BudgetManagement },
                                onNavigateToTransactions = {
                                    currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.ACTIVITY)
                                },
                                onNavigateToNotifications = {
                                    currentDestination = DigiWalletDestination.Notifications
                                },
                                onNavigateToProfile = {
                                    currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.PROFILE)
                                },
                                onTransactionClick = { txId ->
                                    currentDestination = DigiWalletDestination.TransactionDetails(txId)
                                }
                            )
                        }
                        BottomNavTab.WALLET -> {
                            WalletScreen(
                                uiState = uiState,
                                onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                                onNavigateToAddMoney = { currentDestination = DigiWalletDestination.AddMoney },
                                onNavigateToSendMoney = { currentDestination = DigiWalletDestination.SendMoney },
                                onNavigateToTransactions = {
                                    currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.ACTIVITY)
                                },
                                onNavigateToSettings = {
                                    currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.PROFILE)
                                },
                                onResetDemoData = { viewModel.resetDemoData() }
                            )
                        }
                        BottomNavTab.ACTIVITY -> {
                            TransactionHistoryScreen(
                                transactions = uiState.transactions,
                                onTransactionClick = { txId ->
                                    currentDestination = DigiWalletDestination.TransactionDetails(txId)
                                }
                            )
                        }
                        BottomNavTab.INSIGHTS -> {
                            FinancialAnalyticsScreen(
                                uiState = uiState,
                                onNavigateToExpenses = {
                                    currentDestination = DigiWalletDestination.ExpenseManagement
                                },
                                onNavigateToBudgets = {
                                    currentDestination = DigiWalletDestination.BudgetManagement
                                }
                            )
                        }
                        BottomNavTab.PROFILE -> {
                            ProfileAndSettingsScreen(
                                uiState = uiState,
                                isAuthenticatedWithGoogle = firebaseUser != null,
                                onUpdateProfile = { name, upi -> viewModel.updateProfile(name, upi) },
                                onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                                onSetNotificationsEnabled = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                                onSetBiometricEnabled = { enabled -> viewModel.setBiometricLockEnabled(enabled) },
                                onResetDemoData = { viewModel.resetDemoData() },
                                onSignOut = {
                                    viewModel.setGuestDemoSession(false)
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        signOut(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onSignOutComplete = { firebaseUser = null },
                                            scope = scope
                                        )
                                    } catch (e: Exception) {
                                        firebaseUser = null
                                    }
                                }
                            )
                        }
                    }
                }

                DigiWalletDestination.AddMoney -> {
                    AddMoneyScreen(
                        isProcessing = uiState.isProcessingAction,
                        onBack = { currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME) },
                        onProceedToPay = { amountStr, method, mode ->
                            viewModel.addDemoMoney(amountStr, method, mode) { createdTx ->
                                currentDestination = DigiWalletDestination.TransactionDetails(createdTx.id)
                            }
                        }
                    )
                }

                DigiWalletDestination.SendMoney -> {
                    SendMoneyScreen(
                        uiState = uiState,
                        onBack = { currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME) },
                        onSendConfirmed = { recName, recUpi, amountStr, note ->
                            viewModel.sendDemoMoney(
                                recipientName = recName,
                                recipientUpi = recUpi,
                                amountRupeesInput = amountStr,
                                note = note
                            ) { createdTx ->
                                currentDestination = DigiWalletDestination.TransactionDetails(createdTx.id)
                            }
                        }
                    )
                }

                DigiWalletDestination.QrScanner -> {
                    QrScannerScreen(
                        isProcessing = uiState.isProcessingAction,
                        onBack = { currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME) },
                        onQrPaymentConfirmed = { merchantName, upiId, amountStr, note ->
                            viewModel.sendDemoMoney(
                                recipientName = merchantName,
                                recipientUpi = upiId,
                                amountRupeesInput = amountStr,
                                category = "Food & Dining",
                                note = note
                            ) { createdTx ->
                                currentDestination = DigiWalletDestination.TransactionDetails(createdTx.id)
                            }
                        }
                    )
                }

                is DigiWalletDestination.TransactionDetails -> {
                    val tx = uiState.transactions.firstOrNull { it.id == dest.transactionId }
                    TransactionDetailsScreen(
                        transaction = tx,
                        onBack = {
                            currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME)
                        }
                    )
                }

                DigiWalletDestination.ExpenseManagement -> {
                    ExpenseManagementScreen(
                        expenses = uiState.expenses,
                        onBack = { currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME) },
                        onSaveExpense = { amt, cat, merchant, note ->
                            viewModel.saveExpense(amt, cat, merchant, note) {}
                        },
                        onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                    )
                }

                DigiWalletDestination.BudgetManagement -> {
                    BudgetManagementScreen(
                        uiState = uiState,
                        onBack = { currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME) },
                        onSaveBudget = { cat, limit ->
                            viewModel.saveBudget(cat, limit) {}
                        }
                    )
                }

                DigiWalletDestination.Notifications -> {
                    NotificationsScreen(
                        notifications = uiState.notifications,
                        onBack = { currentDestination = DigiWalletDestination.PrimaryTab(BottomNavTab.HOME) },
                        onMarkRead = { id -> viewModel.markNotificationRead(id) },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() }
                    )
                }
            }
        }
    }
}
