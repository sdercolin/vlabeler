package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.model.Parameter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
    serializersModule = getSerializersModule()
}

val jsonMinified = Json {
    isLenient = true
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = false
    serializersModule = getSerializersModule()
}

private fun getSerializersModule() = SerializersModule {
    polymorphic(Parameter::class) {
        subclass(Parameter.IntParam::class)
        subclass(Parameter.FloatParam::class)
        subclass(Parameter.BooleanParam::class)
        subclass(Parameter.StringParam::class)
        subclass(Parameter.EnumParam::class)
        subclass(Parameter.EntrySelectorParam::class)
        subclass(Parameter.FileParam::class)
        subclass(Parameter.RawFileParam::class)
    }
}

inline fun <reified T> String.parseJson(): T {
    return json.decodeFromString(this)
}

inline fun <reified T> T.stringifyJson(): String {
    return json.encodeToString(this)
}
