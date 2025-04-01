## 예상 패키지 구조

com.androidkotlin.generatorprokt/
├── data/                           # 데이터 레이어
│   ├── device/                     # 장치 관련 데이터 소스
│   │   ├── Serial422Device.kt      # 실제 시리얼 통신 구현
│   │   └── SerialPacketHandler.kt  # 패킷 처리 유틸리티
│   └── repository/                 # 리포지토리 구현체
│       ├── GeneratorRepositoryImpl.kt 
│       └── Serial422RepositoryImpl.kt
├── domain/                         # 도메인 레이어
│   ├── model/                      # 도메인 모델
│   │   ├── MainMode.kt             # 발전기 상태 모델
│   │   ├── SerialCommand.kt        # 통신 명령어 모델
│   │   ├── SerialPacket.kt         # 통신 패킷 모델
│   │   └── SerialResponse.kt       # 통신 응답 모델
│   ├── repository/                 # 리포지토리 인터페이스
│   │   ├── GeneratorRepository.kt
│   │   └── Serial422Repository.kt
│   └── usecase/                    # 유스케이스
│       ├── ConnectSerialUseCase.kt
│       ├── ReceiveSerialDataUseCase.kt
│       └── SendCommandUseCase.kt
├── presentation/                   # 프레젠테이션 레이어
│   ├── common/
│   │   └── view/
│   │       └── GeneratorStateView.kt
│   └── main/
│       ├── activity/
│       │   └── HomeActivity.kt
│       ├── state/
│       │   ├── DeviceStatus.kt
│       │   └── GeneratorUiState.kt
│       └── viewmodel/
│           ├── GeneratorStateViewModel.kt
│           └── HomeViewModel.kt
└── di/                             # 의존성 주입
├── AppModule.kt
├── GeneratorModule.kt
└── SerialModule.kt