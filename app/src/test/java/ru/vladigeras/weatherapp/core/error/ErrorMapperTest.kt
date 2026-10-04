package ru.vladigeras.weatherapp.core.error

import io.ktor.client.plugins.HttpRequestTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.R
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ErrorMapperTest {
    @Test
    fun `timeouts are explained separately from connection failures`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(context.getString(R.string.request_timed_out), ErrorMapper.mapToUiMessage(HttpRequestTimeoutException("https://wttr.is", 15_000), context))
        assertEquals(context.getString(R.string.request_timed_out), ErrorMapper.mapToUiMessage(java.net.SocketTimeoutException(), context))
        assertEquals(context.getString(R.string.connection_error), ErrorMapper.mapToUiMessage(IOException("transfer failed"), context))
    }
}
