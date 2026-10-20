package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.CloverLogo
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    var showSessionMenu by rememberSaveable { mutableStateOf(false) }

    val noRoundsText = stringResource(R.string.no_rounds_yet)
    val selectRoundPrompt = stringResource(R.string.select_round_prompt)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        // Top App Bar: Brand Green (#235347) header with CloverLogo and title "Lucky2D"
        Surface(
            color = LuckyHeader,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CloverLogo(
                    size = 38.dp,
                    modifier = Modifier.testTag("home_header_logo")
                )
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontFamily = LuckySansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.testTag("home_header_title")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date Section: Center-aligned green date indicator
            Text(
                text = "ယနေ့ - ${viewModel.todayDisplayDate}",
                color = LuckyDateGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .testTag("home_date_label")
            )

            // 2x2 Grid Menu Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Row 1: ကော်မရှင် | ဂဏန်းများ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    HomeGridCard(
                        icon = Icons.Default.People,
                        label = "ကော်မရှင်",
                        testTag = "home_agents_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.AGENTS) }
                    )

                    HomeGridCard(
                        icon = Icons.Default.GridView,
                        label = "ဂဏန်းများ",
                        testTag = "home_numbers_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.NUMBERS) }
                    )
                }

                // Row 2: ဘောင်ချာ | တင်ကွက်
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    HomeGridCard(
                        icon = Icons.Default.Receipt,
                        label = "ဘောင်ချာ",
                        testTag = "home_vouchers_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.VOUCHERS) }
                    )

                    HomeGridCard(
                        icon = Icons.Default.Add,
                        label = "တင်ကွက်",
                        testTag = "home_brake_button",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.BRAKE) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Round Selector & Settings Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    val currentRoundDisplay = if (sessions.isEmpty() || selectedSession.isEmpty()) {
                        noRoundsText
                    } else {
                        selectedSession
                    }
                    Text(
                        text = "$selectRoundPrompt - $currentRoundDisplay",
                        color = LuckyTextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { showSessionMenu = true }
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            .testTag("home_round_selector")
                    )

                    DropdownMenu(
                        expanded = showSessionMenu,
                        onDismissRequest = { showSessionMenu = false },
                        modifier = Modifier.background(LuckyCard)
                    ) {
                        if (sessions.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = noRoundsText,
                                        color = LuckyTextMuted,
                                        fontSize = 15.sp
                                    )
                                },
                                onClick = { showSessionMenu = false }
                            )
                        } else {
                            sessions.forEach { sess ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sess.name,
                                            color = if (sess.name == selectedSession) LuckyHeader else LuckyTextPrimary,
                                            fontWeight = if (sess.name == selectedSession) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 16.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSession(sess.name)
                                        showSessionMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "ဆက်တင်",
                        tint = LuckyTextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeGridCard(
    icon: ImageVector,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LuckyHeader),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
