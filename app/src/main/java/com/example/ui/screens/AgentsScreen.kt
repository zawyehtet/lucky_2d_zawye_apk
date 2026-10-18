package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.ui.components.AppHeader
import com.example.ui.components.SearchPill
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

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

    // Winning number for current date & session
    val winNum = wins.firstOrNull { it.date == selectedDate && it.session == selectedSession }?.n

    // Filter agents by search query
    val filteredAgents = agents.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.id.toString() == searchQuery.trim()
    }

    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "ကော်မရှင်များ (Agents)",
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "နေ့စွဲ : $selectedDate",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "အပိုင်း : $selectedSession",
                        color = LuckyLime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (filteredAgents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ကော်မရှင် မရှိသေးပါ။ ＋ နှိပ်၍ အသစ်ထည့်ပါ",
                                color = LuckyTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredAgents, key = { it.id }) { agent ->
                        // Calculate stats for this agent in this session & date
                        val agentVouchers = vouchers.filter {
                            it.date == selectedDate && it.session == selectedSession && (it.agentId == agent.id || it.agent == agent.name)
                        }
                        val totalBet = agentVouchers.sumOf { it.totalAmount }
                        val commAmount = (totalBet * (agent.rate / 100.0)).toLong()
                        val netAmount = (totalBet - commAmount).coerceAtLeast(0L)
                        val voucherCount = agentVouchers.size

                        // Payout calculation if winning number exists
                        var payout = 0L
                        if (!winNum.isNullOrEmpty()) {
                            val winBet = agentVouchers.flatMap { it.items }.filter { it.n == winNum }.sumOf { it.a }
                            payout = (winBet * agent.mult).toLong()
                        }

                        AgentCard(
                            agent = agent,
                            totalBet = totalBet,
                            commission = commAmount,
                            netAmount = netAmount,
                            voucherCount = voucherCount,
                            payout = payout,
                            onStartBet = {
                                viewModel.startNumberEntry(agent)
                            },
                            onDelete = {
                                viewModel.deleteAgent(agent.id)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Agent
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = LuckyLime,
            contentColor = LuckyBg,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("add_agent_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Agent", modifier = Modifier.size(28.dp))
        }

        // Add Agent Dialog
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
    onDelete: () -> Unit
) {
    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LuckyCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Teal Top Header: ကော် အိုင်ဒီ, အမည်, ＋ button to start betting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyHeader)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LuckyBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${agent.id}",
                            color = LuckyLime,
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
                            color = LuckyTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = LuckyDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // ＋ button starts number entry for this agent
                    Button(
                        onClick = onStartBet,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyLime,
                            contentColor = LuckyBg
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(34.dp).testTag("agent_start_bet_${agent.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "ထိုးမည်", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ထိုးမည်", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Bottom stats grid: စုပေါင်း, ကော်, နုပ်ပြီးငွေ, ဘောင်ချာ, ပေးငွေ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCell(label = "စုပေါင်း", value = "${numFormat.format(totalBet)} Ks")
                    StatCell(label = "ကော်", value = "${numFormat.format(commission)} Ks")
                    StatCell(label = "နုပ်ပြီးငွေ", value = "${numFormat.format(netAmount)} Ks", isHighlight = true)
                }

                Divider(color = LuckyBorder, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCell(label = "ဘောင်ချာ", value = "$voucherCount စောင်")
                    StatCell(
                        label = "ပေးငွေ",
                        value = "${numFormat.format(payout)} Ks",
                        valueColor = if (payout > 0) LuckyDanger else LuckyTextSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
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
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = valueColor ?: if (isHighlight) LuckyLime else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
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
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LuckyHeader),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = LuckyLime)
                }
                Text("ကော်မရှင် အသစ်ထည့်မည်", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ကော် အိုင်ဒီ : $nextId (Auto)",
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
                        focusedBorderColor = LuckyLime,
                        unfocusedBorderColor = LuckyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
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
                        placeholder = { Text("00", color = LuckyTextMuted) },
                        label = { Text("ကော် (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyLime,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("add_agent_rate_input")
                    )

                    OutlinedTextField(
                        value = multStr,
                        onValueChange = { multStr = it },
                        placeholder = { Text("00", color = LuckyTextMuted) },
                        label = { Text("အဆ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyLime,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                        onAdd(name, rate, mult)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg),
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
