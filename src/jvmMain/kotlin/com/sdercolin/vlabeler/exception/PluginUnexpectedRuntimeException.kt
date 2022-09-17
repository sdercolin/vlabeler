package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class PluginUnexpectedRuntimeException(cause: Throwable?) : LocalizedException(
    Strings.PluginRuntimeUnexpectedException,
    cause,
)
