package com.gmail.apigeoneer.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

    private const val BASE_URL = "https://api.covidtracking.com/v1/"
    private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var nationalDailyData: List<CovidData>
    private lateinit var perStateDailyData: Map<String, List<CovidData>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * Use Retrofit (w/ Gson Converter)
         */
        // We need to add the setDateFormat() method to state the date format in which
        // it will have to interpret the API Date data
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        // Create an instance of the CovidService Interface
        val covidService = retrofit.create(CovidService::class.java)

        /**
         * Fetch the National data
         */
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Didn't receive a valid response body for the National data.")
                    return
                }
                // To call the older data first (for grafting purposes), we use reversed(
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph w/ national data")
                updateDisplayWithData(nationalDailyData)
            }
        })

        /**
         * Fetch the State data
         */
        covidService.getStateData().enqueue(object: Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Didn't receive a valid response body for the State data.")
                    return
                }
                // To call the older data first (for grafting purposes), we use reversed(
                // We need to create mapping of each state & its Covid data,
                // since the json data contains an array of State objects,
                // one object for each state w/ data for all dates
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner w/ state names")
                // TODO: Update graph w/ state data
            }
        })
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        // Create a new SparkAdapter w/ the data
        // Update radio buttons to select 'Positive' cases & 'All Time' by default
        // Display metric for the most  recent data
    }
}