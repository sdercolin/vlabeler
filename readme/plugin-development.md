# Plugin Development

This article introduces how to develop plugins for `vLabeler`.
Feel free to ask questions or make feature requests if you find the plugin specification not sufficient for your
intended functionality.

## Plugin File Structure

A plugin for `vLabeler` is a folder containing:

1. A `plugin.json` file to define the plugin's behaviors
2. At least one `*.js` file as the scripts
3. Other files that your scripts use

## Plugin Definition

`plugin.json` file is a json object which has the following properties:

| Property                    | Type                   | Default value  | Supported plugin type | Description                                                                                                                                                                        |
|-----------------------------|------------------------|----------------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                        | String                 | (Required)     | all                   | Make sure the value is the same as the folder's name.                                                                                                                              |
| version                     | Integer                | 1              | all                   |                                                                                                                                                                                    |
| type                        | String                 | (Required)     | all                   | `template` or `macro`.                                                                                                                                                             |
| displayedName               | String                 | same as `name` | all                   |                                                                                                                                                                                    |
| author                      | String                 | (Required)     | all                   |                                                                                                                                                                                    |
| email                       | String                 | ""             | all                   |                                                                                                                                                                                    |
| description                 | String                 | ""             | all                   |                                                                                                                                                                                    |
| website                     | String                 | ""             | all                   |                                                                                                                                                                                    |
| supportedLabelFileExtension | String                 | (Required)     | all                   | Extension(s) of your label file (e.g. `ini` for UTAU oto). `*`` and `&#124;` are supported.                                                                                        |
| inputFileExtension          | String &#124; null     | null           | template              | Extension of your input file if any.                                                                                                                                               |
| requireInputFile            | Boolean                | false          | template              | Set to `true` if you always require an input file.                                                                                                                                 |
| outputRawEntry              | Boolean                | false          | template              | Set to `true` if you want to output the raw entry instead of parsed object.                                                                                                        |
| parameters                  | Parameters &#124; null | null           | all                   | See the `Defining Parameters` section for detail.                                                                                                                                  |
| scriptFiles                 | List\<String>          | (Required)     | all                   | File names of all your scripts files. The files will be executed in the same order as declared.                                                                                    |
| resourceFiles               | List\<String>          | empty list     | all                   | List of String. File names of all the files that you use as resources in your scripts. The contents will be passed to your scripts as string values in the same order as declared. |

### Defining Parameters

`parameters` object contains an array named `list`:

```
{
    ...,
    "parameters": {
        "list": [...]
    },
    ...
}
```

Every object in `list` defines a parameter that is shown in the plugin config dialog and passed to your scripts.
The object has the following properties:

| Property             | Type                       | Default value | Supported parameter type | Description                                                                             |
|----------------------|----------------------------|---------------|--------------------------|-----------------------------------------------------------------------------------------|
| type                 | String                     | (Required)    | all                      | Can be any one of `integer`, `float`, `boolean`, `string`, `enum` and ,`entrySelector`. |
| name                 | String                     | (Required)    | all                      | Parameter name for reference in your scripts.                                           |
| label                | String                     | (Required)    | all                      | Displayed in the config dialog.                                                         |
| description          | String                     | ""            | all                      | Displayed in the config dialog.                                                         |
| defaultValue         | (Actual type of the value) | (Required)    | all                      | Value type is according to the parameter's `type`.                                      |
| defaultValueFromFile | String                     | null          | string                   | Set a file name if you want its content to be used as the default value.                |
| min                  | (Actual type of the value) | null          | integer, float           |                                                                                         |
| max                  | (Actual type of the value) | null          | integer, float           |                                                                                         |
| multiLine            | Boolean                    | false         | string                   | Set to `true` if you want to allow multi-line string values.                            |
| optional             | Boolean                    | (Required)    | string                   | Set to `true` if you want to allow empty string values.                                 |
| options              | List\<String>              | (Required)    | enum                     | Items of the enumerable.                                                                |

