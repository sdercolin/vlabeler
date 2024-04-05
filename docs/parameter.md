# Defining a Parameter

In this guide, we'll explore the process of defining parameters in labelers and plugins.

## Overview

Within vLabeler, parameters are user-configurable settings deployed in labelers and plugins.

Both labelers and plugins utilize the same types of parameters. Hence, the subsequent explanations apply equally to
both.

## Structure

Each parameter is framed as a JSON object encompassing the subsequent standard fields:

| Key          | Type                           | Default value | Description                                                                                                                                        |
|--------------|--------------------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| type         | String                         | (Required)    | Types: `integer`, `float`, `boolean`, `string`, `enum`, etc.                                                                                       |
| name         | String                         | (Required)    | A distinct parameter name for referencing within your scripts.                                                                                     |
| label        | String (Localized)             | (Required)    | The display name for the parameter in the configuration dialog.                                                                                    |
| description  | String (Localized) &#124; null | null          | A brief note displayed adjacent to the label.                                                                                                      |
| enableIf     | String &#124; null             | null          | Activates this parameter only if the specified condition is met. See [Configure Enabled Condition](#configure-enabled-condition) for more details. |
| defaultValue | (Dependent on `type`)          | (Required)    | The parameter's default value.                                                                                                                     |

Note that the `label` and `description` fields can be [Localized strings in vLabeler](localized-string.md).

## Parameter Types

### `integer`

Represents an integer. The configuration dialog will feature an input box for this parameter. Non-integer inputs will
not be accepted.

In addition to standard fields, the `integer` type may include:

- `min`: The minimum permissible value. (Optional)
- `max`: The maximum permissible value. (Optional)

### `float`

Represents a floating-point number. It functions similarly to `integer`, but allows decimal values. This type
has the same fields as `integer`.

### `boolean`

Represents a boolean value. This parameter will be presented as a toggle switch in the configuration dialog.

### `string`

Represents textual data. The configuration dialog displays an input box for this parameter.

Additional fields for the `string` type:

- `multiLine`: Enables multiline input if set to `true`. Defaults to `false`.
- `optional`: Allows an empty string input if set to `true`. Defaults to `false`.

The `defaultValue` can point to a file, using the format `file::path/to/file`, to use its
content as the default. You can include some files in the directory of your plugins/labelers, and refer to them by
relative paths. These files should always be encoded in `UTF-8`.

### `enum`

A pre-defined list of string values. The configuration dialog displays these as a drop-down list.

The `enum` type can feature:

- `options`: A mandatory list of valid values.
- `optionDisplayedNames`: Display names for the options. If unspecified, values from the `options` list are used
  directly.
  w
  For instance, an `enum` parameter might look like:

```json5
{
    // ...,
    "defaultValue": "option1",
    "options": [
        "option1",
        "option2",
        "option3"
    ],
    "optionDisplayedNames": [
        {
            "en": "Option 1",
            "zh": "选项1"
        },
        {
            "en": "Option 2",
            "zh": "选项2"
        },
        {
            "en": "Option 3",
            "zh": "选项3"
        },
        // ...
    ],
    // ...
}
```

### `entrySelector`

**This type is specific to macro plugins.**

This parameter comprises a set of filters to select entries from the ongoing subproject. The configuration dialog will
present an editable filter list.

For the `entrySelector` type, the default value configuration should ideally be:

```json
{
    "filters": []
}
```

For a more detailed structure of the `filters` field, refer
to [EntrySelector](../src/jvmMain/kotlin/com/sdercolin/vlabeler/model/EntrySelector.kt).

A sample `entrySelector` parameter is provided below:

```json5
{
    // ...,
    "defaultValue": {
        "filters": [
            {
                "type": "text",
                // an example of a `text` filter
                "subject": "name",
                // `name` for entry name or `sample` for sample name
                "matchType": "Contains",
                // `Contains` | `Equals` | `StartsWith` | `EndsWith` | `Regex`
                "matcherText": "foo"
            },
            {
                "type": "number",
                // an example of a `number` filter
                "subject": "overlap",
                // `name` of any property defined in the labeler
                "matchType": "GreaterThan",
                // `Equals` | `GreaterThan`| `LessThan` | `GreaterThanOrEquals` | `LessThanOrEquals`
                "comparerValue": 0.5,
                // only used when `comparerName` is null
                "comparerName": "offset"
                // nullable, `name` of any property defined in the labeler
            }
        ],
        "expression": "#1 or #2"
        // optional, default to null, which combines all filters with `and` operator
    },
    // ...
}
```

Please refer to [Using an Entry Selector](plugin-development.md#use-an-entry-selector) for further details on
using it within scripts.

### `file`

This type represents a file path along with its encoding. For this parameter, the configuration dialog will display a
path input field as well as an encoding selection dropdown.

For the `file` type, additional fields are:

- `optional`: When set to `true`, the `file` field can be left empty. Defaults to `false`.
- `acceptExtensions`: A list of accepted file extensions, such as `["txt", "json"]`. If it is not set, all files are
  accepted.

Within your scripts, the content of the specified file is passed as the parameter value. If the `optional` is `true` and
the path is unspecified, the parameter value will be `null`.

For plugins, you can set the `file` field of the `defaultValue` to be relative to the plugin's root directory. For
instance:

```json5
{
    // ...,
    "defaultValue": {
        "file": "dictionary.txt"
        // under the root directory of the labeler/plugin
    },
    // ...
}
```

If the default value is set as an empty object `{}`, and `optional` is `false`, the user will be prompted to provide a
file before script execution.

### `rawFile`

This type indicates a file's path. In contrast to the `file` type, the actual path is passed to the scripts instead of
the file's content.

In addition to standard fields and the `file` type fields, `rawFile` has the `isFolder` field. When set to `true`,
only folder selection will be allowed through the input box.

## Configure Enabled Condition

The `enableIf` field allows you to conditionally enable a parameter based on the value of another parameter. Any of the
following formats are accepted:

- `<paramName>`:
  In this case, the parameter will be enabled if the specified parameter named `paramName` is equal to
  JavaScript `true`.
- `<paramName>=<value1>|<value2>|...`:
  In this case, the parameter will be enabled if the specified parameter named `paramName` is equal to any of the
  specified values.

### Examples

The following `enableIf` field will enable the parameter if the `param1` parameter is set to `true`.

```json5
{
    // ...,
    "enableIf": "param1"
    // ...
}
```

The following `enableIf` field will enable the parameter if the `param1` parameter is set to either `a` or `b`.

```json5
{
    // ...,
    "enableIf": "param1=a|b"
    // ...
}
```
