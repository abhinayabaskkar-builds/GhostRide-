package com.example.ghostride

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class BluetoothMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "ride_monitoring_channel"
        const val NOTIFICATION_ID = 1
        const val TAG = "BluetoothMonitor"

        suspend fun handleConnect(context: Context, vehicleAddress: String) {
            val database = GhostRideDatabase.getInstance(context)
            val today = LocalDate.now().dayOfWeek
            val weekday = todayAsWeekday(today)
            val workingDay = database.workingDayDao().getWorkingDay(weekday)

            if (workingDay?.isEnabled != true) {
                Log.d(TAG, "Ignored connect, not a working day: $vehicleAddress")
                return
            }

            Log.d(TAG, "Tentative connect on working day: $vehicleAddress. Starting confirmation window...")

            val vehicle = database.vehicleDao().getVehicleByBluetoothMac(vehicleAddress)
            if (vehicle == null) {
                Log.d(TAG, "No configured vehicle found for address: $vehicleAddress")
                return
            }

            val driver = database.driverDao().getDriverById(vehicle.driverId)
            if (driver == null) {
                Log.d(TAG, "No configured driver found for vehicle: ${vehicle.name}")
                return
            }

            val connectTime = System.currentTimeMillis()
            val windowEndTime = connectTime + (Config.confirmationWindowSeconds * 1000)

            var motionConfirmed = false

            while (System.currentTimeMillis() < windowEndTime) {
                val isInVehicle = LatestMotionState.activityType == DetectedActivity.IN_VEHICLE
                val speed = getCurrentSpeedMetersPerSecond(context)
                val isMovingFastEnough = speed != null && speed >= Config.motionSpeedThresholdMetersPerSecond

                Log.d(TAG, "Checking motion: inVehicle=$isInVehicle, speed=$speed")

                if (isInVehicle && isMovingFastEnough) {
                    motionConfirmed = true
                    break
                }

                delay(Config.motionCheckIntervalMillis)
            }

            if (motionConfirmed) {
                val ride = Ride(
                    driverId = driver.id,
                    vehicleId = vehicle.id,
                    driverNameSnapshot = driver.name,
                    vehicleNameSnapshot = vehicle.name,
                    boardingTime = connectTime,
                    rideStatus = RideStatus.ACTIVE
                )
                database.rideDao().insertRide(ride)
                Log.d(TAG, "Motion confirmed. Ride created: ${ride.id}")
            } else {
                Log.d(TAG, "No motion confirmed within window. Discarding tentative connect.")
            }
        }

        fun handleDisconnect(vehicleAddress: String) {
            Log.d(TAG, "Disconnect detected: $vehicleAddress")
        }

        private fun todayAsWeekday(day: DayOfWeek): Weekday {
            return when (day) {
                DayOfWeek.MONDAY -> Weekday.MONDAY
                DayOfWeek.TUESDAY -> Weekday.TUESDAY
                DayOfWeek.WEDNESDAY -> Weekday.WEDNESDAY
                DayOfWeek.THURSDAY -> Weekday.THURSDAY
                DayOfWeek.FRIDAY -> Weekday.FRIDAY
                DayOfWeek.SATURDAY -> Weekday.SATURDAY
                DayOfWeek.SUNDAY -> Weekday.SUNDAY
            }
        }

        suspend fun getCurrentSpeedMetersPerSecond(context: Context): Float? {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            return suspendCancellableCoroutine { continuation ->
                try {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            continuation.resume(location?.speed)
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    Log.d(TAG, "Location permission not granted, cannot read speed")
                    continuation.resume(null)
                }
            }
        }

        fun startActivityRecognitionUpdates(context: Context) {
            val client = ActivityRecognition.getClient(context)
            val intent = Intent(context, ActivityRecognitionReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            try {
                client.requestActivityUpdates(10000L, pendingIntent)
                    .addOnSuccessListener {
                        Log.d(TAG, "Activity recognition updates requested successfully")
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "Failed to request activity updates: ${it.message}")
                    }
            } catch (e: SecurityException) {
                Log.d(TAG, "Activity Recognition permission not granted")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
            )
            val address = device?.address ?: return

            val isKnownVehicle = address == Config.vehicle1MacAddress ||
                    address == Config.vehicle2MacAddress
            if (!isKnownVehicle) return

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    serviceScope.launch { handleConnect(applicationContext, address) }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleDisconnect(address)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        startActivityRecognitionUpdates(applicationContext)

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ride Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GhostRide")
            .setContentText("Monitoring for your commute")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .build()
    }
}