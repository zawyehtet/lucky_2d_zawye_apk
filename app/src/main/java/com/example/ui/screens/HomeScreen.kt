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
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showSessionMenu by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val noRoundsText = stringResource(R.string.no_rounds_yet)

    // Strictly configured rounds from Settings
    val configuredRounds = remember(sessions) {
        sessions.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    val formattedDotDate = remember(selectedDate) {
        viewModel.formatDotDate(selectedDate)
    }

    val displayDateText = remember(formattedDotDate) {
        "ယနေ့ - $formattedDotDate"
    }

    val controlCardShape = RoundedCornerShape(12.dp)
    val controlBorderColor = Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
            .navigationBarsPadding()
    ) {
        // 1. Clean Top App Bar: Strictly App Branding Title & Logo on Deep Emerald Green (#235347)
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
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
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
                        maxLines = 1,
                        modifier = Modifier.testTag("home_header_title")
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Non-Interactive Dashboard Date Banner (Above the 4 Main Menu Cards)
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .padding(bottom = 16.dp)
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

            // 2x2 Grid Menu Cards (ကော်မရှင်, ဂဏန်းများ, ဘောင်ချာ, တင်ကွက်)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Row 1: ကော်မရှင် | ဂဏန်းများ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
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

            Spacer(modifier = Modifier.height(22.dp))

            // 2. Repositioned Selection Controls directly BELOW 4-Grid Menu Cards:
            // [ Left: Date Selector ("ရက်စွဲ : DD.MM.YYYY") | Right: Draw Round Dropdown | Setting Button (⚙) ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Section: Date Selector / Display ("ရက်စွဲ : 22.10.2026")
                Surface(
                    onClick = { showDatePicker = true },
                    color = Color.White,
                    shape = controlCardShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .border(1.dp, controlBorderColor, controlCardShape)
                        .testTag("home_date_selector")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "ရက်စွဲ ရွေးရန်",
                            tint = LuckyHeader,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ရက်စွဲ : $formattedDotDate",
                            color = LuckyTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Section: Draw Round Dropdown (strictly configured rounds from Settings)
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .height(48.dp)
                ) {
                    val currentRoundDisplay = if (configuredRounds.isEmpty() || selectedSession.isEmpty()) {
                        noRoundsText
                    } else {
                        selectedSession
                    }
                    Surface(
                        onClick = { showSessionMenu = true },
                        color = Color.White,
                        shape = controlCardShape,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, controlBorderColor, controlCardShape)
                            .testTag("home_round_selector")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentRoundDisplay,
                                color = LuckyTextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.select_round_content_desc),
                                tint = LuckyHeader,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSessionMenu,
                        onDismissRequest = { showSessionMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        if (configuredRounds.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = noRoundsText,
                                        color = LuckyTextMuted,
                                        fontSize = 14.sp,
                                        lineHeight = 1.5.em
                                    )
                                },
                                onClick = { showSessionMenu = false }
                            )
                        } else {
                            configuredRounds.forEach { roundName ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = roundName,
                                            color = if (roundName == selectedSession) LuckyHeader else LuckyTextPrimary,
                                            fontWeight = if (roundName == selectedSession) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 15.sp,
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

                // Setting Button (⚙): Compact square icon button alongside the selector
                Surface(
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    color = Color.White,
                    shape = controlCardShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .border(1.dp, controlBorderColor, controlCardShape)
                        .testTag("home_settings_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "ဆက်တင်",
                            tint = LuckyHeader,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
