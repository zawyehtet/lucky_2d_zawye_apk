package com.example.logic

import com.example.data.BetAmountValidationResult
import com.example.data.BetItem
import com.example.data.MAX_BET_AMOUNT
import com.example.data.validateBetAmount

object PatternAlgorithms {

    val POWER_NUMBERS = listOf(
        "05", "50", "16", "61", "27", "72", "38", "83", "49", "94"
    )

    val NAKHAT_NUMBERS = listOf(
        "07", "70", "18", "81", "24", "42", "35", "53", "69", "96"
    )

    val BROTHER_NUMBERS = listOf(
        "01", "10", "12", "21", "23", "32", "34", "43", "45", "54",
        "56", "65", "67", "76", "78", "87", "89", "98", "90", "09"
    )

    val DOUBLES = listOf(
        "00", "11", "22", "33", "44", "55", "66", "77", "88", "99"
    )

    fun reverse2D(n: String): String {
        return if (n.length == 2) "${n[1]}${n[0]}" else n
    }

    // စုံစုံ: both digits even (0, 2, 4, 6, 8) -> 25 numbers
    fun getEvenEven(): List<String> {
        val evens = listOf('0', '2', '4', '6', '8')
        val list = mutableListOf<String>()
        for (d1 in evens) {
            for (d2 in evens) {
                list.add("$d1$d2")
            }
        }
        return list
    }

    // မမ: both digits odd (1, 3, 5, 7, 9) -> 25 numbers
    fun getOddOdd(): List<String> {
        val odds = listOf('1', '3', '5', '7', '9')
        val list = mutableListOf<String>()
        for (d1 in odds) {
            for (d2 in odds) {
                list.add("$d1$d2")
            }
        }
        return list
    }

    // စုံမ: even first, odd second -> 25 numbers
    fun getEvenOdd(): List<String> {
        val evens = listOf('0', '2', '4', '6', '8')
        val odds = listOf('1', '3', '5', '7', '9')
        val list = mutableListOf<String>()
        for (d1 in evens) {
            for (d2 in odds) {
                list.add("$d1$d2")
            }
        }
        return list
    }

    // မစုံ: odd first, even second -> 25 numbers
    fun getOddEven(): List<String> {
        val odds = listOf('1', '3', '5', '7', '9')
        val evens = listOf('0', '2', '4', '6', '8')
        val list = mutableListOf<String>()
        for (d1 in odds) {
            for (d2 in evens) {
                list.add("$d1$d2")
            }
        }
        return list
    }

    // ထိပ် D: D0..D9 (10 numbers)
    fun getHead(d: Char): List<String> {
        val list = mutableListOf<String>()
        for (i in 0..9) {
            list.add("$d$i")
        }
        return list
    }

    // နောက် D: 0D..9D (10 numbers)
    fun getTail(d: Char): List<String> {
        val list = mutableListOf<String>()
        for (i in 0..9) {
            list.add("$i$d")
        }
        return list
    }

    // အပါ: all 19 numbers containing digit D
    fun getIncludes(d: Char): List<String> {
        val set = linkedSetOf<String>()
        for (i in 0..9) {
            set.add("$d$i")
            set.add("$i$d")
        }
        return set.toList()
    }

    // ဘရိတ် N: (d1 + d2) % 10 == N
    fun getBrake(brake: Int): List<String> {
        val target = ((brake % 10) + 10) % 10
        val list = mutableListOf<String>()
        for (i in 0..9) {
            for (j in 0..9) {
                if ((i + j) % 10 == target) {
                    list.add("$i$j")
                }
            }
        }
        return list
    }

    // ခွေ: all ordered pairs from distinct digits.
    // withDoubles = true: include D1D1, D2D2
    fun getKway(digitsStr: String, withDoubles: Boolean): List<String> {
        val distinct = digitsStr.filter { it.isDigit() }.toSet().toList()
        if (distinct.size < 2) return emptyList()
        val list = mutableListOf<String>()
        for (i in distinct.indices) {
            for (j in distinct.indices) {
                if (i != j || withDoubles) {
                    list.add("${distinct[i]}${distinct[j]}")
                }
            }
        }
        return list
    }

