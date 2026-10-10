package com.gukos.battery_graph.ui.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.gukos.battery_graph.R
import com.gukos.battery_graph.data.entity.BatteryRecord
import com.gukos.battery_graph.ui.theme.chargingColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

// X軸の値は「baseTime からの経過分」。epoch ミリ秒を Float にすると約2分単位に丸められるため
private const val MILLIS_PER_MINUTE = 60_000L

// ズームの上限（これ以上拡大しない表示幅、分）
private const val MIN_VISIBLE_MINUTES = 30f

// 目盛りの間隔の候補（分）。表示幅に対して目盛りが MAX_TICKS 個以下になる最小のものを使う
private val TICK_STEPS_MINUTES = listOf(
    15L, 30L, 60L, 2 * 60L, 3 * 60L, 6 * 60L, 12 * 60L,
    24 * 60L, 2 * 24 * 60L, 7 * 24 * 60L, 14 * 24 * 60L
)
private const val MAX_TICKS = 5

/**
 * 残量の推移グラフ。充電中の区間は色を変えて描く。
 *
 * @param records 古い順の記録
 */
@Composable
fun BatteryGraph(
    records: List<BatteryRecord>,
    period: GraphPeriod,
    modifier: Modifier = Modifier
) {
    if (records.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.graph_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val chargeColor = chargingColor().toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply chart@{
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                isScaleXEnabled = true
                isScaleYEnabled = false
                isDragEnabled = true
                setPinchZoom(false)
                setTouchEnabled(true)
                isHighlightPerTapEnabled = false
                isHighlightPerDragEnabled = false
                isDoubleTapToZoomEnabled = false
                setDragDecelerationFrictionCoef(0.9f)
                setClipValuesToContent(true)
                setNoDataText("")

                val formatter = TimeAxisFormatter()
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    setDrawAxisLine(false)
                    setAvoidFirstLastClipping(true)
                    valueFormatter = formatter
                }
                setXAxisRenderer(
                    TimeXAxisRenderer(this@chart, formatter)
                )
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 100f
                    setLabelCount(6, true)
                    setDrawAxisLine(false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "${value.toInt()}%"
                    }
                }
            }
        },
        update = { chart ->
            val windowEnd = System.currentTimeMillis()
            val windowStart = windowEnd - period.durationMillis
            val baseTime = windowStart / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE

            val formatter = chart.xAxis.valueFormatter as TimeAxisFormatter
            formatter.baseTime = baseTime

            val entries = records.map {
                Entry(formatter.toX(it.timestamp), it.level.toFloat())
            }
            val dataSet = LineDataSet(entries, "Battery").apply {
                // 各点の色はその点から次の点までの線の色になる
                colors = records.map { if (it.isCharging == true) chargeColor else lineColor }
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 40
            }

            chart.xAxis.apply {
                this.textColor = textColor
                this.gridColor = gridColor
                axisMinimum = 0f
                axisMaximum = formatter.toX(windowEnd)
            }
            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor

            chart.data = LineData(dataSet)
            // 期間を切り替えたらズームを解除する
            if (chart.tag != period) {
                chart.fitScreen()
                chart.tag = period
            }
            // 軸の範囲はデータ設定後に決まるので、表示幅の制限はその後に設定する
            chart.setVisibleXRangeMinimum(MIN_VISIBLE_MINUTES)
            chart.invalidate()
        }
    )
}

/** X軸の値（baseTime からの経過分）と時刻の変換、およびラベルの書式 */
private class TimeAxisFormatter : ValueFormatter() {

    var baseTime: Long = 0L

    // 現在の目盛りの間隔。1日以上なら日付だけを表示する
    var stepMinutes: Long = 60L

    private val dayFormat = SimpleDateFormat("M/d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("H:mm", Locale.getDefault())

    fun toX(millis: Long): Float = (millis - baseTime).toFloat() / MILLIS_PER_MINUTE

    fun toMillis(x: Float): Long = baseTime + (x.toDouble() * MILLIS_PER_MINUTE).roundToLong()

    override fun getFormattedValue(value: Float): String {
        val millis = toMillis(value)
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val isMidnight = calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0
        // 0:00 の目盛りは日付にして、どの日かが分かるようにする
        return if (stepMinutes >= TimeUnit.DAYS.toMinutes(1) || isMidnight) {
            dayFormat.format(millis)
        } else {
            timeFormat.format(millis)
        }
    }
}

/**
 * 目盛りを時刻の区切り（0:00, 6:00, 日付など）に合わせて配置する。
 * MPAndroidChart 標準の目盛りは数値として切りの良い位置になり、時刻とずれるため置き換える。
 */
private class TimeXAxisRenderer(
    chart: LineChart,
    private val formatter: TimeAxisFormatter
) : XAxisRenderer(
    chart.viewPortHandler,
    chart.xAxis,
    chart.getTransformer(YAxis.AxisDependency.LEFT)
) {

    override fun computeAxisValues(min: Float, max: Float) {
        val start = formatter.toMillis(min)
        val end = formatter.toMillis(max)
        val rangeMinutes = (end - start) / MILLIS_PER_MINUTE

        val step = TICK_STEPS_MINUTES.firstOrNull { rangeMinutes / it <= MAX_TICKS }
            ?: TICK_STEPS_MINUTES.last()
        formatter.stepMinutes = step

        val ticks = timeTicks(start, end, step)
        mAxis.mEntryCount = ticks.size
        mAxis.mEntries = FloatArray(ticks.size) { formatter.toX(ticks[it]) }
        mAxis.mCenteredEntries = mAxis.mEntries
        mAxis.mDecimals = 0
        computeSize()
    }

    /** start〜end の間で、その日の 0:00 から step 分ごとの時刻（step が日単位なら日ごと） */
    private fun timeTicks(start: Long, end: Long, stepMinutes: Long): List<Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val minutesPerDay = TimeUnit.DAYS.toMinutes(1)
        val ticks = mutableListOf<Long>()
        while (calendar.timeInMillis <= end) {
            if (calendar.timeInMillis >= start) ticks += calendar.timeInMillis
            if (stepMinutes >= minutesPerDay) {
                calendar.add(Calendar.DAY_OF_YEAR, (stepMinutes / minutesPerDay).toInt())
            } else {
                calendar.add(Calendar.MINUTE, stepMinutes.toInt())
            }
        }
        return ticks
    }
}
