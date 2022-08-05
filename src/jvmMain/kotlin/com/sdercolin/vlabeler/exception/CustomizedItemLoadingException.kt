package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

class CustomizedItemLoadingException(
    cause: Throwable?
) : Exception(string(Strings.CustomizedItemLoadingException), cause)
