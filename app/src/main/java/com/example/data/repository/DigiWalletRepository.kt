package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.BudgetEntity
import com.example.data.local.DemoSeedData
import com.example.data.local.DigiWalletDao
import com.example.data.local.ExpenseEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencyUtils
import com.example.data.model.ParsedQrPayload
import com.example.data.model.ThemeMode
import com.example.data.remote.DefaultPaymentRepository
import com.example.data.remote.PaymentEnvironmentMode
import com.example.data.remote.PaymentOrderResult
import com.example.data.remote.PaymentRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.digiWalletDataStore by preferencesDataStore(name = "digiwallet_prefs")

data class UserSessionPrefs(
    val hasCompletedOnboarding: Boolean = false,
    val isGuestDemoSession: Boolean = false,
    val demoBalancePaise: Long = DemoSeedData.DEFAULT_DEMO_BALANCE_PAISE,
    val isBalanceVisible: Boolean = true,
    val displayName: String = "Monish",
    val email: String = "monish@gmail.com",
    val upiHandle: String = "monish@digiwallet",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false
)

class DigiWalletRepository(
    private val context: Context,
    private val dao: DigiWalletDao,
    private val cloudRepo: CloudFirestoreRepository,
    private val paymentRepository: PaymentRepository = DefaultPaymentRepository()
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val GUEST_DEMO_SESSION = booleanPreferencesKey("guest_demo_session")
        val DEMO_BALANCE_PAISE = longPreferencesKey("demo_balance_paise")
        val BALANCE_VISIBLE = booleanPreferencesKey("balance_visible")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EMAIL = stringPreferencesKey("email")
        val UPI_HANDLE = stringPreferencesKey("upi_handle")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val INITIAL_SEED_DONE = booleanPreferencesKey("initial_seed_done")
    }

    val sessionPrefsFlow: Flow<UserSessionPrefs> = context.digiWalletDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val theme = ThemeMode.entries.firstOrNull { it.name == themeStr } ?: ThemeMode.SYSTEM
        UserSessionPrefs(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            isGuestDemoSession = prefs[Keys.GUEST_DEMO_SESSION] ?: false,
            demoBalancePaise = prefs[Keys.DEMO_BALANCE_PAISE] ?: DemoSeedData.DEFAULT_DEMO_BALANCE_PAISE,
            isBalanceVisible = prefs[Keys.BALANCE_VISIBLE] ?: true,
            displayName = prefs[Keys.DISPLAY_NAME] ?: "Monish",
            email = prefs[Keys.EMAIL] ?: "monish@gmail.com",
            upiHandle = prefs[Keys.UPI_HANDLE] ?: "monish@digiwallet",
            themeMode = theme,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
        )
    }

    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val expensesFlow: Flow<List<ExpenseEntity>> = dao.observeExpenses()
    val budgetsFlow: Flow<List<BudgetEntity>> = dao.observeBudgets()
    val notificationsFlow: Flow<List<NotificationEntity>> = dao.observeNotifications()

    suspend fun ensureSeeded() {
        val prefs = context.digiWalletDataStore.data.first()
        val seeded = prefs[Keys.INITIAL_SEED_DONE] ?: false
        if (!seeded) {
            val uid = Firebase.auth.currentUser?.uid ?: "demo_user"
            dao.insertTransactions(DemoSeedData.seedTransactions(uid))
            dao.insertExpenses(DemoSeedData.seedExpenses(uid))
            dao.insertBudgets(DemoSeedData.seedBudgets(uid))
            dao.insertNotifications(DemoSeedData.seedNotifications())
            context.digiWalletDataStore.edit {
                it[Keys.INITIAL_SEED_DONE] = true
                it[Keys.DEMO_BALANCE_PAISE] = DemoSeedData.DEFAULT_DEMO_BALANCE_PAISE
                it[Keys.DISPLAY_NAME] = "Monish"
            }
        }
    }

    suspend fun resetDemoData() {
        val uid = Firebase.auth.currentUser?.uid ?: "demo_user"
        dao.clearAllTransactions()
        dao.clearAllExpenses()
        dao.clearAllBudgets()
        dao.clearAllNotifications()
        dao.insertTransactions(DemoSeedData.seedTransactions(uid))
        dao.insertExpenses(DemoSeedData.seedExpenses(uid))
        dao.insertBudgets(DemoSeedData.seedBudgets(uid))
        dao.insertNotifications(DemoSeedData.seedNotifications())
        context.digiWalletDataStore.edit {
            it[Keys.DEMO_BALANCE_PAISE] = DemoSeedData.DEFAULT_DEMO_BALANCE_PAISE
        }
        if (Firebase.auth.currentUser != null) {
            val currentPrefs = sessionPrefsFlow.first()
            cloudRepo.upsertUserProfile(
                displayName = currentPrefs.displayName,
                email = currentPrefs.email,
                upiHandle = currentPrefs.upiHandle,
                demoBalancePaise = DemoSeedData.DEFAULT_DEMO_BALANCE_PAISE
            )
        }
    }

    suspend fun completeOnboarding() {
        context.digiWalletDataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setGuestDemoSession(enabled: Boolean) {
        context.digiWalletDataStore.edit {
            it[Keys.GUEST_DEMO_SESSION] = enabled
            if (enabled) it[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun syncGoogleUserSession(uid: String, name: String?, email: String?) {
        val resolvedName = name?.takeIf { it.isNotBlank() } ?: "Monish"
        val resolvedEmail = email?.takeIf { it.isNotBlank() } ?: "monish@gmail.com"
        val handlePrefix = resolvedEmail.substringBefore("@").lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "monish" }
        val resolvedUpi = "$handlePrefix@digiwallet"

        context.digiWalletDataStore.edit {
            it[Keys.DISPLAY_NAME] = resolvedName
            it[Keys.EMAIL] = resolvedEmail
            it[Keys.UPI_HANDLE] = resolvedUpi
            it[Keys.ONBOARDING_DONE] = true
        }

        val currentPrefs = sessionPrefsFlow.first()
        cloudRepo.upsertUserProfile(
            displayName = resolvedName,
            email = resolvedEmail,
            upiHandle = resolvedUpi,
            demoBalancePaise = currentPrefs.demoBalancePaise
        )
    }

    suspend fun toggleBalanceVisibility() {
        context.digiWalletDataStore.edit { prefs ->
            val current = prefs[Keys.BALANCE_VISIBLE] ?: true
            prefs[Keys.BALANCE_VISIBLE] = !current
        }
    }

    suspend fun updateProfile(displayName: String, upiHandle: String) {
        val cleanName = displayName.trim().ifEmpty { "Monish" }
        val cleanUpi = upiHandle.trim().ifEmpty { "monish@digiwallet" }
        context.digiWalletDataStore.edit {
            it[Keys.DISPLAY_NAME] = cleanName
            it[Keys.UPI_HANDLE] = cleanUpi
        }
        if (Firebase.auth.currentUser != null) {
            val prefs = sessionPrefsFlow.first()
            cloudRepo.upsertUserProfile(
                displayName = cleanName,
                email = prefs.email,
                upiHandle = cleanUpi,
                demoBalancePaise = prefs.demoBalancePaise
            )
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.digiWalletDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.digiWalletDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.digiWalletDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun addDemoMoney(
        amountPaise: Long,
        paymentMethod: String,
        mode: PaymentEnvironmentMode = PaymentEnvironmentMode.DEMO_SIMULATION
    ): Result<TransactionEntity> = runCatching {
        when (val orderResult = paymentRepository.executeAddMoney(amountPaise, paymentMethod, mode)) {
            is PaymentOrderResult.SimulatedDemoSuccess -> {
                val uid = Firebase.auth.currentUser?.uid ?: "demo_user"
                val currentBalance = sessionPrefsFlow.first().demoBalancePaise
                val updatedBalance = currentBalance + amountPaise

                val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                val entity = TransactionEntity(
                    id = txId,
                    ownerId = uid,
                    amountPaise = amountPaise,
                    currency = "INR",
                    transactionType = "CREDIT",
                    category = "Salary & Income",
                    title = "Wallet Top-Up ($paymentMethod)",
                    counterparty = paymentMethod,
                    description = "Simulated demo deposit via $paymentMethod",
                    paymentMethod = paymentMethod,
                    status = "COMPLETED",
                    referenceId = orderResult.referenceId,
                    isDemo = true,
                    timestampMillis = orderResult.timestampMillis
                )
                dao.insertTransaction(entity)
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        title = "Demo Deposit Added",
                        message = "${CurrencyUtils.formatPaise(amountPaise)} added to your simulated wallet via $paymentMethod.",
                        type = "TRANSACTION",
                        isRead = false,
                        timestampMillis = orderResult.timestampMillis
                    )
                )
                context.digiWalletDataStore.edit {
                    it[Keys.DEMO_BALANCE_PAISE] = updatedBalance
                }

                if (Firebase.auth.currentUser != null) {
                    val prefs = sessionPrefsFlow.first()
                    cloudRepo.upsertUserProfile(
                        displayName = prefs.displayName,
                        email = prefs.email,
                        upiHandle = prefs.upiHandle,
                        demoBalancePaise = updatedBalance
                    )
                    cloudRepo.createTransaction(
                        transactionId = txId,
                        amountPaise = amountPaise,
                        transactionType = "CREDIT",
                        category = "Salary & Income",
                        title = entity.title,
                        counterparty = entity.counterparty,
                        description = entity.description,
                        paymentMethod = paymentMethod,
                        status = "COMPLETED",
                        referenceId = orderResult.referenceId,
                        isDemo = true,
                        timestampMillis = orderResult.timestampMillis
                    )
                }
                entity
            }
            is PaymentOrderResult.BackendNotConfigured -> {
                throw IllegalStateException(orderResult.explanation)
            }
            is PaymentOrderResult.Failure -> {
                throw IllegalArgumentException(orderResult.reason)
            }
        }
    }

    suspend fun sendDemoMoney(
        recipientName: String,
        recipientUpi: String,
        amountPaise: Long,
        category: String = "Peer Transfer",
        note: String,
        mode: PaymentEnvironmentMode = PaymentEnvironmentMode.DEMO_SIMULATION
    ): Result<TransactionEntity> = runCatching {
        val currentBalance = sessionPrefsFlow.first().demoBalancePaise
        when (val orderResult = paymentRepository.executePeerTransfer(
            recipientUpi = recipientUpi,
            recipientName = recipientName,
            amountPaise = amountPaise,
            currentDemoBalancePaise = currentBalance,
            note = note,
            mode = mode
        )) {
            is PaymentOrderResult.SimulatedDemoSuccess -> {
                val uid = Firebase.auth.currentUser?.uid ?: "demo_user"
                val updatedBalance = currentBalance - amountPaise
                val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                val cleanNote = note.trim().ifEmpty { "Demo transfer to $recipientName" }

                val entity = TransactionEntity(
                    id = txId,
                    ownerId = uid,
                    amountPaise = amountPaise,
                    currency = "INR",
                    transactionType = "DEBIT",
                    category = category,
                    title = recipientName.trim().ifEmpty { recipientUpi },
                    counterparty = recipientUpi.trim(),
                    description = cleanNote,
                    paymentMethod = "UPI",
                    status = "COMPLETED",
                    referenceId = orderResult.referenceId,
                    isDemo = true,
                    timestampMillis = orderResult.timestampMillis
                )
                dao.insertTransaction(entity)
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        title = "Payment Successful",
                        message = "${CurrencyUtils.formatPaise(amountPaise)} sent to ${entity.title} (Demo Mode).",
                        type = "TRANSACTION",
                        isRead = false,
                        timestampMillis = orderResult.timestampMillis
                    )
                )
                context.digiWalletDataStore.edit {
                    it[Keys.DEMO_BALANCE_PAISE] = updatedBalance
                }

                if (Firebase.auth.currentUser != null) {
                    val prefs = sessionPrefsFlow.first()
                    cloudRepo.upsertUserProfile(
                        displayName = prefs.displayName,
                        email = prefs.email,
                        upiHandle = prefs.upiHandle,
                        demoBalancePaise = updatedBalance
                    )
                    cloudRepo.createTransaction(
                        transactionId = txId,
                        amountPaise = amountPaise,
                        transactionType = "DEBIT",
                        category = category,
                        title = entity.title,
                        counterparty = entity.counterparty,
                        description = cleanNote,
                        paymentMethod = "UPI",
                        status = "COMPLETED",
                        referenceId = orderResult.referenceId,
                        isDemo = true,
                        timestampMillis = orderResult.timestampMillis
                    )
                }
                entity
            }
            is PaymentOrderResult.BackendNotConfigured -> {
                throw IllegalStateException(orderResult.explanation)
            }
            is PaymentOrderResult.Failure -> {
                throw IllegalArgumentException(orderResult.reason)
            }
        }
    }

    suspend fun saveExpense(
        existingId: String? = null,
        amountPaise: Long,
        category: String,
        merchant: String,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ): Result<ExpenseEntity> = runCatching {
        require(amountPaise > 0L) { "Expense amount must be greater than ₹0." }
        require(merchant.isNotBlank()) { "Please enter a merchant or title." }
        val uid = Firebase.auth.currentUser?.uid ?: "demo_user"
        val expId = existingId ?: "exp_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val entity = ExpenseEntity(
            id = expId,
            ownerId = uid,
            amountPaise = amountPaise,
            category = category,
            merchant = merchant.trim(),
            note = note.trim(),
            dateMillis = dateMillis
        )
        dao.insertExpense(entity)
        if (Firebase.auth.currentUser != null) {
            cloudRepo.createExpense(
                expenseId = expId,
                amountPaise = amountPaise,
                category = category,
                merchant = entity.merchant,
                note = entity.note.takeIf { it.isNotBlank() },
                dateMillis = dateMillis
            )
        }
        entity
    }

    suspend fun deleteExpense(expenseId: String) {
        dao.deleteExpenseById(expenseId)
        if (Firebase.auth.currentUser != null) {
            cloudRepo.deleteExpense(expenseId)
        }
    }

    suspend fun saveBudget(
        category: String,
        limitPaise: Long,
        monthYear: String = "2026-10"
    ): Result<BudgetEntity> = runCatching {
        require(limitPaise > 0L) { "Budget limit must be greater than ₹0." }
        val uid = Firebase.auth.currentUser?.uid ?: "demo_user"
        val sanitizedCategory = category.lowercase().filter { it.isLetterOrDigit() }
        val budgetId = "bud_${sanitizedCategory}_$monthYear"
        val entity = BudgetEntity(
            id = budgetId,
            ownerId = uid,
            category = category,
            limitPaise = limitPaise,
            monthYear = monthYear
        )
        dao.insertBudget(entity)
        if (Firebase.auth.currentUser != null) {
            cloudRepo.createOrUpdateBudget(
                budgetId = budgetId,
                category = category,
                limitPaise = limitPaise,
                monthYear = monthYear
            )
        }
        entity
    }

    suspend fun deleteBudget(budgetId: String) {
        dao.deleteBudgetById(budgetId)
    }

    suspend fun markNotificationRead(id: String) {
        dao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsRead()
    }

    companion object {
        fun parseUpiQrPayload(rawQr: String): ParsedQrPayload {
            val trimmed = rawQr.trim()
            if (trimmed.isEmpty()) {
                return ParsedQrPayload("", "", null, "", false, "QR code content is empty.")
            }
            if (trimmed.startsWith("upi://pay", ignoreCase = true)) {
                val queryPart = trimmed.substringAfter("?", "")
                val params = queryPart.split("&").mapNotNull { pair ->
                    val idx = pair.indexOf("=")
                    if (idx > 0) {
                        val k = pair.substring(0, idx).lowercase()
                        val v = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                        k to v
                    } else null
                }.toMap()
                val pa = params["pa"].orEmpty().trim()
                val pn = params["pn"].orEmpty().trim().ifEmpty { pa.substringBefore("@").replaceFirstChar { it.uppercase() } }
                val amStr = params["am"].orEmpty().trim()
                val tn = params["tn"].orEmpty().trim()
                if (pa.isBlank() || !pa.contains("@")) {
                    return ParsedQrPayload("", "", null, "", false, "Invalid UPI QR code: missing payee address (pa).")
                }
                val parsedPaise = if (amStr.isNotEmpty()) CurrencyUtils.parseRupeesToPaise(amStr) else null
                return ParsedQrPayload(
                    merchantName = pn,
                    upiId = pa,
                    suggestedAmountPaise = parsedPaise,
                    note = tn.ifEmpty { "Coffee & Snacks" },
                    isValid = true
                )
            } else if (trimmed.contains("@") && !trimmed.contains(" ")) {
                val name = trimmed.substringBefore("@").replaceFirstChar { it.uppercase() }
                return ParsedQrPayload(
                    merchantName = name,
                    upiId = trimmed,
                    suggestedAmountPaise = null,
                    note = "",
                    isValid = true
                )
            } else {
                return ParsedQrPayload(
                    merchantName = "",
                    upiId = "",
                    suggestedAmountPaise = null,
                    note = "",
                    isValid = false,
                    errorMessage = "Unrecognized QR format. Expected a valid UPI payment QR (upi://pay?pa=...)."
                )
            }
        }
    }
}
