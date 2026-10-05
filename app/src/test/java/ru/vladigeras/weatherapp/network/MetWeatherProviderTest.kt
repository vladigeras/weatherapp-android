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
import java.util.concurrent.ConcurrentLinkedQueue

class MetWeatherProviderTest {
    private val clients = mutableListOf<HttpClient>()
    private val originalZone = TimeZone.getDefault()
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-10-03T17:25:00Z"), ZoneOffset.UTC)
    private val prefs = WeatherDisplayPrefs(provider = WeatherProviderId.YR, showSunTimes = false)

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

    @Test fun `complete alone maps nearest instant real intervals and nine phone days for foreign coordinates`() = runTest {
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
        assertEquals(9, weather.daily.size)
        assertEquals("2026-10-03", weather.daily.first().date)
        assertEquals(-2.0, weather.daily.first().temperatureMin!!, 0.0)
        assertEquals(20.0, weather.daily.first().temperatureMax!!, 0.0)
        assertEquals(WeatherCondition.CLEAR, weather.daily.first().condition)
        assertEquals(setOf(3600L, 21600L), weather.hourly.zipWithNext { a, b -> b.epochSeconds - a.epochSeconds }.toSet())
        val later = Clock.fixed(Instant.parse("2026-10-05T02:25:00Z"), ZoneOffset.UTC)
        val hours = WeatherMapper(mockk(), later).mapToHourlyForecast(weather.hourly, weather.timezone, 48)
        assertEquals(16, hours.size)
        assertEquals("06:00", hours.first().time)
        assertEquals(21600L, hours.last().epochSeconds - hours[hours.lastIndex - 1].epochSeconds)
        assertFalse(adapter.capabilities.effectivePrefs(prefs).showUvIndex)
        assertTrue(weather.daily.all { it.uvIndexMax == null && it.windDirectionDominant == null })
    }

    @Test fun `precipitation requires complete cover and does not add overlapping intervals`() = runTest {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val complete = provider().getWeather(0.0, 0.0, prefs)
        assertEquals(24.0, complete.daily[1].precipitationSum!!, 0.0)
        val dry = provider(fixture().replace("\"precipitation_amount\":1.0", "\"precipitation_amount\":0.0").replace("\"precipitation_amount\":6.0", "\"precipitation_amount\":0.0")).getWeather(0.0, 0.0, prefs)
        assertEquals(0.0, dry.daily[1].precipitationSum!!, 0.0)
        val partial = provider(fixture(hours = (18..60).toList())).getWeather(0.0, 0.0, prefs)
        assertNull(partial.daily.first().precipitationSum)
        val gap = provider(fixture(hours = (0..24).filter { it !in 1..6 }, omitOne = setOf(0))).getWeather(0.0, 0.0, prefs)
        assertNull(gap.daily.first().precipitationSum)
        val alternate = provider(fixture(hours = (0..24).toList(), omitAll = setOf(1), sixRain = 60.0)).getWeather(0.0, 0.0, prefs)
        assertEquals(78.0, alternate.daily.first().precipitationSum!!, 0.0)
    }

    @Test fun `full local days follow daylight saving boundaries and fractional zone gaps stay absent`() = runTest {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        for ((start, expected) in listOf("2026-03-08T05:00:00Z" to 23.0, "2026-11-01T04:00:00Z" to 25.0)) {
            val at = Clock.fixed(Instant.parse(start), ZoneOffset.UTC)
            val weather = provider(fixture(start, (0..30).toList()), at = at).getWeather(0.0, 0.0, prefs.copy(forecastDays = 1))
            assertEquals(expected, weather.daily.single().precipitationSum!!, 0.0)
        }
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kathmandu"))
        assertNull(provider().getWeather(0.0, 0.0, prefs).daily.first().precipitationSum)
    }

