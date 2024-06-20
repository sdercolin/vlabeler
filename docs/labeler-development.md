# Develop Custom Labelers

A `labeler` is a set of configuration file and scripts that define many behaviors of the application for a specific
labeling scenario.

This guide will walk you through the process of developing a custom labeler for `vLabeler`.

Here is a list of the topics covered in this guide:

- [Understand a vLabeler Project](#understand-a-vlabeler-project)
- [Labeler Structure](#labeler-structure)
- [Labeler Definition](#labeler-definition)
- [Scripting in a Labeler](#scripting-in-a-labeler)
    - [Constructing a Project](#constructing-a-project)
    - [Property Getter](#property-getter)
    - [Property Setter](#property-setter)
    - [Parsing Raw Labels](#parsing-raw-labels)
    - [Writing Raw Labels](#writing-raw-labels)
    - [Injecting Parameter Values](#injecting-parameter-values)
- [Miscellaneous](#miscellaneous)

## Understand a vLabeler Project

Before we start, we need some basic knowledge about the project structure of `vLabeler`.

### Entry

An entry is the smallest data unit in `vLabeler`. It represents a piece of audio data with a start time and a duration.
Most voice generation software use similar ways to represent audio data. For example, in UTAU, an entry is a line in the
`oto.ini` file, and in NNSVS, an entry is a line in the `lab` file.

An entry contains at least its start time, end time, label name and a reference to the audio file. In addition, it may
contain other time points and extra information.

In `vLabeler`, we convert the existing label data into entries, and then edit the entries, which is the core of the
labeling process. After that, we convert the entries back to the original label data.

### Module

A module is a subproject in `vLabeler` (We use the name `subproject` in the UI, and use `module` in the codebase or
developing context).

A module contains a set of entries. Because in many voice database, the entries are organized in a hierarchical way,
we use modules to represent the hierarchy.

A module should have a name, a reference to a directory that contains the audio files, and a reference to a raw label
file (e.g. `oto.ini` or `lab` file) to support batch importing and exporting.

### Project

Finally, a project is a collection of modules. It also contains some meta information such as the name of the project,
the root directory of the voice database, etc.

Let's take a UTAU singer as an example. The structure of the voice bank may look like this:

```
your_singer
    ├── some wav files
    ├── oto.ini
    ├── C4
    │   ├── some wav files
    │   └── oto.ini
    ├── F4
    │   ├── some wav files
    │   └── oto.ini
    └── C5
        ├── some wav files
        └── oto.ini
```

This is a typical structure of a multi-pitch UTAU voice bank. Each pitch has its own `oto.ini` file, and the `oto.ini`
file in the root directory is used for some special sample files.

In `vLabeler`, the built-in `UTAU singer labeler` is designed for this scenario. For the voice bank above, it will
create a project with 4 modules and each module contains the entries in the corresponding `oto.ini` file.

```
your_vlabeler_project
    ├── (Root) module
    │   ├── path: "" (the same as the root directory of the voice bank)
    │   ├── entries: (entries in the root `oto.ini` file)
    │   └── sample files: (wav files in the root directory)
    ├── "C4" module
    │   ├── path: "C4" (the "C4" folder)
    │   ├── entries: (entries in the "C4/oto.ini" file)
    │   └── sample files: (wav files in the "C4" folder)
    ├── "F4" module
    ......
```

Now we have a basic understanding of the project structure of `vLabeler`.

In this example, the structure of the voice bank is very similar to the structure of the project. So it is easy to
create a project from the voice bank. However, in many cases, the structure of the voice bank is very different from
the structure of the project. For example, in NNSVS, the entries are organized in a single `lab` file, and its
corresponding audio file is in a different folder. In this case, users may probably want to have one subproject for
each audio file along with its `lab` file.

Apparently, that's very different from the way in the UTAU example. We need to customize the project construction
process to support different scenarios. This is one of the main functions of a labeler.

In addition, labelers should define:

- How to parse raw labels into entries
- How to write entries as raw labels
- How to display entries in the UI
- What properties of entries can be viewed and edited in the UI
- etc.

In the following sections, we will learn the structure of a labeler and how to develop a custom labeler.

## Labeler Structure

A labeler is a folder with the following structure:

```
your_labeler
    ├── labeler.json
    ├── parser.js
    ├── writer.js
    ├── projectConstructor.js
    ... (other scripts and resources)
```

- folder name (e.g. `your_labeler`) should be a unique name of the labeler
- `labeler.json` is the main configuration file of the labeler
- `*.js` files are scripts used by the labeler
- other files, such as dictionary files, may be used by the scripts

<details>
<summary>Legacy labeler as a single file</summary>

Before 1.0.0-beta20 (labeler serial version: 2), labelers are single files with the `.labeler.json` extension. It
doesn't support resource files, and has all the scripts embedded in the file. It is still supported, but we recommend
using the new structure.
</details>

### Referencing scripts

An `EmbeddedScripts` type is defined for referencing scripts in the `labeler.json` file.
When we have a field of the `EmbeddedScripts` type, its value could be:

- a string, which is the path of the script relative to the `labeler.json` file
- a list of strings, which are JavaScript code snippets split by lines

For example, the following `labeler.json` file references a script `parser.js` in the same folder.

```json5
{
    // ...,
    "parser": {
        "scope": "Entry",
        "scripts": "parser.js"
    },
    // ...
}
```

And if the scripts are short, we can also embed them in the `labeler.json` file.

```json5
{
    // ...,
    "parser": {
        "scope": "Entry",
        "scripts": [
            "// JavaScript code line 1",
            "// JavaScript code line 2"
        ]
    },
    // ...
}
```

## Labeler Definition

Let's look into the `labeler.json` file. The following table briefly describes the fields in the root JSON object.

You can also refer to the heavily commented Kotlin source
code [LabelerConf.kt](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) for details.

| Key                  | Type                           | Default value | Description                                                                                                                                |
|----------------------|--------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| name                 | String                         | (Required)    | This value should match the folder's name.                                                                                                 |
| version              | Integer                        | 1             | The version of the labeler.                                                                                                                |
| serialVersion        | Integer                        | 0             | The serial (structure) version of the labeler.                                                                                             |
| singleFile           | Boolean                        | true          | Whether the labeler is a single file (legacy).                                                                                             |
| extension            | String                         | (Required)    | The extension of the raw label file.                                                                                                       |
| defaultInputFilePath | String &#124; null             | null          | The default raw label file path defined for single module projects.                                                                        |
| displayedName        | String (Localized)             | `name` value  | The displayed name of the labeler.                                                                                                         |
| author               | String                         | (Required)    | The author of the labeler.                                                                                                                 |
| email                | String                         | ""            | Contact email of the author.                                                                                                               |
| description          | String (Localized)             | ""            | A brief description of the labeler.                                                                                                        |
| website              | String                         | ""            | The website or source code repository of the labeler.                                                                                      |
| categoryTag          | String                         | ""            | The category tag of the labeler. The labeler will be categorized as `Other` if not specified.                                              |
| displayOrder         | Integer                        | 0             | The display order of the labeler in the dropdown list.                                                                                     |
| continuous           | Boolean                        | false         | Whether the entries are continuous, i.e. the end time of an entry is the start time of the next entry.                                     |
| allowSameNameEntry   | Boolean                        | false         | Whether a module can contain entries with the same name.                                                                                   |
| defaultEntryName     | String &#124; null             | null          | The default name of an entry. If null, sample file name without extension will be used.                                                    |
| defaultValues        | Float[]                        | (Required)    | The default values of timing parameters listed as `[start, *fields, end]` in milliseconds.                                                 |
| fields               | Field[]                        | (Required)    | The custom timing field definitions of an entry besides standard "start" and "end" fields. See [Field](#field) for details.                |
| extraFields          | ExtraField[]                   | []            | The extra field definitions that are not timing fields in entry level. See [Extra Field](#extra-field) for details.                        |
| moduleExtraFields    | ExtraField[]                   | []            | The extra field definitions in module level. See [Extra Field](#extra-field) for details.                                                  |
| lockedDrag           | LockedDrag                     | {}            | The definition of locked drag behavior i.e. all parameters will move with dragged one. See [Locked Drag](#locked-drag) for details.        |
| overflowBeforeStart  | PointOverflow                  | "Error"       | Action taken when there are points before "start". See [Point Overflow](#point-overflow) for details.                                      |
| overflowAfterEnd     | PointOverflow                  | "Error"       | Action taken when there are points after "end". See [Point Overflow](#point-overflow) for details.                                         |
| postEditNextTrigger  | PostEditTrigger                | {}            | Trigger settings of `Go to next entry after editing` action on "start" and "end". See [Post-edit Actions](#post-edit-actions) for details. |
| postEditDoneTrigger  | PostEditTrigger                | {}            | Trigger settings of `Mark as done after editing` action on "start" and "end". See [Post-edit Actions](#post-edit-actions) for details.     |
| decimalDigit         | Integer &#124; null            | 2             | Decimal digit count used in `properties` and `writer`.                                                                                     |
| properties           | Property[]                     | []            | The definitions of properties. See [Property](#property) for details.                                                                      |
| parser               | Parser                         | (Required)    | The definition of the parser. See [Parser](#parser) for details.                                                                           |
| writer               | Writer                         | (Required)    | The definition of the writer. See [Writer](#writer) for details.                                                                           |
| parameters           | ParameterHolder[]              | []            | The definitions of parameters. See [Parameters](#parameters) for details.                                                                  |
| projectConstructor   | ProjectConstructor &#124; null | null          | The definition of the project constructor. See [Project Constructor](#project-constructor) for details.                                    |
| resourceFiles        | String[]                       | []            | Files utilized as resources in your scripts. Their contents are fed into your scripts as string values in the order listed.                |

We will explain some of the fields in the following sections.

### Naming and Versioning

A certain distribution of a labeler should have a unique name and version. i.e. everytime you modify the labeler and
publish it, you should increase the version number. You should also avoid using a same name as existing labelers.

`vLabeler` automatically handles labeler updating based on the name and version. If a project is created with a higher
version of a labeler than the one installed in the application (or the labeler is not installed), the application will
automatically install the new version of the labeler from the project file. If the version of the labeler installed in
the application is higher than the one in the project file, the application will use the installed version.

Note that if a labeler defines resource files, the application will not automatically update the labeler because the
resource files are not bundled in the project file. Users need to manually update the labeler to same version as the
one in the project file, or a higher version. When publishing a new version of a labeler, please make sure that it is
compatible with the previous versions, because vLabeler doesn't stop users from using an older version of the labeler.

### Serial Version

The serial version is used to determine whether the structure of the labeler is compatible with the application. Please
check the latest serial version in the [Update History of Labeler Structure](labeler-structure-updates.md) section and
use it as the `serialVersion` of your labeler.

When you follow this guide to develop a labeler, please make sure you have the `singleFile` field set to `false`.
This field is used to determine whether the labeler is a single file (legacy) labeler.

### Extension

The `extension` field is used to determine the file extension of the raw label file. It is used to filter input and
output label files in the file chooser dialog. This field is used to filter out plugins that are not compatible with
the labeler. This value doesn't include the preceding dot (e.g. use `lab` instead of `.lab`).

### Continuous

The boolean flag `continuous` is used to determine whether the entries are continuous, i.e. the end time of an entry is
the start time of the next entry. If the entries are continuous, the multi-entry editing feature will be enabled and
activated by default.

This field is a critical field that determines a lot of behaviors of the application. Please make sure you set it
correctly.

### Field

The `fields` field defines the custom timing field definitions of an entry besides standard "start" and "end" fields.

A value of a field (including standard fields "start" and "end") is a floating number representing time in milliseconds,
relative to the start time of the sample file. Every field will be rendered in the editor as a controller line, which
can be dragged to change the value of the field.

The value of the custom fields will be stored in the `points` field of
an [entry](../src/jvmMain/resources/js/class_entry.js) as floating numbers. The order of the
values in `points` should be strictly the same as the order of its corresponding `Field` in the `fields` field.

The `fields` field is an array of `Field` objects, which has the following fields:

| Key                 | Type                | Default value | Description                                                                                                                                        |
|---------------------|---------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| name                | String              | (Required)    | The name of the field.                                                                                                                             |
| label               | String (Localized)  | (Required)    | The label text of the field in the editor.                                                                                                         |
| color               | String              | (Required)    | The hex color code of the field, used in the editor.                                                                                               |
| height              | Float               | (Required)    | The height of the controller line of the field relative to the height of the waveforms. (Between 0 and 1)                                          |
| dragBase            | Boolean             | false         | Whether the field is used as the base of locked drag. See [Locked Drag](#locked-drag) for details.                                                 |
| constraints         | Constraint[]        | []            | The constraints of the field. See [Constraint](#constraint) for details.                                                                           |
| shortcutIndex       | Integer &#124; null | null          | Index of this field in the shortcut list. Could be 1~8 (0 is reserved for "start"). See [Shortcut](#shortcut) for details.                         |
| replaceStart        | Boolean             | false         | Whether the field should replace the "start" field. See [Replace standard fields](#replace-standard-fields) for details.                           |
| replaceEnd          | Boolean             | false         | Whether the field should replace the "end" field. See [Replace standard fields](#replace-standard-fields) for details.                             |
| triggerPostEditNext | Boolean             | false         | Whether the edition of this field should trigger `Go to next entry after editing` action. See [Post-edit actions](#post-edit-actions) for details. |
| triggerPostEditDone | Boolean             | false         | Whether the edition of this field should trigger `Mark as done after editing` action. See [Post-edit actions](#post-edit-actions) for details.     |

#### Constraint

The `constraints` field defines the constraints of a field. It is an array of `Constraint` objects. Each `Constraint`
object is in the following format:

```json
{
    "min": 1,
    "max": 2
}
```

- `min`: Index of the field that should be smaller or equal to this field. (Optional)
- `max`: Index of the field that should be larger or equal to this field. (Optional)

The standard fields `start` and `end` should not be considered here, because all fields should be between `start` and
`end`.

For every constraint, you don't need to set it in both fields. For example, when we have two
fields `["field1", "field2"]`,
if you want to set the constraint that
`field1` should be smaller or equal to `field2`, you can set `field1`'s `max` to `1` (the index of `field2`), or set
`field2`'s `min` to `0` (the index of `field1`). You don't have to set both.

By defining constraints, you can request the application to block dragging when the constraints are not satisfied.
In the example above, when user drags the controller of `field1` to a position that is larger than the `field2`
position, the controller of `field1` will stop at the same position as `field2`.

Please note that this is only used to block editions via dragging. Users can still edit the fields by inputting values
directly or using plugins to set the field values. In these cases, the constraints will not be checked. If you want to
define stronger constraints, you can check them and throw errors in your scripts, such as property setters and writer.

#### Shortcut

`vLabeler` has default shortcuts to set a field to the current cursor position. The shortcuts are `Q`, `W`, `E`, `R`,
..., `I`, `O`, `P` by default. `Q` is used to set the `start` field. The next shortcuts, beginning with `W`, are
assigned to the `Field`s in the order of their `shortcutIndex` values. The `end` field is assigned with the next
available shortcut after the last `Field`.

We recommend you to set the `shortcutIndex` of your custom fields in the order of their appearance in the timeline.

#### Replace Standard Fields

In `vLabeler`, all the custom timing points should be between the `start` and `end` fields. However, in some cases, we
want to allow the custom fields to be before `start` or after `end`. For example, in UTAU, the `overlap` field could be
before `start`.

To support this, the built-in `UTAU singer labeler` and `UTAU oto labeler` add a new `left` field as custom fields,
and set `replaceStart` to `true` for the `left` field.

In this way, the `left` field will replace the `start` field to be displayed in the editor, and the `overlap` will not
be restricted to be between `start` (actually `left`) and `end`.

When the user edits the `left` field, the `start` field will be automatically updated to the minimum value among al
. The real `start` field, which is hidden in the editor, will be automatically updated to the minimum values among all
the custom fields.

Note that if you use fields to replace the standard fields, you need to assign values for both, in this case, the
`left` field and the `start` field, in the labeler scripts, especially the `parser` and property setters.

Note that this feature is only supported for non-[continuous](#continuous) labelers.

### Extra Field

The `extraFields` field defines the extra field definitions that are not timing fields, used in entry level or module
level.
Comparing to the `fields` field, the extra fields are not timing fields, and their values are stored as strings or
explicit `null`s.

Typically, the extra fields are used to store some extra information of an entry that is not related to timing.
Some are not supposed to be visible to users, but only used in the scripts; some may be visible and/or editable in a
dialog for users to edit the extra information of an entry.

For entry level extras, the values will be stored in the `extras` field of
an [entry](../src/jvmMain/resources/js/class_entry.js) The order of the values in `extras`
should be strictly the same as the order of its corresponding `ExtraField` in the `extraFields` field.
When an extra field has a `null` value, it should also appear in the `extras` field to keep the correct index.

For module level extras, the values will be stored as a map. See [Parsing in Scope `Modules`](#parsing-in-scope-modules)
and [Writing in Scope `Modules`](#writing-in-scope-modules) for details.

The `extraFields` field is an array of `ExtraField` objects, which has the following fields:

| Key           | Type               | Default value  | Description                                                    |
|---------------|--------------------|----------------|----------------------------------------------------------------|
| name          | String             | (Required)     | The name of the extra field.                                   |
| displayedName | String (Localized) | same as `name` | The displayed name of the extra field in the configuration UI. |
| defaultValue  | String &#124; null | (Required)     | The default value of the extra field.                          |
| isVisible     | Boolean            | false          | Whether the extra field is visible in the configuration UI.    |
| isEditable    | Boolean            | false          | Whether the extra field is editable in the configuration UI.   |
| isOptional    | Boolean            | false          | Whether the extra field can have a `null` value.               |

An example of a defined `ExtraField` is the `rawRight` in the `UTAU singer labeler` or `UTAU oto labeler`.
In UTAU oto files, a "cutoff" or "right" value can be negative or non-negative. A negative value means a relative value
to the start time of the sample file, and a non-negative value means a relative value to the end time of the sample.

In `vLabeler`, basically we use relative value to the start of the sample for all fields, but for `end` field, we allow
negative values to be temporarily stored with the `needSync` field set to `true`. By setting the `end` to a negative
value (which in `vLabeler` means it is relative to the end of sample) and `needSync` to `true`, the application will
automatically convert the value to a relative value to the start of the sample when the sample is loaded for the first
time.
Apparently, we need to know the sample's duration to do the conversion,
but it remains unknown until the sample is once loaded, which may happen later or even never when the project is
exported.
So we need to store the original value of `cutoff` or `right` somewhere to be used during export.

You can check the scripts of this labeler to see how the `rawRight` field is used.

### Locked Drag

Sometimes we want to move all the timing points together when dragging one of them. For example, when editing UTAU oto
files, we may want to keep the distances between all parameters, but move their positions together.

To support this, `vLabeler` provides a `Locked Drag` feature, which is referred to as `fixed-drag` in the UI.
By default, when user drags a controller line of a "primary" field, all the other controller lines will move with it.

When dragging with `Shift` key pressed, locked drag is not conducted for the "primary" field(s), but for all the other
fields.

In the preferences settings, users can choose to set the `start` field as "primary", or "Use settings defined by the
labeler". Here, the "settings" refer to the `lockedDrag` field in the root object, which has the following value:

```json
{
    "useStart": false,
    "useDragBase": false
}
```

- `useStart`: Whether the `start` field should be used as "primary" field. Defaults to `false`. If there is a field with
  `replaceStart` set to `true`, it will be affected by this setting, instead of the original `start` field.
- `useDragBase`: Whether a field with `dragBase` set to `true` should be used as "primary" field. Defaults
  to `false`.

### Point Overflow

The `overflowBeforeStart` and `overflowAfterEnd` fields define the actions taken when there are points before "start"
and after "end" respectively.

The value of the fields should be one of the following:

- `Error`: Throw an error when there are points before "start" or after "end".
- `AdjustBorder`: Adjust the "start" or "end" to the minimum or maximum value of the points.
- `AdjustPoint`": Adjust the overflow points to the "start" or "end" value.

These fields default to `Error`.

### Post-edit Actions

`vLabeler` has implemented two actions that can be triggered after editing an entry:

- `Go to next entry after editing`
- `Mark as done after editing`

By `editing the entry`, we actually means the defined trigger fields are edited.

In the root object, there are two fields `postEditNextTrigger` and `postEditDoneTrigger` that define the trigger for
the two actions respectively. Their values are of the `PostEditTrigger` type, in the following format:

```json
{
    "useStart": false,
    "useEnd": false
}
```

- `useStart`: Whether the `start` field should be used as trigger field. Defaults to `false`. If there is a field with
  `replaceStart` set to `true`, it will be affected by this setting, instead of the original `start` field.
- `useEnd`: Whether the `end` field should be used as trigger field. Defaults to `false`. If there is a field with
  `replaceEnd` set to `true`, it will be affected by this setting, instead of the original `end` field.

For setting triggers on custom fields, you can use the `triggerPostEditNext` and `triggerPostEditDone` fields in the
`Field` object instead. They are `false` by default.

### Property

By using standard fields "start" and "end", custom fields defined by `fields` and extra fields defined by `extraFields`,
we can store all the information and provide UI for users to edit them.

However, the values stored in the fields are not always the values we want to display to users. For example, in UTAU oto
files, the `preutterance` field is relative to the `left` field, and that's the value users are familiar with. But in
`vLabeler`, we always save values relative to the start of the sample. So we need to define a property to do the
conversion when displaying the value to users, or when users input a value, with the definition in the original oto
files.

The root object of labeler has a `properties` field, which is an array of `Property` objects.
A `Property` object defines a property of an entry to be shown in the property views and used in scripts for simpler
calculations. The `Property` object has the following fields:

| Key           | Type                        | Default value | Description                                                                                                                                                                                                                       |
|---------------|-----------------------------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name          | String                      | (Required)    | The name of the property.                                                                                                                                                                                                         |
| displayedName | String (Localized)          | (Required)    | The displayed name of the property in the property view.                                                                                                                                                                          |
| valueGetter   | EmbeddedScripts             | (Required)    | The scripts to get the value of the property.                                                                                                                                                                                     |
| valueSetter   | EmbeddedScripts &#124; null | null          | The scripts to set the value of the property.                                                                                                                                                                                     |
| shortcutIndex | Integer &#124; null         | null          | Index of this property in the shortcut list used by the action `Set Property`. Could be 0~9. Basically we recommend to set it as the same as the index of this object in the `properties` array, if you want to make it writable. |

See [Property Getter](#property-getter) and [Property Setter](#property-setter) for details about the scripts.

### Parser

The `parser` field defines how the raw labels are parsed into entries. The object has the following fields:

| Key               | Type                     | Default value | Description                                                                                                                                    |
|-------------------|--------------------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| scope             | "Entry" &#124; "Modules" | (Required)    | The scope of the parser, which determines the available input and required output of the parser scripts.                                       |
| defaultEncoding   | String                   | "UTF-8"       | The default encoding used to read the raw label file.                                                                                          |
| extractionPattern | String (Regex)           | ""            | The regular expression used to extract variables from an entry line. Only used when `scope` is `Entry`.                                        |
| variableNames     | String[]                 | []            | The names of the variables extracted from an entry line. Only used when `scope` is `Entry`. The variables will later be passed to the scripts. |
| scripts           | EmbeddedScripts          | (Required)    | The scripts to parse the raw labels.                                                                                                           |

See [Parsing Raw Labels](#parsing-raw-labels) for details about the scripts.

### Writer

The `writer` field defines how the entries are written as raw labels. The object has the following fields:

| Key     | Type                        | Default value | Description                                                                                              |
|---------|-----------------------------|---------------|----------------------------------------------------------------------------------------------------------|
| scope   | "Entry" &#124; "Modules"    | "Entry"       | The scope of the writer, which determines the available input and required output of the writer scripts. |
| format  | String &#124; null          | null          | String template to format the entry as a line in the raw label files. Only used when `scope` is `Entry`. |
| scripts | EmbeddedScripts &#124; null | null          | The scripts to write the entries as raw labels.                                                          |

Either `format` or `scripts` should be defined. If both are defined, `scripts` will be used.

#### Use `format`

A `format` value is a string template where a `{<variable name>}` is a placeholder for a variable.
For example, `{sample}:{name}={start},{middle},{end}` will be written like `a.wav:a:100,220.5,300`.

The variables available in the template are:

- `sample`: The name of the sample file.
- `name`: The name of the entry.
- `start`: The `start` field of the entry as a number.
- `end`: The `end` field of the entry as a number.
- "name" of a **Field**: The value of the field as a number.
- "name" of a **Property**: The value of the property as a number.
- "name" of an **ExtraField**: The value of the extra field as a string or `null`.

If a name is shared by a field/extra field and a property, it will be used to refer to the property.

#### Use `scripts`

See [Writing Raw Labels](#writing-raw-labels) for details about the scripts.

### Parameters

While labelers are used as configuration files for the application, their contents are not supposed to be changed by
users directly. However, sometimes we want to provide some parameters for users to configure the labeler. For example,
in the `UTAU singer labeler`, we want to provide a parameter to allow users to choose whether a negative `overlap`
value is allowed. Different users may have different preferences on this, so we want to make it configurable in runtime.

To support this, `vLabeler` provides a `parameters` field in the root object to define labeler parameters.
These parameters are shown in the labeler settings dialog during project creation, and some of them are also editable
during project editing.

The object in the `parameters` field is a `ParameterHolder` object, which has the following fields:

| Key        | Type                        | Default value | Description                                                                                                                            |
|------------|-----------------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------|
| parameter  | Parameter                   | (Required)    | The definition of the parameter. See [Parameter](parameter.md) for details.                                                            |
| injector   | EmbeddedScripts &#124; null | null          | The scripts to inject the parameter value into the labeler. See [Injecting Parameter Values](#injecting-parameter-values) for details. |
| changeable | Boolean                     | false         | Whether the parameter value can be changed after project creation.                                                                     |

### Project Constructor

The object only contain a field `scripts` in `EmbeddedScripts` type, e.g.

```json
{
    "scripts": "projectConstructor.js"
}
```

See the [Construct a Project] section for details about the scripts.

## Scripting in a Labeler

In the previous sections, we have learned the structure and definition of a labeler. In this section, we will learn how
to write scripts for a labeler.

Before we look into the details, please read [Scripting in vLabeler](scripting.md) to get a basic understanding of the
scripting environment in `vLabeler`.

You may also want to refer to [Referencing scripts](#referencing-scripts) again about how we reference scripts in the
`labeler.json` file.

### Constructing a Project

In the [Understand a vLabeler Project](#understand-a-vlabeler-project) section, we have learned the structure of a
project. In this section, we will learn how to construct a project from a labeler.

A simplest case of constructing a project is to create a single module project with a few entries written in a single
file under the root directory. The directory may look like this:

```
your_database
    ├── some wav files
    └── a raw label file
```

In this case, no scripts are needed. We can just set the `defaultInputFilePath` field in the `labeler.json` file to the
path of the raw label file relative to the root directory, leaving `projectConstructor` as `null`

```json5
{
    // ...,
    "defaultInputFilePath": "a raw label file",
    "projectConstructor": null,
    // ...
}
```

The project is constructed automatically in the following structure:

```
your_project
    └── (Root) module
        ├── path: "" (the same as the root directory of the database)
        ├── entries: (entries in the raw label file)
        └── sample files: (wav files in the root directory)
```

However, in many cases, we want to have multiple modules in a project, and the entries are organized in different files
in different folders.

In this case, we need to set the `scripts` field in the `projectConstructor` field in the `labeler.json` file to
refer to a JavaScript code snippet.

#### Input

Before your scripts are executed, the following variables will be set in the JavaScript environment:

| Name                     | Type                | Description                                                                                                      |
|--------------------------|---------------------|------------------------------------------------------------------------------------------------------------------|
| root                     | [File](file-api.md) | The root directory of the project.                                                                               |
| params                   | Dictionary          | A dictionary containing all parameters defined in the labeler. You can get values using their `name` as the key. |
| resources                | String[]            | Texts from resource files, in the order they appear in `labeler.json`.                                           |
| encoding                 | String              | The encoding of the raw label file, selected by the user during project creation.                                |
| acceptedSampleExtensions | String[]            | String array of the accepted sample file extensions by `vLabeler`. (e.g. `["wav", "mp3"]`)                       |
| debug                    | Boolean             | Whether the execution is in debug mode (during the Gradle `run` task).                                           |

#### Output

After your scripts are executed, the `modules` variable should be set properly for the application to construct the
project.

The `modules` variable should be a list of [ModuleDefinition](../src/jvmMain/resources/js/module_definition.js) objects,
which has the following fields:

| Name                | Type                 | Description                                                                                                                       |
|---------------------|----------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| name                | String               | The name of the module.                                                                                                           |
| sampleDirectoryPath | String               | The absolute path of the directory containing the sample files.                                                                   |
| sampleFileNames     | String[]             | The names of the sample files that are used in the module.                                                                        |
| inputFilePaths      | String[] &#124; null | The absolute paths of the input files that are used in the module.                                                                |
| labelFilePath       | String &#124; null   | The absolute path of the output label file. If not set, user needs to select an output path everytime when exporting this module. |

Here's a simple example of a project constructor script:

```js
let modules = []

for (let folder of root.listChildDirectories()) {
    let sampleFiles = folder.listChildFiles().filter(file => acceptedSampleExtensions.includes(file.getExtension()))
    if (sampleFiles.length > 0) {
        let labelPath = folder.resolve("label.txt").getAbsolutePath()
        let def = new ModuleDefinition(
                folder.getName(),
                folder.getAbsolutePath(),
                sampleFiles.map(file => file.getName()),
                [labelPath],
                labelPath
        )
        modules.push(def)
    }
}

if (modules.length === 0) {
    error("No sample files found. Please check the labeler settings to ensure your sample folders are included.")
}
```

In this example, we iterate through all the folders in the root directory, and for each folder, we check if there are
sample files in it. If there are, we create a `ModuleDefinition` object and add it to the `modules` list.

Each module will have:

- The name of the folder as its name.
- The folder as its sample directory.
- All the files with accepted extensions in the folder as its sample files.
- The `label.txt` file under the folder as its input file. We don't really need to check the existence
  of this file. It will be checked by the application in the following steps.
- The `label.txt` file under the folder as its output file, which means this file will be created or overwritten when
  exporting this module, if the user doesn't manually determine the output path.

At last, we recommend checking if there are modules created. If not, throw an error with customized message to help
users to check their settings.

### Property Getter

The `valueGetter` field in a [Property](#property) object refers to a JavaScript code snippet that gets the value of the
property.

#### Input

- `entry`: The current [entry](../src/jvmMain/resources/js/class_entry.js) object.

#### Output

- `value`: You need to set the **global** `value` variable to the calculated value as a number.

Note: `let value = ...` or `const value = ...` will be ignored.

For example, if we want to define a simple property `duration` that is the difference between `end` and `start`, we can
write the getter as:

```js
value = entry.end - entry.start
```

#### Error handling

Basically we don't expect to have errors in the getter. Once an error occurs, the return value is silently set to `0`,
with error message logged.

### Property Setter

The `valueSetter` field in a [Property](#property) object refers to a JavaScript code snippet that sets the value of
the property.

#### Input

- `entry`: The current [entry](../src/jvmMain/resources/js/class_entry.js) object.
- `value`: The value input by the user as a number.

#### Output

- `entry`: You need to modify the `entry` object to reflect the change brought by the new value.

For example, if we want to define a simple property `duration` that is the difference between `end` and `start`, we can
write the setter as:

```js
entry.end = entry.start + value
```

#### Error handling

The [`error()` API](scripting.md#error-handling) is available in the setter scripts.

### Parsing Raw Labels

Before we introduce the details of the parser scripts, let's see how a project is created after all configurations
are done.

1. Module definitions are created as described in the [Constructing a Project](#constructing-a-project) section.
2. Create entries for each module
    1. If the scope is `Entry`, and a template generator plugin is used, the plugin will be executed to generate the
       entries. If the template plugin has defined
       an [input finder](plugin-development.md#dynamic-input-file-retrieval),
       it will be executed to find the input files; otherwise, the input files set in the module definition will be
       used. Non-existing files will be passed as `null` in the `inputs` array.
    2. If the scope is `Entry`, and no template generator plugin is used, only the **first** input file set in the
       module definition will be used. If this file exists, the `Entry` scope parser is executed on this file;
       otherwise, every sample will be assigned a default entry, using the `defaultValues` field in the labeler.
    3. If the scope is `Modules`, the module definitions are grouped by all the other properties except for `name`.
       For each group, the `Modules` scoped parser is executed. To support this process, the project constructor should
       ensure that only `name` is different for the module definitions in the same group.
3. Create modules with the entries created in the previous step
4. Create project with the modules created in the previous step

To summarize, the `Entry` scoped parser is executed per module, and the `Modules` scoped parser is executed per module
group which is defined by the project constructor in the previous step.

Next, let's see how to write the parser scripts that should be set in the `parser` object.

#### Common Input

The following variables will be set in the JavaScript environment before the parser scripts are executed, for both
`Entry` and `Modules` scoped parsers:

| Name            | Type       | Description                                                                                                      |
|-----------------|------------|------------------------------------------------------------------------------------------------------------------|
| inputFileNames  | String[]   | The names of the input files. For `Entry` scope, it only contains one element.                                   |
| sampleFileNames | String[]   | The names of the all the sample files in this module.                                                            |
| params          | Dictionary | A dictionary containing all parameters defined in the labeler. You can get values using their `name` as the key. |
| resources       | String[]   | Texts from resource files, in the order they appear in `labeler.json`.                                           |
| encoding        | String     | The encoding of the raw label file, selected by the user during project creation.                                |
| debug           | Boolean    | Whether the execution is in debug mode (during the Gradle `run` task).                                           |

#### Parsing in Scope `Entry`

As introduced in the [Parser](#parser) section, the `Entry` scoped parser utilizes the `extractionPattern` field and
`variableNames` field to extract variables from an entry line.

The input file is read line by line, and each line is matched with the `extractionPattern` to extract the variables.
After that, the parser scripts are executed to create the entry for this line.

Besides the common input variables, the following variables will be set in the JavaScript environment:

- `input`: the text of current line of the input file.
- any element in `variableNames`: the value of the variable extracted from the current line.

You need to assign the **global** `entry` variable to the created [entry](../src/jvmMain/resources/js/class_entry.js)
object.

Note: `let entry = ...` or `const entry = ...` will be ignored.

Here is an example of a parser script to work on a fake label file:

```js
// assume we have extracted `name`, `sample`, `start`, `end` from the input line using the regex
entry = new Entry(sample, name, parseFloat(start), parseFloat(end), [], [])

// or, parse the line here
parts = input.split(",")
entry = new Entry(parts[0], parts[1], parseFloat(parts[2]), parseFloat(parts[3]), [], [])
```

#### Parsing in Scope `Modules`

The `Modules` scoped parser is executed per module group, which is defined by the project constructor in the previous
step.

Here, we check the `inputFiles` in the module definition to see if they exist. If no input file exists, the application
creates an entry as fallback on each sample file, using the `defaultValues` field in the labeler.

If any input file exists, the parser scripts are executed with the following variables prepared in the JavaScript
environment, besides the common input variables:

- `moduleDefinitions`: the [ModuleDefinition](../src/jvmMain/resources/js/module_definition.js) objects of the module
  group.
- `inputs`: the file contents of the input files. Non-existing files will be passed as `null` in the `inputs` array.

You need to assign a `modules` variable with type `Entry[][]` to the created entries. Each element in the `modules`
variable is an array of entries for a module in the module group. The order of the elements in `modules` should be the
same as the order of the `moduleDefinitions` variable.

In addition, you can assign a `moduleExtras` variable with type `Dictionary[]` to the extras of each module in the
module group. The order should be the same as `modules`. The keys of each dictionary are the names of
elements in `moduleExtraFields` in `labeler.json`. The values are all stored as strings. If a value is `null`, do not
include the corresponding key in the dictionary.

### Writing Raw Labels

In the [Writer](#writer) section, we have learned that the `format` field or `scripts` field in the `writer` object
defines how the entries are written as raw labels. The `format` approach is only used for simple cases. In most cases,
we need to use the `scripts` field to write the raw labels.

Similar to the parser, we have two scopes for the writer: `Entry` and `Modules`.

#### Common Input

The following variables will be set in the JavaScript environment before the writer scripts are executed, for both
`Entry` and `Modules` scoped parsers:

| Name      | Type       | Description                                                                                                      |
|-----------|------------|------------------------------------------------------------------------------------------------------------------|
| params    | Dictionary | A dictionary containing all parameters defined in the labeler. You can get values using their `name` as the key. |
| resources | String[]   | Texts from resource files, in the order they appear in `labeler.json`.                                           |
| debug     | Boolean    | Whether the execution is in debug mode (during the Gradle `run` task).                                           |

#### Writing in Scope `Entry`

With the `Entry` scope, the writer scripts are executed per entry.

Besides the common input variables, the application sets the same variables as described in
the [Use `format`](#use-format) section.

The writer scripts should set the **global** `output` variable to the text of the output line.

Note: `let output = ...` or `const output = ...` will be ignored.

#### Writing in Scope `Modules`

With the `Modules` scope, the writer scripts are executed per module group, which is defined by the project constructor
in the previous step.

Besides the common input variables, the application sets the following variables before executing the writer scripts:

- `moduleNames`: the names of the modules in the module group.
- `modules`: in `Entry[][]` type, the entries of the module group. The order of the elements in `modules` is the same
  as the order of the `moduleNames` variable.
- `moduleExtras`: in `Dictionary[]` type, the extras of each module in the module group. The order of the elements in
  `moduleExtras` is the same as the order of the `moduleNames` variable. The keys of each dictionary are the names of
  elements in `moduleExtraFields` in `labeler.json`. The values are all stored as strings. If a value is `null`, the
  corresponding key will not appear in the dictionary.

The writer scripts should set the `output` variable to the text of the output file, which will be written to the
`labelFilePath` field in the module definition.

### Injecting Parameter Values

We have introduced how to define parameters in the [Parameters](#parameters) section. These parameters are configured
by users, and are passed to most scripts we have mentioned above.

However, sometimes we want to edit the labeler's JSON object itself based on the parameter values.
For example, we have a `useNegativeOvl` in the `UTAU oto labeler` to allow users to choose whether negative `overlap`
values are allowed. We want to adjust the `constraints` field of the `ovl` field based on this parameter.

To support this, `vLabeler` provides an `injector` field in the `ParameterHolder` object, which refers to a JavaScript
code snippet that is executed during project creation, and if the parameter is `changeable`, after it is changed as
well.

Every injector script is executed with the following variables prepared in the JavaScript environment:

- `labeler`: the JSON object loaded from the `labeler.json` file.
- `value`: the value of the parameter, in its actual type.

You can modify the `labeler` object to change the labeler's JSON object.

Note that if there are multiple parameters with `injector` defined, they will be modifying the same `labeler` object.

Here is the example of the `injector` script of the `useNegativeOvl` parameter in the `UTAU oto labeler`:

```js
labeler.fields[2].constraints[0].min = value ? null : 3
```

In this example, we set the `min` field of the first constraint of the `ovl` field to `null` if the parameter value is
`true`, which means it can be moved to the left side of the `left` field. Otherwise, we set it to `3`, which means it
cannot be moved to the left side of the `left` field, where `3` is the index of the `left` field in the `fields` array.

In addition, an injector cannot change the following fields:

- `name`
- `version`
- `extension`
- `displayedName`
- `description`
- `author`
- `website`
- `email`
- `continuous`
- `parameters`
- size of `fields`
- size of `defaultValues`
- size of `extraFields`
- `name` of elements in `fields`
- `name` of elements in `extraFields`
- `name` of elements in `properties`

## Miscellaneous

### Examples

All the officially developed labelers are open-sourced. You can check them out to learn more about how to develop
labelers.

Here are some typical labelers that you can refer to:

- [UTAU singer labeler](../resources/common/labelers/utau-singer-labeler): A labeler for UTAU voicebanks with multiple
  pitches. It uses most of the scripts introduced in this document.
- [NNSVS singer labeler](../resources/common/labelers/nnsvs-singer-labeler): A labeler for NNSVS voicebanks. You can
  refer to it for how a different structure of database is converted to a `vLabeler` project.
- [Textgrid labeler](https://github.com/sdercolin/vlabeler-textgrid): A labeler for Praat TextGrid files. It constructs
  a project with module groups, and uses parser and writer with the `Modules` scope.

### Localization

Check [Localized strings in vLabeler](localized-string.md) about the `String (Localized)` type mentioned above.

### Error handling

For in-depth understanding and strategies to handle errors, refer to the section
in [Scripting in vLabeler](scripting.md#error-handling).

### Debugging

You can use logs to help debug your scripts.
The standard output (e.g. `console.log()`) is written to `.logs/info.log` and the error output is written to
`.logs/error.log`.

If your labeler doesn't appear in the list, it might have faced issues during loading,
such as problems parsing `labeler.json`. Check the error log for more information.
