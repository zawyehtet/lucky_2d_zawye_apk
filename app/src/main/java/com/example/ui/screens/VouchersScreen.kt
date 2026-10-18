@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Voucher
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.SearchPill
import com.example.ui.components.ShareHelper
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

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

    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Edit item state
    var editingVoucherId by remember { mutableStateOf<Long?>(null) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingInitialNumber by remember { mutableStateOf("") }
    var editingInitialAmount by remember { mutableStateOf(0L) }

    // Filter vouchers by selectedDate, selectedSession, and search
    val filteredVouchers = vouchers.filter { v ->
        v.date == selectedDate && v.session == selectedSession &&
                (searchQuery.isBlank() ||
                        v.agent.contains(searchQuery, ignoreCase = true) ||
                        v.no.toString() == searchQuery.trim() ||
                        v.items.any { it.n == searchQuery.trim() })
    }

    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Box(modifier = modifier.fillMaxSize().background(LuckyBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = "ဘောင်ချာများ (Vouchers)",
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = { viewModel.setSession(it) }
            )

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
                        text = "စုစုပေါင်း ဘောင်ချာ : ${filteredVouchers.size} စောင်",
                        color = LuckyLime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SearchPill(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ဘောင်ချာ No. သို့မဟုတ် ထိုးသူ/ဂဏန်း ရှာရန်...",
                    modifier = Modifier.testTag("voucher_search_input")
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                text = "ဘောင်ချာ မရှိသေးပါ",
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
                                viewModel.deleteVoucher(voucher.id)
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

        // Edit/Delete Voucher Item Dialog
        if (editingVoucherId != null && editingItemIndex != null) {
            EditBetItemDialog(
                initialNumber = editingInitialNumber,
                initialAmount = editingInitialAmount,
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
    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LuckyCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LuckyBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Teal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyHeader)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "No.${voucher.no}",
                        color = Color(0xFFFFD54F), // Yellow No.
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
                        contentDescription = "Delete Voucher",
                        tint = LuckyDanger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Agent Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuckyCardDark)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(LuckyHeader),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = voucher.agent.take(1),
                        color = LuckyLime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "ကော် အိုင်ဒီ : ${voucher.agentId} , အမည် : ${voucher.agent}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            // White Bet Items List (one line per bet, long press to edit/delete)
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    voucher.items.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onLineLongPress(idx, item) }
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.  ${item.n}",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "= ${numFormat.format(item.a)} Ks",
                                color = Color(0xFF235347),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        if (idx < voucher.items.lastIndex) {
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Footer: စုစုပေါင်း ကျသင့်ငွေ : total with print, share, copy icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "စုစုပေါင်း ကျသင့်ငွေ : ${numFormat.format(voucher.totalAmount)} Ks",
                    color = LuckyLime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun buildReceiptText(voucher: Voucher): String {
    val sb = StringBuilder()
    sb.appendLine("=== Lucky2d ဘောင်ချာ ===")
    sb.appendLine("အမှတ် : No.${voucher.no}")
    sb.appendLine("နေ့စွဲ : ${voucher.date} (${voucher.session})")
    sb.appendLine("အချိန် : ${voucher.time}")
    sb.appendLine("ထိုးသူ : ${voucher.agent} (ID: ${voucher.agentId})")
    sb.appendLine("--------------------------")
    voucher.items.forEachIndexed { i, itm ->
        sb.appendLine("${i + 1}. ${itm.n} = ${itm.a} Ks")
    }
    sb.appendLine("--------------------------")
    sb.appendLine("စုစုပေါင်း : ${voucher.totalAmount} Ks")
    sb.appendLine("==========================")
    return sb.toString()
}
