package com.example.homework_3.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("count")
    val count: String? = null,
    @SerializedName("info")
    val info: String,
    @SerializedName("infocode")
    val infocode: String,
    @SerializedName("lives")
    val lives: List<LiveWeather>? = null,
    @SerializedName("forecasts")
    val forecasts: List<ForecastWeather>? = null
)

data class LiveWeather(
    @SerializedName("province")
    val province: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("adcode")
    val adcode: String,
    @SerializedName("weather")
    val weather: String,
    @SerializedName("temperature")
    val temperature: String,
    @SerializedName("winddirection")
    val winddirection: String,
    @SerializedName("windpower")
    val windpower: String,
    @SerializedName("humidity")
    val humidity: String,
    @SerializedName("reporttime")
    val reporttime: String,
    @SerializedName("temperature_float")
    val temperatureFloat: String? = null,
    @SerializedName("humidity_float")
    val humidityFloat: String? = null
)

data class ForecastWeather(
    @SerializedName("city")
    val city: String,
    @SerializedName("adcode")
    val adcode: String,
    @SerializedName("province")
    val province: String,
    @SerializedName("reporttime")
    val reporttime: String,
    @SerializedName("casts")
    val casts: List<Cast>
)

data class Cast(
    @SerializedName("date")
    val date: String,
    @SerializedName("week")
    val week: String,
    @SerializedName("dayweather")
    val dayweather: String,
    @SerializedName("nightweather")
    val nightweather: String,
    @SerializedName("daytemp")
    val daytemp: String,
    @SerializedName("nighttemp")
    val nighttemp: String,
    @SerializedName("daywind")
    val daywind: String,
    @SerializedName("nightwind")
    val nightwind: String,
    @SerializedName("daypower")
    val daypower: String,
    @SerializedName("nightpower")
    val nightpower: String,
    @SerializedName("daytemp_float")
    val daytempFloat: String? = null,
    @SerializedName("nighttemp_float")
    val nighttempFloat: String? = null
)
