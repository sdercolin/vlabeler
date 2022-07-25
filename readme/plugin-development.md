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
| type                        | String                 | (Required)     | all                   | Currently only `template`.                                                                                                                                                         |
| displayedName               | String                 | same as `name` | all                   |                                                                                                                                                                                    |
| author                      | String                 | (Required)     | all                   |                                                                                                                                                                                    |
| email                       | String                 | ""             | all                   |                                                                                                                                                                                    |
| description                 | String                 | ""             | all                   |                                                                                                                                                                                    |
| website                     | String                 | ""             | all                   |                                                                                                                                                                                    |
| supportedLabelFileExtension | String                 | (Required)     | all                   | Extension(s) of your label file (e.g. `ini` for UTAU oto). `*`` and `&#124;` are supported.                                                                                        |
| inputFileExtension          | String &#124; null     | null           | all                   | Extension of your input file if any.                                                                                                                                               |
| requireInputFile            | Boolean                | false          | all                   | Set to `true` if you always require an input file.                                                                                                                                 |
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

| Property     | Type                                              | Default value | Supported parameter type | Description                                                        |
|--------------|---------------------------------------------------|---------------|--------------------------|--------------------------------------------------------------------|
| type         | String                                            | (Required)    | all                      | Can be any one of `integer`, `float`, `boolean`, `string`, `enum`. |
| name         | String                                            | (Required)    | all                      | Parameter name for reference in your scripts.                      |
| label        | String                                            | (Required)    | all                      | Displayed in the config dialog.                                    |
| description  | String                                            | ""            | all                      | Displayed in the config dialog.                                    |
| defaultValue | Integer &#124; Float &#124; Boolean &#124; String | (Required)    | all                      | Value type is according to the parameter's `type`.                 |
| min          | Integer &#124; Float &#124; null                  | null          | integer, float           |                                                                    |
| max          | Integer &#124; Float &#124; null                  | null          | integer, float           |                                                                    |
| multiLine    | Boolean                                           | false         | string                   | Set to `true` if you want to allow multi-line string values.       |
| optional     | Boolean                                           | (Required)    | string                   | Set to `true` if you want to allow empty string values.            |
| options      | List\<String>                                     | (Required)    | enum                     | Items of the enumerable.                                           |

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

| name      | type          | description                                                                                           |
|-----------|---------------|-------------------------------------------------------------------------------------------------------|
| inputs    | List\<String> | List of texts read from the input files. Check the list size if your input file is optional.          |
| samples   | List\<String> | List of file names of the sample files. Extension `.wav` is not included.                             |
| params    | Dictionary    | Use `name` of the defined parameters as the key to get values in their actual types.                  |
| resources | List\<String> | List of texts read from the resources files in the same order as declared in your `plugin.json`.      |
| debug     | Boolean       | It's set to `true` only when the application is running in the debug environment (Gradle `run` task). |

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

## Debugging

You can use logs to help debug your scripts.
The standard output (e.g. `print()`) is written to `.logs/info.log` and the error output is written to `.logs/error.log`
.
If the plugin is not shown in the list, there are probably some errors while loading the plugin (i.e.
parsing `plugin.json`).
