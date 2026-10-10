package com.gukos.battery_graph.repository

import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.util.BatteryManagerWrapper
import com.gukos.battery_graph.util.BatterySnapshot
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class BatteryRepository(
    private val dao: BatteryDao,
    private val batteryManager: BatteryManagerWrapper
) {

    fun getRecords(): Flow<List<BatteryRecord>> = dao.getAll()

    /** 現在の状態を無条件に記録する（定期記録・手動記録用） */
    suspend fun recordBatteryLevel() {
        batteryManager.getSnapshot()?.let { insert(it) }
    }

    // BATTERY_CHANGED は電圧・温度の変化でも頻繁に届くため、残量か充電状態が変わったときだけ記録する
    suspend fun recordIfChanged(snapshot: BatterySnapshot) {
        val latest = dao.getLatest()
        if (latest?.level == snapshot.level && latest.isCharging == snapshot.isCharging) return
        insert(snapshot)
    }

    /** 保持期間を過ぎた記録を削除し、削除件数を返す */
    suspend fun deleteExpiredRecords(): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        return dao.deleteOlderThan(cutoff)
    }

    private suspend fun insert(snapshot: BatterySnapshot) {
        dao.insert(
            BatteryRecord(
                timestamp = System.currentTimeMillis(),
                level = snapshot.level,
                isCharging = snapshot.isCharging,
                temperature = snapshot.temperature,
                voltage = snapshot.voltage
            )
        )
    }

    companion object {
        const val RETENTION_DAYS = 90L
    }
}
