package com.sdercolin.vlabeler.ipc.response

import com.sdercolin.vlabeler.ipc.IpcMessageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Suppress("unused")
@Serializable
@SerialName("OpenOrCreate")
class OpenOrCreateResponse(val requestedAt: Long, val sentAt: Long) : IpcResponse() {

    @Transient
    override val type: IpcMessageType = IpcMessageType.OpenOrCreate
}
