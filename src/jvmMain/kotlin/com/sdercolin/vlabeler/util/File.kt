package com.sdercolin.vlabeler.util

import java.io.File

fun File.getDirectory(): File = if (isDirectory) this else parentFile
