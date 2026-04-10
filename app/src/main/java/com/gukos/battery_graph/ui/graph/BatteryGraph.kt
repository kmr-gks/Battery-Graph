package com.gukos.battery_graph.ui.graph

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.gukos.battery_graph.data.entity.BatteryRecord


@Composable
fun BatteryGraph(recordsList: List<BatteryRecord>) {

    val records = remember(recordsList) {
        recordsList
            .sortedBy { it.timestamp }
            .map { it.timestamp to it.level }
    }

    if (records.size < 2) return

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f) // 最大5倍ズーム
        offsetX += panChange.x
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
    ) {

        val width = size.width
        val height = size.height

        val paddingLeft = 80f
        val paddingBottom = 60f

        val graphWidth = width - paddingLeft
        val graphHeight = height - paddingBottom

        val minX = records.first().first.toDouble()
        val maxX = records.last().first.toDouble()
        val rangeX = (maxX - minX).coerceAtLeast(1.0)

        val maxY = 100f
        val minY = 0f

        fun mapX(t: Long): Float {
            val base = (((t.toDouble() - minX) / rangeX) * width).toFloat()
            return base * scale + offsetX
        }

        fun mapY(level: Int): Float {
            return height - ((level - minY) / (maxY - minY) * height)
        }

        // =========================
        // 軸（X・Y）
        // =========================

        // Y軸
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, 0f),
            end = Offset(paddingLeft, graphHeight),
            strokeWidth = 2f
        )

        // X軸
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, graphHeight),
            end = Offset(width, graphHeight),
            strokeWidth = 2f
        )

        // =========================
        // Y軸目盛り（0〜100）
        // =========================

        val steps = 5
        for (i in 0..steps) {
            val yValue = i * 20
            val y = graphHeight - (yValue / 100f) * graphHeight

            drawLine(
                color = Color.LightGray,
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        for (i in 0 until records.size - 1) {
            val (t1, l1) = records[i]
            val (t2, l2) = records[i + 1]

            drawLine(
                color = Color.Red,
                start = Offset(mapX(t1), mapY(l1)),
                end = Offset(mapX(t2), mapY(l2)),
                strokeWidth = 4f
            )
        }
    }
}