    // Generate list from pattern name + optional digit string
    fun generateNumbersForPattern(
        pattern: String,
        inputNumber: String,
        withReverse: Boolean,
        withDoublesForKway: Boolean = false
    ): List<String> {
        val baseList = when (pattern) {
            "ဒဲ့" -> if (inputNumber.length == 2) listOf(inputNumber) else emptyList()
            "ပါဝါ" -> POWER_NUMBERS
            "နက္ခတ်" -> NAKHAT_NUMBERS
            "ညီအစ်ကို" -> BROTHER_NUMBERS
            "အပူး" -> DOUBLES
            "စုံစုံ" -> getEvenEven()
            "မမ" -> getOddOdd()
            "စုံမ" -> getEvenOdd()
            "မစုံ" -> getOddEven()
            "ထိပ်" -> {
                val d = inputNumber.firstOrNull() ?: '0'
                getHead(d)
            }
            "နောက်" -> {
                val d = inputNumber.firstOrNull() ?: '0'
                getTail(d)
            }
            "အပါ" -> {
                val d = inputNumber.firstOrNull() ?: '0'
                getIncludes(d)
            }
            "ဘရိတ်" -> {
                val b = inputNumber.firstOrNull()?.digitToIntOrNull() ?: 0
                getBrake(b)
            }
            "ခွေ" -> {
                getKway(inputNumber, withDoublesForKway)
            }
            else -> if (inputNumber.length == 2) listOf(inputNumber) else emptyList()
        }

        if (!withReverse) {
            return baseList
        }

        // With reverse: add reverse of every number deduplicated
        val resultSet = linkedSetOf<String>()
        for (n in baseList) {
            resultSet.add(n)
            resultSet.add(reverse2D(n))
        }
        return resultSet.toList()
    }

    // Parsing results container
    data class ParseResult(
        val items: List<BetItem>,
        val skippedCount: Int,
        val blockedSkipped: List<String>
    )

    /**
     * Strips comment lines (such as `( a=အပါ, ... )` or divider lines `---------`) and inline parenthetical guide notes before tokenizing.
     */
    private fun stripCommentAndGuideLines(text: String): String {
        return text.lines()
            .map { line -> line.replace(Regex("\\([^)]*\\)"), "").trim() }
            .filter { line ->
                line.isNotEmpty() &&
                    !line.startsWith("(") &&
                    !line.endsWith(")") &&
                    !line.all { c -> c == '-' || c == '=' || c.isWhitespace() }
            }
            .joinToString("\n")
    }

