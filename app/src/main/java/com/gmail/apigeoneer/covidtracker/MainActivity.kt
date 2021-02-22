package com.gmail.apigeoneer.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

    private const val BASE_URL = "https://api.covidtracking.com/v1/"
    private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: CovidSparkAdapter
    private lateinit var sparkView: SparkView
    private lateinit var rbAllTime: RadioButton
    private lateinit var rbPositive: RadioButton
    private lateinit var tvDateLabel: TextView
    private lateinit var tvMetricLabel: TextView
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sparkView = findViewById(R.id.spark_view)
        rbAllTime = findViewById(R.id.all_time_rb)
        rbPositive = findViewById(R.id.positive_rb)
        tvMetricLabel = findViewById(R.id.metric_label_tv)
        tvDateLabel = findViewById(R.id.date_label_tv)

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
                // WE USE REVERSED(), to call the older data first (for graphing purposes)
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
                // We're setting up the event listener here because we only want to
                // update the text to display the data we actually have a response
                setupEventListeners()
                /**
                 * WE USE REVERSED(), to call the older data first (for graphing purposes)
                 * We need to create MAPPING of each state w/ its Covid data,
                 * since the JSON data contains an array of State objects,
                 * one object for each state, w/ data for all dates.
                 */
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG, "Update spinner w/ state names")
                // TODO: Update graph w/ state data
            }
        })
    }

    private fun setupEventListeners() {
        /* Here goes all the logic for listening to the event when any of the radio buttons are clicked */

        // Enable scrubbing on the chart & add a scrub Listener
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if(itemData is CovidData) {
                updateInfoDate(itemData)
            }
        }

        // Respond to radio button selected events
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        // Create a new SparkAdapter w/ the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        // Update radio buttons to select 'Positive' cases & 'All Time' by default
        rbPositive.isChecked = true
        rbAllTime.isChecked = true
        // Display metric for the most  recent data
        updateInfoDate(dailyData.last())
    }

    private fun updateInfoDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        // Formatting the no. to include commas & decimals at proper places
        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        // Formatting the date to a more readable form
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}