package ru.vladigeras.weatherapp.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.vladigeras.weatherapp.di.NetworkModule
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.time.TimeSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HttpTimeoutTest {
    @Test
    fun `CIO fails within the connection budget when no server is available`() {
        val port = ServerSocket(0).use { it.localPort }
        val client = NetworkModule.provideHttpClient(NetworkModule.provideJson())
        val start = TimeSource.Monotonic.markNow()
        try {
            val result = runCatching { runBlocking { client.get("http://127.0.0.1:$port").bodyAsText() } }
            assertTrue(result.isFailure)
            assertTrue(start.elapsedNow().inWholeMilliseconds < 5_000)
        } finally {
            client.close()
        }
    }

    @Test
    fun `CIO stops when response body stalls for ten seconds`() {
        val (failure, elapsed) = incompleteResponse(drip = false)
        assertTrue(generateSequence(failure) { it.cause }.any { it is io.ktor.client.network.sockets.SocketTimeoutException })
        assertTrue("Elapsed: $elapsed", elapsed in 9_000..14_000)
    }

    @Test
    fun `CIO stops a continuously arriving response at fifteen seconds`() {
        val (failure, elapsed) = incompleteResponse(drip = true)
        assertTrue(generateSequence(failure) { it.cause }.any { it is HttpRequestTimeoutException })
        assertTrue("Elapsed: $elapsed", elapsed in 14_000..19_000)
    }

    private fun incompleteResponse(drip: Boolean): Pair<Throwable, Long> {
        val connection = AtomicReference<Socket>()
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val worker = thread(isDaemon = true) {
                runCatching {
                    server.accept().use { socket ->
                        connection.set(socket)
                        val input = socket.getInputStream().bufferedReader()
                        while (!input.readLine().isNullOrEmpty()) {}
                        val output = socket.getOutputStream()
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\n{".toByteArray())
                        output.flush()
                        while (!socket.isClosed) {
                            Thread.sleep(1_000)
                            if (drip) {
                                output.write(' '.code)
                                output.flush()
                            }
                        }
                    }
                }
            }
            val client = NetworkModule.provideHttpClient(NetworkModule.provideJson())
            val start = TimeSource.Monotonic.markNow()
            try {
                val result = runCatching {
                    runBlocking { client.get("http://127.0.0.1:${server.localPort}").bodyAsText() }
                }
                return requireNotNull(result.exceptionOrNull()) to start.elapsedNow().inWholeMilliseconds
            } finally {
                client.close()
                connection.get()?.close()
                server.close()
                worker.join(1_500)
            }
        }
    }
}
