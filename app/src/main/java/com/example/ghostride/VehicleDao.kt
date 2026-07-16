package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VehicleDao {

    @Insert
    suspend fun insertVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM Vehicle")
    suspend fun getAllVehicles(): List<Vehicle>

    @Query("SELECT * FROM Vehicle WHERE bluetoothMac = :mac")
    suspend fun getVehicleByBluetoothMac(mac: String): Vehicle?
}