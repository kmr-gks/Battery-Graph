package com.gukos.battery_graph.ui.graph

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.gukos.battery_graph.data.entity.BatteryRecord


@Composable
fun BatteryGraph(recordsList: List<BatteryRecord>) {

    val records = recordsList.map { it.timestamp to it.level }
    Log.e("BatteryGraph", "records: ${records.size}")
    Log.e("BatteryGraph", "records: $records")

    if (records.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {

        val width = size.width
        val height = size.height

        val minX = records.first().first.toFloat()
        val maxX = records.last().first.toFloat()
        val rangeX = maxX - minX

        val maxY = 100f
        val minY = 0f

        fun mapX(t: Long): Float {
            return ((t - minX) / rangeX) * width
        }

        fun mapY(level: Int): Float {
            return height - ((level - minY) / (maxY - minY) * height)
        }

        for (i in 0 until records.size - 1) {
            val p1 = records[i]
            val p2 = records[i + 1]

            drawLine(
                color = Color.Green,
                start = Offset(mapX(p1.first), mapY(p1.second)),
                end = Offset(mapX(p2.first), mapY(p2.second)),
                strokeWidth = 6f
            )
        }
    }
}