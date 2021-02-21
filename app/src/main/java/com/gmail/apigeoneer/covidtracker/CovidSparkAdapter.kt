package com.gmail.apigeoneer.covidtracker

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

    override fun getItem(index: Int) = dailyData[index]

    override fun getCount() = dailyData.size

}
