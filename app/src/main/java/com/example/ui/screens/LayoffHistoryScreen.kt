package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LayoffRecord
import com.example.ui.MainViewModel
import com.example.ui.components.ShareHelper
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LayoffHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val layoffs by viewModel.layoffs.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current

    var selectedDetailRecord by remember { mutableStateOf<LayoffRecord?>(null) }

    BackHandler {
        viewModel.goBack()
    }

    val numFormat = NumberFormat.getNumberInstance(Locale.US)
    val filteredLayoffs = layoffs.filter {
        it.date == selectedDate && it.session == selectedSession
    }
    val grandTotal = filteredLayoffs.sumOf { it.total }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                            text = "တင်ကွက် ဝယ်ရောင်း မှတ်တမ်းများ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
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

            // Table: စဉ် | အချိန် | အပိုင်း | ပမာဏ and footer total
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(16.dp))
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
                        Text("ပမာဏ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }

                    if (filteredLayoffs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "တင်ကွက်မှတ်တမ်း မရှိသေးပါ",
                                color = LuckyTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            itemsIndexed(filteredLayoffs) { idx, record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedDetailRecord = record }
                                        .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}.", color = LuckyTextMuted, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                                    Text(record.time.substringAfter("/"), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                                    Text(record.session, color = LuckyLime, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("${numFormat.format(record.total)} Ks", color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
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
                            text = "${numFormat.format(grandTotal)} Ks",
                            color = LuckyLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
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
                        Text("ဘရိတ်သတ်မှတ်ချက် : ${numFormat.format(record.limit)} Ks", color = LuckyTextSecondary, fontSize = 12.sp)
                        Text("စုစုပေါင်း : ${numFormat.format(record.total)} Ks (${record.items.size} လုံး)", color = LuckyLime, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Divider(color = LuckyBorder)

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            itemsIndexed(record.items) { i, itm ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${i + 1}. ${itm.n}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${numFormat.format(itm.a)} Ks", color = LuckyDanger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
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
        sb.appendLine("${i + 1}. ${itm.n} = ${itm.a} Ks")
    }
    sb.appendLine("--------------------------")
    sb.appendLine("စုစုပေါင်း တင်ငွေ : ${record.total} Ks")
    sb.appendLine("==========================")
    return sb.toString()
}
