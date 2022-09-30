package com.sdercolin.vlabeler.ipc

import com.sdercolin.vlabeler.ipc.request.HeartbeatRequest
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.request.OpenOrCreateRequest
import com.sdercolin.vlabeler.ipc.response.HeartbeatResponse
import com.sdercolin.vlabeler.ipc.response.IpcResponse
import com.sdercolin.vlabeler.ipc.response.OpenOrCreateResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val jsonForIpc = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule
    serializersModule = SerializersModule {
        polymorphic(IpcRequest::class) {
            subclass(HeartbeatRequest::class)
            subclass(OpenOrCreateRequest::class)
        }
        polymorphic(IpcResponse::class) {
            subclass(HeartbeatResponse::class)
            subclass(OpenOrCreateResponse::class)
        }
    }
}
