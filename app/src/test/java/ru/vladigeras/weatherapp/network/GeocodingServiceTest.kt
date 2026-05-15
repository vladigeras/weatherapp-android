package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GeocodingServiceTest {

    private fun createService(mockEngine: MockEngine): GeocodingService {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return GeocodingService(httpClient)
    }

    @Test
    fun searchCity_success_parsesResponse() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "results": [
                            {"id": 1, "name": "Moscow", "latitude": 55.75, "longitude": 37.62, "country": "Russia", "country_code": "RU"}
                        ]
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val service = createService(mockEngine)
        val result = service.searchCity("Moscow", "en")

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals(1, response?.results?.size)
        assertEquals("Moscow", response?.results?.first()?.name)
    }

    @Test
    fun searchCity_parsesMultipleResults() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "results": [
                            {"id": 1, "name": "Moscow", "latitude": 55.75, "longitude": 37.62, "country": "Russia", "country_code": "RU"},
                            {"id": 2, "name": "Moscow", "latitude": 41.7, "longitude": -83.5, "country": "United States", "country_code": "US", "admin1": "Ohio"},
                            {"id": 3, "name": "Moscow", "latitude": 55.7, "longitude": 37.6, "country": "Russia", "country_code": "RU"}
                        ]
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val service = createService(mockEngine)
        val result = service.searchCity("Moscow", "en")

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(3, response?.results?.size)
        assertEquals("Moscow", response?.results?.first()?.name)
        assertEquals("Moscow", response?.results?.get(1)?.name)
        assertEquals("Moscow", response?.results?.get(2)?.name)
    }

    @Test
    fun searchCity_emptyResponse_returnsEmptyList() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"results":[]}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val service = createService(mockEngine)
        val result = service.searchCity("UnknownCity", "en")

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertTrue(response?.results?.isEmpty() ?: false)
    }

    @Test
    fun searchCity_invalidJson_returnsFailure() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "invalid json",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val service = createService(mockEngine)
        val result = service.searchCity("Moscow", "en")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun searchCity_networkError_returnsFailure() = runTest {
        val mockEngine = MockEngine { request ->
            throw IOException("Network error")
        }

        val service = createService(mockEngine)
        val result = service.searchCity("Moscow", "en")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
