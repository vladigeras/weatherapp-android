package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vladigeras.weatherapp.data.WeatherResponse

class WeatherApiServiceImplTest {

    private fun createService(mockEngine: MockEngine): WeatherApiService {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return WeatherApiServiceImpl(httpClient)
    }

    @Test
    fun getWeather_buildsCorrectUrlAndParsesResponse() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url
            assertEquals("55.75", url.parameters["latitude"])
            assertEquals("37.62", url.parameters["longitude"])
            assertEquals("temperature_2m", url.parameters["current"])
            assertEquals("temperature_2m", url.parameters["hourly"])
            assertEquals("temperature_2m_max,temperature_2m_min", url.parameters["daily"])
            assertEquals("3", url.parameters["forecast_days"])
            assertEquals("auto", url.parameters["timezone"])
            
            respond(
                content = """
                    {
                        "latitude": 55.75,
                        "longitude": 37.62,
                        "timezone": "Europe/Moscow",
                        "elevation": 150.0,
                        "generationtime_ms": 0.5,
                        "utc_offset_seconds": 10800,
                        "timezone_abbreviation": "MSK",
                        "current": {
                            "temperature_2m": 25.0,
                            "weathercode": 0,
                            "time": "2026-01-01T12:00",
                            "interval": 900,
                            "apparent_temperature": 24.0,
                            "windspeed_10m": 10.0,
                            "winddirection_10m": 180,
                            "is_day": 1
                        }
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        
        val service = createService(mockEngine)
        val result = service.getWeather(
            latitude = 55.75,
            longitude = 37.62,
            currentParams = "temperature_2m",
            hourlyParams = "temperature_2m",
            dailyParams = "temperature_2m_max,temperature_2m_min",
            forecastDays = 3
        )
        
        assertEquals(55.75, result.latitude, 0.01)
        assertEquals(37.62, result.longitude, 0.01)
        assertEquals(25.0, result.current?.temperature ?: 0.0, 0.01)
    }

    @Test(expected = Exception::class)
    fun getWeather_invalidResponse_throwsException() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "invalid json",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        
        val service = createService(mockEngine)
        service.getWeather(
            latitude = 55.75,
            longitude = 37.62,
            currentParams = "temperature_2m",
            hourlyParams = "temperature_2m",
            dailyParams = "temperature_2m_max,temperature_2m_min",
            forecastDays = 3
        )
    }

    @Test
    fun getWeather_forecastDaysZero_usesDefault() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url
            assertEquals("5", url.parameters["forecast_days"])
            
            respond(
                content = """
                    {
                        "latitude": 55.75,
                        "longitude": 37.62,
                        "timezone": "Europe/Moscow",
                        "elevation": 150.0,
                        "generationtime_ms": 0.5,
                        "utc_offset_seconds": 10800,
                        "timezone_abbreviation": "MSK"
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        
        val service = createService(mockEngine)
        service.getWeather(
            latitude = 55.75,
            longitude = 37.62,
            currentParams = "temperature_2m",
            hourlyParams = "temperature_2m",
            dailyParams = "temperature_2m_max,temperature_2m_min",
            forecastDays = 5
        )
    }
}
