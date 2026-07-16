package com.example.ghostride

import java.util.UUID

enum class RideTag {
    OFFICE_COMMUTE, HOME, OTHER, UNCLASSIFIED
}

enum class RideStatus {
    TENTATIVE, ACTIVE, FINALIZING, COMPLETED
}

data class Ride(
    val id: String = UUID.randomUUID().toString(),
    val driverId: String,
    val vehicleId: String,
    val driverNameSnapshot: String,
    val vehicleNameSnapshot: String,
    val boardingTime: Long,
    val arrivalTime: Long? = null,
    val rideTag: RideTag = RideTag.UNCLASSIFIED,
    val rideStatus: RideStatus = RideStatus.TENTATIVE
)