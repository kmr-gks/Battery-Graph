package com.gukos.battery_graph.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.gukos.battery_graph.util.ChargeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel) {
    val records by viewModel.records.collectAsState()
    val period by viewModel.period.collectAsState()
    val currentStatus by viewModel.currentStatus.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        // 横画面でカメラの切り欠きに重ならないようにする
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            val controls = @Composable {
                StatusCard(currentStatus, records)
                PeriodSelector(period, viewModel::selectPeriod)
            }
            when {
                // 横長: 左に操作、右にグラフを大きく表示する
                maxWidth > maxHeight -> Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        controls()
                        HighPrecisionSwitch(viewModel)
                    }
                    BatteryGraph(
                        records = records,
                        period = period,
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(bottom = 8.dp)
                    )
                }

                // 縦長で十分な高さがある: グラフで残りの高さを埋める
                maxHeight >= MIN_HEIGHT_FOR_FILL -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    controls()
                    BatteryGraph(
                        records = records,
                        period = period,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    HighPrecisionSwitch(viewModel)
                }

                // 高さが足りない（小さい画面・大きな文字）: 全体をスクロールし、グラフの高さを確保する
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    controls()
                    BatteryGraph(
                        records = records,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SCROLLING_GRAPH_HEIGHT)
                    )
                    HighPrecisionSwitch(viewModel)
                }
            }
        }
    }
}

// 縦画面でグラフに残りの高さを割り当てる最低限の画面の高さ。これより低いとスクロールにする
private val MIN_HEIGHT_FOR_FILL = 520.dp
private val SCROLLING_GRAPH_HEIGHT = 280.dp

/** 現在の残量・充電状態・温度・電圧と、減少ペースの目安 */
@Composable
private fun StatusCard(status: BatterySnapshot?, records: List<BatteryRecord>) {
    // 画面を開いた直後で最新状態が届いていなければ、最後の記録を表示する
    val level = status?.level ?: records.lastOrNull()?.level
    val isCharging = status?.isCharging ?: (records.lastOrNull()?.isCharging == true)
    val chargeStatus = status?.chargeStatus
        ?: if (isCharging) ChargeStatus.CHARGING else ChargeStatus.DISCHARGING
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
                        when (chargeStatus) {
                            ChargeStatus.CHARGING -> R.string.status_charging
                            ChargeStatus.PLUGGED_NOT_CHARGING -> R.string.status_plugged_not_charging
                            ChargeStatus.FULL -> R.string.status_full
                            ChargeStatus.DISCHARGING -> R.string.status_discharging
                        }
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
                records.lastOrNull()?.let { last ->
                    // 記録が止まっていることに気づけるよう、最後に記録した時刻を出す
                    Text(
                        text = stringResource(R.string.status_last_record, formatRecordTime(last.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
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

/** 今日なら時刻だけ、それ以外は日付と時刻（12/24時間表示などは端末の設定に従う） */
@Composable
private fun formatRecordTime(millis: Long): String {
    val context = LocalContext.current
    var flags = DateUtils.FORMAT_SHOW_TIME
    if (!DateUtils.isToday(millis)) flags = flags or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE
    return DateUtils.formatDateTime(context, millis, flags)
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
