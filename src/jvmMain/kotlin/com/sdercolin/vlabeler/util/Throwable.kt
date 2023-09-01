package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.exception.LocalizedException
import com.sdercolin.vlabeler.exception.LocalizedTemplateException
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.stringCertain

fun Throwable.getLocalizedMessage(language: Language): String = when (this) {
    is LocalizedException -> stringCertain(this.stringKey, language)
    is LocalizedTemplateException -> stringCertain(
        template,
        language,
        this.localizedMessage?.getCertain(language),
    )
    else -> this.message
} ?: this.toString()
