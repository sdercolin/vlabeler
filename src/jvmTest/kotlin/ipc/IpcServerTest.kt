package ipc

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ipc.IpcServer
import com.sdercolin.vlabeler.ipc.request.HeartbeatRequest
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.request.OpenOrCreateRequest
import com.sdercolin.vlabeler.ipc.response.HeartbeatResponse
import com.sdercolin.vlabeler.ipc.response.OpenOrCreateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assumptions
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import testutil.TestEnv
import java.io.IOException
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for [IpcServer] over a real ZeroMQ REQ/REP socket pair on `tcp://localhost:32342`.
 */
class IpcServerTest {

    private lateinit var scope: CoroutineScope
    private val servers = mutableListOf<IpcServer>()
    private val clientContexts = mutableListOf<ZContext>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        requirePortFree()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun teardown() {
        try {
            clientContexts.forEach { runCatching { it.close() } }
            clientContexts.clear()
            servers.forEach { runCatching { it.close() } }
            servers.clear()
        } finally {
            runCatching { scope.cancel() }
            Log.muted = false
        }
    }

    /**
     * [IpcServer.bind] swallows bind failures, which would make the test hang until timeout instead of failing
     * clearly, so check the fixed port up front. When the port is taken (typically by a running vLabeler instance
     * on a developer machine), skip the test instead of failing; CI machines never run the application, so these
     * tests are always executed there.
     */
    private fun requirePortFree() {
        val portFree = try {
            ServerSocket(PORT).close()
            true
        } catch (e: IOException) {
            false
        }
        Assumptions.assumeTrue(portFree) {
            "Port $PORT is already in use (probably by a running vLabeler instance); skipping the socket test."
        }
    }

    private fun isPortFree(): Boolean = try {
        ServerSocket(PORT).close()
        true
    } catch (e: IOException) {
        false
    }

    private fun startServer(requestFlow: MutableSharedFlow<IpcRequest>): IpcServer {
        val server = IpcServer(scope)
        servers.add(server)
        server.bind()
        server.startReceive(requestFlow)
        return server
    }

    private fun createClient(): ZMQ.Socket {
        val context = ZContext()
        clientContexts.add(context)
        val socket = context.createSocket(SocketType.REQ)
        socket.receiveTimeOut = TIMEOUT_MS.toInt()
        socket.sendTimeOut = TIMEOUT_MS.toInt()
        socket.linger = 0
        socket.connect("tcp://localhost:$PORT")
        return socket
    }

    /**
     * Collects requests emitted by the server into a channel. The default [MutableSharedFlow] drops emissions when
     * there is no subscriber, so this suspends until the subscription is actually active before returning.
     */
    private suspend fun subscribe(requestFlow: MutableSharedFlow<IpcRequest>): Channel<IpcRequest> {
        val received = Channel<IpcRequest>(Channel.UNLIMITED)
        scope.launch { requestFlow.collect { received.send(it) } }
        withTimeout(TIMEOUT_MS) { requestFlow.subscriptionCount.first { it > 0 } }
        return received
    }

    @Test
    fun `heartbeat request and response round trip over the socket`() = runBlocking {
        val requestFlow = MutableSharedFlow<IpcRequest>()
        val received = subscribe(requestFlow)
        startServer(requestFlow).let { server ->
            val client = createClient()

            assertTrue(client.send("""{"type":"Heartbeat","sentAt":42}"""), "Client failed to send the request")
            val request = withTimeout(TIMEOUT_MS) { received.receive() }
            val heartbeat = assertIs<HeartbeatRequest>(request)
            assertEquals(42L, heartbeat.sentAt)

            server.send(HeartbeatResponse(requestedAt = 42, sentAt = 100))
            val responseJson = assertNotNull(client.recvStr(), "Client did not receive a response within timeout")
            assertEquals("""{"type":"Heartbeat","requestedAt":42,"sentAt":100}""", responseJson)
        }
    }

    @Test
    fun `sequential requests of different types are served on the same socket`() = runBlocking {
        val requestFlow = MutableSharedFlow<IpcRequest>()
        val received = subscribe(requestFlow)
        val server = startServer(requestFlow)
        val client = createClient()

        // First exchange: heartbeat.
        assertTrue(client.send("""{"type":"Heartbeat","sentAt":1}"""), "Client failed to send the first request")
        val first = withTimeout(TIMEOUT_MS) { received.receive() }
        assertEquals(1L, assertIs<HeartbeatRequest>(first).sentAt)
        server.send(HeartbeatResponse(requestedAt = 1, sentAt = 2))
        assertEquals(
            """{"type":"Heartbeat","requestedAt":1,"sentAt":2}""",
            assertNotNull(client.recvStr(), "Client did not receive the first response within timeout"),
        )

        // Second exchange: openOrCreate on the same REQ/REP pair.
        val openOrCreateJson = """{"type":"OpenOrCreate","projectFile":"/tmp/ipc-test.lbp",""" +
            """"newProjectArgs":{"labelerName":"test-labeler"},"sentAt":7}"""
        assertTrue(client.send(openOrCreateJson), "Client failed to send the second request")
        val second = withTimeout(TIMEOUT_MS) { received.receive() }
        val openOrCreate = assertIs<OpenOrCreateRequest>(second)
        assertEquals("/tmp/ipc-test.lbp", openOrCreate.projectFile)
        assertEquals("test-labeler", openOrCreate.newProjectArgs.labelerName)
        assertEquals(7L, openOrCreate.sentAt)
        server.send(OpenOrCreateResponse(requestedAt = 7, sentAt = 8))
        assertEquals(
            """{"type":"OpenOrCreate","requestedAt":7,"sentAt":8}""",
            assertNotNull(client.recvStr(), "Client did not receive the second response within timeout"),
        )
    }

    @Test
    fun `close releases the port so a new server can bind and serve`() = runBlocking {
        val firstFlow = MutableSharedFlow<IpcRequest>()
        subscribe(firstFlow)
        val firstServer = startServer(firstFlow)
        firstServer.close()

        // ZeroMQ context termination is expected to release the port; poll with a deadline instead of sleeping
        // blindly so the test stays fast when the port is released immediately.
        withTimeout(TIMEOUT_MS) {
            while (!isPortFree()) {
                delay(50)
            }
        }

        val secondFlow = MutableSharedFlow<IpcRequest>()
        val received = subscribe(secondFlow)
        val secondServer = startServer(secondFlow)
        val client = createClient()

        assertTrue(client.send("""{"type":"Heartbeat","sentAt":9}"""), "Client failed to send the request")
        val request = withTimeout(TIMEOUT_MS) { received.receive() }
        assertEquals(9L, assertIs<HeartbeatRequest>(request).sentAt)
        secondServer.send(HeartbeatResponse(requestedAt = 9, sentAt = 10))
        assertEquals(
            """{"type":"Heartbeat","requestedAt":9,"sentAt":10}""",
            assertNotNull(client.recvStr(), "Client did not receive a response from the rebound server"),
        )
    }

    companion object {
        private const val PORT = 32342
        private const val TIMEOUT_MS = 10_000L
    }
}
