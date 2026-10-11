package com.gukos.battery_graph.ui.graph

import androidx.annotation.StringRes
import com.gukos.battery_graph.R
import java.util.concurrent.TimeUnit

/**
 * グラフに表示する期間
 *
 * @param labelRes ボタンに表示する短い名前（例: 6h）
 * @param descriptionRes 読み上げ用の名前（例: 6 hours）
 */
enum class GraphPeriod(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val durationMillis: Long
) {
    HOURS_6(R.string.period_6h, R.string.period_6h_description, TimeUnit.HOURS.toMillis(6)),
    HOURS_24(R.string.period_24h, R.string.period_24h_description, TimeUnit.HOURS.toMillis(24)),
    DAYS_7(R.string.period_7d, R.string.period_7d_description, TimeUnit.DAYS.toMillis(7)),
    DAYS_30(R.string.period_30d, R.string.period_30d_description, TimeUnit.DAYS.toMillis(30)),
}
