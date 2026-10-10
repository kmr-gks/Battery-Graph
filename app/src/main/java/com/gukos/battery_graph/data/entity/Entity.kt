package com.gukos.battery_graph.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_records",
    indices = [Index(value = ["timestamp"])]
)
data class BatteryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val level: Int,
    // 以下は DB version 2 で追加。version 1 の記録では null
    val isCharging: Boolean? = null,
    // 0.1℃単位（BatteryManager.EXTRA_TEMPERATURE と同じ）
    val temperature: Int? = null,
    // mV
    val voltage: Int? = null
)
