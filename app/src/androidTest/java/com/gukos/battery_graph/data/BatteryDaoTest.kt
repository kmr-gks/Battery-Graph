package com.gukos.battery_graph.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gukos.battery_graph.data.dao.BatteryDao
import com.gukos.battery_graph.data.database.AppDatabase
import com.gukos.battery_graph.data.entity.BatteryRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BatteryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.batteryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getSince_returnsOldestFirst() = runTest {
        dao.insert(BatteryRecord(timestamp = 2_000, level = 70))
        dao.insert(BatteryRecord(timestamp = 1_000, level = 80))
        dao.insert(BatteryRecord(timestamp = 3_000, level = 60))

        assertEquals(listOf(80, 70, 60), dao.getSince(0).first().map { it.level })
    }

    @Test
    fun getSince_includesOneRecordBeforeFrom() = runTest {
        dao.insert(BatteryRecord(timestamp = 1_000, level = 90))
        dao.insert(BatteryRecord(timestamp = 2_000, level = 80))
        dao.insert(BatteryRecord(timestamp = 3_000, level = 70))
        dao.insert(BatteryRecord(timestamp = 4_000, level = 60))

        // 2_500 より前の直近1件（2_000）から返す
        assertEquals(listOf(80, 70, 60), dao.getSince(2_500).first().map { it.level })
    }


    @Test
    fun getLatest_returnsNullWhenEmpty() = runTest {
        assertNull(dao.getLatest())
    }

    @Test
    fun getLatest_returnsNewestRecord() = runTest {
        dao.insert(BatteryRecord(timestamp = 2_000, level = 70, isCharging = true))
        dao.insert(BatteryRecord(timestamp = 1_000, level = 80, isCharging = false))

        val latest = dao.getLatest()!!
        assertEquals(70, latest.level)
        assertEquals(true, latest.isCharging)
    }

    @Test
    fun deleteOlderThan_keepsBoundaryRecord() = runTest {
        dao.insert(BatteryRecord(timestamp = 999, level = 90))
        dao.insert(BatteryRecord(timestamp = 1_000, level = 80))
        dao.insert(BatteryRecord(timestamp = 1_001, level = 70))

        val deleted = dao.deleteOlderThan(1_000)

        assertEquals(1, deleted)
        assertEquals(listOf(80, 70), dao.getSince(0).first().map { it.level })
    }
}
