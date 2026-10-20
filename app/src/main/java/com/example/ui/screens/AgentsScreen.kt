package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
                        AgentCard(
                            agent = agent,
                            totalBet = summary.totalBet,
                            commission = summary.commission,
                            netAmount = summary.netAmount,
                            voucherCount = summary.voucherCount,
                            payout = summary.payout,
                            canStartBet = hasRound,
                            onStartBet = {
                                viewModel.startNumberEntry(agent)
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
    canStartBet: Boolean = true,
    onStartBet: () -> Unit,
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
                        StatCell(label = "ရောင်းကော်", value = fmt(commission))
                        StatCell(label = "နုပ်ပြီးငွေ", value = fmt(netAmount), isHighlight = true)
                    }

                    HorizontalDivider(color = LuckyBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell(label = "ဘောင်ချာ", value = "$voucherCount စောင်")
                        StatCell(
                            label = "ပေါက်သီးကြေး",
                            value = fmt(payout),
                            valueColor = if (payout > 0) LuckyDanger else LuckyTextSecondary
                        )
                        val netResult = netAmount - payout
                        StatCell(
                            label = if (netResult >= 0) "ရရန်" else "ပေးရန်",
                            value = "${if (netResult >= 0) "+" else "-"}${fmt(kotlin.math.abs(netResult))}",
                            valueColor = if (netResult >= 0) LuckySuccess else LuckyDanger
                        )
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
                Text("ကော်မရှင် အသစ်ထည့်မည်", color = LuckyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ကော် အိုင်ဒီ : $nextId",
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
                        .testTag("add_agent_name_input")
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
                            .testTag("add_agent_rate_input")
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
                            .testTag("add_agent_mult_input")
                    )
                }
            }
        },
        confirmButton = {
            AnimatedVisibility(visible = name.isNotBlank()) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val rate = MainViewModel.parseDoubleOrZero(rateStr)
                            val mult = MainViewModel.parseDoubleOrZero(multStr)
                            onAdd(name.trim(), rate, mult)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                    modifier = Modifier.testTag("add_agent_submit_btn")
                ) {
                    Text(stringResource(R.string.action_add), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
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
