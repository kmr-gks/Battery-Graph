package com.gukos.battery_graph.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gukos.battery_graph.BatteryGraphApplication


class BatteryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as BatteryGraphApplication).repository

        return try {
            repository.recordBatteryLevel()
            repository.deleteExpiredRecords()
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "battery record failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "battery_record_work"
        private const val TAG = "BatteryWorker"
    }
}
