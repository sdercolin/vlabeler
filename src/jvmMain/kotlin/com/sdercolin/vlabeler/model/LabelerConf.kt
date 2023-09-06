@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.LabelerConf.Companion.SerialVersion
import com.sdercolin.vlabeler.model.LabelerConf.ExtraField
import com.sdercolin.vlabeler.model.LabelerConf.Field
import com.sdercolin.vlabeler.model.LabelerConf.ParameterHolder
import com.sdercolin.vlabeler.model.LabelerConf.ProjectConstructor
import com.sdercolin.vlabeler.model.LabelerConf.Property
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.DefaultLabelerDir
import com.sdercolin.vlabeler.util.RecordDir
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Configuration of the labeler's appearances and behaviors.
 *
 * @property name Unique name of the labeler.
 * @property version Version code in integer. Configurations with same [name] and [version] should always have same
 *     contents if distributed in public.
 * @property extension File extension of the raw label file.
 * @property serialVersion Serial version of the labeler used for migration. See the current version in [SerialVersion].
 * @property defaultInputFilePath Default name of the input file relative to the sample directory.
 * @property displayedName Name displayed in the UI (localized).
 * @property author Author of the labeler.
 * @property email Email of the author.
 * @property description Description of the labeler (localized).
 * @property website Website url of the labeler.
 * @property categoryTag Category tag of the labeler, "" as `Other`.
 * @property displayOrder Display order of the labeler in the dropdown list.
 * @property continuous Whether the labeler use continuous mode, where the end of entry is forced set to the start of
 *     its next entry.
 * @property allowSameNameEntry Whether to allow more than one entry with a shared name in the project module.
 * @property defaultValues Default value listed as [start, *fields, end] in millisecond.
 * @property defaultExtras (Deprecated) Use [extraFields] instead.
 * @property fields [Field] definitions containing data used in the label files, except for built-in "start" and "end".
 *     Corresponds to [Entry.points].
 * @property extraFieldNames (Deprecated) Use [extraFields] instead.
 * @property extraFields [ExtraField] definitions stored as strings. Corresponds to [Entry.extras].
 * @property lockedDrag Defines when to use locked dragging (all parameters will move with dragged one).
 * @property overflowBeforeStart Action taken when there are points before "start".
 * @property overflowAfterEnd Action taken when there are points after "end".
 * @property postEditNextTrigger Trigger settings of `Go to next entry after editing` on start and end.
 * @property postEditDoneTrigger Trigger settings of `Mark as done after editing` on start and end.
 * @property decimalDigit Decimal digit count used in [properties] and [writer].
 * @property properties Properties that are used in the following procedures. See [Property].
 * @property parser Defines how data from the original label format are parsed.
 * @property writer Defines how to write content in the original label format.
 * @property parameters Configurable parameters of the labeler. See [ParameterHolder].
 * @property projectConstructor Scripts to construct a project with subprojects. See [ProjectConstructor].
 */
