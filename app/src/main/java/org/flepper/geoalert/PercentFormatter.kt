package org.flepper.geoalert

import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat


class PercentFormatter() : ValueFormatter() {
    var mFormat: DecimalFormat
    private var pieChart: PieChart? = null
    private var percentSignSeparated: Boolean

    // Can be used to remove percent signs if the chart isn't in percent mode
    constructor(pieChart: PieChart?) : this() {
        this.pieChart = pieChart
    }

    // Can be used to remove percent signs if the chart isn't in percent mode
    constructor(pieChart: PieChart?, percentSignSeparated: Boolean) : this(pieChart) {
        this.percentSignSeparated = percentSignSeparated
    }

    override fun getFormattedValue(value: Float): String {
        return mFormat.format(value.toInt()) + if (percentSignSeparated) " %" else "%"
    }

    override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
        return if (pieChart != null && pieChart!!.isUsePercentValuesEnabled) {
            // Converted to percent
            getFormattedValue(value)
        } else {
            // raw value, skip percent sign
            mFormat.format(value.toDouble())
        }
    }

    init {
        mFormat = DecimalFormat("###,###,##0")
        percentSignSeparated = true
    }
}
