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

                // ピンチズーム
                setPinchZoom(true)
                //X方向のみズーム可能
                isScaleXEnabled = true
                isScaleYEnabled = false
                // X方向スクロール（横スクロール）
                isDragEnabled = true
                // タッチ有効化
                setTouchEnabled(true)
                // 慣性スクロール（スムーズ）
                setDragDecelerationFrictionCoef(0.9f)
                // スクロール制限（任意）
                setVisibleXRangeMinimum(10f) // 最小表示範囲
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM

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

                    granularity = 15 * 60 * 1000f // 15分単位（ズレ防止）
                    setLabelCount(6, true)
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