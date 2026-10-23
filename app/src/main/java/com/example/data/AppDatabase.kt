package com.example.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "vouchers",
    indices = [Index(value = ["date", "roundNumber"])]
)
data class VoucherEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "voucherId")
    val id: Long = 0,
    val agentName: String,
    val date: String,           // Format: "YYYY-MM-DD"
    val roundNumber: String,    // Format: "22.AM", "11", etc.
    val betsJson: String,       // Serialized bet rows
    val totalAmount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val voucherNo: Int = 1,
    val agentId: Int = 0,
    val formattedTime: String = ""
) {
    val voucherId: Long
        get() = id

    fun toDomainVoucher(): Voucher {
        val items = mutableListOf<BetItem>()
        try {
            val arr = JSONArray(betsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val savedId = obj.optLong("id", 0L)
                val amt = obj.optLong("a", 0L)
                val num = obj.optString("n", "")
                if (num.isNotEmpty() && validateBetAmount(amt).isValid) {
                    items.add(
                        if (savedId > 0L) BetItem(n = num, a = amt, id = savedId)
                        else BetItem(n = num, a = amt)
                    )
                }
            }
        } catch (_: Exception) {
        }
        return Voucher(
            id = id,
            no = voucherNo,
            agent = agentName,
            agentId = agentId,
            date = date,
            session = roundNumber,
            time = formattedTime,
            items = items
        )
    }

    companion object {
        fun fromDomainVoucher(v: Voucher): VoucherEntity {
            val itemsArr = JSONArray()
            v.items.forEach { itm ->
                itemsArr.put(
                    JSONObject().apply {
                        put("id", itm.id)
                        put("n", itm.n)
                        put("a", itm.a)
                    }
                )
            }
            return VoucherEntity(
                id = v.id,
                agentName = v.agent,
                date = v.date,
                roundNumber = v.session,
                betsJson = itemsArr.toString(),
                totalAmount = v.totalAmount,
                timestamp = v.id,
                voucherNo = v.no,
                agentId = v.agentId,
                formattedTime = v.time
            )
        }
    }
}

@Entity(
    tableName = "winning_numbers",
    indices = [Index(value = ["date", "roundNumber"], unique = true)]
)
data class WinningNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val roundNumber: String,
    val winningNumber: String   // e.g., "47"
) {
    fun toDomainWinRecord(): WinRecord = WinRecord(
        date = date,
        session = roundNumber,
        n = winningNumber
    )

    companion object {
        fun fromDomainWinRecord(w: WinRecord, id: Long = 0L): WinningNumberEntity = WinningNumberEntity(
            id = id,
            date = w.date,
            roundNumber = w.session,
            winningNumber = w.n
        )
    }
}

