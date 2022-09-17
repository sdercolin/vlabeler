package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class CustomizedItemLoadingException(
    cause: Throwable?,
) : LocalizedException(Strings.CustomizableItemLoadingException, cause)
