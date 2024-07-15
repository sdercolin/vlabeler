package com.sdercolin.vlabeler.util

import java.io.File
import java.security.MessageDigest

fun calculateMD5(file: File): String {
    val md = MessageDigest.getInstance("MD5")
    file.inputStream().use { fis ->
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}
