package com.example.homework_3

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.ImageViewCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.homework_3.adapter.ForecastAdapter
import com.example.homework_3.databinding.ActivityMainBinding
import com.example.homework_3.model.Cast
import com.example.homework_3.model.LiveWeather
import com.example.homework_3.network.WeatherService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val viewModel = WeatherViewModel()

    // 城市代码映射
    private val cityCodeMap = mapOf(
        "北京" to "110000",
        "上海" to "310000",
        "广州" to "440100",
        "深圳" to "440300"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        observeViewModel()

        // 默认加载广州天气
        loadWeather("广州")
    }

    private fun initViews() {
        // 设置ViewPager2
        binding.viewPager.adapter = WeatherPagerAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateFunctionButtonSelection(position)
            }
        })

        // 设置城市按钮点击事件
        binding.btnBeijing.setOnClickListener {
            loadWeather("北京")
            updateCityButtonSelection("北京")
        }

        binding.btnShanghai.setOnClickListener {
            loadWeather("上海")
            updateCityButtonSelection("上海")
        }

        binding.btnGuangzhou.setOnClickListener {
            loadWeather("广州")
            updateCityButtonSelection("广州")
        }

        binding.btnShenzhen.setOnClickListener {
            loadWeather("深圳")
            updateCityButtonSelection("深圳")
        }

        // 城市按钮 - 切换到实时天气页面
        binding.btnCurrentWeather.setOnClickListener {
            binding.viewPager.currentItem = 0
            updateFunctionButtonSelection(0)
        }

        // 预测按钮 - 切换到预报页面
        binding.btnForecastWeather.setOnClickListener {
            binding.viewPager.currentItem = 1
            updateFunctionButtonSelection(1)
        }

        updateFunctionButtonSelection(0)
    }

    private fun updateCityButtonSelection(selectedCity: String) {
        binding.btnBeijing.setBackgroundResource(
            if (selectedCity == "北京") R.drawable.bubble_button_selected_background 
            else R.drawable.bubble_button_background
        )
        binding.btnShanghai.setBackgroundResource(
            if (selectedCity == "上海") R.drawable.bubble_button_selected_background 
            else R.drawable.bubble_button_background
        )
        binding.btnGuangzhou.setBackgroundResource(
            if (selectedCity == "广州") R.drawable.bubble_button_selected_background 
            else R.drawable.bubble_button_background
        )
        binding.btnShenzhen.setBackgroundResource(
            if (selectedCity == "深圳") R.drawable.bubble_button_selected_background 
            else R.drawable.bubble_button_background
        )
    }

    private fun loadWeather(cityName: String) {
        val cityCode = cityCodeMap[cityName] ?: return
        viewModel.loadWeather(cityCode, cityName)
    }

    private fun updateFunctionButtonSelection(selectedPage: Int) {
        val isCurrentSelected = selectedPage == 0
        val selectedColor = Color.WHITE
        val unselectedColor = Color.parseColor("#E6FFFFFF")

        applyFunctionButtonState(
            isCurrentSelected,
            binding.btnCurrentWeather,
            binding.ivCurrentIcon,
            binding.tvCurrentLabel,
            R.drawable.function_button_city_background,
            selectedColor,
            unselectedColor
        )

        applyFunctionButtonState(
            !isCurrentSelected,
            binding.btnForecastWeather,
            binding.ivForecastIcon,
            binding.tvForecastLabel,
            R.drawable.function_button_forecast_background,
            selectedColor,
            unselectedColor
        )
    }

    private fun applyFunctionButtonState(
        isSelected: Boolean,
        container: View,
        iconView: ImageView,
        labelView: TextView,
        selectedBackgroundRes: Int,
        selectedColor: Int,
        unselectedColor: Int
    ) {
        val color = if (isSelected) selectedColor else unselectedColor
        container.setBackgroundResource(
            if (isSelected) selectedBackgroundRes else android.R.color.transparent
        )
        labelView.setTextColor(color)
        ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(color))
    }

    private fun observeViewModel() {
        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察错误信息
        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class WeatherPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CurrentWeatherFragment()
                1 -> ForecastWeatherFragment()
                else -> CurrentWeatherFragment()
            }
        }
    }
}

