package com.sdercolin.vlabeler.env

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase

val osName get() = System.getProperty("os.name")
val isWindows get() = osName.toLowerCase(Locale.current).contains("windows")
val isMacOS get() = osName.toLowerCase(Locale.current).contains("mac")
