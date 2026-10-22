@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Agent
import com.example.data.BetAmountValidationResult
import com.example.data.BetItem
import com.example.data.validateBetAmount
import com.example.logic.PatternAlgorithms
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppOutlinedTextField
import com.example.ui.components.CloverLogo
import com.example.ui.components.CompactFilledButton
import com.example.ui.components.CompactTextButton
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EntryLongPressDropdownMenu
import com.example.ui.components.RoundRequiredHelperBanner
import com.example.ui.components.entryCombinedClickable
import com.example.ui.components.fmt
import com.example.ui.components.soundOnInputFocus
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun NumberEntryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val settled by viewModel.settled.collectAsState()
    val currentAgent by viewModel.entryAgent.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val entryItems by viewModel.entryItems.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val invalidAmountText = stringResource(R.string.error_invalid_bet_amount)

    val availableRounds = remember(sessions, selectedDate, vouchers, wins, settled) {
        viewModel.getAvailableRoundsForDate(selectedDate)
    }
    val hasRound = selectedSession.isNotBlank() && (sessions.any { it.name == selectedSession } || availableRounds.contains(selectedSession))
    val isCommissionSet = currentAgent != null
    val canTriggerEntryMenu = hasRound && isCommissionSet
    val noRoundsText = stringResource(R.string.no_rounds_yet)

    val listState = rememberLazyListState()

    // Three-stage sequential focus flow:
    // Stage 1: "NUMBER" (ဂဏန်း) -> Stage 2: "PATTERN" (အမျိုးအစား, defaults to "ဒဲ့") -> Stage 3: "AMOUNT" (ပမာဏ)
    var inputNumber by rememberSaveable { mutableStateOf("") }
    var inputPattern by rememberSaveable { mutableStateOf("ဒဲ့") }
    var inputAmount by rememberSaveable { mutableStateOf("") }
    var isReverseOn by rememberSaveable { mutableStateOf(false) }
    var activeField by rememberSaveable { mutableStateOf("NUMBER") } // "NUMBER", "PATTERN", or "AMOUNT"
    var shouldOverwriteAmount by rememberSaveable { mutableStateOf(false) }
    var amountValidationError by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Debounced validation (180ms) on typed bet amount
    LaunchedEffect(inputAmount) {
        if (inputAmount.isEmpty()) {
            amountValidationError = false
        } else {
            delay(180)
            amountValidationError = !validateBetAmount(inputAmount).isValid
        }
    }

    // Dialog / Menu states
    var quickEntryMode by rememberSaveable { mutableStateOf<Int?>(null) } // 1 or 2
    var showKwayDialog by rememberSaveable { mutableStateOf(false) }
    var menuItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var deletingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAgentPicker by rememberSaveable { mutableStateOf(false) }

    // Clear long-press menu if commission is unset
    LaunchedEffect(canTriggerEntryMenu) {
        if (!canTriggerEntryMenu) {
            menuItemIndex = null
        }
    }

    // Auto scroll to newest row when entryItems change
    LaunchedEffect(entryItems.size) {
        if (entryItems.isNotEmpty()) {
            listState.animateScrollToItem(entryItems.size - 1)
        }
    }

    BackHandler {
        viewModel.goBack()
    }

    val totalAmount = entryItems.sumOf { it.a }

    fun advanceToAmount() {
        activeField = "AMOUNT"
        shouldOverwriteAmount = inputAmount.isNotEmpty()
    }

    fun onKeypadNumPress(num: String) {
        if (!hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        when (activeField) {
            "NUMBER" -> {
                if (inputPattern == "ခွေ") {
                    if (inputNumber.length < 9) {
                        inputNumber = (inputNumber + num).take(9)
                    }
                } else if (inputPattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်")) {
                    inputNumber = num.take(1)
                    if (inputNumber.length == 1) {
                        activeField = "PATTERN"
                        shouldOverwriteAmount = false
                    }
                } else {
                    if (inputNumber.length < 2) {
                        inputNumber = (inputNumber + num).take(2)
                        if (inputNumber.length == 2) {
                            activeField = "PATTERN"
                            shouldOverwriteAmount = false
                        }
                    }
                }
            }
            "PATTERN" -> {
                if (inputPattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်") && inputNumber.isEmpty()) {
                    inputNumber = num.take(1)
                    advanceToAmount()
                } else if (inputPattern == "ခွေ" && inputNumber.length < 2) {
                    inputNumber = (inputNumber + num).take(9)
                    activeField = "NUMBER"
                } else {
                    if (inputPattern.isEmpty()) {
                        inputPattern = "ဒဲ့"
                    }
                    val digitsOnly = num.filter { it.isDigit() }
                    inputAmount = digitsOnly
                    activeField = "AMOUNT"
                    shouldOverwriteAmount = false
                }
            }
            else -> {
                val digitsOnly = num.filter { it.isDigit() }
                if (shouldOverwriteAmount) {
                    inputAmount = digitsOnly
                    shouldOverwriteAmount = false
                } else {
                    inputAmount += digitsOnly
                }
            }
        }
    }

    fun onPatternSelected(pattern: String) {
        if (!hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        viewModel.onInputFocusOrTap()
        inputPattern = pattern
        if (pattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်") && inputNumber.length > 1) {
            inputNumber = inputNumber.take(1)
        }
        if (activeField == "PATTERN") {
            advanceToAmount()
        } else if (pattern in listOf("ပါဝါ", "ညီအစ်ကို", "အပူး", "စုံစုံ", "မမ", "စုံမ", "မစုံ", "နက္ခတ်")) {
            advanceToAmount()
        } else if (pattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်") && inputNumber.length == 1) {
            advanceToAmount()
        } else if (pattern == "ဒဲ့" && inputNumber.length == 2) {
            advanceToAmount()
        } else {
            activeField = "NUMBER"
            shouldOverwriteAmount = false
        }
    }

    fun onAdvanceFocusPress() {
        viewModel.onInputFocusOrTap()
        when (activeField) {
            "NUMBER" -> {
                activeField = "PATTERN"
                shouldOverwriteAmount = false
            }
            "PATTERN" -> {
                if (inputPattern.isEmpty()) {
                    inputPattern = "ဒဲ့"
                }
                advanceToAmount()
            }
            else -> {
                activeField = "NUMBER"
                shouldOverwriteAmount = false
            }
        }
    }

    fun handleAddBet(withDoublesForKway: Boolean = false) {
        if (!hasRound) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        if (currentAgent == null) {
            viewModel.showToast("ထိုးသူ အရင်ရွေးပါ")
            showAgentPicker = true
            return
        }

        val amountValidation = validateBetAmount(inputAmount)
        if (!amountValidation.isValid) {
            amountValidationError = true
            viewModel.showToast(invalidAmountText)
            activeField = "AMOUNT"
            return
        }
        val amount = MainViewModel.parseAmountOrZero(inputAmount)

        val effectivePattern = inputPattern.ifEmpty { "ဒဲ့" }

        if (effectivePattern == "ခွေ") {
            val distinctDigits = inputNumber.filter { it.isDigit() }.toSet()
            if (distinctDigits.size < 2) {
                viewModel.showToast("ခွေရန် အနည်းဆုံး ဂဏန်း ၂ လုံး ရိုက်ပါ")
                activeField = "NUMBER"
                return
            }
        } else if (effectivePattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်")) {
            if (inputNumber.isEmpty()) {
                viewModel.showToast("ဂဏန်း ၁ လုံး ရိုက်ထည့်ပါ")
                activeField = "NUMBER"
                return
            }
        } else if (effectivePattern == "ဒဲ့") {
            if (inputNumber.length != 2) {
                viewModel.showToast("၂ လုံးဂဏန်း အပြည့်ရိုက်ပါ")
                activeField = "NUMBER"
                return
            }
        }

        val generatedNumbers = PatternAlgorithms.generateNumbersForPattern(
            pattern = effectivePattern,
            inputNumber = inputNumber,
            withReverse = isReverseOn,
            withDoublesForKway = withDoublesForKway
        )

        if (generatedNumbers.isEmpty()) {
            viewModel.showToast("ဂဏန်း မထွက်ရှိပါ")
            return
        }

        val betList = mutableListOf<BetItem>()
        val blockedFound = mutableListOf<String>()

        for (num in generatedNumbers) {
            if (blocked.contains(num)) {
                blockedFound.add(num)
            } else {
                betList.add(BetItem(num, amount))
            }
        }

        if (blockedFound.isNotEmpty()) {
            viewModel.showToast("မရဂဏန်း (${blockedFound.joinToString()}) ကို ချန်လှပ်ခဲ့ပါသည်")
        }

        if (betList.isNotEmpty()) {
            viewModel.addEntryItems(betList)
            inputNumber = ""
            inputPattern = "ဒဲ့"
            isReverseOn = false
            activeField = "NUMBER"
            shouldOverwriteAmount = false
        }
    }

    if (showDatePicker) {
        com.example.ui.components.AppDatePickerModal(
            currentDate = selectedDate,
            onDateSelected = { viewModel.setDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header: နေ့စွဲ : date , အကြိမ် : session and Close button
            Surface(
                color = LuckyHeader,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                    ) {
                        CloverLogo(size = 26.dp)
                        Text(
                            text = "နေ့စွဲ : $selectedDate , ${stringResource(R.string.round_label)} : ${if (hasRound) selectedSession else noRoundsText}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier.size(32.dp).testTag("close_number_entry_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color.White
                        )
                    }
                }
            }

            if (!hasRound) {
                RoundRequiredHelperBanner(
                    visible = true,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Agent Info Line: ထိုးသူ : agent name , ဘောင်ချာများ ကြည့်ရန် နှိပ်ပါ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCard)
                    .border(width = 0.5.dp, color = LuckyBorder)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = hasRound) { showAgentPicker = true }
                ) {
                    Text(
                        text = "ထိုးသူ : ",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = currentAgent?.name ?: "ရွေးချယ်ပါ",
                        color = LuckyHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "ဘောင်ချာများ ကြည့်ရန် နှိပ်ပါ",
                    color = LuckyHeader,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(Screen.VOUCHERS) }
                        .padding(4.dp)
                )
            }

            // Full-Width Entered Bets Table (စဉ် | ဂဏန်း | ပမာဏ)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 140.dp)
                    .background(LuckyCard)
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyCardDark)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("စဉ်", color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.8f), maxLines = 1, softWrap = false)
                    Text("ဂဏန်း", color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                    Text("ပမာဏ", color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, maxLines = 1, softWrap = false)
                }
                HorizontalDivider(color = LuckyBorder)

                if (entryItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasRound) "ဂဏန်းနှင့် ပမာဏ ရိုက်ထည့်ပါ" else stringResource(R.string.add_round_first),
                            color = LuckyTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(entryItems) { index, item ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .entryCombinedClickable(
                                            enabled = canTriggerEntryMenu,
                                            itemDescription = "${item.n} ပမာဏ ${item.a}",
                                            onClick = {},
                                            onShowMenu = {
                                                if (canTriggerEntryMenu) {
                                                    menuItemIndex = index
                                                }
                                            },
                                            onEditAction = {
                                                if (canTriggerEntryMenu) {
                                                    viewModel.onEditActionStarted()
                                                    editingItemIndex = index
                                                }
                                            },
                                            onDeleteAction = {
                                                if (canTriggerEntryMenu) {
                                                    deletingItemIndex = index
                                                }
                                            }
                                        )
                                        .background(if (index % 2 == 0) LuckyCard else LuckyCardDark)
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                        .testTag("bet_entry_row_$index"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        color = LuckyTextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                    Text(
                                        text = item.n,
                                        color = LuckyHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = fmt(item.a),
                                        color = LuckyTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(1.5f),
                                        textAlign = TextAlign.End
                                    )
                                }

                                EntryLongPressDropdownMenu(
                                    expanded = canTriggerEntryMenu && menuItemIndex == index,
                                    onDismiss = { menuItemIndex = null },
                                    onEdit = {
                                        viewModel.onEditActionStarted()
                                        editingItemIndex = index
                                    },
                                    onDelete = {
                                        deletingItemIndex = index
                                    }
                                )
                            }
                            HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Action Summary Bar under the table:
            // "အမြန်ထိုး", "စုစုပေါင်း = N (X ကွက်)", and "ထိုးရန်" button (stays on this screen!)
            var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardDark)
                    .border(width = 1.dp, color = LuckyBorder)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Button(
                        onClick = { quickMenuExpanded = true },
                        enabled = hasRound,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyHeader,
                            contentColor = Color.White,
                            disabledContainerColor = LuckyBorder.copy(alpha = 0.75f),
                            disabledContentColor = LuckyTextMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .defaultMinSize(minWidth = 1.dp, minHeight = 40.dp)
                            .testTag("quick_entry_dropdown_btn")
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_quick_bet), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    DropdownMenu(
                        expanded = quickMenuExpanded,
                        onDismissRequest = { quickMenuExpanded = false },
                        modifier = Modifier.background(LuckyCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quick_bet_mode_1), color = LuckyTextPrimary) },
                            onClick = {
                                quickMenuExpanded = false
                                quickEntryMode = 1
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quick_bet_mode_2), color = LuckyTextPrimary) },
                            onClick = {
                                quickMenuExpanded = false
                                quickEntryMode = 2
                            }
                        )
                    }
                }

                // စုစုပေါင်း = N
                Text(
                    text = "စုစုပေါင်း = ${fmt(totalAmount)} (${entryItems.size} ကွက်)",
                    color = LuckyTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("entry_total_label")
                )

                // Compact "ထိုးရန်" Button (Saves voucher and stays on NumberEntryScreen!)
                CompactFilledButton(
                    text = stringResource(R.string.action_place_bet),
                    onClick = {
                        viewModel.commitCurrentVoucher()
                    },
                    enabled = hasRound && entryItems.isNotEmpty(),
                    containerColor = LuckySuccess,
                    contentColor = Color.White,
                    horizontalPadding = 20.dp,
                    cornerRadius = 10.dp,
                    modifier = Modifier.testTag("submit_voucher_btn")
                )
            }

            // Compact Keypad (pinned to bottom, 5 equal columns, 4dp gap)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCard)
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 3 Preview Cells (ဂဏန်း | အမျိုးအစား | ပမာဏ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PreviewCell(
                        title = "ဂဏန်း",
                        value = inputNumber,
                        isActive = activeField == "NUMBER",
                        onClick = {
                            viewModel.onInputFocusOrTap()
                            activeField = "NUMBER"
                            shouldOverwriteAmount = false
                        },
                        modifier = Modifier.weight(1f).testTag("preview_cell_number")
                    )

                    val effectivePatternDisplay = inputPattern.ifEmpty { "ဒဲ့" }
                    val patternDisplay = when {
                        isReverseOn -> "$effectivePatternDisplay (အာ)"
                        else -> effectivePatternDisplay
                    }
                    PreviewCell(
                        title = "အမျိုးအစား",
                        value = patternDisplay,
                        isActive = activeField == "PATTERN",
                        onClick = {
                            viewModel.onInputFocusOrTap()
                            activeField = "PATTERN"
                            shouldOverwriteAmount = false
                        },
                        modifier = Modifier.weight(1f).testTag("preview_cell_pattern")
                    )

                    PreviewCell(
                        title = "ပမာဏ",
                        value = inputAmount,
                        isActive = activeField == "AMOUNT",
                        isValueSelected = activeField == "AMOUNT" && shouldOverwriteAmount && inputAmount.isNotEmpty(),
                        isError = amountValidationError,
                        onClick = {
                            viewModel.onInputFocusOrTap()
                            activeField = "AMOUNT"
                            shouldOverwriteAmount = inputAmount.isNotEmpty()
                        },
                        modifier = Modifier.weight(1f).testTag("preview_cell_amount")
                    )
                }

                if (amountValidationError) {
                    Text(
                        text = invalidAmountText,
                        color = LuckyDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .testTag("entry_amount_error_text")
                    )
                }

                // Horizontally Scrollable Pattern Row (defaults to "ဒဲ့")
                val patterns = listOf("ပါဝါ", "ညီအစ်ကို", "ထိပ်", "နောက်", "အပူး", "စုံစုံ", "မမ", "စုံမ", "မစုံ", "ဘရိတ်")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeypadSmallButton(
                        text = "ဒဲ့",
                        isSelected = inputPattern == "ဒဲ့",
                        onClick = {
                            onPatternSelected("ဒဲ့")
                        }
                    )

                    patterns.forEach { p ->
                        KeypadSmallButton(
                            text = p,
                            isSelected = inputPattern == p,
                            onClick = {
                                onPatternSelected(p)
                            }
                        )
                    }
                }

                // Row 1: နက္ခတ် | 1 | 2 | 3 | R
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeypadBtn(
                        text = "နက္ခတ်",
                        bgColor = if (inputPattern == "နက္ခတ်") LuckySuccess else LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onPatternSelected("နက္ခတ်") }
                    )
                    NumKey("1", Modifier.weight(1f)) { onKeypadNumPress("1") }
                    NumKey("2", Modifier.weight(1f)) { onKeypadNumPress("2") }
                    NumKey("3", Modifier.weight(1f)) { onKeypadNumPress("3") }
                    KeypadBtn(
                        text = "R",
                        bgColor = if (isReverseOn) LuckySuccess else LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f).testTag("keypad_r_btn"),
                        onClick = {
                            if (!hasRound) {
                                viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
                                return@KeypadBtn
                            }
                            viewModel.onInputFocusOrTap()
                            isReverseOn = !isReverseOn
                            if (inputPattern.isEmpty()) {
                                inputPattern = "ဒဲ့"
                            }
                            if (activeField == "PATTERN") {
                                advanceToAmount()
                            }
                        }
                    )
                }

                // Row 2: ခွေ | 4 | 5 | 6 | /
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeypadBtn(
                        text = "ခွေ",
                        bgColor = if (inputPattern == "ခွေ") LuckySuccess else LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onPatternSelected("ခွေ")
                        }
                    )
                    NumKey("4", Modifier.weight(1f)) { onKeypadNumPress("4") }
                    NumKey("5", Modifier.weight(1f)) { onKeypadNumPress("5") }
                    NumKey("6", Modifier.weight(1f)) { onKeypadNumPress("6") }
                    KeypadBtn(
                        text = "/",
                        bgColor = LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onAdvanceFocusPress()
                        }
                    )
                }

                // Row 3: အပါ | 7 | 8 | 9 | ENTER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeypadBtn(
                        text = "အပါ",
                        bgColor = if (inputPattern == "အပါ") LuckySuccess else LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onPatternSelected("အပါ")
                        }
                    )
                    NumKey("7", Modifier.weight(1f)) { onKeypadNumPress("7") }
                    NumKey("8", Modifier.weight(1f)) { onKeypadNumPress("8") }
                    NumKey("9", Modifier.weight(1f)) { onKeypadNumPress("9") }
                    KeypadBtn(
                        text = stringResource(R.string.keypad_enter),
                        bgColor = LuckyHeader,
                        textColor = Color.White,
                        isBold = true,
                        modifier = Modifier.weight(1f).testTag("keypad_enter_btn"),
                        onClick = {
                            onAdvanceFocusPress()
                        }
                    )
                }

                // Row 4: ဖျက် | 0 | 00 | 000 | OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeypadBtn(
                        text = "ဖျက်",
                        bgColor = LuckyDanger,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f).testTag("keypad_delete_btn"),
                        onClick = {
                            when (activeField) {
                                "NUMBER" -> {
                                    if (inputNumber.isNotEmpty()) {
                                        inputNumber = inputNumber.dropLast(1)
                                    }
                                }
                                "PATTERN" -> {
                                    isReverseOn = false
                                    inputPattern = "ဒဲ့"
                                }
                                "AMOUNT" -> {
                                    shouldOverwriteAmount = false
                                    if (inputAmount.isNotEmpty()) {
                                        inputAmount = inputAmount.dropLast(1)
                                    }
                                }
                            }
                        }
                    )
                    NumKey("0", Modifier.weight(1f)) { onKeypadNumPress("0") }
                    NumKey("00", Modifier.weight(1f)) { onKeypadNumPress("00") }
                    NumKey("000", Modifier.weight(1f)) { onKeypadNumPress("000") }
                    val isOkEnabled = hasRound && !amountValidationError
                    KeypadBtn(
                        text = "OK",
                        bgColor = if (isOkEnabled) LuckySuccess else LuckyTextMuted,
                        textColor = Color.White,
                        isBold = true,
                        enabled = isOkEnabled,
                        modifier = Modifier.weight(1f).testTag("keypad_ok_btn"),
                        onClick = {
                            if (!hasRound) {
                                viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
                            } else if (inputPattern == "ခွေ") {
                                val amtValid = validateBetAmount(inputAmount).isValid
                                if (!amtValid) {
                                    amountValidationError = true
                                    viewModel.showToast(invalidAmountText)
                                    activeField = "AMOUNT"
                                } else {
                                    showKwayDialog = true
                                }
                            } else {
                                handleAddBet(false)
                            }
                        }
                    )
                }
            }
        }

        // Kway Dialog: "အပူး ထည့်မလား?"
        if (showKwayDialog) {
            AlertDialog(
                onDismissRequest = { showKwayDialog = false },
                containerColor = LuckyCard,
                title = { Text("ခွေ ဂဏန်းများ", color = LuckyTextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("အပူး ထည့်မလား?", color = LuckyTextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showKwayDialog = false
                            handleAddBet(withDoublesForKway = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White)
                    ) {
                        Text("ထည့်မည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showKwayDialog = false
                            handleAddBet(withDoublesForKway = false)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = LuckyTextSecondary)
                    ) {
                        Text("မထည့်ပါ")
                    }
                }
            )
        }

        // Edit Bet Item Dialog (opened via "ပြင်မည်" in the long-press menu)
        editingItemIndex?.let { index ->
            if (index in entryItems.indices) {
                val item = entryItems[index]
                EditBetItemDialog(
                    initialNumber = item.n,
                    initialAmount = item.a,
                    blocked = blocked,
                    onDismiss = { editingItemIndex = null },
                    onSave = { newNum, newAmt ->
                        viewModel.editEntryItem(index, newNum, newAmt)
                        editingItemIndex = null
                    },
                    onDelete = null
                )
            }
        }

        // Delete Confirm Dialog (opened via "ဖျက်မည်" in the long-press menu)
        deletingItemIndex?.let { index ->
            if (index in entryItems.indices) {
                val item = entryItems[index]
                DeleteConfirmDialog(
                    itemName = "${item.n} = ${fmt(item.a)}",
                    onDismiss = { deletingItemIndex = null },
                    onConfirm = {
                        viewModel.deleteEntryItem(index)
                        deletingItemIndex = null
                    }
                )
            }
        }

        // Quick Entry Dialog (Mode 1 or Mode 2)
        quickEntryMode?.let { mode ->
            QuickEntryDialog(
                mode = mode,
                blocked = blocked,
                onDismiss = { quickEntryMode = null },
                onAddItems = { parsedItems, skipped, blockedList ->
                    if (blockedList.isNotEmpty()) {
                        viewModel.showToast("မရဂဏန်း (${blockedList.joinToString()}) ကို ချန်လှပ်ခဲ့ပါသည်")
                    }
                    if (skipped > 0) {
                        viewModel.showToast("မှားယွင်းနေသော စာကြောင်း ($skipped) ခုကို ကျော်ခဲ့ပါသည်")
                    }
                    if (parsedItems.isNotEmpty()) {
                        viewModel.addEntryItems(parsedItems)
                        viewModel.showToast("${parsedItems.size} ကြောင်း ထည့်သွင်းပြီးပါပြီ")
                    }
                    quickEntryMode = null
                }
            )
        }

        // Agent Picker Dialog if user taps agent name
        if (showAgentPicker) {
            AgentPickerDialog(
                agents = agents,
                selected = currentAgent,
                onDismiss = { showAgentPicker = false },
                onSelect = {
                    viewModel.setEntryAgent(it)
                    showAgentPicker = false
                },
                onGoToAgents = {
                    showAgentPicker = false
                    viewModel.navigateTo(Screen.AGENTS)
                }
            )
        }
    }
}

