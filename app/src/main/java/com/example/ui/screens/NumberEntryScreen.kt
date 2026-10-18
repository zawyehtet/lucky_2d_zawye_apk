@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Agent
import com.example.data.BetItem
import com.example.logic.PatternAlgorithms
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NumberEntryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentAgent by viewModel.entryAgent.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val entryItems by viewModel.entryItems.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val blocked by viewModel.blocked.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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

    val numFormat = NumberFormat.getNumberInstance(Locale.US)
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
            if (inputAmount.length < 8) {
                inputAmount += num
            }
        }
    }

    // Logic to add bet items from current keypad
    fun handleAddBet(withDoublesForKway: Boolean = false) {
        if (sessions.isEmpty() || selectedSession.isEmpty()) {
            viewModel.showToast("အကြိမ်ရေ အရင်ထည့်ပါ")
            return
        }
        if (currentAgent == null) {
            viewModel.showToast("အေးဂျင့် အရင်ထည့်ပါ")
            return
        }

        val amount = inputAmount.toLongOrNull() ?: 0L
        if (amount <= 0) {
            viewModel.showToast("ပမာဏ (ငွေ) ရိုက်ထည့်ပါ")
            activeField = "AMOUNT"
            return
        }
        if (settings.maxBet > 0 && amount > settings.maxBet) {
            viewModel.showToast("သတ်မှတ်ထားသော အများဆုံးထိုးကြေး (${settings.maxBet}) ထက် ကျော်လွန်နေပါသည်")
            return
        }

        // Validate pattern input
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
            // Reset input fields
            inputNumber = ""
            inputPattern = "ဒဲ့"
            isReverseOn = false
            activeField = "NUMBER"
        }
    }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header: နေ့စွဲ : date , အပိုင်း : session and X button
            Surface(
                color = LuckyHeader,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "နေ့စွဲ : $selectedDate , အပိုင်း : $selectedSession",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier.size(32.dp).testTag("close_number_entry_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            // Agent Info Line: ထိုးသူ : agent name , ဘောင်ချာများ ကြည့်ရန် နိုပ်ပါ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        text = currentAgent?.name ?: "ရွေးချယ်ပါ (None)",
                        color = LuckyLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "ဘောင်ချာများ ကြည့်ရန် နိုပ်ပါ",
                    color = LuckyTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(Screen.VOUCHERS) }
                        .padding(4.dp)
                )
            }

            // Entered Bets Table (စဉ် | ဂဏန်း | ပမာဏ) - min height ~140dp, auto-scroll to newest row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining upper space
                    .background(LuckyBg)
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("စဉ်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.8f))
                    Text("ဂဏန်း", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text("ပမာဏ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                }

                if (entryItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "အချက်အလက် မရှိသေးပါ။ ဂဏန်းနှင့် ပမာဏ ရိုက်ထည့်ပါ",
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
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            editingItemIndex = index
                                        }
                                    )
                                    .background(if (index % 2 == 0) LuckyCard else LuckyBg)
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = LuckyTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = item.n,
                                    color = LuckyLime,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = numFormat.format(item.a),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // Bar under the table:
            // Dropdown "အမြန်ထိုး ရွေးပါ", "စုစုပေါင်း = N", and "ထိုးရန်" switch
            var quickMenuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardDark)
                    .border(width = 1.dp, color = LuckyBorder)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quick Entry Dropdown
                Box {
                    Button(
                        onClick = { quickMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("quick_entry_dropdown_btn")
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = LuckyLime, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("အမြန်ထိုး", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = quickMenuExpanded,
                        onDismissRequest = { quickMenuExpanded = false },
                        modifier = Modifier.background(LuckyCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("အမြန်ထိုး ၁", color = Color.White) },
                            onClick = {
                                quickMenuExpanded = false
                                quickEntryMode = 1
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("အမြန်ထိုး ၂", color = Color.White) },
                            onClick = {
                                quickMenuExpanded = false
                                quickEntryMode = 2
                            }
                        )
                    }
                }

                // စုစုပေါင်း = N
                Text(
                    text = "စုစုပေါင်း = ${numFormat.format(totalAmount)}",
                    color = LuckyLime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                // "ထိုးရန်" Switch/Button
                Button(
                    onClick = {
                        viewModel.commitCurrentVoucher()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuckyLime,
                        contentColor = LuckyBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("submit_voucher_btn")
                ) {
                    Text("ထိုးရန်", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Compact Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCard)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 3 Preview Cells (ဂဏန်း | ဒဲ့/pattern | ပမာဏ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Number Cell
                    PreviewCell(
                        title = "ဂဏန်း",
                        value = inputNumber,
                        isActive = activeField == "NUMBER",
                        onClick = { activeField = "NUMBER" },
                        modifier = Modifier.weight(1f)
                    )

                    // Pattern Cell
                    val patternDisplay = if (isReverseOn) "$inputPattern (R)" else inputPattern
                    PreviewCell(
                        title = "ဒဲ့/pattern",
                        value = patternDisplay,
                        isActive = false,
                        onClick = {},
                        modifier = Modifier.weight(1.2f)
                    )

                    // Amount Cell
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
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // "ဒဲ့" button to reset pattern
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

                // Keypad Grid: Left Column + Numpad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Left Column: နက္ခတ်, ခွေ, အပါ, ဖျက် (red)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        KeypadBtn(
                            text = "နက္ခတ်",
                            bgColor = if (inputPattern == "နက္ခတ်") LuckyLimeDark else LuckyHeader,
                            textColor = if (inputPattern == "နက္ခတ်") LuckyBg else Color.White,
                            onClick = { inputPattern = "နက္ခတ်" }
                        )
                        KeypadBtn(
                            text = "ခွေ",
                            bgColor = if (inputPattern == "ခွေ") LuckyLimeDark else LuckyHeader,
                            textColor = if (inputPattern == "ခွေ") LuckyBg else Color.White,
                            onClick = {
                                inputPattern = "ခွေ"
                                activeField = "NUMBER"
                            }
                        )
                        KeypadBtn(
                            text = "အပါ",
                            bgColor = if (inputPattern == "အပါ") LuckyLimeDark else LuckyHeader,
                            textColor = if (inputPattern == "အပါ") LuckyBg else Color.White,
                            onClick = {
                                inputPattern = "အပါ"
                                activeField = "NUMBER"
                            }
                        )
                        KeypadBtn(
                            text = "ဖျက်",
                            bgColor = LuckyDanger,
                            textColor = Color.White,
                            onClick = {
                                if (activeField == "NUMBER" && inputNumber.isNotEmpty()) {
                                    inputNumber = inputNumber.dropLast(1)
                                } else if (activeField == "AMOUNT" && inputAmount.isNotEmpty()) {
                                    inputAmount = inputAmount.dropLast(1)
                                } else {
                                    // if both fields are empty, remove last row as per requirement!
                                    viewModel.removeLastEntryItem()
                                }
                            }
                        )
                    }

                    // Numpad 4 columns:
                    // Row 1: 1, 2, 3, R
                    // Row 2: 4, 5, 6, "/"
                    // Row 3: 7, 8, 9, ENTER
                    // Row 4: 0, 00, 000, OK (lime)
                    Column(
                        modifier = Modifier.weight(3.5f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Row 1
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NumKey("1", Modifier.weight(1f)) { onKeypadNumPress("1") }
                            NumKey("2", Modifier.weight(1f)) { onKeypadNumPress("2") }
                            NumKey("3", Modifier.weight(1f)) { onKeypadNumPress("3") }
                            KeypadBtn(
                                text = "R",
                                bgColor = if (isReverseOn) LuckyLime else LuckyKeyBg,
                                textColor = if (isReverseOn) LuckyBg else Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { isReverseOn = !isReverseOn }
                            )
                        }

                        // Row 2
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NumKey("4", Modifier.weight(1f)) { onKeypadNumPress("4") }
                            NumKey("5", Modifier.weight(1f)) { onKeypadNumPress("5") }
                            NumKey("6", Modifier.weight(1f)) { onKeypadNumPress("6") }
                            KeypadBtn(
                                text = "/",
                                bgColor = LuckyKeyBg,
                                textColor = LuckyLime,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeField = if (activeField == "NUMBER") "AMOUNT" else "NUMBER"
                                }
                            )
                        }

                        // Row 3
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NumKey("7", Modifier.weight(1f)) { onKeypadNumPress("7") }
                            NumKey("8", Modifier.weight(1f)) { onKeypadNumPress("8") }
                            NumKey("9", Modifier.weight(1f)) { onKeypadNumPress("9") }
                            KeypadBtn(
                                text = "ENT",
                                bgColor = LuckyKeyBg,
                                textColor = LuckyLime,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeField = if (activeField == "NUMBER") "AMOUNT" else "NUMBER"
                                }
                            )
                        }

                        // Row 4
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NumKey("0", Modifier.weight(1f)) { onKeypadNumPress("0") }
                            NumKey("00", Modifier.weight(1f)) { onKeypadNumPress("00") }
                            NumKey("000", Modifier.weight(1f)) { onKeypadNumPress("000") }
                            KeypadBtn(
                                text = "OK",
                                bgColor = LuckyLime,
                                textColor = LuckyBg,
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
            }
        }

        // Kway Dialog: "အပူး ထည့်မလား?"
        if (showKwayDialog) {
            AlertDialog(
                onDismissRequest = { showKwayDialog = false },
                containerColor = LuckyCard,
                title = { Text("ခွေ ဂဏန်းများ", color = Color.White) },
                text = { Text("အပူး ထည့်မလား?", color = LuckyTextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showKwayDialog = false
                            handleAddBet(withDoublesForKway = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
                    ) {
                        Text("ထည့်မည် (Yes)")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showKwayDialog = false
                            handleAddBet(withDoublesForKway = false)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("မထည့်ပါ (No)")
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
            .background(if (isActive) LuckyLimeDark.copy(alpha = 0.25f) else LuckyCardDark)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) LuckyLime else LuckyBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isBlank()) {
            Text(
                text = title,
                color = LuckyTextMuted,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = value,
                color = if (isActive) LuckyLime else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) LuckyLime else LuckyHeader)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) LuckyBg else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp
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
        textColor = Color.White,
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
            shape = RoundedCornerShape(18.dp),
            title = { Text("ဂဏန်း ပြင်ဆင်ရန် / ဖျက်ရန်", color = Color.White, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = numberStr,
                        onValueChange = {
                            if (it.length <= 2) {
                                numberStr = it
                                errorMsg = null
                            }
                        },
                        label = { Text("ဂဏန်း (၂ လုံး)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            errorMsg = null
                        },
                        label = { Text("ပမာဏ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White)
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
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
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
                        Text("ဖျက်မည်")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = LuckyTextSecondary)
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
        "5a500, 6b600, 1t500, 1w500, p3500, n7500, s500, x200,\n56=2000/1000, 89r350, 79=500\n(a=အပါ, b=ဘရိတ်, t=ထိပ်, w=နောက်, p=ပါဝါ, n=နက္ခတ်, s=အပူး, x=ညီကို)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("2D ဂဏန်းများ ( အမြန်ထိုး $mode )", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        focusedBorderColor = LuckyLime,
                        unfocusedBorderColor = LuckyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
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
                // မလုပ်တော့ပါ (red)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("မလုပ်တော့ပါ", fontSize = 12.sp)
                }

                // PASTE (green)
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.isNotBlank()) {
                            inputText = if (inputText.isBlank()) clipText else "$inputText\n$clipText"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CloverDarkGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("PASTE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // ထည့်မည် (blue / lime)
                Button(
                    onClick = {
                        val result = if (mode == 1) {
                            PatternAlgorithms.parseQuickEntryMode1(inputText, blocked, maxBet)
                        } else {
                            PatternAlgorithms.parseQuickEntryMode2(inputText, blocked, maxBet)
                        }
                        onAddItems(result.items, result.skippedCount, result.blockedSkipped)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg),
                    shape = RoundedCornerShape(10.dp)
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
        title = { Text("ကော်မရှင် ရွေးချယ်ပါ", color = Color.White) },
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
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg),
                        shape = RoundedCornerShape(10.dp)
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
                                color = if (ag.id == selected?.id) LuckyLime else Color.White,
                                fontWeight = if (ag.id == selected?.id) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "ကော် ${ag.rate.toInt()}%",
                                color = LuckyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
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
