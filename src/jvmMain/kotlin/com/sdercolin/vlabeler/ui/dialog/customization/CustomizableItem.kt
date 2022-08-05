package com.sdercolin.vlabeler.ui.dialog.customization

import java.awt.Desktop
import java.io.File

abstract class CustomizableItem(
    val name: String,
    val author: String,
    val version: Int,
    val displayedName: String,
    val description: String,
    val email: String,
    val website: String,
    val rootFile: File,
    val canRemove: Boolean,
    disabled: Boolean,
    val executable: Boolean
) {

    fun remove() {
        rootFile.deleteRecursively()
    }

    fun revealInExplorer() {
        val file = if (rootFile.isDirectory) rootFile else rootFile.parentFile
        Desktop.getDesktop().open(file)
    }

    var disabled: Boolean = disabled
        private set

    fun toggleDisabled() {
        disabled = !disabled
    }

    open fun execute() {}

    enum class Type {
        MacroPlugin,
        TemplatePlugin,
        Labeler
    }
}
