package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AgentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {

    // Fetch agents strictly belonging to the selected date and round
    @Query("SELECT * FROM agents WHERE date = :date AND roundNumber = :round ORDER BY agentId ASC")
    fun getAgentsByDateAndRound(date: String, round: String): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE date = :date AND roundNumber = :round ORDER BY agentId ASC")
    fun getAgentsByDateAndRoundSync(date: String, round: String): List<AgentEntity>

    @Query("SELECT * FROM agents ORDER BY date DESC, roundNumber ASC, agentId ASC")
    fun getAllAgentsSync(): List<AgentEntity>

    // Insert or update an agent scoped to the active session
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAgent(agent: AgentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAgentSync(agent: AgentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllAgentsSync(agents: List<AgentEntity>)

    // CRITICAL BUG FIX: Delete agent ONLY from the specified session
    @Query("DELETE FROM agents WHERE date = :date AND roundNumber = :round AND agentId = :agentId")
    suspend fun deleteAgentBySession(date: String, round: String, agentId: Int)

    @Query("DELETE FROM agents WHERE date = :date AND roundNumber = :round AND agentId = :agentId")
    fun deleteAgentBySessionSync(date: String, round: String, agentId: Int)

    // Clear all agents in a single specific session
    @Query("DELETE FROM agents WHERE date = :date AND roundNumber = :round")
    suspend fun clearAgentsForSession(date: String, round: String)

    @Query("DELETE FROM agents WHERE date = :date AND roundNumber = :round")
    fun clearAgentsForSessionSync(date: String, round: String)

    @Query("SELECT DISTINCT roundNumber FROM agents WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("DELETE FROM agents")
    fun clearAllAgentsSync()
}