    // Quick Entry Mode 1 ("အမြန်ထိုး 1"):
    // Format: "01.78=500" or "01.78R500" or "01.78=500/100"
    // Separators: multiple lines, space, comma, dot separation
    fun parseQuickEntryMode1(
        text: String,
        blocked: Set<String>,
        maxBet: Long = MAX_BET_AMOUNT
    ): ParseResult {
        val effectiveLimit = minOf(maxBet, MAX_BET_AMOUNT)
        val items = mutableListOf<BetItem>()
        var skipped = 0
        val blockedList = mutableListOf<String>()

        val cleanedText = stripCommentAndGuideLines(text)
        if (cleanedText.isBlank()) {
            return ParseResult(items, skipped, blockedList)
        }

        // Split into entry tokens while supporting space/comma/dot separation of numbers before '=' or 'R'
        // First normalize spaces/commas between 2-digit numbers on the same line before '=' or 'R' into dots (e.g. "01 78=500" or "01,78=500" -> "01.78=500")
        val normalizedLines = cleanedText.lines().map { line ->
            line.trim().replace(Regex("(\\b\\d{2})[\\s,]+(?=\\d{2}(?:[.\\s,]+\\d{2})*\\s*[=rR])"), "$1.")
                .replace(Regex("\\s*([=rR/])\\s*"), "$1")
        }
        val tokens = normalizedLines
            .flatMap { it.split(Regex("[\\s;|]+|,(?=\\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])")) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (token in tokens) {
            try {
                // Pattern 3 (Different Amounts / Straight & Reverse): e.g. 01.78=500/100 or 56=2000/1000
                if (token.contains("=") && token.contains("/")) {
                    val eqParts = token.split("=")
                    if (eqParts.size == 2) {
                        val numPart = eqParts[0]
                        val amtParts = eqParts[1].split("/")
                        val directRaw = amtParts.getOrNull(0)
                        val reverseRaw = amtParts.getOrNull(1)
                        val directVal = validateBetAmount(directRaw)
                        val amtDirect = directRaw?.trim()?.toLongOrNull() ?: 0L
                        val amtReverse = reverseRaw?.trim()?.toLongOrNull() ?: -1L
                        val reverseValid = amtReverse == 0L || (validateBetAmount(reverseRaw).isValid && amtReverse <= effectiveLimit)
                        val numbers = numPart.split(Regex("[.,\\s]+")).filter { it.length == 2 && it.all { c -> c.isDigit() } }
                        if (numbers.isEmpty() || !directVal.isValid || amtDirect > effectiveLimit || !reverseValid) {
                            skipped++
                        } else {
                            for (n in numbers) {
                                if (blocked.contains(n)) {
                                    blockedList.add(n)
                                } else {
                                    items.add(BetItem(n, amtDirect))
                                }
                                val rev = reverse2D(n)
                                // If straight and reverse are identical (double like 11, 22), add only the straight amount without duplicating
                                if (rev != n && amtReverse > 0L) {
                                    if (blocked.contains(rev)) {
                                        blockedList.add(rev)
                                    } else {
                                        items.add(BetItem(rev, amtReverse))
                                    }
                                }
                            }
                        }
                        continue
                    }
                }

                // Pattern 2 (Same Amount with R / Reverse): e.g. 01.78R500 or 01.78r500 or 89r350
                if (token.contains("r", ignoreCase = true)) {
                    val rIndex = token.indexOfFirst { it.equals('r', ignoreCase = true) }
                    val numPart = token.substring(0, rIndex)
                    val amtPart = token.substring(rIndex + 1)
                    val amtVal = validateBetAmount(amtPart)
                    val amount = amtPart.trim().toLongOrNull() ?: 0L
                    val numbers = numPart.split(Regex("[.,\\s]+")).filter { it.length == 2 && it.all { c -> c.isDigit() } }
                    if (numbers.isEmpty() || !amtVal.isValid || amount > effectiveLimit) {
                        skipped++
                    } else {
                        for (n in numbers) {
                            val rev = reverse2D(n)
                            val listToBet = if (n == rev) listOf(n) else listOf(n, rev)
                            for (num in listToBet) {
                                if (blocked.contains(num)) {
                                    blockedList.add(num)
                                } else {
                                    items.add(BetItem(num, amount))
                                }
                            }
                        }
                    }
                    continue
                }

                // Pattern 1 (Same Amount): e.g. 01.78=500 or 79=500
                if (token.contains("=")) {
                    val parts = token.split("=")
                    if (parts.size == 2) {
                        val numPart = parts[0]
                        val amtVal = validateBetAmount(parts[1])
                        val amount = parts[1].trim().toLongOrNull() ?: 0L
                        val numbers = numPart.split(Regex("[.,\\s]+")).filter { it.length == 2 && it.all { c -> c.isDigit() } }
                        if (numbers.isEmpty() || !amtVal.isValid || amount > effectiveLimit) {
                            skipped++
                        } else {
                            for (n in numbers) {
                                if (blocked.contains(n)) {
                                    blockedList.add(n)
                                } else {
                                    items.add(BetItem(n, amount))
                                }
                            }
                        }
                        continue
                    }
                }

                skipped++
            } catch (e: Exception) {
                skipped++
            }
        }

        return ParseResult(items, skipped, blockedList)
    }

    // Quick Entry Mode 2 ("အမြန်ထိုး 2"):
    // Formats:
    // 5a500, 6b600, 1t500, 1w500, p3500 / p500, n7500 / n500, s500, x200, 56=2000/1000, 89r350, 79=500
    // (a=အပါ, b=ဘရိတ်, t=ထိပ်, w=နောက်, p=ပါဝါ, n=နက္ခတ်, s=အပူး, x=ညီကို)
    fun parseQuickEntryMode2(
        text: String,
        blocked: Set<String>,
        maxBet: Long = MAX_BET_AMOUNT
    ): ParseResult {
        val effectiveLimit = minOf(maxBet, MAX_BET_AMOUNT)
        val items = mutableListOf<BetItem>()
        var skipped = 0
        val blockedList = mutableListOf<String>()

        val cleanedText = stripCommentAndGuideLines(text)
        if (cleanedText.isBlank()) {
            return ParseResult(items, skipped, blockedList)
        }

        val normalizedLines = cleanedText.lines().map { line ->
            line.trim().replace(Regex("(\\b\\d{2})[\\s,]+(?=\\d{2}(?:[.\\s,]+\\d{2})*\\s*[=rR])"), "$1.")
                .replace(Regex("\\s*([=rR/])\\s*"), "$1")
        }
        val tokens = normalizedLines
            .flatMap { it.split(Regex("[\\s;|]+|,(?=\\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])")) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (rawToken in tokens) {
            if (rawToken.startsWith("(") || rawToken.endsWith(")")) continue
            val token = rawToken.lowercase().trim()
            try {
                // Standard formats in Fast Bet 2: 56=2000/1000, 89r350, 79=500, 01.78r500
                if (token.contains("=") || (token.contains("r") && token.first().isDigit())) {
                    val res = parseQuickEntryMode1(token, blocked, effectiveLimit)
                    items.addAll(res.items)
                    skipped += res.skippedCount
                    blockedList.addAll(res.blockedSkipped)
                    continue
                }

                // Digit + letter + amount: a (အပါ), b (ဘရိတ်), t (ထိပ်), w (နောက်)
                val matchDigitLetterAmt = Regex("^(\\d)([abtw])(\\d+)$").find(token)
                if (matchDigitLetterAmt != null) {
                    val digitChar = matchDigitLetterAmt.groupValues[1][0]
                    val letter = matchDigitLetterAmt.groupValues[2]
                    val rawAmt = matchDigitLetterAmt.groupValues[3]
                    val amtVal = validateBetAmount(rawAmt)
                    val amount = rawAmt.toLongOrNull() ?: 0L

                    if (!amtVal.isValid || amount > effectiveLimit) {
                        skipped++
                        continue
                    }

                    val nums = when (letter) {
                        "a" -> getIncludes(digitChar)
                        "b" -> getBrake(digitChar.digitToInt())
                        "t" -> getHead(digitChar)
                        "w" -> getTail(digitChar)
                        else -> emptyList()
                    }

                    for (n in nums) {
                        if (blocked.contains(n)) {
                            blockedList.add(n)
                        } else {
                            items.add(BetItem(n, amount))
                        }
                    }
                    continue
                }

                // Digits (2..9) + k (ခွေ) + amount: e.g. 123k500
                val matchKwayAmt = Regex("^(\\d{2,9})k(\\d+)$").find(token)
                if (matchKwayAmt != null) {
                    val digitsStr = matchKwayAmt.groupValues[1]
                    val rawAmt = matchKwayAmt.groupValues[2]
                    val amtVal = validateBetAmount(rawAmt)
                    val amount = rawAmt.toLongOrNull() ?: 0L
                    val nums = getKway(digitsStr, withDoubles = false)
                    if (!amtVal.isValid || amount > effectiveLimit || nums.isEmpty()) {
                        skipped++
                        continue
                    }
                    for (n in nums) {
                        if (blocked.contains(n)) {
                            blockedList.add(n)
                        } else {
                            items.add(BetItem(n, amount))
                        }
                    }
                    continue
                }

                // Letter + amount (or optional digit + letter + amount for p/n/s/x):
                // e.g. p3500, p500, n7500, n500, s500, x200, or 3p500, 7n500
                val matchLetterAmt = Regex("^(?:\\d)?([pnsx])(\\d+)$").find(token)
                if (matchLetterAmt != null) {
                    val letter = matchLetterAmt.groupValues[1]
                    val rawAmt = matchLetterAmt.groupValues[2]
                    val amtVal = validateBetAmount(rawAmt)
                    val amount = rawAmt.toLongOrNull() ?: 0L

                    if (!amtVal.isValid || amount > effectiveLimit) {
                        skipped++
                        continue
                    }

                    val nums = when (letter) {
                        "p" -> POWER_NUMBERS
                        "n" -> NAKHAT_NUMBERS
                        "s" -> DOUBLES
                        "x" -> BROTHER_NUMBERS
                        else -> emptyList()
                    }

                    for (n in nums) {
                        if (blocked.contains(n)) {
                            blockedList.add(n)
                        } else {
                            items.add(BetItem(n, amount))
                        }
                    }
                    continue
                }

                skipped++
            } catch (e: Exception) {
                skipped++
            }
        }

        return ParseResult(items, skipped, blockedList)
    }

    /**
     * Validates all bet amounts inside a Quick Entry text string using the shared [validateBetAmount] rule.
     */
    fun validateQuickEntryAmounts(text: String, mode: Int): BetAmountValidationResult {
        val cleaned = stripCommentAndGuideLines(text)
        if (cleaned.isEmpty()) return BetAmountValidationResult.Empty

        val normalizedLines = cleaned.lines().map { line ->
            line.trim().replace(Regex("(\\b\\d{2})[\\s,]+(?=\\d{2}(?:[.\\s,]+\\d{2})*\\s*[=rR])"), "$1.")
                .replace(Regex("\\s*([=rR/])\\s*"), "$1")
        }
        val tokens = normalizedLines
            .flatMap { it.split(Regex("[\\s;|]+|,(?=\\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[0-9]{2,9}[kK]|[pnsxPNSX][0-9])")) }
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("(") && !it.endsWith(")") }

        if (tokens.isEmpty()) return BetAmountValidationResult.Empty

        for (rawToken in tokens) {
            val token = rawToken.trim()
            if (token.contains("=") && token.contains("/")) {
                val eqParts = token.split("=")
                if (eqParts.size == 2) {
                    val amtParts = eqParts[1].split("/")
                    val v1 = validateBetAmount(amtParts.getOrNull(0))
                    if (!v1.isValid) return v1
                    val revStr = amtParts.getOrNull(1)?.trim().orEmpty()
                    if (revStr != "0") {
                        val v2 = validateBetAmount(revStr)
                        if (!v2.isValid) return v2
                    }
                    continue
                }
            }
            if (token.contains("=")) {
                val parts = token.split("=")
                if (parts.size == 2) {
                    val v = validateBetAmount(parts[1])
                    if (!v.isValid) return v
                    continue
                }
            }
            if (token.contains("r", ignoreCase = true)) {
                val rIndex = token.indexOfFirst { it.equals('r', ignoreCase = true) }
                if (rIndex > 0) {
                    val amtPart = token.substring(rIndex + 1)
                    val v = validateBetAmount(amtPart)
                    if (!v.isValid) return v
                    continue
                }
            }
            if (mode == 2) {
                val low = token.lowercase()
                val m1 = Regex("^(\\d)([abtw])(.+)$").find(low)
                if (m1 != null) {
                    val v = validateBetAmount(m1.groupValues[3])
                    if (!v.isValid) return v
                    continue
                }
                val mk = Regex("^(\\d{2,9})k(.+)$").find(low)
                if (mk != null) {
                    val v = validateBetAmount(mk.groupValues[2])
                    if (!v.isValid) return v
                    continue
                }
                val m2 = Regex("^(?:\\d)?([pnsx])(.+)$").find(low)
                if (m2 != null) {
                    val v = validateBetAmount(m2.groupValues[2])
                    if (!v.isValid) return v
                    continue
                }
            }
        }
        return BetAmountValidationResult.Valid
    }

    /**
     * Standalone parser for 2D expressions supporting R split amount syntax using '/':
     * Case 1: Standard single amount (e.g. "58=3000") -> 58 = 3000
     * Case 2: Split amount with slash (e.g. "58=3000/2000") -> 58 = 3000, 85 = 2000
     * Case 3: Twin numbers (e.g. "55=3000/2000") -> 55 = 5000 (combines amounts)
     */
    fun parseBetInput(input: String): List<BetItem> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()

        val normalized = trimmed.replace(Regex("\\s*([=/])\\s*"), "$1")

        // Case 2 & 3: Split amount with slash ^(\d{2})=(\d+)/(\d+)$
        val splitAmountRegex = Regex("""^(\d{2})=(\d+)/(\d+)$""")
        val splitMatch = splitAmountRegex.matchEntire(normalized)
        if (splitMatch != null) {
            val baseNumber = splitMatch.groupValues[1]
            val directAmount = splitMatch.groupValues[2].toLongOrNull() ?: 0L
            val reverseAmount = splitMatch.groupValues[3].toLongOrNull() ?: 0L
            val reverseNumber = reverse2D(baseNumber)

            val entries = mutableListOf<BetItem>()
            if (baseNumber == reverseNumber) {
                // Case 3: Twin numbers: combine direct and reverse amounts
                entries.add(BetItem(baseNumber, directAmount + reverseAmount))
            } else {
                // Case 2: Direct Entry
                entries.add(BetItem(baseNumber, directAmount))
                // Reverse Entry (R)
                if (reverseAmount > 0L) {
                    entries.add(BetItem(reverseNumber, reverseAmount))
                }
            }
            return entries
        }

        // Case 1: Standard single amount ^(\d{2})=(\d+)$
        val singleAmountRegex = Regex("""^(\d{2})=(\d+)$""")
        val singleMatch = singleAmountRegex.matchEntire(normalized)
        if (singleMatch != null) {
            val baseNumber = singleMatch.groupValues[1]
            val amount = singleMatch.groupValues[2].toLongOrNull() ?: 0L
            return listOf(BetItem(baseNumber, amount))
        }

        return emptyList()
    }
}

fun parseBetInput(input: String): List<BetItem> = PatternAlgorithms.parseBetInput(input)
