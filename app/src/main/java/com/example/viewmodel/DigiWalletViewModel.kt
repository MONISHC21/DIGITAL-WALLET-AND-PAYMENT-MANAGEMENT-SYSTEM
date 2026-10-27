package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetEntity
import com.example.data.local.DemoSeedData
import com.example.data.local.ExpenseEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencyUtils
import com.example.data.model.DemoRecipient
import com.example.data.model.ThemeMode
import com.example.data.remote.PaymentEnvironmentMode
import com.example.data.repository.DigiWalletRepository
import com.example.data.repository.UserSessionPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategorySpendSummary(
    val category: String,
    val spentPaise: Long,
    val percentage: Float
)

data class BudgetStatusItem(
    val budget: BudgetEntity,
    val spentPaise: Long,
    val remainingPaise: Long,
    val progressFraction: Float,
    val isApproachingLimit: Boolean,
    val isExceeded: Boolean
)

data class DigiWalletUiState(
    val isLoading: Boolean = true,
    val sessionPrefs: UserSessionPrefs = UserSessionPrefs(),
    val transactions: List<TransactionEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val demoRecipients: List<DemoRecipient> = DemoSeedData.demoRecipients,
    val totalIncomePaise: Long = 0L,
    val totalTxExpensePaise: Long = 0L,
    val totalManualExpensePaise: Long = 0L,
    val combinedAnalyticsExpensePaise: Long = 0L,
    val netSavingsPaise: Long = 0L,
    val budgetStatuses: List<BudgetStatusItem> = emptyList(),
    val categoryBreakdowns: List<CategorySpendSummary> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val isProcessingAction: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class DigiWalletViewModel(
    private val repository: DigiWalletRepository
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    val uiState: StateFlow<DigiWalletUiState> = combine(
        repository.sessionPrefsFlow,
        repository.transactionsFlow,
        repository.expensesFlow,
        repository.budgetsFlow,
        repository.notificationsFlow
    ) { prefs, txs, exps, buds, notifs ->
        val completedTxs = txs.filter { it.status == "COMPLETED" }
        val totalIncome = completedTxs
            .filter { it.transactionType == "CREDIT" || it.transactionType == "REFUND" }
            .sumOf { it.amountPaise }
        val totalTxDebit = completedTxs
            .filter { it.transactionType == "DEBIT" }
            .sumOf { it.amountPaise }
        val totalManualExp = exps.sumOf { it.amountPaise }
        val combinedExpense = totalTxDebit + totalManualExp
        val netSavings = (totalIncome - totalTxDebit).coerceAtLeast(0L)

        // Category spend map combining debit transactions and tracked expenses
        val spendByCategory = mutableMapOf<String, Long>()
        completedTxs.filter { it.transactionType == "DEBIT" }.forEach { tx ->
            spendByCategory[tx.category] = (spendByCategory[tx.category] ?: 0L) + tx.amountPaise
        }
        exps.forEach { exp ->
            spendByCategory[exp.category] = (spendByCategory[exp.category] ?: 0L) + exp.amountPaise
        }

        val budgetItems = buds.map { b ->
            val spent = spendByCategory[b.category] ?: 0L
            val remaining = b.limitPaise - spent
            val ratio = if (b.limitPaise > 0L) spent.toFloat() / b.limitPaise.toFloat() else 0f
            BudgetStatusItem(
                budget = b,
                spentPaise = spent,
                remainingPaise = remaining,
                progressFraction = ratio.coerceIn(0f, 1f),
                isApproachingLimit = ratio >= 0.75f && ratio < 1.0f,
                isExceeded = ratio >= 1.0f
            )
        }

        val totalCategorySpend = spendByCategory.values.sum().coerceAtLeast(1L)
        val breakdowns = spendByCategory.entries
            .sortedByDescending { it.value }
            .map { (cat, amt) ->
                CategorySpendSummary(
                    category = cat,
                    spentPaise = amt,
                    percentage = (amt.toFloat() / totalCategorySpend.toFloat()) * 100f
                )
            }

        DigiWalletUiState(
            isLoading = false,
            sessionPrefs = prefs,
            transactions = txs,
            expenses = exps,
            budgets = buds,
            notifications = notifs,
            demoRecipients = DemoSeedData.demoRecipients,
            totalIncomePaise = totalIncome,
            totalTxExpensePaise = totalTxDebit,
            totalManualExpensePaise = totalManualExp,
            combinedAnalyticsExpensePaise = combinedExpense,
            netSavingsPaise = netSavings,
            budgetStatuses = budgetItems,
            categoryBreakdowns = breakdowns,
            unreadNotificationCount = notifs.count { !it.isRead }
        )
    }.combine(_isProcessing) { state, processing ->
        state.copy(isProcessingAction = processing)
    }.combine(_feedbackMessage) { state, msg ->
        state.copy(feedbackMessage = msg)
    }.combine(_errorMessage) { state, err ->
        state.copy(errorMessage = err)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DigiWalletUiState(isLoading = true)
    )

    fun clearMessages() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun showError(error: String) {
        _errorMessage.value = error
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
        }
    }

    fun setGuestDemoSession(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGuestDemoSession(enabled)
        }
    }

    fun onGoogleSignInSuccess(uid: String, displayName: String?, email: String?) {
        viewModelScope.launch {
            runCatching {
                repository.syncGoogleUserSession(uid, displayName, email)
            }
        }
    }

    fun toggleBalanceVisibility() {
        viewModelScope.launch {
            repository.toggleBalanceVisibility()
        }
    }

    fun addDemoMoney(
        amountRupeesInput: String,
        paymentMethod: String,
        mode: PaymentEnvironmentMode = PaymentEnvironmentMode.DEMO_SIMULATION,
        onSuccess: (TransactionEntity) -> Unit
    ) {
        val paise = CurrencyUtils.parseRupeesToPaise(amountRupeesInput)
        if (paise == null || paise <= 0L) {
            _errorMessage.value = "Please enter a valid amount greater than ₹0."
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.addDemoMoney(paise, paymentMethod, mode)
            _isProcessing.value = false
            result.onSuccess { tx ->
                _feedbackMessage.value = "Added ${CurrencyUtils.formatPaise(paise)} to Demo Wallet"
                onSuccess(tx)
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to add demo money."
            }
        }
    }

    fun sendDemoMoney(
        recipientName: String,
        recipientUpi: String,
        amountRupeesInput: String,
        category: String = "Peer Transfer",
        note: String,
        mode: PaymentEnvironmentMode = PaymentEnvironmentMode.DEMO_SIMULATION,
        onSuccess: (TransactionEntity) -> Unit
    ) {
        val paise = CurrencyUtils.parseRupeesToPaise(amountRupeesInput)
        if (paise == null || paise <= 0L) {
            _errorMessage.value = "Please enter a valid amount greater than ₹0."
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.sendDemoMoney(
                recipientName = recipientName,
                recipientUpi = recipientUpi,
                amountPaise = paise,
                category = category,
                note = note,
                mode = mode
            )
            _isProcessing.value = false
            result.onSuccess { tx ->
                _feedbackMessage.value = "Sent ${CurrencyUtils.formatPaise(paise)} to ${tx.title} (Demo)"
                onSuccess(tx)
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Transfer could not be completed."
            }
        }
    }

    fun saveExpense(
        amountRupeesInput: String,
        category: String,
        merchant: String,
        note: String,
        onSuccess: () -> Unit
    ) {
        val paise = CurrencyUtils.parseRupeesToPaise(amountRupeesInput)
        if (paise == null || paise <= 0L) {
            _errorMessage.value = "Please enter a valid expense amount."
            return
        }
        if (merchant.isBlank()) {
            _errorMessage.value = "Please enter a merchant or description."
            return
        }
        viewModelScope.launch {
            val result = repository.saveExpense(
                amountPaise = paise,
                category = category,
                merchant = merchant,
                note = note
            )
            result.onSuccess {
                _feedbackMessage.value = "Expense of ${CurrencyUtils.formatPaise(paise)} saved"
                onSuccess()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to save expense."
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _feedbackMessage.value = "Expense deleted"
        }
    }

    fun saveBudget(
        category: String,
        limitRupeesInput: String,
        onSuccess: () -> Unit
    ) {
        val paise = CurrencyUtils.parseRupeesToPaise(limitRupeesInput)
        if (paise == null || paise <= 0L) {
            _errorMessage.value = "Please enter a valid monthly budget limit."
            return
        }
        viewModelScope.launch {
            val result = repository.saveBudget(category = category, limitPaise = paise)
            result.onSuccess {
                _feedbackMessage.value = "Budget for $category updated to ${CurrencyUtils.formatPaise(paise)}"
                onSuccess()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to update budget."
            }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            _feedbackMessage.value = "Budget removed"
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            _feedbackMessage.value = "All notifications marked as read"
        }
    }

    fun updateProfile(displayName: String, upiHandle: String) {
        viewModelScope.launch {
            repository.updateProfile(displayName, upiHandle)
            _feedbackMessage.value = "Profile updated"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricLockEnabled(enabled)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.resetDemoData()
            _isProcessing.value = false
            _feedbackMessage.value = "Demo wallet balance & sample records reset to defaults"
        }
    }

    class Factory(private val repository: DigiWalletRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DigiWalletViewModel(repository) as T
        }
    }
}
