---
sidebar_position: 1
title: インストール
---

# インストール

## ダウンロード

[Releases](https://github.com/sdercolin/vlabeler/releases) を参照してください。

Releases では、次のプラットフォーム用のアプリケーションパッケージが提供されます。

- Windows: `~win64.zip`
- macOS (Intel): `~mac-x64.dmg`
- macOS (Apple Silicon): `~mac-arm64.dmg`
- Ubuntu: `~amd64.deb`

その他の種類の Linux OS の場合は、自分でビルドする必要がある場合があります。

## ソースからビルドする

vLabeler は [Compose Multiplatform](https://github.com/JetBrains/compose-jb) を使用して開発しています。Gradle を使用して
アプリケーションをビルドできます。
[詳細](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

現在、クロスプラットフォームのビルドはサポートされていません。ビルドで使われた OS で実行可能なパッケージのみがビルドされます。

ビルドには **JDK 17+** が必要になります。

```
// Package by an installer
./gradlew packageDistributionForCurrentOS

// Or, build an executable app
./gradlew createDistributable
```
