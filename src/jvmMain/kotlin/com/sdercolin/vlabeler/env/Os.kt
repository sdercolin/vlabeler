package com.sdercolin.vlabeler.env

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.sdercolin.vlabeler.util.AppDir
import java.io.File
import java.io.IOException

/**
 * Information about the current operating system.
 */

val osName by lazy { System.getProperty("os.name").toString() }
val osNameWithVersion by lazy { osName + " " + System.getProperty("os.version") }
val osInfo by lazy { osNameWithVersion + " " + System.getProperty("os.arch") }
val isWindows by lazy { osName.toLowerCase(Locale.current).contains("windows") }
val isMacOS by lazy { osName.toLowerCase(Locale.current).contains("mac") }
val isMacOSWithArm by lazy { isMacOS && System.getProperty("os.arch") in listOf("aarch64", "arm64") }
val isLinux by lazy { osName.toLowerCase(Locale.current).contains("linux") }

val isFileSystemCaseSensitive: Boolean by lazy {
    val lowerCaseFile = File(AppDir, "testfile.tmp")
    val upperCaseFile = File(AppDir, "TESTFILE.TMP")
    lowerCaseFile.delete()
    upperCaseFile.delete()
    val isCaseSensitive = try {
        lowerCaseFile.createNewFile()
        if (upperCaseFile.exists()) {
            false // File system is case-insensitive
        } else {
            upperCaseFile.createNewFile() // True if case-sensitive, false if case-insensitive
        }
    } catch (e: IOException) {
        e.printStackTrace()
        false
    } finally {
        lowerCaseFile.delete()
        upperCaseFile.delete()
    }
    isCaseSensitive
}
