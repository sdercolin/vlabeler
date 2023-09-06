package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.segment.analytics.kotlin.core.utilities.safeJsonPrimitive
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * A data class that represents some embedded scripts in a [LabelerConf]. Either [path] or [lines] should be non-null.
 */
@Serializable(with = EmbeddedScriptsSerializer::class)
@Immutable
data class EmbeddedScripts(
    val path: String?,
    val lines: List<String>?,
    @Transient val writeAsPath: Boolean = false,
) {

    fun getScripts(directory: File?): String {
        if (lines?.isNotEmpty() == true) return lines.joinToString("\n")
        if (path != null && directory != null) {
            val file = File(directory, path)
            return file.readText()
        }
        throw IllegalStateException("EmbeddedScripts is empty")
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(EmbeddedScripts::class)
object EmbeddedScriptsSerializer : KSerializer<EmbeddedScripts> {

    override fun serialize(encoder: Encoder, value: EmbeddedScripts) {
        require(encoder is JsonEncoder)
        if (value.writeAsPath) {
            requireNotNull(value.path)
            encoder.encodeSerializableValue(JsonPrimitive.serializer(), JsonPrimitive(value.path))
        } else {
            require(value.lines.isNullOrEmpty().not())
            val array = JsonArray(value.lines.orEmpty().map { JsonPrimitive(it) })
            encoder.encodeSerializableValue(JsonArray.serializer(), array)
        }
    }

    override fun deserialize(decoder: Decoder): EmbeddedScripts {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        element.safeJsonPrimitive?.contentOrNull?.let { path ->
            return EmbeddedScripts(path, emptyList())
        }
        val lines = element.jsonArray.map { it.jsonPrimitive.content }
        return EmbeddedScripts(null, lines)
    }
}
