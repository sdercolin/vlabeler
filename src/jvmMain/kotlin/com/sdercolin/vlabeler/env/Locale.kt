package com.sdercolin.vlabeler.env

import java.util.Locale

/**
 * The default locale of the JVM.
 */
val Locale: Locale by lazy {
    java.util.Locale.getDefault()
}
