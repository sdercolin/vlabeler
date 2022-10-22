package com.sdercolin.vlabeler.tracking.event

import com.sdercolin.vlabeler.env.Locale
import kotlinx.serialization.Serializable

@Serializable
data class LocaleInfo(
    val language: String,
    val script: String,
    val region: String,
    val tag: String,
    val fullTag: String,
) {

    companion object {

        fun get() = Locale.run {
            LocaleInfo(
                language = language,
                script = script,
                region = country,
                tag = "$language-$country",
                fullTag = toLanguageTag(),
            )
        }
    }
}
