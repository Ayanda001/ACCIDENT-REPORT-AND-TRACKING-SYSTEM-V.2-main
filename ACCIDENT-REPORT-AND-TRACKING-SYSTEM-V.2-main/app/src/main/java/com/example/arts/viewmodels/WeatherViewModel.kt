package com.example.arts.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arts.network.CurrentWeather
import com.example.arts.network.RetrofitClient
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Success(val weather: CurrentWeather) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>(WeatherState.Idle)
    val weatherState: LiveData<WeatherState> = _weatherState

    /** Fetches the current weather for a given incident's coordinates. */
    fun loadWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val response = RetrofitClient.openMeteoApi.getCurrentWeather(latitude, longitude)
                val current = response.currentWeather
                if (current != null) {
                    _weatherState.value = WeatherState.Success(current)
                } else {
                    _weatherState.value = WeatherState.Error("No weather data available")
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error("Couldn't load weather: ${e.message}")
            }
        }
    }
}
