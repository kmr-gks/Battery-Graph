package com.gukos.battery_graph.util

import android.content.Intent
import android.os.BatteryManager

/** 電源の接続と充電の状態（表示用） */
enum class ChargeStatus {
    /** 電源に接続されていない */
    DISCHARGING,

    /** 電源に接続され、充電している */
    CHARGING,

    /** 電源に接続されているが充電していない（充電上限機能・給電不足など） */
    PLUGGED_NOT_CHARGING,

    /** 電源に接続され、満充電 */
    FULL,
}

/** ACTION_BATTERY_CHANGED から読み取ったある時点のバッテリー状態 */
data class BatterySnapshot(
    val level: Int,
    // 電源に接続されているか（満充電で接続中も含む）。記録とグラフの色分けにはこちらを使う
    val isCharging: Boolean,
    val temperature: Int?,
    val voltage: Int?,
    // 画面・通知の表示用の詳しい状態
    val chargeStatus: ChargeStatus =
        if (isCharging) ChargeStatus.CHARGING else ChargeStatus.DISCHARGING
) {
    companion object {
        fun fromIntent(intent: Intent): BatterySnapshot? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return null

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

            return BatterySnapshot(
                level = level * 100 / scale,
                isCharging = plugged != 0,
                temperature = temperature.takeIf { it != Int.MIN_VALUE },
                voltage = voltage.takeIf { it > 0 },
                chargeStatus = chargeStatusOf(plugged, status)
            )
        }

        private fun chargeStatusOf(plugged: Int, status: Int): ChargeStatus = when {
            plugged == 0 -> ChargeStatus.DISCHARGING
            status == BatteryManager.BATTERY_STATUS_FULL -> ChargeStatus.FULL
            status == BatteryManager.BATTERY_STATUS_NOT_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeStatus.PLUGGED_NOT_CHARGING
            // 充電中、または状態が取れない端末では接続中を充電中とみなす
            else -> ChargeStatus.CHARGING
        }
    }
}
