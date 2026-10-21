package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Agent
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AgentsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val wins by viewModel.wins.collectAsState()

    val hasRound = sessions.isNotEmpty() && selectedSession.isNotBlank() && sessions.any { it.name == selectedSession }
    val noRoundsText = stringResource(R.string.no_rounds_yet)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var agentToEditId by rememberSaveable { mutableStateOf<Int?>(null) }
    var agentToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }

    val agentToEdit = agents.firstOrNull { it.id == agentToEditId }
    val agentToDelete = agents.firstOrNull { it.id == agentToDeleteId }

    val winNum = if (hasRound) {
        wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n
    } else null

    val filteredAgents = agents.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.id.toString() == searchQuery.trim()
    }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "ကော်မရှင်",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "နေ့စွဲ : $selectedDate",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${stringResource(R.string.round_label)} : ${if (hasRound) selectedSession else noRoundsText}",
                        color = LuckyHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                SearchPill(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    label = stringResource(R.string.label_search_agent),
                    modifier = Modifier.testTag("agent_search_input")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredAgents.isEmpty()) {
                    item {
                        EmptyListPlaceholder(
                            text = "ကော်မရှင် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ",
                            modifier = Modifier.padding(vertical = 40.dp)
                        )
                    }
                } else {
                    items(filteredAgents, key = { it.id }) { agent ->
                        val summary = remember(agent, vouchers, wins, selectedDate, selectedSession, hasRound) {
                            viewModel.getAgentSummaryData(agent, winNum ?: "")
                        }
                        val hasWinForSession = !winNum.isNullOrBlank() && winNum.length == 2
                        AgentCard(
                            agent = agent,
                            totalBet = summary.totalBet,
                            commission = summary.commission,
                            netAmount = summary.netAmount,
                            voucherCount = summary.voucherCount,
                            payout = summary.payout,
                            hasWinningNumber = hasWinForSession,
                            canStartBet = hasRound,
                            onStartBet = {
                                viewModel.startNumberEntry(agent)
                            },
                            onOpenVouchers = {
                                viewModel.openAgentVouchers(agent.name)
                            },
                            onEdit = {
                                viewModel.onEditActionStarted()
                                agentToEditId = agent.id
                            },
                            onDelete = {
                                agentToDeleteId = agent.id
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = LuckyHeader,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 16.dp)
                .testTag("add_agent_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "ကော်မရှင် အသစ်ထည့်ရန်", modifier = Modifier.size(28.dp))
        }

        if (showAddDialog) {
            AddAgentDialog(
                nextId = (agents.maxOfOrNull { it.id } ?: 0) + 1,
                onDismiss = { showAddDialog = false },
                onAdd = { name, rate, mult ->
                    viewModel.addAgent(name, rate, mult)
                    showAddDialog = false
                }
            )
        }

        agentToEdit?.let { ag ->
            EditAgentDialog(
                agent = ag,
                onDismiss = { agentToEditId = null },
                onSave = { updated ->
                    viewModel.updateAgent(updated)
                    agentToEditId = null
                }
            )
        }

        agentToDelete?.let { ag ->
            DeleteConfirmDialog(
                itemName = ag.name,
                onDismiss = { agentToDeleteId = null },
                onConfirm = {
                    viewModel.deleteAgent(ag.id)
                    agentToDeleteId = null
                }
            )
        }
    }
}

@Composable
fun AgentCard(
    agent: Agent,
    totalBet: Long,
    commission: Long,
    netAmount: Long,
    voucherCount: Int,
    payout: Long,
    hasWinningNumber: Boolean = false,
    canStartBet: Boolean = true,
    onStartBet: () -> Unit,
    onOpenVouchers: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val isCommissionEntryValid = agent.id > 0 && agent.name.isNotBlank()

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LuckyCard),
            modifier = Modifier
                .fillMaxWidth()
                .entryCombinedClickable(
                    enabled = isCommissionEntryValid,
                    itemDescription = "${agent.name} ကော် ${agent.rate.toInt()}% အဆ ${agent.mult.toInt()}",
                    onClick = {},
                    onShowMenu = {
                        if (isCommissionEntryValid) {
                            showMenu = true
                        }
                    },
                    onEditAction = onEdit,
                    onDeleteAction = onDelete
                )
                .border(1.dp, LuckyBorder, RoundedCornerShape(12.dp))
                .testTag("agent_card_${agent.id}")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${agent.id}",
                                color = LuckyHeader,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = agent.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "ကော်: ${agent.rate.toInt()}% | အဆ: ${agent.mult.toInt()}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Only the "ထိုးရန်" button remains; the delete button beside it has been removed
                    Button(
                        onClick = onStartBet,
                        enabled = canStartBet,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = LuckyHeader,
                            disabledContainerColor = Color.White.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("agent_start_bet_${agent.id}")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_place_bet),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_place_bet),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Stats grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell(label = "စုပေါင်းထိုးကြေး", value = fmt(totalBet))
                        StatCell(label = "ကော်မရှင် (${agent.rate.toInt()}%)", value = fmt(commission))
                        StatCell(label = "ကော်နုတ်ပြီး", value = fmt(netAmount), isHighlight = true)
                    }

                    HorizontalDivider(color = LuckyBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clickable "ဘောင်ချာ = X" navigating directly to that agent's vouchers list
                        Surface(
                            color = LuckyCardDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { onOpenVouchers() }
                                .border(1.dp, LuckyBorder, RoundedCornerShape(8.dp))
                                .testTag("agent_vouchers_btn_${agent.id}")
                        ) {
                            Text(
                                text = "ဘောင်ချာ = $voucherCount",
                                color = LuckyHeader,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Profit/Loss Visibility Rule: show ONLY AFTER a winning number has been entered
                        if (hasWinningNumber) {
                            StatCell(
                                label = "လျော်ကြေး (${agent.mult.toInt()} ဆ)",
                                value = fmt(payout),
                                valueColor = if (payout > 0) LuckyDanger else LuckyTextSecondary
                            )
                            val netResult = netAmount - payout
                            StatCell(
                                label = "အရှုံး/အမြတ်",
                                value = "${if (netResult >= 0) "+" else "-"}${fmt(kotlin.math.abs(netResult))}",
                                valueColor = if (netResult >= 0) LuckySuccess else LuckyDanger
                            )
                        }
                    }
                }
            }
        }

        EntryLongPressDropdownMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
