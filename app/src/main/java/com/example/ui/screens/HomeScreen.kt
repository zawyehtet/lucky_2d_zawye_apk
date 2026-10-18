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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()

    // Filtered vouchers for current date and session
    val currentVouchers = vouchers.filter {
        it.date == selectedDate && it.session == selectedSession
    }
    val totalRetained = viewModel.getTotalRetainedMoney()

    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "Lucky2d",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big Hero Card: စုစုပေါင်း လက်ခံငွေ
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LuckyCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LuckyBorder, RoundedCornerShape(22.dp))
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
                            text = "${numFormat.format(totalRetained)} Ks",
                            color = LuckyLime,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "နေ့စွဲ : $selectedDate ($selectedSession)",
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
                                icon = Icons.Default.ListAlt,
                                label = "ဂဏန်းများ",
                                testTag = "home_numbers_button",
                                onClick = { viewModel.navigateTo(Screen.NUMBERS) }
                            )

                            HomeRoundButton(
                                icon = Icons.Default.Calculate,
                                label = "တင်ကွက်",
                                testTag = "home_brake_button",
                                onClick = { viewModel.navigateTo(Screen.BRAKE) }
                            )
                        }
                    }
                }
            }

            // Latest Vouchers Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ဘောင်ချာများ (လတ်တလော)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "အားလုံး",
                        color = LuckyLime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag("home_view_all_vouchers")
                            .clickable { viewModel.navigateTo(Screen.VOUCHERS) }
                            .padding(4.dp)
                    )
                }
            }

            // Latest 5 vouchers
            val recentVouchers = currentVouchers.take(5)
            if (recentVouchers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ယနေ့ အပိုင်းအတွက် ဘောင်ချာ မရှိသေးပါ",
                            color = LuckyTextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(recentVouchers, key = { it.id }) { voucher ->
                    VoucherItemRow(
                        voucher = voucher,
                        onClick = { viewModel.navigateTo(Screen.VOUCHERS) }
                    )
                }
            }
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
    voucher: com.example.data.Voucher,
    onClick: () -> Unit
) {
    val numFormat = NumberFormat.getNumberInstance(Locale.US)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LuckyCard)
            .border(1.dp, LuckyBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
            text = "${numFormat.format(voucher.totalAmount)} Ks",
            color = LuckyLime,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
