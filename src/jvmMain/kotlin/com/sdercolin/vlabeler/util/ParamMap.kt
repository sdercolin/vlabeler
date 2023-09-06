@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.FileWithEncoding
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.ParamTypedMap.TypedValue
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

/**
 * Serializable dynamic type map. Basically it should contain all the parameters of a [BasePlugin].
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

    fun resolveItem(name: String, project: Project?, js: JavaScript?): JsonElement {
        return resolveItem(map[name], project, js)
    }

    private fun resolveItem(value: Any?, project: Project?, js: JavaScript?): JsonElement {
        if (value == null) return JsonNull
        return when (value) {
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is EntrySelector -> {
                if (project != null && js != null) {
                    buildJsonArray {
                        for (index in value.select(project.currentModule.entries, project.labelerConf, js)) {
                            add(JsonPrimitive(index))
                        }
                    }
                } else {
                    Log.error("EntrySelector is not supported in this context")
                    JsonNull
                }
            }
            is FileWithEncoding -> {
                val file = value.file?.toFileOrNull(ensureExists = true, ensureIsFile = true)
                if (file == null) {
                    JsonNull
                } else {
                    JsonPrimitive(file.readTextByEncoding(value.encoding))
                }
            }
            else -> throw IllegalArgumentException("$value is not supported")
        }
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ParamMap && map == other.map
    }
}

/**
 * Serializable dynamic type map with [TypedValue]. We don't expect it to contain all the parameters of a [BasePlugin],
 * instead, when it's actually used, it will be converted to a [ParamMap] with [resolve], which will fill all the
 * missing parameters with default values.
 */
@Serializable
class ParamTypedMap(private val map: Map<String, TypedValue>) {

    @Serializable(with = TypedValueSerializer::class)
    class TypedValue(val type: String, val value: Any)

    fun stripFilePaths() = ParamTypedMap(
        map.mapValues { (_, value) ->
            when (value.type) {
                Parameter.FileParam.Type -> {
                    TypedValue(value.type, FileWithEncoding("*", null))
                }
                Parameter.RawFileParam.Type -> {
                    TypedValue(value.type, "*")
                }
                else -> {
                    value
                }
            }
        },
    )

    fun get(key: String): TypedValue? = map[key]

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ParamTypedMap && map == other.map
    }

    companion object {

        fun from(paramMap: ParamMap?, paramDefs: List<Parameter<*>>): ParamTypedMap? {
            if (paramMap == null) return null
            val map = mutableMapOf<String, TypedValue>()
            for ((key, value) in paramMap) {
                val paramDef = paramDefs.find { it.name == key }
                if (paramDef != null && paramDef.defaultValue != value) {
                    map[key] = TypedValue(paramDef.type, value)
                }
            }
            return ParamTypedMap(map).takeIf { it.map.isNotEmpty() }
        }

        @Serializer(TypedValue::class)
        object TypedValueSerializer : KSerializer<TypedValue> {
            override fun deserialize(decoder: Decoder): TypedValue {
                require(decoder is JsonDecoder)
                val element = decoder.decodeJsonElement().jsonObject
                val type = element.getValue("type").jsonPrimitive.content
                val rawValue = element.getValue("value")
                val value: Any = when (type) {
                    Parameter.IntParam.Type -> rawValue.jsonPrimitive.int
                    Parameter.FloatParam.Type -> rawValue.jsonPrimitive.float
                    Parameter.BooleanParam.Type -> rawValue.jsonPrimitive.boolean
                    Parameter.StringParam.Type -> rawValue.jsonPrimitive.content
                    Parameter.EnumParam.Type -> rawValue.jsonPrimitive.content
                    Parameter.EntrySelectorParam.Type -> json.decodeFromJsonElement<EntrySelector>(rawValue)
                    Parameter.FileParam.Type -> json.decodeFromJsonElement<FileWithEncoding>(rawValue)
                    Parameter.RawFileParam.Type -> rawValue.jsonPrimitive.content
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
                            Parameter.IntParam.Type -> JsonPrimitive(value.value as Int)
                            Parameter.FloatParam.Type -> JsonPrimitive(value.value as Float)
                            Parameter.BooleanParam.Type -> JsonPrimitive(value.value as Boolean)
                            Parameter.StringParam.Type -> JsonPrimitive(value.value as String)
                            Parameter.EnumParam.Type -> JsonPrimitive(value.value as String)
                            Parameter.EntrySelectorParam.Type -> json.encodeToJsonElement(value.value as EntrySelector)
                            Parameter.FileParam.Type -> json.encodeToJsonElement(value.value as FileWithEncoding)
                            Parameter.RawFileParam.Type -> JsonPrimitive(value.value as String)
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
fun Map<String, TypedValue>.toParamTypedMap() = ParamTypedMap(this)

fun ParamMap?.orEmpty() = this ?: ParamMap(mapOf())
fun ParamTypedMap?.orEmpty() = this ?: ParamTypedMap(mapOf())

fun ParamTypedMap?.resolve(plugin: BasePlugin): ParamMap = plugin.parameterDefs
    .associate { it.name to (this?.get(it.name)?.value ?: it.defaultValue) }
    .toParamMap()
