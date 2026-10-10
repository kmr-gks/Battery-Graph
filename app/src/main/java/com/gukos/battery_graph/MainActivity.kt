package com.gukos.battery_graph

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gukos.battery_graph.ui.graph.GraphScreen
import com.gukos.battery_graph.ui.theme.BatteryGraphTheme
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModelFactory
import com.gukos.battery_graph.util.BatteryReceiver
import com.gukos.battery_graph.worker.BatteryWorker
import java.util.Date

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels {
        BatteryViewModelFactory(applicationContext)
    }

    // 画面表示中だけ残量の変化を受け取る（バックグラウンドの記録は WorkManager が担当）
    private val batteryReceiver = BatteryReceiver { level ->
        viewModel.onBatteryLevelChanged(level)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        scheduleBatteryWorker(applicationContext)

        setContent {
            BatteryGraphTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        Box(modifier = Modifier.weight(1f)) {
                            GraphScreen(viewModel)
                        }

                        Box(modifier = Modifier.weight(0.31f)) {
                            BatteryScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(batteryReceiver)
        super.onStop()
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
