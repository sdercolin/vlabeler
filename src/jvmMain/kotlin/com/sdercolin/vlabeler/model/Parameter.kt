package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.toFileOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parameters used by a [LabelerConf] or a [Plugin]
 */
@Serializable
@Immutable
sealed class Parameter<T : Any> {
    abstract val name: String
    abstract val label: LocalizedJsonString
    abstract val description: LocalizedJsonString?
    abstract val enableIf: String?
    abstract val defaultValue: T
    abstract fun eval(value: Any): Boolean

    @Serializable
    @SerialName("integer")
    class IntParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: Int,
        val min: Int? = null,
        val max: Int? = null,
    ) : Parameter<Int>() {

        override fun eval(value: Any) = value is Int && value != 0
    }

    @Serializable
    @SerialName("float")
    class FloatParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: Float,
        val min: Float? = null,
        val max: Float? = null,
    ) : Parameter<Float>() {

        override fun eval(value: Any) = value is Float && value != 0f && value.isNaN().not()
    }

    @Serializable
    @SerialName("boolean")
    class BooleanParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: Boolean,
    ) : Parameter<Boolean>() {

        override fun eval(value: Any) = value is Boolean && value
    }

    @Serializable
    @SerialName("string")
    class StringParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: String,
        val multiLine: Boolean = false,
        val optional: Boolean = false,
    ) : Parameter<String>() {

        override fun eval(value: Any) = value is String && value.isNotEmpty()

        companion object {
            val DefaultValueFileReferencePattern = Regex("^file::(.*)$")
        }
    }

    @Serializable
    @SerialName("enum")
    class EnumParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: LocalizedJsonString,
        val options: List<LocalizedJsonString>,
    ) : Parameter<LocalizedJsonString>() {

        override fun eval(value: Any) = value is LocalizedJsonString
    }

    @Serializable
    @SerialName("entrySelector")
    class EntrySelectorParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: EntrySelector,
    ) : Parameter<EntrySelector>() {

        override fun eval(value: Any) = value is EntrySelector && value.filters.isNotEmpty()
    }

    @Serializable
    @SerialName("file")
    class FileParam(
        override val name: String,
        override val label: LocalizedJsonString,
        override val description: LocalizedJsonString? = null,
        override val enableIf: String? = null,
        override val defaultValue: FileWithEncoding,
        val optional: Boolean = false,
        val acceptExtensions: List<String>? = null,
    ) : Parameter<FileWithEncoding>() {

        override fun eval(value: Any) = value is FileWithEncoding && value.file != null
    }

    fun check(value: Any, labelerConf: LabelerConf?): Boolean {
        return when (this) {
            is BooleanParam -> (value as? Boolean) != null
            is EnumParam -> (value as? LocalizedJsonString)?.let { enumValue ->
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
                selector.filters.all { it.isValid(requireNotNull(labelerConf)) }
            } == true
            is FileParam -> (value as? FileWithEncoding)?.let {
                if (optional && it.file == null) return true
                val file = it.file?.toFileOrNull(ensureIsFile = true) ?: return@let false
                if (acceptExtensions != null && file.extension !in acceptExtensions) return@let false
                true
            } == true
        }
    }
}
