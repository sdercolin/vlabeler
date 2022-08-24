package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.util.toUri
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
) {

    fun remove() {
        rootFile.deleteRecursively()
    }

    fun revealInExplorer() {
        val file = if (rootFile.isDirectory) rootFile else rootFile.parentFile
        Desktop.getDesktop().open(file)
    }

    fun hasEmail(): Boolean {
        return email.isNotEmpty()
    }

    fun openEmail() {
        Desktop.getDesktop().browse("mailto:$email".toUri())
    }

    fun hasWebsite(): Boolean {
        return website.isNotBlank()
    }

    fun openWebsite() {
        val uri = website.takeIf { it.isNotBlank() }?.toUri() ?: return
        Desktop.getDesktop().browse(uri)
    }

    var disabled: Boolean by mutableStateOf(disabled)
        private set

    fun toggleDisabled() {
        disabled = !disabled
    }

    open fun canExecute(): Boolean {
        return false
    }

    open fun execute() {}

    enum class Type {
        MacroPlugin,
        TemplatePlugin,
        Labeler
    }
}
