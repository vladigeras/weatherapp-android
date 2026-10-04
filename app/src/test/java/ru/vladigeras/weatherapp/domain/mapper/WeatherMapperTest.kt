package ru.vladigeras.weatherapp.domain.mapper

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import ru.vladigeras.weatherapp.data.*
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.util.asProviderWeather
import java.time.Instant
import java.time.Clock
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class WeatherMapperTest {
    private val language = mockk<LanguagePreferenceRepository>()
    private val mapper = WeatherMapper(language, Clock.fixed(Instant.parse("2026-04-27T00:00:00Z"), ZoneOffset.UTC))

    @Test
    fun `daily values and local astronomy survive normalization and formatting`() = runTest {
        coEvery { language.getAppLocale() } returns Locale.ENGLISH
        val response = rawWeather("Europe/Moscow", 10800).asProviderWeather()
        val forecast = mapper.mapToDailyForecast(response.daily).single()
        assertEquals("27 Apr", forecast.date)
        assertEquals("Mon", forecast.dayName)
        assertEquals(0, forecast.weatherCode)
        assertEquals(15.0, forecast.temperatureMin!!, 0.001)
        assertEquals(25.0, forecast.temperatureMax!!, 0.001)
        assertEquals(2.5, forecast.precipitationSum!!, 0.001)
        assertEquals("04:30", forecast.sunrise)
        assertEquals("20:15", forecast.sunset)
        assertEquals(10.0, forecast.windSpeedMax!!, 0.001)
        assertEquals(180, forecast.windDirectionDominant)
        assertEquals(5.0, forecast.uvIndexMax!!, 0.001)
        coEvery { language.getAppLocale() } returns Locale.forLanguageTag("ru-RU")
        val russian = mapper.mapToDailyForecast(response.daily).single()
        assertEquals("27 Апр.", russian.date)
        assertEquals("Пн", russian.dayName)
    }

    @Test
    fun `missing values remain absent`() = runTest {
        coEvery { language.getAppLocale() } returns Locale.ENGLISH
        val forecast = mapper.mapToDailyForecast(listOf(ForecastDay("2026-04-27"))).single()
        assertNull(forecast.weatherCode)
        assertNull(forecast.temperatureMin)
        assertNull(forecast.temperatureMax)
        assertNull(forecast.precipitationSum)
        assertNull(forecast.sunrise)
        assertNull(forecast.sunset)
        assertNull(forecast.windSpeedMax)
        assertNull(forecast.windDirectionDominant)
        assertNull(forecast.uvIndexMax)
        val current = rawWeather("Europe/Moscow", 10800).copy(current = null, hourly = null).asProviderWeather().current
        assertEquals(CurrentWeather(), current)
    }

    @Test
    fun `local hourly time is not offset twice in eastern and western zones`() {
        for ((zone, offset) in listOf("Europe/Moscow" to 10800, "America/New_York" to -14400)) {
            val response = rawWeather(zone, offset).asProviderWeather()
            val hourly = mapper.mapToHourlyForecast(response.hourly, response.timezone, 24).single()
            assertEquals("10:00", hourly.time)
            assertEquals(LocalDateTime.parse("2026-04-27T10:00").atZone(ZoneId.of(zone)).toEpochSecond(), hourly.epochSeconds)
            assertEquals(20.0, hourly.temperature!!, 0.001)
            assertEquals(65, hourly.humidity)
            assertEquals(10.0, hourly.windSpeed!!, 0.001)
            assertEquals(0, hourly.weatherCode)
        }
    }

    @Test
    fun `48 hours have distinct keys even with repeated clock times`() {
        val start = Instant.parse("2026-04-27T00:00:00Z").epochSecond
        val hourly = (0 until 48).map { ForecastHour(start + it * 3600L) }
        val result = mapper.mapToHourlyForecast(hourly, "Europe/Moscow", 48)
        assertEquals(48, result.map { it.epochSeconds }.toSet().size)
        assertEquals(result[0].time, result[24].time)
    }

    @Test
    fun `hidden groups are absent while stored preferences are retained`() {
        val prefs = WeatherDisplayPrefs(showForecastDays = false, showHourlyForecast = false)
        val response = rawWeather("Europe/Moscow", 10800).asProviderWeather(prefs)
        assertTrue(response.daily.isEmpty())
        assertTrue(response.hourly.isEmpty())
        assertEquals(7, prefs.forecastDays)
    }

    @Test
    fun `empty hourly forecast does not require timezone`() {
        assertTrue(mapper.mapToHourlyForecast(emptyList(), null, 12).isEmpty())
    }

    private fun rawWeather(zone: String, offset: Int) = WeatherResponse(
        latitude = 55.7, longitude = 37.6, generationtimeMs = 0.1, utcOffsetSeconds = offset,
        timezone = zone, elevation = 0.0,
        hourly = HourlyWeather(listOf("2026-04-27T10:00"), listOf(20.0), listOf(65), listOf(10.0), weatherCode = listOf(0)),
        daily = DailyWeather(
            time = listOf("2026-04-27"), weatherCode = listOf(0), temperature2mMin = listOf(15.0),
            temperature2mMax = listOf(25.0), precipitationSum = listOf(2.5),
            sunrise = listOf("2026-04-27T04:30"), sunset = listOf("2026-04-27T20:15"),
            windspeed10mMax = listOf(10.0), winddirection10mDominant = listOf(180), uvIndexMax = listOf(5.0)
        )
    )
}
