package com.sdercolin.vlabeler.env

/**
 * Sets directory mode for the Awt file dialog.
 */
fun setAwtDirectoryMode(on: Boolean) {
    if (isMacOS) {
        System.setProperty("apple.awt.fileDialogForDirectories", on.toString())
    }
}
