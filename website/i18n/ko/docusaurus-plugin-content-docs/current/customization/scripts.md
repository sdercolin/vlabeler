---
sidebar_position: 3
title: 사용자 정의 스크립트 사용
---

# 사용자 정의 스크립트 사용

vLabeler는 사용자 정의 스크립트를 사용하여 라벨링 작업을 자동화할 수 있습니다.
내장 플러그인 두 가지가 제공되며, `도구` -> `일괄 편집` 메뉴에서 실행할 수 있어요. :

## 스크립트 실행

현재 하위 프로젝트에 접근할 수 있는 플러그인이에요.

다음 프로퍼티들을 사용할 수 있어요. :

- `entries`: 현재 하위 프로젝트의 엔트리 목록.
  자세한 내용은 [Entry](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_entry.js)를
  참조해 주세요.
- `currentEntryIndex`: 엔트리 목록에서 현재 엔트리가 위치한 인덱스.
- `module`: 현재 하위 프로젝트의 모듈 객체. 이 프로퍼티는 읽기 전용이라, 이 객체를 수정해도 저장되지 않아요.
  자세한 내용은 [Module](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_module.js)을
  참조해 주세요.

## 스크립트 실행 (다중 하위 프로젝트)

모든 하위 프로젝트에 접근할 수 있는 플러그인이에요.

다음 프로퍼티들을 사용할 수 있어요. :

- `modules`: 프로젝트의 모듈 목록.
  자세한 내용은 [Module](https://github.com/sdercolin/vlabeler/blob/main/src/jvmMain/resources/js/class_module.js)을
  참조해 주세요.
- `currentModuleIndex`: 모듈 목록에서 현재 모듈이 위치한 인덱스.

## 스크립트 작성 방법

스크립트는 ES12 문법의 JavaScript로 작성할 수 있어요.
자세한 내용은 [vLabeler에서의 스크립트 작성(영문)](https://github.com/sdercolin/vlabeler/blob/main/docs/scripting.md)을
참조해 주세요.
