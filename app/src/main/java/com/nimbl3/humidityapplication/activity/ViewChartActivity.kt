package com.nimbl3.humidityapplication.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nimbl3.humidityapplication.R
import com.nimbl3.humidityapplication.model.Humidity
import kotlinx.android.synthetic.main.activity_view_chart.*
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import kotlin.properties.Delegates

class ViewChartActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    private val mGso by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("255969535922-4pd7j60pptut9is33nhib580s9r7mlbq.apps.googleusercontent.com")
                .requestEmail()
                .build()
    }

    private val mApiClient by lazy {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso)
                .build()
    }

    private val mHumidityReference by lazy {
        FirebaseDatabase.getInstance().reference.child("humidity")
    }

    private var humidity by Delegates.observable(Humidity())
    { _, _, new ->
        tvTag.visibility = View.VISIBLE
        tvHumidity.text = String.format("%.02f%%", new.measurement)
        setStatus(new.measurement)
        setTextColor(new.measurement)
    }

    private fun setTextColor(measurement: Double) {
        val blue = ContextCompat.getColor(this, R.color.real_blue)
        val red = ContextCompat.getColor(this, R.color.real_red)
        when (measurement) {
            in (40..60) -> {
                tvHumidity.setTextColor(blue)
                tvStatus.setTextColor(blue)
            }
            in (0..39) -> {
                tvHumidity.setTextColor(red)
                tvStatus.setTextColor(red)
            }
            in (61..100) -> {
                tvHumidity.setTextColor(red)
                tvStatus.setTextColor(red)
            }
        }
    }

    private fun setStatus(measurement: Double) {
        when (measurement) {
            in (40..60) -> tvStatus.text = "Good humid!"
            in (0..39) -> tvStatus.text = "Low humid!"
            in (61..100) -> tvStatus.text = "Too humid!"
        }
    }


    private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(dateSnapshot: DataSnapshot) {
            setData(dateSnapshot)
        }

        override fun onCancelled(p0: DatabaseError?) {

        }

    }

    private fun setData(dateSnapshot: DataSnapshot) {
        val humidities = mutableListOf<Humidity>()
        dateSnapshot.children.mapNotNullTo(humidities) { it.getValue(Humidity::class.java) }
        invalidateChart(humidities)
        humidity = humidities.first()
    }

    private fun invalidateChart(humiditys: MutableList<Humidity>) {
        var entries = ArrayList<Entry>()
        humiditys.filter { isLessThanSixtyMin(it.date) }.mapNotNullTo(entries) {
            Entry(calcDiff(it.date), it.measurement.toFloat())
        }

        entries.sortBy { it.x }

        val lineDataSet = LineDataSet(entries, "humidity").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.1f
            setDrawFilled(true)
            setDrawCircles(false)
            lineWidth = 1f
            setColor(Color.RED)

            setCircleColor(Color.RED)
            highLightColor = Color.RED
            color = Color.WHITE
            fillColor = Color.BLUE
            fillAlpha = 85
            setDrawValues(false)
            setDrawHorizontalHighlightIndicator(true)
        }


        val lineData = LineData(lineDataSet)
        lcHumidity.data = lineData
        lcHumidity.notifyDataSetChanged()
        lcHumidity.invalidate()

    }

    private fun isLessThanSixtyMin(date: String): Boolean {
        val d1 = LocalDateTime.now()
        val d2 = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val diff = Duration.between(d2, d1)
        return diff.toMinutes() < 60
    }

    private fun calcDiff(date: String): Float {
        val d1 = LocalDateTime.now()
        val d2 = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val diff = Duration.between(d2, d1)
        return diff.toMinutes().toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_chart)

        mApiClient

        lcHumidity.apply {
            fitScreen()
            setViewPortOffsets(0f, 10f, 0f, 0f)
            setTouchEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false

            xAxis.apply {
                axisMinimum = 0f
                axisMaximum = 60f
                setDrawAxisLine(false)
                setDrawGridLines(false)
                labelCount = 6
                setValueFormatter { value, _ ->
                    if (value in floatArrayOf(0f, 60f)) {
                        ""
                    } else {
                        String.format("%.0f min ago", value)
                    }
                }

                position = XAxis.XAxisPosition.BOTTOM_INSIDE
            }

            axisLeft.apply {
                setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                axisMinimum = 10f
                axisMaximum = 100f
                enableGridDashedLine(10f, 10f, 2f)
                labelCount = 5
                granularity = 20f
                setDrawAxisLine(false)
                gridColor = Color.RED
                setValueFormatter { value, _ ->
                    if (value in floatArrayOf(0f)) {
                        ""
                    } else {
                        String.format("%.0f%%", value)
                    }

                }
                isEnabled = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mHumidityReference.addValueEventListener(valueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHumidityReference.removeEventListener(valueEventListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.view_chart, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut()
            Auth.GoogleSignInApi.signOut(mApiClient)
                    .setResultCallback {
                        if (it.isSuccess) {
                            val intent = Intent(ViewChartActivity@ this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(LoginActivity@ this, "fail", Toast.LENGTH_SHORT).show()
                        }
                    }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}