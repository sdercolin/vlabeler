package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.util.lastPathSection
import java.lang.Runtime.Version

val runtimeVersion: Version? get() = Runtime.version()

val isDebug: Boolean by lazy {
    ProcessHandle.current().info().command().get().lastPathSection == "java"
}
