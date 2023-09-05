package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class VideoFileNotFoundException(
    cause: Throwable?,
    private val path: String,
    private val supportedExtensions: List<String>,
) : LocalizedException(Strings.VideoComponentInitializationException, cause) {

    override val args: Array<out Any?>
        get() = arrayOf(path, supportedExtensions.joinToString("/"))
}
