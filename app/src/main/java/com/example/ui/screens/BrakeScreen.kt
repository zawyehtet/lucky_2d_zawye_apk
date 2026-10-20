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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppHeader
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.RoundRequiredHelperBanner
import com.example.ui.components.soundOnInputFocus
import com.example.ui.components.fmt
import com.example.ui.theme.*

@Composable
fun BrakeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentBrakeInput by viewModel.currentBrakeInput.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()
    val wins by viewModel.wins.collectAsState()

    val hasRound = sessions.isNotEmpty() && selectedSession.isNotBlank() && sessions.any { it.name == selectedSession }
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
    var lastSavedBrakeText by rememberSaveable(settings.brakeLimit) {
        mutableStateOf(if (settings.brakeLimit > 0L) settings.brakeLimit.toString() else "")
    }

    val showBrakeSaveBtn = hasRound && MainViewModel.shouldShowSaveButton(currentBrakeInput, lastSavedBrakeText)

    val limit = MainViewModel.parseAmountOrZero(currentBrakeInput)
    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers()
    }

    // Amount kept within the configured brake limit (မိမိလက်ခံသော/ဘရိတ်အတွင်း ပေါက်သီးပမာဏ)
    val keptWinningAmount = remember(summaries, formattedWinNum, hasValidWinNum, limit, currentBrakeInput, settings.brakeLimit) {
        if (hasValidWinNum) {
            val winSummary = summaries.firstOrNull { it.n == formattedWinNum }
            val activeBrake = if (currentBrakeInput.isNotBlank()) limit else settings.brakeLimit
            if (winSummary != null) {
                if (activeBrake > 0L) minOf(winSummary.netAmount, activeBrake) else winSummary.netAmount
            } else 0L
        } else 0L
    }
    val winningEntryCount = remember(summaries, formattedWinNum, hasValidWinNum) {
        if (hasValidWinNum) summaries.firstOrNull { it.n == formattedWinNum }?.entryCount ?: 0 else 0
    }

    val leftRetainedList = summaries.map { summary ->
        val retained = minOf(summary.netAmount, limit)
        Triple(summary.n, summary.entryCount, retained)
    }.filter { it.third > 0 }

    val rightLayoffList = summaries.mapNotNull { summary ->
        if (summary.netAmount > limit) {
            Triple(summary.n, summary.entryCount, summary.netAmount - limit)
        } else null
    }

    val totalRetained = leftRetainedList.sumOf { it.third }
    val totalLayoff = rightLayoffList.sumOf { it.third }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "တင်ကွက်များ",
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) },
                onBack = { viewModel.goBack() }
            )

            // Header input "ဘရိတ်", date and session
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundRequiredHelperBanner(visible = !hasRound)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "နေ့စွဲ : $selectedDate",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${stringResource(R.string.round_label)} : ${if (hasRound) selectedSession else noRoundsText}",
                        color = LuckyHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                                lastSavedBrakeText = currentBrakeInput.trim()
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

            // Full-Width Split Table Card
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
                    // Split Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "လက်ခံကွက် (${leftRetainedList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.5f)))
                        Text(
                            text = "တင်ကွက် (${rightLayoffList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Split Subheaders (ဂဏန်း | အကြိမ် | ပမာဏ)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCardDark)
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဂဏန်း", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("အကြိမ်", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("ပမာဏ", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(LuckyBorder))
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဂဏန်း", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("အကြိမ်", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("ပမာဏ", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    HorizontalDivider(color = LuckyBorder)

                    // Split Body Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left List: Retained (လက်ခံကွက်)
                        val regularLeftRetainedList = remember(leftRetainedList, formattedWinNum, hasValidWinNum) {
                            if (hasValidWinNum) {
                                leftRetainedList.filter { it.first != formattedWinNum }
                            } else {
                                leftRetainedList
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // Top Winning Number Row in "လက်ခံကွက်" (Kept Bets) Column
                            if (hasValidWinNum) {
                                item(key = "kept_winning_number_top_row") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFEE2E2))
                                            .border(1.5.dp, Color(0xFFEF4444))
                                            .padding(vertical = 8.dp, horizontal = 8.dp)
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
                                            text = "$winningEntryCount",
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = fmt(keptWinningAmount),
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.testTag("brake_kept_winning_amount")
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFFEF4444).copy(alpha = 0.4f), thickness = 1.dp)
                                }
                            }

                            if (!hasValidWinNum && regularLeftRetainedList.isEmpty()) {
                                item {
                                    Text(
                                        text = "မရှိပါ",
                                        color = LuckyTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                itemsIndexed(regularLeftRetainedList) { idx, (num, cnt, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                            .padding(vertical = 7.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(num, color = LuckyHeader, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                        Text("$cnt", color = LuckyTextMuted, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        Text(fmt(amt), color = LuckySuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                                    }
                                    HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
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

                        // Right List: Layoff
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (rightLayoffList.isEmpty()) {
                                item {
                                    Text(
                                        text = "ဘရိတ်ကျော် မရှိပါ",
                                        color = LuckyTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                itemsIndexed(rightLayoffList) { idx, (num, cnt, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                            .padding(vertical = 7.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(num, color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                        Text("$cnt", color = LuckyTextMuted, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        Text(fmt(amt), color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                                    }
                                    HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // Total Summaries
                    HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCardDark)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "စုစုပေါင်း: ${fmt(totalRetained)}",
                            color = LuckySuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "တင်ရန်: ${fmt(totalLayoff)}",
                            color = LuckyDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Action Buttons aligned cleanly across the full table width
                    HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCard)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.SETTLEMENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckySuccess, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("goto_settlement_btn")
                        ) {
                            Text("ရက်ချုပ်", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = hasRound,
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("open_layoff_dialog_btn")
                        ) {
                            Text("တင်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.LAYOFF_HISTORY) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("goto_layoff_history_btn")
                        ) {
                            Text("မှတ်တမ်း", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Layoff Confirm Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        text = stringResource(R.string.layoff_confirm_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.layoff_confirm_message, fmt(limit)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                        Text(
                            text = stringResource(R.string.layoff_confirm_summary, fmt(totalLayoff), rightLayoffList.size),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.components.CompactTextButton(
                            text = stringResource(R.string.action_layoff_cancel),
                            onClick = { showConfirmDialog = false },
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            horizontalPadding = 16.dp,
                            modifier = Modifier.testTag("layoff_cancel_btn")
                        )

                        com.example.ui.components.CompactFilledButton(
                            text = stringResource(R.string.action_layoff_confirm),
                            onClick = {
                                showConfirmDialog = false
                                viewModel.executeLayoff(isAll = true)
                            },
                            containerColor = LuckyDanger,
                            contentColor = Color.White,
                            horizontalPadding = 16.dp,
                            modifier = Modifier.testTag("layoff_all_btn")
                        )
                    }
                }
            )
        }
    }
}
