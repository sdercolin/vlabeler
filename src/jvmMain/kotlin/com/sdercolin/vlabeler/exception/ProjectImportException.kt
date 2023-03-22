package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class ProjectImportException(
    cause: Throwable?,
) : LocalizedException(Strings.ProjectImportException, cause)
