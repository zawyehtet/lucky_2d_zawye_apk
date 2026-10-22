package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppHeader
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.RoundRequiredHelperBanner
import com.example.ui.components.fmt
import com.example.ui.components.soundOnInputFocus
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun BrakeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentBrakeInput by viewModel.currentBrakeInput.collectAsState()
    val savedBrakeLimitText by viewModel.savedBrakeLimitText.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val settledRecords by viewModel.settled.collectAsState()
    val agents by viewModel.agents.collectAsState()

    val availableRounds = remember(sessions, selectedDate, vouchers, wins, settledRecords) {
        viewModel.getAvailableRoundsForDate(selectedDate)
    }
    val hasRound = selectedSession.isNotBlank() && (sessions.any { it.name == selectedSession } || availableRounds.contains(selectedSession))
    val noRoundsText = stringResource(R.string.no_rounds_yet)

    val currentWinNum = remember(wins, selectedDate, selectedSession, hasRound) {
        if (hasRound) {
            wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n ?: ""
        } else ""
    }
    val hasValidWinNum = remember(currentWinNum, hasRound) {
        hasRound && currentWinNum.length == 2 && currentWinNum.all { it.isDigit() }
    }
    val formattedWinNum = remember(currentWinNum) {
        com.example.ui.components.formatWinningNumberTwoDigits(currentWinNum)
    }

    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val showBrakeSaveBtn = hasRound && MainViewModel.shouldShowSaveButton(currentBrakeInput, savedBrakeLimitText)

    val limit = MainViewModel.parseAmountOrZero(currentBrakeInput)
    val activeBrake = (if (currentBrakeInput.isNotBlank()) limit else settings.brakeLimit).coerceAtLeast(0L)

    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers()
    }

    // Amount kept within the configured brake limit (မိမိလက်ခံသော/ဘရိတ်အတွင်း ပေါက်သီးပမာဏ)
    // When activeBrake == 0, dealer retains zero risk (0).
    val keptWinningAmount = remember(summaries, formattedWinNum, hasValidWinNum, activeBrake) {
        if (hasValidWinNum && activeBrake > 0L) {
            val winSummary = summaries.firstOrNull { it.n == formattedWinNum }
            if (winSummary != null) {
                minOf(winSummary.netAmount, activeBrake)
            } else 0L
        } else 0L
    }

    // When activeBrake == 0, "လက်ခံကွက်" (Left Column) is completely empty (0 retained risk)
    val leftRetainedList = remember(summaries, activeBrake) {
        if (activeBrake > 0L) {
            summaries.mapNotNull { summary ->
                val retained = minOf(summary.netAmount, activeBrake)
                if (retained > 0L) Triple(summary.n, summary.entryCount, retained) else null
            }
        } else {
            emptyList()
        }
    }

    // When activeBrake == 0, "တင်ကွက်" (Right Column) receives 100% of all bets
    val rightLayoffList = remember(summaries, activeBrake) {
        summaries.mapNotNull { summary ->
            if (summary.netAmount > activeBrake) {
                Triple(summary.n, summary.entryCount, summary.netAmount - activeBrake)
            } else null
        }
    }

    val liveCalc = remember(vouchers, layoffs, agents, sessions, selectedDate, selectedSession, formattedWinNum, hasValidWinNum, currentBrakeInput, settings.brakeLimit) {
        viewModel.calculateSettlementForSession(
            date = selectedDate,
            session = selectedSession,
            winningNum = if (hasValidWinNum) formattedWinNum else ""
        )
    }

    val totalRetained = leftRetainedList.sumOf { it.third }
    val totalLayoff = rightLayoffList.sumOf { it.third }

    val ledgerSales = if (activeBrake <= 0L) 0L else liveCalc.totalRetained
    val ledgerComm = if (activeBrake <= 0L) 0L else liveCalc.commission
    val ledgerNet = if (activeBrake <= 0L) 0L else liveCalc.retainedNet
    val ledgerPayout = if (activeBrake <= 0L) 0L else liveCalc.payout
    val ledgerProfitLoss = if (activeBrake <= 0L) 0L else liveCalc.profitLoss

    val orangeHeaderBg = Color(0xFFF57C00)

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "တင်ကွက်များ",
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) },
                onBack = { viewModel.goBack() },
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setDate(it) },
                historicalRounds = availableRounds,
                actions = {
                    Button(
                        onClick = { viewModel.exportCurrentSessionLedgerToFile(context, launchShare = true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = LuckyHeader
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("export_ledger_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            // Header section: "ဘရိတ် : [Amount]", Date, "အပိုင်း : [Session]", and Post-Draw Winning Number box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoundRequiredHelperBanner(visible = !hasRound)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        com.example.ui.components.InteractiveDateChip(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.setDate(it) }
                        )
                        Text(
                            text = "ဘရိတ် : ${fmt(activeBrake)}",
                            color = LuckyHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "အပိုင်း : ${if (hasRound) selectedSession else noRoundsText}",
                            color = LuckyTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Post-Draw State: prominent winning number box right next to "အပိုင်း : [Session]"
                        if (hasValidWinNum) {
                            Surface(
                                color = Color(0xFFFFF59D),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .border(1.5.dp, LuckyDanger, RoundedCornerShape(6.dp))
                                    .testTag("brake_header_winning_box")
                            ) {
                                Text(
                                    text = formattedWinNum,
                                    color = LuckyDanger,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOutlinedTextField(
                        value = currentBrakeInput,
                        onValueChange = { viewModel.updateBrakeLimit(it.filter { c -> c.isDigit() }) },
                        enabled = hasRound,
                        label = { Text(stringResource(R.string.label_brake)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .soundOnInputFocus()
                            .testTag("brake_limit_input")
                    )

                    AnimatedVisibility(visible = showBrakeSaveBtn) {
                        Button(
                            onClick = {
                                viewModel.saveBrakeLimit()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("brake_save_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_save), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // 2-Column Split Table Card
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
                    // Yellow/Orange Split Headers: လက်ခံကွက် | တင်ကွက်
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(orangeHeaderBg)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "လက်ခံကွက် (${leftRetainedList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.6f)))
                        Text(
                            text = "တင်ကွက် (${rightLayoffList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Balanced Subheaders: Left "ဂဏန်းများ | ပမာဏ" and Right "ဂဏန်းများ | ပမာဏ"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဂဏန်းများ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("ပမာဏ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color.White.copy(alpha = 0.4f)))
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဂဏန်းများ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("ပမာဏ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    HorizontalDivider(color = LuckyBorder)

                    // Split Body Content (Left Column vs Right Column)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val regularLeftRetainedList = remember(leftRetainedList, formattedWinNum, hasValidWinNum) {
                            if (hasValidWinNum) {
                                leftRetainedList.filter { it.first != formattedWinNum }
                            } else {
                                leftRetainedList
                            }
                        }

                        // LEFT COLUMN: လက်ခံကွက် + Post-Draw Single-Round Summary Ledger + Left Footer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                // Pinned Winning Number Row in "လက်ခံကွက်" highlighted in Yellow/Red background with retained amount within brake limit
                                if (hasValidWinNum && activeBrake > 0L) {
                                    item(key = "kept_winning_number_top_row") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFFFF59D))
                                                .border(1.5.dp, Color(0xFFEF4444))
                                                .padding(vertical = 8.dp, horizontal = 10.dp)
                                                .testTag("brake_kept_winning_row"),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = formattedWinNum,
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.testTag("brake_kept_winning_number")
                                                )
                                                Surface(
                                                    color = Color(0xFFEF4444),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.winning_summary_label),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            .testTag("brake_kept_winning_badge")
                                                    )
                                                }
                                            }
                                            Text(
                                                text = fmt(keptWinningAmount),
                                                color = Color(0xFFDC2626),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.testTag("brake_kept_winning_amount")
                                            )
                                        }
                                        HorizontalDivider(color = Color(0xFFEF4444).copy(alpha = 0.4f), thickness = 1.dp)
                                    }
                                }

                                if ((activeBrake <= 0L || !hasValidWinNum) && regularLeftRetainedList.isEmpty()) {
                                    item {
                                        Text(
                                            text = "မရှိပါ",
                                            color = LuckyTextMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    itemsIndexed(regularLeftRetainedList) { idx, (num, _, amt) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                                .padding(vertical = 7.dp, horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(num, color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                            Text(fmt(amt), color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                        }
                                        HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    }
                                }
                            }

                            // Post-Draw State: Single-round summary ledger stacked below Left Column table
                            if (hasValidWinNum) {
                                HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("brake_post_draw_ledger"),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    BrakeLedgerLine("စုစုပေါင်း =", fmt(ledgerSales), LuckyTextPrimary)
                                    BrakeLedgerLine("ကော် =", fmt(ledgerComm), LuckyTextPrimary)
                                    BrakeLedgerLine("နှုတ်ပြီး =", fmt(ledgerNet), LuckyTextPrimary)
                                    BrakeLedgerLine("ပေါက်သီး =", fmt(ledgerPayout), LuckyDanger)
                                    val sign = when {
                                        ledgerProfitLoss > 0L -> "+"
                                        ledgerProfitLoss < 0L -> "-"
                                        else -> ""
                                    }
                                    val plColor = when {
                                        ledgerProfitLoss > 0L -> LuckySuccess
                                        ledgerProfitLoss < 0L -> LuckyDanger
                                        else -> LuckyTextPrimary
                                    }
                                    BrakeLedgerLine(
                                        label = "အမြတ်ငွေ =",
                                        value = "$sign${fmt(abs(ledgerProfitLoss))}",
                                        valueColor = plColor
                                    )
                                }
                            }

                            // Left Column Footer: "စုစုပေါင်း", "+ သိမ်းမည်" (or "သိမ်းရန်" post-draw), and "ရက်ချုပ်"
                            HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyCardDark)
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "စုစုပေါင်း = ${fmt(totalRetained)}",
                                    color = LuckyTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.lockAndSaveBrakeSettlement() },
                                        enabled = hasRound,
                                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("brake_lock_save_settlement_btn")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_save),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.SETTLEMENT) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuckySuccess, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("goto_settlement_btn")
                                    ) {
                                        Text(
                                            text = "ရက်ချုပ်",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(LuckyBorder)
                        )

                        // RIGHT COLUMN: တင်ကွက် + Right Footer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (rightLayoffList.isEmpty()) {
                                    item {
                                        Text(
                                            text = "ဘရိတ်ကျော် မရှိပါ",
                                            color = LuckyTextMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    itemsIndexed(rightLayoffList) { idx, (num, _, amt) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                                .padding(vertical = 7.dp, horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(num, color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                            Text(fmt(amt), color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                        }
                                        HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    }
                                }
                            }

                            // Right Column Footer: "စုစုပေါင်း", "+ တင်မည်", and "မှတ်တမ်းများ"
                            HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyCardDark)
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "စုစုပေါင်း = ${fmt(totalLayoff)}",
                                    color = LuckyDanger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { showConfirmDialog = true },
                                        enabled = hasRound,
                                        colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("open_layoff_dialog_btn")
                                    ) {
                                        Text(
                                            text = "+ တင်မည်",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.LAYOFF_HISTORY) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("goto_layoff_history_btn")
                                    ) {
                                        Text(
                                            text = "မှတ်တမ်းများ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Layoff Selection Dialog ("ဒိုင်သို့ တင်ကွက် ရွေးချယ်ပါ")
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = stringResource(R.string.layoff_confirm_title),
                        color = LuckyTextPrimary,
                        fontSize = 18.sp,
                        lineHeight = 1.5.em,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.layoff_confirm_message, fmt(activeBrake)),
                            color = LuckyTextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 1.5.em
                        )
                        Text(
                            text = stringResource(R.string.layoff_confirm_summary, fmt(totalLayoff), rightLayoffList.size),
                            color = LuckyTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Option 1: အားလုံးတင်မည်
                        Button(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.executeLayoff(isAll = true)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuckyHeader,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("layoff_all_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.action_layoff_all),
                                fontSize = 15.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Option 2: အကွက် ၃၀ တင်မည်
                        Button(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.executeLayoff(isAll = false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuckyDanger,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("layoff_30_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.action_layoff_30),
                                fontSize = 15.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false },
                        modifier = Modifier.testTag("layoff_cancel_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.action_layoff_cancel),
                            color = LuckyTextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun BrakeLedgerLine(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LuckyTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
