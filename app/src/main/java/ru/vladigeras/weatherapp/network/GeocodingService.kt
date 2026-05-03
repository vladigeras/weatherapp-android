package ru.vladigeras.weatherapp.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.vladigeras.weatherapp.BuildConfig
import javax.inject.Inject

class GeocodingService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun searchCity(query: String, languageCode: String): Result<GeocodingResponse> {
        return try {
            val response = httpClient.get("${BuildConfig.GEOCODING_API_URL}/search") {
                url {
                    parameters.append("name", query)
                    parameters.append("count", "5")
                    parameters.append("language", languageCode)
                    parameters.append("format", "json")
                }
            }.body<GeocodingResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    val admin1: String? = null
)