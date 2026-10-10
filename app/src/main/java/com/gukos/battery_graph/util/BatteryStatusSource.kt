package com.gukos.battery_graph.util

/** 現在のバッテリー状態の取得元。テストでは偽物に差し替える */
fun interface BatteryStatusSource {
    fun getSnapshot(): BatterySnapshot?
}
