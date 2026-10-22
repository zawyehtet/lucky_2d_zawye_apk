package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.example.data.Agent

@Entity(
    tableName = "agents",
    indices = [Index(value = ["date", "roundNumber", "agentId"])],
    primaryKeys = ["date", "roundNumber", "agentId"]
)
data class AgentEntity(
    val date: String,
    val roundNumber: String,
    val agentId: Int,
    val name: String,
    val commissionRate: Double,
    val multiplier: Double
) {
    fun toDomainAgent(): Agent = Agent(
        id = agentId,
        name = name,
        rate = commissionRate,
        mult = multiplier,
        date = date,
        roundNumber = roundNumber
    )

    companion object {
        fun fromDomainAgent(
            agent: Agent,
            date: String = agent.date,
            round: String = agent.roundNumber
        ): AgentEntity = AgentEntity(
            date = date,
            roundNumber = round,
            agentId = agent.id,
            name = agent.name,
            commissionRate = agent.rate,
            multiplier = agent.mult
        )
    }
}
