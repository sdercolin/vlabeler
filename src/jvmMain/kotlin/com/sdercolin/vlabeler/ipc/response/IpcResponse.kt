package com.sdercolin.vlabeler.ipc.response

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ipc.IpcMessageType
import kotlinx.serialization.Serializable

@Serializable
@Immutable
sealed class IpcResponse {
    abstract val type: IpcMessageType
}
