package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FirestoreBudget
import com.example.data.model.FirestoreExpense
import com.example.data.model.FirestoreTransaction
import com.example.data.model.FirestoreUserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Cloud Firestore Repository enforcing custom database ID (`R.string.firestore_database_id`),
 * server timestamps (`FieldValue.serverTimestamp()`), and strict per-user isolation under `/users/{userId}`.
 */
class CloudFirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String,
        upiHandle: String,
        demoBalancePaise: Long
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.take(80),
                    "email" to email.take(120),
                    "upiHandle" to upiHandle.take(80),
                    "demoBalancePaise" to demoBalancePaise,
                    "currency" to "INR",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to displayName.take(80),
                    "email" to email.take(120),
                    "upiHandle" to upiHandle.take(80),
                    "demoBalancePaise" to demoBalancePaise,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getUserProfile(targetUserId: String = requireUserId()): Result<FirestoreUserProfile?> = runCatching {
        val docRef = db.collection("users").document(targetUserId)
        try {
            val snap = docRef.get().await()
            snap.toObject(FirestoreUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String = requireUserId()): Flow<FirestoreUserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(FirestoreUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun createTransaction(
        transactionId: String = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}",
        amountPaise: Long,
        transactionType: String,
        category: String,
        title: String,
        counterparty: String,
        description: String?,
        paymentMethod: String,
        status: String = "COMPLETED",
        referenceId: String,
        isDemo: Boolean = true,
        timestampMillis: Long = System.currentTimeMillis()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        val payload = mutableMapOf<String, Any>(
            "ownerId" to uid,
            "amountPaise" to amountPaise,
            "currency" to "INR",
            "transactionType" to transactionType,
            "category" to category.take(40),
            "title" to title.take(100),
            "counterparty" to counterparty.take(100),
            "paymentMethod" to paymentMethod.take(50),
            "status" to status,
            "referenceId" to referenceId.take(64),
            "isDemo" to isDemo,
            "timestampMillis" to timestampMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!description.isNullOrBlank()) {
            payload["description"] = description.take(200)
        }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getUserTransactions(userId: String = requireUserId()): Result<List<FirestoreTransaction>> = runCatching {
        val colRef = db.collection("users").document(userId).collection("transactions")
        try {
            val snap = colRef.whereEqualTo("ownerId", userId).get().await()
            snap.toObjects(FirestoreTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun getTransactionById(targetUserId: String, transactionId: String): Result<FirestoreTransaction?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("transactions").document(transactionId)
        try {
            val snap = docRef.get().await()
            snap.toObject(FirestoreTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeTransactions(userId: String = requireUserId()): Flow<List<FirestoreTransaction>> = flow {
        val colRef = db.collection("users").document(userId).collection("transactions")
        emitAll(
            colRef.whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FirestoreTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.timestampMillis }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                    throw error
                }
        )
    }

    suspend fun createExpense(
        expenseId: String = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}",
        amountPaise: Long,
        category: String,
        merchant: String,
        note: String?,
        dateMillis: Long = System.currentTimeMillis()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        val payload = mutableMapOf<String, Any>(
            "ownerId" to uid,
            "amountPaise" to amountPaise,
            "category" to category.take(40),
            "merchant" to merchant.take(100),
            "dateMillis" to dateMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!note.isNullOrBlank()) {
            payload["note"] = note.take(200)
        }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createOrUpdateBudget(
        budgetId: String,
        category: String,
        limitPaise: Long,
        monthYear: String = "2026-10"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val createPayload = mapOf(
                    "ownerId" to uid,
                    "category" to category.take(40),
                    "limitPaise" to limitPaise,
                    "monthYear" to monthYear.take(20),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "category" to category.take(40),
                    "limitPaise" to limitPaise,
                    "monthYear" to monthYear.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
