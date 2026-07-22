package com.example.ghostride

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DriverDao {

    @Insert
    suspend fun insertDriver(driver: Driver)

    @Update
    suspend fun updateDriver(driver: Driver)

    @Delete
    suspend fun deleteDriver(driver: Driver)

    @Query("SELECT * FROM Driver")
    suspend fun getAllDrivers(): List<Driver>

    @Query("SELECT * FROM Driver WHERE id = :driverId")
    suspend fun getDriverById(driverId: String): Driver?
}