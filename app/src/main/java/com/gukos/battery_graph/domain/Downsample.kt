package com.gukos.battery_graph.domain

import com.gukos.battery_graph.data.entity.BatteryRecord

/**
 * グラフに描く点を減らす。画面の幅より多い点を描いても見た目は変わらず、描画が重くなるだけのため。
 *
 * 期間を [buckets] 個の区間に分け、各区間で [value] が最小・最大の点を残す（山や谷の形を保つ）。
 * 充電状態が切り替わる点と最初・最後の点も残し、充電区間の色の境目がずれないようにする。
 *
 * @param records 古い順の記録
 * @param value グラフに描く値（既定は残量）
 * @return 古い順の記録。点が少なければそのまま返す
 */
fun downsample(
    records: List<BatteryRecord>,
    buckets: Int,
    value: (BatteryRecord) -> Double = { it.level.toDouble() }
): List<BatteryRecord> {
    if (records.size <= buckets * 2) return records

    val first = records.first().timestamp
    val span = (records.last().timestamp - first).coerceAtLeast(1)
    val keep = BooleanArray(records.size)
    keep[0] = true
    keep[records.lastIndex] = true

    var bucketStart = 0
    while (bucketStart < records.size) {
        val bucket = ((records[bucketStart].timestamp - first) * buckets / span)
        var end = bucketStart
        var minIndex = bucketStart
        var maxIndex = bucketStart
        while (end < records.size &&
            (records[end].timestamp - first) * buckets / span == bucket
        ) {
            if (value(records[end]) < value(records[minIndex])) minIndex = end
            if (value(records[end]) > value(records[maxIndex])) maxIndex = end
            end++
        }
        keep[minIndex] = true
        keep[maxIndex] = true
        bucketStart = end
    }

    // 充電状態が切り替わる前後の点
    for (i in 1 until records.size) {
        if (records[i].isCharging != records[i - 1].isCharging) {
            keep[i - 1] = true
            keep[i] = true
        }
    }
    return records.filterIndexed { i, _ -> keep[i] }
}
