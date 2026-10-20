package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SettledRecord
import com.example.ui.Agent2DSettlementItem
import com.example.ui.MainViewModel
import com.example.ui.RoundSettlementSummary
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.abs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettlementScreen(viewModel: MainViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val settledRecords by viewModel.settled.collectAsState()
    val editingRecord by viewModel.editingSettledRecord.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Keep current date's active rounds synced as editable/deletable records
    LaunchedEffect(vouchers, layoffs, sessions, wins, selectedDate) {
        viewModel.syncDailyRecordsFromCurrentDateRounds()
    }

    val totalSales = remember(settledRecords) { settledRecords.sumOf { it.totalRetained } }
    val totalPayout = remember(settledRecords) { settledRecords.sumOf { it.payout } }
    val totalProfitLoss = remember(settledRecords) { settledRecords.sumOf { it.profitLoss } }

    var activeMenuRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val recordToDelete = remember(settledRecords, recordToDeleteId) {
        settledRecords.firstOrNull { it.id == recordToDeleteId }
    }

    val editActionLabel = stringResource(R.string.action_edit)
    val deleteActionLabel = stringResource(R.string.action_delete)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
            .testTag("daily_summary_screen")
    ) {
        AppHeader(
            title = stringResource(R.string.daily_ledger_title),
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() },
            showSessionPill = false
        )

        // 1. "အကြိမ်ရေအလိုက် အမြတ်ရှုံး တွက်ချက်မှု" / 3-column Daily Ledger Table (scrollable area)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .testTag("round_breakdown_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LuckyCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.daily_round_table_title),
                    color = LuckyHeader,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("daily_round_table_title")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("daily_round_table_list")
                ) {
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuckyCardDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("daily_round_table_header"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.col_date),
                                color = LuckyTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("daily_col_date_header")
                            )
                            Text(
                                text = stringResource(R.string.col_round),
                                color = LuckyTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("daily_col_round_header")
                            )
                            Text(
                                text = stringResource(R.string.col_loss_profit),
                                color = LuckyTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("daily_col_loss_profit_header")
                            )
                        }
                    }

                    if (settledRecords.isEmpty()) {
                        item(key = "empty_state") {
                            EmptyStateView(
                                message = stringResource(R.string.daily_records_empty),
                                modifier = Modifier.testTag("daily_records_empty_state")
                            )
                        }
                    } else {
                        items(settledRecords, key = { it.id }) { record ->
                            val isProfit = record.profitLoss > 0L
                            val isLoss = record.profitLoss < 0L
                            val rColor = when {
                                isProfit -> LuckySuccess
                                isLoss -> LuckyDanger
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            val rProfitText = when {
                                isProfit -> "+${fmtThousands(record.profitLoss)}"
                                isLoss -> "-${fmtThousands(abs(record.profitLoss))}"
                                else -> "0"
                            }
                            val isMenuOpen = activeMenuRecordId == record.id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_record_row_${record.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .combinedClickable(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                activeMenuRecordId = record.id
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                activeMenuRecordId = record.id
                                            }
                                        )
                                        .semantics {
                                            contentDescription = "${record.date} ${record.session} ပေါက်သီး ${record.winningNumber.ifEmpty { "-" }} အရှုံး/မြတ် $rProfitText"
                                            customActions = listOf(
                                                CustomAccessibilityAction(label = editActionLabel) {
                                                    viewModel.startEditingSettledRecord(record)
                                                    true
                                                },
                                                CustomAccessibilityAction(label = deleteActionLabel) {
                                                    recordToDeleteId = record.id
                                                    true
                                                }
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Column 1: ရက်စွဲ (Date)
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        Text(
                                            text = record.date,
                                            color = LuckyTextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "ရောင်း ${fmtThousands(record.totalRetained)}",
                                            color = LuckyTextMuted,
                                            fontSize = 11.sp,
                                            lineHeight = 17.sp
                                        )
                                    }

                                    // Column 2: အကြိမ် (Round / Session + Winning Number)
                                    Column(
                                        modifier = Modifier.weight(1.1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = record.session,
                                            color = LuckyTextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "ပေါက်သီး: ${record.winningNumber.ifEmpty { "-" }}",
                                            color = if (record.winningNumber.isNotEmpty()) LuckyDanger else LuckyTextMuted,
                                            fontSize = 11.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Column 3: အရှုံး/မြတ် (Loss / Profit)
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = rProfitText,
                                            color = rColor,
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.End
                                        )
                                        if (record.profitLoss != 0L) {
                                            Text(
                                                text = if (isProfit) stringResource(R.string.label_profit) else stringResource(R.string.label_loss),
                                                color = rColor,
                                                fontSize = 11.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }

                                EntryLongPressDropdownMenu(
                                    expanded = isMenuOpen,
                                    onDismiss = { activeMenuRecordId = null },
                                    title = "${record.session} (${record.date})",
                                    onEdit = {
                                        activeMenuRecordId = null
                                        viewModel.startEditingSettledRecord(record)
                                    },
                                    onDelete = {
                                        activeMenuRecordId = null
                                        recordToDeleteId = record.id
                                    }
                                )
                            }
                            HorizontalDivider(color = LuckyBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // 2. Fixed Bottom Totals Card (outside the scrolling list)
        DailySummaryTotalsCard(
            totalSales = totalSales,
            totalPayout = totalPayout,
            totalProfitLoss = totalProfitLoss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
        )
    }

    // Compact Confirm Delete Dialog ("ဖျက်မည်လား?" with "မဖျက်ပါ" and "ဖျက်မည်")
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDeleteId = null },
            containerColor = LuckyCard,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = stringResource(R.string.confirm_delete_title),
                    color = LuckyTextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${recordToDelete.session} (${recordToDelete.date})",
                    color = LuckyTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTextButton(
                        text = stringResource(R.string.action_delete_cancel),
                        onClick = { recordToDeleteId = null },
                        modifier = Modifier.testTag("cancel_delete_daily_record_btn")
                    )
                    CompactPrimaryButton(
                        text = stringResource(R.string.action_delete),
                        containerColor = LuckyDanger,
                        contentColor = Color.White,
                        onClick = {
                            val targetId = recordToDelete.id
                            recordToDeleteId = null
                            viewModel.deleteSettledRecord(targetId)
                        },
                        modifier = Modifier.testTag("confirm_delete_daily_record_btn")
                    )
                }
            },
            dismissButton = {}
        )
    }

    // Edit Daily Record Dialog (prefilled with saved record data; updates same id in place)
    if (editingRecord != null) {
        val rec = editingRecord!!
        var editDate by rememberSaveable(rec.id) { mutableStateOf(rec.date) }
        var editSession by rememberSaveable(rec.id) { mutableStateOf(rec.session) }
        var editWinNum by rememberSaveable(rec.id) { mutableStateOf(rec.winningNumber) }
        var editSales by rememberSaveable(rec.id) { mutableStateOf(rec.totalRetained.toString()) }
        var editComm by rememberSaveable(rec.id) { mutableStateOf(rec.commission.toString()) }
        var editPayout by rememberSaveable(rec.id) { mutableStateOf(rec.payout.toString()) }

        AlertDialog(
            onDismissRequest = { viewModel.cancelEditingSettledRecord() },
            containerColor = LuckyCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = stringResource(R.string.editing_daily_record_title),
                    color = LuckyTextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppOutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text(stringResource(R.string.col_date)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_daily_record_date_input")
                    )
                    AppOutlinedTextField(
                        value = editSession,
                        onValueChange = { editSession = it },
                        label = { Text(stringResource(R.string.round_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_daily_record_session_input")
                    )
                    AppOutlinedTextField(
                        value = editWinNum,
                        onValueChange = { editWinNum = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text(stringResource(R.string.label_winning_number)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_daily_record_win_input")
                    )
                    AppOutlinedTextField(
                        value = editSales,
                        onValueChange = { editSales = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.col_sales)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_daily_record_sales_input")
                    )
                    AppOutlinedTextField(
                        value = editComm,
                        onValueChange = { editComm = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.col_commission)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_daily_record_comm_input")
                    )
                    AppOutlinedTextField(
                        value = editPayout,
                        onValueChange = { editPayout = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.col_payout)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_daily_record_payout_input")
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTextButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { viewModel.cancelEditingSettledRecord() },
                        modifier = Modifier.testTag("cancel_edit_daily_record_btn")
                    )
                    CompactPrimaryButton(
                        text = stringResource(R.string.action_save),
                        onClick = {
                            viewModel.saveEditedSettledRecord(
                                date = editDate.trim().ifEmpty { rec.date },
                                session = editSession.trim().ifEmpty { rec.session },
                                winningNumber = editWinNum.trim(),
                                totalRetained = MainViewModel.parseAmountOrZero(editSales),
                                commission = MainViewModel.parseAmountOrZero(editComm),
                                payout = MainViewModel.parseAmountOrZero(editPayout)
                            )
                        },
                        modifier = Modifier.testTag("save_edit_daily_record_btn")
                    )
                }
            },
            dismissButton = {}
        )
    }
}

/**
 * Persistent bottom summary Card outside the scrolling list on the Daily Ledger ("စာရင်းချုပ်") screen:
 * - ရောင်းရငွေ = {total sales}
 * - လျော်ငွေ = {total payout}
 * - အရှုံးမြတ် စုစုပေါင်း = {profit or loss} (green with "+" for profit, red with "-" for loss, normal for 0)
 */
@Composable
fun DailySummaryTotalsCard(
    totalSales: Long,
    totalPayout: Long,
    totalProfitLoss: Long,
    modifier: Modifier = Modifier
) {
    val isProfit = totalProfitLoss > 0L
    val isLoss = totalProfitLoss < 0L
    val resultColor = when {
        isProfit -> LuckySuccess
        isLoss -> LuckyDanger
        else -> MaterialTheme.colorScheme.onSurface
    }
    val resultText = remember(totalProfitLoss) {
        MainViewModel.formatDailyTotalResultText(totalProfitLoss)
    }
    val signedTotalText = remember(totalProfitLoss) {
        when {
            totalProfitLoss > 0L -> "+${fmtThousands(totalProfitLoss)}"
            totalProfitLoss < 0L -> "-${fmtThousands(abs(totalProfitLoss))}"
            else -> "0"
        }
    }

    Card(
        modifier = modifier
            .border(1.dp, LuckyBorder, RoundedCornerShape(14.dp))
            .testTag("daily_summary_totals_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.daily_summary_totals_title),
                fontFamily = LuckySansFontFamily,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                color = LuckyHeader,
                modifier = Modifier.testTag("daily_summary_totals_title")
            )

            // Line 1: ရောင်းရငွေ = {total sales}
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_total_sales_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.daily_total_sales_label),
                    fontFamily = LuckySansFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = fmtThousands(totalSales),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("daily_total_sales_value")
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                thickness = 0.5.dp
            )

            // Line 2: လျော်ငွေ = {total payout}
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_total_payout_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.daily_total_payout_label),
                    fontFamily = LuckySansFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = fmtThousands(totalPayout),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("daily_total_payout_value")
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                thickness = 0.5.dp
            )

            // Line 3: အရှုံးမြတ် စုစုပေါင်း (and အမြတ်/အရှုံး result)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_total_profit_loss_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.daily_ledger_total_label),
                        fontFamily = LuckySansFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("daily_ledger_total_label")
                    )
                    Text(
                        text = stringResource(R.string.daily_total_profit_loss_label),
                        fontFamily = LuckySansFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = resultText,
                        fontFamily = LuckySansFontFamily,
                        fontSize = 18.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = resultColor,
                        modifier = Modifier.testTag("daily_total_profit_loss_value")
                    )
                    if (totalProfitLoss != 0L) {
                        Text(
                            text = signedTotalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = resultColor,
                            modifier = Modifier.testTag("daily_ledger_signed_total_value")
                        )
                    }
                }
            }
        }
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
    val activeWinFromFlowA by viewModel.activeWinningNumberFor2DSummary.collectAsState()

    val hasRound = sessions.isNotEmpty() && selectedSession.isNotBlank() && sessions.any { it.name == selectedSession }
    val noRoundsText = stringResource(R.string.no_rounds_yet)

    val currentWin = remember(wins, selectedDate, selectedSession, hasRound, activeWinFromFlowA) {
        val sessionWin = viewModel.getWinningNumberForCurrentSession()
        sessionWin.ifEmpty { activeWinFromFlowA.orEmpty() }
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
    val totalWinningBetAmount = remember(agentItems, vouchers, selectedDate, selectedSession, currentWin) {
        val fromAgents = agentItems.sumOf { it.winBetAmount }
        if (fromAgents > 0L || agentItems.isNotEmpty()) {
            fromAgents
        } else {
            viewModel.getWinningNumberBetAmountForCurrentSession(currentWin)
        }
    }

    fun formatAgentSettlementShare(item: Agent2DSettlementItem): String {
        val isProfit = item.profitLoss >= 0
        val sign = if (isProfit) "+" else "-"
        val status = if (isProfit) "အမြတ်" else "အရှုံး"
        return buildString {
            append("================================\n")
            append("   2D စာရင်းချုပ် (${item.agent.name})\n")
            append("================================\n")
            append("ရက်စွဲ : $selectedDate (${selectedSession.ifEmpty { noRoundsText }})\n")
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
            title = stringResource(R.string.winning_summary_title_2d),
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
            // Styled Winning-Number Summary Header Card at the very top of "2D စာရင်းချုပ်"
            item {
                WinningNumberSummaryHeaderCard(
                    winningNumber = currentWin,
                    winningBetAmount = totalWinningBetAmount,
                    roundName = if (hasRound) selectedSession else ""
                )
            }

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
                        RoundRequiredHelperBanner(visible = !hasRound)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedDate • ${if (hasRound) selectedSession else noRoundsText}",
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

/**
 * Styled header block at the top of "2D စာရင်းချုပ်" showing the winning-number line in the exact format:
 * ပေါက်သီး ဂဏန်း=ပမာဏ (e.g. ပေါက်သီး 47=15,000)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WinningNumberSummaryHeaderCard(
    winningNumber: String?,
    winningBetAmount: Long,
    roundName: String = "",
    modifier: Modifier = Modifier
) {
    val isInspection = LocalInspectionMode.current
    val visibilityState = remember {
        MutableTransitionState(initialState = isInspection).apply { targetState = true }
    }

    val noWinnerSymbol = stringResource(R.string.winning_summary_no_winner)
    val displayNumber = remember(winningNumber, noWinnerSymbol) {
        val formatted = formatWinningNumberTwoDigits(winningNumber)
        if (formatted == "-") noWinnerSymbol else formatted
    }
    val safeAmount = remember(displayNumber, noWinnerSymbol, winningBetAmount) {
        if (displayNumber == noWinnerSymbol) 0L else winningBetAmount.coerceAtLeast(0L)
    }
    val formattedAmountThousands = remember(safeAmount) { fmtThousands(safeAmount) }
    val formattedAmountPlain = remember(safeAmount) { fmt(safeAmount) }

    val fullFormattedWithCommas = stringResource(
        R.string.winning_summary_header_format,
        displayNumber,
        formattedAmountThousands
    )
    val fullFormattedPlain = stringResource(
        R.string.winning_summary_header_format,
        displayNumber,
        formattedAmountPlain
    )

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 200),
                initialOffsetY = { -it / 4 }
            )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "$fullFormattedWithCommas ($fullFormattedPlain)"
                }
                .testTag("winning_summary_header_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val fontScale = LocalDensity.current.fontScale
                val totalChars = displayNumber.length + formattedAmountThousands.length
                val shrinkFactor = remember(maxWidth, fontScale, totalChars) {
                    val widthFactor = if (maxWidth < 340.dp) 0.88f else 1.0f
                    val scaleFactor = if (fontScale > 1.15f) (1.15f / fontScale).coerceIn(0.82f, 1.0f) else 1.0f
                    val lengthFactor = when {
                        totalChars >= 12 -> 0.82f
                        totalChars >= 9 -> 0.90f
                        else -> 1.0f
                    }
                    (widthFactor * scaleFactor * lengthFactor).coerceIn(0.75f, 1.0f)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatedContent(
                        targetState = Pair(displayNumber, formattedAmountThousands),
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(200)) +
                                slideInVertically(animationSpec = tween(200)) { it / 5 })
                                .togetherWith(fadeOut(animationSpec = tween(150)))
                        },
                        label = "WinningHeaderLineTransition"
                    ) { (numStr, amtStr) ->
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("winning_summary_header_row"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.winning_summary_label),
                                fontFamily = LuckySansFontFamily,
                                fontSize = (14f * shrinkFactor).sp,
                                lineHeight = (22f * shrinkFactor).sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .testTag("winning_summary_label_text")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = numStr,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (32f * shrinkFactor).sp,
                                    lineHeight = (48f * shrinkFactor).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.testTag("winning_summary_number_text")
                                )

                                Text(
                                    text = stringResource(R.string.winning_summary_equals),
                                    fontFamily = LuckySansFontFamily,
                                    fontSize = (24f * shrinkFactor).sp,
                                    lineHeight = (36f * shrinkFactor).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier.testTag("winning_summary_equals_text")
                                )
                            }

                            Text(
                                text = amtStr,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (28f * shrinkFactor).sp,
                                lineHeight = (42f * shrinkFactor).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .testTag("winning_summary_amount_text")
                            )
                        }
                    }

                    if (roundName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.winning_summary_round_caption, roundName),
                            fontFamily = LuckySansFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("winning_summary_round_caption_text")
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "2D Summary Header - Light Mode",
    showBackground = true,
    widthDp = 360
)
@Composable
private fun WinningNumberSummaryHeaderLightPreview() {
    MyApplicationTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WinningNumberSummaryHeaderCard(
                winningNumber = "47",
                winningBetAmount = 15000L,
                roundName = "မနက်ပိုင်း"
            )
            WinningNumberSummaryHeaderCard(
                winningNumber = "07",
                winningBetAmount = 5000L,
                roundName = "ညနေပိုင်း"
            )
            WinningNumberSummaryHeaderCard(
                winningNumber = "",
                winningBetAmount = 0L,
                roundName = ""
            )
        }
    }
}

@Preview(
    name = "2D Summary Header - 360dp 1.3x Font Scale",
    showBackground = true,
    widthDp = 360,
    fontScale = 1.3f
)
@Composable
private fun WinningNumberSummaryHeaderLargeFontPreview() {
    MyApplicationTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            WinningNumberSummaryHeaderCard(
                winningNumber = "47",
                winningBetAmount = 15000L,
                roundName = "မနက်ပိုင်း"
            )
        }
    }
}
