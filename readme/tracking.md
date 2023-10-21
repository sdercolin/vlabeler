# About usage data tracking in vLabeler

vLabeler collects anonymous usage data to help us improve the product.
This article explains what data is collected and how it is used.

This article is only in English.
Please use translation services if you need to read it in other languages, but note
that the translation may not be accurate.

## Enable or disable tracking

When user first launches vLabeler, a dialog will pop up to ask for permission to collect usage data.
It requires an explicit action from the user to enable tracking.

The setting can be changed at any time in the menu `Settings` -> `Track App Usages...` dialog.

## Tracking ID

The tracking ID generated when user enables tracking is a random UUID, which does not contain any personal information.

User can switch to a different tracking ID by disabling and enabling tracking again.

## What data is collected

The data collected includes:

- Tracking ID
- Environment information, such as OS version, vLabeler version, etc.
    - Your directory structure, folder names, and file names are not collected.
- App launch events
- Fatal error logs, including the stack trace.
    - The stack trace may contain paths to your files and some parts of the file contents related to the error.
    - Only fatal errors are collected, not warnings or other types of errors.
- Project creation, including the labeler, the labeler parameters and some other settings.
    - Paths or contents of the input file, paths of all directories used and project names are not collected.
    - If you used a `file` type parameter in the labeler's settings, the path or content of the file is not collected.
- Plugin usages, including the parameters that are used.
    - If you used a `file` type parameter, the path or content of the file is not collected.
- Configuration changes
    - The configuration doesn't contain any personal information, file names, file contents, or paths.

You can check the detailed event definitions in the
[source code](../src/jvmMain/kotlin/com/sdercolin/vlabeler/tracking).

## How data is used

The data is firstly sent to [Segment](https://segment.com/), which is a data collection service.
Segment will then send the data to the [Mixpanel](https://mixpanel.com/) account that we manage.

See the following links for the privacy policy of Segment and Mixpanel:

- [Segment Privacy Policy](https://segment.com/legal/privacy/)
- [Mixpanel Privacy Policy](https://mixpanel.com/legal/privacy-policy)
