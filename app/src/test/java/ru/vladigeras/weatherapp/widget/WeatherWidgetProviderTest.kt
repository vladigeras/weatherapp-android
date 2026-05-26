package ru.vladigeras.weatherapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
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
    private lateinit var mockManager: AppWidgetManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        provider = WeatherWidgetProvider()
        mockManager = mockk(relaxed = true)

        mockkObject(WidgetPrefsManager)
        every { WidgetPrefsManager.hasData(context) } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `provider can be instantiated`() {
        assertNotNull(provider)
    }

    @Test
    fun `onUpdate reads widget options for each widget id`() {
        val options1 = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }
        val options2 = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 70)
        }

        every { mockManager.getAppWidgetOptions(1) } returns options1
        every { mockManager.getAppWidgetOptions(2) } returns options2

        provider.onUpdate(context, mockManager, intArrayOf(1, 2))

        verify { mockManager.getAppWidgetOptions(1) }
        verify { mockManager.getAppWidgetOptions(2) }
        verify { mockManager.updateAppWidget(1, any()) }
        verify { mockManager.updateAppWidget(2, any()) }
    }

    @Test
    fun `onUpdate uses fallback defaults when options bundle is empty`() {
        val emptyOptions = Bundle.EMPTY
        every { mockManager.getAppWidgetOptions(42) } returns emptyOptions

        provider.onUpdate(context, mockManager, intArrayOf(42))

        verify { mockManager.getAppWidgetOptions(42) }
        verify { mockManager.updateAppWidget(42, any()) }
    }

    @Test
    fun `onAppWidgetOptionsChanged uses provided sizes`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }

        provider.onAppWidgetOptionsChanged(context, mockManager, 1, options)

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `updateAllWidgets reads widget options for each widget`() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns mockManager
        every { mockManager.getAppWidgetIds(any<ComponentName>()) } returns intArrayOf(10, 20)

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }
        every { mockManager.getAppWidgetOptions(any()) } returns options

        WeatherWidgetProvider.updateAllWidgets(context)

        verify { mockManager.getAppWidgetOptions(10) }
        verify { mockManager.getAppWidgetOptions(20) }
        verify { mockManager.updateAppWidget(10, any()) }
        verify { mockManager.updateAppWidget(20, any()) }
    }

    @Test
    fun `updateAllWidgets does nothing when no widgets exist`() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns mockManager
        every { mockManager.getAppWidgetIds(any<ComponentName>()) } returns intArrayOf()

        WeatherWidgetProvider.updateAllWidgets(context)

        verify(exactly = 0) { mockManager.updateAppWidget(any<Int>(), any()) }
    }

    @Test
    fun `onUpdate with wide widget passes correct dimensions`() {
        val wideOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        }
        every { mockManager.getAppWidgetOptions(1) } returns wideOptions

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall widget with 2 cell height triggers height scaling`() {
        val tallOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
        }
        every { mockManager.getAppWidgetOptions(1) } returns tallOptions

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall widget with height exactly 140 triggers height scaling`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `short widget with height 139 does not trigger height scaling`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 139)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall and narrow widget uses tall narrow breakpoint`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall and medium width widget uses tall medium breakpoint`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall and borderline width uses tall narrow breakpoint`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 210)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        provider.onUpdate(context, mockManager, intArrayOf(1))

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `updateAllWidgets reads options for tall widget`() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns mockManager
        every { mockManager.getAppWidgetIds(any<ComponentName>()) } returns intArrayOf(5)

        val tallOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200)
        }
        every { mockManager.getAppWidgetOptions(5) } returns tallOptions

        WeatherWidgetProvider.updateAllWidgets(context)

        verify { mockManager.getAppWidgetOptions(5) }
        verify { mockManager.updateAppWidget(5, any()) }
    }

    @Test
    fun `onAppWidgetOptionsChanged with tall widget`() {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200)
        }

        provider.onAppWidgetOptionsChanged(context, mockManager, 1, options)

        verify { mockManager.updateAppWidget(1, any()) }
    }
}
