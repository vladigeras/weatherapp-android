package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.*
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import javax.inject.Inject
import kotlin.math.abs

class SevenTimerWeatherProvider @Inject constructor(
    private val client: HttpClient,
    private val json: Json,
    private val clock: Clock
) : WeatherProvider {
    override val id = WeatherProviderId.SEVEN_TIMER
    override val capabilities = ProviderCapabilities(
        maxForecastDays = 7, hourlyStepHours = 3,
        dailyPrecipitation = false, dailyUv = false, dailyWind = false,
        wind = false, sunTimes = false
    )

    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather {
        val response = client.get(BuildConfig.SEVEN_TIMER_API_URL) {
            url {
                parameters.append("lat", latitude.toString())
                parameters.append("lon", longitude.toString())
                parameters.append("product", "civil")
                parameters.append("output", "json")
                parameters.append("unit", "metric")
            }
        }
        if (!response.status.isSuccess()) throw ResponseException(response, "HTTP ${response.status.value}")
        val forecast = json.decodeFromString<SevenTimerResponse>(response.bodyAsText())
        require(forecast.dataseries.isNotEmpty()) { "Missing weather forecast" }
        val init = LocalDateTime.parse(forecast.init, DateTimeFormatter.ofPattern("uuuuMMddHH").withResolverStyle(ResolverStyle.STRICT))
            .toInstant(ZoneOffset.UTC)
        val points = forecast.dataseries.sortedBy { it.timepoint }.map { point ->
            point to init.plusSeconds(point.timepoint * 3600L)
        }
        val now = clock.instant()
        val zone = ZoneId.systemDefault()
        val current = points.minBy { abs(it.second.epochSecond - now.epochSecond) }.first
        val today = now.atZone(zone).toLocalDate()
        return ProviderWeather(
            provider = id,
            timezone = zone.id,
            current = CurrentWeather(
                temperature = current.temperature(), humidity = current.humidity(),
                condition = condition(current.weather, current.precipitationType),
                isDay = when {
                    current.weather?.endsWith("day") == true -> 1
                    current.weather?.endsWith("night") == true -> 0
                    else -> null
                }
            ),
            daily = if (!prefs.showForecastDays) emptyList() else points.groupBy { it.second.atZone(zone).toLocalDate() }
                .filterKeys { !it.isBefore(today) }.entries.take(prefs.forecastDays.coerceAtMost(capabilities.maxForecastDays)).map { (date, dayPoints) ->
                    val temperatures = dayPoints.mapNotNull { it.first.temperature() }
                    val noon = date.atTime(12, 0).atZone(zone).toEpochSecond()
                    ForecastDay(
                        date = date.toString(),
                        condition = dayPoints.minBy { abs(it.second.epochSecond - noon) }.first.let { condition(it.weather, it.precipitationType) },
                        temperatureMin = temperatures.minOrNull(), temperatureMax = temperatures.maxOrNull()
                    )
                },
            hourly = if (!prefs.showHourlyForecast) emptyList() else points.map { (point, instant) ->
                ForecastHour(instant.epochSecond, condition(point.weather, point.precipitationType), point.temperature(), point.humidity())
            }
        )
    }

    internal fun condition(code: String?, precipitationType: String? = null): WeatherCondition? = when (code?.removeSuffix("day")?.removeSuffix("night")) {
        "clear" -> WeatherCondition.CLEAR
        "pcloudy" -> WeatherCondition.PARTLY_CLOUDY
        "mcloudy" -> WeatherCondition.CLOUDY
        "cloudy" -> WeatherCondition.OVERCAST
        "humid" -> WeatherCondition.FOG
        "lightrain" -> WeatherCondition.LIGHT_RAIN
        "oshower", "ishower" -> WeatherCondition.LIGHT_SHOWERS
        "lightsnow" -> WeatherCondition.LIGHT_SNOW
        "rain" -> WeatherCondition.RAIN_UNSPECIFIED
        "snow" -> WeatherCondition.SNOW_UNSPECIFIED
        "rainsnow" -> when (precipitationType) {
            "icep" -> WeatherCondition.ICE_PELLETS
            "frzr" -> WeatherCondition.FREEZING_RAIN_UNSPECIFIED
            else -> null
        }
        "ts", "tsrain" -> WeatherCondition.THUNDERSTORM
        else -> null
    }
}

@Serializable
private data class SevenTimerResponse(val init: String, val dataseries: List<SevenTimerPoint>)

@Serializable
private data class SevenTimerPoint(
    val timepoint: Int,
    val temp2m: Double? = null,
    val rh2m: String? = null,
    val weather: String? = null,
    @SerialName("prec_type") val precipitationType: String? = null
) {
    fun temperature() = temp2m?.takeUnless { it == -9999.0 }
    fun humidity() = rh2m?.removeSuffix("%")?.toIntOrNull()?.takeIf { it in 0..100 }
}
