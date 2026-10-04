package ru.vladigeras.weatherapp.domain.mapper

import ru.vladigeras.weatherapp.data.ForecastDay
import ru.vladigeras.weatherapp.data.ForecastHour
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.ui.DailyForecast
import ru.vladigeras.weatherapp.ui.HourlyForecast
import java.time.Instant
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherMapper @Inject constructor(
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    suspend fun mapToDailyForecast(daily: List<ForecastDay>): List<DailyForecast> {
        val locale = languagePreferenceRepository.getAppLocale()
        val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
        return daily.map { day ->
            val date = LocalDate.parse(day.date)
            DailyForecast(
                date = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercaseChar() }}",
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercaseChar() },
                weatherCode = day.condition?.displayCode,
                temperatureMin = day.temperatureMin,
                temperatureMax = day.temperatureMax,
                precipitationSum = day.precipitationSum,
                sunrise = day.sunrise,
                sunset = day.sunset,
                windSpeedMax = day.windSpeedMax,
                windDirectionDominant = day.windDirectionDominant,
                uvIndexMax = day.uvIndexMax,
                relativeDay = ChronoUnit.DAYS.between(today, date)
            )
        }
    }

    fun mapToHourlyForecast(hourly: List<ForecastHour>, timezone: String?, hours: Int): List<HourlyForecast> {
        if (hourly.isEmpty()) return emptyList()
        val zone = ZoneId.of(requireNotNull(timezone))
        val now = clock.instant().epochSecond
        val end = now + hours * 3600L
        return hourly.filter { it.epochSeconds >= now && it.epochSeconds < end }.map { hour ->
            HourlyForecast(
                time = Instant.ofEpochSecond(hour.epochSeconds).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")),
                weatherCode = hour.condition?.displayCode,
                temperature = hour.temperature,
                humidity = hour.humidity,
                windSpeed = hour.windSpeed,
                epochSeconds = hour.epochSeconds
            )
        }
    }
}
