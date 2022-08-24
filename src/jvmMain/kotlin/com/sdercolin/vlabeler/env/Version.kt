package com.sdercolin.vlabeler.env

import java.util.Properties

val appVersion: String by lazy {
    val stream = Thread.currentThread().contextClassLoader.getResource("app.properties")?.openStream()
    val properties = Properties().apply { load(requireNotNull(stream)) }
    properties.getProperty("app.version")
}
