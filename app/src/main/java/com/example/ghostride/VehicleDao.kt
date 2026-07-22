package com.example.ghostride

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface VehicleDao {

    @Insert
    suspend fun insertVehicle(vehicle: Vehicle)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM Vehicle")
    suspend fun getAllVehicles(): List<Vehicle>

    @Query("SELECT * FROM Vehicle WHERE bluetoothMac = :mac")
    suspend fun getVehicleByBluetoothMac(mac: String): Vehicle?
}