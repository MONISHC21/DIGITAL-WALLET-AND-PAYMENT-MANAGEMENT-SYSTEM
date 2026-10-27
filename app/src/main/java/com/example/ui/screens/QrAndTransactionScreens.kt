package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencyUtils
import com.example.data.model.ParsedQrPayload
import com.example.data.repository.DigiWalletRepository
import com.example.ui.components.CustomTextField
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.TransactionItem
import com.example.ui.components.categoryVisuals
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticSuccessBg
import com.example.ui.theme.SemanticWarning

@Composable
fun QrScannerScreen(
    isProcessing: Boolean,
    onBack: () -> Unit,
    onQrPaymentConfirmed: (merchantName: String, upiId: String, amountRupees: String, note: String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var parsedQr by remember { mutableStateOf<ParsedQrPayload?>(null) }
    var qrErrorMsg by remember { mutableStateOf<String?>(null) }

    // If a valid QR is scanned, transition to Payment Review Screen (never auto-execute!)
    if (parsedQr != null && parsedQr!!.isValid) {
        QrPaymentReviewScreen(
            payload = parsedQr!!,
            isProcessing = isProcessing,
            onBackToScanner = { parsedQr = null },
            onConfirmPayment = onQrPaymentConfirmed
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser")
    val scanLineFraction by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10121B))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("qr_scanner_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasCameraPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191C29))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission",
                        tint = BrandIndigo,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (permissionDeniedOnce)
                            "Camera permission was denied. Grant access or select a Demo Merchant QR below."
                        else
                            "Allow camera access to scan UPI QR codes, or test immediately with our Demo Merchant QR presets below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PrimaryButton(
                        text = "Grant Camera Permission",
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        testTag = "request_camera_permission_button"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // QR Viewfinder Box matching reference poster
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF191C29)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Preview",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(160.dp)
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val cornerLen = 36.dp.toPx()
                val pad = 24.dp.toPx()
                val w = size.width
                val h = size.height

                // Top-left corner
                drawLine(BrandIndigo, Offset(pad, pad), Offset(pad + cornerLen, pad), stroke, StrokeCap.Round)
                drawLine(BrandIndigo, Offset(pad, pad), Offset(pad, pad + cornerLen), stroke, StrokeCap.Round)
                // Top-right corner
                drawLine(BrandIndigo, Offset(w - pad, pad), Offset(w - pad - cornerLen, pad), stroke, StrokeCap.Round)
                drawLine(BrandIndigo, Offset(w - pad, pad), Offset(w - pad, pad + cornerLen), stroke, StrokeCap.Round)
                // Bottom-left corner
                drawLine(BrandIndigo, Offset(pad, h - pad), Offset(pad + cornerLen, h - pad), stroke, StrokeCap.Round)
                drawLine(BrandIndigo, Offset(pad, h - pad), Offset(pad, h - pad - cornerLen), stroke, StrokeCap.Round)
                // Bottom-right corner
                drawLine(BrandIndigo, Offset(w - pad, h - pad), Offset(w - pad - cornerLen, h - pad), stroke, StrokeCap.Round)
                drawLine(BrandIndigo, Offset(w - pad, h - pad), Offset(w - pad, h - pad - cornerLen), stroke, StrokeCap.Round)

                // Animated laser scan line
                val lineY = pad + (h - 2 * pad) * scanLineFraction
                drawLine(
                    color = Color(0xFF7768F8),
                    start = Offset(pad + 8f, lineY),
                    end = Offset(w - pad - 8f, lineY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Position the QR code within the frame",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Flash & Gallery Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (flashEnabled) BrandIndigo else Color(0xFF232738),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Flash", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val payload = DigiWalletRepository.parseUpiQrPayload(
                            "upi://pay?pa=starbucks@upi&pn=Starbucks%20Coffee&am=250&tn=Coffee%20%26%20Snacks"
                        )
                        qrErrorMsg = null
                        parsedQr = payload
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF232738), CircleShape)
                        .testTag("scan_demo_starbucks_qr")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick QR from Gallery",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Gallery QR", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        if (qrErrorMsg != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SemanticError.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = qrErrorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo QR Simulation Presets for Emulator Testing
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191C29))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Simulate Demo QR Scan (Emulator Friendly)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                PrimaryButton(
                    text = "Scan Starbucks Coffee QR (₹250)",
                    onClick = {
                        qrErrorMsg = null
                        parsedQr = DigiWalletRepository.parseUpiQrPayload(
                            "upi://pay?pa=starbucks@upi&pn=Starbucks%20Coffee&am=250&tn=Coffee%20%26%20Snacks"
                        )
                    },
                    testTag = "simulate_starbucks_qr_button"
                )
                OutlinedButton(
                    onClick = {
                        qrErrorMsg = null
                        parsedQr = DigiWalletRepository.parseUpiQrPayload(
                            "upi://pay?pa=more.supermarket@upi&pn=Grocery%20Store&am=450&tn=Fresh%20Produce"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan Grocery Store QR (₹450)", color = Color.White)
                }
                OutlinedButton(
                    onClick = {
                        val invalid = DigiWalletRepository.parseUpiQrPayload("https://untrusted.example.com/malicious")
                        qrErrorMsg = invalid.errorMessage
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_invalid_qr_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Test Invalid / Untrusted QR Payload", color = Color(0xFFFCA5A5))
                }
            }
        }
    }
}

