package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class ProjectParseException(
    cause: Throwable?,
) : LocalizedException(Strings.ProjectParseException, cause)
