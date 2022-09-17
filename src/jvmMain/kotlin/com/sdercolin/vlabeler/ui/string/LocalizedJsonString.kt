@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.ui.string

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

    fun get() = getByLanguage(currentLanguage) ?: requireNotNull(getByLanguage(Language.default))

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
