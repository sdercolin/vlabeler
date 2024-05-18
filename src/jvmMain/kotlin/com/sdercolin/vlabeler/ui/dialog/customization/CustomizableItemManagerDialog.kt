package com.sdercolin.vlabeler.ui.dialog.customization

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.common.LargeDialogContainer
import com.sdercolin.vlabeler.ui.common.WithTooltip
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.runIf
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CustomizableItemManagerDialog(
    type: CustomizableItem.Type,
    appState: AppState,
    state: CustomizableItemManagerDialogState<*> = rememberCustomizableItemManagerDialogState(type, appState),
) {
    val coroutineScope = rememberCoroutineScope()
    LargeDialogContainer(
        onClickOutside = { state.cancelSelection() },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 30.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = string(state.title),
                style = MaterialTheme.typography.h5,
            )
            Spacer(modifier = Modifier.height(25.dp))
            MiddleButtonBar(state)
            Spacer(modifier = Modifier.height(15.dp))
            Content(state)
            Spacer(modifier = Modifier.height(25.dp))
            BottomButtonBar(state)
        }
    }
    if (state.isShowingFileSelector) {
        OpenFileDialog(
            title = string(state.importDialogTitle),
            extensions = listOf(state.definitionFileExtension),
            onCloseRequest = { parent, name ->
                val file = if (parent != null && name != null) File(parent, name) else null
                coroutineScope.launch {
                    state.handleFileSelectorResult(file)
                }
            },
        )
    }
}

@Composable
private fun MiddleButtonBar(state: CustomizableItemManagerDialogState<*>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        @Composable
        fun getTint(enabled: Boolean) = MaterialTheme.colors.onSurface.runIf(!enabled) {
            copy(alpha = 0.2f)
        }

        FreeSizedIconButton(
            modifier = Modifier.padding(5.dp),
            onClick = { state.openFileSelectorForNewItem() },
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = getTint(true),
                modifier = Modifier.size(22.dp),
            )
        }

        val canRemove = state.canRemoveCurrentItem()
        FreeSizedIconButton(
            modifier = Modifier.padding(5.dp),
            enabled = canRemove,
            onClick = { state.requestRemoveCurrentItem() },
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = getTint(canRemove),
                modifier = Modifier.size(22.dp),
            )
        }

        val selectedItem = state.selectedItem
        FreeSizedIconButton(
            modifier = Modifier.padding(5.dp),
            enabled = selectedItem != null,
            onClick = { requireNotNull(selectedItem).revealInExplorer() },
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = getTint(selectedItem != null),
                modifier = Modifier.size(22.dp),
            )
        }

        val hasEmail = selectedItem?.hasEmail() == true
        FreeSizedIconButton(
            modifier = Modifier.padding(5.dp),
            enabled = hasEmail,
            onClick = { requireNotNull(selectedItem).openEmail() },
        ) {
            Icon(
                imageVector = Icons.Default.Mail,
                contentDescription = null,
                tint = getTint(hasEmail),
                modifier = Modifier.size(22.dp),
            )
        }

        val hasWebsite = selectedItem?.hasWebsite() == true
        FreeSizedIconButton(
            modifier = Modifier.padding(5.dp),
            enabled = hasWebsite,
            onClick = { requireNotNull(selectedItem).openWebsite() },
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = getTint(hasWebsite),
                modifier = Modifier.size(22.dp),
            )
        }

        if (state.allowExecution) {
            val canExecute = state.canExecuteSelectedItem()
            FreeSizedIconButton(
                modifier = Modifier.padding(5.dp),
                enabled = canExecute,
                onClick = { state.executeSelectedItem() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = getTint(canExecute),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Content(state: CustomizableItemManagerDialogState<*>) {
    val lazyListState = rememberLazyListState()
    Box(
        modifier = Modifier.fillMaxWidth()
            .weight(1f)
            .background(MaterialTheme.colors.background),
    ) {
        LazyColumn(state = lazyListState) {
            itemsIndexed(
                items = state.items,
                key = { _, item ->
                    item.name
                },
            ) { index, item ->
                Item(index, item, state)
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).width(15.dp),
        )
    }
}

@Composable
fun Item(index: Int, item: CustomizableItem, state: CustomizableItemManagerDialogState<*>) {
    val isSelected = state.selectedIndex == index
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .runIf(isSelected) { background(MaterialTheme.colors.primaryVariant) }
            .plainClickable { state.selectItem(index) }
            .padding(vertical = 20.dp, horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = item.displayedName.get(),
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = string(Strings.PluginDialogInfoAuthor, item.author),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = string(Strings.PluginDialogInfoVersion, item.version),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                )
            }
            val description = item.description.get()
            if (description.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = description,
                    style = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.canRemove.not()) {
            Spacer(Modifier.width(10.dp))
            WithTooltip(string(Strings.CustomizableItemManagerLockedDescription)) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = item.disabled.not(),
            onCheckedChange = { state.toggleItemDisabled(index) },
            colors = getSwitchColors(),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
fun BottomButtonBar(state: CustomizableItemManagerDialogState<*>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { state.openDirectory() }) {
            Text(string(Strings.CustomizableItemManagerOpenDirectory))
        }
        Spacer(Modifier.width(25.dp))
        TextButton(onClick = { state.reload() }) {
            Text(string(Strings.CustomizableItemManagerReload))
        }
        Spacer(Modifier.weight(1f))
        ConfirmButton(onClick = state::finish)
    }
}
