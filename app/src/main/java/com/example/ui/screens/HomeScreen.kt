package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppDatePickerModal
import com.example.ui.components.CloverLogo
import com.example.ui.components.SessionPill
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val availableRounds by viewModel.availableRoundsForSelectedDate.collectAsState()
    var showSessionMenu by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val noRoundsText = stringResource(R.string.no_rounds_yet)

    val combinedRounds = remember(sessions, availableRounds, selectedSession) {
        val configured = sessions.map { it.name.trim() }.filter { it.isNotEmpty() }
        val avail = availableRounds.map { it.trim() }.filter { it.isNotEmpty() }
        val active = if (selectedSession.isNotBlank()) listOf(selectedSession.trim()) else emptyList()
        (configured + avail + active).distinct()
    }

    val displayDateText = remember(selectedDate) {
        "ယနေ့ - ${viewModel.formatDotDate(selectedDate)}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        // Top App Bar: Brand Green (#235347) header with CloverLogo, title "Lucky2D", and interactive [ ရက်စွဲ (Date) ] & [ အကြိမ်ရေ (Round) ] selectors
        Surface(
            color = LuckyHeader,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CloverLogo(
                        size = 36.dp,
                        modifier = Modifier.testTag("home_header_logo")
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontFamily = LuckySansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        modifier = Modifier.testTag("home_header_title")
                    )
                }

                // Header Interactive Date & Round Filter Controls: [ ရက်စွဲ (Date) ] [ အကြိမ်ရေ (Round) ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_header_date_picker_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "ရက်စွဲ ရွေးရန်",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = selectedDate,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    SessionPill(
                        selectedSession = selectedSession,
                        sessions = sessions,
                        historicalRounds = availableRounds,
                        onSessionSelected = { viewModel.setSession(it) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Non-Interactive Dashboard Date Banner (Above the 4 Main Menu Cards)
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 18.dp)
                    .testTag("home_date_label"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = LuckyDateGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayDateText,
                    color = LuckyDateGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 1.5.em,
                    textAlign = TextAlign.Center
                )
            }

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

            Spacer(modifier = Modifier.height(28.dp))

            // Round Selector ("အကြိမ် ရွေးရန် :") & Settings Bar — strictly displays saved/configured rounds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val currentRoundDisplay = if (combinedRounds.isEmpty() || selectedSession.isEmpty()) {
                        noRoundsText
                    } else {
                        selectedSession
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(LuckyCard)
                            .border(1.dp, LuckyBorder, RoundedCornerShape(10.dp))
                            .clickable { showSessionMenu = true }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .testTag("home_round_selector"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "အကြိမ် ရွေးရန် : $currentRoundDisplay",
                            color = LuckyTextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = LuckyHeader,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSessionMenu,
                        onDismissRequest = { showSessionMenu = false },
                        modifier = Modifier.background(LuckyCard)
                    ) {
                        if (combinedRounds.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = noRoundsText,
                                        color = LuckyTextMuted,
                                        fontSize = 15.sp,
                                        lineHeight = 1.5.em
                                    )
                                },
                                onClick = { showSessionMenu = false }
                            )
                        } else {
                            val configuredNames = sessions.map { it.name }.toSet()
                            combinedRounds.forEach { roundName ->
                                val isHistoricalOnly = roundName !in configuredNames
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isHistoricalOnly) "$roundName (မှတ်တမ်း)" else roundName,
                                            color = if (roundName == selectedSession) LuckyHeader else LuckyTextPrimary,
                                            fontWeight = if (roundName == selectedSession) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 16.sp,
                                            lineHeight = 1.5.em
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSession(roundName)
                                        showSessionMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "ဆက်တင်",
                        tint = LuckyTextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Branding Credit
            Text(
                text = stringResource(R.string.home_footer_branding),
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
                    .testTag("home_footer_branding")
            )
        }
    }

    if (showDatePicker) {
        AppDatePickerModal(
            currentDate = selectedDate,
            onDateSelected = { viewModel.setDate(it) },
            onDismiss = { showDatePicker = false }
        )
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