@Serializable
@Immutable
data class LabelerConf(
    override val name: String,
    override val version: Int = 1,
    val serialVersion: Int = 0,
    val extension: String,
    val defaultInputFilePath: String? = null,
    override val displayedName: LocalizedJsonString = name.toLocalized(),
    override val author: String,
    override val email: String = "",
    override val description: LocalizedJsonString = "".toLocalized(),
    override val website: String = "",
    val categoryTag: String = "",
    val displayOrder: Int = 0,
    val continuous: Boolean = false,
    val allowSameNameEntry: Boolean = false,
    val defaultValues: List<Float>,
    val defaultExtras: List<String>? = null,
    val fields: List<Field> = listOf(),
    val extraFieldNames: List<String>? = null,
    val extraFields: List<ExtraField> = listOf(),
    val lockedDrag: LockedDrag = LockedDrag(),
    val overflowBeforeStart: PointOverflow = PointOverflow.Error,
    val overflowAfterEnd: PointOverflow = PointOverflow.Error,
    val postEditNextTrigger: PostEditTrigger = PostEditTrigger(),
    val postEditDoneTrigger: PostEditTrigger = PostEditTrigger(),
    val decimalDigit: Int? = 2,
    val properties: List<Property> = listOf(),
    val parser: Parser,
    val writer: Writer,
    val parameters: List<ParameterHolder> = listOf(),
    val projectConstructor: ProjectConstructor? = null,
    @Transient val directory: File? = null,
) : BasePlugin {

    private val fileName get() = "$name.$LabelerFileExtension"
    val isBuiltIn get() = DefaultLabelerDir.listFiles().orEmpty().any { it.name == fileName }
    val file get() = if (isBuiltIn) DefaultLabelerDir.resolve(fileName) else CustomLabelerDir.resolve(fileName)
    val isSelfConstructed get() = projectConstructor != null

    override val parameterDefs: List<Parameter<*>>
        get() = parameters.map { it.parameter }

    /**
     * Get constraints for canvas usage Pair<a, b> represents "a <= b".
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
     * Get names of all fields that could trigger `Mark as done` after editing.
     */
    val postEditDoneTriggerFieldNames
        get() = listOfNotNull(
            "start".takeIf { postEditDoneTrigger.useStart },
            "end".takeIf { postEditDoneTrigger.useEnd },
        ) + fields.filter { it.triggerPostEditDone }.map { it.name }

    /**
     * Get names of all fields that could trigger `Go to next entry` after editing.
     */
    val postEditNextTriggerFieldNames
        get() = listOfNotNull(
            "start".takeIf { postEditNextTrigger.useStart },
            "end".takeIf { postEditNextTrigger.useEnd },
        ) + fields.filter { it.triggerPostEditNext }.map { it.name }

    /**
     * Custom field of the labeler.
     *
     * @property name Unique name of the field.
     * @property label Label displayed in the UI (localized).
     * @property color Color code in format of "#ffffff".
     * @property height Label height ratio to the height of waveforms part (0~1).
     * @property dragBase True if all the other parameter line move together with this one.
     * @property filling Name of the target field to which a filled area is drawn from this field. "start" and "end" are
     *     also available.
     * @property constraints Define value constraints between the fields. See [Constraint].
     * @property shortcutIndex Index in the shortcut list. Could be 1~8 (0 is reserved for "start").
     * @property replaceStart Set to true if this field should replace "start" when displayed. In this case, other
     *     fields can be set smaller than the replaced "start", and the original [Entry.start] will be automatically set
     *     to the minimum value of all the fields. Cannot be used when [continuous] is true.
     * @property replaceEnd Set to true if this field should replace "end" when displayed. In this case, other fields
     *     can be set larger than the replaced "end", and the original [Entry.end] will be automatically set to the
     *     maximum value of all the fields. Cannot be used when [continuous] is true.
     * @property triggerPostEditNext True if `Go to next entry` should be triggered when this field is edited.
     * @property triggerPostEditDone True if `Mark as done` should be triggered when this field is edited.
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
        val triggerPostEditNext: Boolean = false,
        val triggerPostEditDone: Boolean = false,
    )

    /**
     * Except for "start" and "end" (all fields should be between "start" and "end"). You don't have to declare the same
     * constraint in both two fields.
     *
     * @property min Index of the field that should be smaller or equal to this field.
     * @property max Index of the field that should be greater or equal to this field.
     */
    @Serializable
    @Immutable
    data class Constraint(
        val min: Int? = null,
        val max: Int? = null,
    )

    /**
     * Definition of when should all parameter lines move together when dragging.
     *
     * @property useDragBase True if locked drag is enabled when field with [Field.dragBase] == true is dragged.
     * @property useStart True if locked drag is enabled when the start line is dragged.
     */
    @Serializable
    @Immutable
    data class LockedDrag(
        val useDragBase: Boolean = false,
        val useStart: Boolean = false,
    )

    /**
     * Trigger settings of post edit actions on start and end. For triggering with fields, see
     * [Field.triggerPostEditNext] and [Field.triggerPostEditDone].
     */
    @Serializable
    @Immutable
    data class PostEditTrigger(
        val useStart: Boolean = false,
        val useEnd: Boolean = false,
    )

    /**
     * Action to be taken when points are set outside borders (start/end).
     */
    @Serializable
    @Immutable
    enum class PointOverflow {
        /**
         * Adjust the border to contain the point.
         */
        AdjustBorder,

        /**
         * Adjust the overflowing point to the border.
         */
        AdjustPoint,

        /**
         * Do nothing, which leads to an error.
         */
        Error
    }

    /**
     * Process scope of [Parser] and [Writer].
     */
    enum class Scope {
        /**
         * Read/Write a line for every entry.
         */
        Entry,

        /**
         * Read/Write a file for all modules that are corresponding to the same file.
         */
        Modules,
    }

    /**
     * Definition of extra fields used in entry, module and project scopes. The values are always stored as strings.
     *
     * @property name Unique name of the field.
     * @property default Default value of the field.
     * @property displayedName Displayed name of the field in the UI (localized).
     * @property isVisible Whether the field is visible in the UI.
     * @property isEditable Whether the field is editable in the UI.
     * @property isOptional Whether the field can be null.
     */
    @Serializable
    @Immutable
    data class ExtraField(
        val name: String,
        val default: String,
        val displayedName: LocalizedJsonString,
        val isVisible: Boolean = false,
        val isEditable: Boolean = false,
        val isOptional: Boolean = false,
    )

    /**
     * Definition for parsing the raw label file to local [Entry].
     *
     * @property scope [Scope] of the parser.
     * @property defaultEncoding Default text encoding of the input file.
     * @property extractionPattern Regex pattern that extract groups. Only used when [scope] is [Scope.Entry].
     * @property variableNames Definition of how the extracted string groups will be put into variables later in the
     *     parser JavaScript code. Should be in the same order as the extracted groups. Only used when [scope] is
     *     [Scope.Entry].
     * @property scripts JavaScript code in lines that sets properties of [Entry] using the variables extracted.
     */
    @Serializable
    @Immutable
    data class Parser(
        val scope: Scope,
        val defaultEncoding: String = "UTF-8",
        val extractionPattern: String = "",
        val variableNames: List<String> = emptyList(),
        /**
         * Available input variables:
         * - String "inputFileName": Name of the input file. Could be null.
         * - String array "sampleFileNames": Name of the samples files in the folder.
         * - String "<item in [variableNames]>": Values extracted by [extractionPattern]. Only available when [scope] is
         *   [Scope.Entry].
         * - String "moduleNames": Name of the modules that the scripts need to build. Only available when [scope] is
         *   [Scope.Modules].
         * - String[] array "inputs": Input file contents in lines that belong to this module set. Only available when
         *   [scope] is [Scope.Modules].
         * - Map "params", created according to [parameters], see [ParameterHolder] for details.
         * - String "encoding": the encoding selected in the project creation page.
         *
         * Output variables that the scripts should set:
         * - entry: the JavaScript object for [Entry]. This is only required when [scope] is [Scope.Entry]. See
         *   src/main/resources/labeler/entry.js for the actual JavaScript class definition.
         * - modules: an array of entry (described above) arrays. This is only required when [scope] is [Scope.Modules].
         *   The array should have the same order as 'moduleNames' given as input. Every item in the array should be an
         *   array of [Entry] objects in this module.
         */
        val scripts: EmbeddedScripts,
    )

    /**
     * Definition for line format in the raw label file.
     *
     * @property scope [Scope] of the writer.
     * @property format String format to generate the output line.
     * @property scripts JavaScript code in lines that generate the output line Either [format] or [scripts] should be
     *     given. If both of them are given, [scripts] is used.
     */
    @Serializable
    @Immutable
    data class Writer(
        val scope: Scope = Scope.Entry,
        /**
         * String format using the following variables written as "{<var_name>}", only used by [Scope.Entry]:
         * - {sample}: sample file name without extension.
         * - {name}: entry name.
         * - {start}: [Entry.start]
         * - {end}: [Entry.end]
         * - {[Property.name]}: evaluated value of a [Property].
         * - {[Field.name]}: value in [Entry.points] with the same index of the corresponding [Field].
         * - {<[ExtraField.name] in [extraFields]>}: string value in [Entry.extras], with the same index of the
         *   corresponding [ExtraField].
         *
         * If a name is shared by a [Property] and [Field], the [Property] is assigned to the variable.
         *
         * @sample "{sample}.wav:{name}={start},{middle},{end}" will be written like "a.wav:a:100,220.5,300".
         */
        val format: String? = null,
        /**
         * JavaScript code lines that sets "output" variable.
         *
         * Available input variables in scope [Scope.Entry]:
         * - String "sample": [Entry.sample]
         * - String "name": [Entry.name]
         * - Number "start": [Entry.start]
         * - Number "end": [Entry.end]
         * - Number "[Field.name]": value in [Entry.points] with the same index of the corresponding [Field].
         * - String "<[ExtraField.name] in [extraFields]>": string value in [Entry.extras], with the same index of the
         *   corresponding [ExtraField].
         * - Number "[Property.name]": Evaluated value of a [Property].
         * - Boolean "needSync": [Entry.needSync]
         *
         * If a name is shared by a [Property] and [Field], the [Property] is assigned to the variable.
         *
         * Available input variables in scope [Scope.Modules]:
         * - String array "moduleNames": Name of the modules that the scripts need to handle.
         * - Object[] array "modules": An array of [Entry] arrays. The array has the same order as "moduleNames".
         *   // TODO: add more variables to [Entry], including [Field.name], [ExtraField.name] and [Property.name]
         */
        val scripts: EmbeddedScripts? = null,
    )

    /**
     * Definition of properties that will be written to the raw label file.
     *
     * @property name Unique name of the property.
     * @property displayedName Name displayed in property view UI (localized).
     * @property valueGetter JavaScript code lines that calculates the value from {entry} object and set {value}
     *     variable.
     *    - Input: "entry" - the JavaScript object for [Entry]. See src/main/resources/labeler/entry.js for the actual
     *      JavaScript class definition.
     *    - Output: "value" - the value of the property as number.
     *
     * @property valueSetter JavaScript code lines that takes the value of input the property and update {entry} object
     *     accordingly. Could be null if you want to disable the value setting feature in the UI.
     *    - Input: "value" - the value of the property as number. "entry" - the JavaScript object for [Entry]. See
     *      src/main/resources/labeler/entry.js for the actual JavaScript class definition.
     *    - Output:"entry" - the updated JavaScript object for [Entry].
     *
     * @property shortcutIndex Index in the shortcut list of Action `Set Property`. Could be 0~9.
     */
    @Serializable
    @Immutable
    data class Property(
        val name: String,
        val displayedName: LocalizedJsonString,
        val valueGetter: EmbeddedScripts,
        val valueSetter: EmbeddedScripts? = null,
        val shortcutIndex: Int? = null,
    )

    /**
     * Definition of a configurable parameter used in this labeler. The parameters are accessible:
     * 1. in the JavaScript code of [Parser.scripts] and [Writer.scripts]
     *    - keys are defined in as `parameters[].parameter.name`
     *    - value could be undefined, which means the parameter is not set
     * 2. via injected (updated) [LabelerConf] by [injector], if it is not null
     *
     * @property parameter Definition of the parameter. They are the same with the parameters used by a plugin, so you
     *     can also see [readme/plugin-development.md] for details.
     * @property injector JavaScript code that injects the parameter value into the labeler. `labeler` and `value` are
     *     available as variables. Note the injector cannot change the following info of the labeler:
     *       - [name]
     *       - [version]
     *       - [extension]
     *       - [displayedName]
     *       - [description]
     *       - [author]
     *       - [website]
     *       - [email]
     *       - [continuous]
     *       - [parameters]
     *       - size of [fields]
     *       - size of [defaultValues]
     *       - size of [extraFields]
     *       - [Field.name]s in [fields]
     *
     * @property changeable Whether the parameter is changeable after the project is created.
     */
    @Serializable(with = ParameterHolderSerializer::class)
    @Immutable
    data class ParameterHolder(
        val parameter: Parameter<*>,
        val injector: EmbeddedScripts? = null,
        val changeable: Boolean = false,
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
                decoder.json.decodeFromJsonElement(EmbeddedScriptsSerializer, it)
            }
            val changeable = element["changeable"]?.jsonPrimitive?.boolean ?: false
            return ParameterHolder(parameter, injector, changeable)
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
                            encoder.json.encodeToJsonElement(EmbeddedScriptsSerializer, it)
                        } ?: JsonNull
                        ),
                    "changeable" to JsonPrimitive(value.changeable),
                ),
            )
            encoder.encodeJsonElement(element)
        }
    }

    /**
     * In order to edit multiple label files in a single project, the labeler should be able to construct subprojects
     * when creating the project. This property defines the subproject structure and building procedure. In the
     * source code, we call the subproject as "Module". The [scripts] is JavaScript code lines that creates
     * [RawModuleDefinition] objects.
     *
     * Available input variables:
     * - File "root": the root directory of the project (the `Sample Directory` chosen in the project creation page) the
     *   `File` type is a wrapper of Java's `java.io.File` class. Please check the documentation in
     *   [readme/file-api.md], or the source code in [src/main/jvmMain/resources/js/file.js].
     * - Map "params", created according to [parameters], see [ParameterHolder] for details.
     * - String "encoding": the encoding selected in the project creation page.
     * - String array "acceptedSampleExtensions": the array of accepted sample extensions in the current application.
     *   Defined in [com.sdercolin.vlabeler.io.Sample.acceptableSampleFileExtensions].
     *
     * Output variables that the scripts should set:
     * - Array "modules": an array of [RawModuleDefinition] objects. Use `new ModuleDefinition()` to create a new
     *   object. Please check the JavaScript source code with documentations in
     *   [src/main/jvmMain/resources/js/module_definition.js] for details. Specifically, if there are multiple modules
     *   that only differ in `name`, they will be processed together in [Parser] and [Writer] if the [Scope] is set to
     *   [Scope.Modules].
     */
    @Serializable
    @Immutable
    data class ProjectConstructor(
        val scripts: EmbeddedScripts,
    )

    val useImplicitStart: Boolean
        get() = fields.any { it.replaceStart }

    val useImplicitEnd: Boolean
        get() = fields.any { it.replaceEnd }

    override fun getSavedParamsFile(): File = RecordDir.resolve(name + LabelerSavedParamsFileExtension)

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

    fun migrate(): LabelerConf {
        if (serialVersion == 0) {
            // Add the extra properties by defaultExtras and extraFieldNames.
            if (extraFieldNames == null || defaultExtras == null) {
                Log.debug(
                    "Migrating extra properties of labeler $name from serialVersion 0 to $SerialVersion failed: " +
                        "extraFieldNames or defaultExtras is null",
                )
                return this
            }
            Log.debug("Migrating extra properties of labeler $name from serialVersion 0 to $SerialVersion")
            return this.copy(
                extraFields = extraFieldNames.mapIndexed { index, extraFieldName ->
                    ExtraField(
                        name = extraFieldName,
                        default = defaultExtras[index],
                        displayedName = LocalizedJsonString(
                            mapOf(Language.default.code to extraFieldName),
                        ),
                        isVisible = false,
                        isEditable = false,
                        isOptional = false, // Old version does not support optional extra fields
                    )
                },
                extraFieldNames = null,
                defaultExtras = null,
            )
        } else {
            return this
        }
    }

    companion object {
        const val LabelerFileExtension = "labeler.json"
        private const val LabelerSavedParamsFileExtension = ".labeler.param.json"
        private const val SerialVersion = 2
    }
}
