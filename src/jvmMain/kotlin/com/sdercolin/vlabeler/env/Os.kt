package com.sdercolin.vlabeler.env

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase

val osName by lazy { System.getProperty("os.name") }
val isWindows by lazy { osName.toLowerCase(Locale.current).contains("windows") }
val isMacOS by lazy { osName.toLowerCase(Locale.current).contains("mac") }
val isLinux by lazy { osName.toLowerCase(Locale.current).contains("linux") }
