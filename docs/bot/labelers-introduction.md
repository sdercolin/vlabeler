# Basic information about major labelers

## Labeler definition

Written in the `LabelerConf.txt` file as Kotlin code.

## UTAU labelling

Including `utau-oto-labeler` and `utau-singer-labeler`.

Raw label is UTAU's `oto.ini` file. Each entry is a line in the file.

With `utau-singer-labeler`, the project can contain multiple sub-projects, each of which corresponds to a folder (
usually pitch) under the voicebank's root folder.

Each sub-project can contains multiple samples, and each sample can contain multiple entries.

## NNSVS labelling

Mainly with `nnsvs-singer-labeler`.
Also used for labelling other AI based synthesizers with similar label format, e.g. DiffSinger.

Raw label is a Sinsy-style label file. Each entry is a line in the file.

The project can contain multiple sub-projects, each of which corresponds to one label file and one audio file, with the
same name except for the extension.
e.g. `1.lab` and `1.wav` will be used in the sub-project `1`.

## Praat TextGrid labelling

See the specific repository for details: https://github.com/sdercolin/vlabeler-textgrid

Its README is named `README-textgrid.md`.
