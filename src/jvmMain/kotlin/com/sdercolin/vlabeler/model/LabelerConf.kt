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
 * @property name Unique name of the labeler
 * @property version Version code in integer. Configurations with same [name] and [version] should always have same
 *   contents if distributed in public
 * @property extension File extension of the raw label file
 * @property defaultInputFilePath Default name of the input file relative to the sample directory
 * @property displayedName Name displayed in the UI (localized)
 * @property author Author of the labeler
 * @property email Email of the author
 * @property description Description of the labeler (localized)
 * @property website Website url of the labeler
 * @property continuous Whether the labeler use continuous mode, where the end of entry is forced set to the start of
 *   its next entry
 * @property allowSameNameEntry Whether to allow more than one entry with a shared name in the project module
 * @property defaultValues Default value listed as [start, *fields, end] in millisecond
 * @property defaultExtras Default [extraFieldNames] values
 * @property fields Objects in [Field] type containing data used in the label files, except for built-in "start" and
 *   "end". Corresponds to [Entry.points]
 * @property extraFieldNames Extra field names for some data only for calculation. These fields are saved as String
 * @property lockedDrag Defines when to use locked dragging (all parameters will move with dragged one)
 * @property overflowBeforeStart Action taken when there are points before "start"
 * @property overflowAfterEnd Action taken when there are points after "end"
 * @property decimalDigit Decimal digit count used in [properties] and [writer]
 * @property properties Properties that are used in the following procedures. See [Property]
 * @property parser Defines how data from the original label format are parsed
 * @property writer Defines how to write content in the original label format
 * @property parameters Configurable parameters of the labeler. See [ParameterHolder]
 * @property projectConstructor Scripts to construct a project with sub-projects. See [ProjectConstructor]
 */
@Serializable
@Immutable
data class LabelerConf(
    override val name: String,
    override val version: Int = 1,
    val extension: String,
    val defaultInputFilePath: String? = null,
    override val displayedName: LocalizedJsonString = name.toLocalized(),
    override val author: String,
    override val email: String = "",
    override val description: LocalizedJsonString = "".toLocalized(),
    override val website: String = "",
    val continuous: Boolean = false,
    val allowSameNameEntry: Boolean = false,
    val defaultValues: List<Float>,
    val defaultExtras: List<String>,
    val fields: List<Field> = listOf(),
    val extraFieldNames: List<String> = listOf(),
    val lockedDrag: LockedDrag = LockedDrag(),
    val overflowBeforeStart: PointOverflow = PointOverflow.Error,
    val overflowAfterEnd: PointOverflow = PointOverflow.Error,
    val decimalDigit: Int = 2,
    val properties: List<Property> = listOf(),
    val parser: Parser,
    val writer: Writer,
    val parameters: List<ParameterHolder> = listOf(),
    val projectConstructor: ProjectConstructor? = null,
) : BasePlugin {

    val fileName get() = "$name.$LabelerFileExtension"
    val isBuiltIn get() = DefaultLabelerDir.listFiles().orEmpty().any { it.name == fileName }
    val isSelfConstructed get() = projectConstructor != null

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
     * @property name Unique name of the field
     * @property label Label displayed in the UI (localized)
     * @property color Color code in format of "#ffffff"
     * @property height Label height ratio to the height of waveforms part (0~1)
     * @property dragBase True if all the other parameter line move together with this one
     * @property filling Name of the target field to which a filled area is drawn from this field. "start" and "end" are
     *   also available
     * @property constraints Define value constraints between the fields. See [Constraint]
     * @property shortcutIndex Index in the shortcut list. Could be 1~8 (0 is reserved for "start")
     * @property replaceStart Set to true if this field should replace "start" when displayed. In this case, other fields
     *   can be set smaller than the replaced "start", and the original [Entry.start] will be automatically set to the
     *   minimum value of all the fields. Cannot be used when [continuous] is true
     * @property replaceEnd Set to true if this field should replace "end" when displayed. In this case, other fields
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
     * @property min Index of the field that should be smaller or equal to this field
     * @property max Index of the field that should be greater or equal to this field
     */
    @Serializable
    @Immutable
    data class Constraint(
        val min: Int? = null,
        val max: Int? = null,
    )

    /**
     * Definition of when should all parameter lines move together when dragging
     * @property useDragBase True if locked drag is enabled when field with [Field.dragBase] == true is dragged
     * @property useStart True if locked drag is enabled when the start line is dragged
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
     * @property defaultEncoding Default text encoding of the input file
     * @property extractionPattern Regex pattern that extract groups
     * @property variableNames Definition of how the extracted string groups will be put into variables later in the
     *   parser JavaScript code. Should be in the same order as the extracted groups
     * @property scripts JavaScript code in lines that sets properties of [Entry] using the variables extracted
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
         * - String array "sampleNames": Names of the samples without extension in the folder
         * - String "<item in [variableNames]>": Values extracted by [extractionPattern]
         * - Map "params", created according to [parameters], see [ParameterHolder] for details
         *
         * Output variables that the scripts should set:
         * - String "sample" (sample file name without extension)
         * - String "name"
         * - Float "start" (in millisecond)
         * - Float "end" (in millisecond)
         * - Float array "points" (in millisecond)
         * - String array corresponding values defined in [extraFieldNames]
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
     * @property format String format to generate the output line
     * @property scripts JavaScript code in lines that generate the output line
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
     * @property name Unique name of the property
     * @property displayedName Name displayed in property view UI (localized)
     * @property value Mathematical expression text including fields written as "{[Field.name]}" and "{start}", "{end}".
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
     * @property parameter Definition of the parameter. They are the same with the parameters used by a plugin, so you
     *   can also see [readme/plugin-development.md] for details
     * @property injector JavaScript code that injects the parameter value into the labeler.
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
    @Immutable
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
            val injector = element["injector"]?.takeUnless { it is JsonNull }?.let {
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

    /**
     * In order to edit multiple label files in a single project, the labeler should be able to construct sub-projects
     * when creating the project.
     * This property defines the sub-project structure and building procedure.
     * In the source code, we call the sub-project as "Module".
     * The [scripts] is JavaScript code lines that creates [RawModuleDefinition] objects.
     *
     * Available input variables:
     * - File "root": the root directory of the project (the `Sample Directory` choosed in the project creation page)
     *   the `File` type is a wrapper of Java's `java.io.File` class.
     *   Please check the documentation in [readme/file-api.md],
     *   or the source code in [src/main/jvmMain/resources/js/file.js]
     * - Map "params", created according to [parameters], see [ParameterHolder] for details
     * - String array "acceptedSampleExtensions": the array of accepted sample extensions in the current application.
     *   Defined in [com.sdercolin.vlabeler.io.Sample.acceptableSampleFileExtensions]
     *
     * Output variables that the scripts should set:
     * - Array "modules": an array of [RawModuleDefinition] objects.
     *   Use `new ModuleDefinition()` to create a new object.
     *   Please check the JavaScript source code with documentations in
     *   [src/main/jvmMain/resources/js/module_definition.js] for details.
     */
    @Serializable
    @Immutable
    data class ProjectConstructor(
        val scripts: List<String>,
    )

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
