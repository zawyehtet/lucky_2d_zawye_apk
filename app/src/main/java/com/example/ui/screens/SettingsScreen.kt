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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Session
import com.example.data.WinRecord
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppHeader
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.RecordActionMenuDialog
import com.example.ui.components.recordLongPress
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

    // Dialogs
    var showAddBlockedDialog by remember { mutableStateOf(false) }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var showAddWinDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var resetConfirmationText by remember { mutableStateOf("") }

    // Session long-press management
    var sessionToManage by remember { mutableStateOf<Session?>(null) }
    var sessionToEdit by remember { mutableStateOf<Session?>(null) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    // Win long-press management
    var winToManage by remember { mutableStateOf<WinRecord?>(null) }
    var winToEdit by remember { mutableStateOf<WinRecord?>(null) }
    var winToDelete by remember { mutableStateOf<WinRecord?>(null) }

    // Blocked long-press management
    var blockedToManage by remember { mutableStateOf<String?>(null) }
    var blockedToEdit by remember { mutableStateOf<String?>(null) }
    var blockedToDelete by remember { mutableStateOf<String?>(null) }

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
            // Header card with Max Bet input at top right
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
                                    val digitsOnly = it.filter { c -> c.isDigit() }
                                    maxBetInput = digitsOnly
                                    val amt = digitsOnly.toLongOrNull()
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
                                            .recordLongPress(onLongPress = { blockedToManage = num })
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
                                                .clickable { blockedToDelete = num }
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
                                    .recordLongPress(onLongPress = { sessionToManage = s })
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.navigateTo(Screen.AGENT_SETTLEMENT) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LuckyHeader,
                                        contentColor = LuckyLime
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(32.dp).testTag("open_agent_settlement_btn")
                                ) {
                                    Text("2D စာရင်းချုပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

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
                                        .recordLongPress(
                                            onClick = {
                                                viewModel.setDate(win.date)
                                                viewModel.setSession(win.session)
                                                viewModel.navigateTo(Screen.AGENT_SETTLEMENT)
                                            },
                                            onLongPress = { winToManage = win }
                                        )
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

            // Fixed constant footer line below the Reset row, above the bottom navigation
            item {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF7F978F))) {
                            append("Power By ZY ")
                        }
                        withStyle(SpanStyle(color = LuckyLime, fontWeight = FontWeight.SemiBold)) {
                            append("ShweKhinMyay")
                        }
                    },
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
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
                    viewModel.navigateTo(Screen.AGENT_SETTLEMENT)
                }
            )
        }

        // Reset Confirm Dialog (Change 2: require typing DELETE or confirming twice)
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showResetConfirmDialog = false
                    resetConfirmationText = ""
                },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(18.dp),
                title = { Text("စာရင်းအားလုံး ရှင်းလင်းမည်", color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "အချက်အလက်အားလုံး ဖျက်မည်မှာ သေချာပါသလား? အတည်ပြုရန် အောက်တွင် 'DELETE' ဟု ရိုက်ထည့်ပါ:",
                            color = LuckyTextSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = resetConfirmationText,
                            onValueChange = { resetConfirmationText = it },
                            placeholder = { Text("DELETE", color = LuckyTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuckyDanger,
                                unfocusedBorderColor = LuckyBorder,
                                focusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_delete_all_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetConfirmDialog = false
                            resetConfirmationText = ""
                        },
                        enabled = resetConfirmationText.trim() == "DELETE",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyDanger,
                            contentColor = Color.White,
                            disabledContainerColor = LuckyDanger.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("confirm_reset_button")
                    ) {
                        Text("သေချာသည် (Confirm)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showResetConfirmDialog = false
                        resetConfirmationText = ""
                    }) {
                        Text("မဖျက်ပါ", color = LuckyTextSecondary)
                    }
                }
            )
        }

        // Session Actions Menu
        sessionToManage?.let { s ->
            RecordActionMenuDialog(
                title = "အပိုင်း: ${s.name}",
                onDismiss = { sessionToManage = null },
                onEdit = { sessionToEdit = s },
                onDelete = { sessionToDelete = s }
            )
        }

        // Edit Session Dialog
        sessionToEdit?.let { s ->
            EditSessionDialog(
                session = s,
                onDismiss = { sessionToEdit = null },
                onSave = { name, rate, mult ->
                    viewModel.updateSession(s.name, Session(name, rate, mult))
                    sessionToEdit = null
                }
            )
        }

        // Delete Session Confirm Dialog
        sessionToDelete?.let { s ->
            DeleteConfirmDialog(
                itemName = "အပိုင်း '${s.name}'",
                onDismiss = { sessionToDelete = null },
                onConfirm = {
                    viewModel.deleteSession(s.name)
                    sessionToDelete = null
                }
            )
        }

        // Win Record Actions Menu
        winToManage?.let { win ->
            RecordActionMenuDialog(
                title = "ထွက်ဂဏန်း: ${win.n} (${win.session})",
                onDismiss = { winToManage = null },
                onEdit = { winToEdit = win },
                onDelete = { winToDelete = win }
            )
        }

        // Edit Win Dialog
        winToEdit?.let { win ->
            EditWinDialog(
                winRecord = win,
                sessions = sessions,
                onDismiss = { winToEdit = null },
                onSave = { date, sess, num ->
                    viewModel.updateWinRecord(win, WinRecord(date, sess, num))
                    winToEdit = null
                }
            )
        }

        // Delete Win Confirm Dialog
        winToDelete?.let { win ->
            DeleteConfirmDialog(
                itemName = "ထွက်ဂဏန်း ${win.date} (${win.session}) - ${win.n}",
                onDismiss = { winToDelete = null },
                onConfirm = {
                    viewModel.deleteWinRecord(win)
                    winToDelete = null
                }
            )
        }

        // Blocked Number Actions Menu
        blockedToManage?.let { num ->
            RecordActionMenuDialog(
                title = "မရဂဏန်း: $num",
                onDismiss = { blockedToManage = null },
                onEdit = { blockedToEdit = num },
                onDelete = { blockedToDelete = num }
            )
        }

        // Edit Blocked Dialog
        blockedToEdit?.let { num ->
            EditBlockedDialog(
                blockedNumber = num,
                onDismiss = { blockedToEdit = null },
                onSave = { newNum ->
                    viewModel.editBlockedNumber(num, newNum)
                    blockedToEdit = null
                }
            )
        }

        // Delete Blocked Confirm Dialog
        blockedToDelete?.let { num ->
            DeleteConfirmDialog(
                itemName = "မရဂဏန်း '$num'",
                onDismiss = { blockedToDelete = null },
                onConfirm = {
                    viewModel.removeBlockedNumber(num)
                    blockedToDelete = null
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

@Composable
fun EditSessionDialog(
    session: Session,
    onDismiss: () -> Unit,
    onSave: (name: String, rate: Double, mult: Double) -> Unit
) {
    var name by remember { mutableStateOf(session.name) }
    var rateStr by remember { mutableStateOf(session.rate.toInt().toString()) }
    var multStr by remember { mutableStateOf(session.mult.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text("အပိုင်း ပြင်ဆင်မည် (Edit Session)", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("အပိုင်း အမည်") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("ကော် (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = multStr,
                        onValueChange = { multStr = it },
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
                        val rate = rateStr.toDoubleOrNull() ?: session.rate
                        val mult = multStr.toDoubleOrNull() ?: session.mult
                        onSave(name.trim(), rate, mult)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
            ) {
                Text("သိမ်းမည် (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ", color = Color.White) }
        }
    )
}

@Composable
fun EditWinDialog(
    winRecord: WinRecord,
    sessions: List<Session>,
    onDismiss: () -> Unit,
    onSave: (date: String, session: String, number: String) -> Unit
) {
    var date by remember { mutableStateOf(winRecord.date) }
    var selectedSessionName by remember { mutableStateOf(winRecord.session) }
    var number by remember { mutableStateOf(winRecord.n) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text("ထွက်ဂဏန်း ပြင်ဆင်မည် (Edit Win)", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("ရက်စွဲ") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = selectedSessionName,
                    onValueChange = { selectedSessionName = it },
                    label = { Text("အပိုင်း") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = {
                        if (it.length <= 2) {
                            number = it
                            errorMsg = ""
                        }
                    },
                    label = { Text("၂ လုံးဂဏန်း (00-99)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = LuckyDanger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.length != 2 || !number.all { it.isDigit() }) {
                        errorMsg = "ဂဏန်း ၂ လုံး ဖြစ်ရပါမည်"
                    } else if (date.isBlank() || selectedSessionName.isBlank()) {
                        errorMsg = "ရက်စွဲနှင့် အပိုင်း ဖြည့်စွက်ပါ"
                    } else {
                        onSave(date.trim(), selectedSessionName.trim(), number.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
            ) {
                Text("သိမ်းမည် (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ", color = Color.White) }
        }
    )
}

@Composable
fun EditBlockedDialog(
    blockedNumber: String,
    onDismiss: () -> Unit,
    onSave: (newNumber: String) -> Unit
) {
    var num by remember { mutableStateOf(blockedNumber) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text("မရဂဏန်း ပြင်ဆင်မည်", color = Color.White, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = num,
                    onValueChange = {
                        if (it.length <= 2) {
                            num = it
                            errorMsg = ""
                        }
                    },
                    label = { Text("၂ လုံးဂဏန်း (00-99)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuckyLime, focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = LuckyDanger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (num.length != 2 || !num.all { it.isDigit() }) {
                        errorMsg = "ဂဏန်း ၂ လုံး ဖြစ်ရပါမည်"
                    } else {
                        onSave(num.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
            ) {
                Text("သိမ်းမည် (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်တော့ပါ", color = Color.White) }
        }
    )
}
