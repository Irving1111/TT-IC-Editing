package com.example.homework_3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homework_3.R
import com.example.homework_3.model.Cast

class ForecastAdapter(private var forecasts: List<Cast>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvForecastDate: TextView = view.findViewById(R.id.tvForecastDate)
        val tvForecastWeek: TextView = view.findViewById(R.id.tvForecastWeek)
        val tvForecastWeather: TextView = view.findViewById(R.id.tvForecastWeather)
        val tvForecastTemp: TextView = view.findViewById(R.id.tvForecastTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecasts[position]
        
        // 格式化日期显示
        val dateLabel = when (position) {
            0 -> "今天"
            1 -> "明天"
            else -> "星期${forecast.week}"
        }
        holder.tvForecastDate.text = dateLabel
        holder.tvForecastWeek.text = forecast.date
        
        // 显示天气图标和描述
        val weatherIcon = getWeatherIcon(forecast.dayweather)
        holder.tvForecastWeather.text = "$weatherIcon ${forecast.dayweather}"
        
        // 显示温度范围
        holder.tvForecastTemp.text = "${forecast.daytemp}° ${forecast.nighttemp}°"
    }

    override fun getItemCount() = forecasts.size

    fun updateData(newForecasts: List<Cast>) {
        forecasts = newForecasts
        notifyDataSetChanged()
    }

    private fun getWeatherIcon(weather: String): String {
        return when {
            weather.contains("晴") -> "☀"
            weather.contains("云") -> "☁"
            weather.contains("阴") -> "☁"
            weather.contains("雨") -> "🌧"
            weather.contains("雪") -> "❄"
            weather.contains("雷") -> "⚡"
            else -> "🌤"
        }
    }
}
