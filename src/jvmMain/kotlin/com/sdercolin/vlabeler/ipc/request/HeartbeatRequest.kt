package com.sdercolin.vlabeler.ipc.request

import com.sdercolin.vlabeler.ipc.IpcMessageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Heartbeat")
class HeartbeatRequest(val sentAt: Long) : IpcRequest() {

    @Transient
    override val type: IpcMessageType = IpcMessageType.Heartbeat
}
