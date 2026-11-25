---
sidebar_position: 2
title: Choosing a labeler
---

# Choosing a labeler

Many behaviors of vLabeler depend on customizable `labeler`s. A labeler defines how vLabeler reads, displays, and
writes a certain type of label files. When creating a project, you first need to choose the labeler that matches
your target software. See [Labelers](../customization/labelers.md) for more details about the concept.

Currently, the followings are provided as built-in labelers:

## Working on UTAU oto.ini

![UTAU singer labeler](/img/utau-singer.gif)

For UTAU oto editing, there are two items in the built-in labeler list:

- **UTAU oto labeler**

  If you only want to edit one `oto.ini` file, please use this labeler.

- **UTAU singer labeler**

  If you want to edit multiple `oto.ini` files under a singer voicebank, please use this labeler.
  Please set the singer's root folder (which usually contains a `character.txt` file) as the `Sample Directory` when
  you create the project.

## Working on audio labels

![NNSVS singer labeler](/img/nnsvs-singer.gif)

For label files used in NNSVS/ENUNU and similar systems, the following options are available now:

- **Sinsy lab labeler**

  This labeler is for Sinsy (NNSVS/ENUNU) lab files, which uses `100ns` as the time unit and use ` ` (space) as the
  separator.
  It only accepts one label file per project. A wav file is bound to the label file by the file name, e.g.,
  if the input label file is `foo.lab`, the wav file named `foo.wav` will be selected from the `Sample Directory`.

- **Audacity labeler**

  This labeler is for `Labels` file created by Audacity, which uses `s` as the time unit and use `\t` (tab) as the
  separator.
  It only accepts one label file per project. A wav file is bound to the label file by the file name, e.g.,
  if the input label file is `foo.txt`, the wav file named `foo.wav` will be selected from the `Sample Directory`.

- **NNSVS singer labeler**

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

## Other labelers (not built-in)

- TextGrid labeler (for Praat TextGrid files): [GitHub](https://github.com/sdercolin/vlabeler-textgrid)
