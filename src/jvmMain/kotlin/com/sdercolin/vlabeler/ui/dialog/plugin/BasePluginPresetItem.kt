package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.runtime.Immutable

@Immutable
sealed class BasePluginPresetItem {

    abstract val available: Boolean

    @Immutable
    data class Memory(
        val preset: BasePluginPreset?,
        val slot: Int?,
        val isCurrent: Boolean,
        override val available: Boolean,
    ) : BasePluginPresetItem() {

        fun resolve() = BasePluginPresetTarget.Memory(this)
    }

    @Immutable
    object File : BasePluginPresetItem() {

        override val available: Boolean = true

        fun resolve(file: java.io.File) = BasePluginPresetTarget.File(file)
    }
}

@Immutable
sealed class BasePluginPresetTarget {

    @Immutable
    class Memory(val item: BasePluginPresetItem.Memory) : BasePluginPresetTarget()

    @Immutable
    class File(val file: java.io.File) : BasePluginPresetTarget()
}
