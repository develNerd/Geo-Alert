package org.flepper.geoalert.civilian

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import org.flepper.geoalert.PercentFormatter
import org.flepper.geoalert.R

class CivilianSettingsActivity : AppCompatActivity() {

    var pieChart: PieChart? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_civilian_settings)

        pieChart = findViewById(R.id.piechart) as PieChart
        pieChart!!.setUsePercentValues(true)

        pieChart!!.description.isEnabled = true
        pieChart!!.setExtraOffsets(5f,10f,5f,5f)

        pieChart!!.dragDecelerationFrictionCoef =0.60f
        pieChart!!.isDrawHoleEnabled = true
        pieChart!!.setHoleColor(Color.WHITE)
        pieChart!!.holeRadius = 20f
        pieChart!!.setDrawSlicesUnderHole(true)
        pieChart!!.setDrawRoundedSlices(true)
        pieChart!!.setDrawSlicesUnderHole(true)
        pieChart!!.transparentCircleRadius = 70f

        val yvalues :ArrayList<PieEntry> = ArrayList<PieEntry>()

        yvalues.add(PieEntry(0.56f,"Crime Rates"))
        yvalues.add(PieEntry(0.34f,"Accidents"))
        yvalues.add(PieEntry(0.44f,"Fire incidents"))


        pieChart!!.animateY(800, Easing.EaseInCubic)
        pieChart!!.description.text = ""

        pieChart!!.setUsePercentValues(true)
        pieChart!!.isUsePercentValuesEnabled

        val dataSet: PieDataSet = PieDataSet(yvalues,"Trans.")
        dataSet.selectionShift = 5f
        dataSet.valueFormatter = PercentFormatter(pieChart!!,false)
        val colors = intArrayOf(Color.parseColor("#74B57A"),Color.GRAY,Color.parseColor("#FFAA66CC"))
        val joyfulcolors = ColorTemplate.COLORFUL_COLORS.toMutableList()
        val arrayColors =
            arrayOf("#00ff00", "#0000ff", "#ff0000")
        dataSet.colors = colors.toMutableList()

        val data: PieData = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart!!,false))
        data.setValueTextSize(10f)
        data.setValueTextColor(Color.YELLOW)
        pieChart!!.data = data



        val description: Description = Description()
    }
}
