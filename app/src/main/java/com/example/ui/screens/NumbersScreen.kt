package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.NumberAgentBreakdownItem
import com.example.ui.components.AppHeader
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.CompactPrimaryButton
import com.example.ui.components.RoundRequiredHelperBanner
import com.example.ui.components.SearchPill
import com.example.ui.components.formatWinningNumberTwoDigits
import com.example.ui.components.soundOnInputFocus
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

    val settled by viewModel.settled.collectAsState()
    val availableRounds = remember(sessions, selectedDate, vouchers, wins, settled) {
        viewModel.getAvailableRoundsForDate(selectedDate)
    }
    val hasRound = selectedSession.isNotBlank() && (sessions.any { it.name == selectedSession } || availableRounds.contains(selectedSession))

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedNumberForBreakdown by rememberSaveable { mutableStateOf<String?>(null) }

    val currentWinNum = remember(wins, selectedDate, selectedSession, hasRound) {
        if (hasRound) {
            wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n ?: ""
        } else ""
    }
    val hasValidWinNum = remember(currentWinNum, hasRound) {
        hasRound && currentWinNum.length == 2 && currentWinNum.all { it.isDigit() }
    }
    val formattedWinNum = remember(currentWinNum) {
        formatWinningNumberTwoDigits(currentWinNum)
    }

    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers().sortedWith(
            compareByDescending<com.example.ui.NumberSummary> { it.totalBet }.thenBy { it.n }
        )
    }

    val winningSummaryItem = remember(summaries, formattedWinNum, hasValidWinNum) {
        if (hasValidWinNum) {
            summaries.firstOrNull { it.n == formattedWinNum }
        } else null
    }

    val filteredSummaries = summaries.filter {
        searchQuery.isBlank() || it.n.contains(searchQuery.trim())
    }

    // Consolidated total bet amount (sum of all bets placed across all numbers: လက်ခံကွက် + တင်ကွက်)
    val grandTotal = filteredSummaries.sumOf { it.totalBet }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = stringResource(R.string.numbers_screen_title),
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() },
            selectedDate = selectedDate,
            onDateSelected = { viewModel.setDate(it) },
            historicalRounds = availableRounds
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 14.dp, end = 14.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoundRequiredHelperBanner(visible = !hasRound)

            SearchPill(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                label = stringResource(R.string.label_search_number),
                enabled = hasRound,
                modifier = Modifier.testTag("number_search_input")
            )
        }

        // Full-Width 3-Column Table: [ အမှတ်စဉ် | ဂဏန်း | ပမာဏ ]
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
                // 3-Column Table Header: [ အမှတ်စဉ် | ဂဏန်း | ပမာဏ ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("numbers_table_header"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.col_serial_no),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.label_number),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.label_amount),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f)
                    )
                }

                // Prominent Red Highlight Row at the top of the table when Winning Number is active/set
                if (hasValidWinNum) {
                    val winRowInteractionSource = remember { MutableInteractionSource() }
                    val isWinRowPressed by winRowInteractionSource.collectIsPressedAsState()
                    val winRowContentDesc = stringResource(R.string.winning_row_content_desc, formattedWinNum)
                    // Combined total amount of (လက်ခံကွက် + တင်ကွက်) for this winning number
                    val winTotalBetAmount = winningSummaryItem?.totalBet
                        ?: viewModel.getWinningNumberBetAmountForCurrentSession(formattedWinNum)

                    val winBgColor = if (isWinRowPressed) {
                        Color(0xFFFECACA)
                    } else {
                        Color(0xFFFEE2E2)
                    }
                    val winBorderColor = Color(0xFFEF4444)
                    val winTextColor = Color(0xFFDC2626)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .background(winBgColor)
                            .border(width = 1.5.dp, color = winBorderColor)
                            .clickable(
                                interactionSource = winRowInteractionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    viewModel.open2DSummaryFromWinningNumber(formattedWinNum, selectedSession)
                                }
                            )
                            .semantics(mergeDescendants = true) {
                                contentDescription = winRowContentDesc
                                role = Role.Button
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("highlighted_winning_number_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.winning_summary_label),
                            color = winTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("highlighted_winning_number_label")
                        )

                        Text(
                            text = formattedWinNum,
                            color = winTextColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("highlighted_winning_number_value")
                        )

                        Row(
                            modifier = Modifier.weight(1.4f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fmt(winTotalBetAmount),
                                color = winTextColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.End,
                                modifier = Modifier.testTag("highlighted_winning_number_amount")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = winTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                            val isWinningRow = hasValidWinNum && item.n == formattedWinNum
                            val rowBgColor = when {
                                isWinningRow -> Color(0xFFFEE2E2)
                                index % 2 == 0 -> LuckyCard
                                else -> LuckyCardDark
                            }
                            val rowTextColor = if (isWinningRow) Color(0xFFDC2626) else LuckyTextPrimary
                            val numTextColor = if (isWinningRow) Color(0xFFDC2626) else LuckyHeader

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBgColor)
                                    .then(
                                        if (isWinningRow) {
                                            Modifier.border(1.dp, Color(0xFFEF4444))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        selectedNumberForBreakdown = item.n
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("numbers_row_${item.n}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = if (isWinningRow) Color(0xFFDC2626) else LuckyTextSecondary,
                                    fontWeight = if (isWinningRow) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = item.n,
                                    color = numTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = fmt(item.totalBet),
                                    color = rowTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.4f)
                                )
                            }
                            HorizontalDivider(color = LuckyBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                        }
                    }
                }

                // Table Footer: စုစုပေါင်း (3-Column Aligned, No Frequency Count)
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
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = fmt(grandTotal),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }

    // Agent Detail Modal/View for Tapped Number or Winning Number
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
                        Text(stringResource(R.string.action_close), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
