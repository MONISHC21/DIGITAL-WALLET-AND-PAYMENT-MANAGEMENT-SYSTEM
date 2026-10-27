package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["timestampMillis"]), Index(value = ["category"]), Index(value = ["transactionType"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val transactionType: String, // "CREDIT", "DEBIT", "REFUND"
    val category: String,
    val title: String,
    val counterparty: String,
    val description: String = "",
    val paymentMethod: String = "UPI",
    val status: String = "COMPLETED", // "COMPLETED", "PENDING", "FAILED"
    val referenceId: String,
    val isDemo: Boolean = true,
    val timestampMillis: Long
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["dateMillis"]), Index(value = ["category"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val amountPaise: Long,
    val category: String,
    val merchant: String,
    val note: String = "",
    val dateMillis: Long
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category", "monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val category: String,
    val limitPaise: Long,
    val monthYear: String // e.g., "2026-10"
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["timestampMillis"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String, // "TRANSACTION", "BUDGET", "OFFER"
    val isRead: Boolean = false,
    val timestampMillis: Long
)
