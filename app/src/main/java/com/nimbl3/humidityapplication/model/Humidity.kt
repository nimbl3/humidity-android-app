package com.nimbl3.humidityapplication.model

data class Humidity(val date: String = "",
                    val measurement: Double = 0.0,
                    val temperature: Double = 0.0)