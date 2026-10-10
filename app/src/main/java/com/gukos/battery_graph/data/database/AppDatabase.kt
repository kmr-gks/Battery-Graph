package com.gukos.battery_graph.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.entity.BatteryRecord

@Database(entities = [BatteryRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 充電状態・温度・電圧の列と timestamp のインデックスを追加
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE battery_records ADD COLUMN isCharging INTEGER")
                db.execSQL("ALTER TABLE battery_records ADD COLUMN temperature INTEGER")
                db.execSQL("ALTER TABLE battery_records ADD COLUMN voltage INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_battery_records_timestamp " +
                        "ON battery_records(timestamp)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "battery_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
