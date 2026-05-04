package ru.vladigeras.weatherapp.domain.mapper

import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import ru.vladigeras.weatherapp.ui.DailyForecast
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherMapper @Inject constructor(
    private val languagePreferenceRepository: LanguagePreferenceRepository
) {

    suspend fun mapToDailyForecast(
        daily: DailyWeather?,
        utcOffsetSeconds: Int
    ): List<DailyForecast> {
        return processDailyForecast(daily, utcOffsetSeconds)
    }

    private suspend fun processDailyForecast(daily: DailyWeather?, utcOffsetSeconds: Int): List<DailyForecast> {
        daily ?: return emptyList()
        val size = daily.time.size
        val forecasts = mutableListOf<DailyForecast>()
        for (i in 0 until size) {
            val dateStr = daily.time[i]
            val dayName = getDayName(dateStr)
            val formattedDate = getFormattedDate(dateStr)
            forecasts.add(
                DailyForecast(
                    date = formattedDate,
                    dayName = dayName,
                    weatherCode = daily.weatherCode?.getOrNull(i),
                    temperatureMin = daily.temperature2mMin?.getOrNull(i) ?: 0.0,
                    temperatureMax = daily.temperature2mMax?.getOrNull(i) ?: 0.0,
                    precipitationSum = daily.precipitationSum?.getOrNull(i),
                    sunrise = daily.sunrise?.getOrNull(i)?.let { formatTimeFromUTC(it, utcOffsetSeconds) },
                    sunset = daily.sunset?.getOrNull(i)?.let { formatTimeFromUTC(it, utcOffsetSeconds) },
                    windSpeedMax = daily.windspeed10mMax?.getOrNull(i),
                    windDirectionDominant = daily.winddirection10mDominant?.getOrNull(i),
                    uvIndexMax = daily.uvIndexMax?.getOrNull(i)
                )
            )
        }
        return forecasts
    }

    private suspend fun getDayName(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            val currentLocale = languagePreferenceRepository.getAppLocale()
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale)
            dayName.replaceFirstChar { it.uppercaseChar() }
        } catch (e: Exception) {
            "?"
        }
    }

    private suspend fun getFormattedDate(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            val day = date.dayOfMonth
            val currentLocale = languagePreferenceRepository.getAppLocale()
            val monthName = date.month.getDisplayName(TextStyle.SHORT, currentLocale)
            "$day ${monthName.replaceFirstChar { it.uppercaseChar() }}"
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun formatTimeFromUTC(utcTimeString: String, utcOffsetSeconds: Int): String {
        return runCatching {
            val localTime = LocalDateTime.parse(utcTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atOffset(ZoneOffset.UTC)
                .withOffsetSameInstant(ZoneOffset.ofTotalSeconds(utcOffsetSeconds))
            localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrDefault("--:--")
    }
}