package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppRole
import com.example.ui.components.MtuHeader
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.GatekeeperScannerScreen
import com.example.ui.screens.StudentMealCardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MealCardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppRoot()
            }
        }
    }
}

@Composable
fun MainAppRoot(viewModel: MealCardViewModel = viewModel()) {
    val currentRole by viewModel.currentRole.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()

    // BackHandler: returns to Student Card if on secondary screen
    BackHandler(enabled = currentRole != AppRole.STUDENT) {
        viewModel.selectRole(AppRole.STUDENT)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            MtuHeader(
                currentRole = currentRole,
                onRoleSelected = { viewModel.selectRole(it) },
                activeSession = activeSession,
                isOfflineMode = isOfflineMode,
                onToggleOfflineMode = { viewModel.toggleOfflineMode() },
                unsyncedCount = unsyncedCount,
                onTriggerSync = { viewModel.syncOfflineRecords() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // Student Card Tab
                NavigationBarItem(
                    selected = currentRole == AppRole.STUDENT,
                    onClick = { viewModel.selectRole(AppRole.STUDENT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Student Card"
                        )
                    },
                    label = {
                        Text(
                            text = "Meal Card",
                            fontSize = 11.sp,
                            fontWeight = if (currentRole == AppRole.STUDENT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF004D40),
                        selectedTextColor = Color(0xFF004D40),
                        indicatorColor = Color(0xFFB2DFDB)
                    ),
                    modifier = Modifier.testTag("nav_student_tab")
                )

                // Gatekeeper Scanner Tab
                NavigationBarItem(
                    selected = currentRole == AppRole.GATEKEEPER,
                    onClick = { viewModel.selectRole(AppRole.GATEKEEPER) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unsyncedCount > 0) {
                                    Badge(containerColor = Color(0xFFD84315)) {
                                        Text("$unsyncedCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner Gate"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Gate Scanner",
                            fontSize = 11.sp,
                            fontWeight = if (currentRole == AppRole.GATEKEEPER) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF004D40),
                        selectedTextColor = Color(0xFF004D40),
                        indicatorColor = Color(0xFFB2DFDB)
                    ),
                    modifier = Modifier.testTag("nav_scanner_tab")
                )

                // Admin Dashboard Tab
                NavigationBarItem(
                    selected = currentRole == AppRole.ADMIN,
                    onClick = { viewModel.selectRole(AppRole.ADMIN) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Admin Dashboard"
                        )
                    },
                    label = {
                        Text(
                            text = "Admin Reports",
                            fontSize = 11.sp,
                            fontWeight = if (currentRole == AppRole.ADMIN) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF004D40),
                        selectedTextColor = Color(0xFF004D40),
                        indicatorColor = Color(0xFFB2DFDB)
                    ),
                    modifier = Modifier.testTag("nav_admin_tab")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRole) {
                AppRole.STUDENT -> StudentMealCardScreen(viewModel = viewModel)
                AppRole.GATEKEEPER -> GatekeeperScannerScreen(viewModel = viewModel)
                AppRole.ADMIN -> AdminDashboardScreen(viewModel = viewModel)
            }
        }
    }
}
