package com.example

import com.example.data.model.CurrencyUtils
import com.example.data.repository.DigiWalletRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun currencyFormatting_formatsIndianRupeesCorrectly() {
        assertEquals("₹12,450.00", CurrencyUtils.formatPaise(1_245_000L, showDecimals = true))
        assertEquals("₹450", CurrencyUtils.formatPaise(45_000L, showDecimals = false))
        assertEquals("+₹1,000", CurrencyUtils.formatPaise(100_000L, includeSign = true, isCredit = true))
        assertEquals("-₹450", CurrencyUtils.formatPaise(45_000L, includeSign = true, isCredit = false))
    }

    @Test
    fun parseRupeesToPaise_validatesAndParsesWithoutFloatLoss() {
        assertEquals(25_000L, CurrencyUtils.parseRupeesToPaise("250"))
        assertEquals(25_050L, CurrencyUtils.parseRupeesToPaise("250.5"))
        assertEquals(25_075L, CurrencyUtils.parseRupeesToPaise("250.75"))
        assertNull(CurrencyUtils.parseRupeesToPaise(""))
        assertNull(CurrencyUtils.parseRupeesToPaise("-100"))
        assertNull(CurrencyUtils.parseRupeesToPaise("abc"))
    }

    @Test
    fun qrParsing_validatesUpiQrPayloads() {
        val validQr = "upi://pay?pa=starbucks@upi&pn=Starbucks%20Coffee&am=250&tn=Coffee%20%26%20Snacks"
        val parsed = DigiWalletRepository.parseUpiQrPayload(validQr)
        assertTrue(parsed.isValid)
        assertEquals("Starbucks Coffee", parsed.merchantName)
        assertEquals("starbucks@upi", parsed.upiId)
        assertEquals(25_000L, parsed.suggestedAmountPaise)
        assertEquals("Coffee & Snacks", parsed.note)

        val invalidQr = "https://malicious.example.com/pay"
        val invalidParsed = DigiWalletRepository.parseUpiQrPayload(invalidQr)
        assertFalse(invalidParsed.isValid)
    }
}
