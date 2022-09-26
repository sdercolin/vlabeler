@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.FileWithEncoding
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.currentLanguage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.Charset

/**
 * Serializable dynamic type map
 */
class ParamMap(private val map: Map<String, Any>) : Map<String, Any> {

    override val entries: Set<Map.Entry<String, Any>>
        get() = map.entries

    override val keys: Set<String>
        get() = map.keys

    override val size: Int
        get() = map.size

    override val values: Collection<Any>
        get() = map.values

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun get(key: String): Any? = map[key]

    override fun containsValue(value: Any): Boolean = map.containsValue(value)

    override fun containsKey(key: String): Boolean = map.containsKey(key)

    fun resolve(project: Project?, js: JavaScript?) = buildJsonObject {
        for ((key, value) in map) {
            put(key, resolveItem(value, project, js))
        }
    }
}

@Serializable
class ParamTypedMap(private val map: Map<String, TypedValue>) {

    @Serializable(with = TypedValueSerializer::class)
    class TypedValue(val type: String, val value: Any)

    fun toParamMap() = ParamMap(map.mapValues { it.value.value })

    companion object {

        fun from(paramMap: ParamMap, paramDefs: List<Parameter<*>>): ParamTypedMap {
            val map = mutableMapOf<String, TypedValue>()
            for ((key, value) in paramMap) {
                val paramDef = paramDefs.find { it.name == key }
                if (paramDef != null) {
                    map[key] = TypedValue(paramDef.type, value)
                }
            }
            return ParamTypedMap(map)
        }

        @Serializer(TypedValue::class)
        object TypedValueSerializer : KSerializer<TypedValue> {
            override fun deserialize(decoder: Decoder): TypedValue {
                require(decoder is JsonDecoder)
                val element = decoder.decodeJsonElement().jsonObject
                val type = element.getValue("type").jsonPrimitive.content
                val rawValue = element.getValue("value")
                val value: Any = when (type) {
                    "integer" -> rawValue.jsonPrimitive.int
                    "float" -> rawValue.jsonPrimitive.float
                    "boolean" -> rawValue.jsonPrimitive.boolean
                    "string" -> rawValue.jsonPrimitive.content
                    "enum" -> json.decodeFromJsonElement<LocalizedJsonString>(rawValue)
                    "entrySelector" -> json.decodeFromJsonElement<EntrySelector>(rawValue)
                    "file" -> json.decodeFromJsonElement<FileWithEncoding>(rawValue)
                    else -> throw IllegalArgumentException("Unknown type: $type")
                }
                return TypedValue(type, value)
            }

            override fun serialize(encoder: Encoder, value: TypedValue) {
                require(encoder is JsonEncoder)
                val json = buildJsonObject {
                    put("type", JsonPrimitive(value.type))
                    put(
                        "value",
                        when (value.type) {
                            "integer" -> JsonPrimitive(value.value as Int)
                            "float" -> JsonPrimitive(value.value as Float)
                            "boolean" -> JsonPrimitive(value.value as Boolean)
                            "string" -> JsonPrimitive(value.value as String)
                            "enum" -> json.encodeToJsonElement(value.value as LocalizedJsonString)
                            "entrySelector" -> json.encodeToJsonElement(value.value as EntrySelector)
                            "file" -> json.encodeToJsonElement(value.value as FileWithEncoding)
                            else -> throw IllegalArgumentException("Unknown type: ${value.type}")
                        },
                    )
                }
                encoder.encodeJsonElement(json)
            }
        }
    }
}

fun Map<String, Any>.toParamMap() = ParamMap(this)

fun ParamMap?.orEmpty() = this ?: ParamMap(mapOf())

private fun resolveItem(value: Any?, project: Project?, js: JavaScript?): JsonElement {
    if (value == null) return JsonNull
    return when (value) {
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is EntrySelector -> {
            requireNotNull(project) { "Project is required to resolve EntrySelector" }
            requireNotNull(js) { "JavaScript is required to resolve EntrySelector" }
            buildJsonArray {
                for (index in value.select(project.entries, project.labelerConf, js)) {
                    add(JsonPrimitive(index))
                }
            }
        }
        is LocalizedJsonString -> JsonPrimitive(value.getCertain(currentLanguage))
        is FileWithEncoding -> {
            val file = value.file?.toFileOrNull(ensureExists = true, ensureIsFile = true)
            if (file == null) {
                JsonNull
            } else {
                JsonPrimitive(file.readText(value.encoding?.let { charset(it) } ?: Charset.defaultCharset()))
            }
        }
        else -> throw IllegalArgumentException("$value is not supported")
    }
}
