package com.example.ghostride

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class SilentFailureWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = GhostRideDatabase.getInstance(applicationContext)
        val allRides = database.rideDao().getAllRides()
        val allWorkingDays = database.workingDayDao().getAllWorkingDays()

        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        var foundGap = false

        for (daysAgo in 1..7) {
            val checkDate = today.minusDays(daysAgo.toLong())
            val checkWeekday = dayOfWeekToWeekday(checkDate.dayOfWeek)
            val isWorkingDay = allWorkingDays.find { it.day == checkWeekday }?.isEnabled == true

            if (!isWorkingDay) continue

            val hasRideOnThatDate = allRides.any { ride ->
                val rideDate = Instant.ofEpochMilli(ride.boardingTime)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                rideDate == checkDate
            }

            if (!hasRideOnThatDate) {
                foundGap = true
                break
            }
        }

        if (foundGap) {
            showGapNotification()
        }

        return Result.success()
    }

    private fun dayOfWeekToWeekday(day: java.time.DayOfWeek): Weekday {
        return when (day) {
            java.time.DayOfWeek.MONDAY -> Weekday.MONDAY
            java.time.DayOfWeek.TUESDAY -> Weekday.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> Weekday.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> Weekday.THURSDAY
            java.time.DayOfWeek.FRIDAY -> Weekday.FRIDAY
            java.time.DayOfWeek.SATURDAY -> Weekday.SATURDAY
            java.time.DayOfWeek.SUNDAY -> Weekday.SUNDAY
        }
    }

    private fun showGapNotification() {
        val notification = Notification.Builder(applicationContext, BluetoothMonitorService.CHANNEL_ID)
            .setContentTitle("GhostRide")
            .setContentText("No commute detected recently. Please check the app.")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }
}