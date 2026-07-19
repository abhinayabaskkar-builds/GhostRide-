package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: Profile)

    @Query("SELECT * FROM Profile WHERE id = 1")
    suspend fun getProfile(): Profile?
}