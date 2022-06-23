package com.sdercolin.vlabeler.env

fun setAwtDirectoryMode(on: Boolean) {
    if (isMacOS) {
        System.setProperty("apple.awt.fileDialogForDirectories", on.toString())
    }
}
