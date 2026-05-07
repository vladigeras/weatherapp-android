package ru.vladigeras.weatherapp.domain.mapper

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.data.DailyWeather
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import java.util.Locale

class WeatherMapperTest {

    private lateinit var languagePreferenceRepository: LanguagePreferenceRepository
    private lateinit var weatherMapper: WeatherMapper

    @Before
    fun setup() {
        languagePreferenceRepository = mockk()
        weatherMapper = WeatherMapper(languagePreferenceRepository)
    }

    @Test
    fun `mapToDailyForecast returns empty list when daily is null`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val result = weatherMapper.mapToDailyForecast(null, 0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `mapToDailyForecast formats dates with English locale`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals("27 Apr", result[0].date)
        assertEquals("28 Apr", result[1].date)
    }

    @Test
    fun `mapToDailyForecast formats dates with Russian locale`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale("ru", "RU")

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        // Russian short month name for April is "апр." (with dot)
        assertEquals("27 Апр.", result[0].date)
        assertEquals("28 Апр.", result[1].date)
    }

    @Test
    fun `mapToDailyForecast formats day names with English locale`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        // April 27, 2026 is Monday, short form in English is "Mon"
        assertEquals("Mon", result[0].dayName)
        // April 28, 2026 is Tuesday, short form in English is "Tue"
        assertEquals("Tue", result[1].dayName)
    }

    @Test
    fun `mapToDailyForecast formats day names with Russian locale`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale("ru", "RU")

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        // April 27, 2026 is Monday, short form in Russian is "Пн"
        assertEquals("Пн", result[0].dayName)
        // April 28, 2026 is Tuesday, short form in Russian is "Вт"
        assertEquals("Вт", result[1].dayName)
    }

    @Test
    fun `mapToDailyForecast maps weather codes correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals(0, result[0].weatherCode)
        assertEquals(1, result[1].weatherCode)
    }

    @Test
    fun `mapToDailyForecast maps temperature min max correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals(15.0, result[0].temperatureMin, 0.001)
        assertEquals(25.0, result[0].temperatureMax, 0.001)
        assertEquals(14.0, result[1].temperatureMin, 0.001)
        assertEquals(23.0, result[1].temperatureMax, 0.001)
    }

    @Test
    fun `mapToDailyForecast maps precipitation sum correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals(0.0, result[0].precipitationSum ?: 0.0, 0.001)
        assertEquals(2.5, result[1].precipitationSum ?: 0.0, 0.001)
    }

    @Test
    fun `mapToDailyForecast converts sunrise time from UTC to local`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        // UTC+3 hours (10800 seconds)
        val utcOffsetSeconds = 10800
        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, utcOffsetSeconds)

        assertEquals(2, result.size)
        // Sunrise UTC "2026-04-27T04:30:00" + 3 hours = 07:30
        assertEquals("07:30", result[0].sunrise)
        // Sunrise UTC "2026-04-28T04:29:00" + 3 hours = 07:29
        assertEquals("07:29", result[1].sunrise)
    }

    @Test
    fun `mapToDailyForecast converts sunset time from UTC to local`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        // UTC+3 hours (10800 seconds)
        val utcOffsetSeconds = 10800
        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, utcOffsetSeconds)

        assertEquals(2, result.size)
        // Sunset UTC "2026-04-27T20:15:00" + 3 hours = 23:15
        assertEquals("23:15", result[0].sunset)
        // Sunset UTC "2026-04-28T20:16:00" + 3 hours = 23:16
        assertEquals("23:16", result[1].sunset)
    }

    @Test
    fun `mapToDailyForecast maps wind speed max correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals(10.0, result[0].windSpeedMax ?: 0.0, 0.001)
        assertEquals(12.0, result[1].windSpeedMax ?: 0.0, 0.001)
    }

    @Test
    fun `mapToDailyForecast maps wind direction dominant correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals(180, result[0].windDirectionDominant ?: 0)
        assertEquals(200, result[1].windDirectionDominant ?: 0)
    }

    @Test
    fun `mapToDailyForecast maps uv index max correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeather()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(2, result.size)
        assertEquals(5.0, result[0].uvIndexMax ?: 0.0, 0.001)
        assertEquals(3.0, result[1].uvIndexMax ?: 0.0, 0.001)
    }

    @Test
    fun `mapToDailyForecast handles null weather code`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeatherWithNulls()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        assertEquals(null, result[0].weatherCode)
    }

    @Test
    fun `mapToDailyForecast handles null temperature values`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeatherWithNulls()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        // Temperature min/max should default to 0.0 when null
        assertEquals(0.0, result[0].temperatureMin, 0.001)
        assertEquals(0.0, result[0].temperatureMax, 0.001)
    }

    @Test
    fun `mapToDailyForecast handles null precipitation sum`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeatherWithNulls()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        assertEquals(null, result[0].precipitationSum)
    }

    @Test
    fun `mapToDailyForecast handles null sunrise sunset`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeatherWithNulls()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        assertEquals(null, result[0].sunrise)
        assertEquals(null, result[0].sunset)
    }

    @Test
    fun `mapToDailyForecast handles null wind values`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeatherWithNulls()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        assertEquals(null, result[0].windSpeedMax)
        assertEquals(null, result[0].windDirectionDominant)
    }

    @Test
    fun `mapToDailyForecast handles null uv index`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = createTestDailyWeatherWithNulls()
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        assertEquals(null, result[0].uvIndexMax)
    }

    @Test
    fun `mapToDailyForecast handles invalid date string`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = DailyWeather(
            time = listOf("invalid-date"),
            weatherCode = listOf(0),
            temperature2mMax = listOf(25.0),
            temperature2mMin = listOf(15.0)
        )
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        // Invalid date should return the original string for date
        assertEquals("invalid-date", result[0].date)
        // Invalid date should return "?" for day name
        assertEquals("?", result[0].dayName)
    }

    @Test
    fun `mapToDailyForecast handles invalid time string for sunrise`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = DailyWeather(
            time = listOf("2026-04-27"),
            weatherCode = listOf(0),
            temperature2mMax = listOf(25.0),
            temperature2mMin = listOf(15.0),
            sunrise = listOf("invalid-time")
        )
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(1, result.size)
        // Invalid time should return "--:--"
        assertEquals("--:--", result[0].sunrise)
    }

    @Test
    fun `mapToDailyForecast handles time conversion with negative offset`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        // UTC-5 hours (-18000 seconds) - e.g., New York
        val utcOffsetSeconds = -18000
        val daily = DailyWeather(
            time = listOf("2026-04-27"),
            weatherCode = listOf(0),
            temperature2mMax = listOf(25.0),
            temperature2mMin = listOf(15.0),
            sunrise = listOf("2026-04-27T09:30:00")
        )
        val result = weatherMapper.mapToDailyForecast(daily, utcOffsetSeconds)

        assertEquals(1, result.size)
        // Sunrise UTC "2026-04-27T09:30:00" - 5 hours = 04:30
        assertEquals("04:30", result[0].sunrise)
    }

    @Test
    fun `mapToDailyForecast handles time conversion with zero offset`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val utcOffsetSeconds = 0
        val daily = DailyWeather(
            time = listOf("2026-04-27"),
            weatherCode = listOf(0),
            temperature2mMax = listOf(25.0),
            temperature2mMin = listOf(15.0),
            sunrise = listOf("2026-04-27T09:30:00")
        )
        val result = weatherMapper.mapToDailyForecast(daily, utcOffsetSeconds)

        assertEquals(1, result.size)
        // Sunrise UTC "2026-04-27T09:30:00" + 0 hours = 09:30
        assertEquals("09:30", result[0].sunrise)
    }

    @Test
    fun `mapToDailyForecast processes multiple days correctly`() = runTest {
        coEvery { languagePreferenceRepository.getAppLocale() } returns Locale.ENGLISH

        val daily = DailyWeather(
            time = listOf("2026-04-27", "2026-04-28", "2026-04-29", "2026-04-30"),
            weatherCode = listOf(0, 1, 2, 3),
            temperature2mMax = listOf(25.0, 23.0, 22.0, 24.0),
            temperature2mMin = listOf(15.0, 14.0, 13.0, 14.0)
        )
        val result = weatherMapper.mapToDailyForecast(daily, 0)

        assertEquals(4, result.size)
        assertEquals(0, result[0].weatherCode)
        assertEquals(1, result[1].weatherCode)
        assertEquals(2, result[2].weatherCode)
        assertEquals(3, result[3].weatherCode)
    }

    private fun createTestDailyWeather(): DailyWeather {
        return DailyWeather(
            time = listOf("2026-04-27", "2026-04-28"),
            weatherCode = listOf(0, 1),
            temperature2mMax = listOf(25.0, 23.0),
            temperature2mMin = listOf(15.0, 14.0),
            precipitationSum = listOf(0.0, 2.5),
            sunrise = listOf("2026-04-27T04:30:00", "2026-04-28T04:29:00"),
            sunset = listOf("2026-04-27T20:15:00", "2026-04-28T20:16:00"),
            windspeed10mMax = listOf(10.0, 12.0),
            winddirection10mDominant = listOf(180, 200),
            uvIndexMax = listOf(5.0, 3.0)
        )
    }

    private fun createTestDailyWeatherWithNulls(): DailyWeather {
        return DailyWeather(
            time = listOf("2026-04-27"),
            weatherCode = listOf(null),
            temperature2mMax = listOf(null),
            temperature2mMin = listOf(null),
            precipitationSum = listOf(null),
            sunrise = listOf(null),
            sunset = listOf(null),
            windspeed10mMax = listOf(null),
            winddirection10mDominant = listOf(null),
            uvIndexMax = listOf(null)
        )
    }
}
