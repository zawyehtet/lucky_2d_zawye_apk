package com.example.logic

import com.example.data.BetItem

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

    // Quick Entry Mode 1:
    // Format: "01.78=500" or "01.78R500" or "01.78=500/100"
    // Separators: space, comma, newline, pipe, semicolon
    fun parseQuickEntryMode1(
        text: String,
        blocked: Set<String>,
        maxBet: Long
    ): ParseResult {
        val items = mutableListOf<BetItem>()
        var skipped = 0
        val blockedList = mutableListOf<String>()

        // Split text by whitespace, newlines, pipes, semicolons, or commas separating distinct entries (preserving invalid comma amounts like 78=500,00 so they fail validation)
        val tokens = text.split(Regex("[\\s;|\n\r]+|,(?=\\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])")).filter { it.isNotBlank() }

        for (token in tokens) {
            try {
                // Check format 1: numbers=amount/revAmount (e.g. 01.78=500/100 or 56=2000/1000)
                if (token.contains("=") && token.contains("/")) {
                    val eqParts = token.split("=")
                    if (eqParts.size == 2) {
                        val numPart = eqParts[0]
                        val amtParts = eqParts[1].split("/")
                        val amtDirect = amtParts.getOrNull(0)?.toLongOrNull() ?: 0L
                        val amtReverse = amtParts.getOrNull(1)?.toLongOrNull() ?: 0L
                        val numbers = numPart.split(".").filter { it.length == 2 && it.all { c -> c.isDigit() } }
                        if (numbers.isEmpty() || amtDirect <= 0 || amtDirect > maxBet || amtReverse < 0 || amtReverse > maxBet) {
                            skipped++
                        } else {
                            for (n in numbers) {
                                if (blocked.contains(n)) {
                                    blockedList.add(n)
                                } else {
                                    items.add(BetItem(n, amtDirect))
                                }
                                val rev = reverse2D(n)
                                if (amtReverse > 0) {
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

                // Check format 2: numbersRamount (e.g. 01.78R500, 89r350)
                if (token.contains("r", ignoreCase = true)) {
                    val rIndex = token.indexOfFirst { it.equals('r', ignoreCase = true) }
                    val numPart = token.substring(0, rIndex)
                    val amtPart = token.substring(rIndex + 1)
                    val amount = amtPart.toLongOrNull() ?: 0L
                    val numbers = numPart.split(".").filter { it.length == 2 && it.all { c -> c.isDigit() } }
                    if (numbers.isEmpty() || amount <= 0 || amount > maxBet) {
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

                // Check format 3: numbers=amount (e.g. 01.78=500, 79=500)
                if (token.contains("=")) {
                    val parts = token.split("=")
                    if (parts.size == 2) {
                        val numPart = parts[0]
                        val amount = parts[1].toLongOrNull() ?: 0L
                        val numbers = numPart.split(".").filter { it.length == 2 && it.all { c -> c.isDigit() } }
                        if (numbers.isEmpty() || amount <= 0 || amount > maxBet) {
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

    // Quick Entry Mode 2:
    // Formats:
    // 5a500, 6b600, 1t500, 1w500, p3500, n7500, s500, x200, 56=2000/1000, 89r350, 79=500
    // (a=အပါ, b=ဘရိတ်, t=ထိပ်, w=နောက်, p=ပါဝါ, n=နက္ခတ်, s=အပူး, x=ညီကို)
    fun parseQuickEntryMode2(
        text: String,
        blocked: Set<String>,
        maxBet: Long
    ): ParseResult {
        val items = mutableListOf<BetItem>()
        var skipped = 0
        val blockedList = mutableListOf<String>()

        val tokens = text.split(Regex("[\\s;|\n\r]+|,(?=\\s|$|[0-9]{1,2}[.=rR]|[0-9][abtwABTW]|[pnsxPNSX][0-9])")).filter { it.isNotBlank() }

        for (rawToken in tokens) {
            val token = rawToken.lowercase().trim()
            try {
                // If contains '=' or 'r' with 2-digit number, delegate to mode 1 parser logic
                if (token.contains("=") || (token.contains("r") && token.first().isDigit() && token.indexOf("r") == 2)) {
                    val res = parseQuickEntryMode1(token, blocked, maxBet)
                    items.addAll(res.items)
                    skipped += res.skippedCount
                    blockedList.addAll(res.blockedSkipped)
                    continue
                }

                // Digit + letter + amount: a, b, t, w
                val matchDigitLetterAmt = Regex("^(\\d)([abtw])(\\d+)$").find(token)
                if (matchDigitLetterAmt != null) {
                    val digitChar = matchDigitLetterAmt.groupValues[1][0]
                    val letter = matchDigitLetterAmt.groupValues[2]
                    val amount = matchDigitLetterAmt.groupValues[3].toLongOrNull() ?: 0L

                    if (amount <= 0 || amount > maxBet) {
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

                // Letter + amount: p, n, s, x
                val matchLetterAmt = Regex("^([pnsx])(\\d+)$").find(token)
                if (matchLetterAmt != null) {
                    val letter = matchLetterAmt.groupValues[1]
                    val amount = matchLetterAmt.groupValues[2].toLongOrNull() ?: 0L

                    if (amount <= 0 || amount > maxBet) {
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

                // Fallback direct 2D e.g. "45=500" or "45 500"
                skipped++
            } catch (e: Exception) {
                skipped++
            }
        }

        return ParseResult(items, skipped, blockedList)
    }
}
