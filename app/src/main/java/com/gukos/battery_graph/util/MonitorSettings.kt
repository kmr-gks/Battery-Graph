package com.gukos.battery_graph.util

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 高精度記録（常駐サービス）の ON/OFF 設定 */
class MonitorSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _highPrecisionEnabled =
        // 初期設定は ON（残量が変わるたびに記録する）
        MutableStateFlow(prefs.getBoolean(KEY_HIGH_PRECISION, true))
    val highPrecisionEnabled: StateFlow<Boolean> = _highPrecisionEnabled.asStateFlow()

    fun setHighPrecisionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HIGH_PRECISION, enabled) }
        _highPrecisionEnabled.value = enabled
    }

    /** 通知の許可を一度求めたか（初回起動時に1回だけ求めるため） */
    var notificationPermissionRequested: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, value) }

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_HIGH_PRECISION = "high_precision_enabled"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
