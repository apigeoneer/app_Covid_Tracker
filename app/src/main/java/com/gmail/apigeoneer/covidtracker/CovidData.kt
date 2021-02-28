package com.gmail.apigeoneer.covidtracker

import java.util.*

data class CovidData(
    //Not needed, Not working
    //@SerializedName("dateChecked") val dateChecked : String
    val dateChecked : Date,
    val positiveIncrease : Int,
    val negativeIncrease : Int,
    val deathIncrease : Int,
    val state : String
)