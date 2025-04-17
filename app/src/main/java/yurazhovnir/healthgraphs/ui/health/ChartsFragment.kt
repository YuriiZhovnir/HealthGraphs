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
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.launch
import yurazhovnir.healthgraphs.HealthDataPeriod
import yurazhovnir.healthgraphs.R
import yurazhovnir.healthgraphs.base.BaseBindingFragment
import yurazhovnir.healthgraphs.base.RoundedBarChartTime
import yurazhovnir.healthgraphs.databinding.FragmentChartsBinding
import yurazhovnir.healthgraphs.dp
import yurazhovnir.healthgraphs.helper.HealthConnectManager
import yurazhovnir.healthgraphs.model.HealthRecord
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    private lateinit var healthConnectManager: HealthConnectManager
    private val dayFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val weekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("MMM", Locale.getDefault())
    var healthDataPeriod: HealthDataPeriod = HealthDataPeriod.Day
    private val healthRecords: ArrayList<HealthRecord>? = ArrayList()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        healthConnectManager = HealthConnectManager(requireContext())
        loadHealthData()
    }
        private fun loadHealthData() {
            lifecycleScope.launch {
                healthRecords?.clear()

                if (healthConnectManager.checkAndRequestPermissions()) {
                    val now = Instant.now()
                    if (healthDataPeriod == HealthDataPeriod.Year){
                        for (i in 1..healthDataPeriod.days) {
                            val currentMonthEnd = if (i == 0) {
                                now
                            } else {
                                val currentDate = LocalDateTime.ofInstant(now, ZoneId.systemDefault())
                                val firstDayOfMonth = currentDate.minusMonths(i.toLong()).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                                val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusSeconds(1)
                                lastDayOfMonth.atZone(ZoneId.systemDefault()).toInstant()
                            }

                            val currentMonthStart = LocalDateTime.ofInstant(currentMonthEnd, ZoneId.systemDefault())
                                .withDayOfMonth(1)
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()

                            var monthlySteps = 0
                            var monthlyHydration = 0.0
                            var monthlySleepHours = 0.0
                            var monthlyHeartRate = 0.0
                            var monthlyActiveMinutes = 0.0
                            var daysWithData = 0

                            var currentDay = currentMonthStart
                            while (currentDay.isBefore(currentMonthEnd)) {
                                val nextDay = currentDay.plus(1, ChronoUnit.DAYS)

                                val dailySummary = healthConnectManager.getDailyBiometricSummary(
                                    startTime = currentDay,
                                    endTime = nextDay
                                )

                                if (dailySummary.steps != null || dailySummary.hydration != null ||
                                    dailySummary.sleepHours != null || dailySummary.heartRate != null ||
                                    dailySummary.physicalActivity != null) {

                                    monthlySteps += dailySummary.steps ?: 0
                                    monthlyHydration += dailySummary.hydration ?: 0.0
                                    monthlySleepHours += dailySummary.sleepHours ?: 0.0
                                    monthlyHeartRate += if (dailySummary.heartRate != null) dailySummary.heartRate else 0.0
                                    monthlyActiveMinutes += dailySummary.physicalActivity ?: 0.0
                                    daysWithData++
                                }
                                currentDay = nextDay
                            }

                            val avgHeartRate = if (daysWithData > 0) monthlyHeartRate / daysWithData else 0.0
                            val avgSleepHours = if (daysWithData > 0) monthlySleepHours / daysWithData else 0.0

                            val newRecord = HealthRecord().apply {
                                val monthStartDateTime = LocalDateTime.ofInstant(currentMonthStart, ZoneId.systemDefault())
                                val formattedDate = monthStartDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                doneAt = formattedDate
                                startsAt = formattedDate

                                steps = monthlySteps
                                hydration = monthlyHydration
                                sleep = avgSleepHours.toInt()
                                heartRate = avgHeartRate.toInt()
                                activeMinutes = monthlyActiveMinutes.toInt()
                                source = "Health Connect"
                            }

                            healthRecords?.add(newRecord)
                        }
                    }else{
                        for (i in 1..healthDataPeriod.days) {
                            val dayEnd = now.minus((i - 1).toLong(), ChronoUnit.DAYS)
                            val dayStart = dayEnd.minus(1, ChronoUnit.DAYS)

                            val summary = healthConnectManager.getDailyBiometricSummary(
                                startTime = dayStart,
                                endTime = dayEnd
                            )

                            val newRecord = HealthRecord().apply {
                                val recordDateTime = LocalDateTime.ofInstant(dayStart, ZoneId.systemDefault())
                                val formattedDate = recordDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                doneAt = formattedDate
                                startsAt = formattedDate

                                steps = summary.steps
                                hydration = summary.hydration
                                sleep = summary.sleepHours?.toInt()
                                heartRate = summary.heartRate?.toInt()
                                activeMinutes = summary.physicalActivity?.toInt()
                                source = "Health Connect"
                            }

                            healthRecords?.add(newRecord)
                        }
                    }

                    updateAllCharts()
                }
            }
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
                HealthDataPeriod.Day -> dayFormat
                HealthDataPeriod.Week -> weekFormat
                HealthDataPeriod.Month -> monthFormat
                HealthDataPeriod.Year -> yearFormat
            }

            when (healthDataPeriod) {
                HealthDataPeriod.Day -> {
                    for (i in 0 until 24 step 4) {
                        calendar.set(Calendar.HOUR_OF_DAY, i)
                        calendar.set(Calendar.MINUTE, 0)
                        if (formatter is SimpleDateFormat) {
                            labels.add(formatter.format(calendar.time))
                        } else {
                            val localDateTime = LocalDateTime.of(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.DAY_OF_MONTH),
                                i,
                                0
                            )
                            labels.add(localDateTime.format(formatter as DateTimeFormatter))
                        }
                    }
                }
                else -> {
                    calendar.time = Date()

                    for (i in 0 until healthDataPeriod.days) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        if (formatter is SimpleDateFormat) {
                            labels.add(formatter.format(calendar.time))
                        } else {
                            val localDateTime = LocalDateTime.ofInstant(
                                calendar.toInstant(),
                                ZoneId.systemDefault()
                            )
                            labels.add(localDateTime.format(formatter as DateTimeFormatter))
                        }
                    }
                }
            }

            return labels.reversed()
        }

        private fun createDataMap(
            labels: List<String>,
            valueSelector: (HealthRecord) -> Int
        ): Map<String, Int> {
            val dataMap = mutableMapOf<String, Int>()

            labels.forEach { label ->
                dataMap[label] = 0
            }

            healthRecords?.forEachIndexed { index, record ->
                if (index < labels.size) {
                    val label = labels[index]
                    dataMap[label] = valueSelector(record)
                }
            }

            return dataMap
        }

        private fun setupBarChart(
            container: LinearLayout,
            chartId: Int,
            dataMap: Map<String, Int>,
            unit: String
        ) {
            val maxValue = dataMap.values.maxOrNull() ?: 1
            val avgValue = if (dataMap.isNotEmpty()) {
                dataMap.values.sum().toDouble() / dataMap.size
            } else {
                0.0
            }

            container.removeAllViews()

            RoundedBarChartTime(requireContext()).apply {
                id = chartId
                setupChartWithData(this, dataMap, maxValue, avgValue, unit)
                container.addView(this)
                (layoutParams as? LinearLayout.LayoutParams)?.height = 180.dp
            }
        }

        private fun setupChartWithData(
            chart: RoundedBarChartTime,
            dataMap: Map<String, Int>,
            maxValue: Int,
            avgValue: Double,
            unit: String
        ) {
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
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return when (value.toInt()) {
                                0, avgValue.toInt(), maxValue -> "${value.toInt()}$unit"
                                else -> ""
                            }
                        }
                    }
                    axisMinimum = 0f
                    axisMaximum = maxValue.toFloat() * 1.2f // Add some space at the top
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
                    labelCount = dataMap.size
                    axisLineColor = Color.TRANSPARENT
                    gridColor = Color.TRANSPARENT
                    textColor = ContextCompat.getColor(context, R.color.text_green_dark)
                    typeface = Typeface.DEFAULT
                    setDrawAxisLine(false)

                    // Rotate labels if needed
                    if (healthDataPeriod == HealthDataPeriod.Month) {
                        labelRotationAngle = 45f
                    }
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
                    ContextCompat.getColor(requireContext(), R.color.black),
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

        override fun onDestroyView() {
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            super.onDestroyView()
        }
}