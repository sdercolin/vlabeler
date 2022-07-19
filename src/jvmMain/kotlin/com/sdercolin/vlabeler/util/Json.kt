package com.sdercolin.vlabeler.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

inline fun <reified T> String.parseJson(): T {
    return json.decodeFromString(this)
}

inline fun <reified T> T.stringifyJson(): String {
    return json.encodeToString(this)
}
