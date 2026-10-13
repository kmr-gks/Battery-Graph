package com.gukos.battery_graph.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ダイナミックカラーのない Android 11 以下で使う配色。アイコンの青から緑への色に合わせる
val Blue40 = Color(0xFF1E5FC4)
val Teal40 = Color(0xFF00696E)
val Green40 = Color(0xFF2E7D32)

val Blue80 = Color(0xFFADC6FF)
val Teal80 = Color(0xFF6FD5DB)
val Green80 = Color(0xFF81C784)

// 充電中を表す色（グラフの線・状態表示）。ダイナミックカラーでも意味が変わらないよう固定色にする
private val ChargingLight = Color(0xFF2E7D32)
private val ChargingDark = Color(0xFF81C784)

@Composable
fun chargingColor(): Color = if (isSystemInDarkTheme()) ChargingDark else ChargingLight

// グラフの温度（右の縦軸）と電圧（点線）の色。残量の青・充電中の緑と見分けられる色にする
private val TemperatureLight = Color(0xFFD84315)
private val TemperatureDark = Color(0xFFFFAB91)
private val VoltageLight = Color(0xFF7B1FA2)
private val VoltageDark = Color(0xFFCE93D8)

@Composable
fun temperatureColor(): Color = if (isSystemInDarkTheme()) TemperatureDark else TemperatureLight

@Composable
fun voltageColor(): Color = if (isSystemInDarkTheme()) VoltageDark else VoltageLight
