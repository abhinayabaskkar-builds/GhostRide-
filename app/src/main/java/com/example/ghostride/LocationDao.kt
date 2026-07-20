package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocation(location: Location)

    @Query("SELECT * FROM Location WHERE type = :type")
    suspend fun getLocation(type: LocationType): Location?

    @Query("SELECT * FROM Location")
    suspend fun getAllLocations(): List<Location>
}