@Composable
fun QrPaymentReviewScreen(
    payload: ParsedQrPayload,
    isProcessing: Boolean,
    onBackToScanner: () -> Unit,
    onConfirmPayment: (merchantName: String, upiId: String, amountRupees: String, note: String) -> Unit
) {
    val defaultRupees = payload.suggestedAmountPaise?.let { (it / 100L).toString() } ?: "250"
    var amountInput by remember { mutableStateOf(defaultRupees) }
    var noteInput by remember { mutableStateOf(payload.note.ifEmpty { "Coffee & Snacks" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("qr_payment_review_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToScanner) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Scanner"
                )
            }
            Text(
                text = "Payment Details",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = SemanticSuccess.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = payload.merchantName,
                            tint = SemanticSuccess,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = payload.merchantName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UPI ID: ${payload.upiId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                CustomTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountInput = input
                        }
                    },
                    label = "Amount (₹)",
                    placeholder = "250",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "qr_payment_amount_input"
                )

                Spacer(modifier = Modifier.height(14.dp))

                CustomTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = "Transaction Note",
                    placeholder = "Coffee & Snacks",
                    testTag = "qr_payment_note_input"
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Verified QR Format • Simulated Demo Payment",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandIndigo
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Pay ₹${amountInput.ifBlank { "0" }}",
            isLoading = isProcessing,
            onClick = {
                onConfirmPayment(payload.merchantName, payload.upiId, amountInput, noteInput)
            },
            testTag = "qr_confirm_pay_button"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    transactions: List<TransactionEntity>,
    onBack: (() -> Unit)? = null,
    onTransactionClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Income", "Expense", "Pending", "Food & Dining", "Shopping", "Bills & Utilities")

    val filteredList = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.counterparty.contains(searchQuery, ignoreCase = true) ||
                tx.referenceId.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Income" -> tx.transactionType == "CREDIT" || tx.transactionType == "REFUND"
                "Expense" -> tx.transactionType == "DEBIT"
                "Pending" -> tx.status == "PENDING"
                else -> tx.category.equals(selectedFilter, ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("transaction_history_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search Transactions",
            placeholder = "Search merchant, category, or #TXN ID",
            leadingIcon = Icons.Default.Search,
            testTag = "transaction_search_input"
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandIndigo,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("tx_filter_$filter")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "No matching transactions",
                subtitle = "Try clearing your search or selecting a different filter chip.",
                actionLabel = "Reset Filters",
                onActionClick = {
                    searchQuery = ""
                    selectedFilter = "All"
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onClick = { onTransactionClick(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDetailsScreen(
    transaction: TransactionEntity?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    if (transaction == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "Transaction not found",
                subtitle = "This transaction record may have been reset.",
                actionLabel = "Go Back",
                onActionClick = onBack
            )
        }
        return
    }

    val (icon, tintColor) = categoryVisuals(transaction.category)
    val isCredit = transaction.transactionType == "CREDIT" || transaction.transactionType == "REFUND"
    val amountColor = if (isCredit) SemanticSuccess else SemanticError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("transaction_details_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = tintColor.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = transaction.category,
                            tint = tintColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = CurrencyUtils.formatPaise(
                        amountPaise = transaction.amountPaise,
                        showDecimals = true,
                        includeSign = true,
                        isCredit = isCredit
                    ),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    ),
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = if (transaction.status == "COMPLETED") SemanticSuccessBg else SemanticWarning.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = transaction.status,
                            tint = if (transaction.status == "COMPLETED") SemanticSuccess else SemanticWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transaction.status.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (transaction.status == "COMPLETED") SemanticSuccess else SemanticWarning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(18.dp))

                ReceiptDetailRow("Date & Time", CurrencyUtils.formatDateTime(transaction.timestampMillis))
                ReceiptDetailRow("Transaction ID", transaction.referenceId)
                ReceiptDetailRow("Payment Method", transaction.paymentMethod)
                ReceiptDetailRow("Category", transaction.category)
                ReceiptDetailRow("Merchant / Party", transaction.counterparty)
                ReceiptDetailRow(
                    "Description",
                    transaction.description.ifBlank { "Standard DigiWallet transaction" }
                )
                ReceiptDetailRow(
                    "Environment",
                    if (transaction.isDemo) "Simulated Demo Transaction" else "Verified Transaction"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SecondaryButton(
            text = "Share Receipt",
            onClick = {
                val shareText = buildString {
                    appendLine("DigiWallet Receipt (${if (transaction.isDemo) "DEMO MODE" else "VERIFIED"})")
                    appendLine("Title: ${transaction.title}")
                    appendLine("Amount: ${CurrencyUtils.formatPaise(transaction.amountPaise, showDecimals = true, includeSign = true, isCredit = isCredit)}")
                    appendLine("Status: ${transaction.status}")
                    appendLine("Ref ID: ${transaction.referenceId}")
                    appendLine("Date: ${CurrencyUtils.formatDateTime(transaction.timestampMillis)}")
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share DigiWallet Receipt"))
            },
            testTag = "share_receipt_button"
        )
    }
}

@Composable
private fun ReceiptDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}
