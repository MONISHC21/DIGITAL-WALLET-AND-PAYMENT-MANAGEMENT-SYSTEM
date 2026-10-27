package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DemoRecipient

@Database(
    entities = [
        TransactionEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DigiWalletDatabase : RoomDatabase() {
    abstract fun digiWalletDao(): DigiWalletDao

    companion object {
        @Volatile
        private var INSTANCE: DigiWalletDatabase? = null

        fun getInstance(context: Context): DigiWalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DigiWalletDatabase::class.java,
                    "digiwallet_local.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DemoSeedData {
    const val DEFAULT_DEMO_BALANCE_PAISE = 1_245_000L // ₹12,450.00 matching UI reference

    val demoRecipients = listOf(
        DemoRecipient("rec_rahul", "Rahul", "RK", "rahul@upi", 0xFF5146E5),
        DemoRecipient("rec_pooja", "Pooja", "PS", "pooja@upi", 0xFFEC4899),
        DemoRecipient("rec_amit", "Amit", "AM", "amit@upi", 0xFFF97316),
        DemoRecipient("rec_sakshi", "Sakshi", "SK", "sakshi@upi", 0xFF06B6D4)
    )

    fun seedTransactions(ownerId: String = "demo_user"): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val hour = 3600_000L
        val day = 24 * hour
        return listOf(
            TransactionEntity(
                id = "tx_demo_grocery",
                ownerId = ownerId,
                amountPaise = 45_000L, // ₹450.00
                currency = "INR",
                transactionType = "DEBIT",
                category = "Food & Dining",
                title = "Grocery Store",
                counterparty = "more.supermarket@upi",
                description = "Monthly grocery shopping",
                paymentMethod = "UPI",
                status = "COMPLETED",
                referenceId = "#TXN123456789",
                isDemo = true,
                timestampMillis = now - (2 * hour)
            ),
            TransactionEntity(
                id = "tx_demo_rahul",
                ownerId = ownerId,
                amountPaise = 100_000L, // ₹1,000.00
                currency = "INR",
                transactionType = "CREDIT",
                category = "Peer Transfer",
                title = "From Rahul",
                counterparty = "rahul@upi",
                description = "Shared dinner split settlement",
                paymentMethod = "UPI",
                status = "COMPLETED",
                referenceId = "#TXN123456788",
                isDemo = true,
                timestampMillis = now - (20 * hour)
            ),
            TransactionEntity(
                id = "tx_demo_electricity",
                ownerId = ownerId,
                amountPaise = 120_000L, // ₹1,200.00
                currency = "INR",
                transactionType = "DEBIT",
                category = "Bills & Utilities",
                title = "Electricity Bill",
                counterparty = "bescom.bills@upi",
                description = "Monthly home electricity bill",
                paymentMethod = "UPI",
                status = "COMPLETED",
                referenceId = "#TXN123456787",
                isDemo = true,
                timestampMillis = now - (2 * day)
            ),
            TransactionEntity(
                id = "tx_demo_swiggy",
                ownerId = ownerId,
                amountPaise = 32_000L, // ₹320.00
                currency = "INR",
                transactionType = "DEBIT",
                category = "Food & Dining",
                title = "Swiggy",
                counterparty = "swiggy@upi",
                description = "Evening snacks & coffee",
                paymentMethod = "UPI",
                status = "COMPLETED",
                referenceId = "#TXN123456786",
                isDemo = true,
                timestampMillis = now - (4 * day)
            ),
            TransactionEntity(
                id = "tx_demo_salary",
                ownerId = ownerId,
                amountPaise = 500_000L, // ₹5,000.00
                currency = "INR",
                transactionType = "CREDIT",
                category = "Salary & Income",
                title = "Salary",
                counterparty = "acme.corp@hdfcbank",
                description = "Freelance milestone payout",
                paymentMethod = "Net Banking",
                status = "COMPLETED",
                referenceId = "#TXN123456785",
                isDemo = true,
                timestampMillis = now - (6 * day)
            ),
            TransactionEntity(
                id = "tx_demo_amazon",
                ownerId = ownerId,
                amountPaise = 88_000L, // ₹880.00 (Total expenses: 450+1200+320+880 = ₹2,850 matching poster!)
                currency = "INR",
                transactionType = "DEBIT",
                category = "Shopping",
                title = "Amazon",
                counterparty = "amazon.pay@apl",
                description = "Wireless accessories & cables",
                paymentMethod = "Debit Card",
                status = "COMPLETED",
                referenceId = "#TXN123456784",
                isDemo = true,
                timestampMillis = now - (9 * day)
            )
        )
    }

    fun seedExpenses(ownerId: String = "demo_user"): List<ExpenseEntity> {
        val now = System.currentTimeMillis()
        val day = 24 * 3600_000L
        return listOf(
            ExpenseEntity(
                id = "exp_demo_1",
                ownerId = ownerId,
                amountPaise = 253_000L, // ₹2,530 + ₹770 from tx = ₹3,300 Food & Dining
                category = "Food & Dining",
                merchant = "Organic Fresh Mart & Cafes",
                note = "Weekly groceries and team lunches",
                dateMillis = now - day
            ),
            ExpenseEntity(
                id = "exp_demo_2",
                ownerId = ownerId,
                amountPaise = 197_000L, // ₹1,970 + ₹880 from tx = ₹2,850 Shopping
                category = "Shopping",
                merchant = "Lifestyle & Apparel Store",
                note = "Festive wardrobe essentials",
                dateMillis = now - (3 * day)
            ),
            ExpenseEntity(
                id = "exp_demo_3",
                ownerId = ownerId,
                amountPaise = 120_000L, // ₹1,200 Transport
                category = "Transport",
                merchant = "Metro Pass & Uber Rides",
                note = "Monthly office commute",
                dateMillis = now - (5 * day)
            ),
            ExpenseEntity(
                id = "exp_demo_4",
                ownerId = ownerId,
                amountPaise = 90_000L, // ₹900 Entertainment
                category = "Entertainment",
                merchant = "BookMyShow & Streaming",
                note = "Weekend movie tickets",
                dateMillis = now - (7 * day)
            )
        )
    }

    fun seedBudgets(ownerId: String = "demo_user"): List<BudgetEntity> {
        return listOf(
            BudgetEntity(
                id = "bud_food",
                ownerId = ownerId,
                category = "Food & Dining",
                limitPaise = 500_000L, // ₹5,000 limit (₹3,300 spent = 66%)
                monthYear = "2026-10"
            ),
            BudgetEntity(
                id = "bud_shopping",
                ownerId = ownerId,
                category = "Shopping",
                limitPaise = 400_000L, // ₹4,000 limit (₹2,850 spent = 71%)
                monthYear = "2026-10"
            ),
            BudgetEntity(
                id = "bud_transport",
                ownerId = ownerId,
                category = "Transport",
                limitPaise = 200_000L, // ₹2,000 limit (₹1,200 spent = 60%)
                monthYear = "2026-10"
            ),
            BudgetEntity(
                id = "bud_entertainment",
                ownerId = ownerId,
                category = "Entertainment",
                limitPaise = 200_000L, // ₹2,000 limit (₹900 spent = 45%)
                monthYear = "2026-10"
            )
        )
    }

    fun seedNotifications(): List<NotificationEntity> {
        val now = System.currentTimeMillis()
        val hour = 3600_000L
        val day = 24 * hour
        return listOf(
            NotificationEntity(
                id = "notif_1",
                title = "Money Received",
                message = "You received ₹1,000 from Rahul (Demo Transfer)",
                type = "TRANSACTION",
                isRead = false,
                timestampMillis = now - (2 * hour)
            ),
            NotificationEntity(
                id = "notif_2",
                title = "Budget Alert",
                message = "You have used 71% of your Shopping budget for this month",
                type = "BUDGET",
                isRead = false,
                timestampMillis = now - (5 * hour)
            ),
            NotificationEntity(
                id = "notif_3",
                title = "Payment Successful",
                message = "Electricity bill demo payment of ₹1,200 completed",
                type = "TRANSACTION",
                isRead = true,
                timestampMillis = now - day
            ),
            NotificationEntity(
                id = "notif_4",
                title = "New Offer",
                message = "Demo Reward: Explore QR Scan & Budget Insights in DigiWallet",
                type = "OFFER",
                isRead = true,
                timestampMillis = now - (2 * day)
            ),
            NotificationEntity(
                id = "notif_5",
                title = "Weekly Summary",
                message = "You spent ₹2,850 in demo transactions this week",
                type = "BUDGET",
                isRead = true,
                timestampMillis = now - (3 * day)
            )
        )
    }
}
