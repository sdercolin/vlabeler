package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.LocalizedJsonString

class PluginRuntimeException(cause: Throwable?, val localizedMessage: LocalizedJsonString?) : Exception(cause)
