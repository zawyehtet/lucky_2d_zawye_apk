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
import com.example.security.LicenseManager
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
                var isActivated by remember {
                    mutableStateOf(LicenseManager.isAppActivated(context))
                }

                if (!isActivated) {
                    ActivationScreen(
                        onActivated = {
                            isActivated = true
                        }
                    )
                } else {
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

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LuckyBg)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
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

    override fun onDestroy() {
        if (isFinishing) {
            SoundManager.release()
        }
        super.onDestroy()
    }
}

