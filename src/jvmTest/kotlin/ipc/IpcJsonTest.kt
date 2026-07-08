package ipc

import com.sdercolin.vlabeler.ipc.IpcMessageType
import com.sdercolin.vlabeler.ipc.jsonForIpc
import com.sdercolin.vlabeler.ipc.request.HeartbeatRequest
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.request.OpenOrCreateRequest
import com.sdercolin.vlabeler.ipc.response.HeartbeatResponse
import com.sdercolin.vlabeler.ipc.response.IpcResponse
import com.sdercolin.vlabeler.ipc.response.OpenOrCreateResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the external JSON contract of the IPC API: raw JSON strings exactly as an external client would produce
 * (requests) or consume (responses) through [jsonForIpc], including the `type` class discriminator.
 */
class IpcJsonTest {

    @Test
    fun `heartbeat request is decoded from raw client json`() {
        val raw = """{"type":"Heartbeat","sentAt":1234567890123}"""
        val request = jsonForIpc.decodeFromString<IpcRequest>(raw)
        val heartbeat = assertIs<HeartbeatRequest>(request)
        assertEquals(1234567890123L, heartbeat.sentAt)
        assertEquals(IpcMessageType.Heartbeat, heartbeat.type)
    }

    @Test
    fun `heartbeat request is encoded with type discriminator`() {
        val request: IpcRequest = HeartbeatRequest(sentAt = 5)
        assertEquals("""{"type":"Heartbeat","sentAt":5}""", jsonForIpc.encodeToString(request))
    }

    @Test
    fun `heartbeat response is encoded exactly as a client consumes it`() {
        val response: IpcResponse = HeartbeatResponse(requestedAt = 1, sentAt = 2)
        assertEquals("""{"type":"Heartbeat","requestedAt":1,"sentAt":2}""", jsonForIpc.encodeToString(response))
    }

    @Test
    fun `openOrCreate response is encoded exactly as a client consumes it`() {
        val response: IpcResponse = OpenOrCreateResponse(requestedAt = 10, sentAt = 20)
        assertEquals("""{"type":"OpenOrCreate","requestedAt":10,"sentAt":20}""", jsonForIpc.encodeToString(response))
    }

    @Test
    fun `openOrCreate request with all fields is decoded from raw client json`() {
        val raw = """
            {
                "type": "OpenOrCreate",
                "projectFile": "/home/user/project.lbp",
                "gotoEntryByName": {
                    "parentFolderName": "",
                    "entryName": "あ"
                },
                "gotoEntryByIndex": {
                    "parentFolderName": "module1",
                    "entryIndex": 3
                },
                "newProjectArgs": {
                    "labelerName": "utau-oto-labeler",
                    "sampleDirectory": "/home/user/samples",
                    "cacheDirectory": "/home/user/cache",
                    "labelerParams": {
                        "offset": {"type": "integer", "value": 5},
                        "useNegative": {"type": "boolean", "value": true}
                    },
                    "pluginName": "some-template-plugin",
                    "pluginParams": {
                        "prefix": {"type": "string", "value": "_"},
                        "scale": {"type": "float", "value": 1.5}
                    },
                    "inputFile": "/home/user/oto.ini",
                    "encoding": "Shift-JIS",
                    "autoExport": true
                },
                "sentAt": 1700000000000
            }
        """.trimIndent()
        val request = jsonForIpc.decodeFromString<IpcRequest>(raw)
        val openOrCreate = assertIs<OpenOrCreateRequest>(request)
        assertEquals(IpcMessageType.OpenOrCreate, openOrCreate.type)
        assertEquals("/home/user/project.lbp", openOrCreate.projectFile)
        assertEquals(1700000000000L, openOrCreate.sentAt)

        val byName = assertNotNull(openOrCreate.gotoEntryByName)
        assertEquals("", byName.parentFolderName)
        assertEquals("あ", byName.entryName)

        val byIndex = assertNotNull(openOrCreate.gotoEntryByIndex)
        assertEquals("module1", byIndex.parentFolderName)
        assertEquals(3, byIndex.entryIndex)

        val args = openOrCreate.newProjectArgs
        assertEquals("utau-oto-labeler", args.labelerName)
        assertEquals("/home/user/samples", args.sampleDirectory)
        assertEquals("/home/user/cache", args.cacheDirectory)
        assertEquals("some-template-plugin", args.pluginName)
        assertEquals("/home/user/oto.ini", args.inputFile)
        assertEquals("Shift-JIS", args.encoding)
        assertTrue(args.autoExport)

        val labelerParams = assertNotNull(args.labelerParams)
        assertEquals(setOf("offset", "useNegative"), labelerParams.keys)
        assertEquals("integer", labelerParams.getValue("offset").type)
        assertEquals(5, labelerParams.getValue("offset").value)
        assertEquals("boolean", labelerParams.getValue("useNegative").type)
        assertEquals(true, labelerParams.getValue("useNegative").value)

        val pluginParams = assertNotNull(args.pluginParams)
        assertEquals(setOf("prefix", "scale"), pluginParams.keys)
        assertEquals("string", pluginParams.getValue("prefix").type)
        assertEquals("_", pluginParams.getValue("prefix").value)
        assertEquals("float", pluginParams.getValue("scale").type)
        assertEquals(1.5f, pluginParams.getValue("scale").value)
    }

