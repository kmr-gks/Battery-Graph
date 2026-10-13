package com.gukos.battery_graph.ui.graph

import android.content.Context
import android.graphics.DashPathEffect
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
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
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
import com.gukos.battery_graph.ui.theme.temperatureColor
import com.gukos.battery_graph.ui.theme.voltageColor
import com.gukos.battery_graph.util.TemperatureUnit
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
 * 残量の推移グラフ。充電中の区間は色を変えて描く。
 * 設定に応じて、温度を右の縦軸で、電圧を目盛りなしの点線で重ねて描く。
 *
 * @param records 古い順の記録
 */
@Composable
fun BatteryGraph(
    records: List<BatteryRecord>,
    period: GraphPeriod,
    showTemperature: Boolean,
    showVoltage: Boolean,
    temperatureUnit: TemperatureUnit,
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

    // 7日・30日表示では記録が数千件になるため、項目ごとに画面の幅に見合う数に間引く。
    // 温度・電圧のない記録（DB version 1）はその線に描かない
    val levelPoints = remember(records) { downsample(records, DOWNSAMPLE_BUCKETS) }
    val temperaturePoints = remember(records, showTemperature) {
        if (!showTemperature) emptyList()
        else downsample(records.filter { it.temperature != null }, DOWNSAMPLE_BUCKETS) { it.temperature!!.toDouble() }
    }
    val voltagePoints = remember(records, showVoltage) {
        if (!showVoltage) emptyList()
        else downsample(records.filter { it.voltage != null }, DOWNSAMPLE_BUCKETS) { it.voltage!!.toDouble() }
    }

    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val chargeColor = chargingColor().toArgb()
    val temperatureColor = temperatureColor().toArgb()
    val voltageColor = voltageColor().toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    // 凡例には各項目の現在（最新）の値を出す。電圧は目盛りがないため、値はここでだけ分かる
    val legendLabels = buildList {
        add(stringResource(R.string.legend_level, levelPoints.last().level))
        temperaturePoints.lastOrNull()?.let {
            add(stringResource(R.string.legend_temperature, temperatureUnit.format(it.temperature!!)))
        }
        voltagePoints.lastOrNull()?.let { add(stringResource(R.string.legend_voltage, it.voltage!! / 1000.0)) }
    }

    // グラフは画像として扱われ読み上げられないため、要約を TalkBack 用の説明にする
    val summary = buildString {
        append(
            stringResource(
                R.string.graph_description,
                stringResource(period.descriptionRes),
                records.last().level,
                records.maxOf { it.level },
                records.minOf { it.level }
            )
        )
        if (temperaturePoints.isNotEmpty()) {
            val temperatures = temperaturePoints.map { it.temperature!! }
            append(' ').append(
                stringResource(
                    R.string.graph_description_temperature,
                    temperatureUnit.format(temperatures.last()),
                    temperatureUnit.format(temperatures.max()),
                    temperatureUnit.format(temperatures.min())
                )
            )
        }
        voltagePoints.lastOrNull()?.let {
            append(' ').append(stringResource(R.string.graph_description_voltage, it.voltage!! / 1000.0))
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply chart@{
                description.isEnabled = false
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
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 100f
                    granularity = 1f
                    setLabelCount(6, true)
                    setDrawAxisLine(false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "${value.toInt()}%"
                    }
                }
                axisRight.apply {
                    setDrawAxisLine(false)
                    // 目盛り線は左軸（%）のものだけ描く
                    setDrawGridLines(false)
                    granularity = 1f
                    setLabelCount(6, false)
                }
                legend.apply {
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    form = Legend.LegendForm.LINE
                    isWordWrapEnabled = true
                }
            }
        },
        update = { chart ->
            val windowEnd = System.currentTimeMillis()
            val windowStart = windowEnd - period.durationMillis
            val baseTime = windowStart / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE

            val formatter = chart.xAxis.valueFormatter as TimeAxisFormatter
            formatter.baseTime = baseTime
            formatter.updateFormats(chart.context)

            val dataSets = mutableListOf<LineDataSet>()
            dataSets += LineDataSet(
                levelPoints.map { Entry(formatter.toX(it.timestamp), it.level.toFloat()) },
                legendLabels[0]
            ).apply {
                axisDependency = YAxis.AxisDependency.LEFT
                // 各点の色はその点から次の点までの線の色になる
                colors = levelPoints.map { if (it.isCharging == true) chargeColor else lineColor }
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 40
            }

            // 温度: 右の縦軸（選んだ単位）
            val hasTemperature = temperaturePoints.size >= 2
            if (hasTemperature) {
                val values = temperaturePoints.map { temperatureUnit.fromTenthsCelsius(it.temperature!!) }
                dataSets += LineDataSet(
                    temperaturePoints.mapIndexed { i, r -> Entry(formatter.toX(r.timestamp), values[i].toFloat()) },
                    legendLabels[1]
                ).apply {
                    axisDependency = YAxis.AxisDependency.RIGHT
                    color = temperatureColor
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 1.5f
                }
                chart.axisRight.apply {
                    // 値の幅が小さくても目盛りが細かくなりすぎないよう、最低限の幅と余白を取る
                    val minSpan = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) 8.0 else 4.0
                    val center = (values.max() + values.min()) / 2
                    val half = maxOf(values.max() - values.min(), minSpan) / 2 * 1.2
                    axisMinimum = (center - half).toFloat()
                    axisMaximum = (center + half).toFloat()
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = "%.0f%s".format(value, temperatureUnit.symbol)
                    }
                }
            }
            chart.axisRight.isEnabled = hasTemperature
            // 右の縦軸がないときは、右端の時刻のラベルが切れないよう余白を取る
            chart.extraRightOffset = if (hasTemperature) 0f else 16f

            // 電圧: 目盛りなしの点線。変化の形が見えるよう、左軸の 10〜90% の範囲に引き伸ばして描く
            if (voltagePoints.size >= 2) {
                val volts = voltagePoints.map { it.voltage!!.toDouble() }
                val low = volts.min()
                val span = maxOf(volts.max() - low, 200.0)
                dataSets += LineDataSet(
                    voltagePoints.mapIndexed { i, r ->
                        Entry(formatter.toX(r.timestamp), (10 + 80 * (volts[i] - low) / span).toFloat())
                    },
                    legendLabels.last()
                ).apply {
                    axisDependency = YAxis.AxisDependency.LEFT
                    color = voltageColor
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 1.5f
                    enableDashedLine(12f, 8f, 0f)
                }
            }

            chart.xAxis.apply {
                this.textColor = textColor
                this.gridColor = gridColor
                axisMinimum = 0f
                axisMaximum = formatter.toX(windowEnd)
            }
            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor
            chart.axisRight.textColor = temperatureColor
            // 凡例は自前で作る（残量の線は区間ごとに色が違い、標準の凡例では色の数だけ項目ができるため）。
            // 温度・電圧を表示しないときは凡例を出さない
            chart.legend.isEnabled = dataSets.size > 1
            chart.legend.textColor = textColor
            chart.legend.setCustom(
                dataSets.mapIndexed { i, set ->
                    LegendEntry().apply {
                        label = set.label
                        form = Legend.LegendForm.LINE
                        formLineWidth = 2.5f
                        formSize = 16f
                        formColor = if (i == 0) lineColor else set.color
                        if (set.isDashedLineEnabled) formLineDashEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                    }
                }
            )

            chart.data = LineData(dataSets.toList())
            chart.contentDescription = summary
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
