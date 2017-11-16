package com.nimbl3.humidityapplication

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class HumidityApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}