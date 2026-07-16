package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Driver(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String
)