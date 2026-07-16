package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class GpsLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val rideId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double
)