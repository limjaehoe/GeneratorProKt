# USB 통신 기능 문서

## 개요

GeneratorProKt 앱은 두 가지 USB 통신 방식을 지원합니다:
1. USB422 통신
2. USB232 통신

이 문서는 USB 통신 기능의 아키텍처, 구현 세부 사항 및 사용 방법을 설명합니다.

## 아키텍처

USB 통신 기능은 클린 아키텍처 원칙에 따라 다음과 같이 구성됩니다:

![USB 통신 다이어그램](../diagrams/usb_communication.png)

### 데이터 레이어 (USB 통신 구현)

```
com.yourcompany.generatorprokt.data.device.usb/
├── Usb422Communication.kt         # USB422 통신 구현
├── Usb232Communication.kt         # USB232 통신 구현
├── UsbManager.kt                  # USB 장치 관리
└── model/                         # USB 통신 관련 데이터 모델
    ├── ConnectionState.kt         # 연결 상태 모델
    ├── UsbCommand.kt              # USB 명령어 모델
    └── DeviceInfo.kt              # 장치 정보 모델
```

#### 주요 클래스 및 역할
