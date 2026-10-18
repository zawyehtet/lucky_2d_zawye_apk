package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LayoffItem
import com.example.data.LayoffRecord
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LayoffHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layoffs by viewModel.layoffs.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var selectedDetailRecord by remember { mutableStateOf<LayoffRecord?>(null) }
    var batchToDelete by remember { mutableStateOf<LayoffRecord?>(null) }

    // Item edit / delete inside detail dialog
    var itemToEdit by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }

    val filtered = layoffs.filter { it.date == selectedDate && (selectedSession.isEmpty() || it.session == selectedSession) }
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val grandTotal = filtered.sumOf { it.total }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "တင်ကွက်မှတ်တမ်း (Layoffs)",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Stats card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "နေ့စွဲ : $selectedDate",
                            color = LuckyTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "အပိုင်း : ${if (selectedSession.isEmpty()) "အကြိမ် မရှိသေးပါ" else selectedSession}",
                            color = LuckyLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "မှတ်တမ်းအရေအတွက်",
                            color = LuckyTextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${filtered.size} ခု",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Table of Layoffs
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("စဉ်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(32.dp))
                        Text("အချိန်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                        Text("အပိုင်း", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("တင်ငွေ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }

                    if (filtered.isEmpty()) {
                        EmptyListPlaceholder(
                            text = "အချက်အလက် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(filtered) { idx, record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .recordLongPress(
                                            onClick = { selectedDetailRecord = record },
                                            onLongPress = { batchToDelete = record }
                                        )
                                        .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}.", color = LuckyTextMuted, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                                    Text(record.time.substringAfter("/"), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                                    Text(record.session, color = LuckyLime, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(numFormat.format(record.total), color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                                Divider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }

                    // Footer Total
                    Divider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "စုစုပေါင်း တင်ကွက်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = numFormat.format(grandTotal),
                            color = LuckyLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Delete Batch Confirm Dialog
        batchToDelete?.let { record ->
            DeleteConfirmDialog(
                title = "ဖျက်မည်လား?",
                message = "တင်ကွက်မှတ်တမ်း (အချိန်: ${record.time} ၊ ပမာဏ: ${numFormat.format(record.total)}) ကို ဖျက်မည်လား?",
                onDismiss = { batchToDelete = null },
                onConfirm = {
                    viewModel.deleteLayoffRecord(record.id)
                    batchToDelete = null
                }
            )
        }

        // Detail dialog when tapping a row
        selectedDetailRecord?.let { record ->
            val detailText = buildLayoffDetailText(record)
            AlertDialog(
                onDismissRequest = { selectedDetailRecord = null },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("တင်ကွက် အသေးစိတ်", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { ShareHelper.copyToClipboard(context, detailText) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = LuckyLime)
                            }
                            IconButton(onClick = { ShareHelper.shareText(context, detailText) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                        Text("အချိန် : ${record.time}", color = LuckyTextSecondary, fontSize = 12.sp)
                        Text("ဘရိတ်သတ်မှတ်ချက် : ${numFormat.format(record.limit)}", color = LuckyTextSecondary, fontSize = 12.sp)
                        Text("စုစုပေါင်း : ${numFormat.format(record.total)} (${record.items.size} လုံး)", color = LuckyLime, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Divider(color = LuckyBorder)

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            itemsIndexed(record.items) { i, itm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .recordLongPress(
                                            onClick = {},
                                            onLongPress = {
                                                itemToEdit = Pair(i, itm)
                                            }
                                        )
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${i + 1}. ${itm.n}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(numFormat.format(itm.a), color = LuckyDanger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Divider(color = LuckyBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedDetailRecord = null }) {
                        Text("ပိတ်မည်", color = LuckyLime)
                    }
                }
            )
        }

        // Action / Edit Layoff Item inside Detail View
        itemToEdit?.let { (idx, itm) ->
            var newAmountStr by remember { mutableStateOf(itm.a.toString()) }
            var showItemDeleteConfirm by remember { mutableStateOf(false) }

            if (showItemDeleteConfirm) {
                DeleteConfirmDialog(
                    title = "ဖျက်မည်လား?",
                    message = "${itm.n} = ${numFormat.format(itm.a)} ကို ဖျက်မည်လား?",
                    onDismiss = { showItemDeleteConfirm = false },
                    onConfirm = {
                        selectedDetailRecord?.let { rec ->
                            viewModel.deleteLayoffItem(rec.id, idx)
                            selectedDetailRecord = viewModel.layoffs.value.firstOrNull { it.id == rec.id }
                        }
                        showItemDeleteConfirm = false
                        itemToEdit = null
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { itemToEdit = null },
                    containerColor = LuckyCard,
                    shape = RoundedCornerShape(18.dp),
                    title = {
                        Text("တင်ကွက် ပြင်ဆင်မည် (${itm.n})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newAmountStr,
                                onValueChange = { newAmountStr = it.filter { c -> c.isDigit() } },
                                label = { Text("ပမာဏ (Amount)", color = LuckyTextSecondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuckyLime,
                                    unfocusedBorderColor = LuckyBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = newAmountStr.toLongOrNull() ?: 0L
                                if (amt <= 0) {
                                    viewModel.showToast("ပမာဏ မှားယွင်းနေပါသည်")
                                    return@Button
                                }
                                selectedDetailRecord?.let { rec ->
                                    viewModel.editLayoffItem(rec.id, idx, amt)
                                    selectedDetailRecord = viewModel.layoffs.value.firstOrNull { it.id == rec.id }
                                }
                                itemToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuckyLime, contentColor = LuckyBg)
                        ) {
                            Text("ပြင်ဆင်မည်", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { showItemDeleteConfirm = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = LuckyDanger)
                            ) {
                                Text("ဖျက်မည်")
                            }
                            TextButton(onClick = { itemToEdit = null }) {
                                Text("မလုပ်ပါ", color = LuckyTextSecondary)
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun buildLayoffDetailText(record: LayoffRecord): String {
    val sb = StringBuilder()
    sb.appendLine("=== Lucky2d တင်ကွက်မှတ်တမ်း ===")
    sb.appendLine("အချိန် : ${record.time}")
    sb.appendLine("အပိုင်း : ${record.session}")
    sb.appendLine("ဘရိတ် : ${record.limit}")
    sb.appendLine("--------------------------")
    record.items.forEachIndexed { i, itm ->
        sb.appendLine("${i + 1}. ${itm.n} = ${itm.a}")
    }
    sb.appendLine("--------------------------")
    sb.appendLine("စုစုပေါင်း တင်ငွေ : ${record.total}")
    sb.appendLine("==========================")
    return sb.toString()
}
