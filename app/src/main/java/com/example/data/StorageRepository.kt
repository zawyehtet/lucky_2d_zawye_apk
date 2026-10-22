package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class StorageRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lucky2d_storage", Context.MODE_PRIVATE)
    val database: AppDatabase = AppDatabase.getInstance(context)
    val voucherDao: VoucherDao = database.voucherDao()
    val winningNumberDao: WinningNumberDao = database.winningNumberDao()
    val settlementDao: SettlementDao = database.settlementDao()
    val sessionDao: SessionDao = database.sessionDao()

    companion object {
        private const val KEY_AGENTS = "lucky2d_agents"
        private const val KEY_SESSIONS = "lucky2d_sessions"
        private const val KEY_SESSIONS_MIGRATED_V1 = "lucky2d_sessions_migrated_v1"
        private const val KEY_SETTINGS_MIGRATED_V1 = "lucky2d_settings_migrated_v1"
        private const val KEY_VOUCHERS = "lucky2d_vouchers"
        private const val KEY_LAYOFFS = "lucky2d_layoffs"
        private const val KEY_BLOCKED = "lucky2d_blocked"
        private const val KEY_WINS = "lucky2d_wins"
        private const val KEY_SETTLED = "lucky2d_settled"
        private const val KEY_SETTINGS = "lucky2d_settings"

        private val DEFAULT_ROUND_NAMES_12AM = setOf("12.AM", "12AM")
        private val DEFAULT_ROUND_NAMES_4PM = setOf("4.PM", "4PM")
    }

    private fun isUntouchedDefaultSessions(list: List<Session>): Boolean {
        if (list.size != 2) return false
        val first = list[0]
        val second = list[1]
        return first.name in DEFAULT_ROUND_NAMES_12AM &&
            first.rate == 15.0 &&
            first.mult == 80.0 &&
            second.name in DEFAULT_ROUND_NAMES_4PM &&
            second.rate == 15.0 &&
            second.mult == 80.0
    }

    // Sessions (Rounds) - starts empty on fresh install; migrates untouched pre-seeded defaults once
    fun loadSessions(): List<Session> {
        return try {
            val migrated = prefs.getBoolean(KEY_SESSIONS_MIGRATED_V1, false)
            val jsonStr = prefs.getString(KEY_SESSIONS, null)

            if (jsonStr.isNullOrEmpty()) {
                if (!migrated) {
                    prefs.edit().putBoolean(KEY_SESSIONS_MIGRATED_V1, true).apply()
                    sessionDao.clearAllSessionsSync()
                    emptyList()
                } else {
                    val roomSessions = sessionDao.getAllSessionsSync().map { it.toDomainSession() }
                    roomSessions
                }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Session>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val savedId = obj.optLong("id", 0L)
                    list.add(
                        if (savedId > 0L) {
                            Session(
                                name = obj.getString("name"),
                                rate = obj.optDouble("rate", 0.0),
                                mult = obj.optDouble("mult", 0.0),
                                id = savedId
                            )
                        } else {
                            Session(
                                name = obj.getString("name"),
                                rate = obj.optDouble("rate", 0.0),
                                mult = obj.optDouble("mult", 0.0)
                            )
                        }
                    )
                }

                if (!migrated) {
                    prefs.edit().putBoolean(KEY_SESSIONS_MIGRATED_V1, true).apply()
                    if (isUntouchedDefaultSessions(list)) {
                        saveSessions(emptyList())
                        return emptyList()
                    }
                }
                sessionDao.clearAllSessionsSync()
                sessionDao.insertAllSessionsSync(list.map { SessionEntity.fromDomainSession(it) })
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading sessions", e)
            emptyList()
        }
    }

    fun saveSessions(sessions: List<Session>) {
        try {
            val arr = JSONArray()
            sessions.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("rate", s.rate)
                    put("mult", s.mult)
                }
                arr.put(obj)
            }
            prefs.edit()
                .putString(KEY_SESSIONS, arr.toString())
                .putBoolean(KEY_SESSIONS_MIGRATED_V1, true)
                .apply()
            sessionDao.clearAllSessionsSync()
            sessionDao.insertAllSessionsSync(sessions.map { SessionEntity.fromDomainSession(it) })
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving sessions", e)
        }
    }

    fun updateSessionById(sessionId: Long, updatedSession: Session): List<Session>? {
        val current = loadSessions()
        if (current.none { it.id == sessionId }) return null
        val updated = current.map { s ->
            if (s.id == sessionId) updatedSession.copy(id = sessionId) else s
        }
        saveSessions(updated)
        return updated
    }

    fun deleteSessionById(sessionId: Long): List<Session>? {
        val current = loadSessions()
        if (current.none { it.id == sessionId }) return null
        val updated = current.filter { it.id != sessionId }
        saveSessions(updated)
        return updated
    }

    // Agents
    fun loadAgents(): List<Agent> {
        return try {
            val jsonStr = prefs.getString(KEY_AGENTS, null)
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Agent>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Agent(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            rate = obj.optDouble("rate", 0.0),
                            mult = obj.optDouble("mult", 0.0)
                        )
                    )
                }
                val isOldDefaults = list.size == 3 &&
                    list.getOrNull(0)?.name == "ကိုအောင်" &&
                    list.getOrNull(1)?.name == "မသီတာ" &&
                    list.getOrNull(2)?.name == "ကိုဇော်"
                if (isOldDefaults) {
                    saveAgents(emptyList())
                    emptyList()
                } else {
                    list
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading agents", e)
            emptyList()
        }
    }

    fun saveAgents(agents: List<Agent>) {
        try {
            val arr = JSONArray()
            agents.forEach { a ->
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("rate", a.rate)
                    put("mult", a.mult)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_AGENTS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving agents", e)
        }
    }

    // Vouchers
    fun loadVouchers(): List<Voucher> {
        return try {
            val jsonStr = prefs.getString(KEY_VOUCHERS, null)
            if (jsonStr.isNullOrEmpty()) {
                voucherDao.getAllVouchersSync().map { it.toDomainVoucher() }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Voucher>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemsArr = obj.getJSONArray("items")
                    val items = mutableListOf<BetItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        val savedId = itemObj.optLong("id", 0L)
                        val amt = itemObj.optLong("a", 0L)
                        if (validateBetAmount(amt).isValid) {
                            items.add(
                                if (savedId > 0L) {
                                    BetItem(
                                        n = itemObj.getString("n"),
                                        a = amt,
                                        id = savedId
                                    )
                                } else {
                                    BetItem(
                                        n = itemObj.getString("n"),
                                        a = amt
                                    )
                                }
                            )
                        }
                    }
                    list.add(
                        Voucher(
                            id = obj.getLong("id"),
                            no = obj.getInt("no"),
                            agent = obj.getString("agent"),
                            agentId = obj.optInt("agentId", 0),
                            date = obj.getString("date"),
                            session = obj.getString("session"),
                            time = obj.getString("time"),
                            items = items
                        )
                    )
                }
                voucherDao.clearAllVouchersSync()
                voucherDao.insertAllVouchersSync(list.map { VoucherEntity.fromDomainVoucher(it) })
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading vouchers", e)
            emptyList()
        }
    }

    fun saveVouchers(vouchers: List<Voucher>) {
        try {
            val arr = JSONArray()
            vouchers.forEach { v ->
                val obj = JSONObject().apply {
                    put("id", v.id)
                    put("no", v.no)
                    put("agent", v.agent)
                    put("agentId", v.agentId)
                    put("date", v.date)
                    put("session", v.session)
                    put("time", v.time)
                    val itemsArr = JSONArray()
                    v.items.forEach { itm ->
                        val itemObj = JSONObject().apply {
                            put("id", itm.id)
                            put("n", itm.n)
                            put("a", itm.a)
                        }
                        itemsArr.put(itemObj)
                    }
                    put("items", itemsArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_VOUCHERS, arr.toString()).apply()
            voucherDao.clearAllVouchersSync()
            voucherDao.insertAllVouchersSync(vouchers.map { VoucherEntity.fromDomainVoucher(it) })
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving vouchers", e)
        }
    }

    /**
     * Deletes the entire Voucher matching [voucherId] along with all of its associated BetItems (CASCADE).
     */
    fun deleteVoucherById(voucherId: Long): List<Voucher>? {
        val current = loadVouchers()
        if (current.none { it.id == voucherId }) return null
        val updated = current.filter { it.id != voucherId }
        voucherDao.deleteVoucherByIdSync(voucherId)
        saveVouchers(updated)
        return updated
    }

    /**
     * Deletes ONLY the single number entry matching [numberId] (equivalent to DELETE ... WHERE id = :numberId).
     * Never deletes the parent Voucher object even when its last number is removed.
     */
    fun deleteNumberById(numberId: Long): List<Voucher>? {
        val current = loadVouchers()
        var found = false
        val updated = current.map { v ->
            if (v.items.any { it.id == numberId }) {
                found = true
                v.copy(items = v.items.filter { it.id != numberId })
            } else {
                v
            }
        }
        if (!found) return null
        saveVouchers(updated)
        return updated
    }

    /**
     * Updates ONLY the single number entry matching [numberId], preserving its unique id and parent Voucher.
     */
    fun updateNumberById(numberId: Long, newNumber: String, newAmount: Long): List<Voucher>? {
        if (!validateBetAmount(newAmount).isValid) return null
        val current = loadVouchers()
        var found = false
        val updated = current.map { v ->
            if (v.items.any { it.id == numberId }) {
                found = true
                v.copy(
                    items = v.items.map { item ->
                        if (item.id == numberId) item.copy(n = newNumber, a = newAmount) else item
                    }
                )
            } else {
                v
            }
        }
        if (!found) return null
        saveVouchers(updated)
        return updated
    }

    /**
     * Appends a single number entry to an existing Voucher (including an empty voucher).
     */
    fun addNumberToVoucher(voucherId: Long, item: BetItem): List<Voucher>? {
        if (!validateBetAmount(item.a).isValid) return null
        val current = loadVouchers()
        var found = false
        val updated = current.map { v ->
            if (v.id == voucherId) {
                found = true
                v.copy(items = v.items + item)
            } else {
                v
            }
        }
        if (!found) return null
        saveVouchers(updated)
        return updated
    }

    // Layoff Records
    fun loadLayoffs(): List<LayoffRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_LAYOFFS, null)
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<LayoffRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemsArr = obj.getJSONArray("items")
                    val items = mutableListOf<LayoffItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        items.add(
                            LayoffItem(
                                n = itemObj.getString("n"),
                                a = itemObj.getLong("a")
                            )
                        )
                    }
                    list.add(
                        LayoffRecord(
                            id = obj.getLong("id"),
                            date = obj.getString("date"),
                            session = obj.getString("session"),
                            time = obj.getString("time"),
                            limit = obj.getLong("limit"),
                            items = items,
                            total = obj.getLong("total")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading layoffs", e)
            emptyList()
        }
    }

    fun saveLayoffs(layoffs: List<LayoffRecord>) {
        try {
            val arr = JSONArray()
            layoffs.forEach { l ->
                val obj = JSONObject().apply {
                    put("id", l.id)
                    put("date", l.date)
                    put("session", l.session)
                    put("time", l.time)
                    put("limit", l.limit)
                    put("total", l.total)
                    val itemsArr = JSONArray()
                    l.items.forEach { itm ->
                        val itemObj = JSONObject().apply {
                            put("n", itm.n)
                            put("a", itm.a)
                        }
                        itemsArr.put(itemObj)
                    }
                    put("items", itemsArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_LAYOFFS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving layoffs", e)
        }
    }

    // Blocked numbers
    fun loadBlocked(): Set<String> {
        return try {
            val jsonStr = prefs.getString(KEY_BLOCKED, null)
            if (jsonStr.isNullOrEmpty()) {
                emptySet()
            } else {
                val arr = JSONArray(jsonStr)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                set
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading blocked numbers", e)
            emptySet()
        }
    }

    fun saveBlocked(blocked: Set<String>) {
        try {
            val arr = JSONArray()
            blocked.forEach { arr.put(it) }
            prefs.edit().putString(KEY_BLOCKED, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving blocked numbers", e)
        }
    }

    // Wins (winning numbers)
    fun loadWins(): List<WinRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_WINS, null)
            if (jsonStr.isNullOrEmpty()) {
                winningNumberDao.getAllWinningNumbersSync().map { it.toDomainWinRecord() }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<WinRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        WinRecord(
                            date = obj.getString("date"),
                            session = obj.getString("session"),
                            n = obj.getString("n")
                        )
                    )
                }
                winningNumberDao.clearAllWinningNumbersSync()
                winningNumberDao.insertAllWinningNumbersSync(
                    list.map { WinningNumberEntity.fromDomainWinRecord(it) }
                )
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading wins", e)
            emptyList()
        }
    }

    fun saveWins(wins: List<WinRecord>) {
        try {
            val arr = JSONArray()
            wins.forEach { w ->
                val obj = JSONObject().apply {
                    put("date", w.date)
                    put("session", w.session)
                    put("n", w.n)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_WINS, arr.toString()).apply()
            winningNumberDao.clearAllWinningNumbersSync()
            winningNumberDao.insertAllWinningNumbersSync(
                wins.map { WinningNumberEntity.fromDomainWinRecord(it) }
            )
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving wins", e)
        }
    }

    // Settled Records
    fun loadSettled(): List<SettledRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_SETTLED, null)
            if (jsonStr.isNullOrEmpty()) {
                settlementDao.getAllSettlementsSync().map { it.toDomainSettledRecord() }
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<SettledRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SettledRecord(
                            id = obj.getLong("id"),
                            date = obj.getString("date"),
                            session = obj.getString("session"),
                            winningNumber = obj.getString("winningNumber"),
                            totalRetained = obj.getLong("totalRetained"),
                            commission = obj.getLong("commission"),
                            retainedNet = obj.getLong("retainedNet"),
                            payout = obj.getLong("payout"),
                            profitLoss = obj.getLong("profitLoss"),
                            timestamp = obj.getString("timestamp")
                        )
                    )
                }
                settlementDao.clearAllSettlementsSync()
                settlementDao.insertAllSettlementsSync(
                    list.map { SettlementEntity.fromDomainSettledRecord(it) }
                )
                list
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading settled records", e)
            emptyList()
        }
    }

    fun saveSettled(records: List<SettledRecord>) {
        try {
            val arr = JSONArray()
            records.forEach { r ->
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("session", r.session)
                    put("winningNumber", r.winningNumber)
                    put("totalRetained", r.totalRetained)
                    put("commission", r.commission)
                    put("retainedNet", r.retainedNet)
                    put("payout", r.payout)
                    put("profitLoss", r.profitLoss)
                    put("timestamp", r.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_SETTLED, arr.toString()).apply()
            settlementDao.clearAllSettlementsSync()
            settlementDao.insertAllSettlementsSync(
                records.map { SettlementEntity.fromDomainSettledRecord(it) }
            )
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving settled records", e)
        }
    }

    /**
     * Deletes ONLY the single daily settled record matching [recordId] (DELETE ... WHERE id = :recordId).
     * Never clears the whole table.
     */
    fun deleteSettledRecordById(recordId: Long): List<SettledRecord>? {
        val current = loadSettled()
        if (current.none { it.id == recordId }) return null
        val updated = current.filter { it.id != recordId }
        settlementDao.deleteSettlementByIdSync(recordId)
        saveSettled(updated)
        return updated
    }

    fun deleteById(recordId: Long): List<SettledRecord>? = deleteSettledRecordById(recordId)

    /**
     * Updates ONLY the single daily settled record matching [recordId], keeping the exact same id
     * and leaving all other records unchanged.
     */
    fun updateSettledRecordById(recordId: Long, updatedRecord: SettledRecord): List<SettledRecord>? {
        val current = loadSettled()
        if (current.none { it.id == recordId }) return null
        val updated = current.map { r ->
            if (r.id == recordId) updatedRecord.copy(id = recordId) else r
        }
        saveSettled(updated)
        return updated
    }

    fun updateById(recordId: Long, updatedRecord: SettledRecord): List<SettledRecord>? =
        updateSettledRecordById(recordId, updatedRecord)

    // Composite (date, roundNumber) reactive and synchronous Room DAO queries
    fun getVouchersByDateAndRound(date: String, round: String): kotlinx.coroutines.flow.Flow<List<VoucherEntity>> =
        voucherDao.getVouchersByDateAndRound(date, round)

    fun getRoundsByDate(date: String): kotlinx.coroutines.flow.Flow<List<String>> =
        voucherDao.getRoundsByDate(date)

    fun getWinningNumber(date: String, round: String): kotlinx.coroutines.flow.Flow<WinningNumberEntity?> =
        winningNumberDao.getWinningNumber(date, round)

    fun getSettlement(date: String, round: String): kotlinx.coroutines.flow.Flow<SettlementEntity?> =
        settlementDao.getSettlement(date, round)

    fun getDistinctHistoricalRoundsByDateSync(date: String): List<String> {
        return try {
            val voucherRounds = voucherDao.getRoundsByDateSync(date)
            val winRounds = winningNumberDao.getRoundsByDateSync(date)
            val settlementRounds = settlementDao.getRoundsByDateSync(date)
            val layoffRounds = loadLayoffs().filter { it.date == date }.map { it.session }
            (voucherRounds + winRounds + settlementRounds + layoffRounds)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading historical rounds for date $date", e)
            emptyList()
        }
    }

    // App Settings
    fun loadSettings(): AppSettings {
        return try {
            val migrated = prefs.getBoolean(KEY_SETTINGS_MIGRATED_V1, false)
            val jsonStr = prefs.getString(KEY_SETTINGS, null)
            if (jsonStr.isNullOrEmpty()) {
                val defaults = AppSettings()
                prefs.edit().putBoolean(KEY_SETTINGS_MIGRATED_V1, true).apply()
                saveSettings(defaults)
                defaults
            } else {
                val obj = JSONObject(jsonStr)
                val password = obj.optString("password", "1234")
                val brakeLimit = obj.optLong("brakeLimit", 0L)
                val soundEnabled = obj.optBoolean("soundEnabled", true)
                val loaded = AppSettings(
                    password = password,
                    brakeLimit = brakeLimit,
                    soundEnabled = soundEnabled
                )
                if (!migrated) {
                    prefs.edit().putBoolean(KEY_SETTINGS_MIGRATED_V1, true).apply()
                }
                loaded
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading settings", e)
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        try {
            val obj = JSONObject().apply {
                put("password", settings.password)
                put("brakeLimit", settings.brakeLimit)
                put("soundEnabled", settings.soundEnabled)
            }
            prefs.edit()
                .putString(KEY_SETTINGS, obj.toString())
                .putBoolean(KEY_SETTINGS_MIGRATED_V1, true)
                .apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving settings", e)
        }
    }

    fun loadVouchersForDateAndRound(date: String, round: String): List<Voucher> {
        return try {
            voucherDao.getVouchersByDateAndRoundSync(date, round).map { it.toDomainVoucher() }
        } catch (e: Exception) {
            loadVouchers().filter { it.date == date && it.session == round }
        }
    }

    fun loadWinningNumberForDateAndRound(date: String, round: String): WinRecord? {
        return try {
            winningNumberDao.getWinningNumberSync(date, round)?.toDomainWinRecord()
        } catch (e: Exception) {
            loadWins().firstOrNull { it.date == date && it.session == round }
        }
    }

    fun loadSettlementForDateAndRound(date: String, round: String): SettledRecord? {
        return try {
            settlementDao.getSettlementSync(date, round)?.toDomainSettledRecord()
        } catch (e: Exception) {
            loadSettled().firstOrNull { it.date == date && it.session == round }
        }
    }

    fun resetAllData() {
        try {
            prefs.edit()
                .clear()
                .putBoolean(KEY_SESSIONS_MIGRATED_V1, true)
                .putBoolean(KEY_SETTINGS_MIGRATED_V1, true)
                .apply()
            voucherDao.clearAllVouchersSync()
            winningNumberDao.clearAllWinningNumbersSync()
            settlementDao.clearAllSettlementsSync()
            sessionDao.clearAllSessionsSync()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error resetting data", e)
        }
    }
}
