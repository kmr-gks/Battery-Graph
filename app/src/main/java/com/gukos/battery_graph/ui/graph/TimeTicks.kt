package com.gukos.battery_graph.ui.graph

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private val MINUTES_PER_DAY = TimeUnit.DAYS.toMinutes(1)

/**
 * start〜end の間の目盛りの時刻を、時計の時刻の区切り（0:00, 6:00, 12:00… や日付）に合わせて返す。
 *
 * 経過時間を足していくと夏時間の切り替え日に時計の時刻とずれるため、日ごとに時・分を設定して求める。
 *
 * @param stepMinutes 目盛りの間隔（分）。1日未満なら1日（1440分）を割り切れる値、1日以上なら日単位
 */
internal fun timeTicks(
    start: Long,
    end: Long,
    stepMinutes: Long,
    zone: TimeZone = TimeZone.getDefault()
): List<Long> {
    val day = Calendar.getInstance(zone).apply {
        timeInMillis = start
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val ticks = mutableListOf<Long>()
    while (day.timeInMillis <= end) {
        if (stepMinutes >= MINUTES_PER_DAY) {
            if (day.timeInMillis >= start) ticks += day.timeInMillis
            day.add(Calendar.DAY_OF_YEAR, (stepMinutes / MINUTES_PER_DAY).toInt())
        } else {
            val tick = day.clone() as Calendar
            for (minute in 0 until MINUTES_PER_DAY step stepMinutes) {
                tick.set(Calendar.HOUR_OF_DAY, (minute / 60).toInt())
                tick.set(Calendar.MINUTE, (minute % 60).toInt())
                val time = tick.timeInMillis
                // 夏時間で存在しない時刻は次の時刻に繰り上がるため、重複は除く
                if (time in start..end && (ticks.isEmpty() || time > ticks.last())) ticks += time
            }
            day.add(Calendar.DAY_OF_YEAR, 1)
        }
        // 夏時間などで 0:00 が存在しない日でも、次の日の 0:00 にそろえる
        day.set(Calendar.HOUR_OF_DAY, 0)
        day.set(Calendar.MINUTE, 0)
    }
    return ticks
}
