package com.gukos.battery_graph.ui.graph

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.gukos.battery_graph.domain.downsample
import com.gukos.battery_graph.ui.theme.chargingColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
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

// 描く点を「区間ごとの最小・最大」に間引くときの区間の数（点は最大で約2倍）
private const val DOWNSAMPLE_BUCKETS = 600

/**
 * 残量・温度・電圧の推移グラフ。充電中の区間は色を変えて描く。
 *
 * @param records 古い順の記録
 */
@Composable
fun BatteryGraph(
    records: List<BatteryRecord>,
    period: GraphPeriod,
    metric: GraphMetric,
    modifier: Modifier = Modifier
) {
    // 値のない記録（DB version 1 の温度・電圧など）は描かない。
    // 7日・30日表示では記録が数千件になるため、画面の幅に見合う数に間引く
    val points = remember(records, metric) {
        downsample(records.filter { metric.valueOf(it) != null }, DOWNSAMPLE_BUCKETS) { metric.valueOf(it)!! }
    }
    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(
                    if (records.size < 2 || metric == GraphMetric.LEVEL) R.string.graph_empty
                    else R.string.graph_empty_metric
                ),
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
    // グラフは画像として扱われ読み上げられないため、要約を TalkBack 用の説明にする
    val values = points.map { metric.valueOf(it)!! }
    val periodText = stringResource(period.descriptionRes)
    val summary = when (metric) {
        GraphMetric.LEVEL -> stringResource(
            R.string.graph_description, periodText,
            values.last().toInt(), values.max().toInt(), values.min().toInt()
        )
        GraphMetric.TEMPERATURE -> stringResource(
            R.string.graph_description_temperature, periodText, values.last(), values.max(), values.min()
        )
        GraphMetric.VOLTAGE -> stringResource(
            R.string.graph_description_voltage, periodText, values.last(), values.max(), values.min()
        )
    }

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
                // 右端の目盛りのラベルが切れないよう余白を取る。
                // setAvoidFirstLastClipping は最初のラベルを常に右へずらし、目盛り線と時刻がずれるため使わない
                extraRightOffset = 16f

                val formatter = TimeAxisFormatter().apply { updateFormats(context) }
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    setDrawAxisLine(false)
                    valueFormatter = formatter
                }
                setXAxisRenderer(
                    TimeXAxisRenderer(this@chart, formatter)
                )
                axisLeft.setDrawAxisLine(false)
            }
        },
        update = { chart ->
            val windowEnd = System.currentTimeMillis()
            val windowStart = windowEnd - period.durationMillis
            val baseTime = windowStart / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE

            val formatter = chart.xAxis.valueFormatter as TimeAxisFormatter
            formatter.baseTime = baseTime
            formatter.updateFormats(chart.context)

            val entries = points.mapIndexed { i, record ->
                Entry(formatter.toX(record.timestamp), values[i].toFloat())
            }
            val dataSet = LineDataSet(entries, "Battery").apply {
                // 各点の色はその点から次の点までの線の色になる
                colors = points.map { if (it.isCharging == true) chargeColor else lineColor }
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                // 塗りつぶしは 0% からの量が意味を持つ残量だけ
                setDrawFilled(metric == GraphMetric.LEVEL)
                fillColor = lineColor
                fillAlpha = 40
            }
            setUpValueAxis(chart, metric, values)

            chart.xAxis.apply {
                this.textColor = textColor
                this.gridColor = gridColor
                axisMinimum = 0f
                axisMaximum = formatter.toX(windowEnd)
            }
            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor

            chart.data = LineData(dataSet)
            chart.contentDescription = summary
            // 期間や項目を切り替えたらズームを解除する
            if (chart.tag != period to metric) {
                chart.fitScreen()
                chart.tag = period to metric
            }
            // 軸の範囲はデータ設定後に決まるので、表示幅の制限はその後に設定する
            chart.setVisibleXRangeMinimum(MIN_VISIBLE_MINUTES)
            chart.invalidate()
        }
    )
}

/** 縦軸の範囲と書式。残量は 0〜100% に固定し、温度・電圧は値の範囲に余白を足して決める */
private fun setUpValueAxis(chart: LineChart, metric: GraphMetric, values: List<Double>) {
    chart.axisLeft.apply {
        when (metric) {
            GraphMetric.LEVEL -> {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 1f
                setLabelCount(6, true)
            }
            GraphMetric.TEMPERATURE, GraphMetric.VOLTAGE -> {
                // 値の幅が小さくても目盛りが細かくなりすぎないよう、最低限の幅と余白を取る
                val minSpan = if (metric == GraphMetric.TEMPERATURE) 4.0 else 0.2
                val center = (values.max() + values.min()) / 2
                val half = maxOf(values.max() - values.min(), minSpan) / 2 * 1.2
                axisMinimum = (center - half).toFloat()
                axisMaximum = (center + half).toFloat()
                granularity = if (metric == GraphMetric.TEMPERATURE) 1f else 0.05f
                setLabelCount(6, false)
            }
        }
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = when (metric) {
                GraphMetric.LEVEL -> "${value.toInt()}%"
                GraphMetric.TEMPERATURE -> "%.0f℃".format(value)
                GraphMetric.VOLTAGE -> "%.2fV".format(value)
            }
        }
    }
}

/**
 * X軸の値（baseTime からの経過分）と時刻の変換、およびラベルの書式。
 * 日付と時刻の書式は端末の言語と12/24時間表示の設定に合わせる
 */
private class TimeAxisFormatter : ValueFormatter() {

    var baseTime: Long = 0L

    // 現在の目盛りの間隔。1日以上なら日付だけを表示する
    var stepMinutes: Long = 60L

    private var locale: Locale? = null
    private var is24Hour = true
    private lateinit var dayFormat: SimpleDateFormat
    private lateinit var timeFormat: SimpleDateFormat

    /** 言語や12/24時間表示の設定が変わっていれば書式を作り直す */
    fun updateFormats(context: Context) {
        val newLocale = Locale.getDefault()
        val new24Hour = DateFormat.is24HourFormat(context)
        if (newLocale == locale && new24Hour == is24Hour) return
        locale = newLocale
        is24Hour = new24Hour
        dayFormat = SimpleDateFormat(DateFormat.getBestDateTimePattern(newLocale, "Md"), newLocale)
        timeFormat = SimpleDateFormat(
            DateFormat.getBestDateTimePattern(newLocale, if (new24Hour) "Hm" else "hm"),
            newLocale
        )
    }

    fun toX(millis: Long): Float = (millis - baseTime).toFloat() / MILLIS_PER_MINUTE

    fun toMillis(x: Float): Long = baseTime + (x.toDouble() * MILLIS_PER_MINUTE).roundToLong()

    override fun getFormattedValue(value: Float): String {
        val millis = toMillis(value)
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val isMidnight = calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0
        // 0:00 の目盛りは日付にして、どの日かが分かるようにする
        val format = if (stepMinutes >= TimeUnit.DAYS.toMinutes(1) || isMidnight) dayFormat else timeFormat
        // 画面を開いたままタイムゾーンが変わっても、現在のタイムゾーンで表示する
        format.timeZone = TimeZone.getDefault()
        return format.format(millis)
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
}
