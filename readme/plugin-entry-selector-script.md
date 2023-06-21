# About `Expression` type of entry selector in plugin dialogs

*This article is for advanced users. It might require some basic programming knowledge.*

In a `entrySelector` type of parameter in plugin dialogs, you can use `Expression` type of entry selector to select
entries by expression in JavaScript.

## Syntax

The required input is an expression in JavaScript, which should be evaluated to a boolean value.
e.g. `entry.notes.tag === "CV"` or `entry.left > 100`

## Available variables

### Entry

The entry object: `entry`.

This object is basically the same as the [basic entry definition](/src/jvmMain/resources/js/class_entry.js). But we have
added all the properties defined in the labeler to the object. You can check the labeler configuration file (*
.labeler.json) and see the `properties` field to get their `name`s.

For example, in the default UTAU singer/oto labelers, the following fields can be accessed:

- `entry.left` - the "Offset" value
- `entry.right` - the "Cutoff" value
- `entry.preu` - the "Preutterance" value
- `entry.ovl` - the "Overlap" value
- `entry.fixed` - the "Fixed" value

Note these field names might be different with the options that you see in the plugin dialog. For example, the `left` is
the same as the `Offset` option provided by the
built-in [Batch edit oto parameter](/resources/common/plugins/macro/batch-edit-oto-parameter) plugin, because `Offset`
is the `displayName` of the property defined in the labeler, while `left` is the `name` of the property.

Please do not use `entry.start` and `entry.end` because they are not the same as `entry.left` and `entry.right` which
are visible to users.

### Labeler Configuration

The labeler configuration object: `labeler`.

This object is basically the same as the json object loaded from the labeler configuration file (* .labeler.json).
You can check the [LabelerConf](/src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) class for more
documentations.
In most cases, you don't need to use it.

## Tips

- Do not use `var` or `let` to define variables, because the script is run for every entry in the same execution
  environment. This will cause an `Identifier 'xxx' has already been declared` error. Instead, just write `aaa = 2` for
  intermediate variables.
- You can use `;` to separate multiple expressions. The last expression will be used as the result.
  e.g.
  ```javascript
  tag = entry.notes.tag; tag === "CV"
  ```
