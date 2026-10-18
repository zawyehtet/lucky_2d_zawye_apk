package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Session
import com.example.data.WinRecord
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val blockedNumbers by viewModel.blocked.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var maxBetInput by remember(settings.maxBet) { mutableStateOf(settings.maxBet.toString()) }
    var guarantorInput by remember(settings.guarantor) { mutableStateOf(settings.guarantor) }

    // Dialogs
    var showAddBlockedDialog by remember { mutableStateOf(false) }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var showAddWinDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Password fields
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "ဆက်တင် (Settings)",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card with Max Bet input at top right & Guarantor
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Max Bet Input at Top Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "အများဆုံးထိုးကြေး (Max Bet)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            OutlinedTextField(
                                value = maxBetInput,
                                onValueChange = {
                                    maxBetInput = it
                                    val amt = it.toLongOrNull()
                                    if (amt != null && amt > 0) {
                                        viewModel.updateMaxBet(amt)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = LuckyLime,
                                    unfocusedTextColor = LuckyLime
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(130.dp).height(48.dp).testTag("max_bet_input")
                            )
                        }

                        Divider(color = LuckyBorder, thickness = 0.5.dp)

                        // Guarantor Name ("အာမခံပေးသူ")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "အာမခံပေးသူ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            OutlinedTextField(
                                value = guarantorInput,
                                onValueChange = {
                                    guarantorInput = it
                                    viewModel.updateGuarantor(it)
                                },
                                placeholder = { Text("အမည် ရိုက်ထည့်ပါ", color = LuckyTextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(170.dp).height(48.dp).testTag("guarantor_input")
                            )
                        }
                    }
                }
            }

            // Blocked Numbers Card (မရဂဏန်းများ)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = LuckyDanger, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "မရဂဏန်းများ (Blocked 2D)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            IconButton(
                                onClick = { showAddBlockedDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LuckyDanger)
                                    .testTag("add_blocked_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Blocked", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (blockedNumbers.isEmpty()) {
                            Text(
                                text = "မရဂဏန်း (ပိတ်ဂဏန်း) မရှိသေးပါ",
                                color = LuckyTextMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(blockedNumbers.toList().sorted()) { num ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LuckyDanger.copy(alpha = 0.2f))
                                            .border(1.dp, LuckyDanger, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(num, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = LuckyDanger,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.removeBlockedNumber(num) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sessions Card (အကြိမ်ရေ)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "အကြိမ်ရေ (Sessions)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            IconButton(
                                onClick = { showAddSessionDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LuckyHeader)
                                    .testTag("add_session_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Session", tint = LuckyLime, modifier = Modifier.size(18.dp))
                            }
                        }

                        sessions.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyCardDark, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.name, color = LuckyLime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("ကော်: ${s.rate.toInt()}%  |  အဆ: ${s.mult.toInt()}x", color = LuckyTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Winning Numbers History (ထွက်ဂဏန်းများ: စဉ် | ရက် | အပိုင်း | ဂဏန်း)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ထွက်ဂဏန်းများ (Winning History)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            IconButton(
                                onClick = { showAddWinDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LuckyHeader)
                                    .testTag("add_win_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Win", tint = LuckyLime, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (wins.isEmpty()) {
                            Text("ထွက်ဂဏန်းမှတ်တမ်း မရှိသေးပါ", color = LuckyTextMuted, fontSize = 12.sp)
                        } else {
                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LuckyHeader, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("စဉ်", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                                Text("ရက်စွဲ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("အပိုင်း", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("ဂဏန်း", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }

                            wins.take(10).forEachIndexed { idx, win ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 1}.", color = LuckyTextMuted, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                                    Text(win.date, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                    Text(win.session, color = LuckyTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text(win.n, color = LuckyLime, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                                Divider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Change Password Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "စကားဝှက် ပြောင်းရန် (Change Password)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currentPass,
                                onValueChange = { currentPass = it },
                                label = { Text("လက်ရှိ စကားဝှက်") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("current_password_input")
                            )

                            OutlinedTextField(
                                value = newPass,
                                onValueChange = { newPass = it },
                                label = { Text("စကားဝှက် အသစ်") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("new_password_input")
                            )

                            IconButton(
                                onClick = {
                                    if (viewModel.changePassword(currentPass, newPass)) {
                                        currentPass = ""
                                        newPass = ""
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(LuckyLime)
                                    .testTag("save_password_fab")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save Password", tint = LuckyBg)
                            }
                        }
                    }
                }
            }

            // Reset All Data Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "အချက်အလက်အားလုံး ရှင်းထုတ်မည်",
                                color = LuckyDanger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Reset all app data to defaults",
                                color = LuckyTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("reset_all_data_btn")
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Add Blocked Dialog
        if (showAddBlockedDialog) {
            AddBlockedDialog(
                onDismiss = { showAddBlockedDialog = false },
                onAdd = { nums ->
                    viewModel.addBlockedNumbers(nums)
                    showAddBlockedDialog = false
                }
            )
        }

        // Add Session Dialog
        if (showAddSessionDialog) {
            AddSessionDialog(
                onDismiss = { showAddSessionDialog = false },
                onAdd = { name, rate, mult ->
                    viewModel.addSession(name, rate, mult)
                    showAddSessionDialog = false
                }
            )
        }

        // Add Win Dialog
        if (showAddWinDialog) {
            AddWinDialog(
                currentSession = selectedSession,
                sessions = sessions,
                onDismiss = { showAddWinDialog = false },
                onAdd = { num ->
                    viewModel.setWinningNumber(num)
                    showAddWinDialog = false
                }
            )
        }

        // Reset Confirm Dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(18.dp),
                title = { Text("အတည်ပြုပါ", color = Color.White) },
                text = { Text("အချက်အလက်အားလုံး ဖျက်မည်မှာ သေချာပါသလား? (All data will be reset)", color = LuckyTextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White)
                    ) {
                        Text("ဖျက်မည် (Reset)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("မလုပ်တော့ပါ", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun AddBlockedDialog(
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text("2D-မရဂဏန်းများ ထည့်မည်", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ဂဏန်းများကို ကော်မာ သို့မဟုတ် နေရာလွတ် ခြား၍ ရိုက်ပါ (ဥပမာ: 12, 34, 56)", color = LuckyTextMuted, fontSize = 12.sp)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("12, 34, 56", color = LuckyTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nums = input.split(Regex("[\\s,;]+")).map { it.trim() }.filter { it.isNotBlank() }
                    onAdd(nums)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White)
            ) {
                Text("ထည့်မည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ", color = Color.White) }
        }
    )
}

@Composable
fun AddSessionDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, rate: Double, mult: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var multStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text("အပိုင်း အသစ်ထည့်မည် (Add Session)", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("ဥပမာ: 2.PM", color = LuckyTextMuted) },
                    label = { Text("အပိုင်း အမည်") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        placeholder = { Text("15", color = LuckyTextMuted) },
                        label = { Text("ကော် (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = multStr,
                        onValueChange = { multStr = it },
                        placeholder = { Text("80", color = LuckyTextMuted) },
                        label = { Text("အဆ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val rate = rateStr.toDoubleOrNull() ?: 15.0
                        val mult = multStr.toDoubleOrNull() ?: 80.0
                        onAdd(name, rate, mult)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
            ) {
                Text("ထည့်မည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ", color = Color.White) }
        }
    )
}

@Composable
fun AddWinDialog(
    currentSession: String,
    sessions: List<Session>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var winNum by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text("ထွက်ဂဏန်း သတ်မှတ်မည်", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("အပိုင်း: $currentSession", color = LuckyTextSecondary, fontSize = 13.sp)
                OutlinedTextField(
                    value = winNum,
                    onValueChange = { if (it.length <= 2) winNum = it },
                    label = { Text("၂ လုံးဂဏန်း (00-99)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (winNum.length == 2) {
                        onAdd(winNum)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
            ) {
                Text("သိမ်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ", color = Color.White) }
        }
    )
}
