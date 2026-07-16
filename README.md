# vLabeler

[![Discord](https://img.shields.io/discord/984044285584359444?style=for-the-badge&label=discord&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/yrTqG2SrRd)

Select
Language: [English](README.md) | [简体中文](readme/README-zhCN.md) | [日本語](readme/README-ja.md) | [한국어](readme/README-ko.md)

`vLabeler` is an open-source voice labeling application with the following objectives:

- Delivering a modern and seamless UI/UX.
- Offering a customizable labeling process suitable for various voice generation software.
- Ensuring high performance and support across multiple platforms.

**For assistance, feedback, reporting issues, and more, please join our [Discord](https://discord.gg/yrTqG2SrRd).**

![](readme/utau-singer.gif)

<details>
<summary>Demo video (in English)</summary>
Please note that this demo video is a bit outdated (1.0.0-beta1).
Some UI elements may be different in the latest version.

[YouTube](https://youtu.be/xFX8SRrJEzM) | [bilibili](https://www.bilibili.com/video/BV1Ve4y1S7FF)
</details>

## Documentation

**The user manual is available at [docs.vlabeler.com](https://docs.vlabeler.com)**, covering:

- [Getting started](https://docs.vlabeler.com/category/getting-started): installation, choosing a labeler,
  creating a project, quick edit
- [Editor usage](https://docs.vlabeler.com/category/editor): keyboard/mouse actions, tools, multi-entry
  editing, browsing entries, video integration, audio format support
- [Customization](https://docs.vlabeler.com/category/customization): labelers, plugins, custom scripts
- [Troubleshooting](https://docs.vlabeler.com/troubleshooting) and
  [miscellaneous topics](https://docs.vlabeler.com/category/miscellaneous) (app directory, logs,
  usage tracking)

The documentation is provided for both the current stable version and the latest beta version, in English,
Simplified Chinese, Japanese and Korean — use the dropdowns in the top-right corner of the site to switch.

## Download

See [Releases](https://github.com/sdercolin/vlabeler/releases).

Packaged application for the following platforms are provided in the releases.

- Windows: `~win64.zip`
- macOS (Apple Silicon): `~mac-arm64.dmg`
- Ubuntu: `~amd64.deb`

For other types of Linux os, you may have to build it by yourself.

## Building

vLabeler is built with [Compose Multiplatform](https://github.com/JetBrains/compose-jb). You can use Gradle to build the
application. [See more](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

Currently, cross-platform building is not supported. Only packages for your OS are built.

Please ensure you have **JDK 17+** for building.

```
// Package by an installer
./gradlew packageDistributionForCurrentOS

// Or, build an executable app
./gradlew createDistributable
```

## Development

If you want to support a new label format or automate your workflow, you can develop labelers and plugins
without touching the application code. We welcome contributions — you can distribute your creations
anywhere, or create a pull request to make them built-in.

- [Develop Custom Labelers](docs/labeler-development.md)
- [Develop Plugins for vLabeler](docs/plugin-development.md)
- [Scripting in vLabeler](docs/scripting.md)

### Localization help (besides code contributors)

[時雨ゆん](https://twitter.com/Yun_Shigure)
