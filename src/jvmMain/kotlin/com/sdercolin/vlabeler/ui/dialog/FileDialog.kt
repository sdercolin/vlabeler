package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

@Composable
fun FileDialog(
    title: String,
    initialDirectory: String? = null,
    extensions: List<String>? = null,
    onCloseRequest: (directory: String?, result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(null as Frame?, title, LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory, file)
                }
            }
        }.apply {
            directory = initialDirectory
            if (extensions != null) {
                filenameFilter = FilenameFilter { _, name ->
                    extensions.any {
                        name.endsWith(".$it")
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)
