package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.theme.Black50
import kotlinx.coroutines.launch

@Composable
fun PreferencesDialog(appState: AppState) {
    val currentConf = remember { appState.appConf }
    val initialPage = remember { appState.lastViewedPreferencesPage }
    val onViewPage = remember { { page: PreferencesPage -> appState.lastViewedPreferencesPage = page } }
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
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50),
        contentAlignment = Alignment.Center,
    ) {
        Surface {
            PreferencesEditor(
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
            )
        }
    }
}
