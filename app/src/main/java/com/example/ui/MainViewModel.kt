package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.SoundManager
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
    LAYOFF_HISTORY,
    AGENT_SETTLEMENT
}

data class NumberSummary(
    val n: String,
    val entryCount: Int = 0,
    val totalBet: Long,
    val layoffAmount: Long,
    val netAmount: Long
)

data class NumberAgentBreakdownItem(
    val agentId: Int,
    val agentName: String,
    val rate: Double,
    val mult: Double,
    val entryCount: Int,
    val betOnNumber: Long,
    val totalAgentSales: Long,
    val commissionOnNumber: Long,
    val totalSalesCommission: Long,
    val winningPayout: Long
)

data class RoundSettlementSummary(
    val date: String,
    val session: String,
    val winningNumber: String,
    val totalBet: Long,
    val commission: Long,
    val netAmount: Long,
    val payout: Long,
    val profitLoss: Long
)

data class Agent2DSettlementItem(
    val agent: Agent,
    val voucherCount: Int,
    val totalBet: Long,
    val commission: Long,
    val netAmount: Long,
    val winBetAmount: Long,
    val payout: Long,
    val profitLoss: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun parseAmountOrZero(input: String): Long = input.trim().toLongOrNull() ?: 0L
        fun parseDoubleOrZero(input: String): Double = input.trim().toDoubleOrNull() ?: 0.0

        fun validateBetAmountInput(rawInput: String?): BetAmountValidationResult = validateBetAmount(rawInput)
        fun validateBetAmountValue(amount: Long): BetAmountValidationResult = validateBetAmount(amount)

        fun shouldShowSaveButton(currentInput: String, savedValue: String): Boolean {
            return currentInput.isNotBlank() && currentInput.trim() != savedValue.trim()
        }

        fun shouldShowRoundSaveButton(roundNameInput: String): Boolean {
            return roundNameInput.isNotBlank()
        }

        fun formatWinningHeaderLine(
            winningNumber: String?,
            amount: Long,
            useThousandsSeparator: Boolean = false
        ): String {
            val clean = winningNumber?.trim().orEmpty()
            val numPart = if (clean.isEmpty()) {
                "-"
            } else {
                val parsed = clean.toIntOrNull()
                if (parsed != null && parsed in 0..99) "%02d".format(Locale.US, parsed) else clean
            }
            val safeAmount = if (numPart == "-") 0L else amount.coerceAtLeast(0L)
            val amtPart = if (useThousandsSeparator) {
                java.text.NumberFormat.getNumberInstance(Locale.US).format(safeAmount)
            } else {
                safeAmount.toString()
            }
            return "ပေါက်သီး $numPart=$amtPart"
        }

        fun formatDailyTotalResultText(profitLoss: Long): String {
            return when {
                profitLoss > 0L -> "အမြတ် +${java.text.NumberFormat.getNumberInstance(Locale.US).format(profitLoss)}"
                profitLoss < 0L -> "အရှုံး -${java.text.NumberFormat.getNumberInstance(Locale.US).format(kotlin.math.abs(profitLoss))}"
                else -> "0"
            }
        }
    }

    private val repository = StorageRepository(application)
    val soundManager: SoundManager = SoundManager

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
    private val timeFormatter = SimpleDateFormat("yyyy.MM.dd/HH:mm:ss", Locale.getDefault())

    val todayDate: String = dateFormatter.format(Date())
    val todayDisplayDate: String = displayDateFormatter.format(Date())

    private val _selectedDate = MutableStateFlow(todayDate)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedSession = MutableStateFlow("")
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

    val entryTotalAmount: Long
        get() = _entryItems.value.sumOf { it.a }

    // Brake screen limit state (starts empty)
    private val _currentBrakeInput = MutableStateFlow("")
    val currentBrakeInput: StateFlow<String> = _currentBrakeInput.asStateFlow()

    // Flow A: active winning number passed to 2D စာရင်းချုပ်
    private val _activeWinningNumberFor2DSummary = MutableStateFlow<String?>(null)
    val activeWinningNumberFor2DSummary: StateFlow<String?> = _activeWinningNumberFor2DSummary.asStateFlow()

    // Flow B: daily record currently being edited on HomeScreen (ပင်မစာမျက်နှာ)
    private val _editingSettledRecord = MutableStateFlow<SettledRecord?>(null)
    val editingSettledRecord: StateFlow<SettledRecord?> = _editingSettledRecord.asStateFlow()

    // Toast event message
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val hasActiveRound: Boolean
        get() = _sessions.value.isNotEmpty() &&
            _selectedSession.value.isNotBlank() &&
            _sessions.value.any { it.name == _selectedSession.value }

    val isResultInputEnabled: Boolean
        get() = hasActiveRound

    val isCommissionSetForEntry: Boolean
        get() = _entryAgent.value != null

    // Guard against rapid duplicate delete taps for the same numberId / settledRecordId
    private val deletedNumberIds = mutableSetOf<Long>()
    private val deletedSettledIds = mutableSetOf<Long>()
    private val deletedRoundKeys = mutableSetOf<String>()
    private val manuallyEditedRecordIds = mutableSetOf<Long>()
    private var nextSettledIdCounter = 1L

    init {
        soundManager.init(application)
        loadAllData()
    }

    fun loadAllData() {
        deletedNumberIds.clear()
        deletedSettledIds.clear()
        deletedRoundKeys.clear()
        manuallyEditedRecordIds.clear()
        val loadedSessions = repository.loadSessions()
        _sessions.value = loadedSessions
        if (loadedSessions.isNotEmpty()) {
            if (_selectedSession.value.isEmpty() || !loadedSessions.any { it.name == _selectedSession.value }) {
                _selectedSession.value = loadedSessions.first().name
            }
        } else {
            _selectedSession.value = ""
        }

        _agents.value = repository.loadAgents()
        _vouchers.value = repository.loadVouchers()
        _layoffs.value = repository.loadLayoffs()
        _blocked.value = repository.loadBlocked()
        _wins.value = repository.loadWins()
        _settled.value = repository.loadSettled()
        val s = repository.loadSettings()
        _settings.value = s
        soundManager.isSoundEnabled = s.soundEnabled
        _currentBrakeInput.value = if (s.brakeLimit > 0L) s.brakeLimit.toString() else ""
    }

    fun onInputFocusOrTap() {
        soundManager.playClick()
    }

    fun onEditActionStarted() {
        soundManager.playEdit()
    }

    fun setSoundEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(soundEnabled = enabled)
        _settings.value = updated
        soundManager.isSoundEnabled = enabled
        repository.saveSettings(updated)
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    fun setSession(session: String) {
        if (session.isBlank() || _sessions.value.any { it.name == session }) {
            _selectedSession.value = session
        }
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value == screen) return
        val existingIndex = screenHistory.indexOf(screen)
        if (existingIndex >= 0) {
            while (screenHistory.size > existingIndex + 1) {
                screenHistory.removeAt(screenHistory.lastIndex)
            }
        } else {
            screenHistory.add(screen)
        }
        _currentScreen.value = screen
    }

    fun getBackStack(): List<Screen> = screenHistory.toList()

    fun openNumbersFromWinningNumber() {
        navigateTo(Screen.NUMBERS)
    }

    fun open2DSummaryFromWinningNumber(
        winningNumber: String = getWinningNumberForCurrentSession(),
        session: String = _selectedSession.value
    ) {
        if (session.isNotBlank() && _sessions.value.any { it.name == session }) {
            _selectedSession.value = session
        }
        val cleanWin = winningNumber.trim()
        if (cleanWin.isNotEmpty()) {
            _activeWinningNumberFor2DSummary.value = cleanWin
        } else {
            _activeWinningNumberFor2DSummary.value = getWinningNumberForCurrentSession().ifEmpty { null }
        }
        navigateTo(Screen.AGENT_SETTLEMENT)
    }

    fun goBack(): Boolean {
        if (_editingSettledRecord.value != null && _currentScreen.value == Screen.HOME) {
            _editingSettledRecord.value = null
        }
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
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val nextId = (_agents.value.maxOfOrNull { it.id } ?: 0) + 1
        val newAgent = Agent(id = nextId, name = cleanName, rate = rate, mult = mult)
        val updated = _agents.value + newAgent
        _agents.value = updated
        repository.saveAgents(updated)
        soundManager.playSave()
        showToast("ကော်မရှင် အသစ် ထည့်သွင်းပြီးပါပြီ")
    }

    fun updateAgent(agent: Agent) {
        val cleanName = agent.name.trim()
        if (cleanName.isEmpty()) return
        val normalized = agent.copy(name = cleanName)
        val updated = _agents.value.map { if (it.id == normalized.id) normalized else it }
        _agents.value = updated
        if (_entryAgent.value?.id == normalized.id) {
            _entryAgent.value = normalized
        }
        repository.saveAgents(updated)
        soundManager.playEdit()
        showToast("ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteAgent(agentId: Int) {
        if (_agents.value.none { it.id == agentId }) return
        val updated = _agents.value.filter { it.id != agentId }
        _agents.value = updated
        repository.saveAgents(updated)
        soundManager.playDelete()
        showToast("ဖျက်ပြီးပါပြီ")
    }

    // Start Number Entry for an Agent
    fun startNumberEntry(agent: Agent) {
        _entryAgent.value = agent
        _entryItems.value = emptyList()
        navigateTo(Screen.NUMBER_ENTRY)
    }

    fun setEntryAgent(agent: Agent?) {
        _entryAgent.value = agent
    }

    fun validateBetAmountInput(rawInput: String?): BetAmountValidationResult = validateBetAmount(rawInput)

    fun validateBetAmountValue(amount: Long): BetAmountValidationResult = validateBetAmount(amount)

    // Number Entry Draft manipulations
    fun addEntryItem(item: BetItem): BetAmountValidationResult {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return BetAmountValidationResult.Empty
        }
        if (!isCommissionSetForEntry) return BetAmountValidationResult.Empty
        val cleanNum = item.n.trim()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return BetAmountValidationResult.Empty
        val amountValidation = validateBetAmount(item.a)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return amountValidation
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return BetAmountValidationResult.Empty
        }
        _entryItems.value = _entryItems.value + item.copy(n = cleanNum)
        soundManager.playSave()
        return BetAmountValidationResult.Valid
    }

    fun addEntryItems(items: List<BetItem>): BetAmountValidationResult {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return BetAmountValidationResult.Empty
        }
        if (!isCommissionSetForEntry) return BetAmountValidationResult.Empty
        if (items.isEmpty()) return BetAmountValidationResult.Empty
        val invalidResult = items.firstNotNullOfOrNull { item ->
            val v = validateBetAmount(item.a)
            if (!v.isValid) v else null
        }
        if (invalidResult != null) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return invalidResult
        }
        val valid = items.filter { item ->
            val cleanNum = item.n.trim()
            val blocked = _blocked.value.contains(cleanNum)
            cleanNum.length == 2 && cleanNum.all { it.isDigit() } && !blocked && validateBetAmount(item.a).isValid
        }
        if (valid.isEmpty()) return BetAmountValidationResult.Empty
        _entryItems.value = _entryItems.value + valid
        soundManager.playSave()
        return BetAmountValidationResult.Valid
    }

    fun removeLastEntryItem() {
        if (!isCommissionSetForEntry) return
        if (_entryItems.value.isNotEmpty()) {
            _entryItems.value = _entryItems.value.dropLast(1)
            soundManager.playDelete()
        }
    }

    fun editEntryItem(index: Int, newNumber: String, newAmount: Long): Boolean {
        if (!isCommissionSetForEntry) return false
        val cleanNum = newNumber.trim()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) {
            return false
        }
        val amountValidation = validateBetAmount(newAmount)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        if (index in _entryItems.value.indices) {
            val updated = _entryItems.value.toMutableList()
            updated[index] = updated[index].copy(n = cleanNum, a = newAmount)
            _entryItems.value = updated
            soundManager.playEdit()
            return true
        }
        return false
    }

    fun deleteEntryItem(index: Int): Boolean {
        if (!isCommissionSetForEntry) return false
        if (index in _entryItems.value.indices) {
            val updated = _entryItems.value.toMutableList()
            updated.removeAt(index)
            _entryItems.value = updated
            soundManager.playDelete()
            return true
        }
        return false
    }

    fun clearEntryItems() {
        _entryItems.value = emptyList()
    }

    // Stay on Bet Input View after submitting voucher (do NOT redirect to Vouchers page)
    // Also require an active round before saving voucher
    fun commitCurrentVoucher(): Boolean {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return false
        }
        val currentAgent = _entryAgent.value
        if (currentAgent == null) {
            showToast("ထိုးသူ ရွေးချယ်ပါ")
            return false
        }
        val items = _entryItems.value
        if (items.isEmpty()) {
            showToast("ထိုးမည့် ဂဏန်းများ ထည့်သွင်းပါ")
            return false
        }
        if (items.any { !validateBetAmount(it.a).isValid }) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
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
        soundManager.playSave()
        showToast("ဘောင်ချာ (No.$nextNo) သိမ်းဆည်းပြီးပါပြီ")
        return true
    }

    // Single-number edit and delete in a Voucher by unique numberId (never deletes the Voucher object)
    fun editNumber(numberId: Long, newNumber: String, newAmount: Long): Boolean {
        val cleanNum = newNumber.trim()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return false
        val amountValidation = validateBetAmount(newAmount)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        var changed = false
        val updated = _vouchers.value.map { v ->
            if (v.items.any { it.id == numberId }) {
                changed = true
                v.copy(
                    items = v.items.map { item ->
                        if (item.id == numberId) item.copy(n = cleanNum, a = newAmount) else item
                    }
                )
            } else {
                v
            }
        }
        if (!changed) return false
        _vouchers.value = updated
        repository.updateNumberById(numberId, cleanNum, newAmount) ?: repository.saveVouchers(updated)
        soundManager.playEdit()
        showToast("ဘောင်ချာ ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun deleteNumber(numberId: Long): Boolean {
        if (!deletedNumberIds.add(numberId)) {
            return false
        }
        var changed = false
        val updated = _vouchers.value.map { v ->
            if (v.items.any { it.id == numberId }) {
                changed = true
                v.copy(items = v.items.filter { it.id != numberId })
            } else {
                v
            }
        }
        if (!changed) {
            deletedNumberIds.remove(numberId)
            return false
        }
        _vouchers.value = updated
        repository.deleteNumberById(numberId) ?: repository.saveVouchers(updated)
        soundManager.playDelete()
        showToast("ဘောင်ချာစာကြောင်း ဖျက်ပြီးပါပြီ")
        return true
    }

    fun addNumberToVoucher(voucherId: Long, number: String, amount: Long): Boolean {
        val cleanNum = number.trim()
        if (cleanNum.length != 2 || !cleanNum.all { it.isDigit() }) return false
        val amountValidation = validateBetAmount(amount)
        if (!amountValidation.isValid) {
            showToast("ထိုးကြေး ပမာဏ လက်မခံပါ")
            return false
        }
        if (_blocked.value.contains(cleanNum)) {
            showToast("$cleanNum သည် မရဂဏန်း ဖြစ်ပါသည်")
            return false
        }
        val newItem = BetItem(n = cleanNum, a = amount)
        var changed = false
        val updated = _vouchers.value.map { v ->
            if (v.id == voucherId) {
                changed = true
                v.copy(items = v.items + newItem)
            } else {
                v
            }
        }
        if (!changed) return false
        _vouchers.value = updated
        repository.addNumberToVoucher(voucherId, newItem) ?: repository.saveVouchers(updated)
        soundManager.playSave()
        showToast("ဂဏန်း ထည့်သွင်းပြီးပါပြီ")
        return true
    }

    // Aggregations for Numbers Screen and Brake Screen
    fun getFilteredVouchers(date: String = _selectedDate.value, session: String = _selectedSession.value): List<Voucher> {
        if (session.isBlank()) return emptyList()
        return _vouchers.value.filter {
            it.date == date && it.session == session
        }
    }

    fun getFilteredLayoffs(date: String = _selectedDate.value, session: String = _selectedSession.value): List<LayoffRecord> {
        if (session.isBlank()) return emptyList()
        return _layoffs.value.filter {
            it.date == date && it.session == session
        }
    }

    // Total Bet aggregated per number (00 to 99) with entry count (အကြိမ်ရေ)
    fun getAggregatedNumbers(): List<NumberSummary> {
        val vouchers = getFilteredVouchers()
        val layoffs = getFilteredLayoffs()

        val betMap = mutableMapOf<String, Long>()
        val countMap = mutableMapOf<String, Int>()
        vouchers.forEach { v ->
            v.items.forEach { item ->
                betMap[item.n] = (betMap[item.n] ?: 0L) + item.a
                countMap[item.n] = (countMap[item.n] ?: 0) + 1
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
            val count = countMap[numStr] ?: 0
            val layoff = layoffMap[numStr] ?: 0L
            val net = (total - layoff).coerceAtLeast(0L)
            if (total > 0 || layoff > 0) {
                allNumbers.add(NumberSummary(numStr, count, total, layoff, net))
            }
        }

        return allNumbers.sortedWith(compareByDescending<NumberSummary> { it.netAmount }.thenBy { it.n })
    }

    // Breakdown of each sales agent's performance for a specific 2D number
    fun getNumberAgentBreakdown(number: String): List<NumberAgentBreakdownItem> {
        val vouchers = getFilteredVouchers()
        val sess = _sessions.value.firstOrNull { it.name == _selectedSession.value }
        val defaultMult = sess?.mult ?: 0.0
        val defaultRate = sess?.rate ?: 0.0

        val result = mutableListOf<NumberAgentBreakdownItem>()
        val processedAgentIds = mutableSetOf<Int>()

        for (agent in _agents.value) {
            processedAgentIds.add(agent.id)
            val agentVouchers = vouchers.filter { it.agentId == agent.id || it.agent == agent.name }
            var betOnNum = 0L
            var countOnNum = 0
            var totalSales = 0L

            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    totalSales += item.a
                    if (item.n == number) {
                        betOnNum += item.a
                        countOnNum += 1
                    }
                }
            }

            val effRate = if (agent.rate > 0.0) agent.rate else defaultRate
            val effMult = if (agent.mult > 0.0) agent.mult else defaultMult
            val commOnNum = Math.round(betOnNum.toDouble() * (effRate / 100.0))
            val totalComm = Math.round(totalSales.toDouble() * (effRate / 100.0))
            val payout = Math.round(betOnNum.toDouble() * effMult)

            if (betOnNum > 0L || totalSales > 0L || _agents.value.isNotEmpty()) {
                result.add(
                    NumberAgentBreakdownItem(
                        agentId = agent.id,
                        agentName = agent.name,
                        rate = effRate,
                        mult = effMult,
                        entryCount = countOnNum,
                        betOnNumber = betOnNum,
                        totalAgentSales = totalSales,
                        commissionOnNumber = commOnNum,
                        totalSalesCommission = totalComm,
                        winningPayout = payout
                    )
                )
            }
        }

        val extraVouchers = vouchers.filter { it.agentId !in processedAgentIds }
        extraVouchers.groupBy { it.agentId to it.agent }.forEach { (pair, vList) ->
            val (agId, agName) = pair
            var betOnNum = 0L
            var countOnNum = 0
            var totalSales = 0L
            vList.forEach { v ->
                v.items.forEach { item ->
                    totalSales += item.a
                    if (item.n == number) {
                        betOnNum += item.a
                        countOnNum += 1
                    }
                }
            }
            val commOnNum = Math.round(betOnNum.toDouble() * (defaultRate / 100.0))
            val totalComm = Math.round(totalSales.toDouble() * (defaultRate / 100.0))
            val payout = Math.round(betOnNum.toDouble() * defaultMult)
            result.add(
                NumberAgentBreakdownItem(
                    agentId = agId,
                    agentName = agName,
                    rate = defaultRate,
                    mult = defaultMult,
                    entryCount = countOnNum,
                    betOnNumber = betOnNum,
                    totalAgentSales = totalSales,
                    commissionOnNumber = commOnNum,
                    totalSalesCommission = totalComm,
                    winningPayout = payout
                )
            )
        }

        return result.sortedWith(
            compareByDescending<NumberAgentBreakdownItem> { it.betOnNumber }
                .thenByDescending { it.totalAgentSales }
                .thenBy { it.agentId }
        )
    }

    // Total winning payout amount for a given winning number on NumbersScreen
    fun getTotalWinningPayoutForNumber(winNum: String): Long {
        if (winNum.isBlank()) return 0L
        val breakdown = getNumberAgentBreakdown(winNum)
        val agentPayoutSum = breakdown.sumOf { it.winningPayout }
        if (agentPayoutSum > 0L) return agentPayoutSum
        val calc = calculateSettlement(winNum)
        return calc.payout
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
        val raw = _currentBrakeInput.value.trim()
        if (raw.isEmpty()) return
        val limit = parseAmountOrZero(raw)
        val updated = _settings.value.copy(brakeLimit = limit)
        _settings.value = updated
        repository.saveSettings(updated)
        soundManager.playSave()
        showToast("ဘရိတ်သတ်မှတ်ချက် (${limit}) သိမ်းဆည်းပြီးပါပြီ")
    }

    // Layoff execution (ALL or 30 rows)
    fun executeLayoff(isAll: Boolean) {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val limit = if (_currentBrakeInput.value.isNotBlank()) {
            parseAmountOrZero(_currentBrakeInput.value)
        } else {
            _settings.value.brakeLimit
        }
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
        soundManager.playSave()
        showToast("တင်ကွက် (${layoffItems.size} ခု ၊ $totalLayoff) တင်ပြီးပါပြီ")
    }

    // Layoff Item & Batch Management
    fun deleteLayoffRecord(recordId: Long) {
        if (_layoffs.value.none { it.id == recordId }) return
        val updated = _layoffs.value.filter { it.id != recordId }
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        soundManager.playDelete()
        showToast("တင်ကွက်မှတ်တမ်း ဖျက်ပြီးပါပြီ")
    }

    fun deleteLayoffItem(recordId: Long, itemIndex: Int) {
        var changed = false
        val updated = _layoffs.value.mapNotNull { rec ->
            if (rec.id == recordId) {
                val newItems = rec.items.toMutableList()
                if (itemIndex in newItems.indices) {
                    newItems.removeAt(itemIndex)
                    changed = true
                }
                if (newItems.isEmpty()) {
                    null
                } else {
                    rec.copy(items = newItems, total = newItems.sumOf { it.a })
                }
            } else rec
        }
        if (!changed) return
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        soundManager.playDelete()
        showToast("တင်ကွက် ဖျက်ပြီးပါပြီ")
    }

    fun editLayoffItem(recordId: Long, itemIndex: Int, newAmount: Long) {
        if (newAmount <= 0L) return
        var changed = false
        val updated = _layoffs.value.map { rec ->
            if (rec.id == recordId) {
                val newItems = rec.items.toMutableList()
                if (itemIndex in newItems.indices) {
                    newItems[itemIndex] = newItems[itemIndex].copy(a = newAmount)
                    changed = true
                }
                rec.copy(items = newItems, total = newItems.sumOf { it.a })
            } else rec
        }
        if (!changed) return
        _layoffs.value = updated
        repository.saveLayoffs(updated)
        soundManager.playEdit()
        showToast("တင်ကွက် ပြင်ဆင်ပြီးပါပြီ")
    }

    // Settlement
    fun getWinningNumberForCurrentSession(): String {
        if (!hasActiveRound) return ""
        return _wins.value.firstOrNull {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }?.n ?: ""
    }

    // Reuses the existing getAggregatedNumbers() calculation to get the combined total amount (လက်ခံကွက် + တင်ကွက်) bet on the winning number
    fun getWinningNumberBetAmountForCurrentSession(winNumOverride: String? = null): Long {
        val rawWin = winNumOverride?.trim() ?: getWinningNumberForCurrentSession()
        if (rawWin.isEmpty()) return 0L
        val parsed = rawWin.toIntOrNull()
        val normalizedWin = if (parsed != null && parsed in 0..99) "%02d".format(Locale.US, parsed) else rawWin
        val summary = getAggregatedNumbers().firstOrNull { it.n == normalizedWin }
        return summary?.totalBet ?: 0L
    }

    /**
     * Returns the amount kept within the configured brake limit (မိမိလက်ခံသော/ဘရိတ်အတွင်း ပေါက်သီးပမာဏ)
     * for the active winning number on the တင်ကွက်များ (Brake) screen.
     */
    fun getKeptWinningAmountForBrake(winNumOverride: String? = null, brakeLimitOverride: Long? = null): Long {
        val rawWin = winNumOverride?.trim() ?: getWinningNumberForCurrentSession()
        if (rawWin.isEmpty()) return 0L
        val parsed = rawWin.toIntOrNull()
        val normalizedWin = if (parsed != null && parsed in 0..99) "%02d".format(Locale.US, parsed) else rawWin
        val summary = getAggregatedNumbers().firstOrNull { it.n == normalizedWin } ?: return 0L
        val activeBrakeLimit = brakeLimitOverride ?: if (_currentBrakeInput.value.isNotBlank()) {
            parseAmountOrZero(_currentBrakeInput.value)
        } else {
            _settings.value.brakeLimit
        }
        return if (activeBrakeLimit > 0L) {
            minOf(summary.netAmount, activeBrakeLimit)
        } else {
            summary.netAmount
        }
    }

    fun getFormattedWinningHeaderForCurrentSession(
        winNumOverride: String? = null,
        useThousandsSeparator: Boolean = false
    ): String {
        val rawWin = winNumOverride?.trim() ?: getWinningNumberForCurrentSession()
        val amount = getWinningNumberBetAmountForCurrentSession(rawWin)
        return formatWinningHeaderLine(rawWin, amount, useThousandsSeparator)
    }

    fun hasSavedWinningNumberForCurrentSession(): Boolean {
        val win = getWinningNumberForCurrentSession()
        return win.length == 2 && win.all { it.isDigit() }
    }

    fun setWinningNumber(n: String) {
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val clean = n.trim()
        if (clean.isEmpty()) {
            clearWinningNumberForCurrentSession()
            return
        }
        if (clean.length != 2 || !clean.all { it.isDigit() }) return
        val filtered = _wins.value.filterNot {
            it.date == _selectedDate.value && it.session == _selectedSession.value
        }
        val updated = listOf(WinRecord(_selectedDate.value, _selectedSession.value, clean)) + filtered
        _wins.value = updated
        _activeWinningNumberFor2DSummary.value = clean
        repository.saveWins(updated)
        soundManager.playSave()
        showToast("ထွက်ဂဏန်း ($clean) သိမ်းဆည်းပြီးပါပြီ")
    }

    fun clearWinningNumberForCurrentSession() {
        val currentDate = _selectedDate.value
        val currentSess = _selectedSession.value
        if (_wins.value.none { it.date == currentDate && it.session == currentSess }) {
            _activeWinningNumberFor2DSummary.value = null
            return
        }
        val updated = _wins.value.filterNot { it.date == currentDate && it.session == currentSess }
        _wins.value = updated
        _activeWinningNumberFor2DSummary.value = null
        repository.saveWins(updated)
    }

    fun updateWinRecord(oldRecord: WinRecord, newRecord: WinRecord) {
        if (newRecord.n.trim().isEmpty()) return
        val updated = _wins.value.map { if (it.date == oldRecord.date && it.session == oldRecord.session) newRecord else it }
        _wins.value = updated
        if (newRecord.date == _selectedDate.value && newRecord.session == _selectedSession.value) {
            _activeWinningNumberFor2DSummary.value = newRecord.n.trim()
        }
        repository.saveWins(updated)
        soundManager.playEdit()
        showToast("ထွက်ဂဏန်း ပြင်ဆင်ပြီးပါပြီ")
    }

    fun deleteWinRecord(record: WinRecord) {
        if (_wins.value.none { it.date == record.date && it.session == record.session }) return
        val updated = _wins.value.filterNot { it.date == record.date && it.session == record.session }
        _wins.value = updated
        if (record.date == _selectedDate.value && record.session == _selectedSession.value) {
            _activeWinningNumberFor2DSummary.value = null
        }
        repository.saveWins(updated)
        soundManager.playDelete()
        showToast("ထွက်ဂဏန်း ဖျက်ပြီးပါပြီ")
    }

    // Calculate Settlement for a specific date, session, and winning number
    // Net Profit/Loss = Total Bet Amount - (Total Commission + Total Payout)
    fun calculateSettlementForSession(
        date: String = _selectedDate.value,
        session: String = _selectedSession.value,
        winningNum: String
    ): SettledCalculation {
        val vouchers = getFilteredVouchers(date, session)
        val layoffs = getFilteredLayoffs(date, session)

        val totalPerNum = mutableMapOf<String, Long>()
        vouchers.forEach { v ->
            v.items.forEach { item ->
                totalPerNum[item.n] = (totalPerNum[item.n] ?: 0L) + item.a
            }
        }

        val layoffPerNum = mutableMapOf<String, Long>()
        layoffs.forEach { l ->
            l.items.forEach { item ->
                layoffPerNum[item.n] = (layoffPerNum[item.n] ?: 0L) + item.a
            }
        }

        val retainedPerNum = mutableMapOf<String, Long>()
        for (i in 0..99) {
            val numStr = "%02d".format(i)
            val tot = totalPerNum[numStr] ?: 0L
            val lay = layoffPerNum[numStr] ?: 0L
            retainedPerNum[numStr] = (tot - lay).coerceAtLeast(0L)
        }

        val totalRetained = retainedPerNum.values.sum()

        val sess = _sessions.value.firstOrNull { it.name == session }
        val defaultRate = sess?.rate ?: 0.0
        val defaultMult = if ((sess?.mult ?: 0.0) > 0.0) {
            sess!!.mult
        } else {
            _agents.value.firstOrNull { it.mult > 0.0 }?.mult ?: 0.0
        }

        val agentMap = _agents.value.associateBy { it.id }
        var totalCommission = 0.0
        var totalPayout = 0.0

        val vouchersByAgent = vouchers.groupBy { it.agentId }
        for ((agentId, agentVouchers) in vouchersByAgent) {
            val ag = agentMap[agentId] ?: _agents.value.firstOrNull { it.name == agentVouchers.firstOrNull()?.agent }
            val rate = if ((ag?.rate ?: 0.0) > 0.0) ag!!.rate else defaultRate
            val mult = if ((ag?.mult ?: 0.0) > 0.0) ag!!.mult else defaultMult
            var agentCommissionBase = 0.0
            var agentWinRetainedBase = 0.0
            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    val tot = totalPerNum[item.n] ?: 0L
                    if (tot > 0L) {
                        val ret = retainedPerNum[item.n] ?: 0L
                        val scaled = item.a.toDouble() * (ret.toDouble() / tot.toDouble())
                        agentCommissionBase += scaled
                        if (winningNum.isNotEmpty() && item.n == winningNum) {
                            agentWinRetainedBase += scaled
                        }
                    }
                }
            }
            totalCommission += agentCommissionBase * (rate / 100.0)
            if (winningNum.isNotEmpty() && mult > 0.0) {
                totalPayout += agentWinRetainedBase * mult
            }
        }

        val commLong = Math.round(totalCommission)
        val retainedNet = (totalRetained - commLong).coerceAtLeast(0L)

        val winningRetained = if (winningNum.isNotEmpty()) (retainedPerNum[winningNum] ?: 0L) else 0L
        val payout = if (totalPayout > 0.0) {
            Math.round(totalPayout)
        } else if (defaultMult > 0.0 && winningNum.isNotEmpty()) {
            Math.round(winningRetained * defaultMult)
        } else 0L

        // Net Profit/Loss = Total Bet Amount - (Total Commission + Total Payout)
        val profitLoss = totalRetained - (commLong + payout)

        return SettledCalculation(
            totalRetained = totalRetained,
            commission = commLong,
            retainedNet = retainedNet,
            multiplier = defaultMult,
            winningNumber = winningNum,
            winningRetained = winningRetained,
            payout = payout,
            profitLoss = profitLoss
        )
    }

    fun calculateSettlement(winningNum: String): SettledCalculation {
        return calculateSettlementForSession(
            date = _selectedDate.value,
            session = _selectedSession.value,
            winningNum = winningNum
        )
    }

    // Round-by-round (အကြိမ်ရေအလိုက်) net profit/loss calculation for selected date
    fun getRoundSummariesForSelectedDate(): List<RoundSettlementSummary> {
        val date = _selectedDate.value
        val sessionNames = (_sessions.value.map { it.name } +
            _vouchers.value.filter { it.date == date }.map { it.session } +
            _wins.value.filter { it.date == date }.map { it.session })
            .filter { it.isNotBlank() }
            .distinct()

        return sessionNames.map { sessName ->
            val winNum = _wins.value.firstOrNull { it.date == date && it.session == sessName }?.n ?: ""
            val calc = calculateSettlementForSession(date = date, session = sessName, winningNum = winNum)
            RoundSettlementSummary(
                date = date,
                session = sessName,
                winningNumber = winNum,
                totalBet = calc.totalRetained,
                commission = calc.commission,
                netAmount = calc.retainedNet,
                payout = calc.payout,
                profitLoss = calc.profitLoss
            )
        }
    }

    // Agent calculation excluding layoff amounts for agent card
    fun getAgentSummaryData(agent: Agent, winNum: String): AgentSummaryData {
        val vouchers = getFilteredVouchers().filter { it.agentId == agent.id || it.agent == agent.name }
        val allVouchers = getFilteredVouchers()
        val layoffs = getFilteredLayoffs()

        val totalPerNum = mutableMapOf<String, Long>()
        allVouchers.forEach { v ->
            v.items.forEach { item ->
                totalPerNum[item.n] = (totalPerNum[item.n] ?: 0L) + item.a
            }
        }

        val layoffPerNum = mutableMapOf<String, Long>()
        layoffs.forEach { l ->
            l.items.forEach { item ->
                layoffPerNum[item.n] = (layoffPerNum[item.n] ?: 0L) + item.a
            }
        }

        val retainedPerNum = mutableMapOf<String, Long>()
        for (i in 0..99) {
            val numStr = "%02d".format(i)
            val tot = totalPerNum[numStr] ?: 0L
            val lay = layoffPerNum[numStr] ?: 0L
            retainedPerNum[numStr] = (tot - lay).coerceAtLeast(0L)
        }

        var totalBet = 0L
        var agentRetainedTotal = 0.0
        var agentRetainedWin = 0.0

        vouchers.forEach { v ->
            v.items.forEach { item ->
                totalBet += item.a
                val tot = totalPerNum[item.n] ?: 0L
                if (tot > 0L) {
                    val ret = retainedPerNum[item.n] ?: 0L
                    val scaled = item.a.toDouble() * (ret.toDouble() / tot.toDouble())
                    agentRetainedTotal += scaled
                    if (winNum.isNotEmpty() && item.n == winNum) {
                        agentRetainedWin += scaled
                    }
                }
            }
        }

        val commAmount = Math.round(agentRetainedTotal * (agent.rate / 100.0))
        val netAmount = (Math.round(agentRetainedTotal) - commAmount).coerceAtLeast(0L)
        val payout = if (agent.mult > 0.0 && winNum.isNotEmpty()) Math.round(agentRetainedWin * agent.mult) else 0L

        return AgentSummaryData(
            totalBet = totalBet,
            commission = commAmount,
            netAmount = netAmount,
            voucherCount = vouchers.size,
            payout = payout
        )
    }

    fun getAgent2DSettlementList(winNum: String): List<Agent2DSettlementItem> {
        val allVouchers = getFilteredVouchers()
        return _agents.value.map { agent ->
            val agentVouchers = allVouchers.filter { it.agentId == agent.id || it.agent == agent.name }
            var totalBet = 0L
            var winBetAmount = 0L
            agentVouchers.forEach { v ->
                v.items.forEach { item ->
                    totalBet += item.a
                    if (winNum.isNotEmpty() && item.n == winNum) {
                        winBetAmount += item.a
                    }
                }
            }
            val commission = Math.round(totalBet.toDouble() * (agent.rate / 100.0))
            val netAmount = (totalBet - commission).coerceAtLeast(0L)
            val payout = if (winNum.isNotEmpty() && agent.mult > 0.0) {
                Math.round(winBetAmount.toDouble() * agent.mult)
            } else 0L
            val profitLoss = totalBet - (commission + payout)
            Agent2DSettlementItem(
                agent = agent,
                voucherCount = agentVouchers.size,
                totalBet = totalBet,
                commission = commission,
                netAmount = netAmount,
                winBetAmount = winBetAmount,
                payout = payout,
                profitLoss = profitLoss
            )
        }
    }

    fun commitSettlement(calc: SettledCalculation) {
        val editing = _editingSettledRecord.value
        if (editing != null) {
            val updatedRecord = editing.copy(
                id = editing.id,
                date = _selectedDate.value,
                session = _selectedSession.value.ifBlank { editing.session },
                winningNumber = calc.winningNumber,
                totalRetained = calc.totalRetained,
                commission = calc.commission,
                retainedNet = calc.retainedNet,
                payout = calc.payout,
                profitLoss = calc.profitLoss
            )
            manuallyEditedRecordIds.add(editing.id)
            val updated = _settled.value.map { if (it.id == editing.id) updatedRecord else it }
            _settled.value = updated
            repository.updateSettledRecordById(editing.id, updatedRecord)
            _editingSettledRecord.value = null
            soundManager.playSave()
            showToast("စာရင်းချုပ် ပြင်ဆင်ပြီးပါပြီ")
            return
        }
        if (!hasActiveRound) {
            showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        val roundKey = "${_selectedDate.value}|${_selectedSession.value}"
        deletedRoundKeys.remove(roundKey)
        val record = SettledRecord(
            id = System.currentTimeMillis() + (nextSettledIdCounter++),
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
        soundManager.playSave()
        showToast("စာရင်းချုပ် သိမ်းဆည်းပြီးပါပြီ")
    }

    /**
     * Synchronizes active rounds that have bets or a winning number on the selected date into `_settled`
     * so they appear in the "အကြိမ်ရေအလိုက် အမြတ်ရှုံး တွက်ချက်မှု" table as editable/deletable records,
     * without recreating deleted records or overwriting manually edited records.
     */
    fun syncDailyRecordsFromCurrentDateRounds() {
        val rounds = getRoundSummariesForSelectedDate()
        if (rounds.isEmpty()) return
        var currentList = _settled.value
        var changed = false

        for (round in rounds) {
            val roundKey = "${round.date}|${round.session}"
            if (roundKey in deletedRoundKeys) continue

            val existing = currentList.firstOrNull { it.date == round.date && it.session == round.session }
            val hasActivity = _vouchers.value.any { it.date == round.date && it.session == round.session } ||
                _wins.value.any { it.date == round.date && it.session == round.session }

            if (existing != null) {
                if (existing.id !in manuallyEditedRecordIds && hasActivity) {
                    if (existing.totalRetained != round.totalBet ||
                        existing.commission != round.commission ||
                        existing.retainedNet != round.netAmount ||
                        existing.payout != round.payout ||
                        existing.profitLoss != round.profitLoss ||
                        existing.winningNumber != round.winningNumber
                    ) {
                        val updatedRec = existing.copy(
                            winningNumber = round.winningNumber,
                            totalRetained = round.totalBet,
                            commission = round.commission,
                            retainedNet = round.netAmount,
                            payout = round.payout,
                            profitLoss = round.profitLoss
                        )
                        currentList = currentList.map { if (it.id == existing.id) updatedRec else it }
                        changed = true
                    }
                }
            } else if (hasActivity) {
                val newRec = SettledRecord(
                    id = System.currentTimeMillis() + (nextSettledIdCounter++),
                    date = round.date,
                    session = round.session,
                    winningNumber = round.winningNumber,
                    totalRetained = round.totalBet,
                    commission = round.commission,
                    retainedNet = round.netAmount,
                    payout = round.payout,
                    profitLoss = round.profitLoss,
                    timestamp = timeFormatter.format(Date())
                )
                currentList = currentList + newRec
                changed = true
            }
        }

        if (changed) {
            _settled.value = currentList
            repository.saveSettled(currentList)
        }
    }

    fun getDailySummaryTotalSales(): Long = _settled.value.sumOf { it.totalRetained }

    fun getDailySummaryTotalPayout(): Long = _settled.value.sumOf { it.payout }

    fun getDailySummaryTotalProfitLoss(): Long = _settled.value.sumOf { it.profitLoss }

    fun getTotalSettledProfitLoss(): Long = _settled.value.sumOf { it.profitLoss }

    fun startEditingSettledRecord(record: SettledRecord, navigateToHome: Boolean = false) {
        soundManager.playEdit()
        _editingSettledRecord.value = record
        _selectedDate.value = record.date
        if (record.session.isNotBlank()) {
            _selectedSession.value = record.session
        }
        if (navigateToHome) {
            navigateTo(Screen.HOME)
        }
    }

    fun cancelEditingSettledRecord() {
        _editingSettledRecord.value = null
        if (_currentScreen.value == Screen.HOME && screenHistory.size > 1) {
            goBack()
        }
    }

    fun saveEditedSettledRecord(
        date: String? = null,
        session: String? = null,
        winningNumber: String? = null,
        totalRetained: Long? = null,
        commission: Long? = null,
        payout: Long? = null
    ): Boolean {
        val existing = _editingSettledRecord.value ?: return false
        val targetDate = date?.trim()?.ifEmpty { existing.date } ?: existing.date
        val targetSession = session?.trim()?.ifEmpty { existing.session } ?: existing.session
        val targetWin = winningNumber?.trim() ?: existing.winningNumber

        val hasExplicitAmounts = totalRetained != null || commission != null || payout != null
        val hasVouchersForTarget = _vouchers.value.any { it.date == targetDate && it.session == targetSession }

        val finalTotalRetained: Long
        val finalCommission: Long
        val finalRetainedNet: Long
        val finalPayout: Long
        val finalProfitLoss: Long

        if (!hasExplicitAmounts && hasVouchersForTarget) {
            val calc = calculateSettlementForSession(
                date = targetDate,
                session = targetSession,
                winningNum = targetWin
            )
            finalTotalRetained = calc.totalRetained
            finalCommission = calc.commission
            finalRetainedNet = calc.retainedNet
            finalPayout = calc.payout
            finalProfitLoss = calc.profitLoss
        } else {
            finalTotalRetained = (totalRetained ?: existing.totalRetained).coerceAtLeast(0L)
            finalCommission = (commission ?: existing.commission).coerceAtLeast(0L)
            finalRetainedNet = (finalTotalRetained - finalCommission).coerceAtLeast(0L)
            finalPayout = (payout ?: existing.payout).coerceAtLeast(0L)
            finalProfitLoss = finalTotalRetained - (finalCommission + finalPayout)
        }

        val updatedRecord = existing.copy(
            id = existing.id,
            date = targetDate,
            session = targetSession,
            winningNumber = targetWin,
            totalRetained = finalTotalRetained,
            commission = finalCommission,
            retainedNet = finalRetainedNet,
            payout = finalPayout,
            profitLoss = finalProfitLoss
        )

        manuallyEditedRecordIds.add(existing.id)
        val updatedList = _settled.value.map { if (it.id == existing.id) updatedRecord else it }
        _settled.value = updatedList
        repository.updateSettledRecordById(existing.id, updatedRecord)
        _editingSettledRecord.value = null
        soundManager.playSave()
        showToast("စာရင်းချုပ် ပြင်ဆင်ပြီးပါပြီ")
        if (_currentScreen.value == Screen.HOME && screenHistory.size > 1) {
            goBack()
        }
        return true
    }

    fun updateSettledRecord(
        recordId: Long,
        date: String? = null,
        session: String? = null,
        winningNumber: String? = null,
        totalRetained: Long? = null,
        commission: Long? = null,
        payout: Long? = null
    ): Boolean {
        val existing = _settled.value.firstOrNull { it.id == recordId } ?: return false
        val targetDate = date?.trim()?.ifEmpty { existing.date } ?: existing.date
        val targetSession = session?.trim()?.ifEmpty { existing.session } ?: existing.session
        val targetWin = winningNumber?.trim() ?: existing.winningNumber
        val finalTotalRetained = (totalRetained ?: existing.totalRetained).coerceAtLeast(0L)
        val finalCommission = (commission ?: existing.commission).coerceAtLeast(0L)
        val finalRetainedNet = (finalTotalRetained - finalCommission).coerceAtLeast(0L)
        val finalPayout = (payout ?: existing.payout).coerceAtLeast(0L)
        val finalProfitLoss = finalTotalRetained - (finalCommission + finalPayout)

        val updatedRecord = existing.copy(
            id = existing.id,
            date = targetDate,
            session = targetSession,
            winningNumber = targetWin,
            totalRetained = finalTotalRetained,
            commission = finalCommission,
            retainedNet = finalRetainedNet,
            payout = finalPayout,
            profitLoss = finalProfitLoss
        )
        manuallyEditedRecordIds.add(recordId)
        val updatedList = _settled.value.map { if (it.id == recordId) updatedRecord else it }
        _settled.value = updatedList
        repository.updateSettledRecordById(recordId, updatedRecord)
        soundManager.playSave()
        showToast("စာရင်းချုပ် ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun clearSettledLedger() {
        if (_settled.value.isEmpty()) return
        _settled.value = emptyList()
        repository.saveSettled(emptyList())
        soundManager.playDelete()
        showToast("စာရင်းချုပ် မှတ်တမ်းများကို ရှင်းထုတ်ပြီးပါပြီ")
    }

    fun deleteSettledRecord(recordId: Long) {
        if (recordId in deletedSettledIds) return
        val target = _settled.value.firstOrNull { it.id == recordId } ?: return
        deletedSettledIds.add(recordId)
        deletedRoundKeys.add("${target.date}|${target.session}")
        if (_editingSettledRecord.value?.id == recordId) {
            _editingSettledRecord.value = null
        }
        val updated = _settled.value.filter { it.id != recordId }
        _settled.value = updated
        repository.deleteSettledRecordById(recordId)
        soundManager.playDelete()
        showToast("စာရင်းချုပ်မှတ်တမ်း ဖျက်ပြီးပါပြီ")
    }

    // Blocked numbers
    fun addBlockedNumbers(nums: List<String>) {
        val validNums = nums.map { it.trim() }.filter { it.length == 2 && it.all { c -> c.isDigit() } }
        if (validNums.isEmpty()) return
        val set = _blocked.value.toMutableSet()
        set.addAll(validNums)
        _blocked.value = set
        repository.saveBlocked(set)
        soundManager.playSave()
        showToast("မရဂဏန်းများ ထည့်သွင်းပြီးပါပြီ")
    }

    fun editBlockedNumber(oldNum: String, newNum: String) {
        val cleanNew = newNum.trim()
        if (cleanNew.length != 2 || !cleanNew.all { it.isDigit() }) return
        val set = _blocked.value.toMutableSet()
        set.remove(oldNum)
        set.add(cleanNew)
        _blocked.value = set
        repository.saveBlocked(set)
        soundManager.playEdit()
        showToast("မရဂဏန်း ပြင်ဆင်ပြီးပါပြီ")
    }

    fun removeBlockedNumber(num: String) {
        if (!_blocked.value.contains(num)) return
        val set = _blocked.value.toMutableSet()
        set.remove(num)
        _blocked.value = set
        repository.saveBlocked(set)
        soundManager.playDelete()
        showToast("မရဂဏန်း ဖျက်ပြီးပါပြီ")
    }

    // Sessions (Rounds)
    fun addSession(name: String, rate: Double = 0.0, mult: Double = 0.0): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_sessions.value.any { it.name.equals(trimmed, ignoreCase = true) }) {
            showToast("အကြိမ် အမည် ထပ်နေပါသည်")
            return false
        }
        val updated = _sessions.value + Session(trimmed, rate, mult)
        _sessions.value = updated
        repository.saveSessions(updated)
        if (_selectedSession.value.isEmpty() || !_sessions.value.any { it.name == _selectedSession.value }) {
            _selectedSession.value = trimmed
        }
        soundManager.playSave()
        showToast("အကြိမ် အသစ် ထည့်ပြီးပါပြီ")
        return true
    }

    fun updateSession(oldName: String, updatedSession: Session): Boolean {
        val trimmedName = updatedSession.name.trim()
        if (trimmedName.isEmpty()) return false
        if (!oldName.equals(trimmedName, ignoreCase = true) &&
            _sessions.value.any { it.name.equals(trimmedName, ignoreCase = true) }
        ) {
            showToast("အကြိမ် အမည် ထပ်နေပါသည်")
            return false
        }
        val normalized = updatedSession.copy(name = trimmedName)
        val updated = _sessions.value.map { if (it.name == oldName) normalized else it }
        _sessions.value = updated
        repository.saveSessions(updated)
        if (_selectedSession.value == oldName) {
            _selectedSession.value = normalized.name
        }
        soundManager.playEdit()
        showToast("အကြိမ် ပြင်ဆင်ပြီးပါပြီ")
        return true
    }

    fun deleteSession(name: String) {
        if (_sessions.value.none { it.name == name }) return
        val updated = _sessions.value.filter { it.name != name }
        _sessions.value = updated
        repository.saveSessions(updated)
        if (updated.isEmpty()) {
            _selectedSession.value = ""
        } else if (_selectedSession.value == name) {
            _selectedSession.value = updated.first().name
        }
        soundManager.playDelete()
        showToast("အကြိမ် ဖျက်ပြီးပါပြီ")
    }

    // Settings
    fun changePassword(current: String, newPass: String): Boolean {
        if (current.isBlank() || newPass.isBlank()) {
            return false
        }
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
        soundManager.playSave()
        showToast("စကားဝှက် အသစ် ပြောင်းလဲပြီးပါပြီ")
        return true
    }

    fun resetAllData() {
        repository.resetAllData()
        loadAllData()
        soundManager.playDelete()
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
