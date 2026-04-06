package com.gukos.battery_graph.repository

import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.util.BatteryManagerWrapper
import kotlinx.coroutines.flow.Flow

class BatteryRepository(
    private val dao: BatteryDao,
    private val batteryManager: BatteryManagerWrapper
) {

    fun getRecords(): Flow<List<BatteryRecord>> = dao.getAll()

    suspend fun recordBatteryLevel() {
        val record = BatteryRecord(
            timestamp = System.currentTimeMillis(),
            level = batteryManager.getBatteryLevel()
        )
        dao.insert(record)
    }
}