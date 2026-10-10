package com.gukos.battery_graph.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 充電中を表す色（グラフの線・状態表示）。ダイナミックカラーでも意味が変わらないよう固定色にする
private val ChargingLight = Color(0xFF2E7D32)
private val ChargingDark = Color(0xFF81C784)

@Composable
fun chargingColor(): Color = if (isSystemInDarkTheme()) ChargingDark else ChargingLight
