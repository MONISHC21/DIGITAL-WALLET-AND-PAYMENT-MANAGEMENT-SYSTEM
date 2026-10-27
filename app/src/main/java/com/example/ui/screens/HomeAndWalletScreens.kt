package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencyUtils
import com.example.data.remote.PaymentEnvironmentMode
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CustomTextField
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.QuickActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingSummaryCard
import com.example.ui.components.TransactionItem
import com.example.ui.components.WalletBalanceCard
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.SemanticSuccess
import com.example.viewmodel.DigiWalletUiState

@Composable
fun HomeDashboardScreen(
    uiState: DigiWalletUiState,
    onToggleBalanceVisibility: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    onNavigateToSendMoney: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToExpenseManagement: () -> Unit,
    onNavigateToBudgetManagement: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val firstName = uiState.sessionPrefs.displayName.substringBefore(" ").ifEmpty { "Monish" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onNavigateToProfile)
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = BrandIndigo
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = firstName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hi $firstName!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Good morning • ${uiState.sessionPrefs.upiHandle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadNotificationCount > 0) {
                                    Badge(containerColor = BrandIndigo) {
                                        Text(
                                            text = uiState.unreadNotificationCount.toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Wallet Balance Card
        item {
            WalletBalanceCard(
                title = "Total Balance",
                balancePaise = uiState.sessionPrefs.demoBalancePaise,
                isBalanceVisible = uiState.sessionPrefs.isBalanceVisible,
                onToggleVisibility = onToggleBalanceVisibility
            )
        }

        // 3. Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "Add Money",
                    onClick = onNavigateToAddMoney,
                    testTag = "quick_action_add_money",
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Send Money",
                    onClick = onNavigateToSendMoney,
                    testTag = "quick_action_send_money",
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan QR",
                    onClick = onNavigateToQrScanner,
                    testTag = "quick_action_scan_qr",
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.ElectricBolt,
                    label = "Pay Bills",
                    onClick = onNavigateToExpenseManagement,
                    testTag = "quick_action_pay_bills",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Financial Overview (Income, Expenses, Savings)
        item {
            SpendingSummaryCard(
                incomePaise = uiState.totalIncomePaise,
                expensePaise = uiState.totalTxExpensePaise,
                savingsPaise = uiState.netSavingsPaise
            )
        }

        // 5. Recent Transactions Section
        item {
            SectionHeader(
                title = "Recent Transactions",
                actionLabel = "View All",
                onActionClick = onNavigateToTransactions
            )
        }

        if (uiState.transactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No transactions yet",
                    subtitle = "Add or send simulated demo money to see your recent activity here.",
                    actionLabel = "Add Demo Money",
                    onActionClick = onNavigateToAddMoney
                )
            }
        } else {
            items(
                items = uiState.transactions.take(4),
                key = { it.id }
            ) { tx ->
                TransactionItem(
                    transaction = tx,
                    onClick = { onTransactionClick(tx.id) }
                )
            }
        }

        // 6. Monthly Budget Snapshot
        if (uiState.budgetStatuses.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Monthly Budgets",
                    actionLabel = "Manage",
                    onActionClick = onNavigateToBudgetManagement
                )
            }
            items(
                items = uiState.budgetStatuses.take(2),
                key = { it.budget.id }
            ) { budgetStatus ->
                BudgetProgressCard(
                    statusItem = budgetStatus,
                    onClick = onNavigateToBudgetManagement
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
fun WalletScreen(
    uiState: DigiWalletUiState,
    onToggleBalanceVisibility: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    onNavigateToSendMoney: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onResetDemoData: () -> Unit
) {
    var showReceiveDialog by remember { mutableStateOf(false) }
    var showWalletDetailsDialog by remember { mutableStateOf(false) }
    var showLinkedAccountsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("wallet_screen")
    ) {
        Text(
            text = "My Wallet",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        WalletBalanceCard(
            title = "Available Balance",
            balancePaise = uiState.sessionPrefs.demoBalancePaise,
            isBalanceVisible = uiState.sessionPrefs.isBalanceVisible,
            onToggleVisibility = onToggleBalanceVisibility
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Add Money",
                onClick = onNavigateToAddMoney,
                testTag = "wallet_add_money",
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.Send,
                label = "Send Money",
                onClick = onNavigateToSendMoney,
                testTag = "wallet_send_money",
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Download,
                label = "Receive",
                onClick = { showReceiveDialog = true },
                testTag = "wallet_receive_money",
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Refresh,
                label = "Reset Demo",
                onClick = onResetDemoData,
                testTag = "wallet_reset_demo",
                modifier = Modifier.weight(1f),
                containerColor = BrandPurple
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        WalletMenuRow(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Wallet Details",
            subtitle = "UPI ID: ${uiState.sessionPrefs.upiHandle} • Demo Ledger",
            onClick = { showWalletDetailsDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))
        WalletMenuRow(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "Transaction History",
            subtitle = "${uiState.transactions.size} recorded demo transactions",
            onClick = onNavigateToTransactions
        )
        Spacer(modifier = Modifier.height(12.dp))
        WalletMenuRow(
            icon = Icons.Default.AccountBalance,
            title = "Linked Accounts",
            subtitle = "Demo Bank & UPI payment methods",
            onClick = { showLinkedAccountsDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))
        WalletMenuRow(
            icon = Icons.Default.Settings,
            title = "Wallet Settings",
            subtitle = "Theme, notifications, and demo controls",
            onClick = onNavigateToSettings
        )
    }

    if (showReceiveDialog) {
        AlertDialog(
            onDismissRequest = { showReceiveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Receive QR",
                    tint = BrandIndigo,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Receive Demo Payment") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Share your simulated DigiWallet UPI ID to receive demo transfers:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = BrandIndigo.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = uiState.sessionPrefs.upiHandle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandIndigo,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReceiveDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showWalletDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showWalletDetailsDialog = false },
            title = { Text("Wallet Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account Holder: ${uiState.sessionPrefs.displayName}")
                    Text("UPI Handle: ${uiState.sessionPrefs.upiHandle}")
                    Text("Simulated Balance: ${CurrencyUtils.formatPaise(uiState.sessionPrefs.demoBalancePaise, showDecimals = true)}")
                    Text("Environment: Demo Simulation Mode (Integer Paise Ledger)")
                    Text(
                        "Note: Simulated demo funds are stored for testing financial workflows and are never mixed with real bank funds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWalletDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLinkedAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showLinkedAccountsDialog = false },
            title = { Text("Simulated Payment Rails") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• UPI Instant Sandbox (${uiState.sessionPrefs.upiHandle})")
                    Text("• Demo HDFC Savings Account •••• 4821")
                    Text("• Demo Visa Platinum Debit •••• 9014")
                    Text(
                        "Real payment gateway credentials are kept strictly on the backend and never embedded in the APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkedAccountsDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun WalletMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = BrandIndigo.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = BrandIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneyScreen(
    isProcessing: Boolean,
    onBack: () -> Unit,
    onProceedToPay: (amountRupees: String, paymentMethod: String, mode: PaymentEnvironmentMode) -> Unit
) {
    var amountInput by remember { mutableStateOf("1000") }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var selectedMode by remember { mutableStateOf(PaymentEnvironmentMode.DEMO_SIMULATION) }
    val presets = listOf("500", "1000", "2000", "5000")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("add_money_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Add Money",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter Amount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                CustomTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountInput = input
                        }
                    },
                    label = "Amount in INR (₹)",
                    placeholder = "1000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "add_money_amount_input"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = amountInput == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { amountInput = preset },
                            label = {
                                Text(
                                    text = "₹$preset",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandIndigo,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_amount_$preset")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        PaymentMethodOptionCard(
            title = "UPI",
            subtitle = "(Google Pay, PhonePe, etc.)",
            icon = Icons.Default.QrCode2,
            selected = selectedMethod == "UPI",
            onClick = { selectedMethod = "UPI" }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PaymentMethodOptionCard(
            title = "Debit Card",
            subtitle = "Visa, Mastercard, RuPay",
            icon = Icons.Default.CreditCard,
            selected = selectedMethod == "Debit Card",
            onClick = { selectedMethod = "Debit Card" }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PaymentMethodOptionCard(
            title = "Net Banking",
            subtitle = "All major Indian banks",
            icon = Icons.Default.AccountBalance,
            selected = selectedMethod == "Net Banking",
            onClick = { selectedMethod = "Net Banking" }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Mode switcher (Demo Simulation vs Trusted Backend Verification)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandIndigo.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedMode == PaymentEnvironmentMode.DEMO_SIMULATION)
                            "Mode: Simulated Demo Top-Up"
                        else
                            "Mode: Live Payment Gateway Check",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandIndigo
                    )
                    Text(
                        text = "Never treats unverified client callbacks as real funds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        selectedMode = if (selectedMode == PaymentEnvironmentMode.DEMO_SIMULATION)
                            PaymentEnvironmentMode.TRUSTED_BACKEND_READY
                        else
                            PaymentEnvironmentMode.DEMO_SIMULATION
                    }
                ) {
                    Text("Switch")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Proceed to Pay",
            isLoading = isProcessing,
            onClick = {
                onProceedToPay(amountInput, selectedMethod, selectedMode)
            },
            testTag = "proceed_to_pay_button"
        )
    }
}

@Composable
private fun PaymentMethodOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) BrandIndigo else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = BrandIndigo.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = BrandIndigo
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BrandIndigo)
            )
        }
    }
}

@Composable
fun SendMoneyScreen(
    uiState: DigiWalletUiState,
    onBack: () -> Unit,
    onSendConfirmed: (recipientName: String, recipientUpi: String, amountRupees: String, note: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("Rahul") }
    var recipientUpi by remember { mutableStateOf("rahul@upi") }
    var amountInput by remember { mutableStateOf("500") }
    var noteInput by remember { mutableStateOf("Lunch money") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val filteredRecipients = remember(searchQuery, uiState.demoRecipients) {
        if (searchQuery.isBlank()) uiState.demoRecipients
        else uiState.demoRecipients.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.upiId.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("send_money_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Send Money",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Find Recipient",
            placeholder = "Search by name, phone or UPI ID",
            leadingIcon = Icons.Default.Search,
            testTag = "send_money_search_input"
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Recent",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredRecipients, key = { it.id }) { rec ->
                val isSelected = recipientUpi.equals(rec.upiId, ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            recipientName = rec.name
                            recipientUpi = rec.upiId
                        }
                        .padding(6.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) BrandIndigo else Color.Transparent,
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        color = Color(rec.avatarColorHex)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = rec.initials,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rec.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Enter Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                CustomTextField(
                    value = recipientUpi,
                    onValueChange = {
                        recipientUpi = it
                        val matched = uiState.demoRecipients.firstOrNull { r -> r.upiId.equals(it.trim(), ignoreCase = true) }
                        if (matched != null) {
                            recipientName = matched.name
                        } else if (it.contains("@")) {
                            recipientName = it.substringBefore("@").replaceFirstChar { c -> c.uppercase() }
                        }
                    },
                    label = "Recipient UPI ID",
                    placeholder = "rahul@upi",
                    testTag = "send_money_upi_input"
                )

                CustomTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountInput = input
                        }
                    },
                    label = "Amount (₹)",
                    placeholder = "500",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "send_money_amount_input"
                )

                CustomTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = "Add a note (optional)",
                    placeholder = "Lunch money",
                    testTag = "send_money_note_input"
                )

                Text(
                    text = "Available Demo Balance: ${CurrencyUtils.formatPaise(uiState.sessionPrefs.demoBalancePaise, showDecimals = true)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SemanticSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Send Money",
            isLoading = uiState.isProcessingAction,
            onClick = { showConfirmDialog = true },
            testTag = "send_money_submit_button"
        )
    }

    if (showConfirmDialog) {
        val parsedPaise = CurrencyUtils.parseRupeesToPaise(amountInput) ?: 0L
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Demo Transfer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recipient: $recipientName ($recipientUpi)")
                    Text(
                        text = "Amount: ${CurrencyUtils.formatPaise(parsedPaise, showDecimals = true)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandIndigo
                    )
                    if (noteInput.isNotBlank()) {
                        Text("Note: $noteInput")
                    }
                    Text(
                        text = "This transfer updates your simulated DigiWallet balance and logs a demo receipt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSendConfirmed(recipientName, recipientUpi, amountInput, noteInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                    modifier = Modifier.testTag("confirm_send_money_button")
                ) {
                    Text("Confirm & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
