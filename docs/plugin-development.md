# Develop Plugins for vLabeler

This guide illustrates the process of developing plugins for `vLabeler`. Should you encounter any inadequacies in the
plugin specification for your needs, please do not hesitate to submit questions or feature requests.

## Overview

`vLabeler` currently supports two types of plugins:

- **Macro plugins**: Execute during the project editing phase. They are typically used for batch editing of entries.
- **Template plugins**: Execute when initiating a new project. Their primary function is to generate a series of entries
  for further editing.

While both plugin types share the same structural design, their execution contexts differ, leading to variations in
inputs and outputs.

This guide will cover:

- [Plugin File Structure](#plugin-file-structure)
- [Plugin Definition](#plugin-definition)
    - [Parameter Definition](#defining-parameters)
- [Scripting Guidelines for Plugins](#scripting-for-plugins)
    - [Creating Template Generation Scripts](#template-generation-scripts)
    - [Creating Batch Edit (macro) Scripts](#batch-edit-macro-scripts)
    - [Miscellaneous](#miscellaneous)

## Plugin File Structure

A `vLabeler` plugin is a folder containing:

1. A `plugin.json` file to specify the plugin's behavior.
2. At least one `*.js` script file.
3. Additional files needed by your scripts.

## Plugin Definition

The `plugin.json` file is structured as a JSON object, containing the following properties:

| Key                         | Type                   | Default value | Supported Plugin Type | Description                                                                                                                                     |
|-----------------------------|------------------------|---------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| name                        | String                 | (Required)    | All                   | This value should match the folder's name.                                                                                                      |
| version                     | Integer                | 1             | All                   | The version of the plugin.                                                                                                                      |
| type                        | String                 | (Required)    | All                   | Specify as either `template` or `macro`.                                                                                                        |
| displayedName               | String (Localized)     | `name` value  | All                   | The displayed name of the plugin.                                                                                                               |
| author                      | String                 | (Required)    | All                   | The author of the plugin.                                                                                                                       |
| email                       | String                 | ""            | All                   | Contact email of the author.                                                                                                                    |
| description                 | String (Localized)     | ""            | All                   | A brief description of the plugin.                                                                                                              |
| website                     | String                 | ""            | All                   | The website or source code repository of the plugin                                                                                             |
| supportedLabelFileExtension | String                 | (Required)    | All                   | Supported extensions for your label file (e.g., `ini` for UTAU oto). Use "*" to accept all types; use "&#124;" to separate multiple extensions. |
| outputRawEntry              | Boolean                | false         | Template              | If set to `true`, outputs the raw entry text rather than a parsed object.                                                                       |
| scope                       | String                 | "Module"      | All                   | Determines the plugin's access range: either `Module` or `Project`.                                                                             |
| parameters                  | Parameters &#124; null | null          | All                   | For details, refer to the `Defining Parameters` section.                                                                                        |
| scriptFiles                 | String[]               | (Required)    | All                   | Names of all script files. These files execute in the order they are listed.                                                                    |
| resourceFiles               | String[]               | []            | All                   | Files utilized as resources in your scripts. Their contents are fed into your scripts as string values in the order listed.                     |
| inputFinderScriptFile       | String &#124; null     | null          | Template              | Name of the script file used to identify input files dynamically.                                                                               |

### Defining Parameters

Within the `parameters` object is an array named `list`:

```json5
{
    // ...,
    "parameters": {
        "list": [
            // ...
        ]
    },
    // ...
}
```

Each object within `list` specifies a parameter that will appear in the plugin configuration dialog and will be passed
to your scripts.

For a comprehensive definition of a parameter, see [Parameter](parameter.md).

## Scripting for Plugins

Below are guidelines on scripting for plugins:

For detailed information on the scripting environment and available APIs, please refer
to [Scripting in vLabeler](scripting.md).

### Template Generation Scripts

Plugins with `template` type operate on the `New Project` page, facilitating the creation of entry lists for subsequent
edits.

#### Input

Before your scripts are executed, the following variables will be set in the JavaScript environment:

| Name            | Type                | Description                                                                                                              |
|-----------------|---------------------|--------------------------------------------------------------------------------------------------------------------------|
| inputs          | String[]            | Texts sourced from input files, facilitated by the [input finder script](#dynamic-input-file-retrieval).                 |
| samples         | String[]            | List of sample file names.                                                                                               |
| params          | Dictionary          | A dictionary containing all parameters defined in `plugin.json`. You can get values using their `name` as the key.       |
| resources       | String[]            | Texts from resource files, in the order they appear in `plugin.json`.                                                    |
| labeler         | LabelerConf         | JSON object mirroring [LabelerConf](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt).                  |
| labelerParams   | Dictionary          | A dictionary containing all parameters defined in the current labeler. You can get values using their `name` as the key. |
| debug           | Boolean             | Whether the execution is in debug mode (during the Gradle `run` task).                                                   |
| pluginDirectory | [File](file-api.md) | The plugin directory.                                                                                                    |

#### Dynamic input file retrieval

For labelers that create projects with subprojects, you can dynamically find input files for every subproject. The
plugin's `inputFinderScriptFile` attribute in `plugin.json` should specify a script for this.

This JavaScript file:

- Receives `root` (type: `File`), representing the project's root directory.
- Receives `moduleName` (type: `String`), the subproject name.
- Accepts variables `debug`, `params`, `labeler`, `labelerParams`, similar to those in the template generation scripts.
- Outputs `inputFilePaths` (type: `String[]`), containing desired input file paths.
- Optionally, outputs `encoding` (type: `String`) to specify input file encoding; otherwise, the chosen encoding during
  project creation is used.

Refer to the [documentation](file-api.md) for the `File` type's specifications and
the [audacity2lab plugin](../resources/common/plugins/template/audacity2lab) as a sample implementation.

#### Request input files from the user

For input files unrelated to a subproject (like user customizable dictionaries), utilize a `file` or `rawFile` parameter
type.
See [Defining a Parameter](parameter.md) for specifics.

#### Output

Return results to the app using a list named `output`. This can be presented in two methods:

##### 1. Directly return parsed entry objects

Construct an `output` array containing parsed [Entry](../src/jvmMain/resources/js/class_entry.js) objects. For example:

```javascript
let output = [];
for (const line of lines) {
    // parse line to get `name`, `sample`, `start`, `end`, etc.
    const entry = new Entry(sample, name, start, end, points, extras);
    output.push(entry);
}
```

##### 2. Return raw entry strings

If `outputRawEntry` is set to `true`, populate `output` with raw entry strings in the label file's format. The labeler's
parser will process these later.

#### Tips

1. When `labeler.allowSameNameEntry` is `false`, only the first entry with a duplicate name is retained. Address this in
   your script if preserving all is necessary.
2. If no entries are created, an error will be raised. As a remedy, include a fallback entry.
3. Projects initiated by a labeler's parser with a raw label file will encompass all sample files, even those absent in
   the raw label. Default entries are generated for these. However, for projects initiated by a plugin, unreferenced
   sample files in the output are ignored. Ensure all desired sample files have corresponding entries.

#### Example Plugins

Explore these integrated `template` plugins for insight:

- [ust2lab-ja-kana](../resources/common/plugins/template/ust2lab-ja-kana): Converts an input UST file to Sinsy lab
  entries.
- [cv-oto-gen](../resources/common/plugins/template/cv-oto-gen): Produces CV oto entries from parameters.
- [regex-raw-gen](../resources/common/plugins/template/regex-raw-gen): Uses regex to craft raw entry lines, supporting
  all labeler types.
- [audacity2lab](../resources/common/plugins/template/audacity2lab): Creates lab entries from an Audacity label file.
  Also compatible with `NNSVS singer labeler`, which constructs projects with subprojects.

### Batch Edit (Macro) Scripts

In the context of vLabeler, batch edit scripts (type: `macro`) are designed to operate over projects, either over the
entire project or a particular subproject/module. The scope of the plugin determines the domain of operation,
with `Module` scope operating on the current module, and `Project` scope covering the entire project.

#### Input

Before your scripts are executed, the following variables will be set in the JavaScript environment:

| Parameter Name       | Type                | Scope   | Description                                                                                                              |
|----------------------|---------------------|---------|--------------------------------------------------------------------------------------------------------------------------|
| entries              | Entry[]             | Module  | A list of the current Entry objects in the active module.                                                                |
| currentEntryIndex    | Entry[]             | Module  | The index of the currently displayed entry.                                                                              |
| module               | Module              | Module  | The current Module object.                                                                                               |
| modules              | Module[]            | Project | All Module objects in the project.                                                                                       |
| currentModuleIndex   | Integer             | Project | The index of the currently displayed module.                                                                             |
| params               | Dictionary          | All     | A dictionary containing all parameters defined in `plugin.json`. You can get values using their `name` as the key.       |
| resources            | String[]            | All     | Texts from resource files, in the order they appear in `plugin.json`.                                                    |
| labeler              | LabelerConf         | All     | JSON object mirroring [LabelerConf](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt).                  |
| labelerParams        | Dictionary          | All     | A dictionary containing all parameters defined in the current labeler. You can get values using their `name` as the key. |
| debug                | Boolean             | All     | Whether the execution is in debug mode (during the Gradle `run` task).                                                   |
| pluginDirectory      | [File](file-api.md) | All     | The plugin directory.                                                                                                    |                                                                   
| projectRootDirectory | [File](file-api.md) | Project | The project's root directory.                                                                                            |

#### Use an Entry Selector

This type of parameter lets the user specify a subset of entries for operation and is accessible under `Module` scope.
Here's a code snippet for its utilization:

```javascript
let selectedIndexes = params["selector"] // `selector` is the name of the entry selector parameter
for (let index of selectedIndexes) {
    let entry = entries[index]
    // do something with the entry
}
```

#### Output

To bring about changes in the project, directly modify the `entries` or `modules` list. Note that for `Module` scope,
while you can access the `module`, changes to it won't be saved. Here's an example to add suffixes to each entry name in
the current module:

```javascript
let suffix = params["suffix"]
for (let entry of entries) {
    entry.name += suffix
}
```

#### Display a report after execution

After execution, you can display reports. Refer to the section
in [Scripting in vLabeler](scripting.md#display-a-report-after-execution) for more details.

#### Request audio playback after execution

After execution, you can request an audio playback. Refer to the section
in [Scripting in vLabeler](scripting.md#request-audio-playback-after-execution) for more details.

#### Example Plugins

Explore these integrated `macro` plugins for insight:

- [batch-edit-entry-name](../resources/common/plugins/macro/batch-edit-entry-name): Modify names of selected entries
  across all labelers. It demonstrates the use of the entry selector.
- [batch-edit-oto-parameter](../resources/common/plugins/macro/batch-edit-oto-parameter): Edit parameters of chosen UTAU
  oto entries. It demonstrates the use of the `labeler` variable.
- [compare-oto-entries](../resources/common/plugins/macro/compare-oto-entries): Compares an input oto with the ongoing
  project. The function `report()` is used here.
- [execute-scripts](../resources/common/plugins/macro/execute-scripts): Run input scripts to edit the project. It can be
  used as a debugging tool during plugin development.
- [resampler-test](https://github.com/sdercolin/vlabeler-resampler-test): Examine the resampler synthesis of the active
  entry. Demonstrates the use of `requestAudioFilePlayback()`, along with `Env`, `File`, and `CommandLine` APIs.

## Miscellaneous

### Localization

Check [Localized strings in vLabeler](localized-string.md) about the `String (Localized)` type mentioned above.

### Error handling

For in-depth understanding and strategies to handle errors, refer to the section
in [Scripting in vLabeler](scripting.md#error-handling).

### Debugging

You can use logs to help debug your scripts.
The standard output (e.g. `console.log()`) is written to `.logs/info.log` and the error output is written to
`.logs/error.log`.

If your plugin doesn't appear in the list, it might have faced issues during loading,
such as problems parsing `plugin.json`. Check the error log for more information.
