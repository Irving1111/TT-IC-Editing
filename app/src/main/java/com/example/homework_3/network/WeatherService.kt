package com.example.homework_3.network

import android.util.Log
import com.example.homework_3.model.WeatherResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WeatherService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    companion object {
        private const val BASE_URL = "https://restapi.amap.com/v3/weather/weatherInfo"
        private const val API_KEY = "5f930e0954ac0b0de1e3866f0932186d"
    }
    
    /**
     * 获取实时天气
     * @param city 城市编码，例如：110101（北京市东城区）
     */
    suspend fun getLiveWeather(city: String): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?city=$city&extensions=base&key=$API_KEY"
            Log.d("WeatherService", "实时天气请求URL: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            Log.d("WeatherService", "实时天气响应: $body")
            
            if (response.isSuccessful && body != null) {
                val weatherResponse = gson.fromJson(body, WeatherResponse::class.java)
                if (weatherResponse.status == "1") {
                    Result.success(weatherResponse)
                } else {
                    Result.failure(Exception("API错误: ${weatherResponse.info}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取预报天气
     * @param city 城市编码，例如：110101（北京市东城区）
     */
    suspend fun getForecastWeather(city: String): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL?city=$city&extensions=all&key=$API_KEY"
            Log.d("WeatherService", "预报天气请求URL: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            Log.d("WeatherService", "预报天气响应: $body")
            
            if (response.isSuccessful && body != null) {
                val weatherResponse = gson.fromJson(body, WeatherResponse::class.java)
                if (weatherResponse.status == "1") {
                    Result.success(weatherResponse)
                } else {
                    Result.failure(Exception("API错误: ${weatherResponse.info}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
