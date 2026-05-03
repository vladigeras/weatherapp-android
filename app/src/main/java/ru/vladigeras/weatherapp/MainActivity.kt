package ru.vladigeras.weatherapp

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
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

    private var composeContext by mutableStateOf<ComponentActivity?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        composeContext = this
        setContent {
            composeContext?.let { ctx ->
                CompositionLocalProvider(LocalContext provides ctx) {
                    WeatherAppTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            val navController = rememberNavController()
                            WeatherAppNavHost(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        composeContext = this
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