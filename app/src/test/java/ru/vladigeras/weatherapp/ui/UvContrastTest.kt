package ru.vladigeras.weatherapp.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UvContrastTest {
    @Test
    fun uvLevelsKeepTheirMeaningAndReadableContrastInBothThemes() {
        val levels = listOf(
            listOf(0.0, 2.0) to 85f..150f,
            listOf(2.1, 3.0, 5.0) to 40f..65f,
            listOf(5.1, 6.0, 7.0) to 20f..40f,
            listOf(8.0, 11.0) to 0f..15f
        )
        for (dark in listOf(false, true)) {
            val background = if (dark) Color(0xFF1E1E1E) else Color.White
            for ((values, hueRange) in levels) for (uv in values) {
                val color = uvIndexColor(uv, dark).toArgb()
                assertTrue("UV $uv dark=$dark", ColorUtils.calculateContrast(color, background.toArgb()) >= 4.5)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(color, hsl)
                assertTrue("UV $uv hue=${hsl[0]}", hsl[0] in hueRange)
            }
        }
    }
}
