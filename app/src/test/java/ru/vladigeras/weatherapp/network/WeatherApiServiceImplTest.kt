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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            forecastDays = 3,
            forecastHours = 24
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
            forecastDays = 3,
            forecastHours = 24
        )
    }

    @Test
    fun getWeather_forecastDaysZero_passesZeroParameter() = runTest {
        var paramValue: String? = null
        val mockEngine = MockEngine { request ->
            val url = request.url
            paramValue = url.parameters["forecast_days"]

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
            forecastDays = 0,
            forecastHours = 0
        )

        assertEquals("0", paramValue)
    }

    @Test
    fun getWeather_forecastHoursZero_passesZeroParameter() = runTest {
        var paramValue: String? = null
        val mockEngine = MockEngine { request ->
            val url = request.url
            paramValue = url.parameters["forecast_hours"]

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
            forecastDays = 7,
            forecastHours = 0
        )

        assertEquals("0", paramValue)
    }

    @Test
    fun getWeather_parsesHourlyData() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "latitude": 55.75,
                        "longitude": 37.62,
                        "timezone": "Europe/Moscow",
                        "generationtime_ms": 0.5,
                        "utc_offset_seconds": 10800,
                        "timezone_abbreviation": "MSK",
                        "elevation": 150.0,
                        "current": {
                            "temperature_2m": 25.0,
                            "weathercode": 0,
                            "time": "2026-01-01T12:00",
                            "interval": 900
                        },
                        "hourly": {
                            "time": ["2026-01-01T12:00", "2026-01-01T13:00", "2026-01-01T14:00"],
                            "temperature_2m": [20.0, 21.0, 22.0],
                            "relativehumidity_2m": [65, 70, 75],
                            "windspeed_10m": [5.0, 6.0, 7.0]
                        },
                        "hourly_units": {
                            "temperature_2m": "°C",
                            "relativehumidity_2m": "%",
                            "windspeed_10m": "km/h"
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
            hourlyParams = "temperature_2m,relativehumidity_2m,windspeed_10m",
            dailyParams = "",
            forecastDays = 0,
            forecastHours = 3
        )

        assertNotNull(result.hourly)
        assertEquals(3, result.hourly?.time?.size)
        assertEquals(listOf(20.0, 21.0, 22.0), result.hourly?.temperature2m)
        assertEquals(listOf(65, 70, 75), result.hourly?.relativehumidity2m)
        assertEquals(listOf(5.0, 6.0, 7.0), result.hourly?.windspeed10m)
        assertEquals("°C", result.hourlyUnits?.temperature2mUnit)
    }

    @Test
    fun getWeather_parsesDailyData() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "latitude": 55.75,
                        "longitude": 37.62,
                        "timezone": "Europe/Moscow",
                        "generationtime_ms": 0.5,
                        "utc_offset_seconds": 10800,
                        "timezone_abbreviation": "MSK",
                        "elevation": 150.0,
                        "current": {
                            "temperature_2m": 25.0,
                            "weathercode": 0,
                            "time": "2026-01-01T12:00",
                            "interval": 900
                        },
                        "daily": {
                            "time": ["2026-01-01", "2026-01-02", "2026-01-03"],
                            "weathercode": [0, 1, 2],
                            "temperature_2m_max": [25.0, 26.0, 27.0],
                            "temperature_2m_min": [15.0, 16.0, 17.0],
                            "precipitation_sum": [0.5, 1.0, 0.0],
                            "sunrise": ["08:00", "08:05", "08:10"],
                            "sunset": ["16:30", "16:35", "16:40"],
                            "windspeed_10m_max": [10.0, 12.0, 15.0],
                            "winddirection_10m_dominant": [180, 190, 200],
                            "uv_index_max": [5.0, 6.0, 7.0]
                        },
                        "daily_units": {
                            "time": "",
                            "temperature_2m_max": "°C",
                            "temperature_2m_min": "°C",
                            "precipitation_sum": "mm"
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
            hourlyParams = "",
            dailyParams = "temperature_2m_max,temperature_2m_min,precipitation_sum",
            forecastDays = 3,
            forecastHours = 0
        )

        assertNotNull(result.daily)
        assertEquals(3, result.daily?.time?.size)
        assertEquals(listOf(0, 1, 2), result.daily?.weatherCode)
        assertEquals(listOf(25.0, 26.0, 27.0), result.daily?.temperature2mMax)
        assertEquals(listOf(15.0, 16.0, 17.0), result.daily?.temperature2mMin)
        assertEquals(listOf(0.5, 1.0, 0.0), result.daily?.precipitationSum)
        assertEquals(listOf("08:00", "08:05", "08:10"), result.daily?.sunrise)
        assertEquals(listOf("16:30", "16:35", "16:40"), result.daily?.sunset)
        assertEquals(listOf(10.0, 12.0, 15.0), result.daily?.windspeed10mMax)
        assertEquals(listOf(180, 190, 200), result.daily?.winddirection10mDominant)
        assertEquals(listOf(5.0, 6.0, 7.0), result.daily?.uvIndexMax)
        assertEquals("°C", result.dailyUnits?.temperature2mMaxUnit)
    }
}
