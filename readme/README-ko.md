# vLabeler

[![Discord](https://img.shields.io/discord/984044285584359444?style=for-the-badge&label=discord&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/yrTqG2SrRd)

언어 선택: [English](../README.md) | [简体中文](README-zhCN.md) | [日本語](README-ja.md) | [한국어](README-ko.md)

&nbsp;`vLabeler`는 다음 목표를 따르는 오픈소스 음성 라벨링 프로그램이에요.

- &nbsp;현대적이고 유연한 UI/UX
- &nbsp;다양한 음성합성엔진에 맞춘 커스텀이 가능한 라벨링 환경
- &nbsp;높은 성능 보장, 크로스 플랫폼 지원

**&nbsp;도움, 피드백, 오류 제보 등은 [디스코드](https://discord.gg/yrTqG2SrRd)로 문의해 주세요.**

![](utau-singer.gif)

<details>
<summary>데모 영상 (영문)</summary>
&nbsp;초기 버전이었던 1.0.0-beta1의 영상으로, ui가 최신 버전의 vLabeler와 많이 다를 수 있어요. 

[유튜브](https://youtu.be/xFX8SRrJEzM) | [비리비리](https://www.bilibili.com/video/BV1Ve4y1S7FF)
</details>

<br>

## 문서

&nbsp;**사용자 매뉴얼은 [docs.vlabeler.com/ko/](https://docs.vlabeler.com/ko/)에서 보실 수 있어요.** 다음 내용을 다루고 있어요.

- [시작하기](https://docs.vlabeler.com/ko/category/getting-started): 설치, 라벨러 선택, 프로젝트 만들기, 빠른 편집
- [에디터 사용법](https://docs.vlabeler.com/ko/category/editor): 키보드/마우스 조작, 도구, 다중 엔트리 편집 모드, 엔트리 탐색, 영상 연동, 오디오 포맷 지원
- [커스터마이즈](https://docs.vlabeler.com/ko/category/customization): 라벨러, 플러그인, 커스텀 스크립트
- [문제 해결](https://docs.vlabeler.com/ko/troubleshooting) 및 기타 주제 (앱 폴더, 로그, 사용 데이터 수집)

&nbsp;문서는 현재 안정 버전과 최신 베타 버전 모두 제공되며, 영어, 중국어(간체), 일본어, 한국어를 지원해요. 사이트 오른쪽 위의 드롭다운 메뉴로 전환할 수 있어요.

<br>

## 다운로드

&nbsp;[Releases](https://github.com/sdercolin/vlabeler/releases) 를 봐 주세요.

&nbsp;Releases에 빌드본이 제공되는 플랫폼들은 다음과 같아요.

- 윈도우: `~win64.zip`
- 맥 (애플 실리콘): `~mac-arm64.dmg`
- 우분투: `~amd64.deb`

&nbsp;기타 리눅스 계열 운영체제들을 사용하실 경우, 직접 빌드하셔야 해요.

<br>

## 빌드

&nbsp;vLabeler는 [Compose Multiplatform](https://github.com/JetBrains/compose-jb)을 사용하고 있어요. 빌드에는 Gradle이 사용되며, 상세한 내용은 [이곳](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution)을 참조해 주세요.

&nbsp;현재 크로스 플랫폼 빌드는 지원되고 있지 않아요. 사용 중인 운영체제를 지정해 빌드하는 것만 가능합니다.

&nbsp;빌드에는 **JDK 17** 이상이 필요해요.

```
// 인스톨러를 사용해 앱 설치 패키지를 만들거나
./gradlew packageDistributionForCurrentOS

// 실행 가능한 형태의 앱을 만들 수 있어요
./gradlew createDistributable
```

<br>

## 개발

&nbsp;새로운 라벨 형식을 지원하거나 작업을 자동화하고 싶으시다면, 앱 코드를 수정하지 않고도 라벨러와 플러그인을 개발할 수 있어요.
여러분의 기여를 환영해요. 만드신 것은 자유롭게 배포하셔도 되고, Pull Request를 통해 내장 기능으로 제안하실 수도 있어요.

- [커스텀 라벨러 개발](../docs/labeler-development.md) (영문)
- [vLabeler 플러그인 개발](../docs/plugin-development.md) (영문)
- [vLabeler 스크립팅](../docs/scripting.md) (영문)

### 현지화에 도움을 주신 분들 (코드 기여자 제외)

[時雨ゆん](https://twitter.com/Yun_Shigure)

[빈빈](https://x.com/2xxbin)
