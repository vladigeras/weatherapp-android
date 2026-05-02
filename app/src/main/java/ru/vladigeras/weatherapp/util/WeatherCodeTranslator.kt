package ru.vladigeras.weatherapp.util

import android.content.Context
import ru.vladigeras.weatherapp.R

/**
 * Utility class for translating weather codes to human-readable strings based on locale.
 * Uses string array defined in resources for translation.
 * Format: "code|description" (pipe-separated)
 */
object WeatherCodeTranslator {
    
    /**
     * Translates a weather code to a human-readable string based on the app's locale.
     * 
     * @param context The application context
     * @param weatherCode The numeric weather code from the API
     * @return Translated weather description or "Unknown" if code not found
     */
    fun translate(context: Context, weatherCode: Int): String {
        val entries = context.resources.getStringArray(R.array.weather_codes)
        
        for (entry in entries) {
            val parts = entry.split("|")
            if (parts.size == 2) {
                val code = parts[0].toIntOrNull()
                if (code == weatherCode) {
                    return parts[1]
                }
            }
        }
        
        return context.getString(R.string.unknown_weather)
    }
    
    /**
     * Returns a list of weather code translation entries.
     * Each entry is in format "code|description".
     * Useful for Compose where we can't use Context directly.
     * 
     * @param context The application context
     * @return List of string entries
     */
    fun getWeatherCodeEntries(context: Context): List<String> {
        return context.resources.getStringArray(R.array.weather_codes).toList()
    }
}
