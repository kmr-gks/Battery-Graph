package com.gukos.battery_graph.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.entity.BatteryRecord

@Database(entities = [BatteryRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "battery_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}