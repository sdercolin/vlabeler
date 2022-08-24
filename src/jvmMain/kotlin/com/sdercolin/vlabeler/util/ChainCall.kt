package com.sdercolin.vlabeler.util

inline fun <T> T.runIf(
    condition: Boolean,
    block: T.() -> T,
): T = if (condition) block(this) else this

inline fun <T> T.runIfNotNull(
    condition: Boolean,
    block: T.() -> T?,
): T? = if (condition) block(this) else this

inline fun <T, R : Any> T.runIfHave(
    parameter: R?,
    block: T.(R) -> T,
): T = if (parameter != null) block(parameter) else this
