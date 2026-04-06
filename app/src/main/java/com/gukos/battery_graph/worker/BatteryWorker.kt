package com.gukos.battery_graph.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gukos.battery_graph.data.database.AppDatabase
import com.gukos.battery_graph.repository.BatteryRepository
import com.gukos.battery_graph.util.BatteryManagerWrapper


class BatteryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.batteryDao()
        val wrapper = BatteryManagerWrapper(applicationContext)
        val repository = BatteryRepository(dao, wrapper)

        repository.recordBatteryLevel()

        return Result.success()
    }
}