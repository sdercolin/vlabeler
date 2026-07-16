# vLabeler

[![Discord](https://img.shields.io/discord/984044285584359444?style=for-the-badge&label=discord&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/yrTqG2SrRd)

选择语言：[English](../README.md) | [简体中文](README-zhCN.md) | [日本語](README-ja.md) | [한국어](README-ko.md)

`vLabeler` 是一个开源的语音标注应用程序，旨在提供：

- 现代，流畅的 UI/UX
- 可定制的标注流程，以供多个语音合成软件使用
- 高性能的多平台支持

**如需帮助、建议、问题报告等，请加入我们的 [Discord](https://discord.gg/yrTqG2SrRd)。**

![](utau-singer.gif)

<details>
<summary>Demo 视频（英语）</summary>

请注意，由于此演示视频基于1.0.0-beta1，部分UI可能与最新版本不同。

[YouTube](https://youtu.be/xFX8SRrJEzM) | [bilibili](https://www.bilibili.com/video/BV1Ve4y1S7FF)
</details>

## 文档

**用户手册请参阅 [docs.vlabeler.com/zh-Hans/](https://docs.vlabeler.com/zh-Hans/)**，包括：

- [快速上手](https://docs.vlabeler.com/zh-Hans/category/getting-started)：安装、选择标注器、创建项目、快速编辑
- [编辑器使用](https://docs.vlabeler.com/zh-Hans/category/editor)：键盘/鼠标操作、工具、多条目编辑模式、浏览条目、视频集成、音频格式支持
- [自定义](https://docs.vlabeler.com/zh-Hans/category/customization)：标注器、插件、自定义脚本
- [故障排除](https://docs.vlabeler.com/zh-Hans/troubleshooting)以及其他主题（应用目录、日志、使用数据收集）

文档同时提供当前稳定版和最新测试版的内容，支持英语、简体中文、日语和韩语——请使用网站右上角的下拉菜单进行切换。

## 下载

请参阅 [Releases](https://github.com/sdercolin/vlabeler/releases)。

Releases 中提供了适用于以下平台的应用程序包。

- Windows: `~win64.zip`
- macOS (Apple Silicon): `~mac-arm64.dmg`
- Ubuntu: `~amd64.deb`

对于其他类型的 Linux 操作系统，您可能需要自己进行构建。

## 构建

vLabeler 基于 [Compose Multiplatform](https://github.com/JetBrains/compose-jb) 开发。
您可以使用 Gradle
来构建应用。[查看更多](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

目前不支持跨平台构建。您只能构建适用于您的操作系统的应用程序包。

请确保您有 **JDK 17+** 用于构建。

```
// 构建安装包
./gradlew packageDistributionForCurrentOS

// 或者，构建可执行程序
./gradlew createDistributable
```

## 开发

如果您想要支持新的标注格式或自动化您的工作流程，您可以开发标注器和插件，而无需修改应用程序代码。
我们欢迎您的贡献——您可以在任何地方发布您的作品，或者通过创建 Pull Request 使其成为内置内容。

- [开发自定义标注器](../docs/labeler-development.md)（英语）
- [为 vLabeler 开发插件](../docs/plugin-development.md)（英语）
- [vLabeler 中的脚本](../docs/scripting.md)（英语）

### 本地化帮助（代码贡献者以外）

[時雨ゆん](https://twitter.com/Yun_Shigure)
