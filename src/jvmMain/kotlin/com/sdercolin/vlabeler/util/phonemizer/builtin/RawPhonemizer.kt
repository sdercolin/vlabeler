package com.sdercolin.vlabeler.util.phonemizer.builtin

// ktlint-disable
import com.sdercolin.vlabeler.util.phonemizer.PhonemizerLanguage
import com.sdercolin.vlabeler.util.phonemizer.PhonemizerPlugin

object RawPhonemizer : PhonemizerPlugin {
    override val id = PhonemizerLanguage.Raw.id
    override val displayNameKey = PhonemizerLanguage.Raw.displayNameKey
    override fun phonemize(text: String) = text.split("[\\s,;]+".toRegex()).filter { it.isNotBlank() }
}
