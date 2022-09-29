package com.sdercolin.vlabeler.ipc.response

import com.sdercolin.vlabeler.ipc.IpcMessageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Heartbeat")
class HeartbeatResponse(val requestedAt: Long, val sentAt: Long) : IpcResponse() {

    @Transient
    override val type: IpcMessageType = IpcMessageType.Heartbeat
}
