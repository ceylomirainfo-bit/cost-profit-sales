package com.example

import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateUtils
import com.example.ui.viewmodel.LineItemDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSaleLineCalculations() {
        // Line item: 3 units of 100g Chilli Powder @ Selling 360, Cost 230, Discount 80
        val draft = LineItemDraft(
            unitCostPrice = 230.0,
            unitSellingPrice = 360.0,
            quantity = 3,
            discount = 80.0
        )

        val expectedGross = 360.0 * 3 // 1080.0
        val expectedNet = 1080.0 - 80.0 // 1000.0
        val expectedCOGS = 230.0 * 3 // 690.0
        val expectedProfit = expectedNet - expectedCOGS // 310.0

        assertEquals(expectedGross, draft.grossAmount, 0.001)
        assertEquals(expectedNet, draft.netRevenue, 0.001)
        assertEquals(expectedCOGS, draft.costOfGoodsSold, 0.001)
        assertEquals(expectedProfit, draft.grossProfit, 0.001)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.formatLkr(1250.5)
        assertEquals("LKR 1,250.50", formatted)

        val formattedZero = CurrencyFormatter.formatLkr(0.0)
        assertEquals("LKR 0.00", formattedZero)
    }

    @Test
    fun testDateFormatting() {
        val display = DateUtils.formatIsoToDisplay("2026-10-24")
        assertEquals("24 Oct 2026", display)
    }

    @Test
    fun testPinValidationRules() {
        // Valid 4-digit pin
        val validPin = "1234"
        val isNumericAnd4Digits = validPin.length == 4 && validPin.all { it.isDigit() }
        assertEquals(true, isNumericAnd4Digits)

        // Invalid lengths or characters
        val tooShort = "123"
        val tooLong = "12345"
        val nonNumeric = "12a4"
        assertEquals(false, tooShort.length == 4 && tooShort.all { it.isDigit() })
        assertEquals(false, tooLong.length == 4 && tooLong.all { it.isDigit() })
        assertEquals(false, nonNumeric.length == 4 && nonNumeric.all { it.isDigit() })
    }
}

