package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.repository.update.UpdateRepository
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppUpdaterState {

    suspend fun check()
}

class AppUpdaterStateImpl(
    private val snackbarHostState: SnackbarHostState,
    private val dialogState: AppDialogState,
    private val appRecordStore: AppRecordStore,
) : AppUpdaterState {

    private val repository = UpdateRepository()

    override suspend fun check() {
        withContext(Dispatchers.IO) {
            repository.fetchUpdate()
                .onSuccess {
                    it ?: return@onSuccess
                    Log.debug("Application update found: $it")
                    if (appRecordStore.value.isUpdateIgnored(it.version)) {
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
