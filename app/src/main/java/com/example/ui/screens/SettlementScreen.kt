package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Agent2DSettlementItem
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettlementScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val settledRecords by viewModel.settled.collectAsState()
    val editingRecord by viewModel.editingSettledRecord.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Keep current date's active rounds synced as editable/deletable records while preserving locked records
    LaunchedEffect(vouchers, layoffs, sessions, wins, selectedDate) {
        viewModel.syncDailyRecordsFromCurrentDateRounds()
    }

    val totalProfitLoss = remember(settledRecords) { settledRecords.sumOf { it.profitLoss } }

    var activeMenuRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showClearAllConfirm by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }

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
        // Top App Bar: Back button, Title "ရက်ချုပ်အားလုံး", "Export" button, and right-side button "ရှင်းမည်"
        Surface(
            color = LuckyHeader,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("header_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "နောက်သို့",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.daily_ledger_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        modifier = Modifier.testTag("daily_ledger_header_title")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportDailySettlementsToFile(context, launchShare = true, shareAsCsv = false)
                            showExportDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = LuckyHeader
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("export_settlements_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showClearAllConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyDanger,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("clear_settlements_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.action_clear),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4-Column Table: [ စဉ် | ရက်စွဲ | အပိုင်း | အမြတ်ငွေ ]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
                .testTag("round_breakdown_card"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = LuckyCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header: [ စဉ် | ရက်စွဲ | အပိုင်း | အမြတ်ငွေ ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("daily_round_table_header"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စဉ်",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(0.55f)
                            .testTag("daily_col_serial_header")
                    )
                    Text(
                        text = stringResource(R.string.col_date),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("daily_col_date_header")
                    )
                    Text(
                        text = "အပိုင်း",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1.0f)
                            .testTag("daily_col_round_header")
                    )
                    Text(
                        text = "အမြတ်ငွေ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("daily_col_loss_profit_header")
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("daily_round_table_list")
                ) {
                    if (settledRecords.isEmpty()) {
                        item(key = "empty_state") {
                            EmptyStateView(
                                message = stringResource(R.string.daily_records_empty),
                                modifier = Modifier.testTag("daily_records_empty_state")
                            )
                        }
                    } else {
                        itemsIndexed(settledRecords, key = { _, rec -> rec.id }) { idx, record ->
                            val isProfit = record.profitLoss > 0L
                            val isLoss = record.profitLoss < 0L
                            val rColor = when {
                                isProfit -> LuckySuccess
                                isLoss -> LuckyDanger
                                else -> LuckyTextPrimary
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
                                    .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
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
                                            contentDescription = "${record.date} ${record.session} အမြတ်ငွေ $rProfitText"
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
                                    // Column 1: စဉ်
                                    Text(
                                        text = "${idx + 1}",
                                        color = LuckyTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.55f)
                                    )

                                    // Column 2: ရက်စွဲ
                                    Text(
                                        text = record.date,
                                        color = LuckyTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1.15f)
                                    )

                                    // Column 3: အပိုင်း
                                    Text(
                                        text = record.session,
                                        color = LuckyTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    // Column 4: အမြတ်ငွေ
                                    Text(
                                        text = rProfitText,
                                        color = rColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.1f)
                                    )
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

        // Fixed Bottom Summary Bar showing ONLY: "စုစုပေါင်းစာရင်းချုပ် = [Total Profit/Loss with + or - sign]"
        DailySummaryTotalsCard(
            totalProfitLoss = totalProfitLoss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
        )
    }

    // Export Format & Share Dialog ("ရက်ချုပ်အားလုံး Export")
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = LuckyCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "ရက်ချုပ်အားလုံး Export (မျှဝေရန်)",
                    color = LuckyTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "နေ့စဉ်စာရင်းချုပ် မှတ်တမ်း (${settledRecords.size} ခု) ကို မျှဝေမည့် ပုံစံ ရွေးချယ်ပါ -",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportDailySettlementsToFile(context, launchShare = true, shareAsCsv = false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyHeader,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("export_share_txt_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Text (စာသား) ပုံစံဖြင့် မျှဝေမည်", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportDailySettlementsToFile(context, launchShare = true, shareAsCsv = true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckySuccess,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("export_share_csv_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV ပုံစံဖြင့် မျှဝေမည်", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            ShareHelper.copyToClipboard(
                                context = context,
                                text = viewModel.buildDailySettlementsTextSummary(),
                                label = "ရက်ချုပ်အားလုံး"
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("export_copy_txt_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = LuckyHeader, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clipboard သို့ ကူးယူမည်", color = LuckyHeader, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.action_close), color = LuckyTextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Clear All Confirmation Dialog ("ရှင်းမည်")
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = LuckyCard,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = stringResource(R.string.confirm_delete_title),
                    color = LuckyTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "ရက်ချုပ်အားလုံး စာရင်းများကို ရှင်းမည်လား?",
                    color = LuckyTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactTextButton(
                        text = stringResource(R.string.action_delete_cancel),
                        onClick = { showClearAllConfirm = false }
                    )
                    CompactPrimaryButton(
                        text = stringResource(R.string.action_clear),
                        containerColor = LuckyDanger,
                        contentColor = Color.White,
                        onClick = {
                            showClearAllConfirm = false
                            viewModel.clearSettledLedger()
                        },
                        modifier = Modifier.testTag("confirm_clear_all_settlements_btn")
                    )
                }
            },
            dismissButton = {}
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
 * Bottom Summary Bar on "ရက်ချုပ်အားလုံး" showing ONLY the grand total balance line:
 * "စုစုပေါင်းစာရင်းချုပ် = [Total Profit/Loss with + or - sign]"
 */
@Composable
fun DailySummaryTotalsCard(
    totalSales: Long = 0L,
    totalCommission: Long = 0L,
    totalPayout: Long = 0L,
    totalProfitLoss: Long,
    modifier: Modifier = Modifier
) {
    val isProfit = totalProfitLoss > 0L
    val isLoss = totalProfitLoss < 0L
    val resultColor = when {
        isProfit -> LuckySuccess
        isLoss -> LuckyDanger
        else -> LuckyTextPrimary
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
            .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
            .testTag("daily_summary_totals_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = LuckyCard,
            contentColor = LuckyTextPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("daily_total_profit_loss_row"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "စုစုပေါင်းစာရင်းချုပ် =",
                fontFamily = LuckySansFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LuckyTextPrimary,
                modifier = Modifier.testTag("daily_ledger_total_label")
            )
            Text(
                text = signedTotalText,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = resultColor,
                modifier = Modifier.testTag("daily_total_profit_loss_value")
            )
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
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LuckyTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Formats an individual agent's 2D settlement summary for copy-to-clipboard / share.
 */
fun formatAgent2DSettlementClipboardText(
    date: String,
    session: String,
    winningNumber: String,
    item: Agent2DSettlementItem
): String {
    val winDisplay = winningNumber.ifEmpty { "-" }
    return buildString {
        append("2D စာရင်းချုပ်\n")
        append("ရက်စွဲ : $date ($session)\n")
        append("အမည်= ${item.agent.name}\n")
        append("စုစုပေါင်း ရောင်းကြေး = ${fmt(item.totalBet)}\n")
        append("ကော်မရှင် (${fmt(item.agent.rate)}%) =${fmt(item.commission)}\n")
        append("ကော်နုတ်ပြီး =${fmt(item.netAmount)}\n")
        append("ပေါက်သီး($winDisplay)=${fmt(item.winBetAmount)}\n")
        append("လျော်ကြေး =${fmt(item.payout)}\n")
        append("စုစုပေါင်း= ${fmt(item.profitLoss)}")
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

    val totalWinningBetAmount = remember(agentItems, vouchers, selectedDate, selectedSession, currentWin) {
        val fromAgents = agentItems.sumOf { it.winBetAmount }
        if (fromAgents > 0L || agentItems.isNotEmpty()) {
            fromAgents
        } else {
            viewModel.getWinningNumberBetAmountForCurrentSession(currentWin)
        }
    }

    fun formatAgentSettlementShare(item: Agent2DSettlementItem): String {
        return formatAgent2DSettlementClipboardText(
            date = selectedDate,
            session = selectedSession.ifEmpty { noRoundsText },
            winningNumber = currentWin,
            item = item
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        // Screen renamed from "2D စာရင်းချုပ်" to "ကော်မရှင်တနေ့သာစာရင်း"
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
            // Styled Winning-Number Summary Header Card at the top of "ကော်မရှင်တနေ့သာစာရင်း"
            item {
                WinningNumberSummaryHeaderCard(
                    winningNumber = currentWin,
                    winningBetAmount = totalWinningBetAmount,
                    roundName = if (hasRound) selectedSession else ""
                )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LuckyBorder, RoundedCornerShape(12.dp)),
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
                                        color = LuckyTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
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
                                            ShareHelper.shareText(context, formatAgentSettlementShare(item), "ကော်မရှင်တနေ့သာစာရင်း")
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "မျှဝေမည်", tint = LuckyHeader, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = LuckyBorder)

                            // Structured format required by Section 7:
                            // - စုစုပေါင်းရောင်းကြေး =
                            // - ကော်မရှင် =
                            // - ကော်နုတ်ပြီး =
                            // - ပေါက်သီးထိုးကြေး =
                            // - လျော်ကြေး =
                            // - စုစုပေါင်း = [အမြတ်/အရှုံး +/-]
                            SettlementMetricRow("စုစုပေါင်းရောင်းကြေး =", fmt(item.totalBet), LuckyTextPrimary)
                            SettlementMetricRow("ကော်မရှင် =", fmt(item.commission), LuckyTextPrimary)
                            SettlementMetricRow("ကော်နုတ်ပြီး =", fmt(item.netAmount), LuckyHeader)
                            SettlementMetricRow("ပေါက်သီးထိုးကြေး =", fmt(item.winBetAmount), LuckyDanger)
                            SettlementMetricRow("လျော်ကြေး =", fmt(item.payout), LuckyDanger)

                            HorizontalDivider(color = LuckyBorder, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "စုစုပေါင်း =",
                                    color = LuckyTextPrimary,
                                    fontSize = 14.sp,
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
 * Styled header block at the top of "ကော်မရှင်တနေ့သာစာရင်း" showing the winning-number line in the exact format:
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
