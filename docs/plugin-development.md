# Develop plugins for vLabeler

This article introduces how to develop plugins for `vLabeler`.
Feel free to ask questions or make feature requests if you find the plugin specification not sufficient for your
intended functionality.

## Overview

Currently we support two types of plugins:

- Macro plugins: They are executed during project editing. Typically they are used to edit the entries in batch.
- Template plugins: They are executed when creating a new project. They generate a list of entries for subsequent
  editing.

The two types of plugins are defined in the same structure, but they are executed in different contexts i.e. the
inputs and outputs are different.

In this article, we will introduce:

- [The file structure of a plugin](#plugin-file-structure)
- [How to define a plugin](#plugin-definition)
    - [Defining parameters](#defining-parameters)
- [How to write scripts for plugins](#scripting-for-plugins)
    - [Template generation scripts](#template-generation-scripts)
    - [Batch edit (macro) scripts](#batch-edit-macro-scripts)

## Plugin File Structure

A plugin for `vLabeler` is a folder containing:

1. A `plugin.json` file to define the plugin's behaviors
2. At least one `*.js` file as the scripts
3. Other files that your scripts use

## Plugin Definition

`plugin.json` file is a JSON object which has the following properties:

| Property                    | Type                   | Default value  | Supported plugin type | Description                                                                                                                                                                        |
|-----------------------------|------------------------|----------------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                        | String                 | (Required)     | all                   | Make sure the value is the same as the folder's name.                                                                                                                              |
| version                     | Integer                | 1              | all                   |                                                                                                                                                                                    |
| type                        | String                 | (Required)     | all                   | `template` or `macro`.                                                                                                                                                             |
| displayedName               | String (Localized)     | same as `name` | all                   |                                                                                                                                                                                    |
| author                      | String                 | (Required)     | all                   |                                                                                                                                                                                    |
| email                       | String                 | ""             | all                   |                                                                                                                                                                                    |
| description                 | String (Localized)     | ""             | all                   |                                                                                                                                                                                    |
| website                     | String                 | ""             | all                   |                                                                                                                                                                                    |
| supportedLabelFileExtension | String                 | (Required)     | all                   | Extension(s) of your label file (e.g. `ini` for UTAU oto). "*" and "&#124;" are supported.                                                                                         |
| outputRawEntry              | Boolean                | false          | template              | Set to `true` if you want to output the raw entry instead of parsed object.                                                                                                        |
| scope                       | String                 | "Module"       | all                   | `Module` or `Project`. The scope that the plugin can access and edit.                                                                                                              |
| parameters                  | Parameters &#124; null | null           | all                   | See the `Defining Parameters` section for detail.                                                                                                                                  |
| scriptFiles                 | String[]               | (Required)     | all                   | File names of all your scripts files. The files will be executed in the same order as declared.                                                                                    |
| resourceFiles               | String[]               | []             | all                   | List of String. File names of all the files that you use as resources in your scripts. The contents will be passed to your scripts as string values in the same order as declared. |
| inputFinderScriptFile       | String &#124; null     | null           | template              | File name of the script file to help find the input files dynamically.                                                                                                             |

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

Every object in `list` defines a parameter that is shown in the plugin configuration dialog and passed to your scripts.

See [Parameter](parameter.md) for the definition of a parameter.

## Scripting for Plugins

The following sections describe how to write scripts for plugins.

See [Scripting](scripting.md) for the scripting environment and available APIs.

### Template Generation Scripts

A plugin with `template` type is selected and executed on the `New Project` page.
It should create a list of entries for subsequent editions.

#### Input

The following variables are provided before your scripts are executed.

| name            | type                | description                                                                                                            |
|-----------------|---------------------|------------------------------------------------------------------------------------------------------------------------|
| inputs          | String[]            | List of texts read from the input files. They are provided by the [input finder script](#find-input-files-dynamically) |
| samples         | String[]            | List of file names of the sample files.                                                                                |
| params          | Dictionary          | Use `name` of the defined parameters as the key to get values in their actual types.                                   |
| resources       | String[]            | List of texts read from the resources files in the same order as declared in your `plugin.json`.                       |
| labeler         | LabelerConf         | Equivalent Json object to [LabelerConf](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) object.     |
| labelerParams   | Dictionary          | Use `name` of the defined parameters in current labeler as the key to get values in their actual types.                |
| debug           | Boolean             | It's set to `true` only when the application is running in the debug environment (Gradle `run` task).                  |
| pluginDirectory | [File](file-api.md) | Directory of this plugin                                                                                               |

#### Find input files dynamically

To support your template plugin with labelers that construct a project with subprojects, you may want to find input
files dynamically for every subproject. To do this, you can provide a script file via the
plugin's `inputFinderScriptFile` property in the `plugin.json`.

The script file should be a JavaScript file which:

- takes a variable `root` of `File` type as input, which indicates the root directory of the project, i.e.
  the `Sample Directory` set in the project creation page.
- takes a variable `moduleName` of `String` type as input, which indicates the name of the subproject.
- takes variables `debug`, `labeler`, `params` as input, which are the same as the ones provided in the template
  generation
  scripts.
- should set a variable `inputFilePaths` of `String[]` type as output, which contains all the absolute paths of the
  input files you want to use.
- may set a variable `encoding` of `String` type as output, which indicates the encoding of the input files. If not set,
  the encoding selected in the project creation page is used.

The `File` type is a JavaScript wrapper of Java's `java.io.File` class. See the [documentation](file-api.md) for
details.

Check the [audacity2lab plugin](../resources/common/plugins/template/audacity2lab) for an example.
In this example, for consistency, even if the project has only one subproject, the input files are still found by the
input finder script, so that we don't have to care about where the input comes from in the main script.

#### Use an input file parameter

If your input file does not belong to a subproject (such as a dictionary file), you can use a parameter of type `file`
or `rawFile` to get the content.
See [Parameter Type](#parameter-types) for details.

#### Output

You have to create a list named `output` to pass the result back to the application.
You can choose to offer `output` in the following two ways:

##### 1. Output the parsed entry object

Make an `output` array with parsed [Entry](../src/jvmMain/resources/js/class_entry.js) objects. e.g.

```javascript
let output = []

for (const line of lines) {
    const entry = parseLineToEntry(line)
    output.push(entry)
}
```

##### 2. Output the raw entry string

If `outputRawEntry` is set to `true`, instead of entry objects, `output` should be set to a list of strings in the
format of the label file. They will be parsed by the labeler's parser later.

#### Tips

1. If `labeler.allowSameNameEntry` is set to `false`, the labeler will discard entries with the same name except the
   first one. Handle the duplicated entry names in your scripts if you want to keep them.
2. The labeler will throw an error if no entry is created. Create a fallback entry in your scripts if you don't want
   users to see the error.
3. If a project is created by labeler's parser with a raw label file, it will include all sample files even if they do
   not appear in the raw label file. In that case, a default entry will be created for each sample file. However, if a
   project is created by a plugin, the sample files that are not referenced in the output will be ignored. So please be
   sure to create entries for all the sample files that you want to include in the project.

#### Examples

Check the following built-in `template` plugins as examples:

- [ust2lab-ja-kana](../resources/common/plugins/template/ust2lab-ja-kana): Use an input ust file to generate sinsy lab
  entries
- [cv-oto-gen](../resources/common/plugins/template/cv-oto-gen): Generate CV oto entries from parameters
- [regex-raw-gen](../resources/common/plugins/template/regex-raw-gen): Use a regular expression to generate raw entry
  lines. Supports all types of labelers.
- [audacity2lab](../resources/common/plugins/template/audacity2lab): Generate lab entries from an audacity label file.
  It also supports the `NNSVS singer labeler` which constructs a project with subprojects.
  See [Find input files dynamically](#find-input-files-dynamically) for details.

### Batch Edit (Macro) Scripts

A plugin with `macro` type is executed by an item in the menu `Tools` -> `Batch Edit`. It is only available when editing
a project.

According to the `scope` property in the `plugin.json`, the plugin can be executed on the whole project or on the
current module (subproject).
If `scope` is set to `Module`, the input includes a list of `Entry` object in the current module named `entries`
along with an integer named `currentEntryIndex`.
If `scope` is set to `Project`, the input includes a list of `Module` object named `modules` along with an integer
named `currentModuleIndex`.

You can modify these variables directly to conduct batch edit on the whole project.

#### Input

The following variables are provided before your scripts are executed.

| name                 | type                | description                                                                                                                                              |
|----------------------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| entries              | Entry[]             | Only available when the plugin's `scope` is `Module`. List of current [Entry](../src/jvmMain/resources/js/class_entry.js) objects in the current module. |
| currentEntryIndex    | Entry[]             | Only available when the plugin's `scope` is `Module`. The index of current shown entry.                                                                  |
| module               | Module              | Only available when the plugin's `scope` is `Module`. The current [Module](../src/jvmMain/resources/js/class_module.js) object.                          |
| modules              | Module[]            | Only available when the plugin's `scope` is `Project`. List of current [Module](../src/jvmMain/resources/js/class_module.js) objects in the project.     |
| currentModuleIndex   | Integer             | Only available when the plugin's `scope` is `Project`. The index of current shown module.                                                                |
| params               | Dictionary          | Use `name` of the defined parameters as the key to get values in their actual types.                                                                     |
| resources            | String[]            | List of texts read from the resources files in the same order as declared in your `plugin.json`.                                                         |
| labeler              | LabelerConf         | Equivalent Json object to [LabelerConf](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) object.                                       |
| labelerParams        | Dictionary          | Use `name` of the defined parameters in current labeler as the key to get values in their actual types.                                                  |
| debug                | Boolean             | It's set to `true` only when the application is running in the debug environment (Gradle `run` task).                                                    |
| pluginDirectory      | [File](file-api.md) | Directory of this plugin                                                                                                                                 |
| projectRootDirectory | [File](file-api.md) | Only available when the plugin's `scope` is `Project`.  Root directory of the project.                                                                   |

#### Use an entry selector

A parameter with `entrySelector` type is passed in `params` as a list of the selected indexes of the `entries` list.

The following code is an example of using an entry selector:

```javascript
let selectedIndexes = params["selector"] // use actual name of your parameter
for (let index of selectedIndexes) {
    let entry = entries[index]
    // edit the entry
}
```

This type of parameter is only available when the `scope` is `Module`. Otherwise, no value is passed.

#### Output

Change the content of the given `entries` or `modules` list to change the project's content.

Note that you can access to `module` if the scope is `Module`, but the changes to it will not be reflected.

The following code is an example of adding a suffix to every entry's name in the current module:

```javascript
let suffix = params["suffix"]

// This is `Module` scope, so `entries` is available
for (let entry of entries) {
    entry.name += suffix
}
```

#### Display a report after execution

See the corresponding section in [Scripting](scripting.md#display-a-report-after-execution) for more details.

#### Request audio playback after execution

See the corresponding section in [Scripting](scripting.md#request-audio-playback-after-execution) for more details.

#### Examples

Check the following built-in `macro` plugins as examples:

- [batch-edit-entry-name](../resources/common/plugins/macro/batch-edit-entry-name): Edit selected entry names for all
  labelers. You can refer to it for the usage of the entry selector
- [batch-edit-oto-parameter](../resources/common/plugins/macro/batch-edit-oto-parameter): Edit parameters of selected
  entries for UTAU oto. The `labeler` is used to get the specific settings about `oto.ini`
- [compare-oto-entries](../resources/common/plugins/macro/compare-oto-entries): Compare an input oto with the current
  project. You can refer to it for the usage of the `report()` function
- [execute-scripts](../resources/common/plugins/macro/execute-scripts): Execute input scripts to edit the project. It
  can be used as a debug tool when developing a plugin.
- [resampler-test](../resources/common/plugins/macro/resampler-test): Test the resampler synthesis of the current entry.
  You can refer to it for the usage of the `requestAudioFilePlayback()` function and `Env`, `File`, `CommandLine` APIs

### Localization

Check [Localized string](localized-string.md) for the localization support in plugin definition and scripts.

### Error handling

See the corresponding section in [Scripting](scripting.md#error-handling) for more details.

### Debugging

You can use logs to help debug your scripts.
The standard output (e.g. `console.log()`) is written to `.logs/info.log` and the error output is written to
`.logs/error.log`.
If the plugin is not shown in the list, there are probably some errors while loading the plugin (i.e.
parsing `plugin.json`).
