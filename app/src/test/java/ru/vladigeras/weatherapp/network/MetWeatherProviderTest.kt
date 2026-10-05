package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.*
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.*
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone

class MetWeatherProviderTest {
    private val clients = mutableListOf<HttpClient>()
    private val originalZone = TimeZone.getDefault()
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-10-03T17:25:00Z"), ZoneOffset.UTC)
    private val prefs = WeatherDisplayPrefs(provider = WeatherProviderId.YR)

    @Before fun setup() { TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow")) }
    @After fun tearDown() = runBlocking {
        try {
            val jobs = clients.map { it.coroutineContext[Job]!! }
            clients.forEach { it.close() }
            jobs.joinAll()
        } finally { TimeZone.setDefault(originalZone) }
    }

    private fun provider(body: String = fixture(), status: HttpStatusCode = HttpStatusCode.OK, at: Clock = clock) =
        MetWeatherProvider(HttpClient(MockEngine { respond(body, status) }).also(clients::add), json, at)

    @Test fun `complete maps nearest ready values and real intervals without daily calculations`() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals(Url(BuildConfig.MET_FORECAST_API_URL).host, request.url.host)
            assertEquals(Url(BuildConfig.MET_FORECAST_API_URL).encodedPath, request.url.encodedPath)
            assertEquals(setOf("lat", "lon"), request.url.parameters.names())
            assertEquals("40.7127", request.url.parameters["lat"])
            assertEquals("-74.0069", request.url.parameters["lon"])
            respond(fixture())
        }).also(clients::add)
        val adapter = MetWeatherProvider(client, json, clock)
        val weather = adapter.getWeather(40.712799, -74.006999, prefs.copy(forecastDays = 16))
        assertEquals(1, calls)
        assertEquals(WeatherProviderId.YR, weather.provider)
        assertEquals("Europe/Moscow", weather.timezone)
        assertEquals(CurrentWeather(17.0, 16.0, 70, 7.2, WeatherCondition.RAIN_UNSPECIFIED, 1), weather.current)
        assertTrue(weather.daily.isEmpty())
        assertEquals(setOf(3600L, 21600L), weather.hourly.zipWithNext { a, b -> b.epochSeconds - a.epochSeconds }.toSet())
        val later = Clock.fixed(Instant.parse("2026-10-05T02:25:00Z"), ZoneOffset.UTC)
        val hours = WeatherMapper(mockk(), later).mapToHourlyForecast(weather.hourly, weather.timezone, 48)
        assertEquals(16, hours.size)
        assertEquals("06:00", hours.first().time)
        assertEquals(21600L, hours.last().epochSeconds - hours[hours.lastIndex - 1].epochSeconds)
        val effective = adapter.capabilities.effectivePrefs(prefs)
        assertEquals(0, effective.forecastDays)
        assertFalse(effective.showForecastDays)
        assertFalse(effective.showPrecipitation)
        assertFalse(effective.showSunTimes)
        assertFalse(effective.showUvIndex)
        assertTrue(effective.showWind)
        assertFalse(adapter.capabilities.dailyWind)
    }

    @Test fun `phone time formatting keeps real points in fractional and foreign zones`() = runTest {
        for ((zone, expected) in listOf("Asia/Kathmandu" to "23:45", "America/New_York" to "14:00")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            val weather = provider(fixture(hours = listOf(17, 18))).getWeather(0.0, 0.0, prefs)
            assertTrue(weather.daily.isEmpty())
            val hours = WeatherMapper(mockk(), clock).mapToHourlyForecast(weather.hourly, weather.timezone, 12)
            assertEquals(expected, hours.single().time)
            assertEquals(Instant.parse("2026-10-03T18:00:00Z").epochSecond, hours.single().epochSeconds)
        }
    }

    @Test fun `optional data and unknown polar twilight remain absent without formulas`() = runTest {
        val body = """{"properties":{"timeseries":[{"time":"2026-10-03T17:00:00Z","data":{"instant":{"details":{}},"next_6_hours":{"summary":{"symbol_code":"unknown_polartwilight"},"details":{}}}}]}}"""
        val weather = provider(body).getWeather(0.0, 0.0, prefs)
        assertEquals(CurrentWeather(), weather.current)
        assertTrue(weather.daily.isEmpty())
        assertNull(weather.hourly.single().temperature)
        val hidden = provider().getWeather(0.0, 0.0, prefs.copy(showForecastDays = false, showHourlyForecast = false))
        assertTrue(hidden.daily.isEmpty())
        assertTrue(hidden.hourly.isEmpty())
        assertNotNull(hidden.current.temperature)
    }

    @Test fun `documented symbols preserve precipitation type and intensity without inventing unknown conditions`() {
        val adapter = provider()
        val cases = mapOf("clearsky" to WeatherCondition.CLEAR, "fair" to WeatherCondition.MOSTLY_CLEAR,
            "partlycloudy" to WeatherCondition.PARTLY_CLOUDY, "cloudy" to WeatherCondition.OVERCAST, "fog" to WeatherCondition.FOG,
            "lightrain" to WeatherCondition.LIGHT_RAIN, "rain" to WeatherCondition.RAIN_UNSPECIFIED, "heavyrain" to WeatherCondition.HEAVY_RAIN,
            "lightrainshowers" to WeatherCondition.LIGHT_SHOWERS, "rainshowers" to WeatherCondition.RAIN_UNSPECIFIED, "heavyrainshowers" to WeatherCondition.HEAVY_SHOWERS,
            "lightsnow" to WeatherCondition.LIGHT_SNOW, "snow" to WeatherCondition.SNOW_UNSPECIFIED, "heavysnow" to WeatherCondition.HEAVY_SNOW,
            "lightsnowshowers" to WeatherCondition.SNOW_SHOWERS, "snowshowers" to WeatherCondition.SNOW_UNSPECIFIED, "heavysnowshowers" to WeatherCondition.HEAVY_SNOW_SHOWERS)
        cases.forEach { (code, expected) -> listOf("day", "night", "polartwilight").forEach { assertEquals(expected, adapter.condition("${code}_$it")) } }
        for (code in listOf("lightsleet", "sleet", "heavysleet", "lightsleetshowers", "sleetshowers", "heavysleetshowers")) assertEquals(WeatherCondition.SLEET, adapter.condition(code))
        for (code in cases.keys.filter { "rain" in it || "snow" in it } + listOf("lightsleet", "sleet", "heavysleet", "lightssleetshowers", "sleetshowers", "heavysleetshowers", "lightssnowshowers")) {
            val thunder = if (code == "lightsnowshowers") "lightssnowshowers" else code
            assertEquals(WeatherCondition.THUNDERSTORM, adapter.condition("${thunder}andthunder_night"))
        }
        assertNull(adapter.condition("unknownandthunder_day"))
        assertNull(adapter.condition(null))
    }

    @Test fun `HTTP malformed and empty forecasts fail without fallback`() = runTest {
        for ((body, status) in listOf("Unavailable" to HttpStatusCode.ServiceUnavailable, "{" to HttpStatusCode.OK,
            "{}" to HttpStatusCode.OK, """{"properties":{"timeseries":[]}}""" to HttpStatusCode.OK)) {
            val result = runCatching { provider(body, status).getWeather(0.0, 0.0, prefs) }
            assertTrue(result.isFailure)
            if (status == HttpStatusCode.ServiceUnavailable) assertTrue(result.exceptionOrNull() is ResponseException)
        }
    }

    @Test fun `cancellation stops forecast request without another request`() = runTest {
        val entered = CompletableDeferred<Unit>()
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals(Url(BuildConfig.MET_FORECAST_API_URL).host, request.url.host)
            entered.complete(Unit)
            awaitCancellation()
        }).also(clients::add)
        val job = async { MetWeatherProvider(client, json, clock).getWeather(0.0, 0.0, prefs) }
        entered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(1, calls)
    }

    companion object {
        fun fixture(start: String = "2026-10-03T00:00:00Z", hours: List<Int> = (0..60).toList() + (66..240 step 6).toList()): String {
            val points = hours.joinToString(",") { hour ->
                val one = if (hour <= 60) """, "next_1_hours":{"summary":{"symbol_code":"${if (hour == 9) "clearsky_day" else "rain_day"}"},"details":{"precipitation_amount":1.0}}""" else ""
                val six = """, "next_6_hours":{"summary":{"symbol_code":"rain_day"},"details":{"precipitation_amount":123.0,"air_temperature_min":-999.0,"air_temperature_max":999.0}}"""
                """{"time":"${Instant.parse(start).plusSeconds(hour * 3600L)}","data":{"instant":{"details":{"air_temperature":$hour,"apparent_air_temperature":${hour - 1},"relative_humidity":70.4,"wind_speed":2.0,"ultraviolet_index_clear_sky":9.0}}$one$six}}"""
            }
            return """{"properties":{"timeseries":[$points]}}"""
        }
    }
}
