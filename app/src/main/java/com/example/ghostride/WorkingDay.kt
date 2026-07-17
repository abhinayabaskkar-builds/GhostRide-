package com.example.ghostride

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Weekday {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

@Entity
data class WorkingDay(
    @PrimaryKey
    val day: Weekday,
    val isEnabled: Boolean
)