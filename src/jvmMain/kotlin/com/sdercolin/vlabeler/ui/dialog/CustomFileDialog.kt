package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import cafe.adriel.bonsai.core.Bonsai
import cafe.adriel.bonsai.core.BonsaiStyle
import cafe.adriel.bonsai.core.node.Branch
import cafe.adriel.bonsai.core.node.Leaf
import cafe.adriel.bonsai.core.tree.Tree
import cafe.adriel.bonsai.core.tree.TreeScope
import com.sdercolin.vlabeler.ui.common.CancelButton
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.toFile
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.SplitPaneState
import java.awt.Cursor
import java.io.File

@Composable
fun CustomFileDialog(
    mode: Int,
    title: String,
    initialDirectory: String?,
    initialFileName: String?,
    extensions: List<String>?,
    directoryMode: Boolean,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) {
    val root = HomeDir
    val fileFree = DirectoryTree(root)
    DialogWindow(
        title = title,
        icon = painterResource(Resources.iconIco),
        onCloseRequest = { onCloseRequest(null, null) },
        state = rememberDialogState(width = 800.dp, height = 600.dp),
        resizable = true,
    ) {
        AppTheme {
            Content(
                mode = mode,
                root = root,
                fileTree = fileFree,
                initialDirectory = initialDirectory,
                initialFileName = initialFileName,
                extensions = extensions,
                directoryMode = directoryMode,
                onCloseRequest = onCloseRequest,
            )
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
private fun Content(
    mode: Int,
    root: File,
    fileTree: Tree<File>,
    initialDirectory: String?,
    initialFileName: String?,
    extensions: List<String>?,
    directoryMode: Boolean,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) {
    val saveMode = remember(mode) { mode == java.awt.FileDialog.SAVE }
    val splitPaneState = remember {
        SplitPaneState(
            initialPositionPercentage = 0.3f,
            moveEnabled = true,
        )
    }
    var currentDirectory by remember { mutableStateOf(initialDirectory?.toFile() ?: root) }
    var currentFileName by remember { mutableStateOf(initialFileName ?: "") }
    var currentExtension by remember {
        mutableStateOf(
            extensions?.firstOrNull {
                currentFileName.isNotEmpty() && currentFileName.endsWith(it)
            } ?: extensions?.firstOrNull() ?: "*",
        )
    }

    fun updateCurrentDirectory(directory: File) {
        currentDirectory = directory
        if (!saveMode) {
            currentFileName = ""
        }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        HorizontalSplitPane(splitPaneState = splitPaneState) {
            first {
                Bonsai(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    tree = fileTree,
                    style = BonsaiStyle(
                        nodeNameTextStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                        toggleIconColorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface),
                    ),
                    onClick = {
                        updateCurrentDirectory(it.content)
                    },
                )
            }
            second {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val canGoBack = currentDirectory != root
                        FreeSizedIconButton(
                            onClick = {
                                updateCurrentDirectory(currentDirectory.parentFile ?: root)
                            },
                            modifier = Modifier.padding(5.dp),
                            enabled = canGoBack,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = if (canGoBack) 0.8f else 0.3f),
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        BasicTextField(
                            modifier = Modifier.weight(1f)
                                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                                .padding(10.dp),
                            value = currentDirectory.absolutePath,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 5.dp, vertical = 15.dp),
                    ) {
                        val files = currentDirectory.listFiles().orEmpty().sortedWith { o1, o2 ->
                            when {
                                o1.isDirectory && !o2.isDirectory -> -1
                                !o1.isDirectory && o2.isDirectory -> 1
                                else -> o1.name.compareTo(o2.name)
                            }
                        }
                        val lazyState = rememberLazyListState()
                        LazyColumn(
                            state = lazyState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(files, key = { it.absolutePath }) { file ->
                                val isSelectable = if (directoryMode) {
                                    file.isDirectory
                                } else {
                                    file.isDirectory || currentExtension == "*" ||
                                        file.name.endsWith(".$currentExtension")
                                }
                                val isSelected = file.name == currentFileName
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(enabled = isSelectable) {
                                            if (file.isDirectory) {
                                                updateCurrentDirectory(file)
                                            } else {
                                                currentFileName = file.name
                                            }
                                        }
                                        .runIf(isSelected) {
                                            background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val icon = when {
                                        file.isDirectory -> Icons.Default.Folder
                                        else -> Icons.Default.Description
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colors.onSurface.copy(
                                            alpha = if (isSelectable) 0.8f else 0.3f,
                                        ),
                                    )
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.body2.copy(
                                            color = MaterialTheme.colors.onSurface.copy(
                                                alpha = if (isSelectable) 1f else 0.3f,
                                            ),
                                        ),
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(lazyState),
                        )
                    }
                    if (!directoryMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                        ) {
                            BasicTextField(
                                modifier = Modifier.weight(1f)
                                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                                    .padding(10.dp),
                                value = currentFileName,
                                onValueChange = { currentFileName = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colors.onSurface.copy(alpha = 0.8f)),
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Row(
                                    modifier = Modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    val selectable = extensions.orEmpty().size > 1
                                    FreeSizedIconButton(
                                        onClick = { expanded = !expanded },
                                        enabled = selectable,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandLess,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colors.onSurface.copy(
                                                alpha = if (selectable) 0.8f else 0.3f,
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    BasicTextField(
                                        modifier = Modifier.width(60.dp),
                                        value = "*.$currentExtension",
                                        onValueChange = { },
                                        readOnly = true,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.body2.copy(
                                            color = MaterialTheme.colors.onSurface,
                                        ),
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    extensions?.forEach { extension ->
                                        DropdownMenuItem(
                                            onClick = {
                                                currentExtension = extension
                                                expanded = false
                                            },
                                        ) {
                                            Text("*.$extension")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        CancelButton(
                            onClick = {
                                onCloseRequest(null, null)
                            },
                        )
                        Spacer(Modifier.width(25.dp))
                        val canSubmit = when {
                            !directoryMode && !saveMode -> {
                                currentFileName.isNotEmpty() &&
                                    currentDirectory.resolve(currentFileName).isFile &&
                                    (currentExtension == "*" || currentFileName.endsWith(".$currentExtension"))
                            }

                            else -> true
                        }
                        val confirmText = when {
                            directoryMode -> Strings.CommonSelect
                            saveMode -> Strings.CommonSave
                            else -> Strings.CommonOpen
                        }
                        ConfirmButton(
                            text = string(confirmText),
                            enabled = canSubmit,
                            onClick = {
                                val parent = if (directoryMode) {
                                    currentDirectory.parentFile?.absolutePath ?: ""
                                } else {
                                    currentDirectory.absolutePath
                                }
                                val name = when {
                                    directoryMode -> {
                                        currentDirectory.name
                                    }

                                    saveMode -> {
                                        if (currentFileName.endsWith(".$currentExtension")) {
                                            currentFileName
                                        } else {
                                            "$currentFileName.$currentExtension"
                                        }
                                    }

                                    else -> {
                                        currentFileName
                                    }
                                }
                                onCloseRequest(parent, name)
                            },
                        )
                    }
                }
            }
            splitter {
                visiblePart {
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colors.onBackground),
                    )
                }
                handle {
                    Box(
                        Modifier
                            .markAsHandle()
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                            .width(5.dp)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectoryTree(root: File): Tree<File> =
    Tree {
        DirectoryTree(root)
    }

@Composable
private fun TreeScope.DirectoryTree(
    root: File,
) {
    root.listFiles()
        ?.filter { it.isDirectory }
        ?.sortedBy { it.name }
        ?.forEach { DirectoryNode(it) }
}

@Composable
private fun TreeScope.DirectoryNode(file: File) {
    if (file.listFiles()?.any { it.isDirectory } == true) {
        Branch(
            content = file,
            name = file.name,
        ) {
            DirectoryTree(file)
        }
    } else {
        Leaf(
            content = file,
            name = file.name,
        )
    }
}
