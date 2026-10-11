package com.gukos.battery_graph.domain

import com.gukos.battery_graph.data.entity.BatteryRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DownsampleTest {

    private fun record(minute: Int, level: Int, isCharging: Boolean = false) =
        BatteryRecord(timestamp = minute * 60_000L, level = level, isCharging = isCharging)

    // 1分ごとに 100% から 1% ずつ下がり、0% の次は 100% に戻る記録
    private fun sawtooth(count: Int) = List(count) { record(it, 100 - it % 101) }

    @Test
    fun keepsRecordsWhenFewEnough() {
        val records = sawtooth(100)

        assertSame(records, downsample(records, buckets = 50))
    }

    @Test
    fun reducesToAboutTwoPointsPerBucket() {
        val records = sawtooth(10_000)

        val result = downsample(records, buckets = 100)

        assertTrue("size=${result.size}", result.size <= 100 * 2 + 2)
        assertTrue("size=${result.size}", result.size >= 100)
    }

    @Test
    fun keepsFirstLastAndOrder() {
        val records = sawtooth(10_000)

        val result = downsample(records, buckets = 100)

        assertEquals(records.first(), result.first())
        assertEquals(records.last(), result.last())
        assertEquals(result.sortedBy { it.timestamp }, result)
    }

    @Test
    fun keepsPeaksAndValleys() {
        val records = sawtooth(10_000)

        val result = downsample(records, buckets = 100)

        assertEquals(100, result.maxOf { it.level })
        assertEquals(0, result.minOf { it.level })
    }

    @Test
    fun keepsChargingTransitions() {
        // 一定の残量のまま、1件だけ充電中になる
        val records = List(10_000) { record(it, 50, isCharging = it == 5_000) }

        val result = downsample(records, buckets = 100)

        val charging = result.indexOfFirst { it.isCharging == true }
        assertEquals(5_000, result[charging].timestamp / 60_000L)
        // 色の境目がずれないよう、切り替わりの前後の点も残る
        assertEquals(4_999, result[charging - 1].timestamp / 60_000L)
        assertEquals(5_001, result[charging + 1].timestamp / 60_000L)
    }
}
