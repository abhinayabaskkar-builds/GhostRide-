package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Location(
    @PrimaryKey
    val type: LocationType,
    val displayAddress: String,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadiusMeters: Int = 200
)