package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class RequiredLabelerNotFoundException(
    private val labelerName: String,
    private val version: String,
) : LocalizedException(Strings.LoadProjectErrorLabelerNotFound, null) {

    override val args: Array<out Any?>
        get() = arrayOf(labelerName, version)
}
