package com.example.ghostride

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

            if (workingDay?.isEnabled == true) {
                Log.d(TAG, "Tentative connect on working day: $vehicleAddress")
            } else {
                Log.d(TAG, "Ignored connect, not a working day: $vehicleAddress")
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