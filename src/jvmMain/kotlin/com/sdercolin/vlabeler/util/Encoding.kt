package com.sdercolin.vlabeler.util

import org.apache.tika.parser.txt.CharsetDetector

val AvailableEncodings = listOf(
    "UTF-8",
    "Shift-JIS",
    "GBK",
    "ISO-8859-1",
    "Windows-1251",
    "Windows-1252",
    "GB2312",
    "ISO-8859-9",
    "EUC-JP",
    "EUC-KR",
)

val DefaultEncoding = AvailableEncodings[0]

fun encodingNameEquals(first: String, second: String) =
    cleanEncodingName(first).equals(cleanEncodingName(second), ignoreCase = true)

private fun cleanEncodingName(name: String) = name
    .replace("_", " ")
    .replace("-", " ")

fun ByteArray.detectEncoding(): String? {
    val detector = CharsetDetector()
    detector.setText(this)
    return detector.detect().name
}
