package com.example.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "daily_settlements",
    primaryKeys = ["date"]
)
data class DailySettlementEntity(
    val date: String,
    val totalSales: Long,
    val totalCommission: Long,
    val netSales: Long,
    val winningPayout: Long,
    val profitOrLoss: Long,
    val roundsSummary: String = "",
    val winningNumbersSummary: String = "",
    val roundCount: Int = 0,
    val timestamp: String = ""
)
