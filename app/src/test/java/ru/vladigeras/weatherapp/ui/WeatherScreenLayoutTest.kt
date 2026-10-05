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
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.ui.theme.WeatherAppTheme
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ru-rRU")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeatherScreenLayoutTest {
    @Test
    fun yrShowsPhoneTimeAndCreditWhileMissingCurrentValuesStayHidden() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val state = WeatherUiState.Success(18.0, null, null, null, -3, null, "Europe/Moscow", "Moscow", "°C",
            hourlyForecast = listOf(HourlyForecast("21:00", -3, 18.0, null, null)),
            prefs = WeatherDisplayPrefs(provider = WeatherProviderId.YR, showForecastDays = false))
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
            assertTrue(texts.contains(activity.getString(R.string.phone_forecast_time)))
            assertTrue(texts.contains(activity.getString(R.string.met_attribution)))
            assertTrue(texts.contains("CC BY 4.0"))
            assertTrue(texts.none { "км/ч" in it || "%" in it || "Ощущается" in it })
        } finally { controller.pause().stop().destroy() }
    }

    @Test
    fun largeFontKeepsWindValueOnOneLineInBothOrientations() {
        for ((width, height) in listOf(360 to 800, 800 to 360)) {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val state = WeatherUiState.Success(18.0, 17.0, 73, 12.0, 2, 1, "Europe/Moscow", "Moscow", "°C",
                emptyList(), emptyList(), WeatherDisplayPrefs(showHourlyForecast = false, showForecastDays = false), 1)
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
