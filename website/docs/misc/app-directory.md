---
sidebar_position: 1
title: Custom app directory
---

# Custom app directory

vLabeler saves user-specific data such as customized labelers, plugins, application settings and logs in the app
directory. By default, it is `~/vLabeler` (`~/Library/vLabeler` on macOS).

You can change the location by setting the environment variable `VLABELER_APP_DIR` to an absolute path of a directory
before launching the app. The directory is created if it does not exist. If the value is not valid, the default
location is used instead.

Note that existing contents are not migrated automatically. If you want to keep your current settings, please copy the
contents of the original app directory to the new location manually.
