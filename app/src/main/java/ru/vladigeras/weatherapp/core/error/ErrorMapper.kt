package ru.vladigeras.weatherapp.core.error

import android.content.Context
import kotlinx.coroutines.CancellationException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import ru.vladigeras.weatherapp.R
import java.io.IOException

object ErrorMapper {
    
    fun mapToUiMessage(throwable: Throwable, context: Context): String {
        return try {
            when (throwable) {
                is SecurityException -> context.getString(R.string.location_permission_required)
                is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException,
                is java.net.SocketTimeoutException -> context.getString(R.string.request_timed_out)
                is ResponseException -> context.getString(R.string.service_unavailable)
                is IOException -> context.getString(R.string.connection_error)
                is CancellationException -> throwable.message.orEmpty()
                else -> context.getString(R.string.something_went_wrong)
            }
        } catch (e: Exception) {
            throwable.message ?: "Unknown error"
        }
    }
}
