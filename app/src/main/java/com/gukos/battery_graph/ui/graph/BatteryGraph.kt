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
import androidx.compose.material3.DrawerValue
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
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                isScaleXEnabled = true
                isScaleYEnabled = false
                isDragEnabled = true
                setPinchZoom(true)
                setTouchEnabled(true)
                setDragDecelerationFrictionCoef(0.9f)
                setVisibleXRangeMinimum(10f) // 最小表示範囲

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM

                    // ★これで「グリッドと一緒に動く」
                    setDrawGridLines(true)
                    // ★これを追加
                    valueFormatter = object : ValueFormatter() {

                        private val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)
                        private val dateFormat = SimpleDateFormat("MM/dd", Locale.JAPAN)
                        private var lastDate: String? = null

                        override fun getFormattedValue(value: Float): String {
                            val date = Date(value.toLong())
                            val currentDate = dateFormat.format(date)
                            val time = timeFormat.format(date)

                            return if (currentDate != lastDate) {
                                lastDate = currentDate
                                "$time\n$currentDate"
                            } else {
                                time
                            }
                        }
                    }
                }
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
                setDrawValues(false)
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}