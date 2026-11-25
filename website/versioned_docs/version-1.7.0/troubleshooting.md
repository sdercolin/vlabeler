---
sidebar_position: 6
title: Troubleshooting
---

# Troubleshooting

**For assistance, feedback, reporting issues, and more, please join our [Discord](https://discord.gg/yrTqG2SrRd).**
When reporting issues, please attach the recent [log files](./misc/logs.md).

## Known issues

1. The `Scroll Canvas to Left/Right` actions in `Keymap` -> `Mouse scroll actions` cannot be changed at present.
   If you are using a trackpad, we recommend you to disable the `Go to Next/Previous Entry` actions in the same page,
   so that you can scroll the canvas by trackpad without triggering the entry navigation actions.
2. On Linux, the file chooser may not be able to select an empty folder. Please create something inside, or copy +
   paste its path
3. On Linux, sometimes the application may not be able to recycle memory properly.
   You can use `Tools` -> `Recycle Memory` to force it to recycle memory.
4. Sometimes the window freezes unexpectedly. You can resize the window to refresh it.
5. If all texts besides the menu bar are invisible, please try setting the environment variable `SKIKO_RENDER_API` to
   `SOFTWARE`.
6. On some Linux distributions, the file dialogs may not work properly. In this case, you can enable a custom file
   dialog in `Settings` -> `Preferences` -> `Miscellaneous`. Please note that the custom file dialog may not be as
   powerful as the system file dialog.
