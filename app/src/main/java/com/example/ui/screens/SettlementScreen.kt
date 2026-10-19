package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettledRecord
import com.example.ui.Agent2DSettlementItem
import com.example.ui.MainViewModel
import com.example.ui.RoundSettlementSummary
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun SettlementScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val settledRecords by viewModel.settled.collectAsState()

    val savedWin = remember(wins, selectedDate, selectedSession) {
        viewModel.getWinningNumberForCurrentSession()
    }

    var winNumInput by remember(savedWin, selectedDate, selectedSession) {
        mutableStateOf(savedWin)
    }

    val cleanWin = winNumInput.trim()

    val currentCalculation = remember(vouchers, layoffs, sessions, selectedDate, selectedSession, cleanWin) {
        viewModel.calculateSettlement(cleanWin)
    }

    // Round-by-round (အကြိမ်ရေအလိုက်) Profit/Loss Breakdown for the selected date
    val roundSummaries = remember(vouchers, layoffs, sessions, wins, selectedDate, selectedSession, cleanWin) {
        val baseRounds = viewModel.getRoundSummariesForSelectedDate()
        baseRounds.map { r ->
            if (r.session == selectedSession && cleanWin.length == 2 && cleanWin != r.winningNumber) {
                val liveCalc = viewModel.calculateSettlementForSession(
                    date = selectedDate,
                    session = selectedSession,
                    winningNum = cleanWin
                )
                RoundSettlementSummary(
                    date = selectedDate,
                    session = r.session,
                    winningNumber = cleanWin,
                    totalBet = liveCalc.totalRetained,
                    commission = liveCalc.commission,
                    netAmount = liveCalc.retainedNet,
                    payout = liveCalc.payout,
                    profitLoss = liveCalc.profitLoss
                )
            } else {
                r
            }
        }
    }

    val grandTotalBet = remember(roundSummaries) { roundSummaries.sumOf { it.totalBet } }
    val grandTotalComm = remember(roundSummaries) { roundSummaries.sumOf { it.commission } }
    val grandTotalPayout = remember(roundSummaries) { roundSummaries.sumOf { it.payout } }
    val grandTotalProfitLoss = remember(grandTotalBet, grandTotalComm, grandTotalPayout) {
        grandTotalBet - (grandTotalComm + grandTotalPayout)
    }

    var recordToDelete by remember { mutableStateOf<SettledRecord?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    fun buildSettlementShareText(): String {
        val sb = StringBuilder()
        sb.append("================================\n")
        sb.append("   ရက်ချုပ် / အမြတ်အရှုံး စာရင်း\n")
        sb.append("================================\n")
        sb.append("ရက်စွဲ : $selectedDate\n")
        sb.append("အချိန် : $selectedSession\n")
        sb.append("ပေါက်သီး : ${cleanWin.ifEmpty { "-" }}\n")
        sb.append("--------------------------------\n")
        sb.append("စုစုပေါင်း ရောင်းကြေး : ${fmt(currentCalculation.totalRetained)}\n")
        sb.append("ကော်မရှင် : -${fmt(currentCalculation.commission)}\n")
        sb.append("ကော်နုတ်ပြီး ရောင်းကြေး : ${fmt(currentCalculation.retainedNet)}\n")
        sb.append("ပေါက်သီး ထိုးကြေး : ${fmt(currentCalculation.winningRetained)}\n")
        sb.append("စုစုပေါင်း လျော်ကြေး : -${fmt(currentCalculation.payout)}\n")
        sb.append("--------------------------------\n")
        val isProfit = currentCalculation.profitLoss >= 0
        val sign = if (isProfit) "+" else "-"
        val label = if (isProfit) "အမြတ်" else "အရှုံး"
        sb.append("အသားတင် ($label) : $sign${fmt(abs(currentCalculation.profitLoss))}\n")
        if (roundSummaries.isNotEmpty()) {
            sb.append("--------------------------------\n")
            sb.append("[အကြိမ်ရေအလိုက် အမြတ် / အရှုံး]\n")
            roundSummaries.forEach { r ->
                val rProfit = r.profitLoss >= 0
                val rSign = if (rProfit) "+${fmt(r.profitLoss)} (အမြတ်)" else "-${fmt(abs(r.profitLoss))} (အရှုံး)"
                sb.append("• ${r.session} [ပေါက်သီး: ${r.winningNumber.ifEmpty { "-" }}]\n")
                sb.append("  ရောင်းကြေး: ${fmt(r.totalBet)} | ကော်မရှင်: ${fmt(r.commission)} | လျော်ကြေး: ${fmt(r.payout)}\n")
                sb.append("  ရလဒ်: $rSign\n")
            }
            val gProfit = grandTotalProfitLoss >= 0
            val gSign = if (gProfit) "+${fmt(grandTotalProfitLoss)} (အမြတ်)" else "-${fmt(abs(grandTotalProfitLoss))} (အရှုံး)"
            sb.append("စုစုပေါင်း အသားတင် : $gSign\n")
        }
        sb.append("================================\n")
        return sb.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "ရက်ချုပ် / အမြတ်အရှုံး စာရင်း",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Winning Number & Quick Link to Per-Agent 2D Settlement
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ထွက်ဂဏန်း (ပေါက်သီး) ထည့်သွင်းရန်",
                                    color = LuckyHeader,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$selectedDate • $selectedSession",
                                    color = LuckyTextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(Screen.AGENT_SETTLEMENT) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuckyHeader),
                                modifier = Modifier.testTag("open_agent_settlement_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "2D စာရင်းချုပ်",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("2D စာရင်းချုပ်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = winNumInput,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    winNumInput = digits
                                },
                                placeholder = { Text("ပေါက်သီး (၀၀-၉၉)", color = LuckyTextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    color = LuckyDanger,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = LuckyBg,
                                    unfocusedContainerColor = LuckyBg,
                                    focusedBorderColor = LuckyHeader,
                                    unfocusedBorderColor = LuckyBorder
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settlement_winning_input")
                            )

                            Button(
                                onClick = {
                                    if (cleanWin.length == 2) {
                                        viewModel.setWinningNumber(cleanWin)
                                    } else {
                                        viewModel.showToast("ဂဏန်း ၂ လုံး အပြည့်ထည့်ပါ")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuckyHeader,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("save_winning_number_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "သိမ်းမည်", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ပေါက်သီးသိမ်းမည်", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Selected Round Calculation Card (Full-Width)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "လက်ရှိအချိန် စာရင်းချုပ် ($selectedSession)",
                                color = LuckyHeader,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (cleanWin.isNotEmpty()) LuckyDanger.copy(alpha = 0.12f) else LuckyCardDark,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ပေါက်သီး: ${cleanWin.ifEmpty { "-" }}",
                                    color = if (cleanWin.isNotEmpty()) LuckyDanger else LuckyTextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = LuckyBorder)

                        SettlementMetricRow("စုစုပေါင်း ရောင်းကြေး", fmt(currentCalculation.totalRetained), LuckyTextPrimary)
                        SettlementMetricRow("ကော်မရှင်", "-${fmt(currentCalculation.commission)}", LuckyWarning)
                        SettlementMetricRow("ကော်နုတ်ပြီး ရောင်းကြေး", fmt(currentCalculation.retainedNet), LuckyHeader)
                        SettlementMetricRow(
                            "ပေါက်သီး (${cleanWin.ifEmpty { "-" }}) ထိုးကြေး",
                            fmt(currentCalculation.winningRetained),
                            if (currentCalculation.winningRetained > 0) LuckyDanger else LuckyTextSecondary
                        )
                        SettlementMetricRow(
                            "လျော်ကြေး (အဆ ${fmt(currentCalculation.multiplier)})",
                            "-${fmt(currentCalculation.payout)}",
                            if (currentCalculation.payout > 0) LuckyDanger else LuckyTextSecondary
                        )

                        HorizontalDivider(color = LuckyBorder)

                        val isProfit = currentCalculation.profitLoss >= 0
                        val statusColor = if (isProfit) LuckySuccess else LuckyDanger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isProfit) "အသားတင် အမြတ်" else "အသားတင် အရှုံး",
                                    color = statusColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ရောင်းကြေး - (ကော်မရှင် + လျော်ကြေး)",
                                    color = LuckyTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "${if (isProfit) "+" else "-"}${fmt(abs(currentCalculation.profitLoss))}",
                                color = statusColor,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Full-width Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ShareHelper.copyToClipboard(context, buildSettlementShareText())
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("settlement_copy_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuckyHeader)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "ကူးယူမည်", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ကူးယူမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    ShareHelper.shareText(context, buildSettlementShareText(), "ရက်ချုပ်စာရင်း")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("settlement_share_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuckyHeader)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "မျှဝေမည်", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("မျှဝေမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (cleanWin.length == 2) {
                                        viewModel.setWinningNumber(cleanWin)
                                    }
                                    viewModel.commitSettlement(currentCalculation)
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("commit_settlement_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuckyHeader,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "မှတ်တမ်းတင်မည်", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("မှတ်တမ်းတင်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Round-by-Round (အကြိမ်ရေအလိုက်) Profit/Loss Breakdown & Overall Total Profit/Loss
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("round_breakdown_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "အကြိမ်ရေအလိုက် အမြတ် / အရှုံး တွက်ချက်မှု ($selectedDate)",
                            color = LuckyHeader,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Full-Width Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuckyCardDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("အချိန် / ပေါက်သီး", color = LuckyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.15f))
                            Text("ရောင်းကြေး", color = LuckyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("ကော်မရှင်", color = LuckyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.95f))
                            Text("လျော်ကြေး", color = LuckyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("အမြတ်/အရှုံး", color = LuckyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.15f))
                        }

                        roundSummaries.forEach { round ->
                            val rProfit = round.profitLoss >= 0
                            val rColor = if (rProfit) LuckySuccess else LuckyDanger
                            val rSign = if (rProfit) "+" else "-"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.15f)) {
                                    Text(
                                        text = round.session,
                                        color = LuckyTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ပေါက်သီး: ${round.winningNumber.ifEmpty { "-" }}",
                                        color = if (round.winningNumber.isNotEmpty()) LuckyDanger else LuckyTextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = fmt(round.totalBet),
                                    color = LuckyTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = fmt(round.commission),
                                    color = LuckyTextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.95f)
                                )

                                Text(
                                    text = fmt(round.payout),
                                    color = if (round.payout > 0) LuckyDanger else LuckyTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )

                                Column(
                                    modifier = Modifier.weight(1.15f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "$rSign${fmt(abs(round.profitLoss))}",
                                        color = rColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (rProfit) "အမြတ်" else "အရှုံး",
                                        color = rColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            HorizontalDivider(color = LuckyBorder)
                        }

                        // Overall Grand Total Profit/Loss across all rounds of the day
                        val isGrandProfit = grandTotalProfitLoss >= 0
                        val grandColor = if (isGrandProfit) LuckySuccess else LuckyDanger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(grandColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, grandColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "တစ်နေ့တာ စုစုပေါင်း (${if (isGrandProfit) "အမြတ်" else "အရှုံး"})",
                                    color = grandColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ရောင်းကြေး ${fmt(grandTotalBet)} | ကော်မရှင် ${fmt(grandTotalComm)} | လျော်ကြေး ${fmt(grandTotalPayout)}",
                                    color = LuckyTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "${if (isGrandProfit) "+" else "-"}${fmt(abs(grandTotalProfitLoss))}",
                                color = grandColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 4. Ledger Records (စာရင်းချုပ် မှတ်တမ်းများ) with Round-by-Round & Overall Profit/Loss
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "စာရင်းချုပ် မှတ်တမ်းများ (${settledRecords.size})",
                                color = LuckyHeader,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (settledRecords.isNotEmpty()) {
                                TextButton(
                                    onClick = { showClearAllDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "မှတ်တမ်းရှင်းမည်",
                                        tint = LuckyDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("မှတ်တမ်းရှင်းမည်", color = LuckyDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (settledRecords.isEmpty()) {
                            EmptyStateView(message = "သိမ်းဆည်းထားသော စာရင်းချုပ် မှတ်တမ်း မရှိသေးပါ")
                        } else {
                            val ledgerGrandTotalBet = settledRecords.sumOf { it.totalRetained }
                            val ledgerGrandTotalComm = settledRecords.sumOf { it.commission }
                            val ledgerGrandTotalPayout = settledRecords.sumOf { it.payout }
                            val ledgerGrandNet = settledRecords.sumOf { it.profitLoss }
                            val isLedgerProfit = ledgerGrandNet >= 0
                            val ledgerColor = if (isLedgerProfit) LuckySuccess else LuckyDanger

                            // Overall Ledger Profit/Loss Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ledgerColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .border(1.dp, ledgerColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "မှတ်တမ်းအားလုံး စုစုပေါင်း (${if (isLedgerProfit) "အမြတ်" else "အရှုံး"})",
                                        color = ledgerColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "ရောင်း ${fmt(ledgerGrandTotalBet)} | ကော် ${fmt(ledgerGrandTotalComm)} | လျော် ${fmt(ledgerGrandTotalPayout)}",
                                        color = LuckyTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${if (isLedgerProfit) "+" else "-"}${fmt(abs(ledgerGrandNet))}",
                                    color = ledgerColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            settledRecords.forEach { rec ->
                                val recProfit = rec.profitLoss >= 0
                                val recColor = if (recProfit) LuckySuccess else LuckyDanger
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LuckyBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, LuckyBorder, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "${rec.date} • ${rec.session}",
                                                color = LuckyTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = LuckyDanger.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "ပေါက်သီး: ${rec.winningNumber.ifEmpty { "-" }}",
                                                    color = LuckyDanger,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { recordToDelete = rec },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "ဖျက်မည်",
                                                tint = LuckyDanger,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("စုစုပေါင်း ရောင်းကြေး: ${fmt(rec.totalRetained)}", color = LuckyTextPrimary, fontSize = 11.sp)
                                        Text("ကော်မရှင်: ${fmt(rec.commission)}", color = LuckyTextSecondary, fontSize = 11.sp)
                                        Text("လျော်ကြေး: ${fmt(rec.payout)}", color = LuckyDanger, fontSize = 11.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (recProfit) "အမြတ်" else "အရှုံး",
                                            color = recColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${if (recProfit) "+" else "-"}${fmt(abs(rec.profitLoss))}",
                                            color = recColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (recordToDelete != null) {
        val target = recordToDelete!!
        DeleteConfirmDialog(
            title = "မှတ်တမ်း ဖျက်ရန်",
            message = "${target.date} (${target.session}) စာရင်းချုပ်မှတ်တမ်းကို ဖျက်ရန် သေချာပါသလား?",
            onDismiss = { recordToDelete = null },
            onConfirm = {
                viewModel.deleteSettledRecord(target.id)
                recordToDelete = null
            }
        )
    }

    if (showClearAllDialog) {
        DeleteConfirmDialog(
            title = "စာရင်းချုပ် မှတ်တမ်းအားလုံး ရှင်းရန်",
            message = "သိမ်းဆည်းထားသော စာရင်းချုပ် မှတ်တမ်းအားလုံးကို ဖျက်ရန် သေချာပါသလား?",
            onDismiss = { showClearAllDialog = false },
            onConfirm = {
                viewModel.clearSettledLedger()
                showClearAllDialog = false
            }
        )
    }
}

@Composable
private fun SettlementMetricRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = LuckyTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AgentSettlementScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val wins by viewModel.wins.collectAsState()

    val currentWin = remember(wins, selectedDate, selectedSession) {
        viewModel.getWinningNumberForCurrentSession()
    }

    val agentItems = remember(agents, vouchers, selectedDate, selectedSession, currentWin) {
        viewModel.getAgent2DSettlementList(currentWin)
    }

    val totalBetSum = remember(agentItems) { agentItems.sumOf { it.totalBet } }
    val totalCommSum = remember(agentItems) { agentItems.sumOf { it.commission } }
    val totalPayoutSum = remember(agentItems) { agentItems.sumOf { it.payout } }
    val totalProfitLossSum = remember(totalBetSum, totalCommSum, totalPayoutSum) {
        totalBetSum - (totalCommSum + totalPayoutSum)
    }

    fun formatAgentSettlementShare(item: Agent2DSettlementItem): String {
        val isProfit = item.profitLoss >= 0
        val sign = if (isProfit) "+" else "-"
        val status = if (isProfit) "အမြတ်" else "အရှုံး"
        return buildString {
            append("================================\n")
            append("   2D စာရင်းချုပ် (${item.agent.name})\n")
            append("================================\n")
            append("ရက်စွဲ : $selectedDate ($selectedSession)\n")
            append("ပေါက်သီး : ${currentWin.ifEmpty { "-" }}\n")
            append("--------------------------------\n")
            append("စုစုပေါင်း ရောင်းကြေး : ${fmt(item.totalBet)}\n")
            append("ကော်မရှင် (${fmt(item.agent.rate)}%) : -${fmt(item.commission)}\n")
            append("ကော်နုတ်ပြီး : ${fmt(item.netAmount)}\n")
            append("ပေါက်သီး (${currentWin.ifEmpty { "-" }}) : ${fmt(item.winBetAmount)}\n")
            append("လျော်ကြေး (အဆ ${fmt(item.agent.mult)}) : -${fmt(item.payout)}\n")
            append("--------------------------------\n")
            append("အသားတင် ($status) : $sign${fmt(abs(item.profitLoss))}\n")
            append("================================\n")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "2D စာရင်းချုပ်",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedDate • $selectedSession",
                                color = LuckyHeader,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = LuckyDanger.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ပေါက်သီး: ${currentWin.ifEmpty { "-" }}",
                                    color = LuckyDanger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val isOverallProfit = totalProfitLossSum >= 0
                        val overallColor = if (isOverallProfit) LuckySuccess else LuckyDanger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(overallColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "စုစုပေါင်း (${if (isOverallProfit) "အမြတ်" else "အရှုံး"})",
                                    color = overallColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ရောင်း ${fmt(totalBetSum)} | ကော် ${fmt(totalCommSum)} | လျော် ${fmt(totalPayoutSum)}",
                                    color = LuckyTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "${if (isOverallProfit) "+" else "-"}${fmt(abs(totalProfitLossSum))}",
                                color = overallColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (agentItems.isEmpty()) {
                item {
                    EmptyStateView(message = "ကော်မရှင် စာရင်း မရှိသေးပါ")
                }
            } else {
                items(agentItems, key = { it.agent.id }) { item ->
                    val isProfit = item.profitLoss >= 0
                    val statusColor = if (isProfit) LuckySuccess else LuckyDanger
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LuckyCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.agent.name,
                                        color = LuckyTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ကော်မရှင် ${fmt(item.agent.rate)}% • အဆ ${fmt(item.agent.mult)} • ဘောင်ချာ (${item.voucherCount})",
                                        color = LuckyTextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            ShareHelper.copyToClipboard(context, formatAgentSettlementShare(item))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "ကူးယူမည်", tint = LuckyHeader, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            ShareHelper.shareText(context, formatAgentSettlementShare(item), "2D စာရင်းချုပ်")
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "မျှဝေမည်", tint = LuckyHeader, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = LuckyBorder)

                            SettlementMetricRow("စုစုပေါင်း ရောင်းကြေး", fmt(item.totalBet), LuckyTextPrimary)
                            SettlementMetricRow("ကော်မရှင်", "-${fmt(item.commission)}", LuckyWarning)
                            SettlementMetricRow("ပေါက်သီး (${currentWin.ifEmpty { "-" }}) ထိုးကြေး", fmt(item.winBetAmount), LuckyDanger)
                            SettlementMetricRow("လျော်ကြေး", "-${fmt(item.payout)}", LuckyDanger)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isProfit) "အသားတင် အမြတ်" else "အသားတင် အရှုံး",
                                    color = statusColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${if (isProfit) "+" else "-"}${fmt(abs(item.profitLoss))}",
                                    color = statusColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