### Types

- `integer`: Integer value. Should be between `min` and `max` if defined.
- `float`: Float value. Should be between `min` and `max` if defined.
- `boolean`: Should be either `true` or `false`.
- `string`: String value. Should not contain line breaks if `multiLine` is `false`. Should not be empty if `optional`
  is `false`.
- `enum`: Enumerable value described as string. Should be one of the items in `options`.
- `entrySelector`: **Can only be used in a `macro` type plugin.** Instance
  of [EntrySelector](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/EntrySelector.kt) type:

```
{
    "filters": [
        {
            "type": "text", // an example of a `text` filter
            "subject": "name", // `name` for entry name or `sample` for sample name
            "matchType": "Contains", // `Contains` | `Equals` | `StartsWith` | `EndsWith` | `Regex`
            "matcherText": "foo"
        },
        {
            "type": "number", // an example of a `number` filter
            "subject": "overlap", // `name` of any property defined in the labeler
            "matchType": "GreaterThan", // `Equals` | `GreaterThan`| `LessThan` | `GreaterThanOrEquals` | `LessThanOrEquals`
            "comparerValue": 0.5, // only used when `comparerName` is null
            "comparerName": "offset" // nullable, `name` of any property defined in the labeler
        }
    ]
}
```

####

## Scripting Environment

