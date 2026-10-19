package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MainViewModel
import com.example.ui.NumberAgentBreakdownItem
import com.example.ui.components.AppHeader
import com.example.ui.components.SearchPill
import com.example.ui.components.fmt
import com.example.ui.theme.*

@Composable
fun NumbersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val agents by viewModel.agents.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedNumberForBreakdown by remember { mutableStateOf<String?>(null) }
    var showWinEntryDialog by remember { mutableStateOf(false) }

    val currentWinNum = remember(wins, selectedDate, selectedSession) {
        wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n ?: ""
    }

    val totalWinningPayout = remember(currentWinNum, vouchers, layoffs, agents, sessions, selectedDate, selectedSession) {
        viewModel.getTotalWinningPayoutForNumber(currentWinNum)
    }

    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers()
    }

    val filteredSummaries = summaries.filter {
        searchQuery.isBlank() || it.n.contains(searchQuery.trim())
    }

    val grandTotal = filteredSummaries.sumOf { it.netAmount }
    val totalEntriesCount = filteredSummaries.sumOf { it.entryCount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "ဂဏန်းများ",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Winning Number & Total Winning Payout Highlight Card (Requirement 3)
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentWinNum.isNotEmpty()) Color(0xFFE8F5E9) else LuckyCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (currentWinNum.isNotEmpty()) LuckySuccess else LuckyBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        if (currentWinNum.isNotEmpty()) {
                            selectedNumberForBreakdown = currentWinNum
                        } else {
                            showWinEntryDialog = true
                        }
                    }
                    .testTag("winning_highlight_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentWinNum.isNotEmpty()) LuckySuccess else LuckyHeader),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentWinNum.isNotEmpty()) {
                                Text(
                                    text = currentWinNum,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "ထွက်ဂဏန်း",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (currentWinNum.isNotEmpty()) "ထွက်ဂဏန်း : $currentWinNum" else "ထွက်ဂဏန်း မထည့်ရသေးပါ",
                                color = LuckyTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (currentWinNum.isNotEmpty()) {
                                    "ပေါက်ငွေ ပမာဏ : ${fmt(totalWinningPayout)} (နှိပ်၍ စာရင်းကြည့်ရန်)"
                                } else {
                                    "ထွက်ဂဏန်း ထည့်သွင်းရန် နှိပ်ပါ"
                                },
                                color = if (currentWinNum.isNotEmpty()) LuckySuccess else LuckyTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showWinEntryDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = LuckyCard,
                            contentColor = LuckyHeader
                        ),
                        modifier = Modifier.height(34.dp).testTag("edit_win_on_numbers_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ထွက်ဂဏန်း", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Date & Search Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "နေ့စွဲ : $selectedDate ($selectedSession)",
                    color = LuckyTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "စုစုပေါင်း ဂဏန်း : ${filteredSummaries.size} လုံး",
                    color = LuckyHeader,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            SearchPill(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "ဂဏန်း ရိုက်ထည့်၍ ရှာရန်..."
            )
        }

        // Full-Width Screen-Fit Table (ဂဏန်းများ ဇယား)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = LuckyCard),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စဉ်",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        text = "ဂဏန်း",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "အကြိမ်ရေ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "ပမာဏ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                if (filteredSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ထိုးထားသော ဂဏန်း မရှိသေးပါ",
                            color = LuckyTextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(filteredSummaries, key = { _, item -> item.n }) { index, item ->
                            val isWinningRow = currentWinNum.isNotEmpty() && item.n == currentWinNum
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        when {
                                            isWinningRow -> Color(0xFFE8F5E9)
                                            index % 2 == 0 -> LuckyCard
                                            else -> LuckyCardDark
                                        }
                                    )
                                    .clickable {
                                        selectedNumberForBreakdown = item.n
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = LuckyTextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(0.7f)
                                )

                                Row(
                                    modifier = Modifier.weight(1.1f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.n,
                                        color = if (isWinningRow) LuckySuccess else LuckyHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Text(
                                    text = "${item.entryCount} ကြိမ်",
                                    color = LuckyTextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.1f)
                                )

                                Column(
                                    modifier = Modifier.weight(1.5f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = fmt(item.netAmount),
                                        color = if (isWinningRow) LuckySuccess else LuckyTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.End
                                    )
                                    if (item.layoffAmount > 0) {
                                        Text(
                                            text = "တင်: -${fmt(item.layoffAmount)}",
                                            color = LuckyDanger,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = LuckyBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                        }
                    }
                }

                // Table Footer: စုစုပေါင်း
                HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စုစုပေါင်း",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = "$totalEntriesCount ကြိမ်",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = fmt(grandTotal),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }

    // Winning Number Entry Dialog
    if (showWinEntryDialog) {
        var winInput by remember { mutableStateOf(currentWinNum) }
        AlertDialog(
            onDismissRequest = { showWinEntryDialog = false },
            containerColor = LuckyCard,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "ထွက်ဂဏန်း သတ်မှတ်မည်",
                    color = LuckyTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "နေ့စွဲ : $selectedDate ($selectedSession)",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = winInput,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            if (filtered.length <= 2) winInput = filtered
                        },
                        label = { Text("၂ လုံးဂဏန်း (00-99)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (winInput.length == 2) {
                            viewModel.setWinningNumber(winInput)
                            showWinEntryDialog = false
                            selectedNumberForBreakdown = winInput
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White)
                ) {
                    Text("သိမ်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWinEntryDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = LuckyTextSecondary)
                }
            }
        )
    }

    // Requirement 3: Agent Detail Modal/View for Tapped Number or Winning Number
    selectedNumberForBreakdown?.let { targetNum ->
        val breakdownList = remember(targetNum, vouchers, agents, sessions, selectedDate, selectedSession) {
            viewModel.getNumberAgentBreakdown(targetNum)
        }
        NumberAgentBreakdownDialog(
            number = targetNum,
            isWinningNumber = targetNum == currentWinNum,
            date = selectedDate,
            session = selectedSession,
            breakdownList = breakdownList,
            onDismiss = { selectedNumberForBreakdown = null }
        )
    }
}

