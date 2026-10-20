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
