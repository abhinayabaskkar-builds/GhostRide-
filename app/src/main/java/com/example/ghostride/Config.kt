package com.example.ghostride

object Config {
    const val gracePeriodSeconds: Long = 120
    const val confirmationWindowSeconds: Long = 180
    const val geofenceRadiusMeters: Double = 200.0
    const val vehicle1MacAddress: String = "AA:BB:CC:DD:EE:01"
    const val vehicle2MacAddress: String = "AA:BB:CC:DD:EE:02"
    const val motionSpeedThresholdMetersPerSecond: Float = 2.5f
    const val motionCheckIntervalMillis: Long = 10000L
    const val officeLatitude: Double = 14.5511
    const val officeLongitude: Double = 121.0492

    const val homeLatitude: Double = 14.5176
    const val homeLongitude: Double = 121.0509

}