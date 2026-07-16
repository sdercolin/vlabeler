---
sidebar_position: 1
title: 설치
---

# 설치

## 다운로드

[Releases](https://github.com/sdercolin/vlabeler/releases)를 봐 주세요.

Releases에 빌드본이 제공되는 플랫폼들은 다음과 같아요.

- 윈도우: `~win64.zip`
- 맥 (애플 실리콘): `~mac-arm64.dmg`
- 우분투: `~amd64.deb`

기타 리눅스 계열 운영체제들을 사용하실 경우, 직접 빌드하셔야 해요.

## 소스에서 빌드하기

vLabeler는 [Compose Multiplatform](https://github.com/JetBrains/compose-jb)을 사용하고 있어요. 빌드에는 Gradle이 사용되며,
상세한 내용은
[이곳](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)을
참조해 주세요.

현재 크로스 플랫폼 빌드는 지원되고 있지 않아요. 사용 중인 운영체제를 지정해 빌드하는 것만 가능합니다.

빌드에는 **JDK 17** 이상이 필요해요.

```
// 인스톨러를 사용해 앱 설치 패키지를 만들거나
./gradlew packageDistributionForCurrentOS

// 실행 가능한 형태의 앱을 만들 수 있어요
./gradlew createDistributable
```
