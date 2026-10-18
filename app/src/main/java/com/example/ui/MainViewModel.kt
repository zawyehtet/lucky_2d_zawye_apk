package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.logic.PatternAlgorithms
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    HOME,
    AGENTS,
    NUMBER_ENTRY,
    VOUCHERS,
    NUMBERS,
    BRAKE,
    SETTLEMENT,
    SETTINGS,
    LAYOFF_HISTORY
}

data class NumberSummary(
    val n: String,
    val totalBet: Long,
    val layoffAmount: Long,
    val netAmount: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StorageRepository(application)

    // Current date and active session
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("yyyy.MM.dd/HH:mm:ss", Locale.getDefault())

    val todayDate: String = dateFormatter.format(Date())

    private val _selectedDate = MutableStateFlow(todayDate)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedSession = MutableStateFlow("12.AM")
    val selectedSession: StateFlow<String> = _selectedSession.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation back stack for sub-screens
    private val screenHistory = mutableListOf(Screen.HOME)

    // Data lists
    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _vouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    private val _layoffs = MutableStateFlow<List<LayoffRecord>>(emptyList())
    val layoffs: StateFlow<List<LayoffRecord>> = _layoffs.asStateFlow()

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    private val _wins = MutableStateFlow<List<WinRecord>>(emptyList())
    val wins: StateFlow<List<WinRecord>> = _wins.asStateFlow()

    private val _settled = MutableStateFlow<List<SettledRecord>>(emptyList())
    val settled: StateFlow<List<SettledRecord>> = _settled.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Number Entry state
    private val _entryAgent = MutableStateFlow<Agent?>(null)
    val entryAgent: StateFlow<Agent?> = _entryAgent.asStateFlow()

    private val _entryItems = MutableStateFlow<List<BetItem>>(emptyList())
    val entryItems: StateFlow<List<BetItem>> = _entryItems.asStateFlow()

    // Brake screen limit state
    private val _currentBrakeInput = MutableStateFlow("50000")
    val currentBrakeInput: StateFlow<String> = _currentBrakeInput.asStateFlow()

    // Toast event message
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        val loadedSessions = repository.loadSessions()
        _sessions.value = loadedSessions
        if (loadedSessions.isNotEmpty() && !loadedSessions.any { it.name == _selectedSession.value }) {
            _selectedSession.value = loadedSessions.first().name
        }

        _agents.value = repository.loadAgents()
        _vouchers.value = repository.loadVouchers()
        _layoffs.value = repository.loadLayoffs()
        _blocked.value = repository.loadBlocked()
        _wins.value = repository.loadWins()
        _settled.value = repository.loadSettled()
        val s = repository.loadSettings()
        _settings.value = s
        _currentBrakeInput.value = s.brakeLimit.toString()
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    fun setSession(session: String) {
        _selectedSession.value = session
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenHistory.add(screen)
            _currentScreen.value = screen
        }
    }

    fun goBack(): Boolean {
        if (screenHistory.size > 1) {
            screenHistory.removeAt(screenHistory.lastIndex)
            _currentScreen.value = screenHistory.last()
            return true
        } else if (_currentScreen.value != Screen.HOME) {
            _currentScreen.value = Screen.HOME
            screenHistory.clear()
            screenHistory.add(Screen.HOME)
            return true
        }
        return false
    }

    // Agent Management
    fun addAgent(name: String, rate: Double, mult: Double) {
        val nextId = (_agents.value.maxOfOrNull { it.id } ?: 0) + 1
        val newAgent = Agent(id = nextId, name = name.trim(), rate = rate, mult = mult)
        val updated = _agents.value + newAgent
        _agents.value = updated
        repository.saveAgents(updated)
        showToast("ကော်မရှင် အသစ် ထည့်သွင်းပြီးပါပြီ")
    }

    fun updateAgent(agent: Agent) {
        val updated = _agents.value.map { if (it.id == agent.id) agent else it }
        _agents.value = updated
        repository.saveAgents(updated)
        showToast("ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteAgent(agentId: Int) {
        val updated = _agents.value.filter { it.id != agentId }
        _agents.value = updated
        repository.saveAgents(updated)
        showToast("ဖျက်ပြီးပါပြီ")
    }

    // Start Number Entry for an Agent
    fun startNumberEntry(agent: Agent) {
        _entryAgent.value = agent
        _entryItems.value = emptyList()
        navigateTo(Screen.NUMBER_ENTRY)
    }

    fun setEntryAgent(agent: Agent) {
        _entryAgent.value = agent
    }

    // Number Entry Draft manipulations
    fun addEntryItem(item: BetItem) {
        if (settings.value.maxBet > 0 && item.a > settings.value.maxBet) {
            showToast("သတ်မှတ်ထားသော အများဆုံးထိုးကြေး (${settings.value.maxBet}) ထက် ကျော်လွန်နေပါသည်")
            return
        }
        if (_blocked.value.contains(item.n)) {
            showToast("${item.n} သည် မရဂဏန်း (ပိတ်ဂဏန်း) ဖြစ်ပါသည်")
            return
        }
        _entryItems.value = _entryItems.value + item
    }

    fun addEntryItems(items: List<BetItem>) {
        val valid = items.filter { item ->
            val blocked = _blocked.value.contains(item.n)
            val overLimit = settings.value.maxBet > 0 && item.a > settings.value.maxBet
            !blocked && !overLimit
        }
        _entryItems.value = _entryItems.value + valid
    }

    fun removeLastEntryItem() {
        if (_entryItems.value.isNotEmpty()) {
            _entryItems.value = _entryItems.value.dropLast(1)
        }
    }

    fun editEntryItem(index: Int, newNumber: String, newAmount: Long) {
        if (index in _entryItems.value.indices) {
            val updated = _entryItems.value.toMutableList()
            updated[index] = BetItem(newNumber, newAmount)
            _entryItems.value = updated
        }
    }

    fun deleteEntryItem(index: Int) {
        if (index in _entryItems.value.indices) {
            val updated = _entryItems.value.toMutableList()
            updated.removeAt(index)
            _entryItems.value = updated
        }
    }

    fun clearEntryItems() {
        _entryItems.value = emptyList()
    }

    // Save Voucher when "ထိုးရန်" switch is turned on
    fun commitCurrentVoucher(): Boolean {
        val currentAgent = _entryAgent.value
        if (currentAgent == null) {
            showToast("ထိုးသူ (ကော်မရှင်) ရွေးချယ်ပါ")
            return false
        }
        val items = _entryItems.value
        if (items.isEmpty()) {
            showToast("ထိုးမည့် ဂဏန်းများ ထည့်သွင်းပါ")
            return false
        }

        val sessionVouchers = _vouchers.value.filter {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }
        val nextNo = (sessionVouchers.maxOfOrNull { it.no } ?: 0) + 1
        val newVoucher = Voucher(
            id = System.currentTimeMillis(),
            no = nextNo,
            agent = currentAgent.name,
            agentId = currentAgent.id,
            date = _selectedDate.value,
            session = _selectedSession.value,
            time = timeFormatter.format(Date()),
            items = items
        )

        val updated = listOf(newVoucher) + _vouchers.value
        _vouchers.value = updated
        repository.saveVouchers(updated)

        _entryItems.value = emptyList()
        showToast("ဘောင်ချာ သိမ်းဆည်းပြီးပါပြီ")
        navigateTo(Screen.VOUCHERS)
        return true
    }

    // Edit Voucher lines
    fun editVoucherItem(voucherId: Long, itemIndex: Int, newNumber: String, newAmount: Long) {
        val updated = _vouchers.value.mapNotNull { v ->
            if (v.id == voucherId) {
                if (itemIndex in v.items.indices) {
                    val newItems = v.items.toMutableList()
                    newItems[itemIndex] = BetItem(newNumber, newAmount)
                    v.copy(items = newItems)
                } else v
            } else v
        }
        _vouchers.value = updated
        repository.saveVouchers(updated)
        showToast("ဘောင်ချာ ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteVoucherItem(voucherId: Long, itemIndex: Int) {
        val updated = _vouchers.value.mapNotNull { v ->
            if (v.id == voucherId) {
                if (itemIndex in v.items.indices) {
                    val newItems = v.items.toMutableList()
                    newItems.removeAt(itemIndex)
                    if (newItems.isEmpty()) {
                        // Delete voucher when its last line is removed as per prompt
                        null
                    } else {
                        v.copy(items = newItems)
                    }
                } else v
            } else v
        }
        _vouchers.value = updated
        repository.saveVouchers(updated)
        showToast("ဘောင်ချာစာကြောင်း ဖျက်ပြီးပါပြီ")
    }

    fun deleteVoucher(voucherId: Long) {
        val updated = _vouchers.value.filter { it.id != voucherId }
        _vouchers.value = updated
        repository.saveVouchers(updated)
        showToast("ဘောင်ချာ ဖျက်ပြီးပါပြီ")
    }

    // Aggregations for Numbers Screen and Brake Screen
    fun getFilteredVouchers(): List<Voucher> {
        return _vouchers.value.filter {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }
    }

    fun getFilteredLayoffs(): List<LayoffRecord> {
        return _layoffs.value.filter {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }
    }

    // Total Bet aggregated per number (00 to 99)
    fun getAggregatedNumbers(): List<NumberSummary> {
        val vouchers = getFilteredVouchers()
        val layoffs = getFilteredLayoffs()

        val betMap = mutableMapOf<String, Long>()
        vouchers.forEach { v ->
            v.items.forEach { item ->
                betMap[item.n] = (betMap[item.n] ?: 0L) + item.a
            }
        }

        val layoffMap = mutableMapOf<String, Long>()
        layoffs.forEach { l ->
            l.items.forEach { item ->
                layoffMap[item.n] = (layoffMap[item.n] ?: 0L) + item.a
            }
        }

        val allNumbers = mutableListOf<NumberSummary>()
        for (i in 0..99) {
            val numStr = "%02d".format(i)
            val total = betMap[numStr] ?: 0L
            val layoff = layoffMap[numStr] ?: 0L
            val net = (total - layoff).coerceAtLeast(0L)
            if (total > 0 || layoff > 0) {
                allNumbers.add(NumberSummary(numStr, total, layoff, net))
            }
        }

        // Sorted by net/amount descending, then number ascending
        return allNumbers.sortedWith(compareByDescending<NumberSummary> { it.netAmount }.thenBy { it.n })
    }

    // Total retained amount for today's session
    fun getTotalRetainedMoney(): Long {
        val summaries = getAggregatedNumbers()
        return summaries.sumOf { it.netAmount }
    }

    // Layoff / Brake calculations
    fun updateBrakeLimit(limitStr: String) {
        _currentBrakeInput.value = limitStr
    }

    fun saveBrakeLimit() {
        val limit = _currentBrakeInput.value.toLongOrNull() ?: 50000L
        val updated = _settings.value.copy(brakeLimit = limit)
        _settings.value = updated
        repository.saveSettings(updated)
        showToast("ဘရိတ်သတ်မှတ်ချက် (${limit}) သိမ်းဆည်းပြီးပါပြီ")
    }

    // Layoff execution (ALL or 30 rows)
    fun executeLayoff(isAll: Boolean) {
        val limit = _currentBrakeInput.value.toLongOrNull() ?: _settings.value.brakeLimit
        val summaries = getAggregatedNumbers()
        val candidateItems = summaries.filter { it.netAmount > limit }

        val targetItems = if (isAll) candidateItems else candidateItems.take(30)
        if (targetItems.isEmpty()) {
            showToast("တင်ကွက်တင်ရန် ဘရိတ်ထက်ကျော်လွန်သော ဂဏန်းမရှိပါ")
            return
        }

        val layoffItems = targetItems.map { summary ->
            LayoffItem(
                n = summary.n,
                a = summary.netAmount - limit
            )
        }

        val totalLayoff = layoffItems.sumOf { it.a }
        val record = LayoffRecord(
            id = System.currentTimeMillis(),
            date = _selectedDate.value,
            session = _selectedSession.value,
            time = timeFormatter.format(Date()),
            limit = limit,
            items = layoffItems,
            total = totalLayoff
        )

        val updated = listOf(record) + _layoffs.value
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        showToast("တင်ကွက် (${layoffItems.size} ခု ၊ $totalLayoff) တင်ပြီးပါပြီ")
    }

    // Settlement
    fun getWinningNumberForCurrentSession(): String {
        return _wins.value.firstOrNull {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }?.n ?: ""
    }

    fun setWinningNumber(n: String) {
        val filtered = _wins.value.filterNot {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }
        val updated = listOf(WinRecord(_selectedDate.value, _selectedSession.value, n)) + filtered
        _wins.value = updated
        repository.saveWins(updated)
        showToast("ထွက်ဂဏန်း ($n) သိမ်းဆည်းပြီးပါပြီ")
    }

    fun calculateSettlement(winningNum: String): SettledCalculation {
        val vouchers = getFilteredVouchers()
        val totalRetained = getTotalRetainedMoney()

        // Commission = sum of (voucher total * agent rate / 100)
        val agentMap = _agents.value.associateBy { it.id }
        var totalCommission = 0.0
        vouchers.forEach { v ->
            val ag = agentMap[v.agentId] ?: _agents.value.firstOrNull { it.name == v.agent }
            val rate = ag?.rate ?: 15.0
            totalCommission += (v.totalAmount * (rate / 100.0))
        }

        val commLong = totalCommission.toLong()
        val retainedNet = (totalRetained - commLong).coerceAtLeast(0L)

        // Session multiplier (default 80)
        val sess = _sessions.value.firstOrNull { it.name == _selectedSession.value }
        val mult = sess?.mult ?: 80.0

        // Payout = retained net amount on winning number * multiplier
        val summaries = getAggregatedNumbers()
        val winItem = summaries.firstOrNull { it.n == winningNum }
        val winningRetained = winItem?.netAmount ?: 0L
        val payout = (winningRetained * mult).toLong()

        val profitLoss = retainedNet - payout

        return SettledCalculation(
            totalRetained = totalRetained,
            commission = commLong,
            retainedNet = retainedNet,
            multiplier = mult,
            winningNumber = winningNum,
            winningRetained = winningRetained,
            payout = payout,
            profitLoss = profitLoss
        )
    }

    fun commitSettlement(calc: SettledCalculation) {
        val record = SettledRecord(
            id = System.currentTimeMillis(),
            date = _selectedDate.value,
            session = _selectedSession.value,
            winningNumber = calc.winningNumber,
            totalRetained = calc.totalRetained,
            commission = calc.commission,
            retainedNet = calc.retainedNet,
            payout = calc.payout,
            profitLoss = calc.profitLoss,
            timestamp = timeFormatter.format(Date())
        )
        val updated = listOf(record) + _settled.value
        _settled.value = updated
        repository.saveSettled(updated)
        showToast("စာရင်းချုပ် သိမ်းဆည်းပြီးပါပြီ")
    }

    fun clearSettledLedger() {
        _settled.value = emptyList()
        repository.saveSettled(emptyList())
        showToast("စာရင်းချုပ် မှတ်တမ်းများကို ရှင်းထုတ်ပြီးပါပြီ")
    }

    // Blocked numbers
    fun addBlockedNumbers(nums: List<String>) {
        val set = _blocked.value.toMutableSet()
        set.addAll(nums.filter { it.length == 2 && it.all { c -> c.isDigit() } })
        _blocked.value = set
        repository.saveBlocked(set)
        showToast("မရဂဏန်းများ ထည့်သွင်းပြီးပါပြီ")
    }

    fun removeBlockedNumber(num: String) {
        val set = _blocked.value.toMutableSet()
        set.remove(num)
        _blocked.value = set
        repository.saveBlocked(set)
        showToast("မရဂဏန်း ဖျက်ပြီးပါပြီ")
    }

    // Sessions
    fun addSession(name: String, rate: Double, mult: Double) {
        if (_sessions.value.any { it.name.equals(name, ignoreCase = true) }) {
            showToast("အပိုင်း အမည် ထပ်နေပါသည်")
            return
        }
        val updated = _sessions.value + Session(name.trim(), rate, mult)
        _sessions.value = updated
        repository.saveSessions(updated)
        showToast("အပိုင်း အသစ် ထည့်ပြီးပါပြီ")
    }

    // Settings
    fun updateMaxBet(maxBet: Long) {
        val updated = _settings.value.copy(maxBet = maxBet)
        _settings.value = updated
        repository.saveSettings(updated)
        showToast("အများဆုံးထိုးကြေး ပြင်ဆင်ပြီးပါပြီ")
    }

    fun updateGuarantor(guarantor: String) {
        val updated = _settings.value.copy(guarantor = guarantor)
        _settings.value = updated
        repository.saveSettings(updated)
    }

    fun changePassword(current: String, newPass: String): Boolean {
        if (current != _settings.value.password) {
            showToast("လက်ရှိ စကားဝှက် မှားယွင်းနေပါသည်")
            return false
        }
        if (newPass.length < 4) {
            showToast("စကားဝှက် အနည်းဆုံး ၄ လုံး ရှိရမည်")
            return false
        }
        val updated = _settings.value.copy(password = newPass)
        _settings.value = updated
        repository.saveSettings(updated)
        showToast("စကားဝှက် အသစ် ပြောင်းလဲပြီးပါပြီ")
        return true
    }

    fun resetAllData() {
        repository.resetAllData()
        loadAllData()
        showToast("အချက်အလက်အားလုံး ပြန်လည်ရှင်းထုတ်ပြီးပါပြီ")
    }
}

data class SettledCalculation(
    val totalRetained: Long,
    val commission: Long,
    val retainedNet: Long,
    val multiplier: Double,
    val winningNumber: String,
    val winningRetained: Long,
    val payout: Long,
    val profitLoss: Long
)
