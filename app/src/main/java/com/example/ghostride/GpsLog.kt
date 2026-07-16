package com.example.ghostride

import java.util.UUID

data class GpsLog(
    val id: String = UUID.randomUUID().toString(),
    val rideId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double
)