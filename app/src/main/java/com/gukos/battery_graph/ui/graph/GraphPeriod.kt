package com.gukos.battery_graph.ui.graph

import androidx.annotation.StringRes
import com.gukos.battery_graph.R
import java.util.concurrent.TimeUnit

/** グラフに表示する期間 */
enum class GraphPeriod(@StringRes val labelRes: Int, val durationMillis: Long) {
    HOURS_6(R.string.period_6h, TimeUnit.HOURS.toMillis(6)),
    HOURS_24(R.string.period_24h, TimeUnit.HOURS.toMillis(24)),
    DAYS_7(R.string.period_7d, TimeUnit.DAYS.toMillis(7)),
    DAYS_30(R.string.period_30d, TimeUnit.DAYS.toMillis(30)),
}
