package com.gukos.battery_graph.domain

import com.gukos.battery_graph.data.entity.BatteryRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class DischargeEstimateTest {

    private val now = 1_767_225_600_000L

    private fun record(minutesAgo: Long, level: Int, isCharging: Boolean? = false) =
        BatteryRecord(
            timestamp = now - TimeUnit.MINUTES.toMillis(minutesAgo),
            level = level,
            isCharging = isCharging
        )

    @Test
    fun estimate_usesDropOverElapsedTime() {
        // 2時間で10%減少 → 5%/時、残り50%なら10時間
        val records = listOf(record(120, 60), record(60, 55), record(0, 50))

        val estimate = DischargeEstimate.estimate(records, now)!!

        assertEquals(5.0, estimate.percentPerHour, 0.001)
        assertEquals(600L, estimate.remainingMinutes)
    }

    @Test
    fun estimate_spanEndsAtNowNotLatestRecord() {
        // 最後の記録から時間が経っていれば、その分ペースは緩やかになる
        val records = listOf(record(120, 60), record(60, 50))

        val estimate = DischargeEstimate.estimate(records, now)!!

        assertEquals(5.0, estimate.percentPerHour, 0.001)
    }

    @Test
    fun estimate_startsAfterLastChargingRecord() {
        val records = listOf(
            record(240, 20),
            record(180, 100, isCharging = true),
            record(60, 100),
            record(0, 95)
        )

        val estimate = DischargeEstimate.estimate(records, now)!!

        // 充電終了後の60分で5%減少
        assertEquals(5.0, estimate.percentPerHour, 0.001)
    }

    @Test
    fun estimate_looksBackAtMostThreeHours() {
        val records = listOf(record(600, 100), record(180, 80), record(0, 71))

        val estimate = DischargeEstimate.estimate(records, now)!!

        // 3時間前の 80% からの減少だけを使う
        assertEquals(3.0, estimate.percentPerHour, 0.001)
    }

    @Test
    fun estimate_interpolatesLevelAtLookbackStart() {
        // 3時間前より前の記録（600分前）と3時間以内の記録（170分前）の間で、180分前の残量を補間する
        val records = listOf(record(600, 100), record(170, 80), record(0, 71))

        val estimate = DischargeEstimate.estimate(records, now)!!

        // 180分前の残量 = 100 - 20 × (420 / 430) ≈ 80.47%、3時間で約9.47%減少
        assertEquals(3.16, estimate.percentPerHour, 0.01)
    }

    @Test
    fun estimate_interpolatesWhenNoRecordWithinLookback() {
        // 記録の間隔が長く、3時間以内には最新の記録しかない
        val records = listOf(record(600, 100), record(60, 70))

        val estimate = DischargeEstimate.estimate(records, now)!!

        // 3時間前の残量 = 100 - 30 × (420 / 540) ≈ 76.67%、3時間で約6.67%減少
        assertEquals(2.22, estimate.percentPerHour, 0.01)
    }

    @Test
    fun estimate_nullWhenDropTooSmall() {
        // 充電を外した直後などで 1% しか減っていない
        val records = listOf(record(60, 100), record(0, 99))

        assertNull(DischargeEstimate.estimate(records, now))
    }

    @Test
    fun estimate_nullWhenCharging() {
        val records = listOf(record(120, 60), record(0, 50, isCharging = true))

        assertNull(DischargeEstimate.estimate(records, now))
    }

    @Test
    fun estimate_nullWhenSpanTooShort() {
        val records = listOf(record(20, 60), record(0, 58))

        assertNull(DischargeEstimate.estimate(records, now))
    }

    @Test
    fun estimate_nullWhenNotDecreasing() {
        val records = listOf(record(120, 50), record(0, 50))

        assertNull(DischargeEstimate.estimate(records, now))
    }

    @Test
    fun estimate_nullWhenEmpty() {
        assertNull(DischargeEstimate.estimate(emptyList(), now))
    }
}
