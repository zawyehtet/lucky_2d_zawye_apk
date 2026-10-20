package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BetItem
import com.example.data.Session
import com.example.data.StorageRepository
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.util.SoundEffect
import com.example.util.SoundManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string resources for round labels and long press menu and confirm dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Lucky2D", context.getString(R.string.app_name))
        assertEquals("အကြိမ်", context.getString(R.string.round_label))
        assertEquals("အကြိမ် မရှိသေးပါ", context.getString(R.string.no_rounds_yet))
        assertEquals("အကြိမ်ရေ အရင်ထည့်ပါ", context.getString(R.string.add_round_first))
        assertEquals("ထိုးရန်", context.getString(R.string.action_place_bet))
        assertEquals("ပြင်မည်", context.getString(R.string.action_edit))
        assertEquals("ဖျက်မည်", context.getString(R.string.action_delete))
        assertEquals("ဖျက်မည်လား?", context.getString(R.string.confirm_delete_title))
        assertEquals("မဖျက်ပါ", context.getString(R.string.action_delete_cancel))
    }

    @Test
    fun `fresh install starts with empty rounds and empty brake input and blocks actions until round added`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        // 1. Fresh state has no rounds, empty selectedSession, and empty brake input
        assertTrue(viewModel.sessions.value.isEmpty())
        assertEquals("", viewModel.selectedSession.value)
        assertEquals("", viewModel.currentBrakeInput.value)
        assertFalse(viewModel.hasActiveRound)

        // 2. Attempting to save winning number or add bet without a round is blocked in ViewModel
        viewModel.setWinningNumber("42")
        assertTrue(viewModel.wins.value.isEmpty())

        viewModel.addAgent("ကိုအောင်", 15.0, 80.0)
        val agent = viewModel.agents.value.first()
        viewModel.setEntryAgent(agent)
        viewModel.addEntryItem(BetItem("15", 1000L))
        assertTrue(viewModel.entryItems.value.isEmpty())
        assertFalse(viewModel.commitCurrentVoucher())

        // 3. Add a round -> selects it automatically -> actions are now allowed
        viewModel.addSession("မနက်ပိုင်း", 15.0, 80.0)
        assertTrue(viewModel.hasActiveRound)
        assertEquals("မနက်ပိုင်း", viewModel.selectedSession.value)

        // 4. Delete the last round -> disables fields again and clears selection
        viewModel.deleteSession("မနက်ပိုင်း")
        assertTrue(viewModel.sessions.value.isEmpty())
        assertEquals("", viewModel.selectedSession.value)
        assertFalse(viewModel.hasActiveRound)
    }

    @Test
    fun `one-time migration removes unedited pre-seeded 12AM and 4PM defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lucky2d_offline_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(
                "lucky2d_sessions",
                """[{"name":"12.AM","rate":15.0,"mult":80.0},{"name":"4.PM","rate":15.0,"mult":80.0}]"""
            )
            .remove("lucky2d_sessions_migrated_v1")
            .commit()

        val repo = StorageRepository(context)
        val migratedSessions = repo.loadSessions()
        assertTrue(migratedSessions.isEmpty())
    }

    @Test
    fun `betting workflow stays on NumberEntryScreen and calculates agent breakdown and round profit loss`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        // 0. Add a round first (required before entering data)
        viewModel.addSession("နေ့လည်ပိုင်း", 15.0, 80.0)
        assertEquals("နေ့လည်ပိုင်း", viewModel.selectedSession.value)

        // 1. Add two agents
        viewModel.addAgent("ကိုအောင်", 15.0, 80.0)
        viewModel.addAgent("မစု", 10.0, 80.0)
        val agents = viewModel.agents.value
        assertEquals(2, agents.size)

        // 2. Start number entry for Agent 1 and submit voucher -> verify stays on NUMBER_ENTRY
        viewModel.startNumberEntry(agents[0])
        assertEquals(Screen.NUMBER_ENTRY, viewModel.currentScreen.value)

        viewModel.addEntryItem(BetItem("15", 1000L))
        viewModel.addEntryItem(BetItem("25", 2000L))
        val committed = viewModel.commitCurrentVoucher()
        assertTrue(committed)
        // Must remain on NUMBER_ENTRY for continuous input
        assertEquals(Screen.NUMBER_ENTRY, viewModel.currentScreen.value)
        assertTrue(viewModel.entryItems.value.isEmpty())

        // 3. Submit another voucher for Agent 2 on number "15"
        viewModel.setEntryAgent(agents[1])
        viewModel.addEntryItem(BetItem("15", 500L))
        assertTrue(viewModel.commitCurrentVoucher())
        assertEquals(Screen.NUMBER_ENTRY, viewModel.currentScreen.value)

        // 4. Verify Numbers Overview aggregation and Agent Detail Breakdown for "15"
        val aggregated = viewModel.getAggregatedNumbers()
        val num15 = aggregated.first { it.n == "15" }
        assertEquals(2, num15.entryCount)
        assertEquals(1500L, num15.totalBet)

        val breakdown15 = viewModel.getNumberAgentBreakdown("15")
        assertEquals(2, breakdown15.size)
        val agent1Breakdown = breakdown15.first { it.agentName == "ကိုအောင်" }
        assertEquals(1000L, agent1Breakdown.betOnNumber)
        assertEquals(3000L, agent1Breakdown.totalAgentSales)
        assertEquals(450L, agent1Breakdown.totalSalesCommission) // 15% of 3000
        assertEquals(80000L, agent1Breakdown.winningPayout) // 1000 * 80

        // 5. Verify Winning Number & Round-by-Round Profit/Loss Breakdown
        viewModel.setWinningNumber("15")
        val roundSummaries = viewModel.getRoundSummariesForSelectedDate()
        val currentRound = roundSummaries.first { it.session == viewModel.selectedSession.value }
        assertEquals("15", currentRound.winningNumber)
        assertEquals(3500L, currentRound.totalBet)
        // Commission = 450 (Agent 1) + 50 (10% of 500 for Agent 2) = 500
        assertEquals(500L, currentRound.commission)
        // Payout = 1500 * 80 = 120000
        assertEquals(120000L, currentRound.payout)
        // Net Profit/Loss = 3500 - (500 + 120000) = -117000
        assertEquals(-117000L, currentRound.profitLoss)
    }

    @Test
    fun `editing and deleting commission bet entries recalculates totals without changing commission setting`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        viewModel.addSession("မနက်ပိုင်း", 15.0, 80.0)
        viewModel.addAgent("ကိုမင်း", 12.0, 80.0)
        val agent = viewModel.agents.value.first()

        // While commission is not selected, addEntryItem / editEntryItem / deleteEntryItem are ignored
        viewModel.setEntryAgent(null)
        viewModel.addEntryItem(BetItem("12", 1000L))
        assertTrue(viewModel.entryItems.value.isEmpty())
        assertFalse(viewModel.editEntryItem(0, "12", 5000L))
        assertFalse(viewModel.deleteEntryItem(0))

        // Select commission agent and add two entries
        viewModel.setEntryAgent(agent)
        viewModel.addEntryItem(BetItem("12", 1000L))
        viewModel.addEntryItem(BetItem("34", 2000L))
        assertEquals(2, viewModel.entryItems.value.size)
        assertEquals(3000L, viewModel.entryItems.value.sumOf { it.a })

        // Edit first entry -> updates item and recalculates total, keeping commission agent unchanged
        assertTrue(viewModel.editEntryItem(0, "12", 5000L))
        assertEquals(5000L, viewModel.entryItems.value[0].a)
        assertEquals(7000L, viewModel.entryItems.value.sumOf { it.a })
        assertEquals(agent, viewModel.entryAgent.value)
        assertEquals(12.0, viewModel.agents.value.first().rate, 0.001)

        // Delete second entry -> removes item and recalculates total, keeping commission agent unchanged
        assertTrue(viewModel.deleteEntryItem(1))
        assertEquals(1, viewModel.entryItems.value.size)
        assertEquals(5000L, viewModel.entryItems.value.sumOf { it.a })
        assertEquals(agent, viewModel.entryAgent.value)
        assertEquals(12.0, viewModel.agents.value.first().rate, 0.001)
    }

    @Test
    fun `sound effects play on valid events and respect sound switch and empty save guard`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()
        viewModel.setSoundEnabled(true)
        SoundManager.clearRecordedEffects()

        // 1. Empty or invalid saves do NOT play save sound
        viewModel.addSession("   ", 15.0, 80.0)
        viewModel.addAgent("", 15.0, 80.0)
        assertFalse(viewModel.commitCurrentVoucher())
        assertTrue(SoundManager.playedEffects.isEmpty())

        // 2. Valid save plays SAVE sound
        viewModel.addSession("မနက်", 15.0, 80.0)
        assertEquals(listOf(SoundEffect.SAVE), SoundManager.playedEffects)

        // 3. Edit start and edit save play EDIT sound
        SoundManager.clearRecordedEffects()
        viewModel.onEditActionStarted()
        viewModel.updateSession("မနက်", Session("နေ့လည်", 15.0, 80.0))
        assertEquals(listOf(SoundEffect.EDIT, SoundEffect.EDIT), SoundManager.playedEffects)

        // 4. Confirmed delete plays DELETE sound
        SoundManager.clearRecordedEffects()
        viewModel.deleteSession("နေ့လည်")
        assertEquals(listOf(SoundEffect.DELETE), SoundManager.playedEffects)

        // 5. Turning Sound switch OFF suppresses all sounds and persists setting
        viewModel.setSoundEnabled(false)
        assertFalse(viewModel.settings.value.soundEnabled)
        assertFalse(StorageRepository(app).loadSettings().soundEnabled)

        SoundManager.clearRecordedEffects()
        viewModel.onInputFocusOrTap()
        viewModel.onEditActionStarted()
        viewModel.addSession("ညနေ", 15.0, 80.0)
        viewModel.deleteSession("ညနေ")
        assertTrue(SoundManager.playedEffects.isEmpty())
    }

    @Test
    fun `voucher single-number delete and edit keeps voucher id intact and recalculates totals`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ဂဏန်းပြင်ဆင်ရန်/ဖျက်ရန်", context.getString(R.string.voucher_number_menu_title))

        val viewModel = MainViewModel(app)
        viewModel.resetAllData()
        viewModel.setSoundEnabled(true)

        viewModel.addSession("မနက်ပိုင်း", 10.0, 80.0)
        viewModel.addAgent("ကိုကျော်", 10.0, 80.0)
        val agent = viewModel.agents.value.first()

        // Create a voucher with 5 numbers
        viewModel.setEntryAgent(agent)
        val fiveItems = listOf(
            BetItem("11", 1000L),
            BetItem("22", 2000L),
            BetItem("33", 3000L),
            BetItem("44", 4000L),
            BetItem("55", 5000L)
        )
        viewModel.addEntryItems(fiveItems)
        assertTrue(viewModel.commitCurrentVoucher())

        val initialVoucher = viewModel.vouchers.value.single()
        val originalVoucherId = initialVoucher.id
        assertEquals(5, initialVoucher.items.size)
        assertEquals(15000L, initialVoucher.totalAmount)

        // 1. Delete one number by its unique id -> leaves 4 numbers, same voucher id, updated total (12000L)
        val secondNumberId = initialVoucher.items[1].id // "22" -> 2000L
        SoundManager.clearRecordedEffects()
        assertTrue(viewModel.deleteNumber(secondNumberId))
        assertEquals(listOf(SoundEffect.DELETE), SoundManager.playedEffects)

        // Rapid duplicate tap for the same numberId is ignored
        assertFalse(viewModel.deleteNumber(secondNumberId))
        assertEquals(listOf(SoundEffect.DELETE), SoundManager.playedEffects)

        val afterOneDelete = viewModel.vouchers.value.single()
        assertEquals(originalVoucherId, afterOneDelete.id)
        assertEquals(4, afterOneDelete.items.size)
        assertFalse(afterOneDelete.items.any { it.id == secondNumberId })
        assertEquals(listOf("11", "33", "44", "55"), afterOneDelete.items.map { it.n })
        assertEquals(13000L, afterOneDelete.totalAmount)
        assertEquals(13000L, viewModel.getAggregatedNumbers().sumOf { it.totalBet })

        // 2. Edit one number by its unique id -> updates only that number and recalculates totals
        val targetEditItem = afterOneDelete.items.first { it.n == "33" } // 3000L -> change to "88", 6000L
        SoundManager.clearRecordedEffects()
        viewModel.onEditActionStarted()
        assertTrue(viewModel.editNumber(targetEditItem.id, "88", 6000L))
        assertEquals(listOf(SoundEffect.EDIT, SoundEffect.EDIT), SoundManager.playedEffects)

        val afterEdit = viewModel.vouchers.value.single()
        assertEquals(originalVoucherId, afterEdit.id)
        assertEquals(4, afterEdit.items.size)
        assertEquals("88", afterEdit.items.first { it.id == targetEditItem.id }.n)
        assertEquals(6000L, afterEdit.items.first { it.id == targetEditItem.id }.a)
        assertEquals(listOf("11", "88", "44", "55"), afterEdit.items.map { it.n })
        assertEquals(16000L, afterEdit.totalAmount)
        assertEquals(16000L, viewModel.getAggregatedNumbers().sumOf { it.totalBet })

        // 3. Delete all remaining numbers one by one -> deleting the last number leaves an empty voucher (0 numbers, total 0, same voucher id)
        val remainingIds = afterEdit.items.map { it.id }
        for (id in remainingIds) {
            assertTrue(viewModel.deleteNumber(id))
        }

        val emptyVoucher = viewModel.vouchers.value.single()
        assertEquals(originalVoucherId, emptyVoucher.id)
        assertEquals(0, emptyVoucher.items.size)
        assertEquals(0L, emptyVoucher.totalAmount)
        assertTrue(viewModel.getAggregatedNumbers().isEmpty())

        // Verify persisted repository state also retains the empty voucher with same id
        val persistedEmptyVoucher = StorageRepository(app).loadVouchers().single()
        assertEquals(originalVoucherId, persistedEmptyVoucher.id)
        assertTrue(persistedEmptyVoucher.items.isEmpty())
        assertEquals(0L, persistedEmptyVoucher.totalAmount)

        // 4. User can still add numbers to the empty voucher
        assertTrue(viewModel.addNumberToVoucher(originalVoucherId, "99", 2500L))
        val refilledVoucher = viewModel.vouchers.value.single()
        assertEquals(originalVoucherId, refilledVoucher.id)
        assertEquals(1, refilledVoucher.items.size)
        assertEquals("99", refilledVoucher.items.single().n)
        assertEquals(2500L, refilledVoucher.totalAmount)
    }

    @Test
    fun `2D summary winning number header reuses existing totals and formats string properly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        // 1. Before any round or winning number is set, empty state shows "-" and 0 without crashing
        assertEquals("ပေါက်သီး -=0", viewModel.getFormattedWinningHeaderForCurrentSession())
        assertEquals(
            "ပေါက်သီး -=0",
            context.getString(R.string.winning_summary_header_format, "-", "0")
        )

        // 2. Add two rounds and an agent
        viewModel.addSession("မနက်ပိုင်း", 15.0, 80.0)
        viewModel.addSession("ညနေပိုင်း", 15.0, 80.0)
        viewModel.setSession("မနက်ပိုင်း")
        viewModel.addAgent("ကိုအောင်", 15.0, 80.0)
        val agent = viewModel.agents.value.first()

        // 3. Place bets on "47" (10,000 + 5,000 = 15,000) and "07" (3,500) in "မနက်ပိုင်း"
        viewModel.setEntryAgent(agent)
        viewModel.addEntryItems(
            listOf(
                BetItem("47", 10000L),
                BetItem("07", 3500L),
                BetItem("47", 5000L)
            )
        )
        assertTrue(viewModel.commitCurrentVoucher())

        // 4. Set winning number to "47" -> total bet on "47" is 15000
        viewModel.setWinningNumber("47")
        val betOn47 = viewModel.getWinningNumberBetAmountForCurrentSession()
        assertEquals(15000L, betOn47)
        assertEquals(15000L, viewModel.getAgent2DSettlementList("47").sumOf { it.winBetAmount })
        assertEquals(
            "ပေါက်သီး 47=15000",
            viewModel.getFormattedWinningHeaderForCurrentSession(useThousandsSeparator = false)
        )
        assertEquals(
            "ပေါက်သီး 47=15,000",
            viewModel.getFormattedWinningHeaderForCurrentSession(useThousandsSeparator = true)
        )
        assertEquals(
            "ပေါက်သီး 47=15000",
            context.getString(R.string.winning_summary_header_format, "47", betOn47.toString())
        )

        // 5. Update winning number to "07" -> keeps leading zero and updates live to 3500
        viewModel.setWinningNumber("07")
        assertEquals(3500L, viewModel.getWinningNumberBetAmountForCurrentSession())
        assertEquals(
            "ပေါက်သီး 07=3500",
            viewModel.getFormattedWinningHeaderForCurrentSession(useThousandsSeparator = false)
        )
        assertEquals(
            "ပေါက်သီး 07=3,500",
            viewModel.getFormattedWinningHeaderForCurrentSession(useThousandsSeparator = true)
        )

        // 6. Switch round to "ညနေပိုင်း" (no winning number yet) -> updates live to "ပေါက်သီး -=0"
        viewModel.setSession("ညနေပိုင်း")
        assertEquals(0L, viewModel.getWinningNumberBetAmountForCurrentSession())
        assertEquals("ပေါက်သီး -=0", viewModel.getFormattedWinningHeaderForCurrentSession())
    }

    @Test
    fun `daily summary page syncs rounds, edits record in place with same id, deletes single id, and recalculates three totals`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        // Verify Daily Summary string resources
        assertEquals("ရက်ချုပ်/အမြတ်ရှုံး", context.getString(R.string.daily_summary_page_title))
        assertEquals("အကြိမ်ရေအလိုက် အမြတ်ရှုံး တွက်ချက်မှု", context.getString(R.string.daily_round_table_title))
        assertEquals("စုစုပေါင်း စာရင်းချုပ်", context.getString(R.string.daily_summary_totals_title))
        assertEquals("ရောင်းရငွေ", context.getString(R.string.daily_total_sales_label))
        assertEquals("လျော်ငွေ", context.getString(R.string.daily_total_payout_label))
        assertEquals("အမြတ်/အရှုံး", context.getString(R.string.daily_total_profit_loss_label))

        // Empty state starts with 0 for all three bottom totals
        assertEquals(0L, viewModel.getDailySummaryTotalSales())
        assertEquals(0L, viewModel.getDailySummaryTotalPayout())
        assertEquals(0L, viewModel.getDailySummaryTotalProfitLoss())
        assertEquals("0", MainViewModel.formatDailyTotalResultText(viewModel.getDailySummaryTotalProfitLoss()))

        // Create two rounds with bets and winning numbers
        viewModel.addSession("မနက်ပိုင်း", 10.0, 80.0)
        viewModel.addSession("ညနေပိုင်း", 10.0, 80.0)
        viewModel.addAgent("ကိုအောင်", 10.0, 80.0)
        val agent = viewModel.agents.value.first()

        // Round 1 ("မနက်ပိုင်း"): Sales = 10,000, Comm = 1,000, Win "15" (bet 0) -> Payout = 0, Profit = +9,000
        viewModel.setSession("မနက်ပိုင်း")
        viewModel.setEntryAgent(agent)
        viewModel.addEntryItem(BetItem("12", 10000L))
        assertTrue(viewModel.commitCurrentVoucher())
        viewModel.setWinningNumber("15")

        // Round 2 ("ညနေပိုင်း"): Sales = 5,000, Comm = 500, Win "25" (bet 1,000) -> Payout = 80,000, Loss = -75,500
        viewModel.setSession("ညနေပိုင်း")
        viewModel.setEntryAgent(agent)
        viewModel.addEntryItems(listOf(BetItem("25", 1000L), BetItem("30", 4000L)))
        assertTrue(viewModel.commitCurrentVoucher())
        viewModel.setWinningNumber("25")

        // Sync active rounds into Daily Summary records
        viewModel.syncDailyRecordsFromCurrentDateRounds()
        assertEquals(2, viewModel.settled.value.size)

        val morningRecord = viewModel.settled.value.first { it.session == "မနက်ပိုင်း" }
        val eveningRecord = viewModel.settled.value.first { it.session == "ညနေပိုင်း" }

        // Check initial three bottom totals:
        // Sales = 10,000 + 5,000 = 15,000
        // Payout = 0 + 80,000 = 80,000
        // Profit/Loss = +9,000 + (-75,500) = -66,500
        assertEquals(15000L, viewModel.getDailySummaryTotalSales())
        assertEquals(80000L, viewModel.getDailySummaryTotalPayout())
        assertEquals(-66500L, viewModel.getDailySummaryTotalProfitLoss())
        assertEquals("အရှုံး -66,500", MainViewModel.formatDailyTotalResultText(viewModel.getDailySummaryTotalProfitLoss()))

        // 1. Start editing morningRecord and cancel -> leaves record and totals unchanged
        viewModel.startEditingSettledRecord(morningRecord)
        viewModel.cancelEditingSettledRecord()
        assertEquals(2, viewModel.settled.value.size)
        assertEquals(10000L, viewModel.settled.value.first { it.id == morningRecord.id }.totalRetained)

        // 2. Edit morningRecord in place -> keeps same id, row count stays 2, and recalculates totals
        viewModel.startEditingSettledRecord(morningRecord)
        assertTrue(
            viewModel.saveEditedSettledRecord(
                session = "မနက်ပိုင်း",
                winningNumber = "15",
                totalRetained = 20000L,
                commission = 2000L,
                payout = 0L
            )
        )
        assertEquals(2, viewModel.settled.value.size)
        val editedMorning = viewModel.settled.value.first { it.id == morningRecord.id }
        assertEquals(morningRecord.id, editedMorning.id)
        assertEquals(20000L, editedMorning.totalRetained)
        assertEquals(18000L, editedMorning.profitLoss)

        // Updated three bottom totals after edit:
        // Sales = 20,000 + 5,000 = 25,000
        // Payout = 80,000
        // Profit/Loss = 18,000 - 75,500 = -57,500
        assertEquals(25000L, viewModel.getDailySummaryTotalSales())
        assertEquals(80000L, viewModel.getDailySummaryTotalPayout())
        assertEquals(-57500L, viewModel.getDailySummaryTotalProfitLoss())

        // 3. Delete eveningRecord by its unique id -> removes ONLY that row, guards duplicate delete, recalculates totals
        viewModel.deleteSettledRecord(eveningRecord.id)
        viewModel.deleteSettledRecord(eveningRecord.id) // duplicate tap ignored
        assertEquals(1, viewModel.settled.value.size)
        assertEquals(morningRecord.id, viewModel.settled.value.single().id)
        assertEquals(20000L, viewModel.getDailySummaryTotalSales())
        assertEquals(0L, viewModel.getDailySummaryTotalPayout())
        assertEquals(18000L, viewModel.getDailySummaryTotalProfitLoss())
        assertEquals("အမြတ် +18,000", MainViewModel.formatDailyTotalResultText(viewModel.getDailySummaryTotalProfitLoss()))
    }

    @Test
    fun `backend bet amount validation accepts normal high amounts and rejects zero negative and over MAX_BET_AMOUNT`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        assertEquals("ထိုးကြေး ပမာဏ လက်မခံပါ", context.getString(R.string.error_invalid_bet_amount))

        viewModel.addSession("မနက်ပိုင်း", 10.0, 80.0)
        viewModel.addAgent("ကိုအောင်", 10.0, 80.0)
        viewModel.setEntryAgent(viewModel.agents.value.first())

        // Valid normal amounts (100, 1000, 50000, 1000000) pass without any old UI maxBet cap
        assertTrue(viewModel.addEntryItem(BetItem("12", 100L)).isValid)
        assertTrue(viewModel.addEntryItem(BetItem("34", 1000L)).isValid)
        assertTrue(viewModel.addEntryItem(BetItem("56", 50000L)).isValid)
        assertTrue(viewModel.addEntryItem(BetItem("78", 1000000L)).isValid)
        assertEquals(4, viewModel.entryItems.value.size)

        // 0, negative, and > MAX_BET_AMOUNT fail in ViewModel
        assertFalse(viewModel.addEntryItem(BetItem("90", 0L)).isValid)
        assertFalse(viewModel.addEntryItem(BetItem("90", -500L)).isValid)
        assertFalse(viewModel.addEntryItem(BetItem("90", com.example.data.MAX_BET_AMOUNT + 1L)).isValid)
        assertEquals(4, viewModel.entryItems.value.size)
    }

    @Test
    fun `manage rounds title is renamed to အကြိမ်ရေ and save button visibility follows live input and edit state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()
        viewModel.setSoundEnabled(true)
        SoundManager.clearRecordedEffects()

        // 1. Verify title string is renamed to "အကြိမ်ရေ"
        assertEquals("အကြိမ်ရေ", context.getString(R.string.manage_round_times_title))

        // 2. While input field is empty or blank, Save button is hidden; saving blank does nothing
        var liveRoundInput = ""
        assertFalse(MainViewModel.shouldShowRoundSaveButton(liveRoundInput))
        assertFalse(viewModel.addSession("   ", 10.0, 80.0))
        assertTrue(viewModel.sessions.value.isEmpty())
        assertTrue(SoundManager.playedEffects.isEmpty())

        // 3. Typing text makes Save button visible immediately
        liveRoundInput = "မနက်ပိုင်း"
        assertTrue(MainViewModel.shouldShowRoundSaveButton(liveRoundInput))

        // 4. Saving round persists it, plays SAVE sound, and clears field so Save button hides again
        assertTrue(viewModel.addSession(liveRoundInput, 10.0, 80.0))
        liveRoundInput = ""
        assertFalse(MainViewModel.shouldShowRoundSaveButton(liveRoundInput))
        assertEquals(1, viewModel.sessions.value.size)
        assertEquals("မနက်ပိုင်း", viewModel.sessions.value.first().name)
        assertEquals(listOf(SoundEffect.SAVE), SoundManager.playedEffects)

        // 5. Editing an already saved round populates input and shows Save button again
        val existingRound = viewModel.sessions.value.first()
        liveRoundInput = existingRound.name
        assertTrue(MainViewModel.shouldShowRoundSaveButton(liveRoundInput))

        // 6. Saving edited round updates it and clears input so Save button hides again
        assertTrue(viewModel.updateSession(existingRound.name, Session("ညနေပိုင်း", 12.0, 80.0)))
        liveRoundInput = ""
        assertFalse(MainViewModel.shouldShowRoundSaveButton(liveRoundInput))
        assertEquals("ညနေပိုင်း", viewModel.sessions.value.single().name)
    }

    @Test
    fun `standardized summary titles 3-column daily ledger strings and launchSingleTop backstack work end to end`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

        // Standardized screen titles and 3-column Daily Ledger strings
        assertEquals("ဂဏန်းများ", context.getString(R.string.numbers_screen_title))
        assertEquals("2D စာရင်းချုပ်", context.getString(R.string.winning_summary_title_2d))
        assertEquals("စာရင်းချုပ်", context.getString(R.string.daily_ledger_title))
        assertEquals("ရက်စွဲ", context.getString(R.string.col_date))
        assertEquals("အကြိမ်", context.getString(R.string.col_round))
        assertEquals("အရှုံး/မြတ်", context.getString(R.string.col_loss_profit))
        assertEquals("အရှုံးမြတ် စုစုပေါင်း", context.getString(R.string.daily_ledger_total_label))

        // Rapid navigation with launchSingleTop does not push duplicate backstack entries
        viewModel.navigateTo(Screen.SETTLEMENT)
        viewModel.navigateTo(Screen.SETTLEMENT)
        assertEquals(Screen.SETTLEMENT, viewModel.currentScreen.value)
        assertTrue(viewModel.goBack())
        assertEquals(Screen.HOME, viewModel.currentScreen.value)

        // Verify Bottom Nav ရက်ချုပ်, 3-column Numbers header အမှတ်စဉ်, and Home footer branding strings
        assertEquals("ရက်ချုပ်", context.getString(R.string.nav_daily_summary))
        assertEquals("အမှတ်စဉ်", context.getString(R.string.col_serial_no))
        assertEquals("Power by Lcky2d3d ShweKhinMyay", context.getString(R.string.home_footer_branding))

        // Verify winning number volume on Numbers (လက်ခံကွက် + တင်ကွက်) and kept winning amount within brake limit on Brake
        viewModel.addSession("မနက်ပိုင်း", 10.0, 80.0)
        viewModel.addAgent("ကိုအောင်", 10.0, 80.0)
        viewModel.setEntryAgent(viewModel.agents.value.first())
        viewModel.addEntryItems(listOf(BetItem("47", 12000L), BetItem("15", 4000L)))
        assertTrue(viewModel.commitCurrentVoucher())

        viewModel.updateBrakeLimit("5000")
        viewModel.executeLayoff(isAll = true) // lays off 7000 on "47", leaving netAmount = 5000
        viewModel.setWinningNumber("47")

        // Full aggregated volume (လက်ခံကွက် + တင်ကွက်) on Numbers screen is 12000
        assertEquals(12000L, viewModel.getWinningNumberBetAmountForCurrentSession("47"))
        // Kept amount within configured brake limit (5000) on Brake screen is 5000
        assertEquals(5000L, viewModel.getKeptWinningAmountForBrake("47"))
    }
}

