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

    val hasRound = sessions.isNotEmpty() && selectedSession.isNotBlank() && sessions.any { it.name == selectedSession }
    val noRoundsText = stringResource(R.string.no_rounds_yet)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedNumberForBreakdown by rememberSaveable { mutableStateOf<String?>(null) }
    var showWinEntryDialog by rememberSaveable { mutableStateOf(false) }

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

    var inlineWinInput by rememberSaveable(currentWinNum, selectedDate, selectedSession) {
        mutableStateOf(currentWinNum)
    }

    val showInlineSaveBtn = hasRound && MainViewModel.shouldShowSaveButton(inlineWinInput, currentWinNum)

    val totalWinningPayout = remember(currentWinNum, vouchers, layoffs, agents, sessions, selectedDate, selectedSession) {
        viewModel.getTotalWinningPayoutForNumber(currentWinNum)
    }

    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers()
    }

    val winningSummaryItem = remember(summaries, formattedWinNum, hasValidWinNum) {
        if (hasValidWinNum) {
            summaries.firstOrNull { it.n == formattedWinNum }
        } else null
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
            title = stringResource(R.string.numbers_screen_title),
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
            RoundRequiredHelperBanner(visible = !hasRound)

            // Winning Number & Total Winning Payout Highlight Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasValidWinNum) Color(0xFFE8F5E9) else LuckyCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (hasValidWinNum) LuckySuccess else LuckyBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = hasRound) {
                        showWinEntryDialog = true
                    }
                    .testTag("winning_highlight_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(if (hasValidWinNum) LuckySuccess else LuckyHeader),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasValidWinNum) {
                                    Text(
                                        text = formattedWinNum,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = stringResource(R.string.label_winning_number),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (hasValidWinNum) "ထွက်ဂဏန်း : $formattedWinNum" else "ထွက်ဂဏန်း မထည့်ရသေးပါ",
                                    color = LuckyTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (hasValidWinNum) {
                                        "ပေါက်ငွေ ပမာဏ : ${fmt(totalWinningPayout)} (နှိပ်၍ 2D စာရင်းချုပ် ကြည့်ရန်)"
                                    } else if (!hasRound) {
                                        stringResource(R.string.add_round_first)
                                    } else {
                                        "ထွက်ဂဏန်း ထည့်သွင်းရန်"
                                    },
                                    color = if (hasValidWinNum) LuckySuccess else LuckyTextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showWinEntryDialog = true },
                            enabled = hasRound,
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
                            Text(stringResource(R.string.label_winning_number), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Inline Winning Number input field & conditional Save button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppOutlinedTextField(
                            value = inlineWinInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(2)
                                inlineWinInput = filtered
                                if (filtered.isEmpty() && currentWinNum.isNotEmpty()) {
                                    viewModel.clearWinningNumberForCurrentSession()
                                }
                            },
                            enabled = hasRound,
                            label = { Text(stringResource(R.string.label_winning_number)) },
                            trailingIcon = {
                                if (inlineWinInput.isNotEmpty() || currentWinNum.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            inlineWinInput = ""
                                            viewModel.clearWinningNumberForCurrentSession()
                                        },
                                        modifier = Modifier.testTag("numbers_clear_winning_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.action_clear),
                                            tint = LuckyTextSecondary
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("numbers_winning_input")
                        )

                        AnimatedVisibility(visible = showInlineSaveBtn) {
                            CompactPrimaryButton(
                                text = stringResource(R.string.action_save),
                                onClick = {
                                    if (inlineWinInput.trim().length == 2) {
                                        viewModel.setWinningNumber(inlineWinInput.trim())
                                    } else {
                                        viewModel.showToast("ဂဏန်း ၂ လုံး အပြည့်ထည့်ပါ")
                                    }
                                },
                                enabled = hasRound,
                                modifier = Modifier.testTag("numbers_save_winning_btn")
                            )
                        }
                    }

                    // Compact "ထွက်ဂဏန်း ကြည့်ရန်" button right under the winning number input
                    AnimatedVisibility(visible = hasValidWinNum) {
                        CompactPrimaryButton(
                            text = stringResource(R.string.action_view_winning_number),
                            onClick = {
                                viewModel.openNumbersFromWinningNumber()
                            },
                            horizontalPadding = 16.dp,
                            cornerRadius = 10.dp,
                            modifier = Modifier.testTag("numbers_view_winning_btn")
                        )
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
                    text = "နေ့စွဲ : $selectedDate (${if (hasRound) selectedSession else noRoundsText})",
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
                label = stringResource(R.string.label_search_number),
                enabled = hasRound
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

                // Flow A Step 4-6: Highlighted Winning Number Row at the very top of the numbers table
                if (hasValidWinNum) {
                    val winRowInteractionSource = remember { MutableInteractionSource() }
                    val isWinRowPressed by winRowInteractionSource.collectIsPressedAsState()
                    val winRowContentDesc = stringResource(R.string.winning_row_content_desc, formattedWinNum)
                    val winEntryCount = winningSummaryItem?.entryCount ?: 0
                    val winNetAmount = winningSummaryItem?.netAmount ?: viewModel.getWinningNumberBetAmountForCurrentSession(formattedWinNum)

                    val containerColor = if (isWinRowPressed) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .background(containerColor)
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
                            text = stringResource(R.string.label_winning_number),
                            color = onContainerColor.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("highlighted_winning_number_label")
                        )

                        Text(
                            text = formattedWinNum,
                            color = onContainerColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("highlighted_winning_number_value")
                        )

                        Text(
                            text = "$winEntryCount ကြိမ်",
                            color = onContainerColor.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1.1f)
                        )

                        Row(
                            modifier = Modifier.weight(1.5f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fmt(winNetAmount),
                                color = onContainerColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = onContainerColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = onContainerColor.copy(alpha = 0.25f), thickness = 1.dp)
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
        var winInput by rememberSaveable(currentWinNum) { mutableStateOf(currentWinNum) }
        val showDialogSave = hasRound && MainViewModel.shouldShowSaveButton(winInput, currentWinNum)
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
                    RoundRequiredHelperBanner(visible = !hasRound)
                    Text(
                        text = "နေ့စွဲ : $selectedDate (${if (hasRound) selectedSession else noRoundsText})",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp
                    )
                    AppOutlinedTextField(
                        value = winInput,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }
                            if (filtered.length <= 2) winInput = filtered
                        },
                        enabled = hasRound,
                        label = { Text(stringResource(R.string.label_winning_number)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                    )
                }
            },
            confirmButton = {
                AnimatedVisibility(visible = showDialogSave) {
                    Button(
                        onClick = {
                            if (winInput.length == 2) {
                                viewModel.setWinningNumber(winInput)
                                showWinEntryDialog = false
                                selectedNumberForBreakdown = winInput
                            }
                        },
                        enabled = hasRound,
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWinEntryDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = LuckyTextSecondary)
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
