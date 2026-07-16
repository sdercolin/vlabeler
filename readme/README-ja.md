# vLabeler

[![Discord](https://img.shields.io/discord/984044285584359444?style=for-the-badge&label=discord&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/yrTqG2SrRd)

言語を選択: [English](../README.md) | [简体中文](README-zhCN.md) | [日本語](README-ja.md) | [한국어](README-ko.md)

`vLabeler` はオープンソースの音声ラベリングアプリケーションで、次のことを目指しています。

- モダンで流暢な UI/UX
- さまざまな種類の音声生成ソフトウェアで使用される、カスタマイズ可能なラベル付けプロセス
- マルチプラットフォームをサポートする高性能

**ヘルプ、提案、問題レポートなどについては、[Discord](https://discord.gg/yrTqG2SrRd)
にて承っております。**

![](utau-singer.gif)

<details>
<summary>デモ動画（英語）</summary>

このデモ動画は 1.0.0-beta1 に基づいています。最新バージョンでは、一部の UI 要素が異なります。

[YouTube](https://youtu.be/xFX8SRrJEzM) | [bilibili](https://www.bilibili.com/video/BV1Ve4y1S7FF)
</details>

## ドキュメント

**ユーザーマニュアルは [docs.vlabeler.com/ja/](https://docs.vlabeler.com/ja/) でご覧いただけます。** 内容は次のとおりです。

- [はじめに](https://docs.vlabeler.com/ja/category/getting-started)：インストール、ラベラーの選択、プロジェクトの作成、クイック編集
- [エディタの使い方](https://docs.vlabeler.com/ja/category/editor)：キーボード/マウス操作、ツール、複数エントリ編集モード、エントリの閲覧、動画連携、音声フォーマットのサポート
- [カスタマイズ](https://docs.vlabeler.com/ja/category/customization)：ラベラー、プラグイン、カスタムスクリプト
- [トラブルシューティング](https://docs.vlabeler.com/ja/troubleshooting)およびその他のトピック（アプリディレクトリ、ログ、使用状況の収集）

ドキュメントは現在の安定版と最新のベータ版の両方に対応しており、英語・簡体字中国語・日本語・韓国語でご利用いただけます。サイト右上のドロップダウンで切り替えてください。

## ダウンロード

[Releases](https://github.com/sdercolin/vlabeler/releases) を参照してください。

Releases では、次のプラットフォーム用のアプリケーションパッケージが提供されます。

- Windows: `~win64.zip`
- macOS (Apple Silicon): `~mac-arm64.dmg`
- Ubuntu: `~amd64.deb`

その他の種類の Linux OS の場合は、自分でビルドする必要がある場合があります。

## ビルド

vLabeler は [Compose Multiplatform](https://github.com/JetBrains/compose-jb) を使用して開発しています。Gradle を使用して
アプリケーションをビルドできます。[詳細](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)

現在、クロスプラットフォームのビルドはサポートされていません。ビルドで使われた OS で実行可能なパッケージのみがビルドされます。

ビルドには **JDK 17+** が必要になります。

```
// インストーラーをビルドします
./gradlew packageDistributionForCurrentOS

// もしくは、実行可能なアプリケーションをビルドします
./gradlew createDistributable
```

## 開発

新しいラベル形式への対応や作業の自動化は、アプリケーションのコードを変更することなく、ラベラーやプラグインの開発によって実現できます。
開発へのご参加を歓迎します。作成したものは自由に配布していただけるほか、Pull Request を作成してビルトインにすることもできます。

- [カスタムラベラーの開発](../docs/labeler-development.md)（英語）
- [vLabeler プラグインの開発](../docs/plugin-development.md)（英語）
- [vLabeler におけるスクリプティング](../docs/scripting.md)（英語）

### ローカライズのヘルプ (コード貢献者以外)

[時雨ゆん](https://twitter.com/Yun_Shigure)
