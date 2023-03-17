package model

import com.sdercolin.vlabeler.ipc.jsonForIpc
import com.sdercolin.vlabeler.ipc.request.HeartbeatRequest
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.response.HeartbeatResponse
import com.sdercolin.vlabeler.ipc.response.IpcResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for serialization of [IpcRequest] and [IpcResponse].
 */
class IpcMessageTest {

    @Test
    fun testRequest() {
        val request = HeartbeatRequest(1) as IpcRequest
        val json = jsonForIpc.encodeToString(request)
        val deserialized = jsonForIpc.decodeFromString<IpcRequest>(json) as HeartbeatRequest
        assertEquals(deserialized.sentAt, (request as HeartbeatRequest).sentAt)
    }

    @Test
    fun testResponse() {
        val response = HeartbeatResponse(1, 2) as IpcResponse
        val json = jsonForIpc.encodeToString(response)
        val deserialized = jsonForIpc.decodeFromString<IpcResponse>(json) as HeartbeatResponse
        assertEquals(deserialized.sentAt, (response as HeartbeatResponse).sentAt)
        assertEquals(deserialized.requestedAt, response.requestedAt)
    }
}