fun RowScope.StatCell(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    valueColor: Color? = null
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            color = LuckyTextMuted,
            fontSize = 11.sp,
            maxLines = 1
        )
        Text(
            text = value,
            color = valueColor ?: if (isHighlight) LuckyHeader else LuckyTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
fun AddAgentDialog(
    nextId: Int,
    onDismiss: () -> Unit,
    onAdd: (name: String, rate: Double, mult: Double) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var rateStr by rememberSaveable { mutableStateOf("") }
    var multStr by rememberSaveable { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top section: Header banner with avatar icon and camera badge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ကော်မရှင် အသစ် ထည့်မယ်",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, LuckyHeader, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = LuckyHeader,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Form Container (Dark Green #235347)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ကော် အိုင်ဒီ :",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(105.dp)
                        )
                        Text(
                            text = "$nextId",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ကော် အမည် :",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(105.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = {
                                Text("ရိုက်ထည့်ပါ", color = LuckyTextMuted, fontSize = 13.sp)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = LuckyTextPrimary,
                                unfocusedTextColor = LuckyTextPrimary,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = LuckyBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("add_agent_name_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ကော်မရှင်ခ :",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(105.dp)
                        )
                        OutlinedTextField(
                            value = rateStr,
                            onValueChange = { rateStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = LuckyTextPrimary,
                                unfocusedTextColor = LuckyTextPrimary,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = LuckyBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("add_agent_rate_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "အဆ :",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(105.dp)
                        )
                        OutlinedTextField(
                            value = multStr,
                            onValueChange = { multStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = LuckyTextPrimary,
                                unfocusedTextColor = LuckyTextPrimary,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = LuckyBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .soundOnInputFocus()
                                .testTag("add_agent_mult_input")
                        )
                    }
                }

                // Bottom Action Buttons: "မလုပ်တော့ပါ" (Red #F44336) and "ထည့်မည်" (Green #235347)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("မလုပ်တော့ပါ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val rate = MainViewModel.parseDoubleOrZero(rateStr)
                                val mult = MainViewModel.parseDoubleOrZero(multStr)
                                onAdd(name.trim(), rate, mult)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyHeader,
                            contentColor = Color.White,
                            disabledContainerColor = LuckyBorder,
                            disabledContentColor = LuckyTextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("add_agent_submit_btn")
                    ) {
                        Text(stringResource(R.string.action_add), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EditAgentDialog(
    agent: Agent,
    onDismiss: () -> Unit,
    onSave: (Agent) -> Unit
) {
    val savedRateStr = if (agent.rate > 0) agent.rate.toInt().toString() else ""
    val savedMultStr = if (agent.mult > 0) agent.mult.toInt().toString() else ""

    var name by rememberSaveable(agent.id) { mutableStateOf(agent.name) }
    var rateStr by rememberSaveable(agent.id) { mutableStateOf(savedRateStr) }
    var multStr by rememberSaveable(agent.id) { mutableStateOf(savedMultStr) }

    val hasChanges = name.isNotBlank() && (
        name.trim() != agent.name ||
            rateStr.trim() != savedRateStr ||
            multStr.trim() != savedMultStr
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(LuckyHeader),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("ကော်မရှင် ပြင်မည်", color = LuckyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ကော် အိုင်ဒီ : ${agent.id}",
                    color = LuckyTextSecondary,
                    fontSize = 13.sp
                )

                AppOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .soundOnInputFocus()
                        .testTag("edit_agent_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppOutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text(stringResource(R.string.label_agent_commission_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .soundOnInputFocus()
                            .testTag("edit_agent_rate_input")
                    )

                    AppOutlinedTextField(
                        value = multStr,
                        onValueChange = { multStr = it },
                        label = { Text(stringResource(R.string.label_multiplier)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .soundOnInputFocus()
                            .testTag("edit_agent_mult_input")
                    )
                }
            }
        },
        confirmButton = {
            AnimatedVisibility(visible = hasChanges) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val rate = MainViewModel.parseDoubleOrZero(rateStr)
                            val mult = MainViewModel.parseDoubleOrZero(multStr)
                            onSave(agent.copy(name = name.trim(), rate = rate, mult = mult))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                    modifier = Modifier.testTag("edit_agent_save_btn")
                ) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = LuckyTextSecondary)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