    @Test fun `sun events are grouped by actual phone date including adjacent dates and polar absence`() = runTest {
        val calls = ConcurrentLinkedQueue<String>()
        val client = HttpClient(MockEngine { request ->
            val date = request.url.parameters["date"]
            if (date == null) {
                assertEquals(Url(BuildConfig.MET_FORECAST_API_URL).encodedPath, request.url.encodedPath)
                respond(fixture())
            } else {
                assertEquals(Url(BuildConfig.MET_SUN_API_URL).host, request.url.host)
                assertEquals(Url(BuildConfig.MET_SUN_API_URL).encodedPath, request.url.encodedPath)
                assertEquals(setOf("lat", "lon", "date"), request.url.parameters.names())
                calls.add(date)
                respond(when (date) {
                    "2026-10-02" -> """{"properties":{"sunset":{"time":"2026-10-02T22:30Z"}}}"""
                    "2026-10-03" -> """{"properties":{"sunrise":{"time":"2026-10-03T08:15Z"},"sunset":{"time":"2026-10-03T22:30Z"}}}"""
                    else -> """{"properties":{"sunrise":null,"sunset":null}}"""
                })
            }
        }).also(clients::add)
        val adapter = MetWeatherProvider(client, json, clock)
        val weather = adapter.getWeather(40.7, -74.0, prefs.copy(showSunTimes = true, forecastDays = 3))
        assertEquals("11:15", weather.daily[0].sunrise)
        assertEquals("01:30", weather.daily[0].sunset)
        assertNull(weather.daily[1].sunrise)
        assertEquals("01:30", weather.daily[1].sunset)
        assertNull(weather.daily[2].sunrise)
        assertNull(weather.daily[2].sunset)
        assertEquals(setOf("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05", "2026-10-06"), calls.toSet())
        calls.clear()
        val hidden = adapter.getWeather(40.7, -74.0, prefs.copy(showSunTimes = true, showForecastDays = false, showHourlyForecast = false))
        assertTrue(hidden.daily.isEmpty())
        assertTrue(hidden.hourly.isEmpty())
        assertTrue(calls.isEmpty())
    }

    @Test fun `optional data and unknown polar twilight remain absent without formulas`() = runTest {
        val body = """{"properties":{"timeseries":[{"time":"2026-10-03T17:00:00Z","data":{"instant":{"details":{}},"next_6_hours":{"summary":{"symbol_code":"unknown_polartwilight"},"details":{}}}}]}}"""
        val weather = provider(body).getWeather(0.0, 0.0, prefs)
        assertEquals(CurrentWeather(), weather.current)
        assertEquals(ForecastDay("2026-10-03"), weather.daily.single())
        assertNull(weather.hourly.single().temperature)
        val hidden = provider().getWeather(0.0, 0.0, prefs.copy(showWind = false, showPrecipitation = false))
        assertTrue(hidden.daily.all { it.windSpeedMax == null && it.precipitationSum == null })
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
        val client = HttpClient(MockEngine { request -> if (request.url.parameters["date"] == null) respond(fixture()) else respond("Unavailable", HttpStatusCode.ServiceUnavailable) }).also(clients::add)
        assertTrue(runCatching { MetWeatherProvider(client, json, clock).getWeather(0.0, 0.0, prefs.copy(showSunTimes = true)) }.exceptionOrNull() is ResponseException)
    }

    @Test fun `cancellation stops pending sun children without another provider`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine { request ->
            if (request.url.parameters["date"] == null) respond(fixture()) else {
                assertEquals(Url(BuildConfig.MET_SUN_API_URL).host, request.url.host)
                entered.complete(Unit)
                awaitCancellation()
            }
        }).also(clients::add)
        val job = async { MetWeatherProvider(client, json, clock).getWeather(0.0, 0.0, prefs.copy(showSunTimes = true)) }
        entered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    companion object {
        fun fixture(start: String = "2026-10-03T00:00:00Z", hours: List<Int> = (0..60).toList() + (66..240 step 6).toList(), omitOne: Set<Int> = emptySet(), sixRain: Double = 6.0, omitAll: Set<Int> = emptySet()): String {
            val points = hours.joinToString(",") { hour ->
                val one = if (hour <= 60 && hour !in omitOne && hour !in omitAll) """, "next_1_hours":{"summary":{"symbol_code":"${if (hour == 9) "clearsky_day" else "rain_day"}"},"details":{"precipitation_amount":1.0}}""" else ""
                val six = if (hour in omitAll) "" else """, "next_6_hours":{"summary":{"symbol_code":"rain_day"},"details":{"precipitation_amount":$sixRain,"air_temperature_min":${hour - 2},"air_temperature_max":${hour + 2}}}"""
                """{"time":"${Instant.parse(start).plusSeconds(hour * 3600L)}","data":{"instant":{"details":{"air_temperature":$hour,"apparent_air_temperature":${hour - 1},"relative_humidity":70.4,"wind_speed":2.0,"ultraviolet_index_clear_sky":9.0}}$one$six}}"""
            }
            return """{"properties":{"timeseries":[$points]}}"""
        }
    }
}
