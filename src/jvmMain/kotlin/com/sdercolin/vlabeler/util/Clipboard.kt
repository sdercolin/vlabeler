package com.sdercolin.vlabeler.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object Clipboard {

    fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }
}
