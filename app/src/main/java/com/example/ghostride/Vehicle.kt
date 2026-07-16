package com.example.ghostride

import java.util.UUID

data class Vehicle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bluetoothMac: String,
    val driverId: String
)