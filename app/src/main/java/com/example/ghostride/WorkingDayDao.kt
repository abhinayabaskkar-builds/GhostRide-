package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkingDayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkingDay(workingDay: WorkingDay)

    @Update
    suspend fun updateWorkingDay(workingDay: WorkingDay)

    @Query("SELECT * FROM WorkingDay")
    suspend fun getAllWorkingDays(): List<WorkingDay>

    @Query("SELECT * FROM WorkingDay WHERE day = :day")
    suspend fun getWorkingDay(day: Weekday): WorkingDay?
}