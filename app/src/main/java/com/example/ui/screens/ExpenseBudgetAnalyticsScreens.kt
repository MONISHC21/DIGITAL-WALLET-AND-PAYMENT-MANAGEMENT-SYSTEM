package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.data.local.NotificationEntity
import com.example.data.model.CurrencyUtils
import com.example.data.model.ExpenseCategory
import com.example.data.model.ThemeMode
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CustomTextField
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.categoryVisuals
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.CategoryBillsColor
import com.example.ui.theme.CategoryEntertainmentColor
import com.example.ui.theme.CategoryFoodColor
import com.example.ui.theme.CategoryShoppingColor
import com.example.ui.theme.CategoryTransportColor
import com.example.ui.theme.SemanticError
import com.example.ui.theme.SemanticSuccess
import com.example.ui.theme.SemanticWarning
import com.example.viewmodel.DigiWalletUiState

@Composable
fun ExpenseManagementScreen(
    expenses: List<ExpenseEntity>,
    onBack: () -> Unit,
    onSaveExpense: (amountRupees: String, category: String, merchant: String, note: String) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var amountInput by remember { mutableStateOf("250") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var merchantInput by remember { mutableStateOf("Cafe & Dining") }
    var noteInput by remember { mutableStateOf("Lunch with friends") }

    val selectableCategories = listOf(
        ExpenseCategory.FOOD,
        ExpenseCategory.SHOPPING,
        ExpenseCategory.TRANSPORT,
        ExpenseCategory.BILLS,
        ExpenseCategory.ENTERTAINMENT,
        ExpenseCategory.HEALTHCARE,
        ExpenseCategory.EDUCATION
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("expense_management_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Add & Track Expenses",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                        testTag = "expense_amount_input"
                    )

                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(selectableCategories) { cat ->
                            val isSelected = selectedCategory == cat
                            val (icon, tint) = categoryVisuals(cat.displayName)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) BrandIndigo else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    shape = CircleShape,
                                    color = if (isSelected) BrandIndigo.copy(alpha = 0.16f) else tint.copy(alpha = 0.12f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = cat.displayName,
                                            tint = if (isSelected) BrandIndigo else tint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.shortName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) BrandIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    CustomTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        label = "Merchant / Title",
                        placeholder = "Lunch with friends",
                        leadingIcon = Icons.Default.CalendarMonth,
                        testTag = "expense_merchant_input"
                    )

                    CustomTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = "Note",
                        placeholder = "Optional note",
                        testTag = "expense_note_input"
                    )

                    PrimaryButton(
                        text = "Save Expense",
                        onClick = {
                            onSaveExpense(
                                amountInput,
                                selectedCategory.displayName,
                                merchantInput,
                                noteInput
                            )
                        },
                        testTag = "save_expense_button"
                    )
                }
            }
        }

        item {
            SectionHeader(title = "Logged Personal Expenses (${expenses.size})")
        }

        if (expenses.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.PieChart,
                    title = "No manual expenses logged",
                    subtitle = "Add your cash or external expenses above to track them alongside your budgets."
                )
            }
        } else {
            items(expenses, key = { it.id }) { exp ->
                val (icon, tint) = categoryVisuals(exp.category)
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            shape = CircleShape,
                            color = tint.copy(alpha = 0.14f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = exp.category, tint = tint)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exp.merchant,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${exp.category} • ${CurrencyUtils.formatShortDate(exp.dateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (exp.note.isNotBlank()) {
                                Text(
                                    text = exp.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = CurrencyUtils.formatPaise(exp.amountPaise),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SemanticError
                        )
                        IconButton(onClick = { onDeleteExpense(exp.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete expense",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetManagementScreen(
    uiState: DigiWalletUiState,
    onBack: () -> Unit,
    onSaveBudget: (category: String, limitRupees: String) -> Unit
) {
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD.displayName) }
    var limitInput by remember { mutableStateOf("5000") }

    val availableCategories = ExpenseCategory.entries
        .filter { it != ExpenseCategory.SALARY && it != ExpenseCategory.TRANSFER }
        .map { it.displayName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .testTag("budget_management_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Monthly Budget",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "October 2026",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Period",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.budgetStatuses.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.PieChart,
                        title = "No budgets configured",
                        subtitle = "Create a monthly category budget to monitor your spending."
                    )
                }
            } else {
                items(uiState.budgetStatuses, key = { it.budget.id }) { item ->
                    BudgetProgressCard(
                        statusItem = item,
                        onClick = {
                            selectedCategory = item.budget.category
                            limitInput = (item.budget.limitPaise / 100L).toString()
                            showAddBudgetDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Add New Budget",
            onClick = { showAddBudgetDialog = true },
            testTag = "add_new_budget_button"
        )
    }

    if (showAddBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = { Text("Configure Monthly Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableCategories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    CustomTextField(
                        value = limitInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                limitInput = input
                            }
                        },
                        label = "Monthly Limit (₹)",
                        placeholder = "5000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        testTag = "budget_limit_input"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveBudget(selectedCategory, limitInput)
                        showAddBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAnalyticsScreen(
    uiState: DigiWalletUiState,
    onNavigateToExpenses: () -> Unit,
    onNavigateToBudgets: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("Monthly") }
    val periods = listOf("Monthly", "Weekly", "Yearly")

    val scaleFactor = when (selectedPeriod) {
        "Weekly" -> 0.28f
        "Yearly" -> 11.5f
        else -> 1.0f
    }
    val displayedTotalPaise = (uiState.combinedAnalyticsExpensePaise * scaleFactor).toLong()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("financial_analytics_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Analytics",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onNavigateToExpenses) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense")
                    }
                    TextButton(onClick = onNavigateToBudgets) {
                        Text("Budgets")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { period ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandIndigo,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bar Chart Card matching the reference poster
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Total Expenses ($selectedPeriod)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatPaise(displayedTotalPaise, showDecimals = false),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = BrandIndigo
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Trend",
                            tint = SemanticSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+12% from last period • Based on ${uiState.transactions.size + uiState.expenses.size} stored records",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    val labels = when (selectedPeriod) {
                        "Weekly" -> listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                        "Yearly" -> listOf("2022", "2023", "2024", "2025", "2026")
                        else -> listOf("Jun", "Jul", "Aug", "Sep", "Oct")
                    }
                    val fractions = listOf(0.52f, 0.76f, 0.64f, 0.92f, 0.72f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val barCount = labels.size
                        val slotWidth = size.width / barCount
                        val barWidth = (slotWidth * 0.44f).coerceAtMost(36.dp.toPx())

                        fractions.forEachIndexed { index, frac ->
                            val barHeight = size.height * frac
                            val left = index * slotWidth + (slotWidth - barWidth) / 2f
                            val top = size.height - barHeight
                            val barColor = if (index == 3) BrandIndigo else BrandPurple.copy(alpha = 0.65f)
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        labels.forEach { lbl ->
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown Donut Chart Card matching the reference poster
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.categoryBreakdowns.isEmpty()) {
                        Text(
                            text = "No spending categories recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val sliceColors = listOf(
                            BrandIndigo,
                            BrandPurple,
                            CategoryTransportColor,
                            CategoryFoodColor,
                            CategoryBillsColor,
                            CategoryEntertainmentColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(110.dp)) {
                                    var startAngle = -90f
                                    uiState.categoryBreakdowns.take(6).forEachIndexed { idx, item ->
                                        val sweep = (item.percentage / 100f) * 360f
                                        drawArc(
                                            color = sliceColors[idx % sliceColors.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.categoryBreakdowns.take(5).forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(sliceColors[idx % sliceColors.size])
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${item.percentage.toInt()}%",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Transactions", "Budgets", "Offers")

    val filtered = remember(notifications, selectedFilter) {
        notifications.filter { n ->
            when (selectedFilter) {
                "Transactions" -> n.type == "TRANSACTION"
                "Budgets" -> n.type == "BUDGET"
                "Offers" -> n.type == "OFFER"
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .testTag("notifications_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextButton(onClick = onMarkAllRead) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark Read")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { f ->
                val isSelected = selectedFilter == f
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = f },
                    label = { Text(f) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandIndigo,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Notifications,
                title = "No notifications",
                subtitle = "You're all caught up!"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { notif ->
                    val (icon, tint) = when (notif.type) {
                        "TRANSACTION" -> Icons.Default.CheckCircle to SemanticSuccess
                        "BUDGET" -> Icons.Default.Warning to SemanticWarning
                        else -> Icons.Default.LocalOffer to CategoryFoodColor
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMarkRead(notif.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead)
                                MaterialTheme.colorScheme.surface
                            else
                                BrandIndigo.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = tint.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = notif.title, tint = tint)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyUtils.formatShortDate(notif.timestampMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandIndigo)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAndSettingsScreen(
    uiState: DigiWalletUiState,
    isAuthenticatedWithGoogle: Boolean,
    onUpdateProfile: (name: String, upiHandle: String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onResetDemoData: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var nameInput by remember(uiState.sessionPrefs.displayName) {
        mutableStateOf(uiState.sessionPrefs.displayName)
    }
    var upiInput by remember(uiState.sessionPrefs.upiHandle) {
        mutableStateOf(uiState.sessionPrefs.upiHandle)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("profile_settings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar & Name Card
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = BrandIndigo
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.sessionPrefs.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = uiState.sessionPrefs.displayName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = uiState.sessionPrefs.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = BrandIndigo.copy(alpha = 0.12f),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = if (isAuthenticatedWithGoogle)
                    "Google Cloud Synced • ${uiState.sessionPrefs.upiHandle}"
                else
                    "Local Demo Session • ${uiState.sessionPrefs.upiHandle}",
                style = MaterialTheme.typography.labelSmall,
                color = BrandIndigo,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        ProfileOptionCard(
            icon = Icons.Default.Edit,
            title = "Edit Profile",
            subtitle = "Name & simulated UPI handle",
            onClick = { showEditProfileDialog = true }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileOptionCard(
            icon = Icons.Default.Lock,
            title = "Security & Privacy",
            subtitle = if (uiState.sessionPrefs.biometricLockEnabled) "Biometric re-auth enabled • Zero-trust rules" else "Manage app lock & privacy details",
            onClick = { showSecurityDialog = true }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileOptionCard(
            icon = Icons.Default.Notifications,
            title = "Notification Settings",
            subtitle = if (uiState.sessionPrefs.notificationsEnabled) "In-app alerts active" else "Notifications muted",
            onClick = { onSetNotificationsEnabled(!uiState.sessionPrefs.notificationsEnabled) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileOptionCard(
            icon = Icons.Default.Palette,
            title = "Theme",
            subtitle = uiState.sessionPrefs.themeMode.label,
            onClick = { showThemeDialog = true }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileOptionCard(
            icon = Icons.Default.Language,
            title = "Language & Currency",
            subtitle = "English (India) • INR (₹)",
            onClick = { showAboutDialog = true }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileOptionCard(
            icon = Icons.Default.Refresh,
            title = "Reset Demo Data",
            subtitle = "Restore ₹12,450.00 balance & seeded sample records",
            onClick = onResetDemoData
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileOptionCard(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = "Help & About DigiWallet",
            subtitle = "Architecture, security & backend readiness",
            onClick = { showAboutDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { showLogoutConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("logout_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SemanticError.copy(alpha = 0.12f),
                contentColor = SemanticError
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = "Full Name",
                        placeholder = "Monish"
                    )
                    CustomTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = "Demo UPI Handle",
                        placeholder = "monish@digiwallet"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, upiInput)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sessionPrefs.themeMode == mode,
                                onClick = {
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text("Security & Privacy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Biometric Confirmation Prompt")
                        Switch(
                            checked = uiState.sessionPrefs.biometricLockEnabled,
                            onCheckedChange = onSetBiometricEnabled
                        )
                    }
                    Text(
                        text = "• All monetary values are stored in integer paise (Long) to prevent floating-point drift.\n• Cloud Firestore rules enforce per-user document isolation under /users/{userId}.\n• No payment gateway secret keys are stored in the APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = BrandIndigo) },
            title = { Text("DigiWallet v1.0") },
            text = {
                Text(
                    "DigiWallet — Smart Digital Wallet & Payment Management.\n\n" +
                        "Built with Kotlin, Jetpack Compose, Material 3, Room local database, and Firebase Authentication & Cloud Firestore.\n\n" +
                        "All wallet balances and transactions in Demo Mode are clearly labeled simulations."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to sign out of your DigiWallet session?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticError)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileOptionCard(
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = BrandIndigo.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = BrandIndigo, modifier = Modifier.size(20.dp))
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
