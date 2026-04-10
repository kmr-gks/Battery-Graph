package com.gukos.battery_graph.ui.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gukos.battery_graph.data.entity.BatteryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatteryGraph(recordsList: List<BatteryRecord>) {

    AndroidView(
        modifier = Modifier.fillMaxSize(), // ← これが重要
        factory = { context ->
            LineChart(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
                description.isEnabled = false
                legend.isEnabled = false

                setTouchEnabled(true)
                setPinchZoom(true)
                setScaleEnabled(true)
            }
        },
        update = { chart ->

            val entries = recordsList
                .sortedBy { it.timestamp }
                .map {
                    Entry(it.timestamp.toFloat(), it.level.toFloat())
                }

            val dataSet = LineDataSet(entries, "Battery").apply {
                color = Color.GREEN
                setDrawCircles(false)
                lineWidth = 2f
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}