package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.fmt
import com.example.ui.theme.*

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

    val leftRetainedList = summaries.map { summary ->
        val retained = minOf(summary.netAmount, limit)
        Triple(summary.n, summary.entryCount, retained)
    }.filter { it.third > 0 }

    val rightLayoffList = summaries.mapNotNull { summary ->
        if (summary.netAmount > limit) {
            Triple(summary.n, summary.entryCount, summary.netAmount - limit)
        } else null
    }

    val totalRetained = leftRetainedList.sumOf { it.third }
    val totalLayoff = rightLayoffList.sumOf { it.third }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "တင်ကွက်များ",
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) },
                onBack = { viewModel.goBack() }
            )

            // Header input "ဘရိတ် :", date and session
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ဘရိတ် :",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )

                    OutlinedTextField(
                        value = currentBrakeInput,
                        onValueChange = { viewModel.updateBrakeLimit(it.filter { c -> c.isDigit() }) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuckyHeader,
                            unfocusedBorderColor = LuckyBorder,
                            focusedTextColor = LuckyTextPrimary,
                            unfocusedTextColor = LuckyTextPrimary,
                            focusedContainerColor = LuckyCard,
                            unfocusedContainerColor = LuckyCard
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("brake_limit_input")
                    )

                    Button(
                        onClick = { viewModel.saveBrakeLimit() },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("သိမ်းမည်", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }

            // Full-Width Split Table Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
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
                            text = "လက်ခံကွက် (${leftRetainedList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.5f)))
                        Text(
                            text = "တင်ကွက် (${rightLayoffList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Split Subheaders (ဂဏန်း | အကြိမ် | ပမာဏ)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCardDark)
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဂဏန်း", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("အကြိမ်", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("ပမာဏ", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(LuckyBorder))
                        Row(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဂဏန်း", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("အကြိမ်", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Text("ပမာဏ", color = LuckyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    HorizontalDivider(color = LuckyBorder)

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
                                itemsIndexed(leftRetainedList) { idx, (num, cnt, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                            .padding(vertical = 7.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(num, color = LuckyHeader, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                        Text("$cnt", color = LuckyTextMuted, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        Text(fmt(amt), color = LuckySuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                                    }
                                    HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
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
                                itemsIndexed(rightLayoffList) { idx, (num, cnt, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                            .padding(vertical = 7.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(num, color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                                        Text("$cnt", color = LuckyTextMuted, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        Text(fmt(amt), color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                                    }
                                    HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // Total Summaries
                    HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCardDark)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "စုစုပေါင်း: ${fmt(totalRetained)}",
                            color = LuckySuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "တင်ရန်: ${fmt(totalLayoff)}",
                            color = LuckyDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Action Buttons aligned cleanly across the full table width
                    HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyCard)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left Column Buttons aligned with Left Table
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.saveBrakeLimit() },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("သိမ်းမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(Screen.SETTLEMENT) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckySuccess, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("goto_settlement_btn")
                            ) {
                                Text("ရက်ချုပ်", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }

                        // Right Column Buttons aligned with Right Table
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { showConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyDanger, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("open_layoff_dialog_btn")
                            ) {
                                Text("တင်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(Screen.LAYOFF_HISTORY) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("goto_layoff_history_btn")
                            ) {
                                Text("မှတ်တမ်း", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // Layoff Confirm Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        text = "တင်ကွက်များ တင်ရန် သေချာပါသလား?",
                        color = LuckyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "ဘရိတ် (${fmt(limit)}) ထက် ကျော်လွန်နေသော တင်ကွက်များကို စာရင်းသွင်းမည်။\nတင်ရန် စုစုပေါင်း: ${fmt(totalLayoff)} (${rightLayoffList.size} လုံး)",
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
                        OutlinedButton(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.executeLayoff(isAll = false)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LuckyHeader),
                            modifier = Modifier.testTag("layoff_30_btn")
                        ) {
                            Text("၃၀ လုံး တင်မည်", fontWeight = FontWeight.Bold)
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
                            Text("အားလုံး တင်မည်", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = LuckyTextSecondary)
                    ) {
                        Text("မလုပ်တော့ပါ")
                    }
                }
            )
        }
    }
}
