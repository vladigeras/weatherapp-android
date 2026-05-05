package ru.vladigeras.weatherapp.core.error

import android.content.Context
import kotlinx.coroutines.CancellationException
import ru.vladigeras.weatherapp.R
import java.io.IOException

object ErrorMapper {
    
    fun mapToUiMessage(throwable: Throwable, context: Context): String {
        return try {
            when (throwable) {
                is SecurityException -> context.getString(R.string.location_permission_required)
                is IOException -> context.getString(R.string.weather_error, "No internet")
                is CancellationException -> throwable.message.orEmpty()
                else -> context.getString(R.string.something_went_wrong)
            }
        } catch (e: Exception) {
            throwable.message ?: "Unknown error"
        }
    }
}