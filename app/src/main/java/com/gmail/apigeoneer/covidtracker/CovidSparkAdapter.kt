package com.gmail.apigeoneer.covidtracker

import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(
    private val dailyData: List<CovidData>
) : SparkAdapter() {

    override fun getY(index: Int): Float {
        val chosenDayDate = dailyData[index]
        // Default return was Int
        return chosenDayDate.positiveIncrease.toFloat()
    }

    override fun getItem(index: Int) = dailyData[index]

    override fun getCount() = dailyData.size

}
