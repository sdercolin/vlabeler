---
sidebar_position: 1
title: Labelers
---

# Labelers

A "labeler" is a configuration file for `vLabeler` which defines the behavior of a certain type of voice generation
software.
For example, the built-in `UTAU oto labeler` is a labeler for editing UTAU's `oto.ini` files.

A labeler defines:

- what data/fields a voice entry should contain (e.g. for UTAU, you need `fixed`, `preutterance`, `overlap`, etc.)
- how the data/fields are displayed in the editor as parameter controllers
- whether entries should be connected (every entry's start should be the same as the previous entry's end)
- how to parse a label file to a `vLabeler` project
- how to generate a label file from a `vLabeler` project
- how to build subprojects under a `vLabeler` project
- and more behaviors when editing the certain type of label files

A labeler may also support some configurable fields via GUI, without changing the labeler's file itself.
You can find the settings in the `Settings` icon next to the labeler selector in the `New Project` page.

See [Choosing a labeler](../getting-started/choosing-a-labeler.md) for the list of built-in labelers.

## Custom labelers

If you want to edit the labels for a voice generation software that is not supported by `vLabeler`,
instead of requesting development supporting that software, you can create a labeler to make it work. (it requires
some knowledge of the coding though).
Please check
[Develop Custom Labelers](https://github.com/sdercolin/vlabeler/blob/main/docs/labeler-development.md) for details
about labeler development.

You can import your own labelers in `Settings` -> `Labelers...`.
You can also distribute the labelers created by yourself to other users, or contact us for making them built-in.
