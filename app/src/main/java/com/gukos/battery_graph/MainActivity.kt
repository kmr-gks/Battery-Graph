package com.gukos.battery_graph

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.gukos.battery_graph.service.BatteryMonitorService
import com.gukos.battery_graph.ui.screen.BatteryScreen
import com.gukos.battery_graph.ui.screen.SettingsScreen
import com.gukos.battery_graph.ui.theme.BatteryGraphTheme
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModelFactory
import com.gukos.battery_graph.util.BatteryReceiver

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels {
        val app = application as BatteryGraphApplication
        BatteryViewModelFactory(app.repository, app.monitorSettings, app.graphSettings)
    }

    // 画面表示中だけ残量の変化を受け取る（バックグラウンドの記録は常駐サービスと WorkManager が担当）
    private val batteryReceiver = BatteryReceiver { snapshot ->
        viewModel.onBatteryChanged(snapshot)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BatteryGraphTheme {
                // 画面は2つだけなので、ナビゲーションのライブラリは使わずに切り替える
                var showSettings by rememberSaveable { mutableStateOf(false) }
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(viewModel, onBack = { showSettings = false })
                } else {
                    BatteryScreen(viewModel, onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // OS に止められていた場合に備えて、画面を開くたびに再開する（起動済みなら何もしない）
        BatteryMonitorService.startIfEnabled(this)
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
}
