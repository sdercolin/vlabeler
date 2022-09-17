package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class ProjectUpdateOnSampleException(
    cause: Throwable?,
) : LocalizedException(Strings.ProjectUpdateOnSampleException, cause)
