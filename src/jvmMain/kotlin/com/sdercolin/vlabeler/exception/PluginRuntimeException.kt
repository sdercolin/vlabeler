package com.sdercolin.vlabeler.exception

// TODO: allow localized text from an expected error
class PluginRuntimeException(cause: Throwable?) : Exception(cause?.message, cause)
