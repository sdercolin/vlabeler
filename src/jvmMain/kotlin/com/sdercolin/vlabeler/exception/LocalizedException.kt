package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.LocalizedText
import com.sdercolin.vlabeler.ui.string.Strings

/**
 * A wrapped exception that contains a localized message.
 */
abstract class LocalizedException(override val stringKey: Strings, cause: Throwable? = null) :
    Exception(cause), LocalizedText
