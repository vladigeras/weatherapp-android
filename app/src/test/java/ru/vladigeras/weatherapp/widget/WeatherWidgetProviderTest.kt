package ru.vladigeras.weatherapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.BroadcastReceiver
import java.util.concurrent.atomic.AtomicInteger
import java.util.Collections
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import io.mockk.coEvery
import io.mockk.Runs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.just
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var provider: WeatherWidgetProvider
    private lateinit var mockManager: AppWidgetManager
    private val receiverCompletions = mutableListOf<CompletableDeferred<Unit>>()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        provider = WeatherWidgetProvider()
        mockManager = mockk(relaxed = true)

        mockkObject(WidgetPrefsManager)
        coEvery { WidgetPrefsManager.getData(context) } returns null
    }

    @After
    fun tearDown() = runBlocking {
        try {
            withTimeout(5000) { receiverCompletions.forEach { it.await() } }
        } finally {
            unmockkAll()
        }
    }

    private fun receiverCompletion(pending: BroadcastReceiver.PendingResult = mockk(relaxed = true)): CompletableDeferred<Unit> {
        val completed = CompletableDeferred<Unit>()
        receiverCompletions.add(completed)
        every { pending.finish() } answers { completed.complete(Unit) }
        BroadcastReceiver::class.java.getDeclaredMethod("setPendingResult", BroadcastReceiver.PendingResult::class.java)
            .apply { isAccessible = true }.invoke(provider, pending)
        return completed
    }

    private suspend fun updateReceiver(update: () -> Unit) {
        val completed = receiverCompletion()
        update()
        withTimeout(5000) { completed.await() }
    }

    @Test
    fun `provider can be instantiated`() = runBlocking {
        assertNotNull(provider)
    }

    @Test
    fun `cleared weather displays no data in both widget layouts`() = runBlocking {
        for (width in listOf(180, 300)) {
            val completed = CompletableDeferred<RemoteViews>()
            every { mockManager.updateAppWidget(1, any<RemoteViews>()) } answers { completed.complete(secondArg()) }
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
            }
            updateReceiver { provider.onAppWidgetOptionsChanged(context, mockManager, 1, options) }
            val views = withTimeout(2000) { completed.await() }
            val root = views.apply(context, FrameLayout(context))
            val container = if (width < 260) R.id.vertical_container else R.id.horizontal_container
            val city = if (width < 260) R.id.widget_city else R.id.widget_city_horizontal
            assertEquals(View.VISIBLE, root.findViewById<View>(container).visibility)
            assertEquals(context.getString(R.string.widget_no_data), root.findViewById<TextView>(city).text)
            assertEquals(View.GONE, root.findViewById<View>(R.id.widget_icon).visibility)
        }
    }

    @Test
    fun `onUpdate reads widget options for each widget id`() = runBlocking {
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

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1, 2)) }

        verify { mockManager.getAppWidgetOptions(1) }
        verify { mockManager.getAppWidgetOptions(2) }
        verify { mockManager.updateAppWidget(1, any()) }
        verify { mockManager.updateAppWidget(2, any()) }
    }

    @Test
    fun `onUpdate uses fallback defaults when options bundle is empty`() = runBlocking {
        val emptyOptions = Bundle.EMPTY
        every { mockManager.getAppWidgetOptions(42) } returns emptyOptions

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(42)) }

        verify { mockManager.getAppWidgetOptions(42) }
        verify { mockManager.updateAppWidget(42, any()) }
    }

    @Test
    fun `onAppWidgetOptionsChanged uses provided sizes`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }

        updateReceiver { provider.onAppWidgetOptionsChanged(context, mockManager, 1, options) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `updateAllWidgets reads widget options for each widget`() = runBlocking {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns mockManager
        every { mockManager.getAppWidgetIds(any<ComponentName>()) } returns intArrayOf(10, 20)

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }
        every { mockManager.getAppWidgetOptions(any()) } returns options

        WeatherWidgetProvider.updateAllWidgets(context)

        verify(timeout = 2000) { mockManager.getAppWidgetOptions(10) }
        verify(timeout = 2000) { mockManager.getAppWidgetOptions(20) }
        verify(timeout = 2000) { mockManager.updateAppWidget(10, any()) }
        verify(timeout = 2000) { mockManager.updateAppWidget(20, any()) }
    }

    @Test
    fun `updateAllWidgets does nothing when no widgets exist`() = runBlocking {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns mockManager
        every { mockManager.getAppWidgetIds(any<ComponentName>()) } returns intArrayOf()

        WeatherWidgetProvider.updateAllWidgets(context)

        verify(exactly = 0) { mockManager.updateAppWidget(any<Int>(), any()) }
    }

    @Test
    fun `onUpdate with wide widget passes correct dimensions`() = runBlocking {
        val wideOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        }
        every { mockManager.getAppWidgetOptions(1) } returns wideOptions

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall widget with 2 cell height triggers height scaling`() = runBlocking {
        val tallOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
        }
        every { mockManager.getAppWidgetOptions(1) } returns tallOptions

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall widget with height exactly 140 triggers height scaling`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `short widget with height 139 does not trigger height scaling`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 139)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall and narrow widget uses tall narrow breakpoint`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall and medium width widget uses tall medium breakpoint`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `tall and borderline width uses tall narrow breakpoint`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 210)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)
        }
        every { mockManager.getAppWidgetOptions(1) } returns options

        updateReceiver { provider.onUpdate(context, mockManager, intArrayOf(1)) }

        verify { mockManager.updateAppWidget(1, any()) }
    }

    @Test
    fun `updateAllWidgets reads options for tall widget`() = runBlocking {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(context) } returns mockManager
        every { mockManager.getAppWidgetIds(any<ComponentName>()) } returns intArrayOf(5)

        val tallOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200)
        }
        every { mockManager.getAppWidgetOptions(5) } returns tallOptions

        WeatherWidgetProvider.updateAllWidgets(context)

        verify(timeout = 2000) { mockManager.getAppWidgetOptions(5) }
        verify(timeout = 2000) { mockManager.updateAppWidget(5, any()) }
    }

    @Test
    fun `onAppWidgetOptionsChanged with tall widget`() = runBlocking {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200)
        }

        updateReceiver { provider.onAppWidgetOptionsChanged(context, mockManager, 1, options) }

        verify { mockManager.updateAppWidget(1, any()) }
    }
    @Test
    fun `receiver remains responsive and publishes concurrent updates in order`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val reads = AtomicInteger()
        val cities = Collections.synchronizedList(mutableListOf<String>())
        val pending = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
        val firstFinished = receiverCompletion(pending)
        lateinit var secondFinished: CompletableDeferred<Unit>
        coEvery { WidgetPrefsManager.getData(context) } coAnswers {
            if (reads.incrementAndGet() == 1) {
                entered.complete(Unit)
                release.await()
                WidgetPrefsManager.WidgetData("Old", "18°C", 0, 1)
            } else WidgetPrefsManager.WidgetData("New", "27°C", 3, 1)
        }
        every { mockManager.getAppWidgetOptions(1) } returns Bundle.EMPTY
        every { mockManager.updateAppWidget(1, any<RemoteViews>()) } answers {
            val root = secondArg<RemoteViews>().apply(context, FrameLayout(context))
            cities.add(root.findViewById<TextView>(R.id.widget_city).text.toString())
        }
        try {
            provider.onUpdate(context, mockManager, intArrayOf(1))
            withTimeout(2000) { entered.await() }
            verify(exactly = 0) { pending.finish() }
            secondFinished = receiverCompletion()
            provider.onUpdate(context, mockManager, intArrayOf(1))
            delay(100)
            assertEquals(1, reads.get())
        } finally {
            release.complete(Unit)
        }
        withTimeout(5000) { firstFinished.await(); secondFinished.await() }
        assertEquals(listOf("Old", "New"), cities.toList())
        verify(exactly = 1) { pending.finish() }
    }
    @Test
    fun `latest resize remains the final layout`() = runBlocking {
        val readingOldOptions = CompletableDeferred<Unit>()
        val releaseOldOptions = CountDownLatch(1)
        val newerPublished = CompletableDeferred<Unit>()
        val layouts = Collections.synchronizedList(mutableListOf<Int>())
        val oldOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }
        val newOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 350)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }
        every { mockManager.getAppWidgetOptions(1) } answers {
            readingOldOptions.complete(Unit)
            check(releaseOldOptions.await(5, TimeUnit.SECONDS))
            oldOptions
        }
        every { mockManager.updateAppWidget(1, any<RemoteViews>()) } answers {
            val root = secondArg<RemoteViews>().apply(context, FrameLayout(context))
            layouts.add(root.findViewById<View>(R.id.horizontal_container).visibility)
            if (layouts.size == 1) newerPublished.complete(Unit)
        }
        val firstFinished = receiverCompletion()
        lateinit var secondFinished: CompletableDeferred<Unit>
        try {
            provider.onUpdate(context, mockManager, intArrayOf(1))
            withTimeout(2000) { readingOldOptions.await() }
            secondFinished = receiverCompletion()
            provider.onAppWidgetOptionsChanged(context, mockManager, 1, newOptions)
            withTimeoutOrNull(500) { newerPublished.await() }
        } finally {
            releaseOldOptions.countDown()
        }
        withTimeout(5000) { firstFinished.await(); secondFinished.await() }
        assertEquals(View.VISIBLE, layouts.last())
    }
}
