---
sidebar_position: 1
title: Installation
---

# Installation

## Download

See [Releases](https://github.com/sdercolin/vlabeler/releases).

Packaged application for the following platforms are provided in the releases.

- Windows: `~win64.zip`
- macOS (Intel): `~mac-x64.dmg`
- macOS (Apple Silicon): `~mac-arm64.dmg`
- Ubuntu: `~amd64.deb`

For other types of Linux OS, you may have to build it by yourself.

## Building from source

vLabeler is built with [Compose Multiplatform](https://github.com/JetBrains/compose-jb). You can use Gradle to build
the application.
[See more](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

Currently, cross-platform building is not supported. Only packages for your OS are built.

Please ensure you have **JDK 17+** for building.

```
// Package by an installer
./gradlew packageDistributionForCurrentOS

// Or, build an executable app
./gradlew createDistributable
```
