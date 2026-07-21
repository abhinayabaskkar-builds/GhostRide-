package com.example.ghostride

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRideTag(value: RideTag): String = value.name

    @TypeConverter
    fun toRideTag(value: String): RideTag = RideTag.valueOf(value)

    @TypeConverter
    fun fromRideStatus(value: RideStatus): String = value.name

    @TypeConverter
    fun toRideStatus(value: String): RideStatus = RideStatus.valueOf(value)

    @TypeConverter
    fun fromLocationType(value: LocationType): String = value.name

    @TypeConverter
    fun toLocationType(value: String): LocationType = LocationType.valueOf(value)

    @TypeConverter
    fun fromDiscardReason(value: DiscardReason): String = value.name

    @TypeConverter
    fun toDiscardReason(value: String): DiscardReason = DiscardReason.valueOf(value)
}