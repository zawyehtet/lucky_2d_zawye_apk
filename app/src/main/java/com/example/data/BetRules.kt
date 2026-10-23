package com.example.data

const val MAX_BET_AMOUNT = 999_999_999L

enum class BetAmountValidationResult {
    Valid,
    Empty,
    NonNumeric,
    ZeroOrNegative,
    TooLarge;

    val isValid: Boolean
        get() = this == Valid
}

fun validateBetAmount(rawInput: String?): BetAmountValidationResult {
    val trimmed = rawInput?.trim().orEmpty()
    if (trimmed.isEmpty()) return BetAmountValidationResult.Empty

    // Support split amount syntax: [DirectAmount]/[ReverseAmount] (e.g. "3000/2000")
    if (trimmed.contains("/")) {
        val parts = trimmed.split("/")
        if (parts.size != 2) return BetAmountValidationResult.NonNumeric
        val part1 = parts[0].trim()
        val part2 = parts[1].trim()
        if (part1.isEmpty() || part2.isEmpty()) return BetAmountValidationResult.Empty
        val res1 = validateBetAmount(part1)
        if (!res1.isValid) return res1
        val res2 = validateBetAmount(part2)
        if (!res2.isValid) return res2
        return BetAmountValidationResult.Valid
    }

    val isNegative = trimmed.startsWith("-")
    val digitsPart = if (isNegative) trimmed.substring(1) else trimmed
    if (digitsPart.isEmpty() || !digitsPart.all { it.isDigit() }) {
        return BetAmountValidationResult.NonNumeric
    }
    if (isNegative) {
        return BetAmountValidationResult.ZeroOrNegative
    }
    val parsed = trimmed.toLongOrNull() ?: return BetAmountValidationResult.TooLarge
    return validateBetAmount(parsed)
}

fun validateBetAmount(amount: Long): BetAmountValidationResult {
    if (amount <= 0L) return BetAmountValidationResult.ZeroOrNegative
    if (amount > MAX_BET_AMOUNT) return BetAmountValidationResult.TooLarge
    return BetAmountValidationResult.Valid
}
