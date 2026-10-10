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

        /**
         * 最新の記録から遡って、充電していない連続区間（最大3時間）の減少量からペースを求める。
         * 充電中・区間が短い・減っていない場合は null。
         *
         * @param records 古い順の記録
         */
        fun estimate(records: List<BatteryRecord>, now: Long): DischargeEstimate? {
            val latest = records.lastOrNull() ?: return null
            if (latest.isCharging == true) return null

            val lookbackStart = now - LOOKBACK_MILLIS
            var start = latest
            for (record in records.asReversed()) {
                if (record.isCharging == true) break
                start = record
                if (record.timestamp <= lookbackStart) break
            }

            val spanMillis = now - start.timestamp
            if (spanMillis < MIN_SPAN_MILLIS) return null
            val drop = start.level - latest.level
            if (drop <= 0) return null

            val percentPerHour = drop * TimeUnit.HOURS.toMillis(1).toDouble() / spanMillis
            val remainingMinutes = (latest.level / percentPerHour * 60).toLong()
            return DischargeEstimate(percentPerHour, remainingMinutes)
        }
    }
}
