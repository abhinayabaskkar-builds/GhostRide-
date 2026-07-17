package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.UUID

enum class RideTag {
    OFFICE_COMMUTE, HOME, OTHER, UNCLASSIFIED
}

enum class RideStatus {
    TENTATIVE, ACTIVE, FINALIZING, COMPLETED
}

@Entity
@TypeConverters(Converters::class)
data class Ride(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val driverId: String,
    val vehicleId: String,
    val driverNameSnapshot: String,
    val vehicleNameSnapshot: String,
    val boardingTime: Long,
    val boardingLatitude: Double? = null,
    val boardingLongitude: Double? = null,
    val arrivalTime: Long? = null,
    val arrivalLatitude: Double? = null,
    val arrivalLongitude: Double? = null,
    val durationSeconds: Long? = null,
    val distanceMeters: Double? = null,
    val rideTag: RideTag = RideTag.UNCLASSIFIED,
    val rideStatus: RideStatus = RideStatus.TENTATIVE
)