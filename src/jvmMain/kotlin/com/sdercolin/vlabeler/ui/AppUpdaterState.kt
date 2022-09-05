package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.repository.update.UpdateRepository
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AppUpdaterState {

    fun checkUpdates(isAuto: Boolean)
}

class AppUpdaterStateImpl(
    private val snackbarHostState: SnackbarHostState,
    private val dialogState: AppDialogState,
    private val appRecordStore: AppRecordStore,
    private val scope: CoroutineScope,
) : AppUpdaterState {

    private val repository = UpdateRepository()

    override fun checkUpdates(isAuto: Boolean) {
        scope.launch(Dispatchers.IO) {
            repository.fetchUpdate()
                .onSuccess {
                    if (it == null) {
                        if (isAuto.not()) {
                            snackbarHostState.showSnackbar(string(Strings.CheckForUpdatesAlreadyUpdated))
                        }
                        return@launch
                    }
                    Log.debug("Application update found: $it")
                    if (isAuto && appRecordStore.value.isUpdateIgnored(it.version)) {
                        Log.debug("Application update ignored: ${it.version}")
                        return@onSuccess
                    }
                    dialogState.openUpdaterDialog(it)
                }
                .onFailure {
                    snackbarHostState.showSnackbar(string(Strings.CheckForUpdatesFailure))
                }
        }
    }
}
