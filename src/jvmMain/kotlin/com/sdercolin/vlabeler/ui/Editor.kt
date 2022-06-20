package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class EditedEntry(
    val entry: Entry,
    val sampleName: String,
    val index: Int
)

@Composable
fun Editor(
    project: Project,
    editEntry: (EditedEntry) -> Unit,
    showDialog: (EmbeddedDialogArgs) -> Unit,
    appConf: AppConf,
    labelerState: MutableState<LabelerState>,
    appState: MutableState<AppState>,
    playerState: PlayerState,
    keyboardViewModel: KeyboardViewModel,
    player: Player
) {
    val sampleState = produceState(initialValue = null as Sample?, project.currentSampleName, appConf) {
        value = withContext(Dispatchers.IO) {
            loadSampleFile(project.currentSampleFile, appConf)
        }
    }
    val isLoading by remember { derivedStateOf { sampleState.value == null } }
    val editedEntryState = remember { mutableStateOf(project.getEntryForEditing()) }
    LaunchedEffect(project.currentSampleName, project.currentEntryIndex) {
        // when switched to a new entry, merge the edited entry and load the new one
        val edited = appState.value.hasEditedEntry
        if (edited) {
            println("Previous entry merged")
            editEntry(editedEntryState.value)
        }
        println("Entry loaded")
        editedEntryState.value = project.getEntryForEditing()
        if (edited) appState.update { copy(hasEditedEntry = false) }
    }

    LaunchedEffect(appState.value.projectWriteStatus) {
        if (appState.value.projectWriteStatus != AppState.ProjectWriteStatus.UpdateRequested) return@LaunchedEffect
        if (appState.value.hasEditedEntry.not()) return@LaunchedEffect
        // when saving is requested, merge the edited entry first
        println("Entry Merged")
        editEntry(editedEntryState.value)
        appState.update { copy(hasEditedEntry = false) }
    }

    val sample = sampleState.value
    LaunchedEffect(sample) {
        if (sample != null) player.load(sample.info.file)
    }
    Labeler(
        sample = sample,
        sampleName = project.currentSampleName,
        entry = editedEntryState.value.entry,
        currentEntryIndexInTotal = project.currentEntryIndexInTotal,
        totalEntryCount = project.totalEntryCount,
        editEntry = {
            editedEntryState.update { copy(entry = it) }
            appState.update { copy(hasEditedEntry = true) }
        },
        playSampleSection = player::playSection,
        showDialog = showDialog,
        appConf = appConf,
        labelerConf = project.labelerConf,
        labelerState = labelerState,
        playerState = playerState,
        keyboardViewModel = keyboardViewModel
    )
    if (isLoading) {
        CircularProgress()
    }
}
