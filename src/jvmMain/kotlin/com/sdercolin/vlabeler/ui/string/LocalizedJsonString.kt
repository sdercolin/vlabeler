@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.ui.string

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.exception.LocalizedStringDeserializedException
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A localized string that stored in JSON, used by labelers and plugins. Two types of values are supported:
 * 1. A string value, which is a string that is used by [Language.default].
 * 2. A map of language code to string value, e.g. {"en": "Hello", "zh": "你好"}. An entry is used when the current
 *    language code starts with the language code in the entry. e.g. If the current language code is "en-US", the entry
 *    with key "en" is used. Note that a map without an entry for [Language.default] is not allowed.
 */
@Serializable(with = LocalizedJsonStringSerializer::class)
data class LocalizedJsonString(
    val localized: Map<String, String>,
) {
    private fun getByLanguage(language: Language): String? {
        val requiredCode = language.code
        localized.forEach { (code, value) ->
            if (requiredCode.startsWith(code)) {
                return value
            }
        }
        return null
    }

    fun getCertain(language: Language) = getByLanguage(language) ?: requireNotNull(getByLanguage(Language.default))

    @Composable
    fun get() = getCertain(LocalLanguage.current)

    fun validate(): LocalizedJsonString {
        if (localized.isEmpty()) {
            throw LocalizedStringDeserializedException("Empty localized string")
        }
        getByLanguage(Language.default)
            ?: throw LocalizedStringDeserializedException(
                "Default language ${Language.default.code} is not included " +
                    "in localized string ${localized.stringifyJson()}",
            )
        return this
    }
}

fun String.toLocalized() = LocalizedJsonString(mapOf(Language.default.code to this))

@Serializer(LocalizedJsonString::class)
object LocalizedJsonStringSerializer : KSerializer<LocalizedJsonString> {

    override fun serialize(encoder: Encoder, value: LocalizedJsonString) {
        if (value.localized.size == 1) {
            encoder.encodeString(value.localized.values.first())
        } else {
            val map = value.localized.mapValues { (_, v) -> JsonPrimitive(v) }
            encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(map))
        }
    }

    override fun deserialize(decoder: Decoder): LocalizedJsonString {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        runCatching { element.jsonPrimitive.content }
            .getOrNull()
            ?.let { return LocalizedJsonString(mapOf(Language.default.code to it)) }
        return element.jsonObject.toMap()
            .mapValues { it.value.jsonPrimitive.content }
            .let { LocalizedJsonString(it) }
            .validate()
    }
}
