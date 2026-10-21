@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
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
fun SettingsScreen(
    viewModel: MainViewModel,
    initialDialog: String? = null
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val settled by viewModel.settled.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentBrakeInput by viewModel.currentBrakeInput.collectAsState()

    val hasRound = sessions.isNotEmpty() && selectedSession.isNotBlank() && sessions.any { it.name == selectedSession }
    val noRoundsText = stringResource(R.string.no_rounds_yet)

    var activeDialog by rememberSaveable { mutableStateOf(initialDialog) }

    val currentWin = remember(wins, selectedDate, selectedSession, hasRound) {
        viewModel.getWinningNumberForCurrentSession()
    }

    // Winning Number state — starts blank so Save button is hidden until user types or edits
    var winInput by rememberSaveable(selectedDate, selectedSession) {
        mutableStateOf("")
    }

    // Session (Round) add/edit/delete dialog state — all start blank with no defaults
    val haptic = LocalHapticFeedback.current
    var editingSessionOldName by rememberSaveable { mutableStateOf<String?>(null) }
    var newSessionName by rememberSaveable { mutableStateOf("") }
    var newSessionRate by rememberSaveable { mutableStateOf("") }
    var newSessionMult by rememberSaveable { mutableStateOf("") }
    var sessionMenuTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editModalSessionName by rememberSaveable { mutableStateOf("") }
    var editModalSessionRate by rememberSaveable { mutableStateOf("") }
    var editModalSessionMult by rememberSaveable { mutableStateOf("") }
    var deletingSessionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Blocked numbers dialog state
    var blockedInput by rememberSaveable { mutableStateOf("") }

    // Brake limit dialog state — starts blank so Save button is hidden until user types
    var brakeLimitInput by rememberSaveable {
        mutableStateOf("")
    }

    // Password change dialog state
    var currentPassInput by rememberSaveable { mutableStateOf("") }
    var newPassInput by rememberSaveable { mutableStateOf("") }

    // Winning number confirmation & history long-press states
    var showWinSaveConfirm by rememberSaveable { mutableStateOf(false) }
    var activeWinMenuKey by rememberSaveable { mutableStateOf<String?>(null) }
    var editingWinRecord by remember { mutableStateOf<WinRecord?>(null) }
    var editingWinNewNumber by rememberSaveable { mutableStateOf("") }
    var deletingWinRecord by remember { mutableStateOf<WinRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "ဆက်တင်",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() },
            showSessionPill = false
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

            // 1. မရဂဏန်းများ (Blocked Numbers)
            item {
                SettingsRowCard(
                    icon = Icons.Default.Block,
                    iconColor = LuckyDanger,
                    title = "မရဂဏန်းများ",
                    subtitle = if (blocked.isEmpty()) "ပိတ်ထားသော ဂဏန်း မရှိသေးပါ" else "ပိတ်ထားသော ဂဏန်း (${blocked.size}) လုံး",
                    testTag = "setting_blocked_numbers",
                    onClick = { activeDialog = "BLOCKED" }
                )
            }

            // 2. အကြိမ်ရေ (Session Management)
            item {
                SettingsRowCard(
                    icon = Icons.Default.AccessTime,
                    iconColor = LuckyHeader,
                    title = "အကြိမ်ရေ",
                    subtitle = sessions.joinToString(", ") { it.name }.ifEmpty { noRoundsText },
                    testTag = "setting_sessions",
                    onClick = {
                        editingSessionOldName = null
                        newSessionName = ""
                        newSessionRate = ""
                        newSessionMult = ""
                        activeDialog = "SESSIONS"
                    }
                )
            }

            // 3. ထွက်ဂဏန်းများ (Winning Numbers History & Entry)
            item {
                val hasSavedWin = hasRound && currentWin.length == 2 && currentWin.all { it.isDigit() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsRowCard(
                        icon = Icons.Default.EmojiEvents,
                        iconColor = LuckyWarning,
                        title = "ထွက်ဂဏန်းများ",
                        subtitle = when {
                            !hasRound -> stringResource(R.string.add_round_first)
                            hasSavedWin -> "လက်ရှိပေါက်သီး: ${formatWinningNumberTwoDigits(currentWin)}"
                            else -> "ပေါက်သီး ထည့်သွင်းရန်နှင့် မှတ်တမ်းကြည့်ရန်"
                        },
                        testTag = "setting_winning_numbers",
                        onClick = {
                            winInput = ""
                            activeDialog = "WINNING_NUMBERS"
                        }
                    )
                    AnimatedVisibility(visible = hasSavedWin) {
                        CompactPrimaryButton(
                            text = stringResource(R.string.action_view_winning_number),
                            onClick = {
                                viewModel.openNumbersFromWinningNumber()
                            },
                            horizontalPadding = 16.dp,
                            cornerRadius = 10.dp,
                            modifier = Modifier.testTag("settings_card_view_winning_number_btn")
                        )
                    }
                }
            }

            // 4. Change Password
            item {
                SettingsRowCard(
                    icon = Icons.Default.Lock,
                    iconColor = LuckyWarning,
                    title = "Change Password",
                    subtitle = "လုံခြုံရေး စကားဝှက် အသစ် သတ်မှတ်ရန်",
                    testTag = "setting_password",
                    onClick = {
                        currentPassInput = ""
                        newPassInput = ""
                        activeDialog = "PASSWORD"
                    }
                )
            }

            // 5. Reset (Master reset with confirmation dialog)
            item {
                SettingsRowCard(
                    icon = Icons.Default.DeleteForever,
                    iconColor = LuckyDanger,
                    title = "Reset",
                    subtitle = "ဘောင်ချာနှင့် မှတ်တမ်းအားလုံး အသစ်ပြန်စရန်",
                    testTag = "setting_reset_all",
                    onClick = { activeDialog = "RESET_ALL" }
                )
            }

            // Additional Settings: Date/Round, Brake Limit, Sound Toggle
            item {
                SettingsRowCard(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = LuckyHeader,
                    title = stringResource(R.string.setting_date_round_title),
                    subtitle = "$selectedDate • ${if (hasRound) selectedSession else noRoundsText}",
                    testTag = "setting_date_session",
                    onClick = { activeDialog = "DATE_SESSION" }
                )
            }

            item {
                val effectiveBrake = if (currentBrakeInput.isNotBlank()) {
                    MainViewModel.parseAmountOrZero(currentBrakeInput)
                } else {
                    settings.brakeLimit
                }
                SettingsRowCard(
                    icon = Icons.Default.Speed,
                    iconColor = LuckySuccess,
                    title = stringResource(R.string.setting_brake_limit_title),
                    subtitle = "${stringResource(R.string.label_brake)}: ${fmt(effectiveBrake)}",
                    testTag = "setting_limits",
                    onClick = {
                        brakeLimitInput = if (effectiveBrake > 0L) effectiveBrake.toString() else currentBrakeInput
                        activeDialog = "LIMITS"
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setting_sound_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(LuckyHeader.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = stringResource(R.string.setting_sound_title),
                                tint = LuckyHeader,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_sound_title),
                                color = LuckyTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.soundEnabled) {
                                    stringResource(R.string.setting_sound_enabled_subtitle)
                                } else {
                                    stringResource(R.string.setting_sound_disabled_subtitle)
                                },
                                color = LuckyTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setSoundEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LuckyHeader
                            ),
                            modifier = Modifier.testTag("sound_switch")
                        )
                    }
                }
            }
        }
    }

    // Dialog 1: Date & Round
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
                        text = stringResource(R.string.dialog_date_round_title),
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

                    Text(
                        text = stringResource(R.string.dialog_select_round_subtitle),
                        color = LuckyTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (sessions.isEmpty()) {
                        EmptyStateView(
                            message = noRoundsText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
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
                            Text(stringResource(R.string.action_close), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog 2: Winning Numbers ("ထွက်ဂဏန်း" — REF: IMAGE 20)
    if (activeDialog == "WINNING_NUMBERS") {
        var sessionDropdownExpanded by remember { mutableStateOf(false) }
        val hasSavedWinningNumber = hasRound && currentWin.length == 2 && currentWin.all { it.isDigit() }

        Dialog(
            onDismissRequest = { activeDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ထွက်ဂဏန်း",
                        color = LuckyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    RoundRequiredHelperBanner(visible = !hasRound)

                    // 1. "ရက်စွဲ" with Date display and Calendar picker icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ရက်စွဲ",
                            color = LuckyTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp)
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = selectedDate,
                                color = LuckyTextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "ရက်စွဲ ရွေးရန်",
                                tint = LuckyHeader
                            )
                        }
                    }

                    // 2. "အပိုင်း" with Session dropdown selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "အပိုင်း",
                            color = LuckyTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { if (sessions.isNotEmpty()) sessionDropdownExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (hasRound) selectedSession else noRoundsText,
                                    color = LuckyTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = LuckyHeader
                                )
                            }
                            DropdownMenu(
                                expanded = sessionDropdownExpanded,
                                onDismissRequest = { sessionDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                sessions.forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = s.name,
                                                color = LuckyTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSession(s.name)
                                            sessionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. "ထွက်ဂဏန်း" with clean underlined input (no default number) + Floating Save Button (Floppy disk icon)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ထွက်ဂဏန်း",
                            color = LuckyTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp)
                        )
                        TextField(
                            value = winInput,
                            onValueChange = { raw ->
                                val filtered = raw.filter { c -> c.isDigit() }.take(2)
                                winInput = filtered
                            },
                            enabled = hasRound,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                color = LuckyTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = LuckyHeader,
                                unfocusedIndicatorColor = LuckyBorder,
                                focusedTextColor = LuckyTextPrimary,
                                unfocusedTextColor = LuckyTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("settings_winning_number_input")
                        )

                        FloatingActionButton(
                            onClick = {
                                val cleanWin = winInput.trim()
                                if (cleanWin.length == 2) {
                                    showWinSaveConfirm = true
                                } else {
                                    viewModel.showToast("ဂဏန်း ၂ လုံး အပြည့်ထည့်ပါ")
                                }
                            },
                            containerColor = LuckyHeader,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_save_winning_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.action_save)
                            )
                        }
                    }

                    AnimatedVisibility(visible = hasSavedWinningNumber) {
                        CompactPrimaryButton(
                            text = stringResource(R.string.action_view_winning_number),
                            onClick = {
                                activeDialog = null
                                viewModel.openNumbersFromWinningNumber()
                            },
                            horizontalPadding = 16.dp,
                            cornerRadius = 10.dp,
                            modifier = Modifier.testTag("view_winning_number_btn")
                        )
                    }

                    // 4. Winning Numbers History Table (combinedClickable -> Long press Edit/Delete, no standalone Delete button)
                    if (wins.isNotEmpty()) {
                        HorizontalDivider(color = LuckyBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuckyHeader, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ရက်စွဲ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("အပိုင်း", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text("ထွက်ဂဏန်း", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            wins.forEach { w: WinRecord ->
                                val rowKey = "${w.date}|${w.session}"
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, LuckyBorder, RoundedCornerShape(6.dp))
                                            .entryCombinedClickable(
                                                enabled = true,
                                                itemDescription = "${w.date} ${w.session} ထွက်ဂဏန်း ${w.n}",
                                                onClick = { activeWinMenuKey = rowKey },
                                                onShowMenu = { activeWinMenuKey = rowKey },
                                                onEditAction = {
                                                    editingWinRecord = w
                                                    editingWinNewNumber = w.n
                                                },
                                                onDeleteAction = {
                                                    deletingWinRecord = w
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = w.date,
                                            color = LuckyTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Text(
                                            text = w.session,
                                            color = LuckyTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = w.n,
                                            color = LuckyDanger,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                    }

                                    EntryLongPressDropdownMenu(
                                        expanded = activeWinMenuKey == rowKey,
                                        onDismiss = { activeWinMenuKey = null },
                                        title = "${w.date} (${w.session}) : ${w.n}",
                                        onEdit = {
                                            activeWinMenuKey = null
                                            editingWinRecord = w
                                            editingWinNewNumber = w.n
                                        },
                                        onDelete = {
                                            activeWinMenuKey = null
                                            deletingWinRecord = w
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTextButton(
                            text = stringResource(R.string.action_close),
                            onClick = { activeDialog = null },
                            modifier = Modifier.testTag("settings_close_winning_btn")
                        )
                    }
                }
            }
        }

        // Confirmation Dialog on Save: "ထွက်ဂဏန်း ထည့်ရန် သေချာသလား?" with "ထည့်မည်" (Yes) and "မလုပ်ပါ" (No)
        if (showWinSaveConfirm) {
            AlertDialog(
                onDismissRequest = { showWinSaveConfirm = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        text = "ထွက်ဂဏန်း ထည့်ရန် သေချာသလား?",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "ရက်စွဲ: $selectedDate | အပိုင်း: $selectedSession\nထွက်ဂဏန်း: ${winInput.trim()}",
                        color = LuckyTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanWin = winInput.trim()
                            showWinSaveConfirm = false
                            if (cleanWin.length == 2) {
                                viewModel.setWinningNumber(cleanWin)
                                winInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyHeader,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_save_winning_btn")
                    ) {
                        Text("ထည့်မည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showWinSaveConfirm = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                    ) {
                        Text("မလုပ်ပါ", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Edit Winning Record Modal ("ပြင်မည်")
        editingWinRecord?.let { targetWin ->
            AlertDialog(
                onDismissRequest = { editingWinRecord = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text("ထွက်ဂဏန်း ပြင်မည်", color = LuckyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${targetWin.date} • ${targetWin.session}", color = LuckyTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = editingWinNewNumber,
                            onValueChange = { editingWinNewNumber = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("ထွက်ဂဏန်း") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editingWinNewNumber.length == 2) {
                                viewModel.updateWinRecord(targetWin, targetWin.copy(n = editingWinNewNumber))
                                editingWinRecord = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingWinRecord = null }) {
                        Text(stringResource(R.string.action_cancel), color = LuckyTextSecondary)
                    }
                }
            )
        }

        // Delete Winning Record Confirmation ("ဖျက်မည်")
        deletingWinRecord?.let { targetWin ->
            DeleteConfirmDialog(
                itemName = "${targetWin.date} (${targetWin.session}) : ${targetWin.n}",
                onDismiss = { deletingWinRecord = null },
                onConfirm = {
                    viewModel.deleteWinRecord(targetWin)
                    deletingWinRecord = null
                }
            )
        }
    }

    // Dialog 3: Sessions (Rounds) Management — "အကြိမ်ရေ"
    if (activeDialog == "SESSIONS") {
        Dialog(
            onDismissRequest = { activeDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val showSessionSaveBtn = MainViewModel.shouldShowRoundSaveButton(newSessionName)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.manage_round_times_title),
                        color = LuckyTextPrimary,
                        fontSize = 17.sp,
                        lineHeight = 1.5.em,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("manage_rounds_title")
                    )

                    // Proportional 4-Column Table: [ စဉ် | အကြိမ်အမည် | ကော်မရှင် % | အဆ ]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
                            .background(LuckyCard)
                            .testTag("sessions_table_container")
                    ) {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuckyHeader)
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                                .testTag("sessions_table_header"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.session_col_serial),
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = stringResource(R.string.session_col_name),
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                text = stringResource(R.string.session_col_commission),
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = stringResource(R.string.session_col_mult),
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        if (sessions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp, horizontal = 12.dp)
                                    .testTag("sessions_empty_state"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.sessions_empty_text),
                                    color = LuckyTextMuted,
                                    fontSize = 14.sp,
                                    lineHeight = 1.5.em,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                sessions.forEachIndexed { index, s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .background(if (index % 2 == 0) LuckyCard else LuckyCardDark)
                                            .combinedClickable(
                                                onClick = {
                                                    // Tapping normally on a row does not trigger accidental deletions
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    sessionMenuTargetId = s.id
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 9.dp)
                                            .testTag("session_row_${s.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = LuckyTextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 1.5.em,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.6f)
                                        )
                                        Text(
                                            text = s.name,
                                            color = LuckyTextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 1.5.em,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Start,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1.4f)
                                        )
                                        Text(
                                            text = "${fmt(s.rate)}%",
                                            color = LuckyTextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 1.5.em,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1.1f)
                                        )
                                        Text(
                                            text = fmt(s.mult),
                                            color = LuckyHeader,
                                            fontSize = 14.sp,
                                            lineHeight = 1.5.em,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                    }
                                    if (index < sessions.lastIndex) {
                                        HorizontalDivider(color = LuckyBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = LuckyBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppOutlinedTextField(
                            value = newSessionName,
                            onValueChange = {
                                newSessionName = it
                                if (it.isBlank()) {
                                    editingSessionOldName = null
                                }
                            },
                            label = { Text(stringResource(R.string.round_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("session_name_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AnimatedVisibility(visible = showSessionSaveBtn) {
                            CompactPrimaryButton(
                                text = stringResource(R.string.action_save),
                                onClick = {
                                    val cleanName = newSessionName.trim()
                                    if (cleanName.isNotEmpty()) {
                                        val rateVal = MainViewModel.parseDoubleOrZero(newSessionRate)
                                        val multVal = MainViewModel.parseDoubleOrZero(newSessionMult)
                                        val oldName = editingSessionOldName
                                        val saved = if (oldName != null) {
                                            viewModel.updateSession(
                                                oldName = oldName,
                                                updatedSession = Session(name = cleanName, rate = rateVal, mult = multVal)
                                            )
                                        } else {
                                            viewModel.addSession(
                                                name = cleanName,
                                                rate = rateVal,
                                                mult = multVal
                                            )
                                        }
                                        if (saved) {
                                            editingSessionOldName = null
                                            newSessionName = ""
                                            newSessionRate = ""
                                            newSessionMult = ""
                                        }
                                    }
                                },
                                horizontalPadding = 16.dp,
                                cornerRadius = 10.dp,
                                modifier = Modifier.testTag("session_save_btn")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppOutlinedTextField(
                            value = newSessionRate,
                            onValueChange = { newSessionRate = it },
                            label = { Text(stringResource(R.string.label_commission_percent)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("session_rate_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        AppOutlinedTextField(
                            value = newSessionMult,
                            onValueChange = { newSessionMult = it },
                            label = { Text(stringResource(R.string.label_multiplier)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("session_mult_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTextButton(
                            text = stringResource(R.string.action_close),
                            onClick = {
                                editingSessionOldName = null
                                activeDialog = null
                            },
                            modifier = Modifier.testTag("session_close_btn")
                        )
                    }
                }
            }
        }
    }

    // Long-press Action Dialog for a Session Row ("ပြင်မည်" / "ဖျက်မည်")
    val sessionMenuTarget = remember(sessions, sessionMenuTargetId) {
        sessions.firstOrNull { it.id == sessionMenuTargetId }
    }
    if (sessionMenuTarget != null) {
        RecordActionMenuDialog(
            title = "${sessionMenuTarget.name} (${fmt(sessionMenuTarget.rate)}% • ${fmt(sessionMenuTarget.mult)})",
            onDismiss = { sessionMenuTargetId = null },
            onEdit = {
                viewModel.onEditActionStarted()
                editingSessionId = sessionMenuTarget.id
                editModalSessionName = sessionMenuTarget.name
                editModalSessionRate = fmt(sessionMenuTarget.rate)
                editModalSessionMult = fmt(sessionMenuTarget.mult)
                sessionMenuTargetId = null
            },
            onDelete = {
                deletingSessionId = sessionMenuTarget.id
                sessionMenuTargetId = null
            }
        )
    }

    // Edit Session Modal ("ပြင်မည်" -> updates exact record by ID without creating duplicate entries)
    val editingSessionTarget = remember(sessions, editingSessionId) {
        sessions.firstOrNull { it.id == editingSessionId }
    }
    if (editingSessionTarget != null) {
        AlertDialog(
            onDismissRequest = { editingSessionId = null },
            containerColor = LuckyCard,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = stringResource(R.string.session_edit_dialog_title),
                    color = LuckyTextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 1.5.em,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("edit_session_modal_title")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppOutlinedTextField(
                        value = editModalSessionName,
                        onValueChange = { editModalSessionName = it },
                        label = { Text(stringResource(R.string.session_col_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_session_modal_name_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    AppOutlinedTextField(
                        value = editModalSessionRate,
                        onValueChange = { editModalSessionRate = it },
                        label = { Text(stringResource(R.string.label_commission_percent)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_session_modal_rate_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    AppOutlinedTextField(
                        value = editModalSessionMult,
                        onValueChange = { editModalSessionMult = it },
                        label = { Text(stringResource(R.string.label_multiplier)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus()
                            .testTag("edit_session_modal_mult_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = editModalSessionName.trim()
                        if (cleanName.isNotEmpty()) {
                            val rateVal = MainViewModel.parseDoubleOrZero(editModalSessionRate)
                            val multVal = MainViewModel.parseDoubleOrZero(editModalSessionMult)
                            val updated = viewModel.updateSessionById(
                                sessionId = editingSessionTarget.id,
                                updatedSession = Session(
                                    name = cleanName,
                                    rate = rateVal,
                                    mult = multVal,
                                    id = editingSessionTarget.id
                                )
                            )
                            if (updated) {
                                editingSessionId = null
                            }
                        }
                    },
                    enabled = editModalSessionName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("edit_session_modal_save_btn")
                ) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold, lineHeight = 1.5.em)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingSessionId = null },
                    modifier = Modifier.testTag("edit_session_modal_cancel_btn")
                ) {
                    Text(stringResource(R.string.action_cancel), color = LuckyTextSecondary, lineHeight = 1.5.em)
                }
            }
        )
    }

    // Delete Session Confirmation Dialog ("ဤအကြိမ်ရေအား ဖျက်မည်လား?" with "မဖျက်ပါ" and "ဖျက်မည်")
    val deletingSessionTarget = remember(sessions, deletingSessionId) {
        sessions.firstOrNull { it.id == deletingSessionId }
    }
    if (deletingSessionTarget != null) {
        DeleteConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.session_delete_confirm_message),
            onDismiss = { deletingSessionId = null },
            onConfirm = {
                val targetId = deletingSessionTarget.id
                if (editingSessionOldName == deletingSessionTarget.name) {
                    editingSessionOldName = null
                    newSessionName = ""
                    newSessionRate = ""
                    newSessionMult = ""
                }
                deletingSessionId = null
                viewModel.deleteSessionById(targetId)
            }
        )
    }

    // Dialog 4: Blocked Numbers
    if (activeDialog == "BLOCKED") {
        Dialog(
            onDismissRequest = { activeDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val showBlockedSaveBtn = blockedInput.isNotBlank()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "မရဂဏန်းများ သတ်မှတ်ရန်",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppOutlinedTextField(
                            value = blockedInput,
                            onValueChange = { blockedInput = it },
                            label = { Text(stringResource(R.string.label_blocked_number)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("blocked_number_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        AnimatedVisibility(visible = showBlockedSaveBtn) {
                            CompactPrimaryButton(
                                text = stringResource(R.string.action_save),
                                onClick = {
                                    val nums = blockedInput.split(",", " ", ".")
                                        .map { it.trim() }
                                        .filter { it.length == 2 && it.all { c -> c.isDigit() } }
                                    if (nums.isNotEmpty()) {
                                        viewModel.addBlockedNumbers(nums)
                                        blockedInput = ""
                                    } else {
                                        viewModel.showToast("ဂဏန်း ၂ လုံး အပြည့်ထည့်ပါ")
                                    }
                                },
                                horizontalPadding = 16.dp,
                                cornerRadius = 10.dp,
                                modifier = Modifier.testTag("blocked_save_btn")
                            )
                        }
                    }

                    if (blocked.isEmpty()) {
                        Text("ပိတ်ထားသော ဂဏန်း မရှိသေးပါ", color = LuckyTextMuted, fontSize = 12.sp, lineHeight = 18.sp)
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
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = LuckyDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CompactTextButton(
                            text = stringResource(R.string.action_close),
                            onClick = { activeDialog = null }
                        )
                    }
                }
            }
        }
    }

    // Dialog 5: Brake Limit
    if (activeDialog == "LIMITS") {
        Dialog(
            onDismissRequest = { activeDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val showBrakeLimitSave = brakeLimitInput.isNotBlank()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_brake_limit_title),
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppOutlinedTextField(
                            value = brakeLimitInput,
                            onValueChange = { brakeLimitInput = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.label_default_brake)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("settings_brake_limit_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        AnimatedVisibility(visible = showBrakeLimitSave) {
                            CompactPrimaryButton(
                                text = stringResource(R.string.action_save),
                                onClick = {
                                    if (brakeLimitInput.isNotBlank()) {
                                        viewModel.updateBrakeLimit(brakeLimitInput)
                                        viewModel.saveBrakeLimit()
                                        activeDialog = null
                                    }
                                },
                                horizontalPadding = 16.dp,
                                cornerRadius = 10.dp,
                                modifier = Modifier.testTag("settings_save_limits_btn")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CompactTextButton(
                            text = stringResource(R.string.action_close),
                            onClick = { activeDialog = null }
                        )
                    }
                }
            }
        }
    }



    // Dialog 7: Password Change
    if (activeDialog == "PASSWORD") {
        val showPasswordSaveBtn = currentPassInput.isNotBlank() && newPassInput.isNotBlank()
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

                    AppOutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        label = { Text(stringResource(R.string.label_current_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    AppOutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text(stringResource(R.string.label_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundOnInputFocus(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { activeDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.action_cancel), color = LuckyTextSecondary, fontWeight = FontWeight.Bold)
                        }
                        AnimatedVisibility(visible = showPasswordSaveBtn) {
                            CompactPrimaryButton(
                                text = stringResource(R.string.action_save),
                                onClick = {
                                    if (viewModel.changePassword(currentPassInput, newPassInput)) {
                                        currentPassInput = ""
                                        newPassInput = ""
                                        activeDialog = null
                                    }
                                },
                                horizontalPadding = 16.dp,
                                cornerRadius = 10.dp,
                                modifier = Modifier.testTag("password_save_btn")
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 8: Reset All Data
    if (activeDialog == "RESET_ALL") {
        DeleteConfirmDialog(
            title = "ဖျက်မည်လား?",
            message = "အက်ပ်အတွင်းရှိ ဘောင်ချာများ၊ ကော်မရှင်များ၊ ပေါက်သီးများနှင့် စာရင်းချုပ်မှတ်တမ်းအားလုံးကို အပြီးအပိုင် ဖျက်မည်လား?",
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
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = LuckyTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
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
