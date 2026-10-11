package com.gukos.battery_graph.domain

import com.gukos.battery_graph.data.entity.BatteryRecord
import java.util.concurrent.TimeUnit

/** 直近の放電ペースと、そのペースで 0% になるまでの目安 */
data class DischargeEstimate(
    val percentPerHour: Double,
    val remainingMinutes: Long
) {
    companion object {
        // この時間より前の記録はペースの計算に使わない
        private val LOOKBACK_MILLIS = TimeUnit.HOURS.toMillis(3)

        // 短すぎる区間ではペースがぶれるため推定しない
        private val MIN_SPAN_MILLIS = TimeUnit.MINUTES.toMillis(30)

        // 残量は 1% 単位なので、1% の減少だけではペースが大きくぶれる
        private const val MIN_DROP_PERCENT = 2.0

        /**
         * 最新の記録から遡って、充電していない連続区間（最大3時間）の減少量からペースを求める。
         * 区間が3時間より前から続いている場合は、3時間前の時点の残量を前後の記録から補間する。
         * 充電中・区間が短い・減少が小さい場合は null。
         *
         * @param records 古い順の記録
         */
        fun estimate(records: List<BatteryRecord>, now: Long): DischargeEstimate? {
            val latest = records.lastOrNull() ?: return null
            if (latest.isCharging == true) return null

            val lookbackStart = now - LOOKBACK_MILLIS
            // 放電区間のうち、3時間前以降の最も古い記録（start）と、その1つ前の記録（before）
            var start = latest
            var before: BatteryRecord? = null
            for (record in records.asReversed().drop(1)) {
                if (record.isCharging == true) break
                if (record.timestamp < lookbackStart) {
                    before = record
                    break
                }
                start = record
            }

            // 区間が3時間前より前から続いていれば、3時間前の残量を線形補間する
            val (startTime, startLevel) = if (before != null && start.timestamp > lookbackStart) {
                val ratio = (lookbackStart - before.timestamp).toDouble() /
                    (start.timestamp - before.timestamp)
                lookbackStart to before.level + (start.level - before.level) * ratio
            } else {
                start.timestamp to start.level.toDouble()
            }

            val spanMillis = now - startTime
            if (spanMillis < MIN_SPAN_MILLIS) return null
            val drop = startLevel - latest.level
            if (drop < MIN_DROP_PERCENT) return null

            val percentPerHour = drop * TimeUnit.HOURS.toMillis(1) / spanMillis
            val remainingMinutes = (latest.level / percentPerHour * 60).toLong()
            return DischargeEstimate(percentPerHour, remainingMinutes)
        }
    }
}
