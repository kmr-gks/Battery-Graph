package com.gukos.battery_graph

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gukos.battery_graph.ui.graph.GraphScreen
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModelFactory
import com.gukos.battery_graph.util.BatteryReceiver
import com.gukos.battery_graph.worker.BatteryWorker
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleBatteryWorker(applicationContext)

        val receiver = BatteryReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(receiver, filter)

        setContent {
            val viewModel: BatteryViewModel = viewModel(
                factory = BatteryViewModelFactory(applicationContext)
            )

            Column(modifier = Modifier.fillMaxSize()) {

                Box(modifier = Modifier.weight(1f)) {
                    GraphScreen(viewModel)
                }

                Box(modifier = Modifier.weight(0.31f)) {
                    BatteryScreen(viewModel)
                }
            }
        }
    }

    @Composable
    fun BatteryScreen(viewModel: BatteryViewModel) {
        val records by viewModel.records.collectAsState()

        Column {
            Button(onClick = { viewModel.recordNow() }) {
                Text("記録する")
            }

            LazyColumn {
                items(records) { record ->
                    Text("${Date(record.timestamp)}: ${record.level}%")
                }
            }
        }
    }

    private fun scheduleBatteryWorker(context: Context) {

        val workRequest =
            PeriodicWorkRequestBuilder<BatteryWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "battery_record_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}