package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class StorageRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lucky2d_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AGENTS = "lucky2d_agents"
        private const val KEY_SESSIONS = "lucky2d_sessions"
        private const val KEY_VOUCHERS = "lucky2d_vouchers"
        private const val KEY_LAYOFFS = "lucky2d_layoffs"
        private const val KEY_BLOCKED = "lucky2d_blocked"
        private const val KEY_WINS = "lucky2d_wins"
        private const val KEY_SETTLED = "lucky2d_settled"
        private const val KEY_SETTINGS = "lucky2d_settings"
    }

    // Default Sessions
    fun loadSessions(): List<Session> {
        return try {
            val jsonStr = prefs.getString(KEY_SESSIONS, null)
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Session>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Session(
                            name = obj.getString("name"),
                            rate = obj.optDouble("rate", 15.0),
                            mult = obj.optDouble("mult", 80.0)
                        )
                    )
                }
                // If it only contains the old default 12.AM and 4.PM, clear them
                val isOldDefaults = list.size == 2 &&
                    list.getOrNull(0)?.name == "12.AM" &&
                    list.getOrNull(1)?.name == "4.PM"
                if (isOldDefaults) {
                    saveSessions(emptyList())
                    emptyList()
                } else {
                    list
                }
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
                    put("name", s.name)
                    put("rate", s.rate)
                    put("mult", s.mult)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving sessions", e)
        }
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
                            rate = obj.optDouble("rate", 15.0),
                            mult = obj.optDouble("mult", 80.0)
                        )
                    )
                }
                // If it only contains the old initial default sample agents, clear them
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
                emptyList()
            } else {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<Voucher>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemsArr = obj.getJSONArray("items")
                    val items = mutableListOf<BetItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        items.add(
                            BetItem(
                                n = itemObj.getString("n"),
                                a = itemObj.getLong("a")
                            )
                        )
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
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving vouchers", e)
        }
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
                emptyList()
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
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving wins", e)
        }
    }

    // Settled Records
    fun loadSettled(): List<SettledRecord> {
        return try {
            val jsonStr = prefs.getString(KEY_SETTLED, null)
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
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
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving settled records", e)
        }
    }

    // App Settings
    fun loadSettings(): AppSettings {
        return try {
            val jsonStr = prefs.getString(KEY_SETTINGS, null)
            if (jsonStr.isNullOrEmpty()) {
                val defaults = AppSettings()
                saveSettings(defaults)
                defaults
            } else {
                val obj = JSONObject(jsonStr)
                AppSettings(
                    maxBet = obj.optLong("maxBet", 9999999L),
                    password = obj.optString("password", "1234"),
                    brakeLimit = obj.optLong("brakeLimit", 50000L)
                )
            }
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error loading settings", e)
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        try {
            val obj = JSONObject().apply {
                put("maxBet", settings.maxBet)
                put("password", settings.password)
                put("brakeLimit", settings.brakeLimit)
            }
            prefs.edit().putString(KEY_SETTINGS, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error saving settings", e)
        }
    }

    fun resetAllData() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error resetting data", e)
        }
    }
}