// 当前天气页面
class CurrentWeatherFragment : Fragment() {
    private lateinit var mainActivity: MainActivity
    
    private lateinit var tvCityName: TextView
    private lateinit var tvWeatherStatus: TextView
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvTempRange: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_current_weather, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mainActivity = activity as MainActivity
        
        tvCityName = view.findViewById(R.id.tvCityName)
        tvWeatherStatus = view.findViewById(R.id.tvWeatherStatus)
        tvCurrentTemp = view.findViewById(R.id.tvCurrentTemp)
        tvTempRange = view.findViewById(R.id.tvTempRange)
        tvWeather = view.findViewById(R.id.tvWeather)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvWind = view.findViewById(R.id.tvWind)

        observeViewModel()
    }

    private fun observeViewModel() {
        mainActivity.viewModel.liveWeather.observe(viewLifecycleOwner) { weather ->
            weather?.let { updateUI(it) }
        }

        mainActivity.viewModel.currentCity.observe(viewLifecycleOwner) { cityName ->
            tvCityName.text = cityName
        }

        mainActivity.viewModel.forecastWeather.observe(viewLifecycleOwner) { forecasts ->
            forecasts?.let {
                if (it.isNotEmpty()) {
                    val firstDay = it[0]
                    tvTempRange.text = "最高: ${firstDay.daytemp}° 最低: ${firstDay.nighttemp}°"
                }
            }
        }
    }

    private fun updateUI(weather: LiveWeather) {
        tvWeatherStatus.text = weather.weather
        tvCurrentTemp.text = "${weather.temperature}°"
        tvWeather.text = weather.weather
        tvHumidity.text = "${weather.humidity}%"
        tvWind.text = "${weather.winddirection}风 ${weather.windpower}级"
    }
}

// 预报天气页面
class ForecastWeatherFragment : Fragment() {
    private lateinit var mainActivity: MainActivity
    private lateinit var tvCityNameForecast: TextView
    private lateinit var rvForecast: RecyclerView
    private lateinit var forecastAdapter: ForecastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forecast_weather, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mainActivity = activity as MainActivity
        
        tvCityNameForecast = view.findViewById(R.id.tvCityNameForecast)
        rvForecast = view.findViewById(R.id.rvForecast)

        forecastAdapter = ForecastAdapter(emptyList())
        rvForecast.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = forecastAdapter
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        mainActivity.viewModel.currentCity.observe(viewLifecycleOwner) { cityName ->
            tvCityNameForecast.text = cityName
        }

        mainActivity.viewModel.forecastWeather.observe(viewLifecycleOwner) { forecasts ->
            forecasts?.let {
                forecastAdapter.updateData(it)
            }
        }
    }
}

class WeatherViewModel : ViewModel() {
    private val weatherService = WeatherService()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _liveWeather = MutableLiveData<LiveWeather?>()
    val liveWeather: LiveData<LiveWeather?> = _liveWeather

    private val _forecastWeather = MutableLiveData<List<Cast>?>()
    val forecastWeather: LiveData<List<Cast>?> = _forecastWeather

    private val _currentCity = MutableLiveData<String>()
    val currentCity: LiveData<String> = _currentCity

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadWeather(cityCode: String, cityName: String) {
        _currentCity.value = cityName
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            // 加载实时天气
            val liveResult = weatherService.getLiveWeather(cityCode)
            liveResult.onSuccess { response ->
                _liveWeather.value = response.lives?.firstOrNull()
            }.onFailure { e ->
                _error.value = "获取实时天气失败: ${e.message}"
            }

            // 加载预报天气
            val forecastResult = weatherService.getForecastWeather(cityCode)
            forecastResult.onSuccess { response ->
                _forecastWeather.value = response.forecasts?.firstOrNull()?.casts
            }.onFailure { e ->
                _error.value = "获取预报天气失败: ${e.message}"
            }

            _isLoading.value = false
        }
    }
}
