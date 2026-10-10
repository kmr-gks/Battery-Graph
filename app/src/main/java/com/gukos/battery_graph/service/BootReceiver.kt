package com.gukos.battery_graph.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 端末の再起動後・アプリ更新後に高精度記録を再開する */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> BatteryMonitorService.startIfEnabled(context)
        }
    }
}
