package com.gukos.battery_graph.data.dao

import androidx.room.*
import com.gukos.battery_graph.data.entity.BatteryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {

    @Insert
    suspend fun insert(record: BatteryRecord)

    /**
     * from 以降の記録を古い順に返す。グラフの左端まで線がつながるよう、from より前の直近1件も含める
     */
    @Query(
        """
        SELECT * FROM battery_records
        WHERE timestamp >= (
            SELECT COALESCE(MAX(timestamp), 0) FROM battery_records WHERE timestamp < :from
        )
        ORDER BY timestamp ASC
        """
    )
    fun getSince(from: Long): Flow<List<BatteryRecord>>

    /**
     * 最後に追加した記録。時刻順ではなく追加順で選ぶ。端末の時刻が戻ると、未来の時刻の記録が
     * いつまでも「最新」とみなされ、重複を防ぐ比較が効かなくなるため
     */
    @Query("SELECT * FROM battery_records ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): BatteryRecord?

    @Query("DELETE FROM battery_records WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}
