package com.sdercolin.vlabeler.env

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase

private val osName get() = System.getProperty("os.name")
val isMacOs get() = osName.toLowerCase(Locale.current).contains("mac")