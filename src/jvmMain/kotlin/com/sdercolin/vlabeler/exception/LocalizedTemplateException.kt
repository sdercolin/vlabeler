package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.Strings

abstract class LocalizedTemplateException(cause: Throwable?, val localizedMessage: LocalizedJsonString?) :
    Exception(cause) {
    abstract val template: Strings
}
