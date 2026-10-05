package ru.vladigeras.weatherapp.ui

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import ru.vladigeras.weatherapp.data.WeatherDisplayPrefs
import ru.vladigeras.weatherapp.data.WeatherProviderId
import ru.vladigeras.weatherapp.data.ProviderCapabilities
import ru.vladigeras.weatherapp.repository.LanguagePreference
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.ui.theme.WeatherAppTheme
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ru-rRU")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeatherScreenLayoutTest {
    @Test
    fun providersShareForecastHeadingAndOnlyYrSettingsShowCredits() {
        for (provider in WeatherProviderId.entries) {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val state = WeatherUiState.Success(18.0, null, null, null, -3, null, "Europe/Moscow", "Moscow", "°C",
                hourlyForecast = listOf(HourlyForecast("21:00", -3, 18.0, null, null)),
                prefs = WeatherDisplayPrefs(provider = provider, showForecastDays = false))
            val viewModel = mockk<WeatherViewModel>(relaxed = true)
            every { viewModel.uiState } returns MutableStateFlow(state)
            every { viewModel.showUpdateToast } returns MutableStateFlow(false)
            try {
                activity.setContent { WeatherAppTheme { WeatherScreen(SavedStateHandle(), viewModel = viewModel) } }
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                repeat(3) {
                    ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
                    root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, 800, 1600)
                }
                val owner = views(root).firstNotNullOf { view -> view.javaClass.methods.firstOrNull { it.name == "getSemanticsOwner" }?.invoke(view) as? SemanticsOwner }
                val texts = nodes(owner.unmergedRootSemanticsNode).flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty().asSequence() }.map { it.text }.toList()
                assertTrue("$provider: $texts", texts.contains("Прогноз по времени"))
                assertTrue(texts.none { "часовом поясе" in it || "шаг" in it })
                assertTrue(texts.none { "MET Norway" in it || "CC BY 4.0" in it })
                assertTrue(texts.none { "км/ч" in it || "%" in it || "Ощущается" in it })
                val settingsViewModel = mockk<SettingsViewModel>(relaxed = true)
                every { settingsViewModel.localPrefs } returns MutableStateFlow(state.prefs)
                every { settingsViewModel.hasChanges } returns MutableStateFlow(false)
                every { settingsViewModel.languagePreference } returns MutableStateFlow(LanguagePreference.SYSTEM)
                every { settingsViewModel.capabilities(any()) } returns if (provider == WeatherProviderId.YR)
                    ProviderCapabilities(0, dailyPrecipitation = false, dailyUv = false, dailyWind = false, sunTimes = false)
                else ProviderCapabilities(16)
                activity.setContent { WeatherAppTheme { SettingsScreen(settingsViewModel) } }
                repeat(3) {
                    ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
                    root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(5000, View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, 800, 5000)
                }
                val settingsTexts = nodes(owner.unmergedRootSemanticsNode).flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty().asSequence() }.map { it.text }.toList()
                assertEquals(provider == WeatherProviderId.YR, settingsTexts.contains(activity.getString(R.string.met_attribution)))
                assertEquals(provider == WeatherProviderId.YR, settingsTexts.contains("CC BY 4.0"))
                assertEquals(provider != WeatherProviderId.YR, settingsTexts.contains(activity.getString(R.string.daily_forecast_days)))
                if (provider == WeatherProviderId.YR) assertTrue(nodes(owner.unmergedRootSemanticsNode).any { node ->
                    node.config.contains(SemanticsProperties.Disabled) && nodes(node).any {
                        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == activity.getString(R.string.daily_forecast) } == true
                    }
                })
            } finally { controller.pause().stop().destroy() }
        }
    }

    @Test
    fun largeFontKeepsWindValueOnOneLineInBothOrientations() {
        for ((width, height) in listOf(360 to 800, 800 to 360)) {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val state = WeatherUiState.Success(18.0, 17.0, 73, 12.0, 2, 1, "Europe/Moscow", "Moscow", "°C",
                emptyList(), emptyList(), WeatherDisplayPrefs(showHourlyForecast = false, showForecastDays = false))
            val viewModel = mockk<WeatherViewModel>(relaxed = true)
            every { viewModel.uiState } returns MutableStateFlow(state)
            every { viewModel.showUpdateToast } returns MutableStateFlow(false)
            try {
                activity.setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                        WeatherAppTheme { WeatherScreen(SavedStateHandle(), viewModel = viewModel) }
                    }
                }
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                repeat(3) {
                    ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, width, height)
                }
                val owner = views(root).firstNotNullOf { view ->
                    view.javaClass.methods.firstOrNull { it.name == "getSemanticsOwner" }?.invoke(view) as? SemanticsOwner
                }
                for (value in listOf("17°C", "73%", "12 км/ч")) {
                    val node = nodes(owner.unmergedRootSemanticsNode).first {
                        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == value } == true
                    }
                    val layouts = mutableListOf<TextLayoutResult>()
                    assertTrue("$value at $width: missing text layout", node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts))
                    assertEquals("$value at $width", 1, layouts.single().lineCount)
                    assertTrue("$value at $width: text exceeds its bounds",
                        layouts.single().multiParagraph.intrinsics.maxIntrinsicWidth <= layouts.single().size.width)
                    assertTrue("$value at $width: ${node.boundsInRoot}", node.boundsInRoot.left >= 0f && node.boundsInRoot.right <= width)
                }
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(views(view.getChildAt(index)))
    }

    private fun nodes(node: SemanticsNode): Sequence<SemanticsNode> = sequence {
        yield(node)
        node.children.forEach { yieldAll(nodes(it)) }
    }
}
