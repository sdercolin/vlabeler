package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.exception.LocalizedException
import com.sdercolin.vlabeler.exception.LocalizedTemplateException
import com.sdercolin.vlabeler.ui.string.*

fun Throwable.getLocalizedMessage(language: Language): String {
    // Check the direct cause
    // Deeper cause is expected to be localized
    val message = getLocalizedMessageOrNot(language) ?: toString()
    val causeMessage = cause?.getLocalizedMessageOrNot(language)
    return if (causeMessage != null) {
        "$message\n\nCaused by:\n$causeMessage"
    } else {
        message
    }
}

private fun Throwable.getLocalizedMessageOrNot(language: Language): String? = when (this) {
    is LocalizedException -> getText(language)
    is LocalizedTemplateException -> stringCertain(
        template,
        language,
        this.localizedMessage?.getCertain(language),
    )
    else -> null
}
