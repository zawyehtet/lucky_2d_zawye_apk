package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Session
import com.example.data.WinRecord
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val settled by viewModel.settled.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentBrakeInput by viewModel.currentBrakeInput.collectAsState()

    var activeDialog by remember { mutableStateOf<String?>(null) }

    // Winning Number state
    var winInput by remember { mutableStateOf("") }

    // Session dialog state
    var newSessionName by remember { mutableStateOf("") }
    var newSessionRate by remember { mutableStateOf("15") }
    var newSessionMult by remember { mutableStateOf("80") }

    // Blocked numbers dialog state
    var blockedInput by remember { mutableStateOf("") }

    // Max bet & Brake limit dialog state
    var maxBetInput by remember(settings.maxBet) { mutableStateOf(settings.maxBet.toString()) }
    var brakeLimitInput by remember(currentBrakeInput) { mutableStateOf(currentBrakeInput) }

    // Password change dialog state
    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }

    val currentWin = remember(wins, selectedDate, selectedSession) {
        viewModel.getWinningNumberForCurrentSession()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "ဆက်တင်များ",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "ပင်မ သတ်မှတ်ချက်များ",
                    color = LuckyHeader,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 1. Date & Session Selector
            item {
                SettingsRowCard(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = LuckyHeader,
                    title = "အလုပ်လုပ်မည့် ရက်စွဲနှင့် အချိန်",
                    subtitle = "$selectedDate • $selectedSession",
                    testTag = "setting_date_session",
                    onClick = { activeDialog = "DATE_SESSION" }
                )
            }

            // 2. Winning Numbers (ထွက်ဂဏန်းများ / ပေါက်သီး)
            item {
                SettingsRowCard(
                    icon = Icons.Default.EmojiEvents,
                    iconColor = LuckyWarning,
                    title = "ထွက်ဂဏန်းများ (ပေါက်သီး)",
                    subtitle = if (currentWin.isNotEmpty()) "လက်ရှိပေါက်သီး: $currentWin" else "ပေါက်သီး ထည့်သွင်းရန်နှင့် မှတ်တမ်းကြည့်ရန်",
                    testTag = "setting_winning_numbers",
                    onClick = {
                        winInput = currentWin
                        activeDialog = "WINNING_NUMBERS"
                    }
                )
            }

            // 3. Sessions (အကြိမ်ရေ / ပွဲစဉ်များ)
            item {
                SettingsRowCard(
                    icon = Icons.Default.AccessTime,
                    iconColor = LuckyHeader,
                    title = "အကြိမ်ရေ (အချိန်) များ စီမံရန်",
                    subtitle = sessions.joinToString(", ") { it.name }.ifEmpty { "12.AM, 4.PM" },
                    testTag = "setting_sessions",
                    onClick = { activeDialog = "SESSIONS" }
                )
            }

            // 4. Blocked Numbers (မရဂဏန်းများ)
            item {
                SettingsRowCard(
                    icon = Icons.Default.Block,
                    iconColor = LuckyDanger,
                    title = "မရဂဏန်းများ သတ်မှတ်ရန်",
                    subtitle = if (blocked.isEmpty()) "ပိတ်ထားသော ဂဏန်း မရှိသေးပါ" else "ပိတ်ထားသော ဂဏန်း (${blocked.size}) လုံး",
                    testTag = "setting_blocked_numbers",
                    onClick = { activeDialog = "BLOCKED" }
                )
            }

            // 5. Max Bet & Brake Limit
            item {
                SettingsRowCard(
                    icon = Icons.Default.Speed,
                    iconColor = LuckySuccess,
                    title = "အများဆုံးထိုးကြေး နှင့် ဘရိတ် သတ်မှတ်ချက်",
                    subtitle = "အများဆုံး: ${fmt(settings.maxBet)} | ဘရိတ်: ${fmt(settings.brakeLimit)}",
                    testTag = "setting_limits",
                    onClick = {
                        maxBetInput = settings.maxBet.toString()
                        brakeLimitInput = settings.brakeLimit.toString()
                        activeDialog = "LIMITS"
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "စာရင်းချုပ်နှင့် လုံခြုံရေး",
                    color = LuckyHeader,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 6. Ledger Records (စာရင်းချုပ် မှတ်တမ်းများ)
            item {
                SettingsRowCard(
                    icon = Icons.Default.Assessment,
                    iconColor = LuckyHeader,
                    title = "စာရင်းချုပ် မှတ်တမ်းများ (${settled.size})",
                    subtitle = "အကြိမ်ရေအလိုက် အမြတ်/အရှုံး နှင့် မှတ်တမ်းများ",
                    testTag = "setting_ledger_records",
                    onClick = { activeDialog = "LEDGER_RECORDS" }
                )
            }

            // 7. Per-agent 2D Settlement (2D စာရင်းချုပ်)
            item {
                SettingsRowCard(
                    icon = Icons.Default.Calculate,
                    iconColor = LuckyHeader,
                    title = "ကော်မရှင်အလိုက် 2D စာရင်းချုပ်",
                    subtitle = "ထိုးသူတစ်ဦးချင်းစီအလိုက် အမြတ်/အရှုံး စာရင်း",
                    testTag = "setting_agent_settlement",
                    onClick = { viewModel.navigateTo(Screen.AGENT_SETTLEMENT) }
                )
            }

            // 8. Change Password
            item {
                SettingsRowCard(
                    icon = Icons.Default.Lock,
                    iconColor = LuckyWarning,
                    title = "စကားဝှက် ပြောင်းလဲရန်",
                    subtitle = "လုံခြုံရေး စကားဝှက် အသစ် သတ်မှတ်ရန်",
                    testTag = "setting_password",
                    onClick = {
                        currentPassInput = ""
                        newPassInput = ""
                        activeDialog = "PASSWORD"
                    }
                )
            }

            // 9. Reset All Data
            item {
                SettingsRowCard(
                    icon = Icons.Default.DeleteForever,
                    iconColor = LuckyDanger,
                    title = "အချက်အလက်အားလုံး ရှင်းလင်းရန်",
                    subtitle = "ဘောင်ချာနှင့် မှတ်တမ်းအားလုံး အသစ်ပြန်စရန်",
                    testTag = "setting_reset_all",
                    onClick = { activeDialog = "RESET_ALL" }
                )
            }
        }
    }

    // Dialog 1: Date & Session
    if (activeDialog == "DATE_SESSION") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ရက်စွဲနှင့် အချိန် ရွေးချယ်ရန်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                    viewModel.setDate(formatted)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = LuckyHeader)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ရက်စွဲ: $selectedDate", color = LuckyTextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Text("အချိန် (ပွဲစဉ်) ရွေးချယ်ပါ", color = LuckyTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    sessions.forEach { s ->
                        val isSelected = s.name == selectedSession
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) LuckyHeader.copy(alpha = 0.12f) else LuckyBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) LuckyHeader else LuckyBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setSession(s.name) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.name,
                                color = if (isSelected) LuckyHeader else LuckyTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LuckyHeader, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { activeDialog = null },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ပိတ်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 2: Winning Numbers (ထွက်ဂဏန်းများ)
    if (activeDialog == "WINNING_NUMBERS") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ထွက်ဂဏန်း (ပေါက်သီး) သတ်မှတ်ရန်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$selectedDate • $selectedSession",
                        color = LuckyHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = winInput,
                        onValueChange = { winInput = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("ပေါက်သီး ဂဏန်း (၀၀ မှ ၉၉)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = LuckyDanger,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_winning_number_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ပိတ်မည်", color = LuckyTextSecondary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (winInput.length == 2) {
                                    viewModel.setWinningNumber(winInput)
                                    activeDialog = null
                                } else {
                                    viewModel.showToast("ဂဏန်း ၂ လုံး အပြည့်ထည့်ပါ")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_save_winning_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("သိမ်းမည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            if (winInput.length == 2) {
                                viewModel.setWinningNumber(winInput)
                            }
                            activeDialog = null
                            viewModel.navigateTo(Screen.AGENT_SETTLEMENT)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = LuckyHeader, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2D စာရင်းချုပ် သို့ သွားမည်", color = LuckyHeader, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (wins.isNotEmpty()) {
                        HorizontalDivider(color = LuckyBorder)
                        Text("သိမ်းဆည်းထားသော ပေါက်သီးများ", color = LuckyTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            wins.take(15).forEach { w: WinRecord ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LuckyBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${w.date} • ${w.session}", color = LuckyTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = w.n,
                                            color = LuckyDanger,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteWinRecord(w) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = LuckyDanger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 3: Sessions Management
    if (activeDialog == "SESSIONS") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "အကြိမ်ရေ (အချိန်) များ စီမံရန်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sessions.forEach { s: Session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(s.name, color = LuckyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("ကော်မရှင် ${fmt(s.rate)}% • အဆ ${fmt(s.mult)}", color = LuckyTextMuted, fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSession(s.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = LuckyDanger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = LuckyBorder)

                    OutlinedTextField(
                        value = newSessionName,
                        onValueChange = { newSessionName = it },
                        label = { Text("အချိန်အမည် (ဥပမာ - 12.AM သို့မဟုတ် 4.PM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSessionRate,
                            onValueChange = { newSessionRate = it },
                            label = { Text("ကော်မရှင် (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newSessionMult,
                            onValueChange = { newSessionMult = it },
                            label = { Text("အဆ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ပိတ်မည်", color = LuckyTextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (newSessionName.isNotBlank()) {
                                    viewModel.addSession(
                                        name = newSessionName.trim(),
                                        rate = newSessionRate.toDoubleOrNull() ?: 15.0,
                                        mult = newSessionMult.toDoubleOrNull() ?: 80.0
                                    )
                                    newSessionName = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("အသစ်ထည့်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 4: Blocked Numbers
    if (activeDialog == "BLOCKED") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "မရဂဏန်းများ သတ်မှတ်ရန်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = blockedInput,
                            onValueChange = { blockedInput = it },
                            label = { Text("ဂဏန်းထည့်ပါ (ဥပမာ - 12, 34)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                val nums = blockedInput.split(",", " ", ".")
                                    .map { it.trim() }
                                    .filter { it.length == 2 && it.all { c -> c.isDigit() } }
                                if (nums.isNotEmpty()) {
                                    viewModel.addBlockedNumbers(nums)
                                    blockedInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ထည့်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (blocked.isEmpty()) {
                        Text("ပိတ်ထားသော ဂဏန်း မရှိသေးပါ", color = LuckyTextMuted, fontSize = 12.sp)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            blocked.sorted().forEach { num ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LuckyBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = num,
                                        color = LuckyDanger,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeBlockedNumber(num) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = LuckyDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { activeDialog = null }) {
                            Text("ပိတ်မည်", color = LuckyHeader, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 5: Limits (Max Bet & Brake Limit)
    if (activeDialog == "LIMITS") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "အများဆုံးထိုးကြေး နှင့် ဘရိတ်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = maxBetInput,
                        onValueChange = { maxBetInput = it.filter { c -> c.isDigit() } },
                        label = { Text("တစ်ကွက် အများဆုံးထိုးကြေး") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = brakeLimitInput,
                        onValueChange = { brakeLimitInput = it.filter { c -> c.isDigit() } },
                        label = { Text("ပုံသေ ဘရိတ် ပမာဏ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("မလုပ်တော့ပါ", color = LuckyTextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val maxB = maxBetInput.toLongOrNull() ?: settings.maxBet
                                viewModel.updateMaxBet(maxB)
                                viewModel.updateBrakeLimit(brakeLimitInput)
                                viewModel.saveBrakeLimit()
                                activeDialog = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("သိမ်းမည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 6: Ledger Records (စာရင်းချုပ် မှတ်တမ်းများ) with per-round and overall breakdown
    if (activeDialog == "LEDGER_RECORDS") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "စာရင်းချုပ် မှတ်တမ်းများ (${settled.size})",
                            color = LuckyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (settled.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearSettledLedger() }) {
                                Text("ရှင်းမည်", color = LuckyDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (settled.isEmpty()) {
                        EmptyStateView(message = "သိမ်းဆည်းထားသော စာရင်းချုပ် မှတ်တမ်း မရှိသေးပါ")
                    } else {
                        val grandBet = settled.sumOf { it.totalRetained }
                        val grandComm = settled.sumOf { it.commission }
                        val grandPayout = settled.sumOf { it.payout }
                        val grandNet = settled.sumOf { it.profitLoss }
                        val isProfit = grandNet >= 0
                        val grandColor = if (isProfit) LuckySuccess else LuckyDanger

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(grandColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "စုစုပေါင်း (${if (isProfit) "အမြတ်" else "အရှုံး"})",
                                    color = grandColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ရောင်း ${fmt(grandBet)} | ကော် ${fmt(grandComm)} | လျော် ${fmt(grandPayout)}",
                                    color = LuckyTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "${if (isProfit) "+" else "-"}${fmt(abs(grandNet))}",
                                color = grandColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settled.forEach { rec ->
                                val rProfit = rec.profitLoss >= 0
                                val rColor = if (rProfit) LuckySuccess else LuckyDanger
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LuckyBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, LuckyBorder, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${rec.date} • ${rec.session} [ပေါက်သီး: ${rec.winningNumber.ifEmpty { "-" }}]",
                                            color = LuckyTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteSettledRecord(rec.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = LuckyDanger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ရောင်း: ${fmt(rec.totalRetained)}", color = LuckyTextPrimary, fontSize = 11.sp)
                                        Text("ကော်: ${fmt(rec.commission)}", color = LuckyTextSecondary, fontSize = 11.sp)
                                        Text("လျော်: ${fmt(rec.payout)}", color = LuckyDanger, fontSize = 11.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (rProfit) "အမြတ်" else "အရှုံး",
                                            color = rColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${if (rProfit) "+" else "-"}${fmt(abs(rec.profitLoss))}",
                                            color = rColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { activeDialog = null },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ပိတ်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 7: Password Change
    if (activeDialog == "PASSWORD") {
        Dialog(onDismissRequest = { activeDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "စကားဝှက် ပြောင်းလဲရန်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        label = { Text("လက်ရှိ စကားဝှက်") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("စကားဝှက် အသစ်") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("မလုပ်တော့ပါ", color = LuckyTextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (viewModel.changePassword(currentPassInput, newPassInput)) {
                                    activeDialog = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("သိမ်းမည်", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 8: Reset All Data
    if (activeDialog == "RESET_ALL") {
        DeleteConfirmDialog(
            title = "အချက်အလက်အားလုံး ရှင်းလင်းရန်",
            message = "အက်ပ်အတွင်းရှိ ဘောင်ချာများ၊ ကော်မရှင်များ၊ ပေါက်သီးများနှင့် စာရင်းချုပ်မှတ်တမ်းအားလုံးကို အပြီးအပိုင် ဖျက်ရန် သေချာပါသလား?",
            onDismiss = { activeDialog = null },
            onConfirm = {
                viewModel.resetAllData()
                activeDialog = null
            }
        )
    }
}

@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LuckyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = LuckyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = LuckyTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LuckyTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
