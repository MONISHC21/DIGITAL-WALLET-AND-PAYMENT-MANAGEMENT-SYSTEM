package com.example.data.remote

import kotlinx.coroutines.delay
import java.util.UUID

enum class PaymentEnvironmentMode {
    DEMO_SIMULATION,
    TRUSTED_BACKEND_READY
}

sealed interface PaymentOrderResult {
    data class SimulatedDemoSuccess(
        val referenceId: String,
        val amountPaise: Long,
        val paymentMethod: String,
        val timestampMillis: Long
    ) : PaymentOrderResult

    data class BackendNotConfigured(
        val explanation: String
    ) : PaymentOrderResult

    data class Failure(
        val reason: String
    ) : PaymentOrderResult
}

/**
 * Clean abstraction separating local Demo Wallet simulation from future trusted backend payment gateway verification.
 * Never embeds payment provider secrets in the Android client.
 */
interface PaymentRepository {
    suspend fun executeAddMoney(
        amountPaise: Long,
        paymentMethod: String,
        mode: PaymentEnvironmentMode = PaymentEnvironmentMode.DEMO_SIMULATION
    ): PaymentOrderResult

    suspend fun executePeerTransfer(
        recipientUpi: String,
        recipientName: String,
        amountPaise: Long,
        currentDemoBalancePaise: Long,
        note: String,
        mode: PaymentEnvironmentMode = PaymentEnvironmentMode.DEMO_SIMULATION
    ): PaymentOrderResult
}

class DefaultPaymentRepository : PaymentRepository {
    override suspend fun executeAddMoney(
        amountPaise: Long,
        paymentMethod: String,
        mode: PaymentEnvironmentMode
    ): PaymentOrderResult {
        if (amountPaise <= 0L) {
            return PaymentOrderResult.Failure("Amount must be greater than ₹0.")
        }
        if (amountPaise > 10_000_000L) {
            return PaymentOrderResult.Failure("Single demo deposit cannot exceed ₹1,00,000.")
        }

        return when (mode) {
            PaymentEnvironmentMode.DEMO_SIMULATION -> {
                delay(450) // Realistic processing feedback
                val refId = "#TXN${UUID.randomUUID().toString().filter { it.isDigit() }.take(9).padEnd(9, '7')}"
                PaymentOrderResult.SimulatedDemoSuccess(
                    referenceId = refId,
                    amountPaise = amountPaise,
                    paymentMethod = paymentMethod,
                    timestampMillis = System.currentTimeMillis()
                )
            }
            PaymentEnvironmentMode.TRUSTED_BACKEND_READY -> {
                PaymentOrderResult.BackendNotConfigured(
                    "Live payment processing requires a deployed backend order endpoint and verified webhook signature handler. Use Demo Mode to test simulated deposits safely."
                )
            }
        }
    }

    override suspend fun executePeerTransfer(
        recipientUpi: String,
        recipientName: String,
        amountPaise: Long,
        currentDemoBalancePaise: Long,
        note: String,
        mode: PaymentEnvironmentMode
    ): PaymentOrderResult {
        if (recipientUpi.isBlank() || !recipientUpi.contains("@")) {
            return PaymentOrderResult.Failure("Please enter a valid UPI ID (e.g., name@upi).")
        }
        if (amountPaise <= 0L) {
            return PaymentOrderResult.Failure("Transfer amount must be greater than ₹0.")
        }
        if (amountPaise > currentDemoBalancePaise) {
            return PaymentOrderResult.Failure("Insufficient simulated demo balance for this transfer.")
        }

        return when (mode) {
            PaymentEnvironmentMode.DEMO_SIMULATION -> {
                delay(450)
                val refId = "#TXN${UUID.randomUUID().toString().filter { it.isDigit() }.take(9).padEnd(9, '8')}"
                PaymentOrderResult.SimulatedDemoSuccess(
                    referenceId = refId,
                    amountPaise = amountPaise,
                    paymentMethod = "UPI",
                    timestampMillis = System.currentTimeMillis()
                )
            }
            PaymentEnvironmentMode.TRUSTED_BACKEND_READY -> {
                PaymentOrderResult.BackendNotConfigured(
                    "Live peer-to-peer UPI transfers require a licensed NPCI/PSP backend integration. Use Demo Mode to simulate transfers."
                )
            }
        }
    }
}
