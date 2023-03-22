package com.sdercolin.vlabeler.ui.dialog.importentries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.io.ImportedModule
import com.sdercolin.vlabeler.ui.ProjectStore
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

class ImportEntriesDialogState(
    private val finish: () -> Unit,
    private val projectStore: ProjectStore,
    importedModules: List<ImportedModule>,
) {

    val forceReplaceContent = projectStore.requireProject().labelerConf.continuous

    var replaceContent by mutableStateOf(forceReplaceContent)

    val existingModuleNames = projectStore.requireProject().modules.map { it.name }

    val items = importedModules.map { importedModule ->
        val defaultTargetName = importedModule.name.takeIf { it in existingModuleNames }
        val compatible = importedModule.isCompatibleWith(projectStore.requireProject())
        Item(importedModule, defaultTargetName, compatible)
    }

    val isValid get() = items.all { it.isValid }

    fun submit() {
        items
            .filter { it.selected }
            .mapNotNull {
                val moduleName = it.targetName
                if (moduleName != null) {
                    moduleName to it.importedModule.entries
                } else {
                    null
                }
            }
            .let { projectStore.importEntries(it, replaceContent) }
        finish()
    }

    fun cancel() {
        finish()
    }

    class Item(
        val importedModule: ImportedModule,
        defaultTargetName: String?,
        val compatible: Boolean,
    ) {

        var selected: Boolean by mutableStateOf(compatible && defaultTargetName != null)
            private set

        fun toggleSelected(selected: Boolean) {
            this.selected = selected
            if (!selected) {
                targetName = null
            }
        }

        var targetName: String? by mutableStateOf(defaultTargetName)
            private set

        fun selectTarget(name: String?) {
            targetName = name
            selected = name != null
        }

        val isValid: Boolean
            get() = selected.not() || targetName != null

        @Composable
        fun getName(): String = importedModule.name.ifEmpty { string(Strings.CommonRootModuleName) }

        @Composable
        fun getSummaryTitle(): String = string(Strings.ImportEntriesDialogItemSummaryTitle, importedModule.entries.size)

        @Composable
        fun getSummaryContent(): String = if (compatible) {
            importedModule.entries.joinToString(", ") { it.name }
        } else {
            string(Strings.ImportEntriesDialogItemIncompatible)
        }
    }
}
