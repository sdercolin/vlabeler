package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

@Immutable
sealed class BasePluginPresetItem {

    abstract val available: Boolean

    @Immutable
    data class Memory(
        val pluginName: String?,
        val slot: Int?,
        val isCurrent: Boolean,
        override val available: Boolean,
    ) : BasePluginPresetItem() {

        fun resolve() = BasePluginPresetTarget.Memory(this)

        @Composable
        override fun getImportText() = if (slot == null) {
            string(Strings.PluginDialogImportFromSavedParams)
        } else {
            string(
                Strings.PluginDialogImportFromSlot,
                slot + 1,
                pluginName ?: string(Strings.PluginDialogEmptySlotName),
            )
        }

        @Composable
        override fun getExportText() = if (slot == null) {
            string(Strings.PluginDialogExportToSavedParams)
        } else {
            string(
                Strings.PluginDialogExportToSlot,
                slot + 1,
                pluginName ?: string(Strings.PluginDialogEmptySlotName),
            )
        }
    }

    @Immutable
    object File : BasePluginPresetItem() {

        override val available: Boolean = true

        fun resolve(file: java.io.File) = BasePluginPresetTarget.File(file)

        @Composable
        override fun getImportText() = string(Strings.PluginDialogImportFromFile)

        @Composable
        override fun getExportText() = string(Strings.PluginDialogExportToFile)
    }

    @Composable
    abstract fun getImportText(): String

    @Composable
    abstract fun getExportText(): String
}

@Immutable
sealed class BasePluginPresetTarget {

    @Immutable
    class Memory(val item: BasePluginPresetItem.Memory) : BasePluginPresetTarget()

    @Immutable
    class File(val file: java.io.File) : BasePluginPresetTarget()
}
