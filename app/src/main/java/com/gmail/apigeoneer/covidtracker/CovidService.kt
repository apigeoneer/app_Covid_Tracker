package com.gmail.apigeoneer.covidtracker

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {
    // Due to the Asynchronous nature of Network calls, we need to wrap up
    // the return value in Retrofit Call obj.
    // here end-point : "us/daily.json"
    @GET("us/daily.json")
    fun getNationalData() : Call<List<CovidData>>

    @GET("states/daily.json")
    fun getStateData() : Call<List<CovidData>>
}