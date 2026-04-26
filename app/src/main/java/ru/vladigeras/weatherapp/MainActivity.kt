package ru.vladigeras.weatherapp

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