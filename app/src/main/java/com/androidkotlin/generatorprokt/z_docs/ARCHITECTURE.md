# GeneratorProKt 아키텍처 문서

## 개요
GeneratorProKt는 X-ray Generator 프로그램의 코틀린 버전으로, 클린 아키텍처 원칙을 기반으로 설계되었습니다. 이 문서는 프로젝트의 전체 아키텍처와 각 구성 요소의 역할을 설명합니다.

## 아키텍처 원칙

GeneratorProKt는 다음과 같은 아키텍처 원칙을 따릅니다:

1. **관심사 분리**: 비즈니스 로직, 데이터 처리, UI 표현을 명확히 분리합니다.
2. **의존성 규칙**: 내부 레이어(도메인)는 외부 레이어(데이터, UI)에 의존하지 않습니다.
3. **테스트 용이성**: 각 컴포넌트를 개별적으로 테스트할 수 있도록 설계합니다.
4. **모듈화**: 각 기능은 독립적으로 개발, 테스트, 유지보수될 수 있습니다.

## 레이어 구조

GeneratorProKt는 세 가지 주요 레이어로 구성됩니다:

![아키텍처 다이어그램](diagrams/architecture.png)

### 1. 프레젠테이션 레이어 (UI)
- 사용자 인터페이스와 상호작용을 담당합니다.
- MVVM 패턴을 사용하여 UI 로직과 비즈니스 로직을 분리합니다.
- ViewBinding을 사용하여 뷰와 상호작용합니다.
- 주요 구성 요소:
    - Activities/Fragments
    - ViewModels
    - Adapters
    - UI 관련 유틸리티

### 2. 도메인 레이어
- 애플리케이션의 비즈니스 로직을 포함합니다.
- 외부 레이어(데이터, UI)에 의존하지 않습니다.
- 주요 구성 요소:
    - 엔티티 (비즈니스 모델)
    - 유스케이스 (비즈니스 로직)
    - 리포지토리 인터페이스 (의존성 역전)

### 3. 데이터 레이어
- 데이터 소스와의 통신을 담당합니다.
- 도메인 레이어에 정의된 리포지토리 인터페이스를 구현합니다.
- 주요 구성 요소:
    - 리포지토리 구현체
    - 로컬 데이터 소스 (Room DB)
    - 장치 통신 (USB422, USB232)
    - 데이터 매퍼

## 주요 패키지 구조

```
com.yourcompany.generatorprokt/
├── data/                           # 데이터 레이어
│   ├── local/                      # 로컬 데이터 소스
│   ├── device/                     # 장치 관련 데이터 소스
│   │   ├── usb/                    # USB 통신 관련 클래스
│   ├── repository/                 # 리포지토리 구현체
│   └── model/                      # 데이터 모델
├── domain/                         # 도메인 레이어
│   ├── model/                      # 도메인 모델
│   ├── repository/                 # 리포지토리 인터페이스
│   └── usecase/                    # 유스케이스
├── presentation/                   # 프레젠테이션 레이어
│   ├── common/                     # 공통 UI 컴포넌트
│   ├── main/                       # 메인 화면
│   ├── apr/                        # APR 관리 화면
│   ├── settings/                   # 설정 화면
│   └── communication/              # 통신 관리 화면
├── di/                             # 의존성 주입
└── util/                           # 유틸리티 클래스
```

## 주요 기능별 아키텍처

각 주요 기능(APR 관리, USB 통신, 설정 등)의 상세 아키텍처는 해당 기능 문서에서 더 자세히 설명합니다:

- [APR 관리 기능](features/APR.md)
- [USB 통신 기능](features/USB_COMMUNICATION.md)
- [설정 기능](features/SETTINGS.md)

## 기술 스택

- **언어**: Kotlin
- **UI 패턴**: MVVM (Model-View-ViewModel)
- **비동기 처리**: Kotlin Coroutines & Flow
- **의존성 주입**: Hilt
- **데이터베이스**: Room DB
- **뷰 바인딩**: ViewBinding

기술 스택의. 상세 사용법은 [tech/](tech/) 폴더의 문서들을 참조하세요.

## 테스트 아키텍처

GeneratorProKt는 다음과 같은 테스트 전략을 사용합니다:

- **단위 테스트**: 각 클래스 및 컴포넌트 별로 JUnit을 사용
- **UI 테스트**: Espresso를 사용한 UI 컴포넌트 테스트
- **통합 테스트**: 여러 컴포넌트 간의 상호작용 테스트

자세한 테스트 전략은 [TESTING.md](tech/TESTING.md)를 참조하세요.