@file:OptIn(ExperimentalFoundationApi::class)

package com.sdercolin.vlabeler.ui.starter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.update

@Composable
fun BoxScope.Starter(
    appState: MutableState<AppState>,
    requestNewProject: (Project) -> Unit,
    availableLabelerConfs: List<LabelerConf>,
    snackbarHostState: SnackbarHostState
) {
    Surface(Modifier.fillMaxSize()) {
        if (!appState.value.isConfiguringNewProject) {
            Column(
                modifier = Modifier.wrapContentSize()
                    .padding(30.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = string(Strings.AppName), style = MaterialTheme.typography.h2)
                Spacer(Modifier.height(50.dp))

                Row {
                    OutlinedButton(
                        modifier = Modifier.size(180.dp, 120.dp),
                        onClick = { appState.update { configureNewProject() } }
                    ) {
                        Text(string(Strings.StarterNewProject))
                    }
                    Spacer(Modifier.width(40.dp))
                    OutlinedButton(
                        modifier = Modifier.size(180.dp, 120.dp),
                        onClick = { appState.update { openOpenProjectDialog() } }
                    ) {
                        Text(string(Strings.StarterOpen))
                    }
                }
            }
        } else {
            ProjectCreator(
                create = requestNewProject,
                cancel = { appState.update { stopConfiguringNewProject() } },
                availableLabelerConfs = availableLabelerConfs,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
