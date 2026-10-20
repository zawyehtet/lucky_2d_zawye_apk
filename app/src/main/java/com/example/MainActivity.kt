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
import com.example.util.SoundManager
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundManager.init(applicationContext)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val currentScreen by viewModel.currentScreen.collectAsState()
                val editingSettledRecord by viewModel.editingSettledRecord.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collectLatest { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                if (currentScreen != Screen.HOME && currentScreen != Screen.NUMBER_ENTRY) {
                    BackHandler {
                        viewModel.goBack()
                    }
                } else if (currentScreen == Screen.HOME && editingSettledRecord != null) {
                    BackHandler {
                        viewModel.cancelEditingSettledRecord()
                    }
                }

                val showBottomBar = currentScreen != Screen.HOME &&
                    currentScreen != Screen.NUMBER_ENTRY &&
                    currentScreen != Screen.SETTLEMENT

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LuckyBg),
                    bottomBar = {
                        if (showBottomBar) {
                            LuckyBottomNav(
                                currentScreen = currentScreen,
                                onSelectScreen = { screen ->
                                    viewModel.navigateTo(screen)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
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

    override fun onDestroy() {
        if (isFinishing) {
            SoundManager.release()
        }
        super.onDestroy()
    }
}

@Composable
fun LuckyBottomNav(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LuckyCard,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = LuckyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "ပင်မ",
                icon = Icons.Default.Home,
                isSelected = currentScreen == Screen.HOME,
                testTag = "nav_home",
                modifier = Modifier.weight(1f),
                onClick = { onSelectScreen(Screen.HOME) }
            )

            BottomNavItem(
                label = "ကော်မရှင်",
                icon = Icons.Default.People,
                isSelected = currentScreen == Screen.AGENTS,
                testTag = "nav_agents",
                modifier = Modifier.weight(1f),
                onClick = { onSelectScreen(Screen.AGENTS) }
            )

            BottomNavItem(
                label = "ဂဏန်းများ",
                icon = Icons.Default.GridView,
                isSelected = currentScreen == Screen.NUMBERS,
                testTag = "nav_numbers",
                modifier = Modifier.weight(1f),
                onClick = { onSelectScreen(Screen.NUMBERS) }
            )

            BottomNavItem(
                label = "ဘောင်ချာ",
                icon = Icons.Default.Receipt,
                isSelected = currentScreen == Screen.VOUCHERS,
                testTag = "nav_vouchers",
                modifier = Modifier.weight(1f),
                onClick = { onSelectScreen(Screen.VOUCHERS) }
            )

            BottomNavItem(
                label = "တင်ကွက်",
                icon = Icons.Default.AddCircleOutline,
                isSelected = currentScreen == Screen.BRAKE || currentScreen == Screen.LAYOFF_HISTORY || currentScreen == Screen.SETTLEMENT,
                testTag = "nav_brake",
                modifier = Modifier.weight(1f),
                onClick = { onSelectScreen(Screen.BRAKE) }
            )

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
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) LuckyHeader else LuckyTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) LuckyHeader else LuckyTextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
