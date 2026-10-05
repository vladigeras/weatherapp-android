package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.*
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.abs
import kotlin.math.roundToInt

class MetWeatherProvider @Inject constructor(
    @Named("met") private val client: HttpClient,
    private val json: Json,
    private val clock: Clock
) : WeatherProvider {
    override val id = WeatherProviderId.YR
    override val capabilities = ProviderCapabilities(
        maxForecastDays = 0, dailyPrecipitation = false, dailyUv = false, dailyWind = false, sunTimes = false
    )

    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather {
        val lat = BigDecimal.valueOf(latitude).setScale(4, RoundingMode.DOWN).toPlainString()
        val lon = BigDecimal.valueOf(longitude).setScale(4, RoundingMode.DOWN).toPlainString()
        val forecast = json.decodeFromString<MetResponse>(request(lat, lon))
        val points = forecast.properties.timeseries.map { Instant.parse(it.time) to it.data }.sortedBy { it.first }
        require(points.isNotEmpty()) { "Missing weather forecast" }
        val now = clock.instant()
        val zone = ZoneId.systemDefault()
        val current = points.minBy { abs(it.first.epochSecond - now.epochSecond) }.second
        return ProviderWeather(
            provider = id, timezone = zone.id,
            current = CurrentWeather(
                temperature = current.instant.details.temperature, feelsLike = current.instant.details.apparentTemperature,
                humidity = current.instant.details.humidity?.roundToInt(), windSpeed = current.instant.details.windSpeed?.times(3.6),
                condition = condition(current.symbol()),
                isDay = when (current.symbol()?.substringAfterLast('_')) { "day" -> 1; "night" -> 0; else -> null }
            ),
            hourly = if (!prefs.showHourlyForecast) emptyList() else points.map { (time, data) ->
                ForecastHour(time.epochSecond, condition(data.symbol()), data.instant.details.temperature,
                    data.instant.details.humidity?.roundToInt(), data.instant.details.windSpeed?.times(3.6))
            }
        )
    }

    private suspend fun request(lat: String, lon: String): String {
        val response = client.get(BuildConfig.MET_FORECAST_API_URL) {
            url {
                parameters.append("lat", lat)
                parameters.append("lon", lon)
            }
        }
        if (!response.status.isSuccess()) throw ResponseException(response, "HTTP ${response.status.value}")
        val body = response.bodyAsBytes()
        return when (val encoding = response.headers[HttpHeaders.ContentEncoding]) {
            "gzip" -> GZIPInputStream(ByteArrayInputStream(body)).use { it.readBytes() }
            "deflate" -> InflaterInputStream(ByteArrayInputStream(body)).use { it.readBytes() }
            null, "identity" -> body
            else -> error("Unsupported content encoding: $encoding")
        }.decodeToString()
    }

    internal fun condition(code: String?): WeatherCondition? = when (code?.substringBefore('_')) {
        "clearsky" -> WeatherCondition.CLEAR
        "fair" -> WeatherCondition.MOSTLY_CLEAR
        "partlycloudy" -> WeatherCondition.PARTLY_CLOUDY
        "cloudy" -> WeatherCondition.OVERCAST
        "fog" -> WeatherCondition.FOG
        "lightrain" -> WeatherCondition.LIGHT_RAIN
        "rain", "rainshowers" -> WeatherCondition.RAIN_UNSPECIFIED
        "heavyrain" -> WeatherCondition.HEAVY_RAIN
        "lightrainshowers" -> WeatherCondition.LIGHT_SHOWERS
        "heavyrainshowers" -> WeatherCondition.HEAVY_SHOWERS
        "lightsnow" -> WeatherCondition.LIGHT_SNOW
        "snow", "snowshowers" -> WeatherCondition.SNOW_UNSPECIFIED
        "heavysnow" -> WeatherCondition.HEAVY_SNOW
        "lightsnowshowers" -> WeatherCondition.SNOW_SHOWERS
        "heavysnowshowers" -> WeatherCondition.HEAVY_SNOW_SHOWERS
        "lightsleet", "sleet", "heavysleet", "lightsleetshowers", "sleetshowers", "heavysleetshowers" -> WeatherCondition.SLEET
        "lightrainandthunder", "rainandthunder", "heavyrainandthunder",
        "lightrainshowersandthunder", "rainshowersandthunder", "heavyrainshowersandthunder",
        "lightsnowandthunder", "snowandthunder", "heavysnowandthunder",
        "lightssnowshowersandthunder", "snowshowersandthunder", "heavysnowshowersandthunder",
        "lightsleetandthunder", "sleetandthunder", "heavysleetandthunder",
        "lightssleetshowersandthunder", "sleetshowersandthunder", "heavysleetshowersandthunder" -> WeatherCondition.THUNDERSTORM
        else -> null
    }
}

@Serializable
private data class MetResponse(val properties: MetProperties)
@Serializable
private data class MetProperties(val timeseries: List<MetPoint>)
@Serializable
private data class MetPoint(val time: String, val data: MetData)
@Serializable
private data class MetData(
    val instant: MetPeriod,
    @SerialName("next_1_hours") val next1: MetPeriod? = null,
    @SerialName("next_6_hours") val next6: MetPeriod? = null,
    @SerialName("next_12_hours") val next12: MetPeriod? = null
) {
    fun symbol() = (next1 ?: next6 ?: next12)?.summary?.symbol
}
@Serializable
private data class MetPeriod(val details: MetDetails = MetDetails(), val summary: MetSummary? = null)
@Serializable
private data class MetSummary(@SerialName("symbol_code") val symbol: String? = null)
@Serializable
private data class MetDetails(
    @SerialName("air_temperature") val temperature: Double? = null,
    @SerialName("apparent_air_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity") val humidity: Double? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null
)
