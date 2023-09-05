package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class VideoComponentInitializationException(
    cause: Throwable?,
) : LocalizedException(Strings.VideoComponentInitializationException, cause)
