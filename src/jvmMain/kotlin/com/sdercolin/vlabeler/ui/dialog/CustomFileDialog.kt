package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import cafe.adriel.bonsai.core.Bonsai
import cafe.adriel.bonsai.core.BonsaiStyle
import cafe.adriel.bonsai.core.node.Node
import cafe.adriel.bonsai.core.tree.Tree
import cafe.adriel.bonsai.filesystem.FileSystemTree
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.toFile
import okio.Path
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.SplitPaneState
import java.awt.Cursor

@Composable
fun CustomFileDialog(
    mode: Int,
    title: String,
    initialDirectory: String?,
    initialFileName: String?,
    extensions: List<String>?,
    directoryMode: Boolean,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
    appConf: AppConf,
) {
    val fileFree = FileSystemTree(initialDirectory?.toFile() ?: HomeDir)
    Dialog(
        title = title,
        icon = painterResource(Resources.iconIco),
        onCloseRequest = { onCloseRequest(null, null) },
        state = rememberDialogState(width = 800.dp, height = 600.dp),
        resizable = true,
    ) {
        AppTheme(appConf.view) {
            Content(
                mode = mode,
                fileFree = fileFree,
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
    fileFree: Tree<Path>,
    initialFileName: String?,
    extensions: List<String>?,
    directoryMode: Boolean,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) {
    val splitPaneState = remember {
        SplitPaneState(
            initialPositionPercentage = 0.3f,
            moveEnabled = true,
        )
    }
    var selectedNode by remember { mutableStateOf<Node<Path>?>(null) }
    Surface(modifier = Modifier.fillMaxSize()) {
        HorizontalSplitPane(splitPaneState = splitPaneState) {
            first {
                Bonsai(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    tree = fileFree,
                    style = BonsaiStyle(
                        nodeNameTextStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                        toggleIconColorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface),
                    ),
                    onClick = {
                        selectedNode = it
                    },
                )
            }
            second {
                Box {
                    Text(selectedNode?.name ?: "null")
                }
            }
            splitter {
                handle {
                    Box(
                        Modifier
                            .markAsHandle()
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                            .width(1.dp)
                            .background(color = MaterialTheme.colors.onSurface)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}
