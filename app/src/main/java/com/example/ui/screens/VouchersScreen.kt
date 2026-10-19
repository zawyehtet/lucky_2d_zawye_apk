@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Voucher
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.SearchPill
import com.example.ui.components.ShareHelper
import com.example.ui.components.fmt
import com.example.ui.components.recordLongPress
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VouchersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val blocked by viewModel.blocked.collectAsState()

    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    var editingVoucherId by remember { mutableStateOf<Long?>(null) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingInitialNumber by remember { mutableStateOf("") }
    var editingInitialAmount by remember { mutableStateOf(0L) }

    val filteredVouchers = vouchers.filter { v ->
        v.date == selectedDate && v.session == selectedSession &&
            (searchQuery.isBlank() ||
                v.agent.contains(searchQuery, ignoreCase = true) ||
                v.no.toString() == searchQuery.trim() ||
                v.items.any { it.n == searchQuery.trim() })
    }

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "ဘောင်ချာများ",
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
                        text = "နေ့စွဲ : $selectedDate ($selectedSession)",
                        color = LuckyTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "စုစုပေါင်း ဘောင်ချာ : ${filteredVouchers.size} စောင်",
                        color = LuckyHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SearchPill(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ဘောင်ချာအမှတ် သို့မဟုတ် ထိုးသူ/ဂဏန်း ရှာရန်...",
                    modifier = Modifier.testTag("voucher_search_input")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredVouchers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ဘောင်ချာ မှတ်တမ်း မရှိသေးပါ",
                                color = LuckyTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredVouchers, key = { it.id }) { voucher ->
                        VoucherCard(
                            voucher = voucher,
                            onLineLongPress = { itemIndex, item ->
                                editingVoucherId = voucher.id
                                editingItemIndex = itemIndex
                                editingInitialNumber = item.n
                                editingInitialAmount = item.a
                            },
                            onDeleteVoucher = {
                                voucherToDelete = voucher
                            },
                            onCopy = {
                                val text = buildReceiptText(voucher)
                                ShareHelper.copyToClipboard(context, text)
                            },
                            onShare = {
                                val text = buildReceiptText(voucher)
                                ShareHelper.shareText(context, text)
                            },
                            onPrint = {
                                val text = buildReceiptText(voucher)
                                ShareHelper.printText(context, text)
                            }
                        )
                    }
                }
            }
        }

        voucherToDelete?.let { v ->
            DeleteConfirmDialog(
                itemName = "ဘောင်ချာ အမှတ် ${v.no}",
                onDismiss = { voucherToDelete = null },
                onConfirm = {
                    viewModel.deleteVoucher(v.id)
                    voucherToDelete = null
                }
            )
        }

        if (editingVoucherId != null && editingItemIndex != null) {
            EditBetItemDialog(
                initialNumber = editingInitialNumber,
                initialAmount = editingInitialAmount,
                maxBet = settings.maxBet,
                blocked = blocked,
                onDismiss = {
                    editingVoucherId = null
                    editingItemIndex = null
                },
                onSave = { newNum, newAmt ->
                    val vId = editingVoucherId ?: return@EditBetItemDialog
                    val idx = editingItemIndex ?: return@EditBetItemDialog
                    viewModel.editVoucherItem(vId, idx, newNum, newAmt)
                    editingVoucherId = null
                    editingItemIndex = null
                },
                onDelete = {
                    val vId = editingVoucherId ?: return@EditBetItemDialog
                    val idx = editingItemIndex ?: return@EditBetItemDialog
                    viewModel.deleteVoucherItem(vId, idx)
                    editingVoucherId = null
                    editingItemIndex = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoucherCard(
    voucher: Voucher,
    onLineLongPress: (Int, com.example.data.BetItem) -> Unit,
    onDeleteVoucher: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LuckyCard),
        modifier = Modifier
            .fillMaxWidth()
            .recordLongPress(onLongPress = onDeleteVoucher)
            .border(1.dp, LuckyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyHeader)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "အမှတ် ${voucher.no}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "အချိန် : ${voucher.time}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDeleteVoucher,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "ဖျက်မည်",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Agent Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(LuckyHeader),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = voucher.agent.take(1),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "အိုင်ဒီ : ${voucher.agentId} , အမည် : ${voucher.agent}",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "${voucher.items.size} ကွက်",
                    color = LuckyTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = LuckyBorder, thickness = 0.5.dp)

            // Full-Width Bet Items Table (စဉ် | ဂဏန်း | ပမာဏ)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCard)
            ) {
                voucher.items.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .recordLongPress(
                                onLongPress = { onLineLongPress(idx, item) }
                            )
                            .background(if (idx % 2 == 0) LuckyCard else LuckyCardDark.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            color = LuckyTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(0.6f)
                        )
                        Text(
                            text = item.n,
                            color = LuckyHeader,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = fmt(item.a),
                            color = LuckyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                    if (idx < voucher.items.lastIndex) {
                        HorizontalDivider(color = LuckyBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }

            HorizontalDivider(color = LuckyBorder, thickness = 1.dp)

            // Footer: စုစုပေါင်း : total with copy, share, print icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardDark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "စုစုပေါင်း : ${fmt(voucher.totalAmount)}",
                    color = LuckySuccess,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "ကူးယူရန်", tint = LuckyHeader, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "မျှဝေရန်", tint = LuckyHeader, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "ပရင့်ထုတ်ရန်", tint = LuckyHeader, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun buildReceiptText(voucher: Voucher): String {
    val sb = StringBuilder()
    sb.appendLine("=== 2D ဘောင်ချာ ===")
    sb.appendLine("အမှတ် : ${voucher.no}")
    sb.appendLine("နေ့စွဲ : ${voucher.date} (${voucher.session})")
    sb.appendLine("အချိန် : ${voucher.time}")
    sb.appendLine("ထိုးသူ : ${voucher.agent} (အိုင်ဒီ: ${voucher.agentId})")
    sb.appendLine("--------------------------")
    voucher.items.forEach { itm ->
        sb.appendLine("${itm.n} = ${fmt(itm.a)}")
    }
    sb.appendLine("--------------------------")
    sb.appendLine("စုစုပေါင်း : ${fmt(voucher.totalAmount)}")
    sb.appendLine("==========================")
    return sb.toString()
}
