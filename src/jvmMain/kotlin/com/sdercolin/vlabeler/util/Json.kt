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

inline fun <reified T> parseJson(string: String): T {
    return json.decodeFromString(string)
}

inline fun <reified T> toJson(value: T): String {
    return json.encodeToString(value)
}
