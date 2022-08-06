package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.exception.CustomizedItemLoadingException
import com.sdercolin.vlabeler.io.asLabelerConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.CustomLabelerDir
import java.io.File

class LabelerManagerDialogState(
    appState: AppState,
    appRecordStore: AppRecordStore
) : CustomizableItemManagerDialogState<LabelerItem>(
    title = Strings.LabelerManagerTitle,
    importDialogTitle = Strings.LabelerManagerImportDialogTitle,
    definitionFileNameSuffix = LabelerConf.LabelerFileExtension,
    directory = CustomLabelerDir,
    allowExecution = false,
    appState = appState,
    appRecordStore = appRecordStore
) {
    override fun saveDisabled() {
        val currentItem = items[requireNotNull(selectedIndex)]
        appRecordStore.update { setLabelerDisabled(currentItem.name, currentItem.disabled) }
    }

    override suspend fun importNewItem(configFile: File) = runCatching {
        configFile.asLabelerConf().getOrThrow()
        val targetFolder = CustomLabelerDir
        configFile.copyTo(targetFolder, overwrite = true)
        Unit
    }.getOrElse {
        throw CustomizedItemLoadingException(it)
    }

    override fun reload() {
        appState.reloadLabelers()
    }
}

@Composable
fun rememberLabelerPluginManagerState(appState: AppState): LabelerManagerDialogState {
    val labelers = appState.availableLabelerConfs
    val state = remember {
        LabelerManagerDialogState(
            appState = appState,
            appRecordStore = appState.appRecordStore
        )
    }
    LaunchedEffect(labelers) {
        val items = labelers.map {
            LabelerItem(
                labelerConf = it,
                disabled = appState.appRecordStore.stateFlow.value.disabledLabelerNames.contains(it.name)
            )
        }
        state.loadItems(items)
    }
    return state
}
