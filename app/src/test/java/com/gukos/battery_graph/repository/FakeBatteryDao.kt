package com.gukos.battery_graph.repository

import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.entity.BatteryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** メモリ上で BatteryDao を再現する。SQL そのものは androidTest の BatteryDaoTest で確認する */
class FakeBatteryDao : BatteryDao {

    private val records = MutableStateFlow<List<BatteryRecord>>(emptyList())
    private var nextId = 1

    val all: List<BatteryRecord> get() = records.value

    override suspend fun insert(record: BatteryRecord) {
        records.value = records.value + record.copy(id = nextId++)
    }

    override fun getAll(): Flow<List<BatteryRecord>> =
        records.map { list -> list.sortedByDescending { it.timestamp } }

    override suspend fun getLatest(): BatteryRecord? =
        records.value.maxByOrNull { it.timestamp }

    override suspend fun deleteOlderThan(timestamp: Long): Int {
        val (expired, kept) = records.value.partition { it.timestamp < timestamp }
        records.value = kept
        return expired.size
    }
}
