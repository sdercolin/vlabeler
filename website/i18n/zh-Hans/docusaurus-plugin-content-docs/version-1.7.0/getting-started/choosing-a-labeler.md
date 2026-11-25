---
sidebar_position: 2
title: 选择标注器
---

# 选择标注器

vLabeler 的许多行为都依赖于可定制的 `标注器`。标注器定义了 vLabeler 如何读取、显示和写入某种类型的标注文件。
在创建项目时，您首先需要选择与您的目标软件相匹配的标注器。
关于标注器这一概念的更多详情，请参阅 [标注器](../customization/labelers.md)。

目前，vLabeler 提供了以下内建标注器：

## 处理 UTAU 的原音设定文件

![UTAU singer 标注器](/img/utau-singer.gif)

适用于 UTAU 原音设定的内建标注器有以下两项：

- **UTAU oto 标注器**

  如果您只想编辑一个 `oto.ini` 文件，请使用此标注器。

- **UTAU singer 标注器**

  如果您想编辑一个歌手声库下的多个 `oto.ini` 文件，请使用此标注器。
  请在创建项目时将 `采样目录` 设置为歌手的根文件夹（通常包含一个 `character.txt` 文件）。

## 处理音频分割标注文件 \{#working-on-audio-labels}

![NNSVS singer 标注器](/img/nnsvs-singer.gif)

对于 NNSVS/ENUNU 和类似语音合成系统中使用的标注文件，您可以使用以下选项：

- **Sinsy lab 标注器**

  此标注器适用于 Sinsy (NNSVS/ENUNU) 格式的 lab 文件，它使用 `100ns` 作为时间单位，使用 ` `（空格）作为分隔符。
  每个项目只接受一个标注文件。wav 文件通过文件名绑定到标注文件，例如，
  如果输入标注文件是 `foo.lab`，则将从 `采样目录` 中选择名为 `foo.wav` 的 wav 文件。

- **Audacity 标注器**

  此标注器适用于 Audacity 创建的 `Labels` 文件，它使用 `s` 作为时间单位，并使用 `\t` (tab) 作为分隔符。
  每个项目只接受一个标注文件。wav 文件通过文件名绑定到标注文件，例如，
  如果输入标注文件是 `foo.txt`，则会从 `采样目录` 中选择名为 `foo.wav` 的 wav 文件。

- **NNSVS singer 标注器**

  此标注器与 `Sinsy lab 标注器` 基本相同，但可以处理多个标注文件。
  通常，如果您的文件结构如下：

  ```
  - singer
      - wav
        - 1.wav
        - 2.wav
      - lab
        - 1.lab
        - 2.lab
  ```

  您可以通过将 `采样目录` 设置为 `singer` 文件夹来创建包含所有 lab 文件的项目。
  您可以在标注器的设置中更改 `wav` 和 `lab` 文件夹名称。

## 其他标注器（非内建）

- TextGrid 标注器（适用于 Praat TextGrid 文件）：[GitHub](https://github.com/sdercolin/vlabeler-textgrid)
