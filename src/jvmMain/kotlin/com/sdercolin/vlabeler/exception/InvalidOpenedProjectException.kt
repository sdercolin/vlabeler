package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class InvalidOpenedProjectException(
    cause: Throwable?,
) : LocalizedException(Strings.InvalidOpenedProjectException, cause)
