package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.Url
import java.awt.Desktop
import java.io.File

abstract class CustomizableItem(
    val name: String,
    val author: String,
    val version: Int,
    val displayedName: LocalizedJsonString,
    val description: LocalizedJsonString,
    val email: String,
    val website: String,
    val rootFile: File,
    val canRemove: Boolean,
    disabled: Boolean,
) {

    fun remove(): Boolean = rootFile.deleteRecursively().also {
        if (it) {
            Log.debug("Removed CustomizableItem $rootFile")
        }
    }

    fun revealInExplorer() {
        val file = if (rootFile.isDirectory) rootFile else rootFile.parentFile
        Desktop.getDesktop().open(file)
    }

    fun hasEmail(): Boolean {
        return email.isNotEmpty()
    }

    fun openEmail() {
        Url.open("mailto:$email")
    }

    fun hasWebsite(): Boolean {
        return website.isNotBlank()
    }

    fun openWebsite() {
        val url = website.takeIf { it.isNotBlank() } ?: return
        Url.open(url)
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
