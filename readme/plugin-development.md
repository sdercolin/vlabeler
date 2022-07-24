# Plugin Development

This article introduces how to develop plugins for `vLabeler`.
Feel free to ask questions or make feature requests if you find the plugin specification not sufficient for your intended functionality.

## Plugin File Structure
A plugin for `vLabeler` is a folder containing:
1. A `plugin.json` file to define the plugin's behaviors
2. At least one `*.js` file as the scripts
3. Other files that your scripts use

## Plugin Definition
`plugin.json` file is a json object which has the following properties:

- `name`: (Required) String. Make sure the value is the same as the folder's name.
- `version`: Integer. Defaults to 1.
- `type`: (Required) String. Currently only `template`.
- `displayedName`: String. Defaults to `name`.
- `author`: (Required) String.
- `email`: String. Defaults to empty string.
- `description`: String. Defaults to empty string.
- `website`: String. Defaults to empty string.
- `supportedLabelFileExtension`: (Required) String. Extension of your label file (e.g. `ini` for UTAU oto).
- `inputFileExtension`: Nullable String. Extension of your input file if any. Defaults to `null`.
- `requireInputFile`: Boolean. Set to `true` if you always require an input file. Defaults to `false`.
- `parameters`: Nullable Object. See the `Defining Parameters` section for detail. Defaults to `null`.
- `scriptFiles`: (Required) List of String. File names of all your scripts files. The files will be executed in the same order as declared.
- `resourceFiles`: List of String. File names of all the files that you use as resources in your scripts. The contents will be passed to your scripts as string values in the same order as declared.

### Defining Parameters
`parameters` object contains an array named `list`.
Every object in `list` defines a parameter that is shown in the plugin config dialog and passed to your scripts.
The object has the following properties:
- `type`: (Required) String. Can be any one of `integer`, `float`, `boolean`, `string`, `enum`(actually string, but its possible values are limited).
- `name`: (Required) String. Parameter name for reference in your scripts.
- `label`: (Required) String. Displayed in the config dialog.
- `defaultValue`: (Required) Integer/Float/Boolean/String. Value type is according to the parameter's `type`.
- `min`, `max`: For parameter type `integer` and `float`. (Optional) Integer/Float.
- `multiLine`: For parameter type `string`. (Optional) Boolean. Set to `true` if you want to allow user enter multi-line string values. Defaults to `false`.
- `optional`: For parameter type `string`. (Required) Boolean. Set to `true` if you want to allow empty string values.
- `options`: For parameter type `enum`. (Required) List of String. Items of the enumerable.

## Scripting Environment
`vLabeler` uses embedded [JavaScript](https://developer.mozilla.org/ja/docs/Web/JavaScript) engine provided by [GraalVM 22.1](https://www.graalvm.org/22.1/reference-manual/js/).

It implements JavaScript in the ECMAScript (ECMA-262) specification which is fully compatible with the ECMAScript 2021 specification (ES12) 
Check [JavaScript Compatibility](https://www.graalvm.org/22.1/reference-manual/js/JavaScriptCompatibility/) for detailed info about language specifications.

## Template Generation Scripts
A plugin with `template` type is selected and executed in the `New Project` page.
It should create a list of entries for subsequent edition.

### Input
The following variables are provided before your scripts are executed.

- `inputs`: List of texts read from the input files. Check the list size if your input file is optional.
- `samples`: List of file names of the sample files. Extension `.wav` is not included.
- `params`: Dictionary / Object. Use `name` of the defined parameters as the key to get values in their actual types.
- `resources`: List of texts read from the resources files in the same order as declared in your `plugin.json`.
 
### Output
You have to create a list named `output` to pass the result back to the application.
The item in the list should be in the following type (the class is defined before your scripts are executed):
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

Please check [LabelerConf.kt](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) for details about its properties.

### Examples
Check the following built-in plugins as examples:
- [ust2lab-ja-kana](../resources/common/plugins/template/ust2lab-ja-kana): Use an input ust file to generate sinsy lab entries
- [cv-oto-gen](../resources/common/plugins/template/cv-oto-gen): Generate CV oto entries from parameters

### Debugging
You can use logs to help debug your scripts.
The standard output (e.g. `print()`) is written to `.logs/info.log` and the error output is written to `.logs/error.log`.
If the plugin is not shown in the list, there are probably some errors while loading the plugin (i.e. parsing `plugin.json`).
