package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RideDao {

    @Insert
    suspend fun insertRide(ride: Ride)

    @Update
    suspend fun updateRide(ride: Ride)

    @Query("SELECT * FROM Ride ORDER BY boardingTime DESC")
    suspend fun getAllRides(): List<Ride>

    @Query("SELECT * FROM Ride WHERE id = :rideId")
    suspend fun getRideById(rideId: String): Ride?

    @Query("SELECT * FROM Ride WHERE rideStatus != 'COMPLETED'")
    suspend fun getActiveRide(): Ride?
}