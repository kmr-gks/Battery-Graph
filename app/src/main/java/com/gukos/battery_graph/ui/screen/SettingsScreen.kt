package com.gukos.battery_graph.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gukos.battery_graph.R
import com.gukos.battery_graph.service.BatteryMonitorService
import com.gukos.battery_graph.ui.viewmodel.BatteryViewModel
import com.gukos.battery_graph.util.TemperatureUnit

/** 設定画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BatteryViewModel, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HighPrecisionSwitch(viewModel)

            Text(
                stringResource(R.string.settings_graph_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            val showTemperature by viewModel.showTemperature.collectAsStateWithLifecycle()
            val showVoltage by viewModel.showVoltage.collectAsStateWithLifecycle()
            val temperatureUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()
            SettingSwitch(
                label = stringResource(R.string.show_temperature_label),
                description = stringResource(R.string.show_temperature_description),
                checked = showTemperature,
                onCheckedChange = viewModel::setShowTemperature
            )
            SettingSwitch(
                label = stringResource(R.string.show_voltage_label),
                description = stringResource(R.string.show_voltage_description),
                checked = showVoltage,
                onCheckedChange = viewModel::setShowVoltage
            )
            Text(
                stringResource(R.string.temperature_unit_label),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            val units = TemperatureUnit.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                units.forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = unit == temperatureUnit,
                        onClick = { viewModel.setTemperatureUnit(unit) },
                        shape = SegmentedButtonDefaults.itemShape(index, units.size),
                        label = { Text(unit.symbol) }
                    )
                }
            }
        }
    }
}

/** ラベルと説明つきのスイッチ。行全体を押して切り替えられ、TalkBack ではまとめて読み上げられる */
@Composable
private fun SettingSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * 高精度記録は初期設定で ON のため、Android 13 以降では初回だけ通知の許可を求める。
 * 許可の前に出した通知は表示されないため、結果が出てからサービスを起動し直して通知を出す
 */
@Composable
fun RequestNotificationPermissionOnce(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { BatteryMonitorService.startIfEnabled(context) }

    LaunchedEffect(Unit) {
        if (viewModel.highPrecisionEnabled.value &&
            needsNotificationPermission(context) &&
            !viewModel.notificationPermissionRequested
        ) {
            viewModel.markNotificationPermissionRequested()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

@Composable
private fun HighPrecisionSwitch(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    val enabled by viewModel.highPrecisionEnabled.collectAsStateWithLifecycle()

    // 許可の前に出した通知は表示されないため、許可ダイアログの結果が出てからサービスを起動する。
    // 通知が許可されなくても記録は動く（通知が表示されないだけ）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { BatteryMonitorService.startIfEnabled(context) }

    fun setEnabled(value: Boolean) {
        viewModel.setHighPrecisionEnabled(value)
        if (value) {
            if (needsNotificationPermission(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                BatteryMonitorService.startIfEnabled(context)
            }
        } else {
            BatteryMonitorService.stop(context)
        }
    }

    SettingSwitch(
        label = stringResource(R.string.high_precision_label),
        description = stringResource(R.string.high_precision_description),
        checked = enabled,
        onCheckedChange = ::setEnabled
    )
}
