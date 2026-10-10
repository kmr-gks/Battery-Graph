package com.gukos.battery_graph.repository

import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.util.BatterySnapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class BatteryRepositoryTest {

    private lateinit var dao: FakeBatteryDao
    private var currentSnapshot: BatterySnapshot? = null
    private var currentTime = START_TIME
    private lateinit var repository: BatteryRepository

    @Before
    fun setUp() {
        dao = FakeBatteryDao()
        repository = BatteryRepository(dao, { currentSnapshot }, { currentTime })
    }

    private fun snapshot(level: Int, isCharging: Boolean = false) =
        BatterySnapshot(level = level, isCharging = isCharging, temperature = 250, voltage = 4000)

    // --- recordBatteryLevel ---

    @Test
    fun recordBatteryLevel_savesCurrentSnapshotWithTimestamp() = runTest {
        currentSnapshot = snapshot(level = 80, isCharging = true)

        repository.recordBatteryLevel()

        val record = dao.all.single()
        assertEquals(START_TIME, record.timestamp)
        assertEquals(80, record.level)
        assertEquals(true, record.isCharging)
        assertEquals(250, record.temperature)
        assertEquals(4000, record.voltage)
    }

    @Test
    fun recordBatteryLevel_savesEvenIfUnchanged() = runTest {
        currentSnapshot = snapshot(level = 80)

        repository.recordBatteryLevel()
        currentTime += MINUTE
        repository.recordBatteryLevel()

        assertEquals(2, dao.all.size)
    }

    @Test
    fun recordBatteryLevel_doesNothingWhenStatusUnavailable() = runTest {
        currentSnapshot = null

        repository.recordBatteryLevel()

        assertTrue(dao.all.isEmpty())
    }

    // --- recordIfChanged ---

    @Test
    fun recordIfChanged_savesFirstRecord() = runTest {
        repository.recordIfChanged(snapshot(level = 80))

        assertEquals(1, dao.all.size)
    }

    @Test
    fun recordIfChanged_skipsWhenLevelAndChargingUnchanged() = runTest {
        repository.recordIfChanged(snapshot(level = 80))
        currentTime += MINUTE
        // 温度・電圧だけが変わっても記録しない
        repository.recordIfChanged(snapshot(level = 80).copy(temperature = 300, voltage = 3900))

        assertEquals(1, dao.all.size)
    }

    @Test
    fun recordIfChanged_savesWhenLevelChanges() = runTest {
        repository.recordIfChanged(snapshot(level = 80))
        currentTime += MINUTE
        repository.recordIfChanged(snapshot(level = 79))

        assertEquals(listOf(80, 79), dao.all.map { it.level })
    }

    @Test
    fun recordIfChanged_savesWhenChargingStateChanges() = runTest {
        repository.recordIfChanged(snapshot(level = 80, isCharging = false))
        currentTime += MINUTE
        repository.recordIfChanged(snapshot(level = 80, isCharging = true))

        assertEquals(listOf(false, true), dao.all.map { it.isCharging })
    }

    @Test
    fun recordIfChanged_savesWhenLatestIsV1RecordWithoutChargingState() = runTest {
        // DB version 1 で保存された記録は isCharging が null
        dao.insert(BatteryRecord(timestamp = START_TIME - MINUTE, level = 80))

        repository.recordIfChanged(snapshot(level = 80, isCharging = false))

        assertEquals(2, dao.all.size)
    }

    @Test
    fun recordIfChanged_concurrentCallsSaveOnlyOnce() = runTest {
        // 常駐サービスと画面の Receiver が同じ変化をほぼ同時に受け取る場合
        dao.yieldInGetLatest = true

        launch { repository.recordIfChanged(snapshot(level = 80)) }
        launch { repository.recordIfChanged(snapshot(level = 80)) }
        testScheduler.advanceUntilIdle()

        assertEquals(1, dao.all.size)
    }

    // --- deleteExpiredRecords ---

    @Test
    fun deleteExpiredRecords_deletesOnlyRecordsOlderThanRetention() = runTest {
        val retention = TimeUnit.DAYS.toMillis(BatteryRepository.RETENTION_DAYS)
        dao.insert(BatteryRecord(timestamp = START_TIME - retention - 1, level = 10))
        dao.insert(BatteryRecord(timestamp = START_TIME - retention, level = 20))
        dao.insert(BatteryRecord(timestamp = START_TIME, level = 30))

        val deleted = repository.deleteExpiredRecords()

        assertEquals(1, deleted)
        assertEquals(listOf(20, 30), dao.all.map { it.level })
    }

    private companion object {
        // 2026-01-01T00:00:00Z
        const val START_TIME = 1_767_225_600_000L
        val MINUTE = TimeUnit.MINUTES.toMillis(1)
    }
}
