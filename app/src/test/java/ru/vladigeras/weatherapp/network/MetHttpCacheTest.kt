package ru.vladigeras.weatherapp.network

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.util.date.GMTDate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.BuildConfig
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.di.NetworkModule
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MetHttpCacheTest {
    private val clients = mutableListOf<HttpClient>()
    private val directory = Files.createTempDirectory("met-http-test").toFile()
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-10-03T17:25:00Z"), ZoneOffset.UTC)
    private val prefs = WeatherDisplayPrefs(provider = WeatherProviderId.YR, showForecastDays = false, showHourlyForecast = false)
    private fun provider(client: HttpClient) = MetWeatherProvider(client, json, clock)
    private fun met(base: HttpClient, child: String): HttpClient {
        val context = mockk<Context> { every { cacheDir } returns directory.resolve(child) }
        return NetworkModule.provideMetHttpClient(context, base).also(clients::add)
    }
    private fun date(offset: Long) = GMTDate(GMTDate().timestamp + offset * 1000).toHttpDate()

    @After fun cleanup(): Unit = runBlocking {
        val jobs = clients.map { it.coroutineContext[Job]!! }
        clients.forEach { it.close() }
        jobs.joinAll()
        directory.deleteRecursively()
    }

    @Test fun `gzip and deflate responses survive fresh HTTP cache and client recreation`() = runTest {
        for (encoding in listOf("gzip", "deflate")) {
            var calls = 0
            val output = ByteArrayOutputStream()
            val stream = if (encoding == "gzip") GZIPOutputStream(output) else DeflaterOutputStream(output)
            stream.use { it.write(MetWeatherProviderTest.fixture().encodeToByteArray()) }
            val base = HttpClient(MockEngine { request ->
                calls++
                assertEquals("WeatherappAndroid/${BuildConfig.VERSION_NAME} (https://github.com/vladigeras/weatherapp-android)", request.headers[HttpHeaders.UserAgent])
                assertEquals("gzip, deflate", request.headers[HttpHeaders.AcceptEncoding])
                assertEquals("application/json", request.headers[HttpHeaders.Accept])
                respond(output.toByteArray(), headers = headersOf(HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ContentEncoding to listOf(encoding), HttpHeaders.Expires to listOf(date(3600)),
                    HttpHeaders.LastModified to listOf(date(-60)), HttpHeaders.Vary to listOf("Accept, Accept-Encoding")))
            }).also(clients::add)
            val first = met(base, encoding)
            val weather = provider(first).getWeather(40.7, -74.0, prefs)
            assertEquals(weather, provider(first).getWeather(40.7, -74.0, prefs))
            first.close()
            first.coroutineContext[Job]!!.join()
            assertEquals(weather, provider(met(base, encoding)).getWeather(40.7, -74.0, prefs))
            assertEquals(1, calls)
        }
    }

    @Test fun `expired responses preserve Last Modified in conditional request and refresh cached body after 304`() = runTest {
        var calls = 0
        val modified = "Sat, 03 Oct 2026 00:00:00 GMT"
        val base = HttpClient(MockEngine { request ->
            calls++
            if (calls == 1) {
                assertNull(request.headers[HttpHeaders.IfModifiedSince])
                respond(MetWeatherProviderTest.fixture(), headers = headersOf(HttpHeaders.Expires to listOf(date(-1)), HttpHeaders.LastModified to listOf(modified)))
            } else {
                assertEquals(modified, request.headers[HttpHeaders.IfModifiedSince])
                respond("", HttpStatusCode.NotModified, headersOf(HttpHeaders.Expires, date(3600)))
            }
        }).also(clients::add)
        val adapter = provider(met(base, "conditional"))
        val initial = adapter.getWeather(40.7, -74.0, prefs)
        assertEquals(initial, adapter.getWeather(40.7, -74.0, prefs))
        assertEquals(initial, adapter.getWeather(40.7, -74.0, prefs))
        assertEquals(2, calls)
    }
}
