package com.gukos.battery_graph.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.gukos.battery_graph.data.entity.BatteryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
    ) {

        val width = size.width
        val height = size.height

        // =========================
        // ★余白を追加
        // =========================
        val paddingLeft = 80f
        val paddingRight = 20f
        val paddingTop = 40f
        val paddingBottom = 60f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        val minX = records.first().first.toDouble()
        val maxX = records.last().first.toDouble()
        val rangeX = (maxX - minX).coerceAtLeast(1.0)

        val maxY = 100f
        val minY = 0f

        fun mapX(t: Long): Float {
            val base = (((t.toDouble() - minX) / rangeX) * graphWidth).toFloat()
            return base * scale + paddingLeft + offsetX
        }

        fun mapY(level: Int): Float {
            return paddingTop + graphHeight -
                    ((level - minY) / (maxY - minY) * graphHeight)
        }

        // =========================
        // Y軸目盛り＋数字
        // =========================

        val steps = 5
        for (i in 0..steps) {
            val value = i * 20
            val y = paddingTop + graphHeight - (value / 100f) * graphHeight

            val textLayout = textMeasurer.measure(
                text = "$value%",
                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(10f, y - textLayout.size.height / 2)
            )

            drawLine(
                color = Color.LightGray,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        // =========================
        // X軸メモリ（時間）
        // =========================

        val xSteps = (4 * scale).toInt().coerceIn(4, 40)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)
        val dateFormat = SimpleDateFormat("MM/dd", Locale.JAPAN)

        var lastDate: String? = null

        for (i in 0..xSteps) {

            val t = minX + (rangeX * i / xSteps)
            val x = mapX(t.toLong())

            val date = dateFormat.format(Date(t.toLong()))
            val time = timeFormat.format(Date(t.toLong()))

            // 目盛り線
            drawLine(
                color = Color.LightGray,
                start = Offset(x, paddingTop),
                end = Offset(x, paddingTop + graphHeight),
                strokeWidth = 1f
            )

            // 時間表示（必ず表示）
            val timeLayout = textMeasurer.measure(
                text = time,
                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
            )

            drawText(
                textLayoutResult = timeLayout,
                topLeft = Offset(
                    x - timeLayout.size.width / 2,
                    paddingTop + graphHeight + 5f
                )
            )

            // ★日付は変わったときだけ表示
            if (date != lastDate) {

                val dateLayout = textMeasurer.measure(
                    text = date,
                    style = TextStyle(fontSize = 10.sp, color = Color.DarkGray)
                )

                drawText(
                    textLayoutResult = dateLayout,
                    topLeft = Offset(
                        x - dateLayout.size.width / 2,
                        paddingTop + graphHeight + 40f
                    )
                )

                lastDate = date
            }
        }

        // Y軸
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // X軸
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, paddingTop + graphHeight),
            end = Offset(width - paddingRight, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // グラフ線
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
