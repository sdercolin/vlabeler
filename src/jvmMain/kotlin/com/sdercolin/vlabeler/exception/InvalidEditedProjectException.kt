package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class InvalidEditedProjectException(
    cause: Throwable?,
) : LocalizedException(Strings.InvalidEditedProjectException, cause)
