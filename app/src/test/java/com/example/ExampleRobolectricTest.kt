package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BetItem
import com.example.ui.MainViewModel
import com.example.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lucky2D", appName)
    }

    @Test
    fun `betting workflow stays on NumberEntryScreen and calculates agent breakdown and round profit loss`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.resetAllData()

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
}
