package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

class ProjectParseException(
    cause: Throwable?
) : Exception(string(Strings.ProjectParseException), cause)
