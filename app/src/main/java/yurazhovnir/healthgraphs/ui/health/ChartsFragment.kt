package yurazhovnir.healthgraphs.ui.health

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import io.realm.kotlin.ext.copyFromRealm
import yurazhovnir.healthgraphs.HealthDataPeriod
import yurazhovnir.healthgraphs.R
import yurazhovnir.healthgraphs.base.BaseBindingFragment
import yurazhovnir.healthgraphs.base.RoundedBarChartTime
import yurazhovnir.healthgraphs.databinding.FragmentChartsBinding
import yurazhovnir.healthgraphs.dp
import yurazhovnir.healthgraphs.helper.RealmHelper
import yurazhovnir.healthgraphs.model.HealthRecord
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale


private const val BAR_CHART_STEPS_ID = 123456
private const val BAR_CHART_HYDRATION_ID = 123457
private const val BAR_CHART_SLEEP_ID = 123458
private const val BAR_CHART_HEART_RATE_ID = 123459
private const val BAR_CHART_EXERCISE_ID = 123460

class ChartsFragment : BaseBindingFragment<FragmentChartsBinding>(FragmentChartsBinding::inflate) {

    override val TAG: String
        get() = "ChartsFragment"

    private val healthRecords: MutableList<HealthRecord> = mutableListOf()
    var healthDataPeriod: HealthDataPeriod = HealthDataPeriod.Week
    private val weekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("MMM", Locale.getDefault())
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHealthData()
    }

    private fun loadHealthData() {
        val fetchedRecords = RealmHelper.realm?.query(HealthRecord::class)?.find()?.copyFromRealm() ?: emptyList()
        healthRecords.clear()

        when (healthDataPeriod) {
            HealthDataPeriod.Week -> {
                val lastDays = getLastDays(7)
                lastDays.forEach { day ->
                    val recordsForDay = fetchedRecords.filter { it.startsAt == day }.firstOrNull()
                    if (recordsForDay != null) {
                        healthRecords.add(recordsForDay)
                    }
                }
            }

            HealthDataPeriod.Month -> {
                val lastDays = getLastDays(30)
                lastDays.forEach { day ->
                    val recordsForDay = fetchedRecords.filter { it.startsAt == day }.firstOrNull()
                    if (recordsForDay != null) {
                        healthRecords.add(recordsForDay)
                    }
                }
            }

            HealthDataPeriod.Year -> {
                val last12Months = getLastMonths()
                last12Months.forEach { month ->
                    val recordsForMonth = fetchedRecords.filter { it.startsAt?.substring(0, 7) == month }
                    if (recordsForMonth.isNotEmpty()) {
                        healthRecords.addAll(recordsForMonth)
                    }
                }
            }
        }
        updateAllCharts()
    }

    private fun getLastDays(to: Int): List<String> {
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val lastDays = mutableListOf<String>()
        var currentDate = LocalDate.now()
        for (i in 0 until to) {
            lastDays.add(currentDate.format(dateFormat))
            currentDate = currentDate.minusDays(1)
        }
        return lastDays.reversed()
    }

    private fun getLastMonths(): List<String> {
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM")
        val lastMonths = mutableListOf<String>()
        var currentDate = LocalDate.now()
        for (i in 0 until 12) {
            lastMonths.add(currentDate.format(dateFormat))
            currentDate = currentDate.minusMonths(1)
        }
        return lastMonths.reversed()
    }

    private fun updateAllCharts() {
        val labels = getFormattedLabels()
        val stepsData = createDataMap(labels) { it.steps ?: 0 }
        val hydrationData = createDataMap(labels) { (it.hydration ?: 0.0).toInt() }
        val sleepData = createDataMap(labels) { it.sleep ?: 0 }
        val heartRateData = createDataMap(labels) { it.heartRate ?: 0 }
        val exerciseData = createDataMap(labels) { it.activeMinutes ?: 0 }

        setupBarChart(binding.chartStepsLayout, BAR_CHART_STEPS_ID, stepsData, "steps")
        setupBarChart(binding.chartHydrationLayout, BAR_CHART_HYDRATION_ID, hydrationData, "L")
        setupBarChart(binding.chartSleepLayout, BAR_CHART_SLEEP_ID, sleepData, "h")
        setupBarChart(binding.chartHeartRateLayout, BAR_CHART_HEART_RATE_ID, heartRateData, "bpm")
        setupBarChart(binding.chartExerciseLayout, BAR_CHART_EXERCISE_ID, exerciseData, "min")
    }

    private fun getFormattedLabels(): List<String> {
        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()

        val formatter = when (healthDataPeriod) {
            HealthDataPeriod.Week -> weekFormat
            HealthDataPeriod.Month -> monthFormat
            HealthDataPeriod.Year -> yearFormat
        }

        when (healthDataPeriod) {
            HealthDataPeriod.Year -> {
                calendar.time = Date()

                for (i in 0 until 12) {
                    labels.add(formatter.format(calendar.time))
                    calendar.add(Calendar.MONTH, -1)
                }
            }

            else -> {
                calendar.time = Date()
                val total = healthRecords.count().plus(1) ?: 0
                for (i in 0 until total) {
                    labels.add(formatter.format(calendar.time))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
        }

        return labels.reversed()
    }

    private fun createDataMap(labels: List<String>, valueSelector: (HealthRecord) -> Int): Map<String, Int> {
        val dataMap = mutableMapOf<String, Int>()
        labels.forEach { label -> dataMap[label] = 0 }

        if (healthDataPeriod == HealthDataPeriod.Year) {
            val formatter = SimpleDateFormat("MMM", Locale.getDefault())
            val grouped = healthRecords.groupBy {
                it.startsAt?.let { start ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(start)?.let { it1 -> formatter.format(it1) }
                } ?: ""
            }

            for ((month, records) in grouped) {
                if (month in dataMap) {
                    dataMap[month] = records.sumOf { valueSelector(it) }
                }
            }
        } else {
            healthRecords.forEachIndexed { index, record ->
                if (index < labels.size) {
                    val label = labels[index]
                    dataMap[label] = valueSelector(record)
                }
            }
        }

        return dataMap
    }

    private fun setupBarChart(container: LinearLayout, chartId: Int, dataMap: Map<String, Int>, unit: String) {
        val maxValue = dataMap.values.maxOrNull() ?: 1
        val avgValue = if (dataMap.isNotEmpty()) dataMap.values.sum().toDouble() / dataMap.size else 0.0

        container.removeAllViews()

        RoundedBarChartTime(requireContext()).apply {
            id = chartId
            setupChartWithData(this, dataMap, maxValue, avgValue, unit)
            container.addView(this)
            (layoutParams as? LinearLayout.LayoutParams)?.height = 180.dp
        }
    }

    private fun setupChartWithData(chart: RoundedBarChartTime, dataMap: Map<String, Int>, maxValue: Int, avgValue: Double, unit: String) {
        chart.apply {
            description?.isEnabled = false
            legend?.isEnabled = false
            isDoubleTapToZoomEnabled = false
            isHorizontalScrollBarEnabled = false
            isDragEnabled = true
            setTouchEnabled(true)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawValueAboveBar(true)
            animateY(1000)
            setRadius(9)
            setBackgroundColor(Color.TRANSPARENT)

            axisLeft.apply {
                isEnabled = true
                axisMinimum = 0f
                axisMaximum = maxValue.toFloat() * 1.2f
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(context, R.color.text_green_dark)
                axisLineColor = Color.TRANSPARENT
                gridColor = Color.TRANSPARENT
            }

            xAxis.apply {
                isEnabled = true
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(dataMap.keys.toTypedArray())

                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = if (healthDataPeriod == HealthDataPeriod.Month) {
                    6
                } else {
                    dataMap.size
                }
                axisLineColor = Color.TRANSPARENT
                gridColor = Color.TRANSPARENT
                textColor = ContextCompat.getColor(context, R.color.text_green_dark)
                typeface = Typeface.DEFAULT
                setDrawAxisLine(false)
            }

            axisRight.isEnabled = false
            data = createBarData(dataMap)

            marker = CustomMarkerView(context, R.layout.marker_view_layout, unit, dataMap)

            invalidate()
        }
    }

    private fun createBarData(dataMap: Map<String, Int>): BarData {
        val filledValues = ArrayList<BarEntry>()
        val emptyValues = ArrayList<BarEntry>()

        dataMap.values.forEachIndexed { index, count ->
            if (count > 0) {
                filledValues.add(BarEntry(index.toFloat(), count.toFloat()))
            } else {
                emptyValues.add(BarEntry(index.toFloat(), 0f))
            }
        }

        val filledBarDataSet = BarDataSet(filledValues, "Filled Data").apply {
            setGradientColor(
                ContextCompat.getColor(requireContext(), R.color.text_green_dark),
                ContextCompat.getColor(requireContext(), R.color.background_green_light)
            )
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_green_dark)
            setDrawValues(false)
            barShadowColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        }

        val emptyBarDataSet = BarDataSet(emptyValues, "Empty Data").apply {
            setGradientColor(
                ContextCompat.getColor(requireContext(), R.color.color_selected_start),
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            color = ContextCompat.getColor(requireContext(), R.color.text_green_dark)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_green_dark)
            setDrawValues(false)
            barShadowColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        }

        return BarData(filledBarDataSet, emptyBarDataSet).apply {
            barWidth = 0.7f
        }
    }

    @SuppressLint("ViewConstructor")
    inner class CustomMarkerView(
        context: Context,
        layoutResource: Int,
        private val unit: String,
        private val dataMap: Map<String, Int>
    ) : MarkerView(context, layoutResource) {
        private val dateText: TextView = findViewById(R.id.dateText)
        private val valueText: TextView = findViewById(R.id.valueText)

        @SuppressLint("SetTextI18n")
        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            e?.let {
                val selectedBarIndex = e.x.toInt()
                val labels = dataMap.keys.toList()
                val selectedLabel = labels.getOrNull(selectedBarIndex) ?: ""
                val value = e.y.toInt()

                dateText.text = selectedLabel
                valueText.text = "$value $unit"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), -height.toFloat())
        }
    }
}
