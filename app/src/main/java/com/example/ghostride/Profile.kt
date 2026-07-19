package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Profile(
    @PrimaryKey
    val id: Int = 1,
    val name: String
)