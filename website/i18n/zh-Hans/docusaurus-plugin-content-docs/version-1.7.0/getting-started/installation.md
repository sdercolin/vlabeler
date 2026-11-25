---
sidebar_position: 1
title: 安装
---

# 安装

## 下载

请参阅 [Releases](https://github.com/sdercolin/vlabeler/releases)。

Releases 中提供了适用于以下平台的应用程序包。

- Windows: `~win64.zip`
- macOS (Apple Silicon): `~mac-arm64.dmg`
- Ubuntu: `~amd64.deb`

对于其他类型的 Linux 操作系统，您可能需要自己进行构建。

## 从源代码构建

vLabeler 基于 [Compose Multiplatform](https://github.com/JetBrains/compose-jb) 开发。
您可以使用 Gradle 来构建应用。
[查看更多](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

目前不支持跨平台构建。您只能构建适用于您的操作系统的应用程序包。

请确保您有 **JDK 17+** 用于构建。

```
// 构建安装包
./gradlew packageDistributionForCurrentOS

// 或者，构建可执行程序
./gradlew createDistributable
```
