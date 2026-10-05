package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    override val capabilities = ProviderCapabilities(maxForecastDays = 9, hourlyStepHours = 1, dailyUv = false)

    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather {
        val lat = BigDecimal.valueOf(latitude).setScale(4, RoundingMode.DOWN).toPlainString()
        val lon = BigDecimal.valueOf(longitude).setScale(4, RoundingMode.DOWN).toPlainString()
        val forecast = json.decodeFromString<MetResponse>(request(BuildConfig.MET_FORECAST_API_URL, lat, lon))
        val points = forecast.properties.timeseries.map { Instant.parse(it.time) to it.data }.sortedBy { it.first }
        require(points.isNotEmpty()) { "Missing weather forecast" }
        val now = clock.instant()
        val zone = ZoneId.systemDefault()
        val current = points.minBy { abs(it.first.epochSecond - now.epochSecond) }.second
        val today = now.atZone(zone).toLocalDate()
        var daily = if (!prefs.showForecastDays) emptyList() else points.groupBy { it.first.atZone(zone).toLocalDate() }
            .filterKeys { !it.isBefore(today) }.entries.take(prefs.forecastDays.coerceAtMost(capabilities.maxForecastDays)).map { (date, dayPoints) ->
                val start = date.atStartOfDay(zone).toInstant()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant()
                val temperatures = dayPoints.flatMap { (time, data) ->
                    listOfNotNull(data.instant.details.temperature) + if (time.plusSeconds(21600) <= end)
                        listOfNotNull(data.next6?.details?.temperatureMin, data.next6?.details?.temperatureMax) else emptyList()
                }
                val noon = date.atTime(12, 0).atZone(zone).toEpochSecond()
                ForecastDay(
                    date = date.toString(),
                    condition = condition(dayPoints.minBy { abs(it.first.epochSecond - noon) }.second.symbol()),
                    temperatureMin = temperatures.minOrNull(), temperatureMax = temperatures.maxOrNull(),
                    precipitationSum = if (prefs.showPrecipitation) precipitation(points, start, end) else null,
                    windSpeedMax = if (prefs.showWind) dayPoints.mapNotNull { it.second.instant.details.windSpeed }.maxOrNull()?.times(3.6) else null
                )
            }
        if (prefs.showSunTimes && daily.isNotEmpty()) {
            val start = LocalDate.parse(daily.first().date).atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDate().minusDays(1)
            val end = LocalDate.parse(daily.last().date).plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1)
                .atZone(ZoneOffset.UTC).toLocalDate().plusDays(1)
            val suns = coroutineScope {
                (0..ChronoUnit.DAYS.between(start, end).toInt()).map { offset -> async {
                    json.decodeFromString<MetSunResponse>(request(BuildConfig.MET_SUN_API_URL, lat, lon, start.plusDays(offset.toLong()))).properties
                } }.awaitAll()
            }
            fun events(select: (MetSunProperties) -> MetSunEvent?) = suns.mapNotNull { select(it)?.time }
                .map { OffsetDateTime.parse(it).atZoneSameInstant(zone) }.distinct().associateBy { it.toLocalDate().toString() }
            val rises = events { it.sunrise }
            val sets = events { it.sunset }
            val format = DateTimeFormatter.ofPattern("HH:mm")
            daily = daily.map { it.copy(sunrise = rises[it.date]?.format(format), sunset = sets[it.date]?.format(format)) }
        }
        return ProviderWeather(
            provider = id, timezone = zone.id,
            current = CurrentWeather(
                temperature = current.instant.details.temperature, feelsLike = current.instant.details.apparentTemperature,
                humidity = current.instant.details.humidity?.roundToInt(), windSpeed = current.instant.details.windSpeed?.times(3.6),
                condition = condition(current.symbol()),
                isDay = when (current.symbol()?.substringAfterLast('_')) { "day" -> 1; "night" -> 0; else -> null }
            ),
            daily = daily,
            hourly = if (!prefs.showHourlyForecast) emptyList() else points.map { (time, data) ->
                ForecastHour(time.epochSecond, condition(data.symbol()), data.instant.details.temperature,
                    data.instant.details.humidity?.roundToInt(), data.instant.details.windSpeed?.times(3.6))
            }
        )
    }

    private suspend fun request(url: String, lat: String, lon: String, date: LocalDate? = null): String {
        val response = client.get(url) {
            url {
                parameters.append("lat", lat)
                parameters.append("lon", lon)
                date?.let { parameters.append("date", it.toString()) }
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

    private fun precipitation(points: List<Pair<Instant, MetData>>, start: Instant, end: Instant): Double? {
        val byTime = points.associate { it.first to it.second }
        val totals = mutableMapOf<Instant, Double?>()
        fun total(time: Instant): Double? {
            if (time == end) return 0.0
            if (totals.containsKey(time)) return totals[time]
            val data = byTime[time]
            val result = listOf(1 to data?.next1, 6 to data?.next6).firstNotNullOfOrNull { (hours, period) ->
                val next = time.plusSeconds(hours * 3600L)
                period?.details?.precipitation?.takeIf { next <= end }?.let { amount -> total(next)?.plus(amount) }
            }
            totals[time] = result
            return result
        }
        return total(start)
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
    @SerialName("wind_speed") val windSpeed: Double? = null,
    @SerialName("air_temperature_min") val temperatureMin: Double? = null,
    @SerialName("air_temperature_max") val temperatureMax: Double? = null,
    @SerialName("precipitation_amount") val precipitation: Double? = null
)
@Serializable
private data class MetSunResponse(val properties: MetSunProperties)
@Serializable
private data class MetSunProperties(val sunrise: MetSunEvent? = null, val sunset: MetSunEvent? = null)
@Serializable
private data class MetSunEvent(val time: String? = null)
