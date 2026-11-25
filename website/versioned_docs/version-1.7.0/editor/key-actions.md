---
sidebar_position: 1
title: Keyboard/mouse actions
---

# Available keyboard/mouse actions

Note that the following `Ctrl` is mapped to `Command` if you are using macOS.

You can customize the key bindings in `Settings` -> `Preferences` -> `Keymaps`.

## Move parameter lines

- Mouse drag on normal parameters: move itself
- Mouse drag on primary parameters: move all the lines together
- `Shift`: invert normal/primary. e.g. mouse drag on primary parameters with Shift pressed only moves itself.
- `Q`/`W`/`E`/`R`/... : move the corresponding parameter line to current cursor position. The order is defined in the
  labeler.
    - In the single entry editing mode, this applies to all the parameter lines of the entry.
    - In the multi-entry editing mode, only `Q` and `W` are available, moving the left and right border of the entry
      under the cursor respectively. You can also bind the `Set Current Entry's Left/Right Border` actions (no default
      key) in `Keymaps` to move the borders of the current entry regardless of the cursor position.

## Audio playback

- `Space`: play the current entry, or stop playing if already playing
- `Shift` + `Space`: play the current sample file or stop playing if already playing
- `Ctrl` + `Shift` + `Space`: play the audio with current screen range or stop playing if already playing
- (with most tools) mouse right click: play the clicked section
- (with the **Cursor** tool) `Alt` + mouse drag on parameters: play the audio near the cursor's position while moving

Please also see the section of the [Playback tool](./tools.md#playback) for more actions.

## Scrolling

- `Shift` + mouse wheel scroll: horizontal scroll
- `F` or Focus button in the center of the bottom bar: Scroll to center the current entry on the screen

## Zoom in/out

- `=` or `+` button in the bottom bar: zoom in
- `-` or `-` button in the bottom bar: zoom out
- `Ctrl` + `Shift` + mouse wheel scroll: zoom in/out
- Resolution button in the bottom bar: open dialog for resolution input

## Switch entry/sample

- Mouse wheel scroll: go to previous/next entry
- `Up`/`Down`: go to previous/next entry
- `<`/`>` buttons in the bottom bar: go to previous/next entry
- `Ctrl` + mouse wheel scroll: go to previous/next sample
- `Ctrl` + `Up`/`Down`: go to previous/next sample
- `<<`/`>>` buttons in the bottom bar: go to previous/next sample
- `Ctrl` + `G` or entry number button in the bottom bar: show `Go to entry...` dialog

## Set notes for entry

- `J` or `Add tag` button in the entry title bar: Start editing the entry's tag
- `K` or `Star` button in the entry title bar: Toggle the entry's `Starred` status
- `L` or `Done` button in the entry title bar: Toggle the entry's `Done` status

### Settings for notes

You can change settings in `Settings` -> `Preferences` -> `Editor` -> `Notes` to hide the items that
you don't need.

## Post-editing actions

You can change settings in `Settings` -> `Preferences` -> `Editor` -> `Post-editing actions` to automatically execute
the following actions after editing entries:

- Go to next entry
- Set the edited entry as `Done` (enabled by default)

## Quickly launch batch edit plugins

- `F1`~`F8`: launch the corresponding batch edit plugin. Need to be configured in
  `Tools` -> `Batch Edit` -> `Slot Settings...` before use. About plugins,
  see [Plugins](../customization/plugins.md) for more details.
