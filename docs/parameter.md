# Defining a parameter

In this article, we will introduce how to define a parameter in labelers and plugins.

## Overview

In vLabeler, a parameter is a setting that can be configured by the user, used in labelers and plugins.

Labelers and plugins use the same parameter types, so the following content applies to both.

## Structure

A parameter is defined as a JSON object, which has the following common fields:

| Property     | Type                           | Default value | Description                                                                            |
|--------------|--------------------------------|---------------|----------------------------------------------------------------------------------------|
| type         | String                         | (Required)    | Can be `integer`, `float`, `boolean`, `string`, `enum`, etc.                           |
| name         | String                         | (Required)    | Unique parameter name for reference in your scripts.                                   |
| label        | String (Localized)             | (Required)    | Parameter name shown in the configuration dialog.                                      |
| description  | String (Localized) &#124; null | null          | Description shown next to the label.                                                   |
| enableIf     | String &#124; null             | null          | If set, this parameter is enabled only when the parameter with the set name is truthy. |
| defaultValue | (Depends on `type`)            | (Required)    | The default value of this parameter.                                                   |

Note that the `label` and `description` fields can be [localized strings](localized-string.md).

## Parameter Types

### `integer`

An integer number. An input box will be shown in the configuration dialog for this parameter.

If the input value is not an integer, it will not be accepted.

Besides the common fields, the `integer` type can have the following fields:

- `min`: If defined, a value smaller than `min` is not accepted.
- `max`: If defined, a value greater than `max` is not accepted.

### `float`

A floating-point number. Basically the same as `integer`, but accepts floating-point numbers.

This type has the same fields as `integer`.

### `boolean`

A boolean value. A switch will be shown in the configuration dialog for this parameter.

### `string`

A string value. An input box will be shown in the configuration dialog for this parameter.

Besides the common fields, the `string` type can have the following fields:

- `multiLine`: If set to `true`, a multi-line input box will be shown in the configuration dialog.
  Otherwise, the input box will be single-line, which does not allow line breaks. Defaults to `false`.
- `optional`: If set to `true`, the input box can receive an empty string. Defaults to `false`.

Specially, if used in a plugin, you can set the `defaultValue` to `file::path/to/file` to read a file's content as the
default value. The file path is relative to the plugin's root directory. The encoding of the file is required to be
`UTF-8`.

### `enum`

A string value that can only be one of the specified values.
A drop-down list will be shown in the configuration dialog for this parameter.

Besides the common fields, the `enum` type can have the following fields:

- `options`: A list of strings that are the possible values of this parameter. (Required)
- `optionDisplayedNames`: A list of localized strings that are the displayed names of the options. If it is not set,
  the options will be displayed as their values. Make sure that the number of items in this list is the same as the
  number of items in the `options` list.

Here is an example of an `enum` parameter:

```
{
    ...,
    "defaultValue": "option1",
    "options": [
        "option1",
        "option2",
        "option3"
    ]
    "optionDisplayedNames": [
        { en: "Option 1", zh: "选项1" },
        { en: "Option 2", zh: "选项2" },
        { en: "Option 3", zh: "选项3" }
    ],
    ...
}
```

When you receive the value of this parameter in your scripts, it will be one of the strings in the `options` list.

### `entrySelector`

**This type is only available in macro plugins.**

This parameter contains a list of filters that can be used to select entries from the current subproject.

An editable list of filters will be shown in the configuration dialog for this parameter.

No additional fields are available for this type.

Basically, we recommend you to set the default value of this parameter to the following object:

```json
{
  "filters": []
}
```

If you want to change the default filters, please
check [EntrySelector](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/EntrySelector.kt) for the structure of the
`filters` field.

Here is an example of an `entrySelector` parameter:

```
{
    ...,
    "defaultValue": {
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
    },
    ...
}
```

See [Use an entry selector](plugin-development.md#use-an-entry-selector) for its usage in scripts.

### `file`

An object of the following structure:

```
{
  "file": "path/to/file", // can be null or omitted if `optional` is true
  "encoding": "UTF-8" // can be null or omitted
}
```

In the configuration dialog, a path input box and an encoding drop-down list will be shown for this parameter.

Besides the common fields, the `file` type can have the following fields:

- `optional`: If set to `true`, the `file` field can be null or omitted, and the input box can receive an empty string.
  Defaults to `false`.
- `acceptExtensions`: A list of strings that are the accepted file extensions. If it is not set, all files are accepted.
  e.g. `["txt", "json"]`. Note that the leading dot is not required. Defaults to `null`.

In your scripts, a string that is loaded from the file with the specified encoding will be passed as the parameter
value. If the path is `null` when `optional` is `true`, the parameter value will be passed as `null`.

Specially, if used in a plugin, you can set the `file` field of the `defaultValue` to a path relative to the plugin's
root directory. It will be resolved to an absolute path when the plugin is loaded. For example, if you set
the `defaultValue` as follows, it will be resolved to `path/to/plugin/dictionary.txt`:

```
{
    ...,
    "defaultValue": {
        "file": "dictionary.txt"
    },
    ...
}
```

You can also set the default value to an empty object `{}`. In this case, if `optional` is `false`, the parameter value
will be regarded as invalid and the user will be asked to provide a file before running the plugin. If it's used in a
labeler, we recommend you to set `optional` to `true` to avoid this situation.

### `rawFile`

A string value that is the path to a file.
In the configuration dialog, a path input box will be shown for this parameter.

Basically it works the same as `file`, but instead of the file content, the path itself is passed as the parameter in
the string type.

Besides the common fields and fields of `file` type, the `rawFile` type uses an `isFolder` field, which is a boolean
value that indicates whether the path is a folder. If it is set to `true`, the file selector launched by the input box
will only allow the user to select a folder. Defaults to `false`.