`vLabeler` uses embedded [JavaScript](https://developer.mozilla.org/ja/docs/Web/JavaScript) engine provided
by [GraalVM 22.1](https://www.graalvm.org/22.1/reference-manual/js/).

It implements JavaScript in the ECMAScript (ECMA-262) specification which is fully compatible with the ECMAScript 2021
specification (ES12)
Check [JavaScript Compatibility](https://www.graalvm.org/22.1/reference-manual/js/JavaScriptCompatibility/) for detailed
info about language specifications.

## Template Generation Scripts

A plugin with `template` type is selected and executed in the `New Project` page.
It should create a list of entries for subsequent edition.

### Input

The following variables are provided before your scripts are executed.

| name      | type          | description                                                                                                        |
|-----------|---------------|--------------------------------------------------------------------------------------------------------------------|
| inputs    | List\<String> | List of texts read from the input files. Check the list size if your input file is optional.                       |
| samples   | List\<String> | List of file names of the sample files. Extension `.wav` is not included.                                          |
| params    | Dictionary    | Use `name` of the defined parameters as the key to get values in their actual types.                               |
| resources | List\<String> | List of texts read from the resources files in the same order as declared in your `plugin.json`.                   |
| labeler   | LabelerConf   | Equivalent Json object to [LabelerConf](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) object. |
| debug     | Boolean       | It's set to `true` only when the application is running in the debug environment (Gradle `run` task).              |

### Output

You have to create a list named `output` to pass the result back to the application.
You can choose to offer `output` in the following two ways:

#### 1. Output the parsed entry object

Put items in the following type to the `output` list (the class is defined before your scripts are executed):

```javascript
class Entry {
    constructor(sample, name, start, end, points, extras) {
        this.sample = sample // sample file name without extension
        this.name = name // entry name (alias)
        this.start = start // float value in millisecond
        this.end = end // float value in millisecond
        this.points = points // list of float values in millisecond
        this.extras = extras // list of string values
    }
}
```

Please check [LabelerConf.kt](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) for details about its
properties.

#### 2. Output the raw entry string

If `outputRawEntry` is set to `true`, instead of the above class, `output` should be set a list of string in the format
of the label file. They will be parsed by the labeler's parser later.

### Tips

1. If `labeler.allowSameNameEntry` is set to `false`, the labeler will discard entries with the same name except the
   first one. Handle the duplicated entry names in your scripts if you want to keep them.
2. The labeler will throw an error if no entry is created. Create a fallback entry in your scripts if you don't want
   users to see the error.
3. If a project is created by labeler's parser with a raw label file, it will include all sample files even if they do
   not appear in the raw label file. In that case, a default entry will be created for each sample file. However, if a
   project is created by a plugin, the sample files that are not referenced in the output will be ignored. So if you
   want to cover every sample file, do it in your scripts.

### Examples

Check the following built-in `template` plugins as examples:

- [ust2lab-ja-kana](../resources/common/plugins/template/ust2lab-ja-kana): Use an input ust file to generate sinsy lab
  entries
- [cv-oto-gen](../resources/common/plugins/template/cv-oto-gen): Generate CV oto entries from parameters
- [regex-raw-gen](../resources/common/plugins/template/regex-raw-gen): Use a regular expression to generate raw entry
  lines. Supports all types of labelers.

## Batch Edit (Macro) Scripts

A plugin with `macro` type is executed by an item in menu `Tools` -> `Batch Edit`. It is only available when editing a
project.

Basically it takes a list of entry objects and edits them, then sets the result to the `output` list.

### Input

The following variables are provided before your scripts are executed.

| name      | type          | description                                                                                                        |
|-----------|---------------|--------------------------------------------------------------------------------------------------------------------|
| entries   | List\<Entry>  | List of current [Entry](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/Entry.kt) objects in the project.       |
| params    | Dictionary    | Use `name` of the defined parameters as the key to get values in their actual types.                               |
| resources | List\<String> | List of texts read from the resources files in the same order as declared in your `plugin.json`.                   |
| labeler   | LabelerConf   | Equivalent Json object to [LabelerConf](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) object. |
| debug     | Boolean       | It's set to `true` only when the application is running in the debug environment (Gradle `run` task).              |

### Use an entry selector

A parameter with `entrySelector` type is passed in `params` as a list of the selected indexes of the `entries` list.

Following code is an example of using an entry selector:

```javascript
let selectedIndexes = params["selector"] // use actual name of your parameter
for (index in entries) {
    let entry = entries[index]
    if (selectedIndexes.includes(index)) {
        // edit the entry and set to the output list
    }
}
```

### Output

You have to create a list named `output` to pass the result back to the application.

`output` needs to be a list of objects in following type:

```javascript
class EditedEntry {
    constructor(originalIndex, entry) {
        this.originalIndex = originalIndex // null for newly added entries
        this.entry = entry
    }
}
```

### Examples

Check the following built-in `macro` plugins as examples:

- [batch-edit-entry-name](../resources/common/plugins/macro/batch-edit-entry-name): Edit selected entry names for all
  labelers. You can refer to it for the usage of entry selector
- [batch-edit-oto-parameter](../resources/common/plugins/macro/batch-edit-oto-parameter): Edit parameters of selected
  entries for UTAU oto. The `labeler` is used to get the specific settings about `oto.ini`
- [execute-scripts](../resources/common/plugins/macro/execute-scripts): Execute input scripts to edit the project. It
  can be used as a debug tool when developing a plugin.

## Error handling

When the scripts encounter illegal inputs, you can show error message to users by throwing an error along with setting
the `expectedError` property to `true`.

Other errors thrown in the scripts will be displayed as "Unexpected errors" without detailed information, indicating
that it is more likely to be a bug of the plugin, rather than an illegal input or something else that may happen in
normal use cases.

If user contacts you with an "Unexpected errors", you can ask for detailed information in the logs to help you solve the
issue.

```javascript
let unknownExpressionMatch = expression.match(/\$\{\w+}/)
if (unknownExpressionMatch) {
    expectedError = true
    throw new Error(`Unknown placeholder: ${unknownExpressionMatch[0]}`)
}
```

## Debugging

You can use logs to help debug your scripts.
The standard output (e.g. `print()`) is written to `.logs/info.log` and the error output is written to `.logs/error.log`
.
If the plugin is not shown in the list, there are probably some errors while loading the plugin (i.e.
parsing `plugin.json`).
