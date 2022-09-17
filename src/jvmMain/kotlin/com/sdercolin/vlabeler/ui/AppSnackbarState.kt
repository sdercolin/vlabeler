package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic

interface AppSnackbarState {

    val snackbarHostState: SnackbarHostState

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = stringStatic(Strings.CommonOkay),
        duration: SnackbarDuration = SnackbarDuration.Short,
    )
}

class AppSnackbarStateImpl(
    override val snackbarHostState: SnackbarHostState,
) : AppSnackbarState {

    override suspend fun showSnackbar(message: String, actionLabel: String?, duration: SnackbarDuration) {
        snackbarHostState.showSnackbar(message, actionLabel, duration)
    }
}
