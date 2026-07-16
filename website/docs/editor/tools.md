---
sidebar_position: 2
title: Tools
---

# Tools

The following editing tools are provided.
You can use shortcuts or menu items under `Edit` -> `Tools` to switch tools, or toggle the toolbox by menu `View`
-> `Show Toolbox`.

## Cursor

The normal cursor tool to drag parameter controllers.

## Scissors

Cut the entry into two parts by your click position.
This is typically used in the [Multi-entry editing mode](./multi-entry-editing.md) to create new labels.

By default, when you click on a valid position with the scissors:

1. Audio of the first part after cutting is played so that you can confirm the phoneme
2. An input box is shown in the editor to rename the first part
3. You can press `Enter` to confirm the input, or press `Esc` to cancel
4. If you move the cursor away from the clicked position, the input is confirmed as well
5. Cutting is conducted. The first one uses your input as its name, and the second one uses the original name
6. The editor goes to the first entry after cutting

In single-entry editing mode, the name input is requested in a dialog instead of in the editor.

These actions can be customized in `Preferences` -> `Editor` -> `Scissors`.

## Pan

Or the hand tool. Drag on the editor to scroll the canvas.

## Playback

Use mouse click or drag to play a certain range of the current sample file.
The following actions are available as default. You can change the key bindings
in `Settings` -> `Preferences` -> `Keymaps` -> `Mouse click actions`.

- Left click: play the audio from the clicked position until the end of the audio
- Right click: play the audio from the clicked position until the end of the screen
- `Shift` + left click: play the audio from the start of the file until the clicked position
- `Shift` + right click: play the audio from the start of the screen until the clicked position
- `Ctrl` + left click & drag: play the audio in the dragged range
- `Ctrl` + `Shift` + left click & drag: play the audio in the dragged range repeatedly
