package com.sdercolin.vlabeler.env

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.sdercolin.vlabeler.util.AppDir
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


/**
 * Information about the current operating system.
 */

val osName by lazy { System.getProperty("os.name").toString() }
val osNameWithVersion by lazy { osName + " " + System.getProperty("os.version") }
val osArch by lazy {
    val arch = System.getProperty("os.arch").toString()
    if (isMacOS) {
        val systemIsArm = isMacOSWithArm
        val jvmIsArm = arch == "aarch64"
        when {
            systemIsArm && jvmIsArm -> "arm64"
            systemIsArm && !jvmIsArm -> "x86_64 (Rosetta)"
            !systemIsArm && jvmIsArm -> "arm64"// This is not possible
            else -> arch
        }
    } else {
        arch
    }
}
val osInfo by lazy { "$osNameWithVersion $osArch" }
val isWindows by lazy { osName.toLowerCase(Locale.current).contains("windows") }
val isMacOS by lazy { osName.toLowerCase(Locale.current).contains("mac") }
val isMacOSWithArm: Boolean by lazy {
    try {
        val process = ProcessBuilder("uname", "-m").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.readLine() == "arm64"
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
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
