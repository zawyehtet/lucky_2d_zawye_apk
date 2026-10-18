package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BrakeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentBrakeInput by viewModel.currentBrakeInput.collectAsState()

    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    val limit = currentBrakeInput.toLongOrNull() ?: 50000L
    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers()
    }

    // Split numbers calculation:
    // LEFT = retained (min(total, limit))
    // RIGHT = layoff (total - limit where total > limit)
    val leftRetainedList = summaries.map { summary ->
        val retained = minOf(summary.netAmount, limit)
        summary.n to retained
    }.filter { it.second > 0 }

    val rightLayoffList = summaries.mapNotNull { summary ->
        if (summary.netAmount > limit) {
            summary.n to (summary.netAmount - limit)
        } else null
    }

    val totalRetained = leftRetainedList.sumOf { it.second }
    val totalLayoff = rightLayoffList.sumOf { it.second }

    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "တင်ကွက်များ (Brake / Layoff)",
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) }
            )

            // Header input "ဘရိတ် :", date and session
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ဘရိတ် :",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = currentBrakeInput,
                        onValueChange = { viewModel.updateBrakeLimit(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyLime,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = LuckyCard,
                            unfocusedContainerColor = LuckyCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("brake_limit_input")
                    )

                    Button(
                        onClick = { viewModel.saveBrakeLimit() },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = LuckyLime, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("သိမ်းမည်", fontSize = 12.sp)
                    }
                }
            }

            // Split screen on a dark card:
            // LEFT = retained (min(total, limit))
            // RIGHT = layoff (total - limit where total > limit)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Split Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "လက်ခံကွက် (Retained)",
                            color = LuckyLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(LuckyBorder))
                        Text(
                            text = "တင်ကွက် (Layoff)",
                            color = LuckyDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Split Body Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left List: Retained
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (leftRetainedList.isEmpty()) {
                                item {
                                    Text(
                                        text = "မရှိပါ",
                                        color = LuckyTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                items(leftRetainedList) { (num, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(num, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(numFormat.format(amt), color = LuckyLime, fontSize = 13.sp)
                                    }
                                    Divider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
                            }
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(LuckyBorder)
                        )

                        // Right List: Layoff
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (rightLayoffList.isEmpty()) {
                                item {
                                    Text(
                                        text = "ဘရိတ်ကျော် မရှိပါ",
                                        color = LuckyTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                items(rightLayoffList) { (num, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(num, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(numFormat.format(amt), color = LuckyDanger, fontSize = 13.sp)
                                    }
                                    Divider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // Total Summaries
                    Divider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCardDark)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "စုစုပေါင်း: ${numFormat.format(totalRetained)}",
                            color = LuckyLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "တင်ရန်: ${numFormat.format(totalLayoff)}",
                            color = LuckyDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Action Buttons:
                    // Left buttons: "＋ သိမ်းမည်", "ရက်ချုပ်"
                    // Right buttons: "＋ တင်မည်", "မှတ်တမ်းများ"
                    Divider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCard)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left Column Buttons
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.saveBrakeLimit() },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("＋ သိမ်း", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(Screen.SETTLEMENT) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("goto_settlement_btn")
                            ) {
                                Text("ရက်ချုပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Right Column Buttons
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { showConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("open_layoff_dialog_btn")
                            ) {
                                Text("＋ တင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(Screen.LAYOFF_HISTORY) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("goto_layoff_history_btn")
                            ) {
                                Text("မှတ်တမ်း", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }

        // Layoff Confirm Dialog:
        // "တင်ကွက်များ ✕ — တင်မယ်ဆိုတာ သေချာပါသလား" with "(ALL) တင်မည်" and "(30) တင်မည်"
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "တင်ကွက်များ ✕ — တင်မယ်ဆိုတာ သေချာပါသလား",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "ဘရိတ် (${limit}) ထက် ကျော်လွန်နေသော တင်ကွက်များကို စာရင်းသွင်းမည်။\nတင်ရန် စုစုပေါင်း: ${numFormat.format(totalLayoff)} (${rightLayoffList.size} လုံး)",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.executeLayoff(isAll = false)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = LuckyLime),
                            modifier = Modifier.testTag("layoff_30_btn")
                        ) {
                            Text("(30) တင်မည်", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.executeLayoff(isAll = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                            modifier = Modifier.testTag("layoff_all_btn")
                        ) {
                            Text("(ALL) တင်မည်", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = LuckyTextMuted)
                    ) {
                        Text("မလုပ်တော့ပါ")
                    }
                }
            )
        }
    }
}
