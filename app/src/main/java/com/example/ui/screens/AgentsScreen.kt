package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var agentToEdit by remember { mutableStateOf<Agent?>(null) }
    var agentToDelete by remember { mutableStateOf<Agent?>(null) }

    val winNum = wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n

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
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
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
                        text = "အကြိမ် : $selectedSession",
                        color = LuckyHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SearchPill(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ကော်မရှင် အမည် သို့မဟုတ် အိုင်ဒီ ရှာရန်...",
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
                        val summary = remember(agent, vouchers, wins, selectedDate, selectedSession) {
                            viewModel.getAgentSummaryData(agent, winNum ?: "")
                        }
                        AgentCard(
                            agent = agent,
                            totalBet = summary.totalBet,
                            commission = summary.commission,
                            netAmount = summary.netAmount,
                            voucherCount = summary.voucherCount,
                            payout = summary.payout,
                            onStartBet = {
                                viewModel.startNumberEntry(agent)
                            },
                            onEdit = {
                                agentToEdit = agent
                            },
                            onDelete = {
                                agentToDelete = agent
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
                onDismiss = { agentToEdit = null },
                onSave = { updated ->
                    viewModel.updateAgent(updated)
                    agentToEdit = null
                }
            )
        }

        agentToDelete?.let { ag ->
            DeleteConfirmDialog(
                itemName = ag.name,
                onDismiss = { agentToDelete = null },
                onConfirm = {
                    viewModel.deleteAgent(ag.id)
                    agentToDelete = null
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
    onStartBet: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    if (showMenu) {
        RecordActionMenuDialog(
            title = "${agent.name} (ရောင်းစာရေး)",
            onDismiss = { showMenu = false },
            onEdit = onEdit,
            onDelete = onDelete
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LuckyCard),
        modifier = Modifier
            .fillMaxWidth()
            .recordLongPress(onLongPress = { showMenu = true })
            .border(1.dp, LuckyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Blue Top Header
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "ဖျက်မည်",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onStartBet,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = LuckyHeader
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("agent_start_bet_${agent.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "ထိုးမည်", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ထိုးမည်", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
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
    var name by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var multStr by remember { mutableStateOf("") }

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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("ရိုက်ထည့်ပါ", color = LuckyTextMuted) },
                    label = { Text("အမည်") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuckyHeader,
                        unfocusedBorderColor = LuckyBorder,
                        focusedTextColor = LuckyTextPrimary,
                        unfocusedTextColor = LuckyTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_agent_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        placeholder = { Text("15", color = LuckyTextMuted) },
                        label = { Text("ကော် (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("add_agent_rate_input")
                    )

                    OutlinedTextField(
                        value = multStr,
                        onValueChange = { multStr = it },
                        placeholder = { Text("80", color = LuckyTextMuted) },
                        label = { Text("အဆ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("add_agent_mult_input")
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
                        onAdd(name.trim(), rate, mult)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                modifier = Modifier.testTag("add_agent_submit_btn")
            ) {
                Text("ထည့်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
            ) {
                Text("မလုပ်တော့ပါ")
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
    var name by remember { mutableStateOf(agent.name) }
    var rateStr by remember { mutableStateOf(if (agent.rate > 0) agent.rate.toInt().toString() else "") }
    var multStr by remember { mutableStateOf(if (agent.mult > 0) agent.mult.toInt().toString() else "") }

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
                Text("ရောင်းစာရေး ပြင်ဆင်မည်", color = LuckyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("အမည်") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuckyHeader,
                        unfocusedBorderColor = LuckyBorder,
                        focusedTextColor = LuckyTextPrimary,
                        unfocusedTextColor = LuckyTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("ကော် (%)") },
                        placeholder = { Text("15", color = LuckyTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = multStr,
                        onValueChange = { multStr = it },
                        label = { Text("အဆ") },
                        placeholder = { Text("80", color = LuckyTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val rate = rateStr.toDoubleOrNull() ?: 0.0
                        val mult = multStr.toDoubleOrNull() ?: 0.0
                        onSave(agent.copy(name = name.trim(), rate = rate, mult = mult))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White)
            ) {
                Text("သိမ်းမည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = LuckyTextSecondary)
            ) {
                Text("မလုပ်တော့ပါ")
            }
        }
    )
}
