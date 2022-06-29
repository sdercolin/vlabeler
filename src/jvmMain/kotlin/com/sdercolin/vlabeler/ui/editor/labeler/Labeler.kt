package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.PropertyView
import com.sdercolin.vlabeler.ui.theme.Black50
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Labeler(
    editorState: EditorState,
    appState: AppState
) {
    val project = editorState.project
    val entry = editorState.editedEntry.entry
    val scope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState(0)

    LaunchedEffect(Unit) {
        editorState.scrollFitViewModel.eventFlow.collectLatest {
            horizontalScrollState.animateScrollTo(it)
        }
    }

    Column(Modifier.fillMaxSize()) {
        EntryTitleBar(
            entryName = entry.name,
            sampleName = project.currentSampleName,
            openEditEntryNameDialog = {
                editorState.openEditEntryNameDialog(duplicate = false, scope = scope)
            }
        )
        Box(Modifier.fillMaxWidth().weight(1f).border(width = 0.5.dp, color = Black50)) {
            Canvas(
                horizontalScrollState = horizontalScrollState,
                editorState = editorState,
                appState = appState
            )
            if (appState.viewState.isPropertyViewDisplayed) {
                PropertyView(editorState.project)
            }
        }
        HorizontalScrollbar(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            adapter = rememberScrollbarAdapter(horizontalScrollState)
        )
        BottomBar(rememberBottomBarState(project, appState, editorState))
    }
}

@Composable
private fun EntryTitleBar(entryName: String, sampleName: String, openEditEntryNameDialog: () -> Unit) {
    Surface {
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .background(color = MaterialTheme.colors.surface)
                .padding(horizontal = 20.dp)
        ) {
            Row(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    modifier = Modifier.alignByBaseline()
                        .clickable { openEditEntryNameDialog() },
                    text = entryName,
                    style = MaterialTheme.typography.h3,
                    maxLines = 1
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "（$sampleName）",
                    style = MaterialTheme.typography.h5,
                    maxLines = 1
                )
            }
        }
    }
}
