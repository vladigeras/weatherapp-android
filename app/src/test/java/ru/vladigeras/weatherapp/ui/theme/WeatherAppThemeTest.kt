package ru.vladigeras.weatherapp.ui.theme

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherAppThemeTest {
    @Test
    fun themeColorsAndSystemIconsFollowTheSelectedTheme() = runBlocking {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val view = View(activity)
        val recomposer = Recomposer(coroutineContext)
        try {
            for (dark in listOf(false, true, false)) {
                val composition = Composition(object : AbstractApplier<Unit>(Unit) {
                    override fun insertTopDown(index: Int, instance: Unit) = Unit
                    override fun insertBottomUp(index: Int, instance: Unit) = Unit
                    override fun remove(index: Int, count: Int) = Unit
                    override fun move(from: Int, to: Int, count: Int) = Unit
                    override fun onClear() = Unit
                }, recomposer)
                try {
                    var primary = Color.Unspecified
                    var background = Color.Unspecified
                    composition.setContent {
                        CompositionLocalProvider(LocalView provides view) {
                            WeatherAppTheme(darkTheme = dark) {
                                primary = MaterialTheme.colorScheme.primary
                                background = MaterialTheme.colorScheme.background
                            }
                        }
                    }
                    assertEquals(if (dark) Color(0xFF90CAF9) else Color(0xFF1976D2), primary)
                    assertEquals(if (dark) Color(0xFF121212) else Color(0xFFFAFAFA), background)
                    val insets = WindowCompat.getInsetsController(activity.window, view)
                    assertEquals(dark, insets.isAppearanceLightStatusBars)
                    assertEquals(!dark, insets.isAppearanceLightNavigationBars)
                } finally {
                    composition.dispose()
                }
            }
        } finally {
            recomposer.cancel()
            controller.pause().stop().destroy()
        }
    }
}
