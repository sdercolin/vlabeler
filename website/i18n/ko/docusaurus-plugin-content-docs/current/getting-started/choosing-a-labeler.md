---
sidebar_position: 2
title: 라벨러 선택하기
---

# 라벨러 선택하기

vLabeler 동작의 대부분은 유저 커스텀이 가능한 `라벨러`들에 의해 이루어져요. 라벨러는 vLabeler가 특정 종류의 라벨 파일을 어떻게
읽고, 표시하고, 저장할지를 정의해요. 프로젝트를 만들 때는 먼저 사용하려는 음성합성엔진에 맞는 라벨러를 선택해야 해요.
라벨러라는 개념에 관한 자세한 내용은 [라벨러](../customization/labelers.md)를 참조해 주세요.

아래에서는 기본 내장 라벨러들을 다루고 있답니다. :

## UTAU의 oto.ini를 편집할래요

![UTAU singer 라벨러](/img/utau-singer.gif)

UTAU oto 편집(원음설정)용 내장 라벨러는 두 종류에요 :

- **UTAU oto 라벨러**

  한 개의 `oto.ini`만 편집할 때 사용해요.

- **UTAU singer 라벨러**

  가수 폴더에 포함된 여러 개의 `oto.ini`를 편집할 때 사용해요.
  `샘플 폴더`는 반드시 가수의 루트 폴더로 지정해 주세요. (주로 `character.txt`를 포함하는, 가수의 가장 바깥쪽 폴더를 '가수의 루트
  폴더' 라고 합니다.)

## 오디오 라벨 파일을 편집할래요 \{#working-on-audio-labels}

![NNSVS singer 라벨러](/img/nnsvs-singer.gif)

NNSVS/ENUNU 계열 라벨 파일용 내장 라벨러는 다음과 같아요. :

- **Sinsy lab 라벨러**

  Sinsy(NNSVS/ENUNU) lab 파일을 편집할 때 사용해요. Sinsy lab은 시간 단위로 `100 나노초`, 구분자로 ` `(스페이스 공백)을 사용하는
  형식이랍니다.
  각 프로젝트 당 한 개의 라벨 파일만 다룰 수 있어요. wav 파일은 파일명을 통해 라벨 파일과 연결됩니다. (예시: 파일명이 `foo.lab`인
  라벨 파일은 `샘플 폴더` 내의 `foo.wav`와 대응됩니다.)

- **Audacity 라벨러**

  Audacity를 통해 만든 `레이블` 파일을 편집할 때 사용해요. Audacity `레이블` 파일은 시간 단위로 `초`, 구분자로 `\t` (탭)을
  사용하는 형식이랍니다.
  각 프로젝트 당 한 개의 라벨 파일만 다룰 수 있어요. wav 파일은 파일명을 통해 라벨 파일과 연결됩니다. (예시: 파일명이 `foo.txt`인
  라벨 파일은 `샘플 폴더` 내의 `foo.wav`와 대응됩니다.)

- **NNSVS singer 라벨러**

  `Sinsy lab 라벨러`와 기본적으로 똑같지만, 여러 개의 라벨 파일을 편집할 수 있답니다.
  주로 singer 폴더의 파일 구조가 하단과 같을 때 이 라벨러를 사용해요.

  ```
  - singer
      - wav
        - 1.wav
        - 2.wav
      - lab
        - 1.lab
        - 2.lab
  ```

  위의 예시에서는, `singer`폴더의 경로를 `샘플 폴더`의 경로로 설정함으로써 모든 라벨 파일들을 포함하는 프로젝트를 만들 수 있어요.
  `wav` 폴더 및 `lab` 폴더의 이름은 라벨러 설정에서 바꿀 수 있답니다.

## 기본 내장 라벨러 이외의 다른 라벨러가 필요해요

- TextGrid 라벨러 (Praat TextGrid 용): [깃헙](https://github.com/sdercolin/vlabeler-textgrid)
