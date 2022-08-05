package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

class PluginRuntimeException(
    cause: Throwable?,
    expected: Boolean
) : Exception(
    if (!expected) string(Strings.PluginRuntimeUnexpectedException) else cause?.message,
    cause
)
