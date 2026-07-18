package com.example.ghostride.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ghostride.ui.screens.DevToolsScreen

object Routes {
    const val DEV_TOOLS = "dev_tools"
}

@Composable
fun GhostRideNavigation(
    onSimulateConnect: () -> Unit,
    onSimulateDisconnect: () -> Unit,
    onCheckSpeed: () -> Unit,
    onStartActivityUpdates: () -> Unit,
    onCreateTestActiveRide: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRunSilentFailureCheck: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DEV_TOOLS
    ) {
        composable(Routes.DEV_TOOLS) {
            DevToolsScreen(
                onSimulateConnect = onSimulateConnect,
                onSimulateDisconnect = onSimulateDisconnect,
                onCheckSpeed = onCheckSpeed,
                onStartActivityUpdates = onStartActivityUpdates,
                onCreateTestActiveRide = onCreateTestActiveRide,
                onRequestBatteryExemption = onRequestBatteryExemption,
                onRunSilentFailureCheck = onRunSilentFailureCheck
            )
        }
    }
}