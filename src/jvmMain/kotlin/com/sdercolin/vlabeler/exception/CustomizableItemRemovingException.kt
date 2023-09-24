package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.Strings

class CustomizableItemRemovingException(
    cause: Throwable?,
) : LocalizedException(Strings.CustomizableItemRemovingException, cause)
