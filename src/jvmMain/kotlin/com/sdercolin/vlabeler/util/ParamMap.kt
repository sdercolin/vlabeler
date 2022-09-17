package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.currentLanguage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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

    fun resolve(project: Project?, js: JavaScript?) = buildJsonObject {
        for ((key, value) in map) {
            put(key, resolveItem(value, project, js))
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
        else -> throw IllegalArgumentException("$value is not supported")
    }
}