@Entity(
    tableName = "settlements",
    indices = [Index(value = ["date", "roundNumber"], unique = true)]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val roundNumber: String,
    val totalSales: Long,
    val totalCommission: Long,
    val netSales: Long,
    val winningPayout: Long,
    val profitOrLoss: Long,
    val winningNumber: String = "",
    val timestamp: String = ""
) {
    fun toDomainSettledRecord(): SettledRecord = SettledRecord(
        id = id,
        date = date,
        session = roundNumber,
        winningNumber = winningNumber,
        totalRetained = totalSales,
        commission = totalCommission,
        retainedNet = netSales,
        payout = winningPayout,
        profitLoss = profitOrLoss,
        timestamp = timestamp
    )

    companion object {
        fun fromDomainSettledRecord(r: SettledRecord): SettlementEntity = SettlementEntity(
            id = r.id,
            date = r.date,
            roundNumber = r.session,
            totalSales = r.totalRetained,
            totalCommission = r.commission,
            netSales = r.retainedNet,
            winningPayout = r.payout,
            profitOrLoss = r.profitLoss,
            winningNumber = r.winningNumber,
            timestamp = r.timestamp
        )
    }
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundName: String,
    val commissionPercent: Double,
    val multiplier: Int
) {
    fun toDomainSession(): Session = Session(
        name = roundName,
        rate = commissionPercent,
        mult = multiplier.toDouble(),
        id = id
    )

    companion object {
        fun fromDomainSession(s: Session): SessionEntity = SessionEntity(
            id = s.id,
            roundName = s.name,
            commissionPercent = s.rate,
            multiplier = s.mult.toInt()
        )
    }
}

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE date = :date AND roundNumber = :round ORDER BY voucherId ASC")
    fun getVouchersByDateAndRound(date: String, round: String): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE date = :date AND roundNumber = :round ORDER BY voucherId ASC")
    fun getVouchersByDateAndRoundSync(date: String, round: String): List<VoucherEntity>

    @Query("SELECT DISTINCT roundNumber FROM vouchers WHERE date = :date")
    fun getRoundsByDate(date: String): Flow<List<String>>

    @Query("SELECT DISTINCT roundNumber FROM vouchers WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("SELECT * FROM vouchers ORDER BY voucherId DESC")
    fun getAllVouchersSync(): List<VoucherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVoucherSync(voucher: VoucherEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllVouchersSync(vouchers: List<VoucherEntity>)

    @Query("DELETE FROM vouchers WHERE voucherId = :id")
    fun deleteVoucherByIdSync(id: Long)

    @Query("DELETE FROM vouchers")
    fun clearAllVouchersSync()
}

@Dao
interface WinningNumberDao {
    @Query("SELECT * FROM winning_numbers WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getWinningNumber(date: String, round: String): Flow<WinningNumberEntity?>

    @Query("SELECT * FROM winning_numbers WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getWinningNumberSync(date: String, round: String): WinningNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWinningNumber(win: WinningNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateWinningNumberSync(win: WinningNumberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllWinningNumbersSync(wins: List<WinningNumberEntity>)

    @Query("SELECT * FROM winning_numbers")
    fun getAllWinningNumbersSync(): List<WinningNumberEntity>

    @Query("SELECT DISTINCT roundNumber FROM winning_numbers WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("DELETE FROM winning_numbers WHERE date = :date AND roundNumber = :round")
    fun deleteWinningNumberSync(date: String, round: String)

    @Query("DELETE FROM winning_numbers")
    fun clearAllWinningNumbersSync()
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getSettlement(date: String, round: String): Flow<SettlementEntity?>

    @Query("SELECT * FROM settlements WHERE date = :date AND roundNumber = :round LIMIT 1")
    fun getSettlementSync(date: String, round: String): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSettlementSync(settlement: SettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSettlementsSync(settlements: List<SettlementEntity>)

    @Query("SELECT * FROM settlements ORDER BY id DESC")
    fun getAllSettlementsSync(): List<SettlementEntity>

    @Query("SELECT DISTINCT roundNumber FROM settlements WHERE date = :date")
    fun getRoundsByDateSync(date: String): List<String>

    @Query("DELETE FROM settlements WHERE id = :id")
    fun deleteSettlementByIdSync(id: Long)

    @Query("DELETE FROM settlements")
    fun clearAllSettlementsSync()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY id ASC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY id ASC")
    fun getAllSessionsSync(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSessionsSync(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions")
    fun clearAllSessionsSync()
}

typealias AgentEntity = com.example.data.local.entity.AgentEntity
typealias AgentDao = com.example.data.local.dao.AgentDao
typealias DailySettlementEntity = com.example.data.local.entity.DailySettlementEntity
typealias DailySettlementDao = com.example.data.local.dao.DailySettlementDao

@Database(
    entities = [
        AgentEntity::class,
        VoucherEntity::class,
        WinningNumberEntity::class,
        SettlementEntity::class,
        DailySettlementEntity::class,
        SessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun voucherDao(): VoucherDao
    abstract fun winningNumberDao(): WinningNumberDao
    abstract fun settlementDao(): SettlementDao
    abstract fun dailySettlementDao(): DailySettlementDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lucky2d_room_database.db"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
