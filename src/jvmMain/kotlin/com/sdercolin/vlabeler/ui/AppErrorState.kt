package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log

interface AppErrorState {
    val error: Throwable?
    val errorPendingAction: ErrorPendingAction?
    fun showError(error: Throwable, pendingAction: ErrorPendingAction? = null)
    fun clearError()

    enum class ErrorPendingAction {
        Exit,
        ExitProject,
    }
}

class AppErrorStateImpl : AppErrorState {
    override var error: Throwable? by mutableStateOf(null)
    override var errorPendingAction: AppErrorState.ErrorPendingAction? by mutableStateOf(null)

    override fun showError(error: Throwable, pendingAction: AppErrorState.ErrorPendingAction?) {
        this.error = error
        this.errorPendingAction = pendingAction
        Log.error(error)
    }

    override fun clearError() {
        error = null
        errorPendingAction = null
    }
}
