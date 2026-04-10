package com.gukos.battery_graph.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.gukos.battery_graph.data.database.AppDatabase
import com.gukos.battery_graph.repository.BatteryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {

            val level = intent.getIntExtra(
                BatteryManager.EXTRA_LEVEL, -1
            )
            Log.d("tag","percent=$level")

            val db = AppDatabase.getDatabase(context)
            val repository = BatteryRepository(db.batteryDao(), BatteryManagerWrapper(context))

            CoroutineScope(Dispatchers.IO).launch {
                repository.recordBatteryLevel()
            }
        }
    }
}