package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.vladigeras.weatherapp.repository.LanguagePreferenceRepository
import java.io.IOException

class GeocodingServiceTest {

    private fun createService(
        mockEngine: MockEngine,
        languagePreferenceRepository: LanguagePreferenceRepository = mockk { every { runBlocking { getEffectiveLocaleCode() } } returns "en" }
    ): GeocodingService {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return GeocodingService(httpClient, languagePreferenceRepository)
    }

    @Test
    fun searchCity_success_parsesResponse() = runBlocking {
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
        val result = service.searchCity("Moscow")
        
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals(1, response?.results?.size)
        assertEquals("Moscow", response?.results?.first()?.name)
    }

    @Test
    fun searchCity_emptyResponse_returnsEmptyList() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"results":[]}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        
        val service = createService(mockEngine)
        val result = service.searchCity("UnknownCity")
        
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertTrue(response?.results?.isEmpty() ?: false)
    }

    @Test
    fun searchCity_networkError_returnsFailure() = runBlocking {
        val mockEngine = MockEngine { request ->
            throw IOException("Network error")
        }
        
        val service = createService(mockEngine)
        val result = service.searchCity("Moscow")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
