package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

class InvalidProjectException(
    cause: Throwable?
) : Exception(string(Strings.InvalidProjectException), cause)
