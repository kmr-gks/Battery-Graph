package com.gukos.battery_graph

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gukos.battery_graph.service.BatteryMonitorService
import com.gukos.battery_graph.ui.graph.GraphScreen
import com.gukos.battery_graph.ui.theme.BatteryGraphTheme
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModelFactory
import com.gukos.battery_graph.util.BatteryReceiver
import java.util.Date

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels {
        val app = application as BatteryGraphApplication
        BatteryViewModelFactory(app.repository, app.monitorSettings)
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

    @Composable
    fun BatteryScreen(viewModel: BatteryViewModel) {
        val records by viewModel.records.collectAsState()

        Column {
            HighPrecisionSwitch(viewModel)

            Button(onClick = { viewModel.recordNow() }) {
                Text("記録する")
            }

            LazyColumn {
                items(records) { record ->
                    val charging = if (record.isCharging == true) " (充電中)" else ""
                    Text("${Date(record.timestamp)}: ${record.level}%$charging")
                }
            }
        }
    }

    @Composable
    fun HighPrecisionSwitch(viewModel: BatteryViewModel) {
        val context = LocalContext.current
        val enabled by viewModel.highPrecisionEnabled.collectAsState()

        // 通知が許可されなくても記録は動く（通知が表示されないだけ）
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        fun setEnabled(value: Boolean) {
            viewModel.setHighPrecisionEnabled(value)
            if (value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                BatteryMonitorService.startIfEnabled(context)
            } else {
                BatteryMonitorService.stop(context)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.high_precision_label))
                Text(
                    stringResource(R.string.high_precision_description),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = enabled, onCheckedChange = ::setEnabled)
        }
    }
}
