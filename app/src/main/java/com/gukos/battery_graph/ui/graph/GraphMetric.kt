package com.gukos.battery_graph.ui.graph

import androidx.annotation.StringRes
import com.gukos.battery_graph.R
import com.gukos.battery_graph.data.entity.BatteryRecord

/** グラフに表示する項目 */
enum class GraphMetric(@StringRes val labelRes: Int) {
    LEVEL(R.string.metric_level),
    TEMPERATURE(R.string.metric_temperature),
    VOLTAGE(R.string.metric_voltage);

    /** 記録から表示する値を取り出す（残量は %、温度は ℃、電圧は V）。値がない記録は null */
    fun valueOf(record: BatteryRecord): Double? = when (this) {
        LEVEL -> record.level.toDouble()
        TEMPERATURE -> record.temperature?.let { it / 10.0 }
        VOLTAGE -> record.voltage?.let { it / 1000.0 }
    }
}
