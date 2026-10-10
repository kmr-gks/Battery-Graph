package com.gukos.battery_graph.ui.graph

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.gukos.battery_graph.data.entity.BatteryRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// X軸の値は「baseTime からの経過分」。epoch ミリ秒を Float にすると約2分単位に丸められるため
private const val MILLIS_PER_MINUTE = 60_000f
private const val MINUTES_PER_DAY = 24 * 60f

// ズームの上限（これ以上拡大しない表示幅）
private const val MIN_VISIBLE_MINUTES = 30f

@Composable
fun BatteryGraph(recordsList: List<BatteryRecord>) {

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply chart@{
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                isScaleXEnabled = true
                isScaleYEnabled = false
                isDragEnabled = true
                setPinchZoom(true)
                setTouchEnabled(true)
                setDragDecelerationFrictionCoef(0.9f)
                setNoDataText("")

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    granularity = 1f
                    valueFormatter = TimeAxisFormatter(this@chart)
                }
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 100f
                }
            }
        },
        update = { chart ->
            val sorted = recordsList.sortedBy { it.timestamp }
            val baseTime = sorted.firstOrNull()?.timestamp ?: 0L
            (chart.xAxis.valueFormatter as TimeAxisFormatter).baseTime = baseTime

            val entries = sorted.map {
                Entry((it.timestamp - baseTime) / MILLIS_PER_MINUTE, it.level.toFloat())
            }
            val dataSet = LineDataSet(entries, "Battery").apply {
                color = Color.RED
                setDrawCircles(false)
                lineWidth = 2f
                setDrawValues(false)
            }

            chart.xAxis.textColor = textColor
            chart.xAxis.gridColor = gridColor
            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor

            chart.data = LineData(dataSet)
            // 軸の範囲はデータ設定後に決まるので、表示幅の制限はその後に設定する
            chart.setVisibleXRangeMinimum(MIN_VISIBLE_MINUTES)
            chart.invalidate()
        }
    )
}

/**
 * 表示中の範囲に応じてラベルの書式を切り替える。
 * - 1日以上表示: "M/d"
 * - 日付をまたぐ: "M/d HH:mm"
 * - 同じ日の中: "HH:mm"
 */
private class TimeAxisFormatter(private val chart: LineChart) : ValueFormatter() {

    var baseTime: Long = 0L

    private val dayFormat = SimpleDateFormat("M/d", Locale.getDefault())
    private val dayTimeFormat = SimpleDateFormat("M/d HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        val date = toDate(value)
        val format = when {
            chart.visibleXRange >= MINUTES_PER_DAY -> dayFormat
            !isSameDay(toDate(chart.lowestVisibleX), toDate(chart.highestVisibleX)) -> dayTimeFormat
            else -> timeFormat
        }
        return format.format(date)
    }

    private fun toDate(minutes: Float) = Date(baseTime + (minutes * MILLIS_PER_MINUTE).toLong())

    private fun isSameDay(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
