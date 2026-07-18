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
import android.location.Location
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

            val vehicle = database.vehicleDao().getVehicleByBluetoothMac(vehicleAddress)
            if (vehicle == null) {
                Log.d(TAG, "No configured vehicle found for address: $vehicleAddress")
                return
            }

            // Check if this is a reconnect within the grace period of an existing ride
            val existingActiveRide = database.rideDao().getActiveRide()
            if (existingActiveRide != null &&
                existingActiveRide.vehicleId == vehicle.id &&
                existingActiveRide.rideStatus == RideStatus.FINALIZING
            ) {
                val resumedRide = existingActiveRide.copy(
                    rideStatus = RideStatus.ACTIVE,
                    arrivalTime = null,
                    arrivalLatitude = null,
                    arrivalLongitude = null
                )
                database.rideDao().updateRide(resumedRide)
                Log.d(TAG, "Reconnected within grace period, resuming ride: ${resumedRide.id}")
                return
            }

            val today = LocalDate.now().dayOfWeek
            val weekday = todayAsWeekday(today)
            val workingDay = database.workingDayDao().getWorkingDay(weekday)

            if (workingDay?.isEnabled != true) {
                Log.d(TAG, "Ignored connect, not a working day: $vehicleAddress")
                return
            }

            Log.d(TAG, "Tentative connect on working day: $vehicleAddress. Starting confirmation window...")

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
                val boardingLocation = getCurrentLocationOrNull(context)
                val ride = Ride(
                    driverId = driver.id,
                    vehicleId = vehicle.id,
                    driverNameSnapshot = driver.name,
                    vehicleNameSnapshot = vehicle.name,
                    boardingTime = connectTime,
                    boardingLatitude = boardingLocation?.latitude,
                    boardingLongitude = boardingLocation?.longitude,
                    rideStatus = RideStatus.ACTIVE
                )
                database.rideDao().insertRide(ride)
                Log.d(TAG, "Motion confirmed. Ride created: ${ride.id}")
            } else {
                Log.d(TAG, "No motion confirmed within window. Discarding tentative connect.")
            }
        }

        suspend fun handleDisconnect(context: Context, vehicleAddress: String) {
            val database = GhostRideDatabase.getInstance(context)
            val vehicle = database.vehicleDao().getVehicleByBluetoothMac(vehicleAddress)
            if (vehicle == null) {
                Log.d(TAG, "Disconnect from unknown vehicle: $vehicleAddress")
                return
            }

            val activeRide = database.rideDao().getActiveRide()
            if (activeRide == null || activeRide.vehicleId != vehicle.id ||
                activeRide.rideStatus != RideStatus.ACTIVE
            ) {
                Log.d(TAG, "Disconnect detected, but no active ride for this vehicle: $vehicleAddress")
                return
            }

            Log.d(TAG, "Disconnect detected, starting grace period: $vehicleAddress")

            val tentativeArrivalTime = System.currentTimeMillis()
            val tentativeLocation = getCurrentLocationOrNull(context)

            val finalizingRide = activeRide.copy(
                rideStatus = RideStatus.FINALIZING,
                arrivalTime = tentativeArrivalTime,
                arrivalLatitude = tentativeLocation?.latitude,
                arrivalLongitude = tentativeLocation?.longitude
            )
            database.rideDao().updateRide(finalizingRide)

            delay(Config.gracePeriodSeconds * 1000)

            val recheckedRide = database.rideDao().getRideById(finalizingRide.id)
            if (recheckedRide?.rideStatus == RideStatus.FINALIZING) {
                val duration = if (recheckedRide.arrivalTime != null) {
                    (recheckedRide.arrivalTime - recheckedRide.boardingTime) / 1000
                } else null

                val distance = calculateDistanceMeters(
                    recheckedRide.boardingLatitude, recheckedRide.boardingLongitude,
                    recheckedRide.arrivalLatitude, recheckedRide.arrivalLongitude
                )

                val tag = tagRide(recheckedRide.arrivalLatitude, recheckedRide.arrivalLongitude)

                val completedRide = recheckedRide.copy(
                    rideStatus = RideStatus.COMPLETED,
                    durationSeconds = duration,
                    distanceMeters = distance,
                    rideTag = tag
                )
                database.rideDao().updateRide(completedRide)
                Log.d(
                    TAG,
                    "Ride finalized: ${completedRide.id}, duration=${duration}s, distance=${distance}m, tag=$tag"
                )
            } else {
                Log.d(TAG, "Ride reconnected within grace period, no finalization needed: ${finalizingRide.id}")
            }
        }

        private fun calculateDistanceMeters(
            lat1: Double?, lon1: Double?, lat2: Double?, lon2: Double?
        ): Double? {
            if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
            val earthRadiusMeters = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return earthRadiusMeters * c
        }

        private fun tagRide(arrivalLat: Double?, arrivalLon: Double?): RideTag {
            if (arrivalLat == null || arrivalLon == null) return RideTag.UNCLASSIFIED

            val distanceToOffice = calculateDistanceMeters(
                arrivalLat, arrivalLon, Config.officeLatitude, Config.officeLongitude
            )
            val distanceToHome = calculateDistanceMeters(
                arrivalLat, arrivalLon, Config.homeLatitude, Config.homeLongitude
            )

            return when {
                distanceToOffice != null && distanceToOffice <= Config.geofenceRadiusMeters ->
                    RideTag.OFFICE_COMMUTE
                distanceToHome != null && distanceToHome <= Config.geofenceRadiusMeters ->
                    RideTag.HOME
                else -> RideTag.UNCLASSIFIED
            }
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
            return getCurrentLocationOrNull(context)?.speed
        }

        suspend fun getCurrentLocationOrNull(context: Context): Location? {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            return suspendCancellableCoroutine { continuation ->
                try {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            continuation.resume(location)
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    Log.d(TAG, "Location permission not granted, cannot read location")
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
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    serviceScope.launch { handleDisconnect(applicationContext, address) }
                }
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