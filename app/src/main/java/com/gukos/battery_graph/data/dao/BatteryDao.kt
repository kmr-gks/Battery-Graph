package com.gukos.battery_graph.data.dao

import androidx.room.*
import com.gukos.battery_graph.data.entity.BatteryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {

    @Insert
    suspend fun insert(record: BatteryRecord)

    @Query("SELECT * FROM battery_records ORDER BY timestamp ASC")
    fun getAll(): Flow<List<BatteryRecord>>
}
