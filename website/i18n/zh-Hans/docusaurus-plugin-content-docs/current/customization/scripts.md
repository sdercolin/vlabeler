---
sidebar_position: 3
title: 使用自定义脚本
---

# 使用自定义脚本

vLabeler 允许您使用自定义脚本来自动化标注任务。
vLabeler 内置了以下两个插件，您可以从 `工具` -> `批量编辑` 菜单中运行它们：

## 执行脚本

此插件允许您访问当前的子项目。

您可以访问以下属性：

- `entries`：当前子项目中的条目列表。
  详情请参阅 [Entry](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_entry.js)。
- `currentEntryIndex`：当前条目在条目列表中的索引。
- `module`：当前子项目的模块（module）对象。此属性为只读，即对该对象的修改不会被保存。
  详情请参阅 [Module](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_module.js)。

## 执行脚本（多子项目）

此插件允许您访问所有子项目。

您可以访问以下属性：

- `modules`：项目中的模块列表。
  详情请参阅 [Module](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_module.js)。
- `currentModuleIndex`：当前模块在模块列表中的索引。

## 如何编写脚本

您可以使用 ES12 语法的 JavaScript 编写脚本。
更多详情请参阅 [Scripting in vLabeler](https://github.com/sdercolin/vlabeler/blob/main/docs/scripting.md)（英文）。
