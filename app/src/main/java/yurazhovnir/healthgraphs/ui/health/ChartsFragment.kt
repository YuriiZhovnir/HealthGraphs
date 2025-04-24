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
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import yurazhovnir.healthgraphs.HealthDataPeriod
import yurazhovnir.healthgraphs.R
import yurazhovnir.healthgraphs.base.BaseBindingFragment
import yurazhovnir.healthgraphs.base.RoundedBarChartTime
import yurazhovnir.healthgraphs.databinding.FragmentChartsBinding
import yurazhovnir.healthgraphs.dp
import java.util.ArrayList


private const val BAR_CHART_STEPS_ID = 123456
private const val BAR_CHART_HYDRATION_ID = 123457
private const val BAR_CHART_SLEEP_ID = 123458
private const val BAR_CHART_HEART_RATE_ID = 123459
private const val BAR_CHART_EXERCISE_ID = 123460

class ChartsFragment : BaseBindingFragment<FragmentChartsBinding>(FragmentChartsBinding::inflate) {
    override val TAG: String
        get() = "ChartsFragment"
    var healthDataPeriod: HealthDataPeriod = HealthDataPeriod.Week
    private val viewModel: ChartsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.healthDataPeriod = healthDataPeriod
        viewModel.loadHealthData()

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.chartLabels.observe(viewLifecycleOwner) { labels ->
            viewModel.dataMaps.value?.let { data ->
                updateAllCharts(labels, data)
            }
        }

        viewModel.dataMaps.observe(viewLifecycleOwner) { data ->
            viewModel.chartLabels.value?.let { labels ->
                updateAllCharts(labels, data)
            }
        }
    }
    private fun updateAllCharts(labels: List<String>, dataMaps: Map<String, Map<String, Int>>) {
        setupBarChart(binding.chartStepsLayout, BAR_CHART_STEPS_ID, dataMaps["steps"] ?: emptyMap(), "steps")
        setupBarChart(binding.chartHydrationLayout, BAR_CHART_HYDRATION_ID, dataMaps["hydration"] ?: emptyMap(), "L")
        setupBarChart(binding.chartSleepLayout, BAR_CHART_SLEEP_ID, dataMaps["sleep"] ?: emptyMap(), "h")
        setupBarChart(binding.chartHeartRateLayout, BAR_CHART_HEART_RATE_ID, dataMaps["heartRate"] ?: emptyMap(), "bpm")
        setupBarChart(binding.chartExerciseLayout, BAR_CHART_EXERCISE_ID, dataMaps["exercise"] ?: emptyMap(), "min")
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
                    7
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
