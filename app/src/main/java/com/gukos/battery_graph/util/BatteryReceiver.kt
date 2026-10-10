package com.gukos.battery_graph.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * ACTION_BATTERY_CHANGED を受け取り、バッテリー状態をコールバックに渡す。
 * このブロードキャストは Manifest では受信できないため、画面表示中に動的登録して使う。
 */
class BatteryReceiver(
    private val onBatteryChanged: (BatterySnapshot) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
        BatterySnapshot.fromIntent(intent)?.let(onBatteryChanged)
    }
}
