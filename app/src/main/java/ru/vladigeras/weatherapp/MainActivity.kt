package ru.vladigeras.weatherapp

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.vladigeras.weatherapp.ui.LocationSelectionScreen
import ru.vladigeras.weatherapp.ui.WeatherScreen
import ru.vladigeras.weatherapp.ui.theme.WeatherAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(base: Context) {
        val locale = getSavedLocale(base)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            val newContext = base.createConfigurationContext(config)
            super.attachBaseContext(newContext)
        } else {
            @Suppress("DEPRECATION")
            val legacyConfig = base.resources.configuration
            legacyConfig.setLocale(locale)
            @Suppress("DEPRECATION")
            base.resources.updateConfiguration(legacyConfig, base.resources.displayMetrics)
            super.attachBaseContext(base)
        }
    }
    
    private fun getSavedLocale(context: Context): java.util.Locale {
        val prefs = context.getSharedPreferences("weatherapp_prefs", Context.MODE_PRIVATE)
        val ordinal = prefs.getInt("language_preference", -1)

        return when (ordinal) {
            1 -> java.util.Locale("ru", "RU")
            2 -> java.util.Locale.ENGLISH
            else -> {
                val deviceLang = java.util.Locale.getDefault()
                if (deviceLang.language == "ru") {
                    java.util.Locale("ru", "RU")
                } else {
                    java.util.Locale.ENGLISH
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    WeatherAppNavHost(navController = navController)
                }
            }
        }
    }
}

@Composable
fun WeatherAppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "weather"
    ) {
        composable("weather") { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            WeatherScreen(
                savedStateHandle = savedStateHandle,
                onNavigateToLocationSelection = {
                    navController.navigate("location_selection")
                }
            )
        }
        
        composable("location_selection") {
            LocationSelectionScreen(
                onLocationChosen = { latitude, longitude ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("latitude", latitude)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("longitude", longitude)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }
    }
}