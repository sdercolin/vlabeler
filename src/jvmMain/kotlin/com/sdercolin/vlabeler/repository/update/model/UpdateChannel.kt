package com.sdercolin.vlabeler.repository.update.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.string.*
import kotlinx.serialization.Serializable

@Serializable
@Immutable
enum class UpdateChannel : LocalizedText {
    Stable,
    Preview,
    ;

    override val stringKey: Strings
        get() = when (this) {
            Stable -> Strings.UpdateChannelStable
            Preview -> Strings.UpdateChannelPreview
        }
}
