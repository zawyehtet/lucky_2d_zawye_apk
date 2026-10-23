package com.example

import com.example.logic.PatternAlgorithms
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPowerAndNakhatNumbers() {
        assertEquals(10, PatternAlgorithms.POWER_NUMBERS.size)
        assertTrue(PatternAlgorithms.POWER_NUMBERS.contains("05"))
        assertTrue(PatternAlgorithms.POWER_NUMBERS.contains("94"))

        assertEquals(10, PatternAlgorithms.NAKHAT_NUMBERS.size)
        assertTrue(PatternAlgorithms.NAKHAT_NUMBERS.contains("07"))
        assertTrue(PatternAlgorithms.NAKHAT_NUMBERS.contains("96"))
    }

    @Test
    fun testBrothersAndDoubles() {
        assertEquals(20, PatternAlgorithms.BROTHER_NUMBERS.size)
        assertTrue(PatternAlgorithms.BROTHER_NUMBERS.contains("01"))
        assertTrue(PatternAlgorithms.BROTHER_NUMBERS.contains("90"))

        assertEquals(10, PatternAlgorithms.DOUBLES.size)
        assertTrue(PatternAlgorithms.DOUBLES.contains("00"))
        assertTrue(PatternAlgorithms.DOUBLES.contains("99"))
    }

    @Test
    fun testParityCombinations() {
        assertEquals(25, PatternAlgorithms.getEvenEven().size)
        assertEquals(25, PatternAlgorithms.getOddOdd().size)
        assertEquals(25, PatternAlgorithms.getEvenOdd().size)
        assertEquals(25, PatternAlgorithms.getOddEven().size)
    }

    @Test
    fun testHeadTailIncludesBrake() {
        // Head '1': 10..19 (10 numbers)
        val head1 = PatternAlgorithms.getHead('1')
        assertEquals(10, head1.size)
        assertTrue(head1.contains("10"))
        assertTrue(head1.contains("19"))

        // Tail '1': 01..91 (10 numbers)
        val tail1 = PatternAlgorithms.getTail('1')
        assertEquals(10, tail1.size)
        assertTrue(tail1.contains("01"))
        assertTrue(tail1.contains("91"))

        // Includes '5': 19 numbers
        val inc5 = PatternAlgorithms.getIncludes('5')
        assertEquals(19, inc5.size)

        // Brake 6: (d1 + d2) % 10 == 6
        val brake6 = PatternAlgorithms.getBrake(6)
        assertTrue(brake6.contains("06"))
        assertTrue(brake6.contains("15"))
        assertTrue(brake6.contains("24"))
        assertTrue(brake6.contains("33"))
    }

    @Test
    fun testKway() {
        // Digits "123" without doubles: 3 * 2 = 6 pairs
        val kwayNoDoubles = PatternAlgorithms.getKway("123", withDoubles = false)
        assertEquals(6, kwayNoDoubles.size)
        assertFalse(kwayNoDoubles.contains("11"))

        // Digits "123" with doubles: 3 * 3 = 9 pairs
        val kwayWithDoubles = PatternAlgorithms.getKway("123", withDoubles = true)
        assertEquals(9, kwayWithDoubles.size)
        assertTrue(kwayWithDoubles.contains("11"))
    }

    @Test
    fun testQuickEntryMode1() {
        val blocked = setOf("99")
        val maxBet = 100000L

        // 01.78=500
        val r1 = PatternAlgorithms.parseQuickEntryMode1("01.78=500", blocked, maxBet)
        assertEquals(2, r1.items.size)
        assertEquals("01", r1.items[0].n)
        assertEquals(500L, r1.items[0].a)
        assertEquals("78", r1.items[1].n)
        assertEquals(500L, r1.items[1].a)

        // 01.78R500 -> 01, 10, 78, 87
        val r2 = PatternAlgorithms.parseQuickEntryMode1("01.78R500", blocked, maxBet)
        assertEquals(4, r2.items.size)

        // 01.78=500/100 -> 01=500, 10=100, 78=500, 87=100
        val r3 = PatternAlgorithms.parseQuickEntryMode1("01.78=500/100", blocked, maxBet)
        assertEquals(4, r3.items.size)
        assertEquals(500L, r3.items.first { it.n == "01" }.a)
        assertEquals(100L, r3.items.first { it.n == "10" }.a)
    }

    @Test
    fun testQuickEntryMode2() {
        val blocked = setOf<String>()
        val maxBet = 100000L

        // 5a500 -> includes 5 (19 items)
        val rA = PatternAlgorithms.parseQuickEntryMode2("5a500", blocked, maxBet)
        assertEquals(19, rA.items.size)

        // 6b600 -> brake 6 (10 items)
        val rB = PatternAlgorithms.parseQuickEntryMode2("6b600", blocked, maxBet)
        assertEquals(10, rB.items.size)

        // 1t500 -> head 1 (10 items)
        val rT = PatternAlgorithms.parseQuickEntryMode2("1t500", blocked, maxBet)
        assertEquals(10, rT.items.size)

        // 1w500 -> tail 1 (10 items)
        val rW = PatternAlgorithms.parseQuickEntryMode2("1w500", blocked, maxBet)
        assertEquals(10, rW.items.size)

        // p3500 -> power (10 items)
        val rP = PatternAlgorithms.parseQuickEntryMode2("p3500", blocked, maxBet)
        assertEquals(10, rP.items.size)

        // n7500 -> nakhat (10 items)
        val rN = PatternAlgorithms.parseQuickEntryMode2("n7500", blocked, maxBet)
        assertEquals(10, rN.items.size)

        // s500 -> doubles (10 items)
        val rS = PatternAlgorithms.parseQuickEntryMode2("s500", blocked, maxBet)
        assertEquals(10, rS.items.size)

        // x200 -> brothers (20 items)
        val rX = PatternAlgorithms.parseQuickEntryMode2("x200", blocked, maxBet)
        assertEquals(20, rX.items.size)
    }

    @Test
    fun testEmptyInputParsingAndConditionalSaveButton() {
        assertEquals(0L, com.example.ui.MainViewModel.parseAmountOrZero(""))
        assertEquals(0L, com.example.ui.MainViewModel.parseAmountOrZero("   "))
        assertEquals(5000L, com.example.ui.MainViewModel.parseAmountOrZero(" 5000 "))

        assertEquals(0.0, com.example.ui.MainViewModel.parseDoubleOrZero(""), 0.001)
        assertEquals(0.0, com.example.ui.MainViewModel.parseDoubleOrZero("  "), 0.001)
        assertEquals(15.5, com.example.ui.MainViewModel.parseDoubleOrZero("15.5"), 0.001)

        // Save button visibility: hidden when blank or equal to saved value
        assertFalse(com.example.ui.MainViewModel.shouldShowSaveButton("", ""))
        assertFalse(com.example.ui.MainViewModel.shouldShowSaveButton("   ", ""))
        assertFalse(com.example.ui.MainViewModel.shouldShowSaveButton("88", "88"))
        assertTrue(com.example.ui.MainViewModel.shouldShowSaveButton("88", ""))
        assertTrue(com.example.ui.MainViewModel.shouldShowSaveButton("99", "88"))

        // Round management Save button visibility: hidden when blank, visible when non-blank
        assertFalse(com.example.ui.MainViewModel.shouldShowRoundSaveButton(""))
        assertFalse(com.example.ui.MainViewModel.shouldShowRoundSaveButton("   "))
        assertTrue(com.example.ui.MainViewModel.shouldShowRoundSaveButton("မနက်ပိုင်း"))
    }

    @Test
    fun testDailySummaryProfitLossFormatting() {
        assertEquals("အမြတ် +12,000", com.example.ui.MainViewModel.formatDailyTotalResultText(12000L))
        assertEquals("အရှုံး -12,000", com.example.ui.MainViewModel.formatDailyTotalResultText(-12000L))
        assertEquals("0", com.example.ui.MainViewModel.formatDailyTotalResultText(0L))
    }

    @Test
    fun testBetAmountValidator() {
        // 1. Valid normal amounts (e.g. 100, 1000, 50000, 1000000, and MAX_BET_AMOUNT = 999_999_999L) pass
        for (validAmt in listOf(100L, 1000L, 50000L, 1000000L, com.example.data.MAX_BET_AMOUNT)) {
            assertTrue(com.example.data.validateBetAmount(validAmt).isValid)
            assertTrue(com.example.data.validateBetAmount(validAmt.toString()).isValid)
        }
        assertTrue(com.example.data.validateBetAmount(" 50000 ").isValid)

        // 2. 0 and negative values fail
        assertFalse(com.example.data.validateBetAmount(0L).isValid)
        assertFalse(com.example.data.validateBetAmount(-1L).isValid)
        assertFalse(com.example.data.validateBetAmount(-500L).isValid)
        assertEquals(com.example.data.BetAmountValidationResult.ZeroOrNegative, com.example.data.validateBetAmount("0"))
        assertEquals(com.example.data.BetAmountValidationResult.ZeroOrNegative, com.example.data.validateBetAmount("-100"))

        // 3. Empty and non-numeric strings fail
        assertEquals(com.example.data.BetAmountValidationResult.Empty, com.example.data.validateBetAmount(""))
        assertEquals(com.example.data.BetAmountValidationResult.Empty, com.example.data.validateBetAmount("   "))
        assertEquals(com.example.data.BetAmountValidationResult.Empty, com.example.data.validateBetAmount(null))
        assertEquals(com.example.data.BetAmountValidationResult.NonNumeric, com.example.data.validateBetAmount("abc"))
        assertEquals(com.example.data.BetAmountValidationResult.NonNumeric, com.example.data.validateBetAmount("100.5"))
        assertEquals(com.example.data.BetAmountValidationResult.NonNumeric, com.example.data.validateBetAmount("12a00"))

        // 4. Values > MAX_BET_AMOUNT (999_999_999L) fail
        assertFalse(com.example.data.validateBetAmount(com.example.data.MAX_BET_AMOUNT + 1L).isValid)
        assertEquals(com.example.data.BetAmountValidationResult.TooLarge, com.example.data.validateBetAmount("1000000000"))
        assertEquals(com.example.data.BetAmountValidationResult.TooLarge, com.example.data.validateBetAmount("99999999999999999999"))
    }

    @Test
    fun testFastBet1And2MultiLineAndClipboardFormat() {
        val blocked = setOf<String>()

        // Fast Bet 1: straight/reverse on double number 11 does not duplicate
        val rDouble = PatternAlgorithms.parseQuickEntryMode1("11.01=500/100", blocked)
        assertEquals(3, rDouble.items.size)
        assertEquals(1, rDouble.items.count { it.n == "11" && it.a == 500L })
        assertEquals(1, rDouble.items.count { it.n == "01" && it.a == 500L })
        assertEquals(1, rDouble.items.count { it.n == "10" && it.a == 100L })

        // Fast Bet 2: multi-line with standard formats, k (ခွေ), and inline guide notes
        val fastBet2Input = """
            5a500 (5 အပါ)
            6b600 (6 ဘရိတ်)
            1t500 (1 ထိပ်)
            1w500 (1 နောက်)
            p3500 (ပါဝါ 3500)
            n7500 (နက္ခတ် 7500)
            s500 (အပူး 500)
            x200 (ညီအစ်ကို 200)
            123k500 (123 ခွေ 500)
            56=2000/1000
            89r350
            79=500

            ( a=အပါ, b=ဘရိတ်, t=ထိပ်, w=နောက်, p=ပါဝါ, n=နက္ခတ်, s=အပူး, x=ညီကို, k=ခွေ )
        """.trimIndent()
        val rFast2 = PatternAlgorithms.parseQuickEntryMode2(fastBet2Input, blocked)
        assertEquals(0, rFast2.skippedCount)
        // 19 + 10 + 10 + 10 + 10 + 10 + 10 + 20 + 6 (123k500 -> 12,13,21,23,31,32) + 2 (56,65) + 2 (89,98) + 1 (79) = 110 items
        assertEquals(110, rFast2.items.size)

        // Agent 2D Settlement clipboard text format verification
        val agent = com.example.data.Agent(id = 1, name = "Aa", rate = 15.0, mult = 80.0)
        val settlementItem = com.example.ui.Agent2DSettlementItem(
            agent = agent,
            voucherCount = 1,
            totalBet = 119600L,
            commission = 17940L,
            netAmount = 101660L,
            winBetAmount = 0L,
            payout = 0L,
            profitLoss = 101660L
        )
        val formattedText = com.example.ui.screens.formatAgent2DSettlementClipboardText(
            date = "2026-10-21",
            session = "21.AM",
            winningNumber = "16",
            item = settlementItem
        )
        val expectedText = """
            2D စာရင်းချုပ်
            ရက်စွဲ : 2026-10-21 (21.AM)
            အမည်= Aa
            စုစုပေါင်း ရောင်းကြေး = 119600
            ကော်မရှင် (15%) =17940
            ကော်နုတ်ပြီး =101660
            ပေါက်သီး(16)=0
            လျော်ကြေး =0
            စုစုပေါင်း= 101660
        """.trimIndent()
        assertEquals(expectedText, formattedText)
    }

    @Test
    fun testOfflineLicenseManagerKeyGenerationAndVerification() {
        assertEquals(
            "Lucky3D_Super_Secret_Key_2026!#",
            com.example.security.SecurityManager.getSharedSecret()
        )

        val sampleDeviceId = "8f4a2c91e0b37d12"
        val key1 = com.example.security.LicenseManager.generateExpectedKey(sampleDeviceId)
        val key2 = com.example.security.LicenseManager.generateExpectedKey("  8F4A2C91E0B37D12  ")
        assertEquals(key1, key2)
        assertTrue(Regex("^[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$").matches(key1))

        val sig1 = com.example.security.LicenseManager.computeActivationSignature(sampleDeviceId)
        val sig2 = com.example.security.LicenseManager.computeActivationSignature("other_device_id")
        assertFalse(com.example.security.SecurityManager.constantTimeEquals(sig1, sig2))
    }

    @Test
    fun testParseBetInputCases() {
        // Case 1: Standard Single Amount (58=3000)
        val singleResult = PatternAlgorithms.parseBetInput("58=3000")
        assertEquals(1, singleResult.size)
        assertEquals("58", singleResult[0].n)
        assertEquals(3000L, singleResult[0].a)

        // Case 2: Split Amount with Slash (58=3000/2000)
        val splitResult = PatternAlgorithms.parseBetInput("58=3000/2000")
        assertEquals(2, splitResult.size)
        assertEquals("58", splitResult[0].n)
        assertEquals(3000L, splitResult[0].a)
        assertEquals("85", splitResult[1].n)
        assertEquals(2000L, splitResult[1].a)

        // Case 3: Edge Case Handling - Twin numbers (55=3000/2000) combines to 5000
        val twinResult = PatternAlgorithms.parseBetInput("55=3000/2000")
        assertEquals(1, twinResult.size)
        assertEquals("55", twinResult[0].n)
        assertEquals(5000L, twinResult[0].a)

        // Spacing tolerance (58 = 3000 / 2000)
        val spacedResult = PatternAlgorithms.parseBetInput(" 58 = 3000 / 2000 ")
        assertEquals(2, spacedResult.size)
        assertEquals("58", spacedResult[0].n)
        assertEquals(3000L, spacedResult[0].a)
        assertEquals("85", spacedResult[1].n)
        assertEquals(2000L, spacedResult[1].a)
    }

    @Test
    fun testValidateBetAmountAndParseAmountOrZeroWithSlash() {
        assertTrue(com.example.data.validateBetAmount("3000/2000").isValid)
        assertTrue(com.example.data.validateBetAmount("500/100").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/").isValid)
        assertFalse(com.example.data.validateBetAmount("/2000").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/-100").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/abc").isValid)
        assertFalse(com.example.data.validateBetAmount("3000/2000/1000").isValid)

        assertEquals(5000L, com.example.ui.MainViewModel.parseAmountOrZero("3000/2000"))
        assertEquals(600L, com.example.ui.MainViewModel.parseAmountOrZero("500/100"))
    }
}

