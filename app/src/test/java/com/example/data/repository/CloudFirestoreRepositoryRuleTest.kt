package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndGetUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CloudFirestoreRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                displayName = "Monish",
                email = ALICE_EMAIL,
                upiHandle = "monish@digiwallet",
                demoBalancePaise = 1_245_000L
            )
        }
        assertTrue(saveResult.isSuccess)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(uid)
        }
        assertTrue(profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertNotNull(profile)
        assertEquals("Monish", profile?.displayName)
        assertEquals(1_245_000L, profile?.demoBalancePaise)
    }

    @Test
    fun createAndObserveTransactions_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudFirestoreRepository(firestore)
        val dynamicTxId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTransaction(
                transactionId = dynamicTxId,
                amountPaise = 45_000L,
                transactionType = "DEBIT",
                category = "Food & Dining",
                title = "Grocery Store",
                counterparty = "store@upi",
                description = "Monthly grocery shopping",
                paymentMethod = "UPI",
                status = "COMPLETED",
                referenceId = "#TXN123456789",
                isDemo = true
            )
        }
        assertTrue(createResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list -> list.any { it.id == dynamicTxId } }
        }
        assertTrue(emitted.any { it.id == dynamicTxId && it.amountPaise == 45_000L })
    }

    @Test
    fun getTransactionById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudFirestoreRepository(firestore)
        val dynamicTxId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTransaction(
                transactionId = dynamicTxId,
                amountPaise = 25_000L,
                transactionType = "DEBIT",
                category = "Food & Dining",
                title = "Starbucks Coffee",
                counterparty = "starbucks@upi",
                description = "Coffee & Snacks",
                paymentMethod = "UPI",
                status = "COMPLETED",
                referenceId = "#TXN999999",
                isDemo = true
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudFirestoreRepository(firestore)
        val crossUserResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getTransactionById(aliceUid, dynamicTxId)
        }
        assertTrue(crossUserResult.isFailure)
        val firestoreEx = crossUserResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CloudFirestoreRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTransactions(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@digiwallet.test"
        const val BOB_EMAIL = "bob@digiwallet.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
