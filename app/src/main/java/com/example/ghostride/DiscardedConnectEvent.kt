package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class DiscardReason {
    NO_MOTION_CONFIRMED,
    LOST_RACE_TO_ACTIVE_RIDE
}

@Entity
data class DiscardedConnectEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val vehicleId: String,
    val reason: DiscardReason
)