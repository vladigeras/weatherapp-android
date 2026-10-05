package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.*
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
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

class SevenTimerWeatherProviderTest {
    private val clients = mutableListOf<HttpClient>()
    private val originalZone = TimeZone.getDefault()
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = WeatherDisplayPrefs(provider = WeatherProviderId.SEVEN_TIMER)
    private val clock = Clock.fixed(Instant.parse("2026-10-03T17:25:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"))
    }

    @After
    fun tearDown() = runBlocking {
        try {
            val jobs = clients.map { it.coroutineContext[Job]!! }
            clients.forEach { it.close() }
            jobs.joinAll()
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }

    private fun provider(body: String = fixture(), status: HttpStatusCode = HttpStatusCode.OK, at: Clock = clock): SevenTimerWeatherProvider {
        val client = HttpClient(MockEngine { respond(body, status) }).also(clients::add)
        return SevenTimerWeatherProvider(client, json, at)
    }

    @Test
    fun `civil alone maps nearest weather and forecasts in phone zone for foreign coordinates`() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals(Url(BuildConfig.SEVEN_TIMER_API_URL).host, request.url.host)
            assertEquals(Url(BuildConfig.SEVEN_TIMER_API_URL).encodedPath, request.url.encodedPath)
            assertEquals(setOf("lat", "lon", "product", "output", "unit"), request.url.parameters.names())
            assertEquals("40.7128", request.url.parameters["lat"])
            assertEquals("-74.006", request.url.parameters["lon"])
            assertEquals("civil", request.url.parameters["product"])
            assertEquals("json", request.url.parameters["output"])
            assertEquals("metric", request.url.parameters["unit"])
            respond(fixture())
        }).also(clients::add)
        val adapter = SevenTimerWeatherProvider(client, json, clock)
        val weather = adapter.getWeather(40.7128, -74.006, prefs.copy(forecastDays = 16))
        assertEquals(1, calls)
        assertEquals(WeatherProviderId.SEVEN_TIMER, weather.provider)
        assertEquals("Europe/Moscow", weather.timezone)
        assertEquals(CurrentWeather(temperature = 18.0, humidity = 70, condition = WeatherCondition.CLEAR, isDay = 0), weather.current)
        assertEquals(7, weather.daily.size)
        assertEquals("2026-10-03", weather.daily.first().date)
        assertEquals(3.0, weather.daily.first().temperatureMin!!, 0.0)
        assertEquals(18.0, weather.daily.first().temperatureMax!!, 0.0)
        assertEquals(WeatherCondition.CLEAR, weather.daily.first().condition)
        assertEquals(0.0, weather.daily[1].temperatureMin!!, 0.0)
        assertEquals(21.0, weather.daily[1].temperatureMax!!, 0.0)
        assertEquals(64, weather.hourly.size)
        assertEquals(Instant.parse("2026-10-03T03:00:00Z").epochSecond, weather.hourly.first().epochSeconds)
        assertEquals(10800L, weather.hourly[1].epochSeconds - weather.hourly[0].epochSeconds)
        val mapper = WeatherMapper(mockk(), clock)
        assertEquals(listOf("21:00", "00:00", "03:00", "06:00"), mapper.mapToHourlyForecast(weather.hourly, weather.timezone, 12).map { it.time })
        assertEquals(8, mapper.mapToHourlyForecast(weather.hourly, weather.timezone, 24).size)
        assertEquals(16, mapper.mapToHourlyForecast(weather.hourly, weather.timezone, 48).size)
        weather.daily.forEach {
            assertNull(it.precipitationSum)
            assertNull(it.uvIndexMax)
            assertNull(it.windSpeedMax)
            assertNull(it.windDirectionDominant)
            assertNull(it.sunrise)
            assertNull(it.sunset)
        }
        assertTrue(weather.hourly.all { it.windSpeed == null })
        assertEquals(3, adapter.capabilities.hourlyStepHours)
        assertTrue(adapter.capabilities.dayNight)
        val effective = adapter.capabilities.effectivePrefs(prefs)
        assertFalse(effective.showWind)
        assertFalse(effective.showSunTimes)
        assertFalse(effective.showPrecipitation)
        assertFalse(effective.showUvIndex)
    }

    @Test
    fun `daily boundaries and future hours follow phone zone across daylight saving change`() = runTest {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val at = Clock.fixed(Instant.parse("2026-11-01T04:00:00Z"), ZoneOffset.UTC)
        val body = """{"init":"2026110100","dataseries":[
            {"timepoint":3,"temp2m":99,"weather":"clearnight"},
            {"timepoint":6,"temp2m":5,"weather":"clearnight"},
            {"timepoint":9,"temp2m":8,"weather":"clearday"}]}"""
        val weather = provider(body, at = at).getWeather(55.7, 37.6, prefs)
        assertEquals("America/New_York", weather.timezone)
        assertEquals("2026-11-01", weather.daily.single().date)
        assertEquals(5.0, weather.daily.single().temperatureMin!!, 0.0)
        assertEquals(8.0, weather.daily.single().temperatureMax!!, 0.0)
        val hours = WeatherMapper(mockk(), at).mapToHourlyForecast(weather.hourly, weather.timezone, 12)
        assertEquals(listOf("01:00", "04:00"), hours.map { it.time })
        assertEquals(10800L, hours[1].epochSeconds - hours[0].epochSeconds)
    }

    @Test
    fun `disabled forecasts stay absent and selected day count is respected`() = runTest {
        val adapter = provider()
        val hidden = adapter.getWeather(55.7, 37.6, prefs.copy(showHourlyForecast = false, showForecastDays = false))
        assertTrue(hidden.hourly.isEmpty())
        assertTrue(hidden.daily.isEmpty())
        assertNotNull(hidden.current.temperature)
        assertEquals(2, adapter.getWeather(55.7, 37.6, prefs.copy(forecastDays = 2)).daily.size)
    }

    @Test
    fun `sentinels missing optional values and unknown codes remain absent`() = runTest {
        val body = """{"init":"2026100300","dataseries":[{"timepoint":18,"temp2m":-9999,"rh2m":"-9999","weather":"unknown","wind10m":{"speed":8},"prec_amount":9},{"timepoint":21}]}"""
        val weather = provider(body).getWeather(55.7, 37.6, prefs)
        assertEquals(CurrentWeather(), weather.current)
        assertEquals(ForecastDay("2026-10-03"), weather.daily.first())
        assertTrue(weather.hourly.all { it.temperature == null && it.humidity == null && it.condition == null })
    }

    @Test
    fun `all documented civil day and night codes map without inventing unknown conditions`() {
        val adapter = provider()
        val codes = mapOf(
            "clear" to WeatherCondition.CLEAR, "pcloudy" to WeatherCondition.PARTLY_CLOUDY,
            "mcloudy" to WeatherCondition.CLOUDY, "cloudy" to WeatherCondition.OVERCAST,
            "humid" to WeatherCondition.FOG, "lightrain" to WeatherCondition.LIGHT_RAIN,
            "oshower" to WeatherCondition.LIGHT_SHOWERS, "ishower" to WeatherCondition.LIGHT_SHOWERS,
            "lightsnow" to WeatherCondition.LIGHT_SNOW, "rain" to WeatherCondition.RAIN_UNSPECIFIED,
            "snow" to WeatherCondition.SNOW_UNSPECIFIED,
            "ts" to WeatherCondition.THUNDERSTORM, "tsrain" to WeatherCondition.THUNDERSTORM
        )
        codes.forEach { (code, expected) ->
            listOf("day", "night").forEach { suffix -> assertEquals(expected, adapter.condition(code + suffix)) }
        }
        assertNull(adapter.condition("unknown"))
        assertNull(adapter.condition(null))
    }

    @Test
    fun `precipitation preserves its known type without inventing intensity in all forecasts`() = runTest {
        val cases = listOf(
            Triple("rain", "rain", WeatherCondition.RAIN_UNSPECIFIED),
            Triple("snow", "snow", WeatherCondition.SNOW_UNSPECIFIED),
            Triple("rainsnow", "icep", WeatherCondition.ICE_PELLETS),
            Triple("rainsnow", "frzr", WeatherCondition.FREEZING_RAIN_UNSPECIFIED),
            Triple("rainsnow", "none", null)
        )
        for ((code, type, expected) in cases) {
            val body = """{"init":"2026100300","dataseries":[{"timepoint":18,"weather":"${code}day","prec_type":"$type","prec_amount":9}]}"""
            val weather = provider(body).getWeather(55.7, 37.6, prefs)
            assertEquals(expected, weather.current.condition)
            assertEquals(expected, weather.daily.single().condition)
            assertEquals(expected, weather.hourly.single().condition)
        }
    }

    @Test
    fun `HTTP malformed empty and invalid init responses fail without a fallback`() = runTest {
        val cases = listOf(
            "Unavailable" to HttpStatusCode.ServiceUnavailable,
            "{" to HttpStatusCode.OK,
            "{}" to HttpStatusCode.OK,
            """{"init":"2026100300","dataseries":[]}""" to HttpStatusCode.OK,
            """{"init":"2026023000","dataseries":[{"timepoint":3}]}""" to HttpStatusCode.OK
        )
        cases.forEach { (body, status) ->
            val result = runCatching { provider(body, status).getWeather(55.7, 37.6, prefs) }
            assertTrue(result.isFailure)
            if (status == HttpStatusCode.ServiceUnavailable) assertTrue(result.exceptionOrNull() is ResponseException)
        }
    }

    @Test
    fun `cancellation stops civil request without another request`() = runTest {
        val entered = CompletableDeferred<Unit>()
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            entered.complete(Unit)
            awaitCancellation()
        }).also(clients::add)
        val job = async { SevenTimerWeatherProvider(client, json, clock).getWeather(55.7, 37.6, prefs) }
        entered.await()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(1, calls)
    }

    companion object {
        fun fixture(): String {
            val points = (3..192 step 3).joinToString(",") { hour ->
                val suffix = if (hour % 24 in 6..15) "day" else "night"
                """{"timepoint":$hour,"temp2m":${hour % 24},"rh2m":"70%","weather":"clear$suffix","wind10m":{"speed":8},"prec_amount":9}"""
            }
            return """{"product":"civil","init":"2026100300","dataseries":[$points]}"""
        }
    }
}
