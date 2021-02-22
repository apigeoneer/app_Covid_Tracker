package com.gmail.apigeoneer.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import org.angmarch.views.NiceSpinner
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val BASE_URL = "https://api.covidtracking.com/v1/"
        private const val TAG = "MainActivity"
        private const val ALL_STATES = "All (nationwide)"
    }

    private lateinit var spinnerSelect: NiceSpinner
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var rgMetricSelection: RadioGroup
    private lateinit var rgTimeSelection: RadioGroup
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var sparkView: SparkView
    private lateinit var rbAllTime: RadioButton
    private lateinit var rbPositive: RadioButton
    private lateinit var tvDateLabel: TextView
    private lateinit var tickerView: TickerView
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerSelect = findViewById(R.id.spinner_select)
        rgMetricSelection = findViewById(R.id.metric_selection_rg)
        rgTimeSelection = findViewById(R.id.time_selection_rg)
        sparkView = findViewById(R.id.spark_view)
        rbAllTime = findViewById(R.id.all_time_rb)
        rbPositive = findViewById(R.id.positive_rb)
        tickerView = findViewById(R.id.ticker_view)
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

                setupEventListeners()
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
                // Update spinner w/ state names
                updateSpinnerWithStateData(perStateDailyData.keys)
            }
        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        // Add state list as data source for the spinner
        spinnerSelect.attachDataSource(stateAbbreviationList)
        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }
    }

    private fun setupEventListeners() {
        /* Here goes all the logic for listening to the event when any of the radio buttons are clicked */
        tickerView.setCharacterLists(TickerUtils.provideNumberList())

        // Enable scrubbing on the chart & add a scrub Listener
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if(itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }

        // Respond to Radio Button selected events (for both the Radio Groups)
        rgTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.week_rb -> TimeScale.WEEK
                R.id.month_rb -> TimeScale.MONTH
                else -> TimeScale.ALLTIME
            }
            // Notifying the adapter that the underlying data has changed, so that it knows that its has to change itself
            adapter.notifyDataSetChanged()
        }

        rgMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.positive_rb -> updateDisplayMetric(Metric.POSITIVE)
                R.id.negative_rb -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.death_rb -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.negative
            Metric.POSITIVE -> R.color.positive
            Metric.DEATH -> R.color.death
        }
        // Adding annotation to make it obv that the variable stores a color
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tickerView.setTextColor(colorInt)

        // Update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset no. & date shown in the bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        // Create a new SparkAdapter w/ the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        // Update radio buttons to select 'Positive' cases & 'All Time' by default
        rbPositive.isChecked = true
        rbAllTime.isChecked = true
        // Display metric for the most  recent data
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        // Formatting the no. to include commas & decimals at proper places
        tickerView.text = NumberFormat.getInstance().format(numCases)
        // Formatting the date to a more readable form
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}