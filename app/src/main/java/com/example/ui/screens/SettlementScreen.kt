package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettledRecord
import com.example.ui.Agent2DSettlementItem
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyListPlaceholder
import com.example.ui.components.ShareHelper
import com.example.ui.components.fmt
import com.example.ui.components.recordLongPress
import com.example.ui.theme.*

@Composable
fun SettlementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val settledList by viewModel.settled.collectAsState()

    var winNumberInput by remember { mutableStateOf("") }
    var showClearLedgerConfirm by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<SettledRecord?>(null) }

    LaunchedEffect(selectedDate, selectedSession) {
        val existingWin = viewModel.getWinningNumberForCurrentSession()
        winNumberInput = existingWin
    }

    BackHandler {
        viewModel.goBack()
    }

    // Calculate settlement values
    val calc = remember(winNumberInput, selectedDate, selectedSession) {
        viewModel.calculateSettlement(winNumberInput.trim())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        Surface(
            color = LuckyHeader,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { viewModel.goBack() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "ရက်ချုပ် (Settlement)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Text(
                    text = "$selectedDate ($selectedSession)",
                    color = LuckyLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Winning Number Input Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ထွက်ဂဏန်း (Winning Number)",
                            color = LuckyTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = winNumberInput,
                                onValueChange = {
                                    if (it.length <= 2) {
                                        winNumberInput = it
                                        if (it.length == 2) {
                                            viewModel.setWinningNumber(it)
                                        }
                                    }
                                },
                                placeholder = { Text("00", color = LuckyTextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(90.dp).testTag("winning_number_input")
                            )

                            if (winNumberInput.length == 2) {
                                Text(
                                    text = "ထွက်ဂဏန်းပေါ် လက်ခံငွေ: ${fmt(calc.winningRetained)}",
                                    color = LuckyTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Calculation Breakdown Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "စာရင်း အကျဉ်းချုပ်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            text = "တင်ကွက်ပို့ပြီးသော ငွေ ကော်မရှင် မတွက်ပါ",
                            color = LuckyTextSecondary,
                            fontSize = 12.sp
                        )

                        if (calc.multiplier <= 0.0) {
                            Text(
                                text = "အဆ သတ်မှတ်ချက် မရှိသေးပါ (လျော်ငွေ ၀)",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp
                            )
                        }

                        SettlementRow("စုစုပေါင်း လက်ခံငွေ (Total Retained)", fmt(calc.totalRetained), Color.White)
                        SettlementRow("ကော်မရှင် (Commission)", "-${fmt(calc.commission)}", LuckyDanger)
                        Divider(color = LuckyBorder, thickness = 0.5.dp)
                        SettlementRow("နုပ်ပြီးငွေ (Retained Net)", fmt(calc.retainedNet), LuckyLime)
                        SettlementRow("လျော်ကြေး (Payout ${calc.multiplier.toInt()}x)", "-${fmt(calc.payout)}", LuckyDanger)
                        Divider(color = LuckyBorder, thickness = 1.dp)

                        // Profit / Loss
                        val plColor = if (calc.profitLoss >= 0) LuckyLime else LuckyDanger
                        val plLabel = if (calc.profitLoss >= 0) "အမြတ် (Profit)" else "အရှုံး (Loss)"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plLabel,
                                color = plColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = fmt(calc.profitLoss),
                                color = plColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button "စာရင်းချုပ်မည်"
                        Button(
                            onClick = {
                                viewModel.commitSettlement(calc)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("commit_settlement_btn")
                        ) {
                            Text("စာရင်းချုပ်မည် (Settle Ledger)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Settled Records Ledger
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စာရင်းချုပ် မှတ်တမ်းများ (Ledger)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (settledList.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearLedgerConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ရှင်းထုတ်မည်", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (settledList.isEmpty()) {
                item {
                    Text(
                        text = "အချက်အလက် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ",
                        color = LuckyTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                itemsIndexed(settledList) { idx, item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LuckyCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .recordLongPress(onLongPress = { recordToDelete = item })
                            .border(1.dp, LuckyBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.date} (${item.session})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "ထွက်ဂဏန်း: ${item.winningNumber.ifEmpty { "-" }}",
                                    color = LuckyLime,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("နုပ်ပြီးငွေ: ${fmt(item.retainedNet)}", color = LuckyTextSecondary, fontSize = 12.sp)
                                Text("လျော်ငွေ: ${fmt(item.payout)}", color = LuckyDanger, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("အချိန်: ${item.timestamp}", color = LuckyTextMuted, fontSize = 11.sp)
                                val plCol = if (item.profitLoss >= 0) LuckyLime else LuckyDanger
                                Text(
                                    text = "ရလဒ်: ${fmt(item.profitLoss)}",
                                    color = plCol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clear Ledger Confirm Dialog
        if (showClearLedgerConfirm) {
            DeleteConfirmDialog(
                title = "ဖျက်ရန် သေချာပါသလား?",
                message = "စာရင်းချုပ်မှတ်တမ်းများ အားလုံးကို ဖျက်ရန် သေချာပါသလား?",
                onDismiss = { showClearLedgerConfirm = false },
                onConfirm = {
                    viewModel.clearSettledLedger()
                    showClearLedgerConfirm = false
                }
            )
        }

        // Delete Individual Settled Record Dialog
        recordToDelete?.let { rec ->
            DeleteConfirmDialog(
                title = "ဖျက်ရန် သေချာပါသလား?",
                message = "${rec.date} (${rec.session}) စာရင်းချုပ်မှတ်တမ်း ကို ဖျက်ရန် သေချာပါသလား?",
                onDismiss = { recordToDelete = null },
                onConfirm = {
                    viewModel.deleteSettledRecord(rec.id)
                    recordToDelete = null
                }
            )
        }
    }
}

@Composable
fun SettlementRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = LuckyTextSecondary, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AgentSettlementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val wins by viewModel.wins.collectAsState()

    val winNum = wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n ?: ""

    BackHandler {
        viewModel.goBack()
    }

    val agentSettlementList = remember(agents, vouchers, wins, selectedDate, selectedSession, winNum) {
        viewModel.getAgent2DSettlementList(winNum)
    }

    val grandTotalBet = agentSettlementList.sumOf { it.totalBet }
    val grandCommission = agentSettlementList.sumOf { it.commission }
    val grandNetAmount = agentSettlementList.sumOf { it.netAmount }
    val grandWinBet = agentSettlementList.sumOf { it.winBetAmount }
    val grandPayout = agentSettlementList.sumOf { it.payout }
    val grandProfitLoss = agentSettlementList.sumOf { it.profitLoss }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "2D စာရင်းချုပ်",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar with Back button, Date, Session, and Winning Number badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(LuckyCard, RoundedCornerShape(10.dp))
                            .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "နေ့စွဲ : $selectedDate",
                            color = LuckyTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "အပိုင်း : ${selectedSession.ifEmpty { "အကြိမ် မရှိသေးပါ" }}",
                            color = LuckyLime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(LuckyHeader, RoundedCornerShape(12.dp))
                            .border(1.dp, LuckyLime, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ထွက်ဂဏန်း: ${winNum.ifEmpty { "--" }}",
                            color = LuckyLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Overall 2D Settlement Summary Card across all agents
            item {
                val allReportText = buildAllAgents2DSettlementText(
                    date = selectedDate,
                    session = selectedSession,
                    winNum = winNum,
                    rows = agentSettlementList,
                    grandTotalBet = grandTotalBet,
                    grandCommission = grandCommission,
                    grandNetAmount = grandNetAmount,
                    grandWinBet = grandWinBet,
                    grandPayout = grandPayout,
                    grandProfitLoss = grandProfitLoss
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "စုစုပေါင်း 2D စာရင်းချုပ်",
                                color = LuckyLime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row {
                                IconButton(
                                    onClick = { ShareHelper.copyToClipboard(context, allReportText) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", tint = LuckyLime, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { ShareHelper.shareText(context, allReportText) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share All", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Divider(color = LuckyBorder, thickness = 0.5.dp)

                        SettlementRow(
                            label = "စုစုပေါင်း ထိုးကြေး",
                            value = fmt(grandTotalBet),
                            valueColor = Color.White
                        )
                        SettlementRow(
                            label = "စုစုပေါင်း ကော်မရှင်",
                            value = fmt(grandCommission),
                            valueColor = LuckyTextSecondary
                        )
                        SettlementRow(
                            label = "ကော်နှုတ်ပြီးငွေ",
                            value = fmt(grandNetAmount),
                            valueColor = LuckyLime
                        )
                        SettlementRow(
                            label = "ပေါက်သီး (${winNum.ifEmpty { "-" }})",
                            value = fmt(grandWinBet),
                            valueColor = Color.White
                        )
                        SettlementRow(
                            label = "စုစုပေါင်း လျော်ကြေး",
                            value = fmt(grandPayout),
                            valueColor = LuckyDanger
                        )

                        Divider(color = LuckyBorder, thickness = 1.dp)

                        val isProfit = grandProfitLoss >= 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isProfit) LuckyLime.copy(alpha = 0.12f) else LuckyDanger.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isProfit) "စုစုပေါင်း အမြတ်" else "စုစုပေါင်း အရှုံး",
                                color = if (isProfit) LuckyLime else LuckyDanger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${if (isProfit) "+" else "-"}${fmt(kotlin.math.abs(grandProfitLoss))}",
                                color = if (isProfit) LuckyLime else LuckyDanger,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Per-agent settlement cards
            if (agentSettlementList.isEmpty()) {
                item {
                    EmptyListPlaceholder(
                        text = "အချက်အလက် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ",
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                items(agentSettlementList, key = { it.agent.id }) { row ->
                    val singleText = buildSingleAgent2DSettlementText(
                        date = selectedDate,
                        session = selectedSession,
                        winNum = winNum,
                        row = row
                    )
                    val isCollect = row.profitLoss >= 0

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = LuckyCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Agent Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyHeader)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(LuckyBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${row.agent.id}",
                                            color = LuckyLime,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = row.agent.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "ကော်: ${fmt(row.agent.rate)}% | အဆ: ${fmt(row.agent.mult)} | ဘောင်ချာ: ${row.voucherCount} စောင်",
                                            color = LuckyTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { ShareHelper.copyToClipboard(context, singleText) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Agent", tint = LuckyLime, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { ShareHelper.shareText(context, singleText) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Agent", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Agent Settlement Details
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SettlementRow(
                                    label = "စုပေါင်းထိုးကြေး",
                                    value = fmt(row.totalBet),
                                    valueColor = Color.White
                                )
                                SettlementRow(
                                    label = "ကော်မရှင် (${fmt(row.agent.rate)}%)",
                                    value = fmt(row.commission),
                                    valueColor = LuckyTextSecondary
                                )
                                SettlementRow(
                                    label = "နုပ်ပြီးငွေ",
                                    value = fmt(row.netAmount),
                                    valueColor = LuckyLime
                                )
                                SettlementRow(
                                    label = "ပေါက်သီး (${winNum.ifEmpty { "-" }})",
                                    value = fmt(row.winBetAmount),
                                    valueColor = Color.White
                                )
                                SettlementRow(
                                    label = "ပေးငွေ (လျော်ကြေး)",
                                    value = fmt(row.payout),
                                    valueColor = if (row.payout > 0) LuckyDanger else LuckyTextSecondary
                                )

                                Divider(color = LuckyBorder, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isCollect) "ရရန် (အမြတ်)" else "ပေးရန် (အရှုံး)",
                                        color = if (isCollect) LuckyLime else LuckyDanger,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = fmt(kotlin.math.abs(row.profitLoss)),
                                        color = if (isCollect) LuckyLime else LuckyDanger,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
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

private fun buildSingleAgent2DSettlementText(
    date: String,
    session: String,
    winNum: String,
    row: Agent2DSettlementItem
): String {
    val statusLabel = if (row.profitLoss >= 0) "ရရန်" else "ပေးရန်"
    return listOf(
        "2D စာရင်းချုပ် - ${row.agent.name}",
        "နေ့စွဲ: $date ($session)",
        "ထွက်ဂဏန်း: ${winNum.ifEmpty { "-" }}",
        "စုပေါင်းထိုးကြေး: ${fmt(row.totalBet)}",
        "ကော်မရှင် (${fmt(row.agent.rate)}%): ${fmt(row.commission)}",
        "နုပ်ပြီးငွေ: ${fmt(row.netAmount)}",
        "ပေါက်သီး: ${fmt(row.winBetAmount)}",
        "ပေးငွေ: ${fmt(row.payout)}",
        "$statusLabel: ${fmt(kotlin.math.abs(row.profitLoss))}"
    ).joinToString("\n")
}

private fun buildAllAgents2DSettlementText(
    date: String,
    session: String,
    winNum: String,
    rows: List<Agent2DSettlementItem>,
    grandTotalBet: Long,
    grandCommission: Long,
    grandNetAmount: Long,
    grandWinBet: Long,
    grandPayout: Long,
    grandProfitLoss: Long
): String {
    val lines = mutableListOf(
        "2D စာရင်းချုပ်",
        "နေ့စွဲ: $date ($session)",
        "ထွက်ဂဏန်း: ${winNum.ifEmpty { "-" }}",
        "----------------"
    )
    rows.forEach { row ->
        val statusLabel = if (row.profitLoss >= 0) "ရရန်" else "ပေးရန်"
        lines.add("${row.agent.name}: ထိုးကြေး=${fmt(row.totalBet)} | နုပ်ပြီး=${fmt(row.netAmount)} | ပေါက်သီး=${fmt(row.winBetAmount)} | ပေးငွေ=${fmt(row.payout)} | $statusLabel=${fmt(kotlin.math.abs(row.profitLoss))}")
    }
    lines.add("----------------")
    lines.add("စုစုပေါင်း ထိုးကြေး: ${fmt(grandTotalBet)}")
    lines.add("စုစုပေါင်း ကော်မရှင်: ${fmt(grandCommission)}")
    lines.add("ကော်နှုတ်ပြီးငွေ: ${fmt(grandNetAmount)}")
    lines.add("စုစုပေါင်း ပေါက်သီး: ${fmt(grandWinBet)}")
    lines.add("စုစုပေါင်း လျော်ကြေး: ${fmt(grandPayout)}")
    val totalLabel = if (grandProfitLoss >= 0) "စုစုပေါင်း အမြတ်" else "စုစုပေါင်း အရှုံး"
    lines.add("$totalLabel: ${fmt(kotlin.math.abs(grandProfitLoss))}")
    return lines.joinToString("\n")
}
