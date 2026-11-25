---
sidebar_position: 3
title: カスタムスクリプトの使用
---

# カスタムスクリプトの使用

vLabeler では、カスタムスクリプトを使用してラベリングタスクを自動化できます。
下記の 2 つのプラグインがビルトインとして提供されており、`ツール` -> `一括編集` メニューから実行できます。

## スクリプトの実行

このプラグインでは、現在のサブプロジェクトにアクセスできます。

下記のプロパティが使用できます。

- `entries`: 現在のサブプロジェクトのエントリのリスト。
  詳細については [Entry](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_entry.js) を参照してください。
- `currentEntryIndex`: エントリのリストにおける現在のエントリのインデックス。
- `module`: 現在のサブプロジェクトのモジュールオブジェクト。このプロパティは読み取り専用であるため、
  このオブジェクトへの変更は保存されません。
  詳細については [Module](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_module.js) を参照してください。

## スクリプトの実行（複数サブプロジェクト）

このプラグインでは、すべてのサブプロジェクトにアクセスできます。

下記のプロパティが使用できます。

- `modules`: プロジェクトのモジュールのリスト。
  詳細については [Module](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_module.js) を参照してください。
- `currentModuleIndex`: モジュールのリストにおける現在のモジュールのインデックス。

## スクリプトの書き方

スクリプトは ES12 構文の JavaScript で書くことができます。
詳細については、[Scripting in vLabeler](https://github.com/sdercolin/vlabeler/blob/main/docs/scripting.md)
（英語）を参照してください。
