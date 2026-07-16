---
sidebar_position: 2
title: 插件
---

# 插件

目前，您可以使用以下两种类型的插件。

## 模板生成器

模板生成器可以通过输入的文件和参数来生成项目模板。

虽然标注器本身支持生成项目模板，但这样生成的模板只是满足了标注器的最低要求，不方便实际使用。
而模板生成器插件则可以创建复杂的模板。它们通常还支持一些可定制的参数。

您可以在 `新建项目` 页面上选择所有支持所选标注器的模板生成器。

您可以在 `设置` -> `模板生成器...` 中导入/删除/禁用模板生成器。

## 批量编辑插件

批量编辑插件可用于执行复杂的批量编辑任务。

您可以在 `工具` -> `批量编辑` 中找到可用的批量编辑插件。某些显示的插件可能无法点击，这代表它不支持当前使用的标注器。

您可以在 `工具` -> `批量编辑` -> `管理插件...` 中导入/删除/禁用批量编辑插件。

## 可用的插件（除内建）

- [resampler-test](https://github.com/sdercolin/vlabeler-resampler-test)：播放当前条目的 UTAU resampler 输出。与 setParam
  的合成测试（F10）功能相似。
- [oto-timing-test](https://github.com/chexq09/vlabeler-oto-timing-check)：将当前条目的音频与节拍器结合输出，用于检查先行发声的位置。与
  setParam 的发声位置检查（F8）功能相似。

## 插件开发

我们欢迎您为 vLabeler 开发插件。
您可以在任何地方分发您的插件，或者创建一个 Pull Request 来将其内置在 vLabeler 中。

详情请见 [Develop Plugins for vLabeler](https://github.com/sdercolin/vlabeler/blob/main/docs/plugin-development.md)（英文）。
