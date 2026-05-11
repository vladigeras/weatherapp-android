package ru.vladigeras.weatherapp.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs

class WeatherParamsBuilderTest {

    private val builder = WeatherParamsBuilder()

    @Test
    fun `build includes base params always`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showPrecipitation = false,
            showSunTimes = false,
            showUvIndex = false,
            showForecastDays = false,
            showHourlyForecast = false
        )

        val (current, hourly, daily) = builder.build(prefs)

        assertTrue(current.contains("temperature_2m"))
        assertTrue(current.contains("is_day"))
        assertTrue(current.contains("weathercode"))
        assertTrue(current.contains("apparent_temperature"))
        assertTrue(hourly.contains("temperature_2m"))
        assertTrue(daily.contains("temperature_2m_max"))
        assertTrue(daily.contains("temperature_2m_min"))
        assertTrue(daily.contains("weathercode"))
    }

    @Test
    fun `build excludes humidity when showHumidity is false`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showHourlyForecast = true,
            hourlyForecastHours = 24
        )

        val (current, hourly, daily) = builder.build(prefs)

        assertFalse(hourly.contains("relativehumidity_2m"))
    }

    @Test
    fun `build excludes humidity when showHumidity is false even with hourly forecast enabled`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showHourlyForecast = true,
            hourlyForecastHours = 48
        )

        val (_, hourly, _) = builder.build(prefs)

        assertFalse(hourly.contains("relativehumidity_2m"))
    }

    @Test
    fun `build includes humidity when showHumidity is true`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = true,
            showWind = false,
            showHourlyForecast = false
        )

        val (_, hourly, _) = builder.build(prefs)

        assertTrue(hourly.contains("relativehumidity_2m"))
    }

    @Test
    fun `build excludes wind when showWind is false`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showHourlyForecast = true,
            hourlyForecastHours = 24
        )

        val (current, hourly, daily) = builder.build(prefs)

        assertFalse(current.contains("windspeed_10m"))
        assertFalse(hourly.contains("windspeed_10m"))
        assertFalse(daily.contains("windspeed_10m_max"))
        assertFalse(daily.contains("winddirection_10m_dominant"))
    }

    @Test
    fun `build excludes wind when showWind is false even with hourly forecast enabled`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showHourlyForecast = true,
            hourlyForecastHours = 48
        )

        val (current, hourly, daily) = builder.build(prefs)

        assertFalse(current.contains("windspeed_10m"))
        assertFalse(hourly.contains("windspeed_10m"))
        assertFalse(daily.contains("windspeed_10m_max"))
        assertFalse(daily.contains("winddirection_10m_dominant"))
    }

    @Test
    fun `build includes wind when showWind is true`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = true,
            showHourlyForecast = false
        )

        val (current, hourly, daily) = builder.build(prefs)

        assertTrue(current.contains("windspeed_10m"))
        assertTrue(hourly.contains("windspeed_10m"))
        assertTrue(daily.contains("windspeed_10m_max"))
        assertTrue(daily.contains("winddirection_10m_dominant"))
    }

    @Test
    fun `build excludes precipitation when showPrecipitation is false`() {
        val prefs = WeatherDisplayPrefs(
            showPrecipitation = false,
            showForecastDays = true,
            forecastDays = 7
        )

        val (_, _, daily) = builder.build(prefs)

        assertFalse(daily.contains("precipitation_sum"))
    }

    @Test
    fun `build includes precipitation when showPrecipitation is true`() {
        val prefs = WeatherDisplayPrefs(
            showPrecipitation = true,
            showForecastDays = true,
            forecastDays = 7
        )

        val (_, _, daily) = builder.build(prefs)

        assertTrue(daily.contains("precipitation_sum"))
    }

    @Test
    fun `build excludes sun times when showSunTimes is false`() {
        val prefs = WeatherDisplayPrefs(
            showSunTimes = false,
            showForecastDays = true,
            forecastDays = 7
        )

        val (_, _, daily) = builder.build(prefs)

        assertFalse(daily.contains("sunrise"))
        assertFalse(daily.contains("sunset"))
    }

    @Test
    fun `build includes sun times when showSunTimes is true`() {
        val prefs = WeatherDisplayPrefs(
            showSunTimes = true,
            showForecastDays = true,
            forecastDays = 7
        )

        val (_, _, daily) = builder.build(prefs)

        assertTrue(daily.contains("sunrise"))
        assertTrue(daily.contains("sunset"))
    }

    @Test
    fun `build excludes UV index when showUvIndex is false`() {
        val prefs = WeatherDisplayPrefs(
            showUvIndex = false,
            showForecastDays = true,
            forecastDays = 7
        )

        val (_, _, daily) = builder.build(prefs)

        assertFalse(daily.contains("uv_index_max"))
    }

    @Test
    fun `build includes UV index when showUvIndex is true`() {
        val prefs = WeatherDisplayPrefs(
            showUvIndex = true,
            showForecastDays = true,
            forecastDays = 7
        )

        val (_, _, daily) = builder.build(prefs)

        assertTrue(daily.contains("uv_index_max"))
    }

    @Test
    fun `build does not duplicate params with distinct`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = true,
            showWind = true,
            showHourlyForecast = true,
            hourlyForecastHours = 24
        )

        val (_, hourly, _) = builder.build(prefs)

        val humidityCount = hourly.split("relativehumidity_2m").size - 1
        val windCount = hourly.split("windspeed_10m").size - 1
        assertEquals(1, humidityCount)
        assertEquals(1, windCount)
    }

    @Test
    fun `build respects all prefs false`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showPrecipitation = false,
            showSunTimes = false,
            showUvIndex = false,
            showForecastDays = false,
            showHourlyForecast = false
        )

        val (current, hourly, daily) = builder.build(prefs)

        assertFalse(current.contains("windspeed_10m"))
        assertFalse(hourly.contains("relativehumidity_2m"))
        assertFalse(hourly.contains("windspeed_10m"))
        assertFalse(daily.contains("precipitation_sum"))
        assertFalse(daily.contains("sunrise"))
        assertFalse(daily.contains("sunset"))
        assertFalse(daily.contains("uv_index_max"))
    }

    @Test
    fun `build includes weathercode in daily when showHourlyForecast is true`() {
        val prefs = WeatherDisplayPrefs(
            showHumidity = false,
            showWind = false,
            showHourlyForecast = true,
            hourlyForecastHours = 24
        )

        val (_, _, daily) = builder.build(prefs)

        assertTrue(daily.contains("weathercode"))
    }

    @Test
    fun `build returns comma-separated params`() {
        val prefs = WeatherDisplayPrefs()

        val (current, hourly, daily) = builder.build(prefs)

        assertTrue(current.contains(","))
        assertTrue(hourly.contains(","))
        assertTrue(daily.contains(","))
    }
}