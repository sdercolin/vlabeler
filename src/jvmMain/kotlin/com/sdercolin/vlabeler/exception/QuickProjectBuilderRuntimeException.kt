package com.sdercolin.vlabeler.exception

import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.Strings

class QuickProjectBuilderRuntimeException(cause: Throwable?, localizedMessage: LocalizedJsonString?) :
    LocalizedTemplateException(cause, localizedMessage) {

    override val template: Strings = Strings.ProjectConstructorRuntimeExceptionTemplate
}
