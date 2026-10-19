package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Voucher
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val totalRetained = viewModel.getTotalRetainedMoney()

    val currentVouchers = remember(vouchers, selectedDate, selectedSession) {
        vouchers.filter { it.date == selectedDate && it.session == selectedSession }
    }

    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var menuVoucher by remember { mutableStateOf<Voucher?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        // App Header
        AppHeader(
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card: Total Retained Money
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LuckyBorder))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "စုစုပေါင်း လက်ခံငွေ",
                            color = LuckyTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fmt(totalRetained),
                            color = LuckyLime,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "နေ့စွဲ : $selectedDate (${if (selectedSession.isEmpty()) "အကြိမ် မရှိသေးပါ" else selectedSession})",
                            color = LuckyTextMuted,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Three round buttons: ကော်မရှင်, ဂဏန်းများ, တင်ကွက်
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HomeRoundButton(
                                icon = Icons.Default.People,
                                label = "ကော်မရှင်",
                                testTag = "home_agents_button",
                                onClick = { viewModel.navigateTo(Screen.AGENTS) }
                            )

                            HomeRoundButton(
                                icon = Icons.Default.FormatListNumbered,
                                label = "ဂဏန်းများ",
                                testTag = "home_numbers_button",
                                onClick = { viewModel.navigateTo(Screen.NUMBERS) }
                            )

                            HomeRoundButton(
                                icon = Icons.Default.Tune,
                                label = "ဘရိတ်တင်ငွေ",
                                testTag = "home_brake_button",
                                onClick = { viewModel.navigateTo(Screen.BRAKE) }
                            )
                        }
                    }
                }
            }

            // Quick Actions: တင်ကွက်မှတ်တမ်း, ဘောင်ချာများ, စာရင်းချုပ်
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.LAYOFF_HISTORY) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = LuckyCard, contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LuckyBorder),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = LuckyLime, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("တင်ကွက်", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.VOUCHERS) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = LuckyCard, contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LuckyBorder),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = LuckyLime, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ဘောင်ချာ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.SETTLEMENT) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = LuckyCard, contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LuckyBorder),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LuckyLime, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("စာရင်းချုပ်", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section Header: မကြာသေးမီက ဘောင်ချာများ (ဖိထားပါက ဖျက်နိုင်သည်)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "မကြာသေးမီက ဘောင်ချာများ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    TextButton(
                        onClick = { viewModel.navigateTo(Screen.VOUCHERS) }
                    ) {
                        Text("အားလုံးကြည့်မည်", color = LuckyLime, fontSize = 12.sp)
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View all",
                            tint = LuckyLime,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Latest 5 vouchers
            val recentVouchers = currentVouchers.take(5)
            if (recentVouchers.isEmpty()) {
                item {
                    EmptyStateView("အချက်အလက် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ")
                }
            } else {
                items(recentVouchers, key = { it.id }) { voucher ->
                    VoucherItemRow(
                        voucher = voucher,
                        onClick = { viewModel.navigateTo(Screen.VOUCHERS) },
                        onLongClick = {
                            menuVoucher = voucher
                        }
                    )
                }
            }
        }

        // Long press menu dialog for voucher
        menuVoucher?.let { v ->
            RecordActionMenuDialog(
                title = "ဘောင်ချာ ${v.agent} (No.${v.no})",
                onDismiss = { menuVoucher = null },
                onEdit = {
                    menuVoucher = null
                    viewModel.navigateTo(Screen.VOUCHERS)
                },
                onDelete = {
                    val target = menuVoucher
                    menuVoucher = null
                    voucherToDelete = target
                }
            )
        }

        // Delete confirm dialog
        voucherToDelete?.let { v ->
            DeleteConfirmDialog(
                itemName = "ဘောင်ချာ ${v.agent} (No.${v.no})",
                onDismiss = { voucherToDelete = null },
                onConfirm = {
                    viewModel.deleteVoucher(v.id)
                    voucherToDelete = null
                }
            )
        }
    }
}

@Composable
fun HomeRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(LuckyHeader)
                .testTag(testTag)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = LuckyLime,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VoucherItemRow(
    voucher: Voucher,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LuckyCard)
            .border(1.dp, LuckyBorder, RoundedCornerShape(16.dp))
            .recordLongPress(
                onLongPress = { onLongClick?.invoke() },
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LuckyHeader),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = voucher.agent.take(1),
                    color = LuckyLime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column {
                Text(
                    text = "${voucher.agent} (No.${voucher.no})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = voucher.time,
                    color = LuckyTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = fmt(voucher.totalAmount),
            color = LuckyLime,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
