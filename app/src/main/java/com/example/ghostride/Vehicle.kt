package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Vehicle(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bluetoothMac: String,
    val driverId: String
)