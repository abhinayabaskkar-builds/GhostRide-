package com.example.ghostride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DiscardedConnectEventDao {

    @Insert
    suspend fun insertDiscardedConnectEvent(event: DiscardedConnectEvent)

    @Query("SELECT * FROM DiscardedConnectEvent ORDER BY timestamp DESC")
    suspend fun getAllDiscardedConnectEvents(): List<DiscardedConnectEvent>
}