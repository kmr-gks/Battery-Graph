package com.gukos.battery_graph

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gukos.battery_graph.data.database.AppDatabase
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.util.BatteryManagerWrapper
import com.gukos.battery_graph.util.BatteryStatusSource
import com.gukos.battery_graph.util.MonitorSettings
import com.gukos.battery_graph.worker.BatteryWorker
import java.util.concurrent.TimeUnit

class BatteryGraphApplication : Application() {

    val batteryStatus: BatteryStatusSource by lazy { BatteryManagerWrapper(this) }

    // Activity・Service・Worker から共通で使う Repository
    val repository: BatteryRepository by lazy {
        BatteryRepository(AppDatabase.getDatabase(this).batteryDao(), batteryStatus)
    }

    val monitorSettings: MonitorSettings by lazy { MonitorSettings(this) }

    override fun onCreate() {
        super.onCreate()
        scheduleBatteryWorker()
    }

    // 15分は WorkManager の最短間隔。端末再起動後も WorkManager が自動で再開する
    private fun scheduleBatteryWorker() {
        val workRequest =
            PeriodicWorkRequestBuilder<BatteryWorker>(15, TimeUnit.MINUTES).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BatteryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
