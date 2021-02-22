package com.gmail.apigeoneer.covidtracker

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(
    private val dailyData: List<CovidData>
) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.ALLTIME

    override fun getY(index: Int): Float {
        val chosenDayDate = dailyData[index]
        // Default return was Int
        return when (metric) {
            Metric.NEGATIVE -> chosenDayDate.negativeIncrease.toFloat()
            Metric.POSITIVE -> chosenDayDate.positiveIncrease.toFloat()
            Metric.DEATH -> chosenDayDate.deathIncrease.toFloat()
        }
    }

    // RETURNS AN OBJ. in the JSON array at the position - index
    override fun getItem(index: Int) = dailyData[index]

    override fun getCount() = dailyData.size

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.ALLTIME) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }
}
