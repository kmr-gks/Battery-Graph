package com.gukos.battery_graph.util

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** 温度の単位 */
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    /** 0.1℃単位の値を、この単位の値にする */
    fun fromTenthsCelsius(tenths: Int): Double {
        val celsius = tenths / 10.0
        return if (this == FAHRENHEIT) celsius * 9 / 5 + 32 else celsius
    }

    val symbol: String get() = if (this == FAHRENHEIT) "°F" else "℃"

    /** 単位付きの文字列（例: 25.3℃、77.5°F） */
    fun format(tenths: Int, decimals: Int = 1): String =
        "%.${decimals}f%s".format(fromTenthsCelsius(tenths), symbol)

    companion object {
        // 日常的に華氏を使う国（米国・リベリア・ミャンマーなど）
        private val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW", "FM", "MH")

        /** 地域に合わせた初期値 */
        fun defaultFor(country: String): TemperatureUnit =
            if (country.uppercase() in FAHRENHEIT_COUNTRIES) FAHRENHEIT else CELSIUS
    }
}

/** グラフの表示設定（温度・電圧を表示するか、温度の単位） */
class GraphSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _showTemperature = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TEMPERATURE, true))
    val showTemperature: StateFlow<Boolean> = _showTemperature.asStateFlow()

    private val _showVoltage = MutableStateFlow(prefs.getBoolean(KEY_SHOW_VOLTAGE, false))
    val showVoltage: StateFlow<Boolean> = _showVoltage.asStateFlow()

    private val _temperatureUnit = MutableStateFlow(
        prefs.getString(KEY_TEMPERATURE_UNIT, null)
            ?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() }
            ?: TemperatureUnit.defaultFor(Locale.getDefault().country)
    )
    val temperatureUnit: StateFlow<TemperatureUnit> = _temperatureUnit.asStateFlow()

    fun setShowTemperature(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_TEMPERATURE, show) }
        _showTemperature.value = show
    }

    fun setShowVoltage(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_VOLTAGE, show) }
        _showVoltage.value = show
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        prefs.edit { putString(KEY_TEMPERATURE_UNIT, unit.name) }
        _temperatureUnit.value = unit
    }

    private companion object {
        // 高精度記録の設定と同じファイルに保存する
        const val PREFS_NAME = "settings"
        const val KEY_SHOW_TEMPERATURE = "graph_show_temperature"
        const val KEY_SHOW_VOLTAGE = "graph_show_voltage"
        const val KEY_TEMPERATURE_UNIT = "temperature_unit"
    }
}
