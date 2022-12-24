# vLabeler

[![Discord](https://img.shields.io/discord/984044285584359444?style=for-the-badge&label=discord&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/yrTqG2SrRd)

Select Language: [English](README.md) | [简体中文](readme/README-zhCN.md) | [日本語](readme/README-ja.md)

`vLabeler` is an open-sourced voice labeling application, aiming:

- Modern and fluent UI/UX
- Customizable labeling process, to be used by different types of voice generation software
- High performance with multiplatform support

**The project is now in Beta.
For help, suggestions, issue reports, etc. please join our [Discord](https://discord.gg/yrTqG2SrRd).**

Demo video (in English): [YouTube](https://youtu.be/xFX8SRrJEzM)
| [bilibili](https://www.bilibili.com/video/BV1Ve4y1S7FF)

## Download

See [Releases](https://github.com/sdercolin/vlabeler/releases).

Packaged application for the following platforms are provided in the releases.

- Windows (.zip/.exe)
- Mac (Apple Silicon or Intel) (.dmg)
- Ubuntu (.deb)

For other types of Linux os, you may have to build it by yourself.

## Building

vLabeler is built with [Compose Multiplatform](https://github.com/JetBrains/compose-jb). You can use Gradle to build the
application. [See more](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

Currently, cross-platform building is not supported. Only packages for your OS are built.

Please ensure you have **JDK 15+** for building.

```
// Package by an installer
./gradlew packageDistributionForCurrentOS

// Or, build an executable app
./gradlew createDistributable
```

## Scenarios

Many behaviors of vLabeler depend on customizable `labeler`s.

Currently, the followings are provided as built-in labelers:

#### Working on UTAU oto.ini

![](readme/utau-singer.gif)

For UTAU oto editing, there are two items in the built-in labeler list:

- UTAU oto labeler

  If you only want to edit one `oto.ini` file, please use this labeler.

- UTAU singer labeler

  If you want to edit multiple `oto.ini` files under a singer voicebank, please use this labeler.
  Please set the singer's root folder (which usually contains a `character.txt` file) as the `Sample Directory` when you
  create the project.

#### Working on audio labels (Continuous mode)

![](readme/nnsvs-singer.gif)

For label files used in NNSVS/ENUNU and similar systems, the following options are available now:

- Sinsy lab labeler

  This labeler is for Sinsy (NNSVS/ENUNU) lab files, which uses `100ns` as the time unit and use ` ` (space) as the
  separator.
  It only accepts one label file per project. A wav file is bound to the label file by the file name, e.g.,
  if the input label file is `foo.lab`, the wav file named `foo.wav` will be selected from the `Sample Directory`.

- Audacity labeler

  This labeler is for `Labels` file created by Audacity, which uses `s` as the time unit and use `\t` (tab) as the
  separator.
  It only accepts one label file per project. A wav file is bound to the label file by the file name, e.g.,
  if the input label file is `foo.txt`, the wav file named `foo.wav` will be selected from the `Sample Directory`.

- NNSVS singer labeler

  This labeler is basically the same with the `Sinsy lab labeler`, but it can handle multiple label files.
  Typically, if you have the following file structures:
  ```
  - singer
      - wav
        - 1.wav
        - 2.wav
      - lab
        - 1.lab
        - 2.lab
  ```
  you can create a project containing all the label files by setting `Sample Directory` to the `singer` folder.
  You can change the `wav` and `lab` folder names in the labeler's settings.

#### Other labelers (not built-in)

- TextGrid labeler (for Praat TextGrid files): [GitHub](https://github.com/sdercolin/vlabeler-textgrid)

## Get started

1. Click `New project...`
2. Select a folder containing your sample files as `Sample Directory`
3. Change `Project location` and `Project name` if you would like
4. Change `Cache directory` if you would like to save the cache files (rendered images, processed wav files, etc.)
   somewhere else
5. Select a labeler, see [Scenarios](#scenarios) for which labeler to use
6. Select a template generator if you don't have an existing label file
7. If you want to edit an existing label file, choose an input file and its encoding
8. Check the settings of the labeler and the template generator you selected
9. Click `OK` and start editing
10. Click `Export` in the menu to get the edited label file

## Available keyboard/mouse actions

Note that the following `Ctrl` is mapped to `Command` if you are using macOS.

You can customize the key bindings in `Settings` -> `Prefereneces` -> `Keymaps`.

### Move parameter lines

- Mouse drag on normal parameters: move itself
- Mouse drag on primary parameters: move all the lines together
- `Shift`: invert normal/primary. e.g. mouse drag on primary parameters with Shift pressed only moves itself.
- `Q`/`W`/`E`/`R`/... : move the corresponding parameter line to current cursor position. The order is defined in the
  labeler. Note that this feature is only available in the single entry editing mode.

### Audio playback

- `Space`: play the current entry, or stop playing if already playing
- `Shift` + `Space`: play the current sample file or stop playing if already playing
- Mouse right click: play the clicked section
- `Alt` + mouse drag on parameters: play the audio near the cursor's position while moving

### Scrolling

- `Shift` + mouse wheel scroll: horizontal scroll
- `F` or Focus button in the center of the bottom bar: Scroll to center the current entry on the screen

### Zoom in/out

- `=` or `+` button in the bottom bar: zoom in
- `-` or `-` button in the bottom bar: zoom out
- `Ctrl` + `Shift` + mouse wheel scroll: zoom in/out
- Resolution button in the bottom bar: open dialog for resolution input

### Switch entry/sample

- Mouse wheel scroll: go to previous/next entry
- `Up`/`Down`: go to previous/next entry
- `<`/`>` buttons in the bottom bar: go to previous/next entry
- `Ctrl` + mouse wheel scroll: go to previous/next sample
- `Ctrl` + `Up`/`Down`: go to previous/next sample
- `<<`/`>>` buttons in the bottom bar: go to previous/next sample
- `Ctrl` + `G` or entry number button in the bottom bar: show `Go to entry...` dialog

### Set notes for entry

- `J` or `Add tag` button in the entry title bar: Start editing the entry's tag
- `K` or `Star` button in the entry title bar: Toggle the entry's `Starred` status
- `L` or `Done` button in the entry title bar: Toggle the entry's `Done` status

#### Settings for notes

You can change settings in `Settings` -> `Prefereneces` -> `Editor` -> `Notes` to hide the items that
you don't need.

The `Done` status is by default automatically set when you edit any values in a entry. You can disable this behavior in
the settings too.

### Quickly launch batch edit plugins

- `F1`~`F8`: launch the corresponding batch edit plugin. Need to be configured in
  `Tools` -> `Batch Edit` -> `Slot Settings...` before use. About plugins, see [Plugins](#plugins) for more details.

## Multi-entry editing mode

For labelers in [continuous mode](#working-on-audio-labels-continuous-mode), you can switch between the
single entry editing mode and a multi-entry editing mode which shows and allows you to edit all the connected entries in
the same sample file.

It's enabled by default. You can click the `Single/Mutiple` button in the center of the bottom bar to enable or disable
it.

In the multi-entry editing mode, names of the entries are displayed at the top of the editor.
By clicking the names, the following actions are conducted:

- Click: Rename the entry
- Long click: Go to the entry

## Video integration

You can attach a video to the sample file with the same name and duration, and watch the video while you're editing.
For example:

```
(sample folder)
    - 1.wav
    - 1.mp4
    - 2.wav
    - 2.webm
```

**You need to install VLC to use this feature.** You can download it
from [here](https://www.videolan.org/vlc/index.html).

Note that even on an Apple Silicon mac, you will need a `macOS` version instead of a `macOS (AppleSilicon)` version if
you are using the application downloaded from `Release` page of this repository.

Use shortcut `V` or `Shift` + `V` to open the attached video in the left-bottom corner or in a new window.
The video will be played silently along with the sample file.

## Browse and filter entries

There are two components showing an entry list:

1. The pinned entry list: shown on the right side of the editor by default. You can toggle it by via menu `View`
   -> `Pin Entry List`. It provides a full powered entry filter (described below).
2. The `Go to entry...` dialog: shown when you press `Ctrl` + `G` or click the entry number button in the bottom bar.
   It provides a simple entry filter.

### Advanced search

In both types of entry list, you can use the following syntax to search entries:

```
aaa;name:bbb;sample:ccc;tag:ddd
```

Multiple conditions can be combined with `;`. Only entries that match all the conditions are shown.

The following condition keys (the part left to `:`) are supported:

- no key: search in the entry name, sample name (without extension) or tag
- `name`: search in the entry name
- `sample`: search in the sample name (without extension)
- `tag`: search in the tag

The match type for this search is `contains`.

### Expanded filter

In the pinned entry list, you can click the `Expand` button to show more options.

For the `Done` filter and `Star` filter, clicking will switch them between `Do not filter`, `Show only starred/done`
and `Show only unstarred/undone`.

These filters are combined with the search text with `AND` logic.

### Linking filter to editor

By default, project navigation (go to next sample/entry etc. by keyboard shortcuts, mouse wheel scroll or button click)
is not affected by the filter.

For example, even if you have filtered out entry `no.5` in the entry list, you can still press `Down` key to go to the
entry `no.5` from entry `no.4`.

The `Link` button in the expanded filter can help you concentrate on a specific set of entries. When it's toggled, the
entries that are not shown in the pinned entry list will be skipped when navigating.

In the above example, if you have toggled the `Link` button, you will go to entry `no.6` when pressing `Down` key
instead of`no.5`.

Note that the `Go to entry...` dialog is not affected by the linked filter, so you can still go to any entry by it
without changing or clearing your filter settings in the pinned entry list.

## Tools

The following editing tool is provided.
You can use shortcuts or menu items under `Edit` -> `Tools` to switch tools, or toggle the toolbox by menu `View`
-> `Show Toolbox`.

Note that you can use the `Play the clicked section` feature by `Right click` with any tool.

### Cursor

The normal cursor tool to drag parameter controllers.

### Scissors

Cut the current entry into two parts by your click position.
By default, when you click on a valid position with the scissors:

1. Audio of the former (left) part after cutting is played so that you can confirm the phoneme
2. A dialog is shown, asking you to rename the former entry
3. Cutting and renaming are conducted
4. The editor goes to the former entry

These actions can be customized in `Prefereneces` -> `Editor` -> `Scissors`.

### Pan

Or the hand tool. Drag on the editor to scroll the canvas.

## Labelers

A "labeler" is a configuration file for `vLabeler` which defines the behavior of a certain type of voice generation
software.
For example, the built-in `UTAU oto labeler` is a labeler for editing UTAU's `oto.ini` files.

A labeler defines:

- what data/fields a voice entry should contain (e.g. for UTAU, you need `fixed`, `preutterance`, `overlap`, etc.)
- how the data/fields are displayed in the editor as parameter controllers
- whether entries should be connected (every entry's start should be the same as the previous entry's end)
- how to parse a label file to a `vLabeler` project
- how to generate a label file from a `vLabeler` project
- how to build sub-projects under a `vLabeler` project
- and more behaviors when editing the certain type of label files

A labeler may also support some configurable fields via GUI, without changing the labeler's file itself.
You can find the settings in the `Settings` icon next to the labeler selector in the `New Project` page.

If you want to edit the labels for a voice generation software that is not supported by `vLabeler`,
instead of requesting development supporting that software, you can create a labeler to make it work. (it requires
some knowledge of the coding though).
Please check [LabelerConf.kt](src/jvmMain/kotlin/com/sdercolin/vlabeler/model/LabelerConf.kt) to understand how to
develop a labeler.

You can import your own labelers in `Settings` -> `Labelers...`.
You can also distribute the labelers created by yourself to other users, or contact us for making them built-in.

## Plugins

Currently, two types of plugins are available.

### Template generators

A template generator can help generate project templates from input files and parameters.

Compared to a default template generated by the labeler which is literally minimum to include all the sample files
given, a template generator plugin can create complicated templates. It's also more customizable with user input
parameters.

All valid template generators which support the selected labeler can be chosen on the `New Project` page.

You can import/delete/disable template generators in `Settings` -> `Template Generators...`.

### Batch edit plugins

Batch edit plugins can be used to conduct complicated batch editing tasks.

You can find available batch edit plugins in `Tools` -> `Batch Edit`. Some displayed plugins may not be clickable, if
it is not supported by the current labeler.

You can import/delete/disable batch edit plugins in `Tools` -> `Batch Edit` -> `Manage plugins...`.

#### More available plugins (except built-in)

- [resampler-test](https://github.com/sdercolin/vlabeler-resampler-test): Play the UTAU resampler output of the current
  entry, which is similar to setParam's synthesis test (F10).

### Plugin Development

We welcome plugin development for the application.
You can distribute your plugin anywhere, or create a pull request to make it built-in.

See [Plugin API Document](readme/plugin-development.md) for details.

## Logs

You can find the logs by clicking `Help` -> `Open Log Directory`, for development/debug/test purposes.

When reporting issues to us, please attach the recent log files.

## App usage tracking

Please check [App Usage Tracking](readme/tracking.md) for details.

## Known issues

1. The `Scroll Canvas to Left/Right` actions in `Keymap` -> `Mouse scroll actions` cannot be changed at present.
   If you are using a trackpad, we recommend you to disable the `Go to Next/Previous Entry` actions in the same page, so
   that you can scroll the canvas by trackpad without triggering the entry navigation actions.
2. On Linux, the file chooser may not be able to select an empty folder. Please create something inside, or copy + paste
   its path
3. On Linux, sometimes the application may not be able to recycle memory properly.
   You can use `Tools` -> `Recycle Memory` to force it to recycle memory.
4. Sometimes the window freezes unexpectedly. You can resize the window to refresh it

### Localization help (besides code contributors)
[時雨ゆん](https://twitter.com/Yun_Shigure)
