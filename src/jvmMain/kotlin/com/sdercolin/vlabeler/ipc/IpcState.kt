package com.sdercolin.vlabeler.ipc

import com.sdercolin.vlabeler.ipc.request.HeartbeatRequest
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.request.OpenOrCreateRequest
import com.sdercolin.vlabeler.ipc.response.HeartbeatResponse
import com.sdercolin.vlabeler.ipc.response.IpcResponse
import com.sdercolin.vlabeler.ipc.response.OpenOrCreateResponse
import com.sdercolin.vlabeler.ui.AppState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * State for Inter-Process Communication.
 */
class IpcState(private val appState: AppState) {

    private val server = IpcServer(appState.mainScope)

    private val requestFlow: MutableSharedFlow<IpcRequest> = MutableSharedFlow()

    init {
        requestFlow.onEach(::handleRequest).launchIn(appState.mainScope)
        server.bind()
        server.startReceive(requestFlow)
    }

    private fun response(response: IpcResponse) {
        server.send(response)
    }

    private fun handleRequest(request: IpcRequest) {
        val response = when (request) {
            is HeartbeatRequest -> HeartbeatResponse(request.sentAt, System.currentTimeMillis())
            is OpenOrCreateRequest -> {
                appState.consumeOpenOrCreateIpcRequest(request) // async
                OpenOrCreateResponse(request.sentAt, System.currentTimeMillis())
            }
        }
        response(response)
    }

    fun close() {
        server.close()
    }
}
