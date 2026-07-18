package com.example.ghostride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ghostride.ui.theme.GhostRideTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var bluetoothGranted = false
    private var notificationGranted = false
    private var locationGranted = false
    private var activityRecognitionGranted = false

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        bluetoothGranted = isGranted
        maybeStartService()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationGranted = isGranted
        maybeStartService()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationGranted = isGranted
    }

    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        activityRecognitionGranted = isGranted
    }

    private fun maybeStartService() {
        if (bluetoothGranted) {
            startForegroundService(Intent(this, BluetoothMonitorService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = GhostRideDatabase.getInstance(applicationContext)

        lifecycleScope.launch {
            val existingDays = database.workingDayDao().getAllWorkingDays()
            if (existingDays.isEmpty()) {
                val defaults = listOf(
                    WorkingDay(Weekday.MONDAY, true),
                    WorkingDay(Weekday.TUESDAY, true),
                    WorkingDay(Weekday.WEDNESDAY, true),
                    WorkingDay(Weekday.THURSDAY, true),
                    WorkingDay(Weekday.FRIDAY, true),
                    WorkingDay(Weekday.SATURDAY, true),
                    WorkingDay(Weekday.SUNDAY, false)
                )
                defaults.forEach { database.workingDayDao().insertWorkingDay(it) }
            }

            val existingDrivers = database.driverDao().getAllDrivers()
            if (existingDrivers.isEmpty()) {
                val placeholderDriver = Driver(name = "Test Driver")
                database.driverDao().insertDriver(placeholderDriver)

                val placeholderVehicle = Vehicle(
                    name = "Test Vehicle",
                    bluetoothMac = Config.vehicle1MacAddress,
                    driverId = placeholderDriver.id
                )
                database.vehicleDao().insertVehicle(placeholderVehicle)
            }
        }

        bluetoothGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        locationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        activityRecognitionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        if (!bluetoothGranted) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else if (!notificationGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            maybeStartService()
        }

        if (!locationGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (!activityRecognitionGranted) {
            activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        setContent {
            GhostRideTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TestScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSimulateConnect = {
                            lifecycleScope.launch {
                                BluetoothMonitorService.handleConnect(
                                    applicationContext,
                                    Config.vehicle1MacAddress
                                )
                            }
                        },
                        onSimulateDisconnect = {
                            lifecycleScope.launch {
                                BluetoothMonitorService.handleDisconnect(
                                    applicationContext,
                                    Config.vehicle1MacAddress
                                )
                            }
                        },
                        onCheckSpeed = {
                            lifecycleScope.launch {
                                val speed = BluetoothMonitorService.getCurrentSpeedMetersPerSecond(
                                    applicationContext
                                )
                                android.util.Log.d("SpeedTest", "Current speed: $speed m/s")
                            }
                        },
                        onStartActivityUpdates = {
                            BluetoothMonitorService.startActivityRecognitionUpdates(applicationContext)
                        },
                        onCreateTestActiveRide = {
                            lifecycleScope.launch {
                                val vehicle = database.vehicleDao()
                                    .getVehicleByBluetoothMac(Config.vehicle1MacAddress)
                                val driver = vehicle?.let { database.driverDao().getDriverById(it.driverId) }

                                if (vehicle != null && driver != null) {
                                    val testRide = Ride(
                                        driverId = driver.id,
                                        vehicleId = vehicle.id,
                                        driverNameSnapshot = driver.name,
                                        vehicleNameSnapshot = vehicle.name,
                                        boardingTime = System.currentTimeMillis() - 300000,
                                        rideStatus = RideStatus.ACTIVE
                                    )
                                    database.rideDao().insertRide(testRide)
                                    android.util.Log.d("TestSetup", "Created test active ride: ${testRide.id}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TestScreen(
    modifier: Modifier = Modifier,
    onSimulateConnect: () -> Unit,
    onSimulateDisconnect: () -> Unit,
    onCheckSpeed: () -> Unit,
    onStartActivityUpdates: () -> Unit,
    onCreateTestActiveRide: () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = "GhostRide")
        Button(onClick = onSimulateConnect) {
            Text("Simulate Vehicle Connect")
        }
        Button(onClick = onSimulateDisconnect) {
            Text("Simulate Vehicle Disconnect")
        }
        Button(onClick = onCheckSpeed) {
            Text("Check GPS Speed")
        }
        Button(onClick = onStartActivityUpdates) {
            Text("Start Activity Recognition")
        }
        Button(onClick = onCreateTestActiveRide) {
            Text("Create Test Active Ride")
        }
    }
}