package com.gukos.battery_graph.util

import android.content.Intent
import android.os.BatteryManager

/** ACTION_BATTERY_CHANGED から読み取ったある時点のバッテリー状態 */
data class BatterySnapshot(
    val level: Int,
    // 電源に接続されているか（満充電で接続中も含む）
    val isCharging: Boolean,
    val temperature: Int?,
    val voltage: Int?
) {
    companion object {
        fun fromIntent(intent: Intent): BatterySnapshot? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return null

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

            return BatterySnapshot(
                level = level * 100 / scale,
                isCharging = plugged != 0,
                temperature = temperature.takeIf { it != Int.MIN_VALUE },
                voltage = voltage.takeIf { it > 0 }
            )
        }
    }
}
