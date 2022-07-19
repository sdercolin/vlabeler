package com.sdercolin.vlabeler.util

inline fun <T : Any> T.runIf(
    condition: Boolean,
    block: T.() -> T
): T = if (condition) block(this) else this

inline fun <T : Any, R : Any> T.runIfNotNull(
    parameter: R?,
    block: T.(R) -> T
): T = if (parameter != null) block(parameter) else this
