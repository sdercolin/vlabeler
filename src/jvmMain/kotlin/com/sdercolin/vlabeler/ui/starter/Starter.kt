package com.sdercolin.vlabeler.ui.starter

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.string.*
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Composable
fun BoxScope.Starter(
    mainScope: CoroutineScope,
    appState: AppState,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.wrapContentSize()
                .padding(50.dp)
                .size(800.dp, 450.dp)
                .align(Alignment.Center),
        ) {
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = string(Strings.AppName),
                style = MaterialTheme.typography.h2,
            )
            Spacer(Modifier.height(50.dp))

            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = string(Strings.StarterStart),
                        style = MaterialTheme.typography.h5,
                    )
                    Spacer(Modifier.height(15.dp))
                    TextButton(onClick = { appState.requestOpenProjectCreator() }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(text = string(Strings.StarterNewProject), style = MaterialTheme.typography.body2)
                    }
                    TextButton(onClick = { appState.openOpenProjectDialog() }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(text = string(Strings.StarterOpen), style = MaterialTheme.typography.body2)
                    }
                }
                Spacer(Modifier.widthIn(min = 50.dp))
                Column(Modifier.weight(1.5f)) {
                    Text(
                        text = string(Strings.StarterRecent),
                        style = MaterialTheme.typography.h5,
                    )
                    Spacer(Modifier.height(30.dp))
                    val appRecord by appState.appRecordFlow.collectAsState()
                    val recentPaths = appRecord.recentProjectPathsWithDisplayNames
                    val recentFiles = recentPaths.map { File(it.first) }
                    if (recentFiles.isEmpty()) {
                        Text(text = string(Strings.StarterRecentEmpty), style = MaterialTheme.typography.body2)
                    } else {
                        recentFiles.forEachIndexed { index, file ->
                            SingleClickableText(
                                modifier = Modifier.padding(bottom = 15.dp),
                                text = recentPaths[index].second,
                                onClick = {
                                    loadProject(mainScope, file, appState)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
