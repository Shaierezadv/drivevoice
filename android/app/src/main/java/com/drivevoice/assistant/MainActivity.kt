package com.drivevoice.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drivevoice.assistant.permissions.AppPermissions
import com.drivevoice.assistant.ui.navigation.Dest
import com.drivevoice.assistant.ui.screens.DrivingModeScreen
import com.drivevoice.assistant.ui.screens.HelpScreen
import com.drivevoice.assistant.ui.screens.PermissionsScreen
import com.drivevoice.assistant.ui.screens.SettingsScreen
import com.drivevoice.assistant.ui.theme.DriveVoiceTheme

class MainActivity : ComponentActivity() {
    private val vm: DrivingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveVoiceTheme {
                // Force RTL for Hebrew-first UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DriveVoiceRoot(vm)
                }
            }
        }
    }
}

@Composable
fun DriveVoiceRoot(vm: DrivingViewModel = viewModel()) {
    val context = LocalContext.current
    var missing by remember { mutableStateOf(AppPermissions.missing(context)) }
    val nav = rememberNavController()
    val start = if (missing.isEmpty()) Dest.Driving.route else Dest.Permissions.route

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        missing = AppPermissions.missing(context)
        if (missing.isEmpty()) {
            nav.navigate(Dest.Driving.route) {
                popUpTo(Dest.Permissions.route) { inclusive = true }
            }
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = current != Dest.Permissions.route

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    NavigationBarItem(
                        selected = current == Dest.Driving.route,
                        onClick = {
                            nav.navigate(Dest.Driving.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                        label = { Text("נהיגה") }
                    )
                    NavigationBarItem(
                        selected = current == Dest.Settings.route,
                        onClick = {
                            nav.navigate(Dest.Settings.route) { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("הגדרות") }
                    )
                    NavigationBarItem(
                        selected = current == Dest.Help.route,
                        onClick = {
                            nav.navigate(Dest.Help.route) { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Default.Help, contentDescription = null) },
                        label = { Text("עזרה") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Permissions.route) {
                PermissionsScreen(
                    missing = missing,
                    onRequest = {
                        permissionLauncher.launch(AppPermissions.required.toTypedArray())
                    },
                    onContinue = {
                        nav.navigate(Dest.Driving.route) {
                            popUpTo(Dest.Permissions.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Dest.Driving.route) {
                DrivingModeScreen(
                    state = vm.ui,
                    onMicClick = { vm.toggleListen() },
                    onConfirmYes = { vm.confirmPending() },
                    onConfirmNo = { vm.cancelPending() }
                )
            }
            composable(Dest.Settings.route) {
                SettingsScreen(
                    confirmBeforeSensitive = vm.ui.confirmBeforeSensitive,
                    onConfirmChanged = { vm.setConfirmBeforeSensitive(it) }
                )
            }
            composable(Dest.Help.route) {
                HelpScreen()
            }
        }
    }
}
