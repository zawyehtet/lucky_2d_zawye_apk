@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Agent
import com.example.data.BetItem
import com.example.logic.PatternAlgorithms
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.CloverLogo
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.fmt
import com.example.ui.components.recordLongPress
import com.example.ui.theme.*

@Composable
fun NumberEntryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentAgent by viewModel.entryAgent.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val entryItems by viewModel.entryItems.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val blocked by viewModel.blocked.collectAsState()

    val listState = rememberLazyListState()

    // Keypad state
    var inputNumber by remember { mutableStateOf("") }
    var inputPattern by remember { mutableStateOf("ဒဲ့") }
    var inputAmount by remember { mutableStateOf("") }
    var isReverseOn by remember { mutableStateOf(false) }
    var activeField by remember { mutableStateOf("NUMBER") } // "NUMBER" or "AMOUNT"

    // Dialog states
    var quickEntryMode by remember { mutableStateOf<Int?>(null) } // 1 or 2
    var showKwayDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showAgentPicker by remember { mutableStateOf(false) }

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

    fun onKeypadNumPress(num: String) {
        if (activeField == "NUMBER") {
            if (inputPattern == "ခွေ") {
                if (inputNumber.length < 9) {
                    inputNumber += num
                }
            } else if (inputPattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်")) {
                inputNumber = num.take(1)
            } else {
                if (inputNumber.length < 2) {
                    inputNumber += num
                }
            }
        } else {
            val digitsOnly = num.filter { it.isDigit() }
            if (inputAmount.length < 8) {
                inputAmount = (inputAmount + digitsOnly).take(8)
            }
        }
    }

    fun handleAddBet(withDoublesForKway: Boolean = false) {
        if (sessions.isEmpty() && selectedSession.isEmpty()) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        if (currentAgent == null) {
            viewModel.showToast("ထိုးသူ အရင်ရွေးပါ")
            showAgentPicker = true
            return
        }

        val amount = inputAmount.toLongOrNull() ?: 0L
        if (amount <= 0) {
            viewModel.showToast("ပမာဏ ရိုက်ထည့်ပါ")
            activeField = "AMOUNT"
            return
        }
        if (settings.maxBet > 0 && amount > settings.maxBet) {
            viewModel.showToast("သတ်မှတ်ထားသော အများဆုံးထိုးကြေး (${settings.maxBet}) ထက် ကျော်လွန်နေပါသည်")
            return
        }

        if (inputPattern == "ခွေ") {
            val distinctDigits = inputNumber.filter { it.isDigit() }.toSet()
            if (distinctDigits.size < 2) {
                viewModel.showToast("ခွေရန် အနည်းဆုံး ဂဏန်း ၂ လုံး ရိုက်ပါ")
                return
            }
        } else if (inputPattern in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်")) {
            if (inputNumber.isEmpty()) {
                viewModel.showToast("ဂဏန်း ၁ လုံး ရိုက်ထည့်ပါ")
                return
            }
        } else if (inputPattern == "ဒဲ့") {
            if (inputNumber.length != 2) {
                viewModel.showToast("၂ လုံးဂဏန်း အပြည့်ရိုက်ပါ")
                return
            }
        }

        val generatedNumbers = PatternAlgorithms.generateNumbersForPattern(
            pattern = inputPattern,
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
        }
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
                        modifier = Modifier.weight(1f)
                    ) {
                        CloverLogo(size = 26.dp)
                        Text(
                            text = "နေ့စွဲ : $selectedDate , အကြိမ် : $selectedSession",
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
                            contentDescription = "ပိတ်မည်",
                            tint = Color.White
                        )
                    }
                }
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
                    modifier = Modifier.clickable { showAgentPicker = true }
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
                            text = "ဂဏန်းနှင့် ပမာဏ ရိုက်ထည့်ပါ",
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .recordLongPress(
                                        onLongPress = {
                                            editingItemIndex = index
                                        }
                                    )
                                    .background(if (index % 2 == 0) LuckyCard else LuckyCardDark)
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
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
                            HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Action Summary Bar under the table:
            // "အမြန်ထိုး", "စုစုပေါင်း = N (X ကွက်)", and "ထိုးရန်" button (stays on this screen!)
            var quickMenuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardDark)
                    .border(width = 1.dp, color = LuckyBorder)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Button(
                        onClick = { quickMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("quick_entry_dropdown_btn")
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("အမြန်ထိုး", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = quickMenuExpanded,
                        onDismissRequest = { quickMenuExpanded = false },
                        modifier = Modifier.background(LuckyCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("အမြန်ထိုး ၁", color = LuckyTextPrimary) },
                            onClick = {
                                quickMenuExpanded = false
                                quickEntryMode = 1
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("အမြန်ထိုး ၂", color = LuckyTextPrimary) },
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
                    maxLines = 1
                )

                // "ထိုးရန်" Button (Saves voucher and stays on NumberEntryScreen!)
                Button(
                    onClick = {
                        viewModel.commitCurrentVoucher()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuckySuccess,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("submit_voucher_btn")
                ) {
                    Text("ထိုးရန်", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
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
                        onClick = { activeField = "NUMBER" },
                        modifier = Modifier.weight(1f)
                    )

                    val patternDisplay = if (isReverseOn) "$inputPattern (အာ)" else inputPattern
                    PreviewCell(
                        title = "အမျိုးအစား",
                        value = patternDisplay,
                        isActive = false,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )

                    PreviewCell(
                        title = "ပမာဏ",
                        value = inputAmount,
                        isActive = activeField == "AMOUNT",
                        onClick = { activeField = "AMOUNT" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Horizontally Scrollable Pattern Row
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
                            inputPattern = "ဒဲ့"
                            activeField = "NUMBER"
                        }
                    )

                    patterns.forEach { p ->
                        KeypadSmallButton(
                            text = p,
                            isSelected = inputPattern == p,
                            onClick = {
                                inputPattern = p
                                if (p in listOf("ထိပ်", "နောက်", "အပါ", "ဘရိတ်")) {
                                    activeField = "NUMBER"
                                }
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
                        onClick = { inputPattern = "နက္ခတ်" }
                    )
                    NumKey("1", Modifier.weight(1f)) { onKeypadNumPress("1") }
                    NumKey("2", Modifier.weight(1f)) { onKeypadNumPress("2") }
                    NumKey("3", Modifier.weight(1f)) { onKeypadNumPress("3") }
                    KeypadBtn(
                        text = "R",
                        bgColor = if (isReverseOn) LuckySuccess else LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { isReverseOn = !isReverseOn }
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
                            inputPattern = "ခွေ"
                            activeField = "NUMBER"
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
                            activeField = if (activeField == "NUMBER") "AMOUNT" else "NUMBER"
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
                            inputPattern = "အပါ"
                            activeField = "NUMBER"
                        }
                    )
                    NumKey("7", Modifier.weight(1f)) { onKeypadNumPress("7") }
                    NumKey("8", Modifier.weight(1f)) { onKeypadNumPress("8") }
                    NumKey("9", Modifier.weight(1f)) { onKeypadNumPress("9") }
                    KeypadBtn(
                        text = "ရွှေ့",
                        bgColor = LuckyHeader,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeField = if (activeField == "NUMBER") "AMOUNT" else "NUMBER"
                        }
                    )
                }

                // Row 4: ဖျက် | 0 | 00 | 000 | ထည့်
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KeypadBtn(
                        text = "ဖျက်",
                        bgColor = LuckyDanger,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (activeField == "NUMBER" && inputNumber.isNotEmpty()) {
                                inputNumber = inputNumber.dropLast(1)
                            } else if (activeField == "AMOUNT" && inputAmount.isNotEmpty()) {
                                inputAmount = inputAmount.dropLast(1)
                            } else {
                                viewModel.removeLastEntryItem()
                            }
                        }
                    )
                    NumKey("0", Modifier.weight(1f)) { onKeypadNumPress("0") }
                    NumKey("00", Modifier.weight(1f)) { onKeypadNumPress("00") }
                    NumKey("000", Modifier.weight(1f)) { onKeypadNumPress("000") }
                    KeypadBtn(
                        text = "OK",
                        bgColor = LuckySuccess,
                        textColor = Color.White,
                        isBold = true,
                        modifier = Modifier.weight(1f).testTag("keypad_ok_btn"),
                        onClick = {
                            if (inputPattern == "ခွေ") {
                                showKwayDialog = true
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

        // Edit / Delete Bet Item Dialog on Long-press (550ms)
        editingItemIndex?.let { index ->
            if (index in entryItems.indices) {
                val item = entryItems[index]
                EditBetItemDialog(
                    initialNumber = item.n,
                    initialAmount = item.a,
                    maxBet = settings.maxBet,
                    blocked = blocked,
                    onDismiss = { editingItemIndex = null },
                    onSave = { newNum, newAmt ->
                        viewModel.editEntryItem(index, newNum, newAmt)
                        editingItemIndex = null
                    },
                    onDelete = {
                        viewModel.deleteEntryItem(index)
                        editingItemIndex = null
                    }
                )
            }
        }

        // Quick Entry Dialog (Mode 1 or Mode 2)
        quickEntryMode?.let { mode ->
            QuickEntryDialog(
                mode = mode,
                maxBet = settings.maxBet,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFE3F2FD) else LuckyCardDark)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) LuckyHeader else LuckyBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isBlank()) {
            Text(
                text = title,
                color = LuckyTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = value,
                color = if (isActive) LuckyHeader else LuckyTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(0.5.dp, LuckyBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
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
    maxBet: Long,
    blocked: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
    onDelete: () -> Unit
) {
    var numberStr by remember { mutableStateOf(initialNumber) }
    var amountStr by remember { mutableStateOf(initialAmount.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
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
            title = { Text("ဂဏန်း ပြင်ဆင်ရန် / ဖျက်ရန်", color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = numberStr,
                        onValueChange = {
                            if (it.length <= 2) {
                                numberStr = it.filter { c -> c.isDigit() }
                                errorMsg = null
                            }
                        },
                        label = { Text("ဂဏန်း (၂ လုံး)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        },
                        label = { Text("ပမာဏ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        )
                    )

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = LuckyDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toLongOrNull() ?: 0L
                        if (numberStr.length != 2 || !numberStr.all { it.isDigit() }) {
                            errorMsg = "ဂဏန်း ၂ လုံး ရိုက်ထည့်ပါ"
                            return@Button
                        }
                        if (amt <= 0) {
                            errorMsg = "ပမာဏ မှန်ကန်စွာ ထည့်ပါ"
                            return@Button
                        }
                        if (maxBet > 0 && amt > maxBet) {
                            errorMsg = "သတ်မှတ်ထားသော အများဆုံးထိုးကြေး ($maxBet) ထက် ကျော်လွန်နေပါသည်"
                            return@Button
                        }
                        if (blocked.contains(numberStr)) {
                            errorMsg = "$numberStr သည် မရဂဏန်း ဖြစ်ပါသည်"
                            return@Button
                        }
                        onSave(numberStr, amt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White)
                ) {
                    Text("သိမ်းမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                    ) {
                        Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("မလုပ်တော့ပါ", color = LuckyTextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
fun QuickEntryDialog(
    mode: Int,
    maxBet: Long,
    blocked: Set<String>,
    onDismiss: () -> Unit,
    onAddItems: (List<BetItem>, Int, List<String>) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    val placeholder = if (mode == 1) {
        "ကြေးတူရင် ---- 01.78=500 | ကြေးတူ R ရင် ---- 01.78R500 |\nကြေးမတူရင် ---- 01.78=500/100"
    } else {
        "5a500, 6b600, 1t500, 1w500, p3500, n7500, s500, x200,\n56=2000/1000, 89r350, 79=500"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text("2D ဂဏန်းများ - အမြန်ထိုး $mode", color = LuckyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(placeholder, color = LuckyTextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuckyHeader,
                        unfocusedBorderColor = LuckyBorder,
                        focusedTextColor = LuckyTextPrimary,
                        unfocusedTextColor = LuckyTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("မလုပ်တော့ပါ", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.isNotBlank()) {
                            inputText = if (inputText.isBlank()) clipText else "$inputText\n$clipText"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckySuccess, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ကူးထည့်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val result = if (mode == 1) {
                            PatternAlgorithms.parseQuickEntryMode1(inputText, blocked, maxBet)
                        } else {
                            PatternAlgorithms.parseQuickEntryMode2(inputText, blocked, maxBet)
                        }
                        onAddItems(result.items, result.skippedCount, result.blockedSkipped)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ထည့်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
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
                Text("ပိတ်မည်", color = LuckyTextSecondary)
            }
        }
    )
}
