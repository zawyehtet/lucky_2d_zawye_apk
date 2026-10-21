package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.LayoffItem
import com.example.data.LayoffRecord
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

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

    val hasRound = sessions.isNotEmpty() && selectedSession.isNotBlank() && sessions.any { it.name == selectedSession }

    var selectedDetailRecord by remember { mutableStateOf<LayoffRecord?>(null) }
    var batchToDelete by remember { mutableStateOf<LayoffRecord?>(null) }
    var itemActionTarget by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }
    var itemToEdit by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<Int, LayoffItem>?>(null) }

    LaunchedEffect(layoffs) {
        selectedDetailRecord?.let { current ->
            selectedDetailRecord = layoffs.firstOrNull { it.id == current.id }
        }
    }

    val filtered = if (!hasRound) {
        emptyList()
    } else {
        layoffs.filter { it.date == selectedDate && it.session == selectedSession }
    }
    val grandTotal = filtered.sumOf { it.total }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "တင်ကွက်မှတ်တမ်း",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) },
            onBack = { viewModel.goBack() },
            actions = {
                Button(
                    onClick = { viewModel.exportCurrentSessionLedgerToFile(context, launchShare = true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LuckyHeader
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("export_layoff_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoundRequiredHelperBanner(visible = !hasRound)

            // Header Stats Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            text = "အကြိမ် : ${if (selectedSession.isEmpty()) "အကြိမ် မရှိသေးပါ" else selectedSession}",
                            color = LuckyHeader,
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
                            color = LuckyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Full-Width Table of Layoffs
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("စဉ်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.6f))
                        Text("အချိန်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.4f))
                        Text("အကြိမ်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("တင်ငွေ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }

                    if (filtered.isEmpty()) {
                        EmptyListPlaceholder(
                            text = "တင်ကွက်မှတ်တမ်း မရှိသေးပါ",
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
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}.", color = LuckyTextMuted, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.6f))
                                    Text(record.time.substringAfter("/"), color = LuckyTextPrimary, fontSize = 13.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.4f))
                                    Text(record.session, color = LuckyHeader, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(fmt(record.total), color = LuckyDanger, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = LuckyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }

                    // Footer Total
                    HorizontalDivider(color = LuckyBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuckyHeader)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            text = fmt(grandTotal),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Delete Batch Confirm Dialog
        batchToDelete?.let { record ->
            DeleteConfirmDialog(
                title = "ဖျက်ရန် သေချာပါသလား?",
                message = "တင်ကွက်မှတ်တမ်း (အချိန်: ${record.time} ၊ ပမာဏ: ${fmt(record.total)}) ကို ဖျက်ရန် သေချာပါသလား?",
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
                shape = RoundedCornerShape(14.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("တင်ကွက် အသေးစိတ်", color = LuckyTextPrimary, fontSize = 16.sp, lineHeight = 1.5.em, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { ShareHelper.copyToClipboard(context, detailText) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "ကူးယူရန်", tint = LuckyHeader)
                            }
                            IconButton(onClick = { ShareHelper.shareText(context, detailText) }) {
                                Icon(Icons.Default.Share, contentDescription = "မျှဝေရန်", tint = LuckyHeader)
                            }
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        Text("အချိန် : ${record.time}", color = LuckyTextSecondary, fontSize = 12.sp, lineHeight = 1.5.em)
                        Text("ဘရိတ်သတ်မှတ်ချက် : ${fmt(record.limit)}", color = LuckyTextSecondary, fontSize = 12.sp, lineHeight = 1.5.em)
                        Text("စုစုပေါင်း : ${fmt(record.total)} (${record.items.size} လုံး)", color = LuckyHeader, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 1.5.em)

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = LuckyBorder)

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            itemsIndexed(record.items) { i, itm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .recordLongPress(
                                            onClick = {},
                                            onLongPress = {
                                                viewModel.onEditActionStarted()
                                                itemActionTarget = Pair(i, itm)
                                            }
                                        )
                                        .padding(vertical = 8.dp)
                                        .testTag("layoff_detail_row_$i"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${i + 1}. ${itm.n}", color = LuckyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(fmt(itm.a), color = LuckyDanger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                HorizontalDivider(color = LuckyBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedDetailRecord = null }) {
                        Text("ပိတ်မည်", color = LuckyHeader, fontWeight = FontWeight.Bold, lineHeight = 1.5.em)
                    }
                }
            )
        }

        // 1. Long-press Action Sheet / Dialog offering 2 distinct actions: "ပြင်မည်" and "ဖျက်မည်"
        itemActionTarget?.let { (idx, itm) ->
            RecordActionMenuDialog(
                title = "${itm.n} = ${fmt(itm.a)}",
                onDismiss = { itemActionTarget = null },
                onEdit = {
                    itemToEdit = Pair(idx, itm)
                },
                onDelete = {
                    itemToDelete = Pair(idx, itm)
                }
            )
        }

        // 2. Delete Confirmation Dialog for a single Layoff Item
        itemToDelete?.let { (idx, itm) ->
            DeleteConfirmDialog(
                itemName = "${itm.n} = ${fmt(itm.a)}",
                onDismiss = { itemToDelete = null },
                onConfirm = {
                    selectedDetailRecord?.let { rec ->
                        viewModel.deleteLayoffItem(rec.id, idx)
                        selectedDetailRecord = viewModel.layoffs.value.firstOrNull { it.id == rec.id }
                    }
                    itemToDelete = null
                }
            )
        }

        // 3. Edit Layoff Item Dialog ("တင်ကွက် ပြင်ဆင်ရန်") with BOTH Number (ဂဏန်း) and Amount (ငွေပမာဏ)
        itemToEdit?.let { (idx, itm) ->
            val savedNumStr = itm.n
            val savedAmtStr = if (itm.a > 0L) itm.a.toString() else ""
            var newNumberStr by rememberSaveable(idx, itm.n, itm.a) { mutableStateOf(savedNumStr) }
            var newAmountStr by rememberSaveable(idx, itm.n, itm.a) { mutableStateOf(savedAmtStr) }

            AlertDialog(
                onDismissRequest = { itemToEdit = null },
                containerColor = LuckyCard,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        text = "တင်ကွက် ပြင်ဆင်ရန်",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 1.5.em,
                        modifier = Modifier.testTag("edit_layoff_dialog_title")
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppOutlinedTextField(
                            value = newNumberStr,
                            onValueChange = { newNumberStr = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("ဂဏန်း", lineHeight = 1.5.em) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundOnInputFocus()
                                .testTag("edit_layoff_number_input")
                        )
                        AppOutlinedTextField(
                            value = newAmountStr,
                            onValueChange = { newAmountStr = it.filter { c -> c.isDigit() } },
                            label = { Text("ငွေပမာဏ", lineHeight = 1.5.em) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundOnInputFocus()
                                .testTag("edit_layoff_amount_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanNum = newNumberStr.trim().filter { it.isDigit() }
                            val amt = MainViewModel.parseAmountOrZero(newAmountStr)
                            if (cleanNum.length !in 2..3) {
                                viewModel.showToast("ဂဏန်း မှားယွင်းနေပါသည်")
                                return@Button
                            }
                            if (amt <= 0L) {
                                viewModel.showToast("ပမာဏ မှားယွင်းနေပါသည်")
                                return@Button
                            }
                            selectedDetailRecord?.let { rec ->
                                if (viewModel.editLayoffItem(rec.id, idx, cleanNum, amt)) {
                                    selectedDetailRecord = viewModel.layoffs.value.firstOrNull { it.id == rec.id }
                                    itemToEdit = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyHeader, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_edit_layoff_btn")
                    ) {
                        Text(
                            text = "သိမ်းဆည်းမည်",
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.5.em
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { itemToEdit = null },
                        modifier = Modifier.testTag("cancel_edit_layoff_btn")
                    ) {
                        Text(
                            text = "မလုပ်တော့ပါ",
                            color = LuckyTextSecondary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.5.em
                        )
                    }
                }
            )
        }
    }
}

private fun buildLayoffDetailText(record: LayoffRecord): String {
    val sum = record.items.sumOf { it.a }
    val lines = record.items.map { "${it.n}=${fmt(it.a)}" } + fmt(sum)
    return lines.joinToString("\n")
}
