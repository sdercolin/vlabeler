package com.sdercolin.vlabeler.util

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Serializable dynamic primitive type map
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

    fun toJsonObject() = buildJsonObject {
        for ((key, value) in map) {
            put(key, toJsonPrimitive(value))
        }
    }
}

fun Map<String, Any>.toParamMap() = ParamMap(this)

fun ParamMap?.orEmpty() = this ?: ParamMap(mapOf())

private fun toJsonPrimitive(value: Any?): JsonPrimitive {
    if (value == null) return JsonNull
    return when (value) {
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        else -> throw IllegalArgumentException("$value is not supported")
    }
}
