---
sidebar_position: 4
title: Browsing entries
---

# Browsing entries

There are two components showing an entry list:

1. The pinned entry list: shown on the right side of the editor by default. You can toggle it by via menu `View`
   -> `Pin Entry List`. It provides a full powered entry filter (described below).
2. The `Go to entry...` dialog: shown when you press `Ctrl` + `G` or click the entry number button in the bottom bar.
   It provides a simple entry filter.

## Searching and filtering entries

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

In the pinned entry list, you can click the `Expand` button to show more options.

For the `Done` filter and `Star` filter, clicking will switch them between `Do not filter`, `Show only starred/done`
and `Show only unstarred/undone`.

All these filters are combined with `AND` logic.

You can click the `More` button to open a dialog to use the advanced filters.

## Selecting multiple entries

In the pinned entry list, you can select multiple entries and edit them together:

- `Ctrl` + click: add an entry to the selection, or remove it from the selection
- `Shift` + click: select a range of entries

Note that the above `Ctrl` is mapped to `Command` if you are using macOS.

Right-click on the selected entries to show the available batch actions:

- Mark the selected entries as done/not done
- Star/unstar the selected entries
- Set a tag for the selected entries
- Remove the selected entries

A click without modifier keys clears the selection. A batch action can be reverted by a single `Undo`.

## Linking filter to editor

By default, project navigation (go to next sample/entry etc. by keyboard shortcuts, mouse wheel scroll or button
click) is not affected by the filter.

For example, even if you have filtered out entry `no.5` in the entry list, you can still press `Down` key to go to the
entry `no.5` from entry `no.4`.

The `Link` button in the expanded filter can help you concentrate on a specific set of entries. When it's toggled, the
entries that are not shown in the pinned entry list will be skipped when navigating.

In the above example, if you have toggled the `Link` button, you will go to entry `no.6` when pressing `Down` key
instead of `no.5`.

Note that the `Go to entry...` dialog is not affected by the linked filter, so you can still go to any entry by it
without changing or clearing your filter settings in the pinned entry list.
