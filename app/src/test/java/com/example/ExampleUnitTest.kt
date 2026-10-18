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
}
