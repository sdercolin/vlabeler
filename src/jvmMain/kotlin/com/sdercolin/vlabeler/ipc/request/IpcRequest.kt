package com.sdercolin.vlabeler.ipc.request

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ipc.IpcMessageType
import kotlinx.serialization.Serializable

@Serializable
@Immutable
sealed class IpcRequest {
    abstract val type: IpcMessageType
}
