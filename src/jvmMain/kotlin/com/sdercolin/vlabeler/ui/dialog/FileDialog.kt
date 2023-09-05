package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.AwtWindow
import com.sdercolin.vlabeler.UseCustomFileDialog
import com.sdercolin.vlabeler.debug.DebugState
import com.sdercolin.vlabeler.env.isWindows
import com.sdercolin.vlabeler.env.setAwtDirectoryMode
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.nfd.NativeFileDialog
import java.awt.FileDialog
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

@Composable
fun OpenFileDialog(
    title: String,
    initialDirectory: String? = null,
    initialFileName: String? = null,
    extensions: List<String>? = null,
    directoryMode: Boolean = false,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) = FileDialog(LOAD, title, initialDirectory, initialFileName, extensions, directoryMode, onCloseRequest)

@Composable
fun SaveFileDialog(
    title: String,
    initialDirectory: String? = null,
    initialFileName: String? = null,
    extensions: List<String>? = null,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) = FileDialog(SAVE, title, initialDirectory, initialFileName, extensions, false, onCloseRequest)

@Composable
private fun FileDialog(
    mode: Int,
    title: String,
    initialDirectory: String?,
    initialFileName: String?,
    extensions: List<String>?,
    directoryMode: Boolean,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) = when {
    DebugState.forceUseCustomFileDialog || UseCustomFileDialog.current -> CustomFileDialog(
        mode,
        title,
        initialDirectory,
        initialFileName,
        extensions,
        directoryMode,
        onCloseRequest,
    )

    !isWindows -> AwtWindow(
        create = {
            if (directoryMode) setAwtDirectoryMode(true)

            object : FileDialog(null as Frame?, title, mode) {
                override fun setVisible(value: Boolean) {
                    super.setVisible(value)
                    if (value) {
                        onCloseRequest(directory, file)
                    }
                }
            }.apply {
                initialDirectory?.let { directory = it }
                initialFileName?.let { file = it }
                if (extensions != null) {
                    filenameFilter = FilenameFilter { _, name ->
                        extensions.any {
                            name.endsWith(it)
                        }
                    }
                }
            }
        },
        dispose = {
            if (directoryMode) setAwtDirectoryMode(false)
            it.dispose()
        },
    )

    else -> LwjglFileDialog(
        mode,
        initialDirectory,
        initialFileName,
        extensions,
        directoryMode,
        onCloseRequest,
    )
}

@Composable
private fun LwjglFileDialog(
    mode: Int,
    initialDirectory: String?,
    initialFileName: String?,
    extensions: List<String>?,
    directoryMode: Boolean,
    onCloseRequest: (parent: String?, name: String?) -> Unit,
) {
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pathPointer = MemoryUtil.memAllocPointer(1)

            val filterList = extensions?.joinToString(",")
            val defaultPathForFile = if (initialDirectory != null && initialFileName != null) {
                File(initialDirectory, initialFileName).absolutePath
            } else null
            val result = when {
                mode == SAVE -> {
                    NativeFileDialog.NFD_SaveDialog(filterList, defaultPathForFile, pathPointer)
                }

                directoryMode -> {
                    NativeFileDialog.NFD_PickFolder(initialDirectory ?: HomeDir.absolutePath, pathPointer)
                }

                else -> {
                    NativeFileDialog.NFD_OpenDialog(filterList, defaultPathForFile, pathPointer)
                }
            }
            if (result == NativeFileDialog.NFD_OKAY) {
                val file = pathPointer.stringUTF8.toFile()
                NativeFileDialog.nNFD_Free(pathPointer[0])
                onCloseRequest(file.parent, file.name)
            } else {
                onCloseRequest(null, null)
            }
            MemoryUtil.memFree(pathPointer)
        }
    }
}
