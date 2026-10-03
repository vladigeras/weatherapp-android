package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
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
        maxForecastDays = 3, hourlyStepHours = 3, explicitSearch = true,
        dailyPrecipitation = false, dailyUv = false, dailyWind = false, dayNight = false
    )

    override suspend fun getWeather(latitude: Double, longitude: Double, prefs: WeatherDisplayPrefs): ProviderWeather {
        val location = "$latitude,$longitude"
        val response = json.decodeFromString<WttrResponse>(request(location, if (prefs.showHourlyForecast) "j1" else "j2"))
        val zone = if (prefs.showHourlyForecast) ZoneId.of(request(location, "%Z").trim()) else null
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

    override suspend fun searchLocations(query: String, languageCode: String): List<SearchLocation> {
        val response = json.decodeFromString<WttrResponse>(request(query, "j2", languageCode))
        return response.areas.take(1).map { area ->
            val latitude = requireNotNull(area.latitude.toDoubleOrNull())
            val longitude = requireNotNull(area.longitude.toDoubleOrNull())
            SearchLocation(
                id = "$latitude,$longitude", name = requireNotNull(area.name.firstOrNull()?.value),
                latitude = latitude, longitude = longitude,
                country = area.country.firstOrNull()?.value, admin1 = area.region.firstOrNull()?.value
            )
        }
    }

    private suspend fun request(location: String, format: String, language: String? = null): String {
        val response = client.get(BuildConfig.WTTR_API_URL) {
            url {
                appendPathSegments(location, encodeSlash = true)
                parameters.append("format", format)
                language?.let { parameters.append("lang", it) }
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
    @SerialName("nearest_area") val areas: List<WttrArea> = emptyList(),
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

@Serializable
private data class WttrArea(
    val latitude: String,
    val longitude: String,
    @SerialName("areaName") val name: List<WttrValue> = emptyList(),
    val country: List<WttrValue> = emptyList(),
    val region: List<WttrValue> = emptyList()
)

@Serializable
private data class WttrValue(val value: String)
