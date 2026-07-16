package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GpsLogDao {

    @Insert
    suspend fun insertGpsLog(gpsLog: GpsLog)

    @Query("SELECT * FROM GpsLog WHERE rideId = :rideId ORDER BY timestamp ASC")
    suspend fun getGpsLogsForRide(rideId: String): List<GpsLog>
}