package com.sdercolin.vlabeler.ipc

import com.sdercolin.vlabeler.ipc.request.HeartbeatRequest
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.response.HeartbeatResponse
import com.sdercolin.vlabeler.ipc.response.IpcResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface AppIpcState {

    val ipcRequestFlow: Flow<IpcRequest>

    fun response(response: IpcResponse)
}

class AppIpcStateImpl(private val scope: CoroutineScope) : AppIpcState {

    private val ipcServer = IpcServer(scope)

    override val ipcRequestFlow: MutableSharedFlow<IpcRequest> = MutableSharedFlow<IpcRequest>()

    init {
        ipcRequestFlow.onEach(::handleRequest).launchIn(scope)
        ipcServer.bind()
        ipcServer.startReceive(ipcRequestFlow)
    }

    override fun response(response: IpcResponse) {
        ipcServer.send(response)
    }

    private fun handleRequest(request: IpcRequest) {
        val response = when (request) {
            is HeartbeatRequest -> HeartbeatResponse(request.sentAt, System.currentTimeMillis())
        }
        response(response)
    }
}
