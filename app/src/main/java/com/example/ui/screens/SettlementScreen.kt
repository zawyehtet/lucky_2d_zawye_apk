package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SettlementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val settledList by viewModel.settled.collectAsState()

    var winNumberInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedDate, selectedSession) {
        val existingWin = viewModel.getWinningNumberForCurrentSession()
        winNumberInput = existingWin
    }

    BackHandler {
        viewModel.goBack()
    }

    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    // Calculate settlement values
    val calc = remember(winNumberInput, selectedDate, selectedSession) {
        viewModel.calculateSettlement(winNumberInput.trim())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        Surface(
            color = LuckyHeader,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { viewModel.goBack() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "ရက်ချုပ် (Settlement)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Text(
                    text = "$selectedDate ($selectedSession)",
                    color = LuckyLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Winning Number Input Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ထွက်ဂဏန်း (Winning Number)",
                            color = LuckyTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = winNumberInput,
                                onValueChange = {
                                    if (it.length <= 2) {
                                        winNumberInput = it
                                        if (it.length == 2) {
                                            viewModel.setWinningNumber(it)
                                        }
                                    }
                                },
                                placeholder = { Text("00", color = LuckyTextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(90.dp).testTag("winning_number_input")
                            )

                            if (winNumberInput.length == 2) {
                                Text(
                                    text = "ထွက်ဂဏန်းပေါ် လက်ခံငွေ: ${numFormat.format(calc.winningRetained)} Ks",
                                    color = LuckyTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Calculation Breakdown Card
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
                            text = "စာရင်း အကျဉ်းချုပ်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        SettlementRow("စုစုပေါင်း လက်ခံငွေ (Total Retained)", "${numFormat.format(calc.totalRetained)} Ks", Color.White)
                        SettlementRow("ကော်မရှင် (Commission)", "-${numFormat.format(calc.commission)} Ks", LuckyDanger)
                        Divider(color = LuckyBorder, thickness = 0.5.dp)
                        SettlementRow("နုပ်ပြီးငွေ (Retained Net)", "${numFormat.format(calc.retainedNet)} Ks", LuckyLime)
                        SettlementRow("လျော်ကြေး (Payout ${calc.multiplier.toInt()}x)", "-${numFormat.format(calc.payout)} Ks", LuckyDanger)
                        Divider(color = LuckyBorder, thickness = 1.dp)

                        // Profit / Loss
                        val plColor = if (calc.profitLoss >= 0) LuckyLime else LuckyDanger
                        val plLabel = if (calc.profitLoss >= 0) "အမြတ် (Profit)" else "အရှုံး (Loss)"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plLabel,
                                color = plColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${numFormat.format(calc.profitLoss)} Ks",
                                color = plColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button "စာရင်းချုပ်မည်"
                        Button(
                            onClick = {
                                viewModel.commitSettlement(calc)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("commit_settlement_btn")
                        ) {
                            Text("စာရင်းချုပ်မည် (Settle Ledger)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Settled Records Ledger
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စာရင်းချုပ် မှတ်တမ်းများ (Ledger)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (settledList.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearSettledLedger() },
                            colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ရှင်းထုတ်မည်", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (settledList.isEmpty()) {
                item {
                    Text(
                        text = "စာရင်းချုပ်မှတ်တမ်း မရှိသေးပါ",
                        color = LuckyTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                itemsIndexed(settledList) { idx, item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LuckyCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, LuckyBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.date} (${item.session})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "ထွက်ဂဏန်း: ${item.winningNumber.ifEmpty { "-" }}",
                                    color = LuckyLime,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("နုပ်ပြီးငွေ: ${numFormat.format(item.retainedNet)} Ks", color = LuckyTextSecondary, fontSize = 12.sp)
                                Text("လျော်ငွေ: ${numFormat.format(item.payout)} Ks", color = LuckyDanger, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("အချိန်: ${item.timestamp}", color = LuckyTextMuted, fontSize = 11.sp)
                                val plCol = if (item.profitLoss >= 0) LuckyLime else LuckyDanger
                                Text(
                                    text = "ရလဒ်: ${numFormat.format(item.profitLoss)} Ks",
                                    color = plCol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = LuckyTextSecondary, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
