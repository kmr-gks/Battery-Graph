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
        insert(batteryManager.getBatteryLevel())
    }

    // BATTERY_CHANGED は電圧・温度の変化でも頻繁に届くため、残量が変わったときだけ記録する
    suspend fun recordIfLevelChanged(level: Int) {
        if (level < 0) return
        if (dao.getLatest()?.level == level) return
        insert(level)
    }

    private suspend fun insert(level: Int) {
        dao.insert(
            BatteryRecord(
                timestamp = System.currentTimeMillis(),
                level = level
            )
        )
    }
}
