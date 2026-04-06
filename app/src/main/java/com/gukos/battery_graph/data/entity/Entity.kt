package com.gukos.battery_graph.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_records")
data class BatteryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val level: Int
)