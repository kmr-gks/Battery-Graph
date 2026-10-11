package com.gukos.battery_graph.ui.graph

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimeTicksTest {

    private val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
    private val newYork = TimeZone.getTimeZone("America/New_York")

    private fun millis(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    /** 時刻を「日 時:分」の文字列にする */
    private fun labels(ticks: List<Long>, zone: TimeZone) = ticks.map {
        val c = Calendar.getInstance(zone).apply { timeInMillis = it }
        "%d %02d:%02d".format(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    @Test
    fun alignsToClockHours() {
        val start = millis(tokyo, 2026, 10, 11, 1, 23)
        val end = millis(tokyo, 2026, 10, 12, 1, 23)

        val ticks = timeTicks(start, end, stepMinutes = 6 * 60, zone = tokyo)

        assertEquals(listOf("11 06:00", "11 12:00", "11 18:00", "12 00:00"), labels(ticks, tokyo))
    }

    @Test
    fun keepsClockHoursOnDaylightSavingStart() {
        // 2026-03-08 はニューヨークで 2:00 が 3:00 になる（1日が23時間）
        val start = millis(newYork, 2026, 3, 8)
        val end = millis(newYork, 2026, 3, 9)

        val ticks = timeTicks(start, end, stepMinutes = 6 * 60, zone = newYork)

        assertEquals(listOf("8 00:00", "8 06:00", "8 12:00", "8 18:00", "9 00:00"), labels(ticks, newYork))
    }

    @Test
    fun keepsClockHoursOnDaylightSavingEnd() {
        // 2026-11-01 はニューヨークで 2:00 が 1:00 に戻る（1日が25時間）
        val start = millis(newYork, 2026, 11, 1)
        val end = millis(newYork, 2026, 11, 2)

        val ticks = timeTicks(start, end, stepMinutes = 6 * 60, zone = newYork)

        assertEquals(listOf("1 00:00", "1 06:00", "1 12:00", "1 18:00", "2 00:00"), labels(ticks, newYork))
    }

    @Test
    fun skipsNonexistentTimeWithoutDuplicates() {
        val start = millis(newYork, 2026, 3, 8)
        val end = millis(newYork, 2026, 3, 8, 5)

        val ticks = timeTicks(start, end, stepMinutes = 60, zone = newYork)

        // 2:00 は存在しない
        assertEquals(listOf("8 00:00", "8 01:00", "8 03:00", "8 04:00", "8 05:00"), labels(ticks, newYork))
    }

    @Test
    fun usesMidnightEveryFewDaysForLongRanges() {
        val start = millis(tokyo, 2026, 10, 1, 12)
        val end = millis(tokyo, 2026, 10, 8, 12)

        val ticks = timeTicks(start, end, stepMinutes = 2 * 24 * 60, zone = tokyo)

        assertEquals(listOf("3 00:00", "5 00:00", "7 00:00"), labels(ticks, tokyo))
    }
}
