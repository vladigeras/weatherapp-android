package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.*
import ru.vladigeras.weatherapp.domain.mapper.WeatherMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class WttrWeatherProviderTest {
    private val prefs = WeatherDisplayPrefs(provider = WeatherProviderId.WTTR)
    private val json = Json { ignoreUnknownKeys = true }
    private val headers = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8")

    @Test
    fun `j2 returns current and local astronomy without requesting timezone`() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals("j2", request.url.parameters["format"])
            assertEquals(Url(BuildConfig.WTTR_API_URL).host, request.url.host)
            assertEquals("55.7,37.6", request.url.segments.last())
            respond(fixture(false), headers = headers)
        })
        val weather = WttrWeatherProvider(client, json).getWeather(55.7, 37.6, prefs.copy(showHourlyForecast = false))
        assertEquals(1, calls)
        assertNull(weather.timezone)
        assertTrue(weather.hourly.isEmpty())
        assertEquals(12.0, weather.current.temperature!!, 0.001)
        assertEquals(8.0, weather.current.feelsLike!!, 0.001)
        assertEquals(39, weather.current.humidity)
        assertEquals(14.0, weather.current.windSpeed!!, 0.001)
        assertEquals(WeatherCondition.CLEAR, weather.current.condition)
        assertNull(weather.current.isDay)
        assertEquals(3, weather.daily.size)
        assertEquals("06:36", weather.daily.first().sunrise)
        assertEquals("18:00", weather.daily.first().sunset)
        weather.daily.forEach {
            assertNull(it.condition)
            assertNull(it.precipitationSum)
            assertNull(it.windSpeedMax)
            assertNull(it.uvIndexMax)
        }
        client.close()
    }

    @Test
    fun `j1 and Z preserve three hour instants and clip actual forecast coverage`() = runTest {
        val formats = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            formats += request.url.parameters["format"]!!
            assertFalse(request.url.parameters.contains("days"))
            assertFalse(request.url.parameters.contains("tp"))
            respond(if (formats.last() == "%Z") "Europe/Moscow\n" else fixture(true), headers = headers)
        })
        val weather = WttrWeatherProvider(client, json).getWeather(55.7, 37.6, prefs)
        assertEquals(listOf("j1", "%Z"), formats)
        assertEquals("Europe/Moscow", weather.timezone)
        assertEquals(24, weather.hourly.size)
        assertEquals(24, weather.hourly.map { it.epochSeconds }.toSet().size)
        assertEquals(10800L, weather.hourly[1].epochSeconds - weather.hourly[0].epochSeconds)
        assertEquals(WeatherCondition.PARTLY_CLOUDY, weather.hourly[0].condition)
        val mapper = WeatherMapper(mockk(), Clock.fixed(Instant.parse("2026-10-03T17:25:00Z"), ZoneOffset.UTC))
        assertEquals(listOf("21:00", "00:00", "03:00", "06:00"), mapper.mapToHourlyForecast(weather.hourly, weather.timezone, 12).map { it.time })
        assertEquals(8, mapper.mapToHourlyForecast(weather.hourly, weather.timezone, 24).size)
        assertEquals(16, mapper.mapToHourlyForecast(weather.hourly, weather.timezone, 48).size)
        val nearEnd = WeatherMapper(mockk(), Clock.fixed(Instant.parse("2026-10-05T17:25:00Z"), ZoneOffset.UTC))
        assertEquals(1, nearEnd.mapToHourlyForecast(weather.hourly, weather.timezone, 48).size)
        client.close()
    }

    @Test
    fun `hourly timezone errors fail instead of using device timezone`() = runTest {
        for (zone in listOf("", "not-a-zone", "+03:00")) {
            val client = HttpClient(MockEngine { request -> respond(if (request.url.parameters["format"] == "%Z") zone else fixture(true), headers = headers) })
            assertTrue(runCatching { WttrWeatherProvider(client, json).getWeather(55.7, 37.6, prefs) }.isFailure)
            client.close()
        }
        val client = HttpClient(MockEngine { request ->
            if (request.url.parameters["format"] == "%Z") respond("Unavailable", HttpStatusCode.ServiceUnavailable, headers)
            else respond(fixture(true), headers = headers)
        })
        assertTrue(runCatching { WttrWeatherProvider(client, json).getWeather(55.7, 37.6, prefs) }.isFailure)
        client.close()
    }

    @Test
    fun `malformed partial JSON and HTTP errors fail`() = runTest {
        for ((body, status) in listOf("{\"weather\":[" to HttpStatusCode.OK, "error" to HttpStatusCode.InternalServerError, "{}" to HttpStatusCode.OK)) {
            val client = HttpClient(MockEngine { respond(body, status, headers) })
            assertTrue(runCatching { WttrWeatherProvider(client, json).getWeather(55.7, 37.6, prefs.copy(showHourlyForecast = false)) }.isFailure)
            client.close()
        }
    }

    @Test
    fun `search sends encoded name and language and returns one candidate`() = runTest {
        val name = "São Paulo / centre"
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals(name, request.url.segments.last())
            assertEquals("ru", request.url.parameters["lang"])
            assertEquals("j2", request.url.parameters["format"])
            respond(fixture(false), headers = headers)
        })
        val results = WttrWeatherProvider(client, json).searchLocations(name, "ru")
        assertEquals(1, calls)
        assertEquals(listOf(SearchLocation("55.752,37.616", "Москва", 55.752, 37.616, "Russia", admin1 = "Moscow City")), results)
        client.close()
    }

    @Test
    fun `missing optional numbers and astronomy remain absent`() = runTest {
        val client = HttpClient(MockEngine { respond("""{"current_condition":[{"temp_C":"N/A","weatherCode":"999"}],"weather":[{"date":"2026-10-03","astronomy":[{"sunrise":"No sunrise","sunset":"No sunset"}]}]}""", headers = headers) })
        val weather = WttrWeatherProvider(client, json).getWeather(55.7, 37.6, prefs.copy(showHourlyForecast = false))
        assertEquals(CurrentWeather(), weather.current)
        assertEquals(ForecastDay("2026-10-03"), weather.daily.single())
        client.close()
    }

    @Test
    fun `WWO codes map to shared conditions and unknown codes stay unknown`() {
        val client = HttpClient(MockEngine { respond("", headers = headers) })
        val provider = WttrWeatherProvider(client, json)
        val groups = mapOf(
            WeatherCondition.CLEAR to listOf(113), WeatherCondition.PARTLY_CLOUDY to listOf(116), WeatherCondition.CLOUDY to listOf(119), WeatherCondition.OVERCAST to listOf(122),
            WeatherCondition.FOG to listOf(143,248), WeatherCondition.RIME_FOG to listOf(260), WeatherCondition.LIGHT_RAIN to listOf(176,293,296),
            WeatherCondition.LIGHT_SNOW to listOf(179,323,326), WeatherCondition.SLEET to listOf(182,317,320,362,365), WeatherCondition.FREEZING_DRIZZLE to listOf(185,281),
            WeatherCondition.HEAVY_FREEZING_DRIZZLE to listOf(284), WeatherCondition.THUNDERSTORM to listOf(200,386,389,392,395), WeatherCondition.SNOW to listOf(227,329,332),
            WeatherCondition.HEAVY_SNOW to listOf(230,335,338), WeatherCondition.LIGHT_DRIZZLE to listOf(263,266), WeatherCondition.RAIN to listOf(299,302), WeatherCondition.HEAVY_RAIN to listOf(305,308),
            WeatherCondition.FREEZING_RAIN to listOf(311), WeatherCondition.HEAVY_FREEZING_RAIN to listOf(314), WeatherCondition.ICE_PELLETS to listOf(350,374,377),
            WeatherCondition.LIGHT_SHOWERS to listOf(353), WeatherCondition.SHOWERS to listOf(356), WeatherCondition.HEAVY_SHOWERS to listOf(359), WeatherCondition.SNOW_SHOWERS to listOf(368), WeatherCondition.HEAVY_SNOW_SHOWERS to listOf(371)
        )
        groups.forEach { (condition, codes) -> codes.forEach { assertEquals(condition, provider.condition(it.toString())) } }
        assertNull(provider.condition("999"))
        assertNull(provider.condition(null))
        client.close()
    }

    companion object {
        fun fixture(hourly: Boolean): String {
            val days = (3..5).joinToString(",") { day ->
                val hours = if (!hourly) "" else ",\"hourly\":[" + (0..21 step 3).joinToString(",") { hour -> """{"time":"${hour*100}","tempC":"12","humidity":"50","windspeedKmph":"9","weatherCode":"116","precipMM":"2","uvIndex":"8"}""" } + "]"
                """{"date":"2026-10-0$day","mintempC":"8","maxtempC":"15","uvIndex":"8","astronomy":[{"sunrise":"06:36 AM","sunset":"06:00 PM"}]$hours}"""
            }
            return """{"current_condition":[{"temp_C":"12","FeelsLikeC":"8","humidity":"39","windspeedKmph":"14","weatherCode":"113"}],"nearest_area":[{"areaName":[{"value":"Москва"}],"latitude":"55.752","longitude":"37.616","country":[{"value":"Russia"}],"region":[{"value":"Moscow City"}]}],"weather":[$days]}"""
        }
    }
}
