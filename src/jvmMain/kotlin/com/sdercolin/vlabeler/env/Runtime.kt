package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.util.lastPathSection
import java.lang.Runtime.Version

/**
 * The current Java runtime version.
 */
val runtimeVersion: Version? get() = Runtime.version()

/**
 * Whether the application is running in debug mode.
 * In debug mode, the application will not be packaged into a single executable file.
 * Basically it means it's being run by the `run` task of Gradle provided by the Compose Multiplatform framework.
 */
val isDebug: Boolean by lazy {
    val process = ProcessHandle.current().info().command().get().lastPathSection
    process in listOf("java", "java.exe")
}