@Composable
fun NumberAgentBreakdownDialog(
    number: String,
    isWinningNumber: Boolean,
    date: String,
    session: String,
    breakdownList: List<NumberAgentBreakdownItem>,
    onDismiss: () -> Unit
) {
    val totalBetOnNum = breakdownList.sumOf { it.betOnNumber }
    val totalSalesComm = breakdownList.sumOf { it.totalSalesCommission }
    val totalPayout = breakdownList.sumOf { it.winningPayout }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LuckyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 24.dp)
                .border(1.dp, LuckyBorder, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isWinningNumber) LuckySuccess else LuckyHeader)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isWinningNumber) "ထွက်ဂဏန်း ($number) - ရောင်းစာရေး စာရင်း" else "ဂဏန်း ($number) - ရောင်းစာရေး စာရင်း",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "နေ့စွဲ : $date ($session) | စုစုပေါင်းထိုးကြေး : ${fmt(totalBetOnNum)}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Table Header:
                // 1. ရောင်းစာရေး (Agent Name / ID)
                // 2. ရောင်းကော် (Total Sales Commission)
                // 3. ပေါက်သီးကြေး (Winning Payout Amount)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyCardDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ရောင်းစာရေး",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "ရောင်းကော်",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "ပေါက်သီးကြေး",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.3f)
                    )
                }
                HorizontalDivider(color = LuckyBorder)

                if (breakdownList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ရောင်းစာရေး စာရင်း မရှိသေးပါ",
                            color = LuckyTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        itemsIndexed(breakdownList) { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Agent Name / ID + Bet on this number
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(
                                        text = "${item.agentId}. ${item.agentName}",
                                        color = LuckyTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ထိုးကြေး: ${fmt(item.betOnNumber)} (${item.entryCount} ကြိမ်)",
                                        color = LuckyTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                // 2. Total Sales Commission (ရောင်းကော်)
                                Column(
                                    modifier = Modifier.weight(1.1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = fmt(item.totalSalesCommission),
                                        color = LuckyHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${fmt(item.rate)}%",
                                        color = LuckyTextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }

                                // 3. Winning Payout Amount (ပေါက်သီးကြေး)
                                Column(
                                    modifier = Modifier.weight(1.3f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = fmt(item.winningPayout),
                                        color = if (item.winningPayout > 0) LuckyDanger else LuckyTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "အဆ ${fmt(item.mult)}",
                                        color = LuckyTextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }

                // Total Footer Row
                HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyCardDark)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စုစုပေါင်း",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = fmt(totalSalesComm),
                        color = LuckyHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = fmt(totalPayout),
                        color = LuckyDanger,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ပိတ်မည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
