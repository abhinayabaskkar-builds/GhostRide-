package com.example.ghostride.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ghostride.ui.screens.DevToolsScreen
import com.example.ghostride.ui.screens.DriversVehiclesScreen
import com.example.ghostride.ui.screens.LocationScreen
import com.example.ghostride.ui.screens.ProfileScreen
import com.example.ghostride.ui.screens.RideDetailScreen
import com.example.ghostride.ui.screens.RideHistoryListScreen
import com.example.ghostride.ui.screens.SetupHomeScreen
import com.example.ghostride.ui.screens.WorkingDaysScreen

object Routes {
    const val SETUP_HOME = "setup_home"
    const val WORKING_DAYS = "working_days"
    const val DRIVERS_VEHICLES = "drivers_vehicles"
    const val PROFILE = "profile"
    const val LOCATION = "location"
    const val DEV_TOOLS = "dev_tools"
    const val RIDE_HISTORY = "ride_history"
    const val RIDE_DETAIL = "ride_detail"
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
                onNavigateToLocation = { navController.navigate(Routes.LOCATION) },
                onNavigateToRideHistory = { navController.navigate(Routes.RIDE_HISTORY) },
                onNavigateToDevTools = { navController.navigate(Routes.DEV_TOOLS) }
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
        composable(Routes.RIDE_HISTORY) {
            RideHistoryListScreen(
                onBack = { navController.popBackStack() },
                onRideClick = { rideId -> navController.navigate("${Routes.RIDE_DETAIL}/$rideId") }
            )
        }
        composable(
            route = "${Routes.RIDE_DETAIL}/{rideId}",
            arguments = listOf(navArgument("rideId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId") ?: return@composable
            RideDetailScreen(
                rideId = rideId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}