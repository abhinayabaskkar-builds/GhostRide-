package com.example.ghostride.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ghostride.ui.screens.DevToolsScreen
import com.example.ghostride.ui.screens.DriversVehiclesScreen
import com.example.ghostride.ui.screens.LocationScreen
import com.example.ghostride.ui.screens.ProfileScreen
import com.example.ghostride.ui.screens.SetupHomeScreen
import com.example.ghostride.ui.screens.WorkingDaysScreen

object Routes {
    const val SETUP_HOME = "setup_home"
    const val WORKING_DAYS = "working_days"
    const val DRIVERS_VEHICLES = "drivers_vehicles"
    const val PROFILE = "profile"
    const val LOCATION = "location"
    const val DEV_TOOLS = "dev_tools"
}

@Composable
fun GhostRideNavigation(
    modifier: Modifier = Modifier,
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
        startDestination = Routes.SETUP_HOME,
        modifier = modifier
    ) {
        composable(Routes.SETUP_HOME) {
            SetupHomeScreen(
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToWorkingDays = { navController.navigate(Routes.WORKING_DAYS) },
                onNavigateToDriversVehicles = { navController.navigate(Routes.DRIVERS_VEHICLES) },
                onNavigateToLocation = { navController.navigate(Routes.LOCATION) }
            )
        }
        composable(Routes.WORKING_DAYS) {
            WorkingDaysScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DRIVERS_VEHICLES) {
            DriversVehiclesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LOCATION) {
            LocationScreen(
                onBack = { navController.popBackStack() }
            )
        }
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