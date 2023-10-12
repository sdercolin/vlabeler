package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.LargeDialogContainer
import kotlinx.coroutines.launch

@Composable
fun PreferencesDialog(appState: AppState) {
    val currentConf = remember { appState.appConf }
    val initialPage = remember { appState.lastViewedPreferencesPage }
    val onViewPage = remember { { page: PreferencesPage -> appState.lastViewedPreferencesPage = page } }
    val args = remember { appState.preferencesDialogArgs }
    val finish = remember {
        { result: AppConf? ->
            appState.closePreferencesDialog()
            if (result != null) appState.updateAppConf(result)
        }
    }
    val apply = remember {
        { newConf: AppConf ->
            appState.updateAppConf(newConf)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    LargeDialogContainer {
        PreferencesEditor(
            appState = appState,
            currentConf = currentConf,
            submit = finish,
            apply = apply,
            initialPage = initialPage,
            onViewPage = onViewPage,
            showSnackbar = {
                coroutineScope.launch {
                    appState.showSnackbar(message = it)
                }
            },
            launchArgs = args,
        )
    }
}
