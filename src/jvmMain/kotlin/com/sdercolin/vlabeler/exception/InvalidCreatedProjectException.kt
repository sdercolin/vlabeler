package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class InvalidCreatedProjectException(
    cause: Throwable?,
) : LocalizedException(Strings.InvalidCreatedProjectException, cause)
