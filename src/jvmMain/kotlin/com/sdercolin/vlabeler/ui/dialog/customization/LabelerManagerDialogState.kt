package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.exception.CustomizableItemLoadingException
import com.sdercolin.vlabeler.io.asLabelerConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.CustomLabelerDir
import java.io.File

class LabelerManagerDialogState(
    appState: AppState,
    appRecordStore: AppRecordStore,
) : CustomizableItemManagerDialogState<LabelerItem>(
    title = Strings.LabelerManagerTitle,
    importDialogTitle = Strings.LabelerManagerImportDialogTitle,
    definitionFileExtension = LabelerConf.LABELER_FILE_EXTENSION,
    directory = CustomLabelerDir,
    allowExecution = false,
    appState = appState,
    appRecordStore = appRecordStore,
) {
    override fun saveDisabled(index: Int) {
        val item = items[index]
        appRecordStore.update { setLabelerDisabled(item.name, item.disabled) }
    }

    override suspend fun importNewItem(configFile: File): String = runCatching {
        val labeler = configFile.asLabelerConf(isBuiltIn = false).getOrThrow()
        if (labeler.singleFile) {
            val targetPath = CustomLabelerDir.resolve(configFile.name)
            configFile.copyTo(targetPath, overwrite = true)
        } else {
            val targetFolder = CustomLabelerDir.resolve(requireNotNull(labeler.directory).name)
            configFile.parentFile.copyRecursively(targetFolder, overwrite = true)
        }
        labeler.name
    }.getOrElse {
        throw CustomizableItemLoadingException(it)
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
            appRecordStore = appState.appRecordStore,
        )
    }
    LaunchedEffect(labelers) {
        val items = labelers.map {
            LabelerItem(
                labelerConf = it,
                disabled = appState.appRecordStore.value.disabledLabelerNames.contains(it.name),
            )
        }
        state.loadItems(items)
    }
    return state
}
