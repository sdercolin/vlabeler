package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.util.lastPathSection
import java.lang.Runtime.Version

val runtimeVersion: Version? get() = Runtime.version()

val isDebug: Boolean by lazy {
    val process = ProcessHandle.current().info().command().get().lastPathSection
    process in listOf("java", "java.exe")
}
