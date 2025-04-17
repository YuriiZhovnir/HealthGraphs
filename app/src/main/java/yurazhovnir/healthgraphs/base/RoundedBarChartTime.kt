package yurazhovnir.healthgraphs.base

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import yurazhovnir.healthgraphs.R


class RoundedBarChartTime @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : BarChart(context, attrs, defStyle) {

    private var mBarBorderPaint: Paint = Paint()
    private var mBarValuePaint: Paint = Paint()

    init {
        attrs?.let { readRadiusAttr(context, it) }
    }

    private fun readRadiusAttr(context: Context, attrs: AttributeSet) {
        val a: TypedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedBarChart, 0, 0)
        try {
            setRadius(a.getDimensionPixelSize(R.styleable.RoundedBarChart_radius, 0))
            mBarBorderPaint.apply {
                color = a.getColor(R.styleable.RoundedBarChart_barBorderColor, 0)
                strokeWidth = a.getDimension(R.styleable.RoundedBarChart_barBorderWidth, 0f)
                style = Paint.Style.STROKE
            }
            mBarValuePaint.apply {
                textSize = Utils.convertDpToPixel(20f)
                textAlign = Paint.Align.CENTER

            }
        } finally {
            a.recycle()
        }
    }

    fun setRadius(radius: Int) {
        renderer = RoundedBarChartRenderer(this, animator, viewPortHandler, radius)
    }

    fun setBorder(color: Int, width: Float) {
        mBarBorderPaint.apply {
            this.color = color
            this.strokeWidth = width
        }
    }

    private inner class RoundedBarChartRenderer(
        chart: BarDataProvider,
        animator: ChartAnimator,
        viewPortHandler: ViewPortHandler,
        private val mRadius: Int,
    ) : BarChartRenderer(chart, animator, viewPortHandler) {


        override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
            val barData = mChart.barData
            for (high in indices) {
                val set = barData.getDataSetByIndex(high.dataSetIndex) ?: continue
                if (!set.isHighlightEnabled) continue

                val e = set.getEntryForXValue(high.x, high.y) ?: continue
                if (!isInBoundsX(e, set)) continue

                val trans = mChart.getTransformer(set.axisDependency)
                mHighlightPaint.color = set.highLightColor
                mHighlightPaint.alpha = set.highLightAlpha

                val isStack = high.stackIndex >= 0 && e.isStacked
                val (y1, y2) = if (isStack) {
                    if (mChart.isHighlightFullBarEnabled) {
                        e.positiveSum to -e.negativeSum
                    } else {
                        val range = e.ranges[high.stackIndex]
                        range.from - 3.7f to range.to
                    }
                } else {
                    e.y to 0f
                }

                prepareBarHighlight(e.x, y1, y2, barData.barWidth / 2f, trans)
                setHighlightDrawPos(high, mBarRect)
                c.drawRoundRect(mBarRect, mRadius.toFloat(), mRadius.toFloat(), mHighlightPaint)
            }
        }

        override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
            val trans = mChart.getTransformer(dataSet.axisDependency)
            mBarBorderPaint.color = dataSet.barBorderColor
            mBarBorderPaint.strokeWidth = Utils.convertDpToPixel(dataSet.barBorderWidth)

            val drawBorder = dataSet.barBorderWidth > 0f
            val phaseX = animator.phaseX
            val phaseY = animator.phaseY

            val buffer = mBarBuffers[index]
            buffer.setPhases(phaseX, phaseY)
            buffer.setDataSet(index)
            buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
            buffer.setBarWidth(mChart.barData.barWidth)
            buffer.feed(dataSet)
            trans.pointValuesToPixel(buffer.buffer)

            val isSingleColor = dataSet.colors.size == 1
            if (isSingleColor) mRenderPaint.color = dataSet.color
            val minHeight = Utils.convertDpToPixel(22f)
            for (j in buffer.buffer.indices step 4) {
                if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) continue
                if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break
                val barValue = dataSet.getEntryForIndex(j / 4).y

                val top = if (barValue == 0f) {
                    mViewPortHandler.contentTop() - 40f
                } else {
                    val calculatedTop = buffer.buffer[j + 1]
                    val bottom = buffer.buffer[j + 3]
                    if (Math.abs(calculatedTop - bottom) < minHeight) {
                        bottom - minHeight
                    } else {
                        calculatedTop - 40f
                    }
                }

                val bottom = buffer.buffer[j + 3]

                if (!isSingleColor) mRenderPaint.color = dataSet.getColor(j / 4)

                dataSet.gradientColor?.let { gradientColor ->
                    mRenderPaint.shader = LinearGradient(
                        buffer.buffer[j], bottom,
                        buffer.buffer[j], top,
                        gradientColor.startColor, gradientColor.endColor,
                        Shader.TileMode.MIRROR
                    )
                } ?: dataSet.gradientColors?.let {
                    mRenderPaint.shader = LinearGradient(
                        buffer.buffer[j], bottom,
                        buffer.buffer[j], top,
                        it[j / 4].startColor, it[j / 4].endColor,
                        Shader.TileMode.MIRROR
                    )
                }

                val increasedRadius = mRadius.toFloat() * 1.5

                c.drawRoundRect(
                    buffer.buffer[j],
                    top,
                    buffer.buffer[j + 2],
                    bottom,
                    increasedRadius.toFloat(),
                    increasedRadius.toFloat(),
                    mRenderPaint
                )

                if (drawBorder) {
                    c.drawRoundRect(
                        buffer.buffer[j],
                        top,
                        buffer.buffer[j + 2],
                        bottom,
                        increasedRadius.toFloat(),
                        increasedRadius.toFloat(),
                        mBarBorderPaint
                    )
                }
            }
        }
    }
}