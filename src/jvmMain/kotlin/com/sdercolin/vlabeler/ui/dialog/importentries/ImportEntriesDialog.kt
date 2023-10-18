package com.sdercolin.vlabeler.ui.dialog.importentries

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.io.ImportedModule
import com.sdercolin.vlabeler.ui.ProjectStore
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.LargeDialogContainer
import com.sdercolin.vlabeler.ui.common.WithTooltip
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.getCheckboxColors
import com.sdercolin.vlabeler.ui.theme.getSwitchColors

@Composable
private fun rememberImportEntriesDialogState(
    finish: () -> Unit,
    projectStore: ProjectStore,
    importedModules: List<ImportedModule>,
) = remember(finish, projectStore, importedModules) {
    ImportEntriesDialogState(finish, projectStore, importedModules)
}

@Composable
fun ImportEntriesDialog(
    finish: () -> Unit,
    projectStore: ProjectStore,
    args: ImportEntriesDialogArgs,
    state: ImportEntriesDialogState = rememberImportEntriesDialogState(finish, projectStore, args.importedModules),
) {
    LargeDialogContainer {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 45.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = string(Strings.ImportEntriesDialogTitle),
                style = MaterialTheme.typography.h5,
            )
            Spacer(modifier = Modifier.height(25.dp))
            Content(state)
            Spacer(modifier = Modifier.height(25.dp))
            ButtonBar(state)
        }
    }
}

@Composable
private fun ColumnScope.Content(state: ImportEntriesDialogState) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth()
            .weight(1f)
            .background(MaterialTheme.colors.background),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp)
                .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            state.items.forEachIndexed { index, it ->
                if (index != 0) {
                    Spacer(modifier = Modifier.height(50.dp))
                }
                Item(it, state.existingModuleNames)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterVertically).width(15.dp),
        )
    }
}

@Composable
private fun Item(item: ImportEntriesDialogState.Item, targetModuleNames: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.selected,
            onCheckedChange = { item.toggleSelected(it) },
            enabled = item.compatible,
            colors = getCheckboxColors(),
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = item.getName(),
                    style = MaterialTheme.typography.h5,
                    color = if (item.compatible) MaterialTheme.colors.onBackground else MaterialTheme.colors.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = item.getSummaryTitle(),
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.getSummaryContent(),
                style = MaterialTheme.typography.body2,
                color = if (item.compatible) MaterialTheme.colors.onBackground else MaterialTheme.colors.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(40.dp))

        var expanded by remember { mutableStateOf(false) }

        @Composable
        fun getModuleDisplayName(moduleName: String?): String = when (moduleName) {
            null -> ""
            "" -> string(Strings.CommonRootModuleName)
            else -> moduleName
        }

        Box {
            TextField(
                modifier = Modifier.widthIn(min = 120.dp),
                value = getModuleDisplayName(item.targetName),
                onValueChange = { },
                readOnly = true,
                label = { Text(string(Strings.ImportEntriesDialogItemTargetLabel)) },
                singleLine = true,
                enabled = item.compatible,
                isError = item.selected && item.targetName == null,
                leadingIcon = {
                    IconButton(onClick = { expanded = true }, enabled = item.compatible) {
                        Icon(Icons.Default.ExpandMore, null)
                    }
                },
            )
            DropdownMenu(
                modifier = Modifier.align(Alignment.CenterEnd),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                (listOf<String?>(null) + targetModuleNames).forEach { moduleName ->
                    DropdownMenuItem(
                        onClick = {
                            item.selectTarget(moduleName)
                            expanded = false
                        },
                    ) {
                        val text = when (moduleName) {
                            null -> ""
                            "" -> string(Strings.CommonRootModuleName)
                            else -> moduleName
                        }
                        Text(text = text)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))
    }
}

@Composable
private fun ButtonBar(state: ImportEntriesDialogState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WithTooltip(
            if (state.forceReplaceContent) {
                string(Strings.ImportEntriesDialogReplaceContentDisabledDescription)
            } else {
                null
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.replaceContent,
                    onCheckedChange = { state.replaceContent = it },
                    colors = getSwitchColors(),
                    enabled = state.forceReplaceContent.not(),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = string(Strings.ImportEntriesDialogReplaceContent),
                    style = MaterialTheme.typography.body2,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = state::cancel) {
            Text(text = string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(onClick = state::submit, enabled = state.isValid)
    }
}
