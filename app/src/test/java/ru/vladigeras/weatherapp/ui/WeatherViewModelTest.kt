package ru.vladigeras.weatherapp.ui

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.vladigeras.weatherapp.repository.WeatherRepository

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var weatherViewModel: WeatherViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        weatherRepository = mockk()
        weatherViewModel = WeatherViewModel(weatherRepository)
        Dispatchers.setMain(testDispatcher)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun executeOnMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean = true
        })
    }
    
    @Before
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
    
    @Test
    fun `initial state is Loading`() = runTest {
        val initialState = weatherViewModel.uiState.first()
        assertTrue(initialState is WeatherUiState.Loading)
    }
}