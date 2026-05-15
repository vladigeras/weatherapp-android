package ru.vladigeras.weatherapp.widget

import android.content.Context
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var provider: WeatherWidgetProvider

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        provider = WeatherWidgetProvider()
    }

    @Test
    fun `provider can be instantiated`() {
        // Just verify that the class can be instantiated without errors
        assertNotNull(provider)
    }
}