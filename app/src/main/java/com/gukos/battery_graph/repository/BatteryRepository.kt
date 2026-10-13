package com.gukos.battery_graph.repository

import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.util.BatteryStatusSource
import com.gukos.battery_graph.util.BatterySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

class BatteryRepository(
    private val dao: BatteryDao,
    private val batteryStatus: BatteryStatusSource,
    private val now: () -> Long = System::currentTimeMillis
) {

    // 常駐サービス・画面・Worker から同時に呼ばれても「最新と比較して保存」が割り込まれないようにする
    private val writeMutex = Mutex()

    /**
     * from 以降の記録（古い順）。直前の1件も含む。
     * DB を開けない異常時に、Room が収集側（メインスレッド）で開き直そうとして画面が固まるのを防ぐため、
     * IO スレッドで動かす
     */
    fun getRecordsSince(from: Long): Flow<List<BatteryRecord>> =
        dao.getSince(from).flowOn(Dispatchers.IO)

    /** 現在の状態を無条件に記録する（定期記録・手動記録用） */
    suspend fun recordBatteryLevel() {
        val snapshot = batteryStatus.getSnapshot() ?: return
        writeMutex.withLock { insert(snapshot) }
    }

    // BATTERY_CHANGED は電圧・温度の小さな変化でも頻繁に届くため、残量か充電状態が変わったとき、
    // または温度が前回の記録から 1℃ 以上変わったときだけ記録する
    suspend fun recordIfChanged(snapshot: BatterySnapshot) {
        writeMutex.withLock {
            val latest = dao.getLatest()
            if (latest?.level == snapshot.level &&
                latest.isCharging == snapshot.isCharging &&
                !temperatureChanged(latest.temperature, snapshot.temperature)
            ) return
            insert(snapshot)
        }
    }

    private fun temperatureChanged(recorded: Int?, current: Int?): Boolean =
        recorded != null && current != null &&
            kotlin.math.abs(current - recorded) >= TEMPERATURE_THRESHOLD

    /** 保持期間を過ぎた記録を削除し、削除件数を返す */
    suspend fun deleteExpiredRecords(): Int {
        val cutoff = now() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        return dao.deleteOlderThan(cutoff)
    }

    private suspend fun insert(snapshot: BatterySnapshot) {
        dao.insert(
            BatteryRecord(
                timestamp = now(),
                level = snapshot.level,
                isCharging = snapshot.isCharging,
                temperature = snapshot.temperature,
                voltage = snapshot.voltage
            )
        )
    }

    companion object {
        const val RETENTION_DAYS = 90L

        // 温度の変化で記録するしきい値（0.1℃単位。10 = 1℃）
        private const val TEMPERATURE_THRESHOLD = 10
    }
}
