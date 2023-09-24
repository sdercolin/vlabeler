package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class CustomizableItemLoadingException(
    cause: Throwable?,
) : LocalizedException(Strings.CustomizableItemLoadingException, cause)
