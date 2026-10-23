package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DailySettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySettlementDao {

    @Query("SELECT * FROM daily_settlements WHERE date = :date LIMIT 1")
    fun getDailySettlementByDate(date: String): Flow<DailySettlementEntity?>

    @Query("SELECT * FROM daily_settlements WHERE date = :date LIMIT 1")
    fun getDailySettlementByDateSync(date: String): DailySettlementEntity?

    @Query("SELECT * FROM daily_settlements ORDER BY date DESC")
    fun getAllDailySettlements(): Flow<List<DailySettlementEntity>>

    @Query("SELECT * FROM daily_settlements ORDER BY date DESC")
    fun getAllDailySettlementsSync(): List<DailySettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySettlement(settlement: DailySettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateDailySettlementSync(settlement: DailySettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllDailySettlementsSync(settlements: List<DailySettlementEntity>)

    @Query("DELETE FROM daily_settlements WHERE date = :date")
    suspend fun deleteDailySettlementByDate(date: String)

    @Query("DELETE FROM daily_settlements WHERE date = :date")
    fun deleteDailySettlementByDateSync(date: String)

    @Query("DELETE FROM daily_settlements")
    fun clearAllDailySettlementsSync()
}
