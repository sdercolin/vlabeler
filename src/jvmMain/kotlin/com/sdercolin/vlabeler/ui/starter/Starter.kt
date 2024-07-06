package com.sdercolin.vlabeler.ui.starter

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.common.WithTooltip
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
                    Spacer(Modifier.height(40.dp))
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = string(Strings.StarterQuickEdit),
                        style = MaterialTheme.typography.h5,
                    )
                    Spacer(Modifier.height(11.dp))
                    Box {
                        val scrollState = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            getQuickProjectBuilders(appState).forEach {
                                Spacer(Modifier.height(4.dp))
                                QuickEditButton(it, appState)
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
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
                        val scrollState = rememberScrollState()
                        Box {
                            Column(modifier = Modifier.verticalScroll(scrollState)) {
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
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(scrollState),
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickEditButton(
    it: Pair<LabelerConf, LabelerConf.QuickProjectBuilder>,
    appState: AppState,
) {
    WithTooltip(tooltip = it.second.description?.get()) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colors.primary,
            LocalContentAlpha provides 1f,
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button,
            ) {
                Row(
                    modifier = Modifier.semantics { role = Role.Button }
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = { appState.openQuickEditFileDialog(it) })
                        .padding(ButtonDefaults.TextButtonContentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = it.second.getDisplayedName() + "...",
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun getQuickProjectBuilders(appState: AppState): List<Pair<LabelerConf, LabelerConf.QuickProjectBuilder>> {
    val all = appState.activeLabelerConfs
        .flatMap { labeler -> labeler.quickProjectBuilders.map { labeler to it } }
    val appRecord by appState.appRecordFlow.collectAsState()
    val recentUsed = appRecord.recentQuickProjectBuilderNames
        .mapNotNull { (labelerName, builderName) ->
            all.find { (labeler, builder) -> labeler.name == labelerName && builder.name == builderName }
        }
    return recentUsed + all.filterNot { recentUsed.contains(it) }
}
