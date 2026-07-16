package com.example.ghostride

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Driver::class, Vehicle::class, Ride::class, GpsLog::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class GhostRideDatabase : RoomDatabase() {

    abstract fun driverDao(): DriverDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun rideDao(): RideDao
    abstract fun gpsLogDao(): GpsLogDao

    companion object {
        @Volatile
        private var INSTANCE: GhostRideDatabase? = null

        fun getInstance(context: Context): GhostRideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GhostRideDatabase::class.java,
                    "ghostride_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}