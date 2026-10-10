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
        MutableStateFlow(prefs.getBoolean(KEY_HIGH_PRECISION, false))
    val highPrecisionEnabled: StateFlow<Boolean> = _highPrecisionEnabled.asStateFlow()

    fun setHighPrecisionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HIGH_PRECISION, enabled) }
        _highPrecisionEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_HIGH_PRECISION = "high_precision_enabled"
    }
}
