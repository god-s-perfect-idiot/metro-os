package com.metro.calendar.data

/**
 * Lightweight day-header weather for Calendar (WP8.1 showed temp + glyph on day/week).
 * Stubbed locally until a suite Weather app exists — stable per [epochDay] so swipe is calm.
 */
enum class WeatherCondition {
    Sunny,
    Cloudy,
    Rain,
    Storm,
}

data class DayWeather(
    val tempCelsius: Int,
    val condition: WeatherCondition,
) {
    fun temperatureLabel(useFahrenheit: Boolean): String {
        val value = if (useFahrenheit) celsiusToFahrenheit(tempCelsius) else tempCelsius
        return "$value°"
    }

    companion object {
        fun forEpochDay(epochDay: Long): DayWeather {
            val seed = (epochDay * 37L + 17L).toInt()
            val tempC = 8 + ((seed ushr 3) % 23) // 8–30°C
            val condition = when ((seed ushr 1) % 4) {
                0 -> WeatherCondition.Sunny
                1 -> WeatherCondition.Cloudy
                2 -> WeatherCondition.Rain
                else -> WeatherCondition.Storm
            }
            return DayWeather(tempCelsius = tempC, condition = condition)
        }

        fun celsiusToFahrenheit(celsius: Int): Int =
            ((celsius * 9) / 5) + 32
    }
}
