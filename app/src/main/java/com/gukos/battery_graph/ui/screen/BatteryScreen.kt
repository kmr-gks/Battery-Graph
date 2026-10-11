package com.gukos.battery_graph.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gukos.battery_graph.R
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.domain.DischargeEstimate
import com.gukos.battery_graph.service.BatteryMonitorService
import com.gukos.battery_graph.ui.graph.BatteryGraph
import com.gukos.battery_graph.ui.graph.GraphPeriod
import com.gukos.battery_graph.ui.theme.chargingColor
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.util.BatterySnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel) {
    val records by viewModel.records.collectAsState()
    val period by viewModel.period.collectAsState()
    val currentStatus by viewModel.currentStatus.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(currentStatus, records)

            PeriodSelector(period, viewModel::selectPeriod)

            BatteryGraph(
                records = records,
                period = period,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            HighPrecisionSwitch(viewModel)
        }
    }
}

/** 現在の残量・充電状態・温度・電圧と、減少ペースの目安 */
@Composable
private fun StatusCard(status: BatterySnapshot?, records: List<BatteryRecord>) {
    // 画面を開いた直後で最新状態が届いていなければ、最後の記録を表示する
    val level = status?.level ?: records.lastOrNull()?.level
    val isCharging = status?.isCharging ?: (records.lastOrNull()?.isCharging == true)
    val estimate = DischargeEstimate.estimate(records, System.currentTimeMillis())

    Card(
        // 残量・状態・温度などを1つのまとまりとして読み上げる
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = level?.let { stringResource(R.string.status_level, it) } ?: "—",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCharging) chargingColor() else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(
                        if (isCharging) R.string.status_charging else R.string.status_discharging
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCharging) chargingColor() else MaterialTheme.colorScheme.onSurface
                )
                if (status?.temperature != null && status.voltage != null) {
                    Text(
                        text = stringResource(
                            R.string.status_details,
                            status.temperature / 10.0,
                            status.voltage / 1000.0
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isCharging) {
                    Text(
                        text = estimate?.let {
                            stringResource(
                                R.string.status_pace,
                                it.percentPerHour,
                                formatDuration(it.remainingMinutes)
                            )
                        } ?: stringResource(R.string.status_pace_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours.toInt(), (minutes % 60).toInt())
    } else {
        stringResource(R.string.duration_minutes, minutes.toInt())
    }
}

@Composable
private fun PeriodSelector(selected: GraphPeriod, onSelect: (GraphPeriod) -> Unit) {
    val periods = GraphPeriod.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        periods.forEachIndexed { index, period ->
            val description = stringResource(period.descriptionRes)
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                label = {
                    // 「6h」ではなく「6 hours」と読み上げる
                    Text(
                        stringResource(period.labelRes),
                        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
                    )
                }
            )
        }
    }
}

@Composable
private fun HighPrecisionSwitch(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    val enabled by viewModel.highPrecisionEnabled.collectAsState()

    // 許可の前に出した通知は表示されないため、許可ダイアログの結果が出てからサービスを起動する。
    // 通知が許可されなくても記録は動く（通知が表示されないだけ）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { BatteryMonitorService.startIfEnabled(context) }

    fun setEnabled(value: Boolean) {
        viewModel.setHighPrecisionEnabled(value)
        if (value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                BatteryMonitorService.startIfEnabled(context)
            }
        } else {
            BatteryMonitorService.stop(context)
        }
    }

    // 行全体をスイッチとして扱い、文字の部分を押しても切り替わるようにする。
    // TalkBack ではラベルと状態がまとめて読み上げられる
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = enabled, role = Role.Switch, onValueChange = ::setEnabled)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.high_precision_label))
            Text(
                stringResource(R.string.high_precision_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}
