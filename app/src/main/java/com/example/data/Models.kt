package com.example.data

data class Agent(
    val id: Int,
    val name: String,
    val rate: Double = 15.0,  // Commission % (e.g. 15%)
    val mult: Double = 80.0   // Multiplier (e.g. 80x)
)

data class Session(
    val name: String,
    val rate: Double = 15.0,
    val mult: Double = 80.0
)

data class BetItem(
    val n: String,
    val a: Long
)

data class Voucher(
    val id: Long,
    val no: Int,
    val agent: String,
    val agentId: Int = 0,
    val date: String,
    val session: String,
    val time: String,
    val items: List<BetItem>
) {
    val totalAmount: Long
        get() = items.sumOf { it.a }
}

data class LayoffItem(
    val n: String,
    val a: Long
)

data class LayoffRecord(
    val id: Long,
    val date: String,
    val session: String,
    val time: String,
    val limit: Long,
    val items: List<LayoffItem>,
    val total: Long
)

data class WinRecord(
    val date: String,
    val session: String,
    val n: String
)

data class SettledRecord(
    val id: Long,
    val date: String,
    val session: String,
    val winningNumber: String,
    val totalRetained: Long,
    val commission: Long,
    val retainedNet: Long,
    val payout: Long,
    val profitLoss: Long,
    val timestamp: String
)

data class AppSettings(
    val maxBet: Long = 9999999L,
    val password: String = "1234",
    val brakeLimit: Long = 50000L
)

data class AgentSummaryData(
    val totalBet: Long,
    val commission: Long,
    val netAmount: Long,
    val voucherCount: Int,
    val payout: Long
)

