package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.UnicodeNormalizer
import com.sdercolin.vlabeler.util.getDirectory
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

@Composable
fun FileNameNormalizerDialog(appConf: AppConf, finish: () -> Unit) {
    DialogWindow(
        title = string(Strings.FileNameNormalizerDialogTitle),
        icon = painterResource(Resources.iconIco),
        onCloseRequest = finish,
        state = rememberDialogState(width = 600.dp, height = 350.dp),
        resizable = true,
    ) {
        AppTheme(appConf.view) {
            Content()
        }
    }
}

@Composable
private fun Content() {
    val snackbarHostState = remember { SnackbarHostState() }
    var pickingFolder by remember { mutableStateOf(false) }
    var pickingFile by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    Surface {
        Column(modifier = Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center) {
            Text(text = string(Strings.FileNameNormalizerTitle), style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = string(Strings.FileNameNormalizerDescription), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Button(onClick = { pickingFolder = true }, modifier = Modifier.height(50.dp).weight(1f)) {
                    Text(text = string(Strings.FileNameNormalizerHandleFolderButton))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(onClick = { pickingFile = true }, modifier = Modifier.height(50.dp).weight(1f)) {
                    Text(text = string(Strings.FileNameNormalizerHandleFileContentButton))
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Snackbar(
                    it,
                    actionColor = MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
    if (pickingFolder) {
        OpenFileDialog(
            title = string(Strings.FileNameNormalizerTitle),
            directoryMode = true,
        ) { parent, name ->
            pickingFolder = false
            if (parent != null && name != null) {
                val directory = File(parent, name).getDirectory()
                try {
                    var total = 0
                    var converted = 0
                    Files.walkFileTree(
                        directory.toPath(),
                        object : SimpleFileVisitor<Path>() {
                            override fun visitFile(
                                file: Path,
                                attrs: BasicFileAttributes,
                            ): FileVisitResult {
                                if (file.toFile().isFile) {
                                    total++
                                    val fileName = file.fileName.toString()
                                    val convertedFileName = UnicodeNormalizer.convertToNfc(fileName)
                                    if (fileName != convertedFileName) {
                                        converted++
                                        val newFile = file.resolveSibling(convertedFileName)
                                        Files.move(file, newFile)
                                    }
                                }
                                return FileVisitResult.CONTINUE
                            }
                        },
                    )
                    showSnackbar(stringStatic(Strings.FileNameNormalizerHandleFolderSuccess, total, converted))
                } catch (t: Throwable) {
                    showSnackbar(t.localizedMessage ?: t.toString())
                }
            }
        }
    }
    if (pickingFile) {
        OpenFileDialog(
            title = string(Strings.FileNameNormalizerTitle),
        ) { parent, name ->
            pickingFile = false
            if (parent != null && name != null) {
                val file = File(parent, name)
                try {
                    val content = file.readText()
                    val converted = UnicodeNormalizer.convertToNfc(content)
                    if (content != converted) {
                        file.writeText(converted)
                        showSnackbar(stringStatic(Strings.FileNameNormalizerHandleFileSuccess))
                    } else {
                        showSnackbar(stringStatic(Strings.FileNameNormalizerHandleFileNoChange))
                    }
                } catch (t: Throwable) {
                    showSnackbar(t.localizedMessage ?: t.toString())
                }
            }
        }
    }
}
