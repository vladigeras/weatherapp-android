package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherCondition
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.repository.WeatherParamsBuilder

class OpenMeteoWeatherProviderTest {
    @Test
    fun `current humidity and hourly conditions keep their own sources for all forecast switches`() = runTest {
        for (days in listOf(false, true)) for (hours in listOf(false, true)) {
            val client = HttpClient(MockEngine { request ->
                assertTrue(request.url.parameters["current"]!!.split(',').contains("relativehumidity_2m"))
                assertTrue(request.url.parameters["hourly"]!!.split(',').contains("weathercode"))
                assertEquals(if (days) "7" else "0", request.url.parameters["forecast_days"])
                assertEquals(if (hours) "13" else "0", request.url.parameters["forecast_hours"])
                respond("""{
                    "latitude":55.7,"longitude":37.6,"generationtime_ms":0.1,"utc_offset_seconds":10800,"timezone":"Europe/Moscow","elevation":100,
                    "current":{"temperature_2m":18,"relativehumidity_2m":73,"weathercode":0},
                    "hourly":{"time":["2026-10-04T15:00","2026-10-04T16:00"],"temperature_2m":[19,20],"relativehumidity_2m":[15,16],"weathercode":[61,0]},
                    "daily":{"time":["2026-10-04"],"weathercode":[3]}
                }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            try {
                val weather = OpenMeteoWeatherProvider(WeatherApiServiceImpl(client), mockk(), WeatherParamsBuilder())
                    .getWeather(55.7, 37.6, WeatherDisplayPrefs(showForecastDays = days, showHourlyForecast = hours))
                assertEquals(73, weather.current.humidity)
                assertEquals(WeatherCondition.CLEAR, weather.current.condition)
                assertEquals(if (days) listOf(WeatherCondition.OVERCAST) else emptyList<WeatherCondition>(), weather.daily.map { it.condition })
                assertEquals(if (hours) listOf(WeatherCondition.LIGHT_RAIN, WeatherCondition.CLEAR) else emptyList<WeatherCondition>(), weather.hourly.map { it.condition })
            } finally {
                client.close()
            }
        }
    }
}