    @Test
    fun `openOrCreate request with only required fields is decoded with defaults`() {
        val raw = """
            {
                "type": "OpenOrCreate",
                "projectFile": "/tmp/minimal.lbp",
                "newProjectArgs": {
                    "labelerName": "test-labeler"
                },
                "sentAt": 42
            }
        """.trimIndent()
        val request = jsonForIpc.decodeFromString<IpcRequest>(raw)
        val openOrCreate = assertIs<OpenOrCreateRequest>(request)
        assertEquals("/tmp/minimal.lbp", openOrCreate.projectFile)
        assertEquals(42L, openOrCreate.sentAt)
        assertNull(openOrCreate.gotoEntryByName)
        assertNull(openOrCreate.gotoEntryByIndex)

        val args = openOrCreate.newProjectArgs
        assertEquals("test-labeler", args.labelerName)
        assertNull(args.sampleDirectory)
        assertNull(args.cacheDirectory)
        assertNull(args.labelerParams)
        assertNull(args.pluginName)
        assertNull(args.pluginParams)
        assertNull(args.inputFile)
        assertEquals(Charset.defaultCharset().name(), args.encoding)
        assertFalse(args.autoExport)
    }

    @Test
    fun `openOrCreate request is encoded with defaults included`() {
        val request: IpcRequest = OpenOrCreateRequest(
            projectFile = "/tmp/project.lbp",
            newProjectArgs = OpenOrCreateRequest.NewProjectArgs(
                labelerName = "utau-oto-labeler",
                encoding = "UTF-8",
            ),
            sentAt = 100,
        )
        val expected = """{"type":"OpenOrCreate","projectFile":"/tmp/project.lbp","gotoEntryByName":null,""" +
            """"gotoEntryByIndex":null,"newProjectArgs":{"labelerName":"utau-oto-labeler","sampleDirectory":null,""" +
            """"cacheDirectory":null,"labelerParams":null,"pluginName":null,"pluginParams":null,""" +
            """"inputFile":null,"encoding":"UTF-8","autoExport":false},"sentAt":100}"""
        assertEquals(expected, jsonForIpc.encodeToString(request))
    }

    @Test
    fun `unknown keys in a request are ignored`() {
        val raw = """{"type":"Heartbeat","sentAt":7,"extraneous":"ignored","futureField":{"a":1}}"""
        val request = jsonForIpc.decodeFromString<IpcRequest>(raw)
        assertEquals(7L, assertIs<HeartbeatRequest>(request).sentAt)
    }

    @Test
    fun `lenient parsing accepts quoted numbers`() {
        val raw = """{"type":"Heartbeat","sentAt":"12"}"""
        val request = jsonForIpc.decodeFromString<IpcRequest>(raw)
        assertEquals(12L, assertIs<HeartbeatRequest>(request).sentAt)
    }

    @Test
    fun `unknown type discriminator fails to decode`() {
        assertFailsWith<SerializationException> {
            jsonForIpc.decodeFromString<IpcRequest>("""{"type":"Unknown","sentAt":1}""")
        }
    }

    @Test
    fun `message type enum matches the wire discriminator names`() {
        assertEquals(listOf("Heartbeat", "OpenOrCreate"), IpcMessageType.values().map { it.name })
    }
}
