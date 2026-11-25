package testutil

import com.sdercolin.vlabeler.ipc.IpcState

/**
 * A no-op [IpcState] for tests: it binds no port and does nothing on close. Records whether [close] was called so
 * that tests exercising the app's shutdown path can assert it.
 */
class FakeIpcState : IpcState {

    var closed: Boolean = false
        private set

    override fun close() {
        closed = true
    }
}
