package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.toFileOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Parameters used by a [LabelerConf] or a [Plugin].
 */
@Serializable
@Immutable
sealed class Parameter<T : Any> {
    abstract val type: String
    abstract val name: String
    abstract val label: LocalizedJsonString
    abstract val description: LocalizedJsonString?
    abstract val enableIf: String?
    abstract val defaultValue: T
    abstract fun eval(value: Any): Boolean

    @Serializable
    @SerialName(IntParam.Type)
    data class IntParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: Int,
        val min: Int? = null,
        val max: Int? = null,
    ) : Parameter<Int>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is Int && value != 0

        companion object {
            const val Type = "integer"
        }
    }

    @Serializable
    @SerialName(FloatParam.Type)
    data class FloatParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: Float,
        val min: Float? = null,
        val max: Float? = null,
    ) : Parameter<Float>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is Float && value != 0f && value.isNaN().not()

        companion object {
            const val Type = "float"
        }
    }

    @Serializable
    @SerialName(BooleanParam.Type)
    data class BooleanParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: Boolean,
    ) : Parameter<Boolean>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is Boolean && value

        companion object {
            const val Type = "boolean"
        }
    }

    @Serializable
    @SerialName(StringParam.Type)
    data class StringParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        /**
         * The default value could be a file reference, e.g. `file::default.txt`. See the overriding logic in
         * [src/jvmMain/kotlin/com/sdercolin/vlabeler/io/Plugin.kt].
         */
        override val defaultValue: String,
        val multiLine: Boolean = false,
        val optional: Boolean = false,
    ) : Parameter<String>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is String && value.isNotEmpty()

        companion object {
            const val Type = "string"

            val DefaultValueFileReferencePattern = Regex("^file::(.*)$")
        }
    }

    @Serializable
    @SerialName(EnumParam.Type)
    class EnumParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: String,
        val options: List<String>,
        val optionDisplayedNames: List<LocalizedJsonString>? = null,
    ) : Parameter<String>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is String

        companion object {
            const val Type = "enum"
        }
    }

    @Serializable
    @SerialName(EntrySelectorParam.Type)
    class EntrySelectorParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: EntrySelector,
    ) : Parameter<EntrySelector>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is EntrySelector && value.filters.isNotEmpty()

        companion object {
            const val Type = "entrySelector"
        }
    }

    @Serializable
    @SerialName(FileParam.Type)
    data class FileParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        /**
         * The `file` field of the default value may be resolved to be relative to the plugin directory. See the
         * overriding logic in [src/jvmMain/kotlin/com/sdercolin/vlabeler/io/Plugin.kt].
         */
        override val defaultValue: FileWithEncoding,
        val optional: Boolean = false,
        val acceptExtensions: List<String>? = null,
    ) : Parameter<FileWithEncoding>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is FileWithEncoding && value.file != null

        companion object {
            const val Type = "file"
        }
    }

    @Serializable
    @SerialName(RawFileParam.Type)
    data class RawFileParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        /**
         * The default value may be resolved to be relative to the plugin directory. See the overriding logic in
         * [src/jvmMain/kotlin/com/sdercolin/vlabeler/io/Plugin.kt].
         */
        override val defaultValue: String,
        val optional: Boolean = false,
        val acceptExtensions: List<String>? = null,
        val isFolder: Boolean = false,
    ) : Parameter<String>() {

        @Transient
        override val type: String = Type

        override fun eval(value: Any) = value is String && value.isNotEmpty()

        companion object {
            const val Type = "rawFile"
        }
    }

    fun check(value: Any, labelerConf: LabelerConf?): Boolean {
        return when (this) {
            is BooleanParam -> (value as? Boolean) != null
            is EnumParam -> (value as? String)?.let { enumValue ->
                enumValue in options
            } == true
            is FloatParam -> (value as? Float)?.let { floatValue ->
                floatValue >= (min ?: Float.NEGATIVE_INFINITY) &&
                    floatValue <= (max ?: Float.POSITIVE_INFINITY)
            } == true
            is IntParam -> (value as? Int)?.let { intValue ->
                intValue >= (min ?: Int.MIN_VALUE) && intValue <= (max ?: Int.MAX_VALUE)
            } == true
            is StringParam -> (value as? String)?.let { stringValue ->
                when {
                    optional.not() && stringValue.isEmpty() -> false
                    multiLine.not() && stringValue.lines().size > 1 -> false
                    else -> true
                }
            } == true
            is EntrySelectorParam -> (value as? EntrySelector)?.let { selector ->
                if (labelerConf != null) selector.isValid(labelerConf) else false
            } == true
            is FileParam -> (value as? FileWithEncoding)?.let {
                if (optional && it.file.isNullOrEmpty()) return true
                val file = it.file?.toFileOrNull(ensureIsFile = true) ?: return@let false
                if (acceptExtensions != null && file.extension !in acceptExtensions) return@let false
                true
            } == true
            is RawFileParam -> (value as? String)?.let { stringValue ->
                if (optional && stringValue.isEmpty()) return true
                val file = stringValue.toFileOrNull(ensureExists = true) ?: return@let false
                val isFolder = file.isDirectory
                if (isFolder != this.isFolder) return@let false
                if (!this.isFolder && acceptExtensions != null && file.extension !in acceptExtensions) return@let false
                true
            } == true
        }
    }
}
