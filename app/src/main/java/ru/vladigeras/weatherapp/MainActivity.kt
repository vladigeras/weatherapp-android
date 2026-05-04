package ru.vladigeras.weatherapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.vladigeras.weatherapp.ui.LocationSelectionScreen
import ru.vladigeras.weatherapp.ui.SettingsActivity
import ru.vladigeras.weatherapp.ui.WeatherScreen
import ru.vladigeras.weatherapp.ui.theme.WeatherAppTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val settingsLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra("LANGUAGE_CHANGED", false) == true) {
                            recreate()
                        }
                    }

                    WeatherAppNavHost(
                        navController = navController,
                        onOpenSettings = {
                            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherAppNavHost(
    navController: NavHostController,
    onOpenSettings: () -> Unit
) {
    NavHost(navController = navController, startDestination = "weather") {
        composable("weather") { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            WeatherScreen(
                savedStateHandle = savedStateHandle,
                onNavigateToLocationSelection = { navController.navigate("location_selection") },
                onOpenSettings = onOpenSettings
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
