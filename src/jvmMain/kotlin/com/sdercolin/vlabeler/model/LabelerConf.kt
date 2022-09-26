@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.LabelerConf.Property
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.DefaultLabelerDir
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Configuration of the labeler's appearances and behaviors
 */
@Serializable
@Immutable
data class LabelerConf(
    /**
     * Unique name of the labeler
     */
    override val name: String,
    /**
     * Version code in integer
     * Configurations with same [name] and [version] should always have same contents if distributed in public
     */
    override val version: Int = 1,
    /**
     * File extension of the raw label file
     */
    val extension: String,
    /**
     * Default name of the input file relative to the sample directory
     */
    val defaultInputFilePath: String? = null,
    /**
     * Name displayed in the UI (localized)
     */
    override val displayedName: LocalizedJsonString = name.toLocalized(),
    override val author: String,
    override val email: String = "",
    override val description: LocalizedJsonString = "".toLocalized(),
    override val website: String = "",
    /**
     * Continuous mode, where the end of entry is forced set to the start of its next entry
     */
    val continuous: Boolean = false,
    /**
     * Whether to allow more than one entry with a shared name in one sample
     */
    val allowSameNameEntry: Boolean = false,
    /**
     * Default value listed as [start, *fields, end] in millisecond
     */
    val defaultValues: List<Float>,
    /**
     * Default [extraFieldNames] values
     */
    val defaultExtras: List<String>,
    /**
     * Fields defined except for built-in "start" and "end".
     * Corresponds to [Entry.points]
     */
    val fields: List<Field> = listOf(),
    /**
     * Extra field names for some data only for calculation.
     * Saved as String
     */
    val extraFieldNames: List<String> = listOf(),
    /**
     * Defines when to use locked dragging (all parameters will move with dragged one)
     */
    val lockedDrag: LockedDrag = LockedDrag(),
    /**
     * Action taken when there are points before "start"
     */
    val overflowBeforeStart: PointOverflow = PointOverflow.Error,
    /**
     * Action taken when there are points after "end"
     */
    val overflowAfterEnd: PointOverflow = PointOverflow.Error,
    /**
     * Decimal digit count of the properties and writer
     */
    val decimalDigit: Int = 2,
    /**
     * Properties that are used in the following procedures. See [Property]
     */
    val properties: List<Property> = listOf(),
    /**
     * Defines how data from the original format are parsed
     */
    val parser: Parser,
    /**
     * Defines how to write content in the original format
     */
    val writer: Writer,
    /**
     * Configurable parameters of the labeler
     */
    val parameters: List<ParameterHolder> = listOf(),
) : BasePlugin {

    val fileName get() = "$name.$LabelerFileExtension"
    val isBuiltIn get() = DefaultLabelerDir.listFiles().orEmpty().any { it.name == fileName }

    override val parameterDefs: List<Parameter<*>>
        get() = parameters.map { it.parameter }

    /**
     * Get constraints for canvas usage
     * Pair<a, b> represents "a <= b"
     */
    val connectedConstraints: List<Pair<Int, Int>>
        get() = fields.withIndex()
            .flatMap { item ->
                item.value.constraints.flatMap { constraint ->
                    val min = constraint.min?.let { it to item.index }
                    val max = constraint.max?.let { item.index to it }
                    listOfNotNull(min, max)
                }
            }
            .distinct()

    /**
     * Custom field of the labeler
     * @param name Unique name of the field
     * @param label Label displayed in the UI (localized)
     * @param color Color code in format of "#ffffff"
     * @param height Label height ratio to the height of waveforms part (0~1)
     * @param dragBase True if all the other parameter line move together with this one
     * @param filling Name of the target field to which a filled area is drawn from this field. "start" and "end" are
     *   also available
     * @param constraints Define value constraints between the fields. See [Constraint]
     * @param shortcutIndex Index in the shortcut list. Could be 1~8 (0 is reserved for "start")
     * @param replaceStart Set to true if this field should replace "start" when displayed. In this case, other fields
     *   can be set smaller than the replaced "start", and the original [Entry.start] will be automatically set to the
     *   minimum value of all the fields. Cannot be used when [continuous] is true
     * @param replaceEnd Set to true if this field should replace "end" when displayed. In this case, other fields
     *   can be set larger than the replaced "end", and the original [Entry.end] will be automatically set to the
     *   maximum value of all the fields. Cannot be used when [continuous] is true
     */
    @Serializable
    @Immutable
    data class Field(
        val name: String,
        val label: LocalizedJsonString,
        val color: String,
        val height: Float,
        val dragBase: Boolean = false,
        val filling: String? = null,
        val constraints: List<Constraint> = listOf(),
        val shortcutIndex: Int? = null,
        val replaceStart: Boolean = false,
        val replaceEnd: Boolean = false,
    )

    /**
     * Except for "start" and "end" (all fields should be between "start" and "end").
     * You don't have to declare the same constraint in both two fields
     * @param min Index of the field that should be smaller or equal to this field
     * @param max Index of the field that should be greater or equal to this field
     */
    @Serializable
    @Immutable
    data class Constraint(
        val min: Int? = null,
        val max: Int? = null,
    )

    /**
     * Definition of when should all parameter lines move together when dragging
     * @param useDragBase True if locked drag is enabled when field with [Field.dragBase] == true is dragged
     * @param useStart True if locked drag is enabled when the start line is dragged
     */
    @Serializable
    @Immutable
    data class LockedDrag(
        val useDragBase: Boolean = false,
        val useStart: Boolean = false,
    )

    /**
     * Action to be taken when points are set outside borders (start/end)
     */
    @Serializable
    @Immutable
    enum class PointOverflow {
        /**
         * Adjust the border to contain the point
         */
        AdjustBorder,

        /**
         * Adjust the overflowing point to the border
         */
        AdjustPoint,

        /**
         * Do nothing, which leads to an error
         */
        Error
    }

    /**
     * Definition for parsing the raw label file to local [Entry]
     * @param defaultEncoding Default text encoding of the input file
     * @param extractionPattern Regex pattern that extract groups
     * @param variableNames Definition of how the extracted string groups will be put into variables later in the parser
     *   JavaScript code. Should be in the same order as the extracted groups
     * @param scripts JavaScript code in lines that sets properties of [Entry] using the variables extracted
     */
    @Serializable
    @Immutable
    data class Parser(
        val defaultEncoding: String,
        val extractionPattern: String,
        val variableNames: List<String>,
        /**
         * Available input variables:
         * - String "inputFileName": Name of the input file without extension
         * - String List "sampleNames": Names of the samples without extension in the folder
         * - String "<item in [variableNames]>": Values extracted by [extractionPattern]
         * - Map "params", created according to [parameters], see [ParameterHolder] for details
         *
         * Output variables that the scripts should set:
         * - String "sample" (sample file name without extension)
         * - String "name"
         * - Float "start" (in millisecond)
         * - Float "end" (in millisecond)
         * - Float List "points" (in millisecond)
         * - String List corresponding values defined in [extraFieldNames]
         *
         * If "sample" is not set, the first sample file is used by all entries in case all entries are bound to the
         * only one sample file, so the file name doesn't exist in the line.
         *
         * If "name" is not set, this entry is ignored.
         * If any of "start", "end" is not set, or points don't have a same size with [fields], this entry will fall
         * back to default values except the entry name.
         */
        val scripts: List<String>,
    )

    /**
     * Definition for line format in the raw label file
     * @param format String format to generate the output line
     * @param scripts JavaScript code in lines that generate the output line
     * Either [format] or [scripts] should be given. If both of them are given, [scripts] is used
     */
    @Serializable
    @Immutable
    data class Writer(
        /**
         * String format using the following variables written as "{<var_name>}":
         * {sample} - sample file name without extension
         * {name} - entry name
         * {start} - [Entry.start]
         * {end} - [Entry.end]
         * {[Property.name]} - Evaluated value of a [Property]
         * {[Field.name]} - value in [Entry.points] with the same index of the corresponding [Field]
         * {<item in [extraFieldNames]>} - value saved in [Parser]
         *
         * If a name is shared by a [Property] and [Field], it's considered as [Property].
         *
         * @sample "{sample}.wav:{name}={start},{middle},{end}" will be written like "a.wav:a:100,220.5,300"
         */
        val format: String? = null,
        /**
         * JavaScript code lines that sets "output" variable using the same variables as described in [format]
         * String type: sample, name, extras
         * Float type: start, end, and others
         * Besides, "params" is available, which is a map created according to [parameters].
         * See [ParameterHolder] for details.
         */
        val scripts: List<String>? = null,
    )

    /**
     * Definition of properties that will be written to the raw label file
     * @param name Unique name of the property
     * @param displayedName Name displayed in property view UI (localized)
     * @param value Mathematical expression text including fields written as "{[Field.name]}" and "{start}", "{end}".
     *   Extra fields of number type defined in [extraFieldNames] are also available. The expression is evaluated in
     *   JavaScript.
     */
    @Serializable
    @Immutable
    data class Property(
        val name: String,
        val displayedName: LocalizedJsonString,
        val value: String,
    )

    /**
     * Definition of a configurable parameter used in this labeler.
     * The parameters are accessible
     * 1. in the JavaScript code of [Parser.scripts] and [Writer.scripts]
     *  - keys are defined in as `parameters[].parameter.name`
     *  - value could be undefined, which means the parameter is not set
     * 2. via injected (updated) [LabelerConf] by [injector], if it is not null
     * @param parameter Definition of the parameter
     * @param injector JavaScript code that injects the parameter value into the labeler.
     *   `labeler` and `value` are available as variables.
     *   Note the injector cannot change the following info of the labeler:
     *   - [name]
     *   - [version]
     *   - [extension]
     *   - [displayedName]
     *   - [description]
     *   - [author]
     *   - [website]
     *   - [email]
     *   - [continuous]
     *   - [parameters]
     *   - size of [fields]
     *   - size of [extraFieldNames]
     *   - size of [defaultExtras]
     *   - size of [defaultValues]
     *   - [Field.name]s in [fields]
     */
    @Serializable(with = ParameterHolderSerializer::class)
    data class ParameterHolder(
        val parameter: Parameter<*>,
        val injector: List<String>? = null,
    )

    @Serializer(ParameterHolder::class)
    object ParameterHolderSerializer : KSerializer<ParameterHolder> {
        override fun deserialize(decoder: Decoder): ParameterHolder {
            require(decoder is JsonDecoder)
            val element = decoder.decodeJsonElement()
            require(element is JsonObject)
            val parameter = requireNotNull(element["parameter"]).let {
                decoder.json.decodeFromJsonElement(PolymorphicSerializer(Parameter::class), it)
            }
            val injector = element["injector"]?.let {
                decoder.json.decodeFromJsonElement(ListSerializer(String.serializer()), it)
            }
            return ParameterHolder(parameter, injector)
        }

        override fun serialize(encoder: Encoder, value: ParameterHolder) {
            require(encoder is JsonEncoder)
            val element = JsonObject(
                mapOf(
                    "parameter" to encoder.json.encodeToJsonElement(
                        PolymorphicSerializer(Parameter::class),
                        value.parameter,
                    ),
                    "injector" to (
                        value.injector?.let {
                            encoder.json.encodeToJsonElement(ListSerializer(String.serializer()), it)
                        } ?: JsonNull
                        ),
                ),
            )
            encoder.encodeJsonElement(element)
        }
    }

    val useImplicitStart: Boolean
        get() = fields.any { it.replaceStart }

    val useImplicitEnd: Boolean
        get() = fields.any { it.replaceEnd }

    fun validate() = this.also {
        if (continuous) {
            require(useImplicitStart.not() && useImplicitEnd.not()) {
                "Cannot use implicit start/end when continuous is true"
            }
        }
        if (parameters.isNotEmpty()) {
            parameters.map { it.parameter }.filterIsInstance<Parameter.StringParam>().forEach {
                require(Parameter.StringParam.DefaultValueFileReferencePattern.matches(it.defaultValue).not()) {
                    "Default value of string parameter in a labeler cannot be a file reference"
                }
            }
        }
    }

    companion object {
        const val LabelerFileExtension = "labeler.json"
    }
}
