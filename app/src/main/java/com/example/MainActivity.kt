package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val currentScreen by viewModel.currentScreen.collectAsState()

                // Collect toasts from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collectLatest { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                // Global BackHandler for non-home root screens
                if (currentScreen != Screen.HOME && currentScreen != Screen.NUMBER_ENTRY) {
                    BackHandler {
                        viewModel.goBack()
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LuckyBg),
                    bottomBar = {
                        // The number-entry screen has no bottom nav
                        if (currentScreen != Screen.NUMBER_ENTRY) {
                            LuckyBottomNav(
                                currentScreen = currentScreen,
                                onSelectScreen = { screen ->
                                    if (screen == Screen.AGENTS) {
                                        viewModel.navigateTo(Screen.AGENTS)
                                    } else {
                                        viewModel.navigateTo(screen)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentScreen == Screen.NUMBER_ENTRY) 0.dp else innerPadding.calculateBottomPadding())
                    ) {
                        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                            when (screen) {
                                Screen.HOME -> HomeScreen(viewModel = viewModel)
                                Screen.AGENTS -> AgentsScreen(viewModel = viewModel)
                                Screen.NUMBER_ENTRY -> NumberEntryScreen(viewModel = viewModel)
                                Screen.VOUCHERS -> VouchersScreen(viewModel = viewModel)
                                Screen.NUMBERS -> NumbersScreen(viewModel = viewModel)
                                Screen.BRAKE -> BrakeScreen(viewModel = viewModel)
                                Screen.SETTLEMENT -> SettlementScreen(viewModel = viewModel)
                                Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                Screen.LAYOFF_HISTORY -> LayoffHistoryScreen(viewModel = viewModel)
                                Screen.AGENT_SETTLEMENT -> AgentSettlementScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LuckyBottomNav(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    // Bottom nav bar: ပင်မ | ဂဏန်း | (+) | ဘောင်ချာ | ဆက်တင်
    // The (+) opens the Agents list.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            color = LuckyCard,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, LuckyBorder, RoundedCornerShape(22.dp))
                .shadow(8.dp, RoundedCornerShape(22.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. ပင်မ (Home)
                BottomNavItem(
                    label = "ပင်မ",
                    icon = Icons.Default.Home,
                    isSelected = currentScreen == Screen.HOME,
                    testTag = "nav_home",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectScreen(Screen.HOME) }
                )

                // 2. ဂဏန်း (Numbers)
                BottomNavItem(
                    label = "ဂဏန်း",
                    icon = Icons.Default.FormatListNumbered,
                    isSelected = currentScreen == Screen.NUMBERS,
                    testTag = "nav_numbers",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectScreen(Screen.NUMBERS) }
                )

                // 3. (+) raised round lime in the middle
                Box(
                    modifier = Modifier
                        .weight(1.1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .size(52.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(LuckyLime)
                            .testTag("nav_plus_button")
                            .clickable {
                                onSelectScreen(Screen.AGENTS)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "ကော်မရှင် / ထိုးမည်",
                            tint = LuckyBg,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // 4. ဘောင်ချာ (Vouchers)
                BottomNavItem(
                    label = "ဘောင်ချာ",
                    icon = Icons.Default.ReceiptLong,
                    isSelected = currentScreen == Screen.VOUCHERS,
                    testTag = "nav_vouchers",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectScreen(Screen.VOUCHERS) }
                )

                // 5. ဆက်တင် (Settings)
                BottomNavItem(
                    label = "ဆက်တင်",
                    icon = Icons.Default.Settings,
                    isSelected = currentScreen == Screen.SETTINGS,
                    testTag = "nav_settings",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectScreen(Screen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) LuckyLime else LuckyTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) LuckyLime else LuckyTextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}
