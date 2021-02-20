package com.gmail.apigeoneer.covidtracker

data class CovidData(
    //Not needed, Not working
    //@SerializedName("dateChecked") val dateChecked : String
    val dateChecked : String,
    val positiveIncrease : Int,
    val negativeIncrease : Int,
    val deathIncrease : Int,
    val state : String
)