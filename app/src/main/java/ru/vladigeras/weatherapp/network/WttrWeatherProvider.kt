package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import kotlinx.coroutines.CancellationException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class WttrWeatherProvider @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) : WeatherProvider {
    override val id = WeatherProviderId.WTTR
    override val capabilities = ProviderCapabilities(
        maxForecastDays = 3, hourlyStepHours = 3,
        dailyPrecipitation = false, dailyUv = false, dailyWind = false, dayNight = false
    )

    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather {
        val location = "$latitude,$longitude"
        var includeHourly = prefs.showHourlyForecast
        val response = try {
            loadResponse(location, if (includeHourly) "j1" else "j2")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!includeHourly) throw e
            includeHourly = false
            loadResponse(location, "j2")
        }
        val zone = if (includeHourly) {
            try {
                val timezone = request(location, "%Z", timeoutMillis = 5_000).trim()
                require(timezone in ZoneId.getAvailableZoneIds()) { "Invalid weather timezone" }
                ZoneId.of(timezone)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } else null
        val current = requireNotNull(response.current.firstOrNull()) { "Missing current weather" }
        return ProviderWeather(
            provider = id,
            timezone = zone?.id,
            current = CurrentWeather(
                current.temperature?.toDoubleOrNull(), current.feelsLike?.toDoubleOrNull(), current.humidity?.toIntOrNull(),
                current.windSpeed?.toDoubleOrNull(), condition(current.code)
            ),
            daily = if (!prefs.showForecastDays) emptyList() else response.days.take(prefs.forecastDays.coerceAtMost(3)).map { day ->
                ForecastDay(
                    date = LocalDate.parse(day.date).toString(),
                    temperatureMin = day.min?.toDoubleOrNull(), temperatureMax = day.max?.toDoubleOrNull(),
                    sunrise = if (prefs.showSunTimes) time(day.astronomy.firstOrNull()?.sunrise) else null,
                    sunset = if (prefs.showSunTimes) time(day.astronomy.firstOrNull()?.sunset) else null
                )
            },
            hourly = if (zone == null) emptyList() else response.days.flatMap { day ->
                val date = LocalDate.parse(day.date)
                day.hourly.map { hour ->
                    val value = hour.time.toInt()
                    val local = date.atTime(LocalTime.of(value / 100, value % 100))
                    ForecastHour(
                        local.atZone(zone).toEpochSecond(), condition(hour.code), hour.temperature?.toDoubleOrNull(),
                        hour.humidity?.toIntOrNull(), hour.windSpeed?.toDoubleOrNull()
                    )
                }
            }.sortedBy { it.epochSeconds }
        )
    }

    private suspend fun loadResponse(location: String, format: String): WttrResponse =
        json.decodeFromString<WttrResponse>(request(location, format)).also {
            require(it.current.isNotEmpty()) { "Missing current weather" }
        }

    private suspend fun request(location: String, format: String, timeoutMillis: Long = 15_000): String {
        val response = client.get(BuildConfig.WTTR_API_URL) {
            timeout { requestTimeoutMillis = timeoutMillis }
            url {
                appendPathSegments(location, encodeSlash = true)
                parameters.append("format", format)
            }
        }
        if (!response.status.isSuccess()) throw ResponseException(response, "HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private fun time(value: String?): String? = value?.let {
        runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("hh:mm a", Locale.US)).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
    }

    internal fun condition(code: String?): WeatherCondition? = when (code?.toIntOrNull()) {
        113 -> WeatherCondition.CLEAR
        116 -> WeatherCondition.PARTLY_CLOUDY
        119 -> WeatherCondition.CLOUDY
        122 -> WeatherCondition.OVERCAST
        143, 248 -> WeatherCondition.FOG
        260 -> WeatherCondition.RIME_FOG
        176, 293, 296 -> WeatherCondition.LIGHT_RAIN
        179, 323, 326 -> WeatherCondition.LIGHT_SNOW
        182, 317, 320, 362, 365 -> WeatherCondition.SLEET
        185, 281 -> WeatherCondition.FREEZING_DRIZZLE
        284 -> WeatherCondition.HEAVY_FREEZING_DRIZZLE
        200, 386, 389, 392, 395 -> WeatherCondition.THUNDERSTORM
        227, 329, 332 -> WeatherCondition.SNOW
        230, 335, 338 -> WeatherCondition.HEAVY_SNOW
        263, 266 -> WeatherCondition.LIGHT_DRIZZLE
        299, 302 -> WeatherCondition.RAIN
        305, 308 -> WeatherCondition.HEAVY_RAIN
        311 -> WeatherCondition.FREEZING_RAIN
        314 -> WeatherCondition.HEAVY_FREEZING_RAIN
        350, 374, 377 -> WeatherCondition.ICE_PELLETS
        353 -> WeatherCondition.LIGHT_SHOWERS
        356 -> WeatherCondition.SHOWERS
        359 -> WeatherCondition.HEAVY_SHOWERS
        368 -> WeatherCondition.SNOW_SHOWERS
        371 -> WeatherCondition.HEAVY_SNOW_SHOWERS
        else -> null
    }
}

@Serializable
private data class WttrResponse(
    @SerialName("current_condition") val current: List<WttrCurrent> = emptyList(),
    @SerialName("weather") val days: List<WttrDay> = emptyList()
)

@Serializable
private data class WttrCurrent(
    @SerialName("temp_C") val temperature: String? = null,
    @SerialName("FeelsLikeC") val feelsLike: String? = null,
    val humidity: String? = null,
    @SerialName("windspeedKmph") val windSpeed: String? = null,
    @SerialName("weatherCode") val code: String? = null
)

@Serializable
private data class WttrDay(
    val date: String,
    @SerialName("mintempC") val min: String? = null,
    @SerialName("maxtempC") val max: String? = null,
    val astronomy: List<WttrAstronomy> = emptyList(),
    val hourly: List<WttrHour> = emptyList()
)

@Serializable
private data class WttrAstronomy(val sunrise: String? = null, val sunset: String? = null)

@Serializable
private data class WttrHour(
    val time: String,
    @SerialName("tempC") val temperature: String? = null,
    val humidity: String? = null,
    @SerialName("windspeedKmph") val windSpeed: String? = null,
    @SerialName("weatherCode") val code: String? = null
)