@Composable
fun PreviewCell(
    title: String,
    value: String,
    isActive: Boolean,
    isValueSelected: Boolean = false,
    isError: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isError -> LuckyDanger
        isActive -> LuckyHeader
        else -> LuckyBorder
    }
    val bgColor = if (isActive) Color(0xFFF0F7FF) else Color.White
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isActive || isError) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isBlank()) {
            Text(
                text = title,
                color = LuckyLabelDarkGrey,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = value,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = if (isValueSelected) {
                    Modifier
                        .background(Color(0xFFBBDEFB), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
fun KeypadSmallButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) LuckySuccess else LuckyHeader)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun KeypadBtn(
    text: String,
    bgColor: Color,
    textColor: Color,
    isBold: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(0.5.dp, LuckyBorder, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NumKey(
    num: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit
) {
    KeypadBtn(
        text = num,
        bgColor = LuckyKeyBg,
        textColor = LuckyTextPrimary,
        isBold = true,
        modifier = modifier,
        onClick = onPress
    )
}

@Composable
fun EditBetItemDialog(
    initialNumber: String,
    initialAmount: Long,
    blocked: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val savedAmtStr = if (initialAmount > 0L) initialAmount.toString() else ""
    var numberStr by rememberSaveable(initialNumber) { mutableStateOf(initialNumber) }
    var amountStr by rememberSaveable(initialAmount) { mutableStateOf(savedAmtStr) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var debouncedAmountError by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val invalidAmountText = stringResource(R.string.error_invalid_bet_amount)

    LaunchedEffect(amountStr) {
        if (amountStr.isEmpty()) {
            debouncedAmountError = false
        } else {
            delay(180)
            debouncedAmountError = !validateBetAmount(amountStr).isValid
        }
    }

    val isAmountImmediatelyValid = validateBetAmount(amountStr).isValid
    val hasChanges = numberStr.trim() != initialNumber || amountStr.trim() != savedAmtStr
    val showSaveBtn = numberStr.isNotBlank() && amountStr.isNotBlank() && hasChanges
    val isSaveEnabled = !debouncedAmountError && isAmountImmediatelyValid && numberStr.trim().length == 2

    if (showDeleteConfirm && onDelete != null) {
        DeleteConfirmDialog(
            itemName = "$initialNumber = $initialAmount",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = LuckyCard,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "ဂဏန်း ပြင်မည်",
                    color = LuckyTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppOutlinedTextField(
                        value = numberStr,
                        onValueChange = {
                            if (it.length <= 2) {
                                numberStr = it.filter { c -> c.isDigit() }
                                errorMsg = null
                            }
                        },
                        label = { Text(stringResource(R.string.label_number)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_bet_number_input")
                    )

                    AppOutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            errorMsg = null
                        },
                        label = { Text(stringResource(R.string.label_amount)) },
                        isError = debouncedAmountError,
                        supportingText = if (debouncedAmountError) {
                            {
                                Text(
                                    text = invalidAmountText,
                                    color = LuckyDanger,
                                    fontSize = 12.sp,
                                    modifier = Modifier.testTag("edit_bet_amount_error_text")
                                )
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_bet_amount_input")
                    )

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = LuckyDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                AnimatedVisibility(visible = showSaveBtn) {
                    Button(
                        onClick = {
                            if (numberStr.length != 2 || !numberStr.all { it.isDigit() }) {
                                errorMsg = "ဂဏန်း ၂ လုံး ရိုက်ထည့်ပါ"
                                return@Button
                            }
                            val validation = validateBetAmount(amountStr)
                            if (!validation.isValid) {
                                debouncedAmountError = true
                                return@Button
                            }
                            val amt = MainViewModel.parseAmountOrZero(amountStr)
                            if (blocked.contains(numberStr)) {
                                errorMsg = "$numberStr သည် မရဂဏန်း ဖြစ်ပါသည်"
                                return@Button
                            }
                            onSave(numberStr, amt)
                        },
                        enabled = isSaveEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        modifier = Modifier.testTag("edit_bet_save_btn")
                    ) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onDelete != null) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                        ) {
                            Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = LuckyTextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
fun QuickEntryDialog(
    mode: Int,
    blocked: Set<String>,
    onDismiss: () -> Unit,
    onAddItems: (List<BetItem>, Int, List<String>) -> Unit
) {
    val context = LocalContext.current
    var inputText by rememberSaveable(mode) { mutableStateOf("") }
    var debouncedAmountError by rememberSaveable(mode) { mutableStateOf(false) }
    val invalidAmountText = stringResource(R.string.error_invalid_bet_amount)

    val titleText = if (mode == 1) {
        stringResource(R.string.quick_bet_mode_1_title)
    } else {
        stringResource(R.string.quick_bet_mode_2_title)
    }
    val placeholderText = if (mode == 1) {
        stringResource(R.string.quick_bet_mode_1_placeholder)
    } else {
        stringResource(R.string.quick_bet_mode_2_placeholder)
    }

    LaunchedEffect(inputText, mode) {
        if (inputText.isBlank()) {
            debouncedAmountError = false
        } else {
            delay(180)
            val validation = PatternAlgorithms.validateQuickEntryAmounts(inputText, mode)
            debouncedAmountError = !validation.isValid
        }
    }

    val lightBlueBorder = Color(0xFF29B6F6)
    val cancelRedColor = Color(0xFFF44336)
    val pasteGreenColor = Color(0xFF4CAF50)
    val submitBlueColor = Color(0xFF2196F3)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 460.dp)
                .testTag("quick_entry_dialog_card"),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = titleText,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.testTag("quick_entry_dialog_title")
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = placeholderText,
                            color = LuckyLabelDarkGrey.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    },
                    isError = debouncedAmountError,
                    singleLine = false,
                    minLines = 8,
                    maxLines = 14,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        errorTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        errorContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedBorderColor = if (debouncedAmountError) LuckyDanger else lightBlueBorder,
                        unfocusedBorderColor = if (debouncedAmountError) LuckyDanger else lightBlueBorder,
                        errorBorderColor = LuckyDanger
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 230.dp, max = 280.dp)
                        .border(
                            width = 1.5.dp,
                            color = if (debouncedAmountError) LuckyDanger else lightBlueBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .soundOnInputFocus()
                        .testTag("quick_entry_text_input")
                )

                if (debouncedAmountError) {
                    Text(
                        text = invalidAmountText,
                        color = LuckyDanger,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("quick_entry_amount_error_text")
                    )
                }

                // Bottom Action Buttons: 1. "မလုပ်တော့ပါ" (Red #F44336), 2. "PASTE" (Green #4CAF50), 3. "ထည့်မည်" (Blue #2196F3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cancelRedColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("quick_entry_cancel_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                if (pasted.isNotBlank()) {
                                    inputText = if (inputText.isBlank()) pasted else "$inputText\n$pasted"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pasteGreenColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("quick_entry_paste_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.action_paste),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (inputText.isBlank()) return@Button
                            val check = PatternAlgorithms.validateQuickEntryAmounts(inputText, mode)
                            if (!check.isValid) {
                                debouncedAmountError = true
                                return@Button
                            }
                            val result = if (mode == 1) {
                                PatternAlgorithms.parseQuickEntryMode1(inputText, blocked)
                            } else {
                                PatternAlgorithms.parseQuickEntryMode2(inputText, blocked)
                            }
                            onAddItems(result.items, result.skippedCount, result.blockedSkipped)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = submitBlueColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("quick_entry_submit_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.action_add),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentPickerDialog(
    agents: List<Agent>,
    selected: Agent?,
    onDismiss: () -> Unit,
    onSelect: (Agent) -> Unit,
    onGoToAgents: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(14.dp),
        title = { Text("ကော်မရှင် ရွေးချယ်ပါ", color = LuckyTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            if (agents.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ကော်မရှင် မရှိသေးပါ။ ကော်မရှင် အသစ် အရင်ထည့်ပါ",
                        color = LuckyTextMuted,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onGoToAgents,
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("＋ ကော်မရှင် အသစ်ထည့်မည်", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(agents.size) { i ->
                        val ag = agents[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(ag) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ag.id}. ${ag.name}",
                                color = if (ag.id == selected?.id) LuckyHeader else LuckyTextPrimary,
                                fontWeight = if (ag.id == selected?.id) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "ကော် ${ag.rate.toInt()}%",
                                color = LuckyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = LuckyTextSecondary)
            }
        }
    )
}
