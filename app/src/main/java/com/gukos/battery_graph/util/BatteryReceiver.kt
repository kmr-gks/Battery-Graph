package com.gukos.battery_graph.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

/**
 * ACTION_BATTERY_CHANGED を受け取り、残量(%)をコールバックに渡す。
 * このブロードキャストは Manifest では受信できないため、画面表示中に動的登録して使う。
 */
class BatteryReceiver(
    private val onLevelChanged: (Int) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return

        onLevelChanged(level * 100 / scale)
    }
}
