package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class TransactionType(val label: String) {
    CREDIT("Income"),
    DEBIT("Expense"),
    REFUND("Refund")
}

enum class TransactionStatus(val label: String) {
    COMPLETED("Completed"),
    PENDING("Pending"),
    FAILED("Failed")
}

enum class ExpenseCategory(
    val displayName: String,
    val shortName: String
) {
    FOOD("Food & Dining", "Food"),
    SHOPPING("Shopping", "Shopping"),
    TRANSPORT("Transport", "Transport"),
    BILLS("Bills & Utilities", "Bills"),
    ENTERTAINMENT("Entertainment", "Entertainment"),
    HEALTHCARE("Healthcare", "Health"),
    EDUCATION("Education", "Education"),
    TRAVEL("Travel", "Travel"),
    TRANSFER("Peer Transfer", "Transfer"),
    SALARY("Salary & Income", "Income"),
    OTHER("Other", "Other");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull {
                it.displayName.equals(value, ignoreCase = true) ||
                    it.shortName.equals(value, ignoreCase = true) ||
                    it.name.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

/**
 * Monetary utility using integer paise (`Long`) exclusively for authoritative calculations.
 * 1 Rupee (₹1) = 100 Paise.
 */
object CurrencyUtils {
    private val indiaLocale = Locale.forLanguageTag("en-IN")

    fun formatPaise(
        amountPaise: Long,
        showDecimals: Boolean = false,
        includeSign: Boolean = false,
        isCredit: Boolean = false
    ): String {
        val absPaise = abs(amountPaise)
        val rupees = absPaise / 100
        val paisePart = absPaise % 100

        val numberFormat = NumberFormat.getNumberInstance(indiaLocale)
        val formattedRupees = numberFormat.format(rupees)

        val decimalPart = if (showDecimals || paisePart > 0L) {
            String.format(Locale.US, ".%02d", paisePart)
        } else {
            ""
        }

        val prefix = when {
            includeSign && isCredit -> "+₹"
            includeSign && !isCredit -> "-₹"
            amountPaise < 0 -> "-₹"
            else -> "₹"
        }
        return "$prefix$formattedRupees$decimalPart"
    }

    /**
     * Parses a user-entered rupee string (e.g. "250" or "250.50") into exact Long paise without floating point inaccuracy.
     */
    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("₹", "").trim()
        if (cleaned.isEmpty()) return null
        val regex = Regex("^\\d+(\\.\\d{1,2})?$")
        if (!regex.matches(cleaned)) return null

        val parts = cleaned.split(".")
        val rupees = parts[0].toLongOrNull() ?: return null
        if (rupees > 10_000_000L) return null // Max ₹1 Crore safety bound

        val paise = if (parts.size > 1) {
            val frac = parts[1].padEnd(2, '0').take(2)
            frac.toLongOrNull() ?: return null
        } else {
            0L
        }
        return (rupees * 100L) + paise
    }

    fun formatDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        return formatter.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        val oneDay = 24 * 60 * 60 * 1000L
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return when {
            diff in 0..<oneDay -> "Today, ${timeFmt.format(Date(millis))}"
            diff in oneDay..<(2 * oneDay) -> "Yesterday, ${timeFmt.format(Date(millis))}"
            else -> dateFmt.format(Date(millis))
        }
    }
}

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val upiHandle: String = "",
    val demoBalancePaise: Long = 1_245_000L, // ₹12,450.00 default demo balance
    val currency: String = "INR",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTransaction(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val amountPaise: Long = 0L,
    val currency: String = "INR",
    val transactionType: String = "DEBIT",
    val category: String = "Food & Dining",
    val title: String = "",
    val counterparty: String = "",
    val description: String? = null,
    val paymentMethod: String = "UPI",
    val status: String = "COMPLETED",
    val referenceId: String = "",
    val isDemo: Boolean = true,
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreExpense(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val amountPaise: Long = 0L,
    val category: String = "Food & Dining",
    val merchant: String = "",
    val note: String? = null,
    val dateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBudget(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val category: String = "Food & Dining",
    val limitPaise: Long = 0L,
    val monthYear: String = "2026-10",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DemoRecipient(
    val id: String,
    val name: String,
    val initials: String,
    val upiId: String,
    val avatarColorHex: Long
)

data class ParsedQrPayload(
    val merchantName: String,
    val upiId: String,
    val suggestedAmountPaise: Long?,
    val note: String,
    val isValid: Boolean,
    val errorMessage: String? = null
)
