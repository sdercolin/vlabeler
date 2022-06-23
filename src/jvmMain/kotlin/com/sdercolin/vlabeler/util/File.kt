package com.sdercolin.vlabeler.util

import java.io.File

fun File.getDirectory() = if (isDirectory) this else parentFile