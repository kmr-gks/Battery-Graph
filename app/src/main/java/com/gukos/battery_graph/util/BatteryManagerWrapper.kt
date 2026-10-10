package com.gukos.battery_graph.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class BatteryManagerWrapper(private val context: Context) : BatteryStatusSource {

    /** 現在のバッテリー状態。ACTION_BATTERY_CHANGED は sticky なので receiver なしで最新値を取得できる */
    override fun getSnapshot(): BatterySnapshot? {
        val intent = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        ) ?: return null
        return BatterySnapshot.fromIntent(intent)
    }
}
