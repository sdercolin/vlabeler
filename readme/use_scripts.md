# Use custom scripts in vLabeler to complete your labeling tasks

vLabeler allows you to use custom scripts to automate your labeling tasks.
There are two plugins provided built-in, which you can run from the `Tools` -> `Batch Edit` menu:

### Execute Scripts

This plugin give you access to the current sub-project.

You have access to the following properties:

- `entries`: The list of entries in the current sub-project. See [Entry](../src/jvmMain/resources/js/class_entry.js) for
  details.
- `currentEntryIndex`: The index of the current entry in the list of entries.
- `module`: The module object of the current sub-project. This property is read-only, which means the modification to
  this object will not be saved. See [Module](../src/jvmMain/resources/js/class_module.js) for details.

### Execute Scripts (Multi-subproject)

This plugin give you access to all sub-projects.

You have access to the following properties:

- `modules`: The list of modules in the project. See [Module](../src/jvmMain/resources/js/class_module.js) for details.
- `currentModuleIndex`: The index of the current module in the list of modules.

### How to write scripts

You can write scripts in JavaScript with ES12 syntax. See [Scripting in vLabeler](../docs/scripting.md) for more
details.
