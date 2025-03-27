## 예상 패키지 구조

com.yourcompany.xraygenerator/
│
├── data/                           # 데이터 레이어
│   ├── local/                      # 로컬 데이터 소스
│   │   ├── dao/                    # Room DAO 클래스들
│   │   │   ├── AprDao.kt
│   │   │   └── SettingsDao.kt
│   │   │
│   │   ├── database/               # Room 데이터베이스 및 엔티티
│   │   │   ├── AppDatabase.kt
│   │   │   ├── AprEntity.kt
│   │   │   └── SettingsEntity.kt
│   │   │
│   │   └── preferences/            # SharedPreferences 래퍼
│   │       └── AppPreferences.kt
│   │
│   ├── device/                     # 장치 관련 데이터 소스
│   │   ├── usb/                    # USB 통신 관련 클래스
│   │   │   ├── Usb422Communication.kt
│   │   │   ├── Usb232Communication.kt
│   │   │   ├── UsbManager.kt
│   │   │   └── model/              # USB 통신 관련 데이터 모델
│   │   │       ├── ConnectionState.kt
│   │   │       └── DeviceInfo.kt
│   │   │
│   │   └── protocol/               # 통신 프로토콜 관련 클래스
│   │       ├── XrayProtocol.kt
│   │       └── CommandFactory.kt
│   │
│   ├── repository/                 # 리포지토리 구현체
│   │   ├── AprRepositoryImpl.kt
│   │   ├── SettingsRepositoryImpl.kt
│   │   └── DeviceCommunicationRepositoryImpl.kt
│   │
│   └── model/                      # 데이터 모델 (엔티티)
│       ├── AprData.kt
│       ├── DeviceSettings.kt
│       └── ...
│
├── domain/                         # 도메인 레이어 (클린 아키텍처 적용시)
│   ├── model/                      # 도메인 모델
│   │   ├── Apr.kt
│   │   ├── Settings.kt
│   │   └── DeviceCommand.kt
│   │
│   ├── repository/                 # 리포지토리 인터페이스
│   │   ├── AprRepository.kt
│   │   ├── SettingsRepository.kt
│   │   └── DeviceCommunicationRepository.kt
│   │
│   └── usecase/                    # 유스케이스
│       ├── apr/                    # APR 관련 유스케이스
│       │   ├── ImportAprCsvUseCase.kt
│       │   ├── GetAllAprsUseCase.kt
│       │   └── ManageAprUseCase.kt
│       │
│       ├── settings/               # 설정 관련 유스케이스
│       │   ├── GetSettingsUseCase.kt
│       │   └── UpdateSettingsUseCase.kt
│       │
│       └── communication/          # 통신 관련 유스케이스
│           ├── ConnectToDeviceUseCase.kt
│           ├── SendCommandUseCase.kt
│           └── DisconnectDeviceUseCase.kt
│
├── presentation/                   # 프레젠테이션 레이어
│   ├── common/                     # 공통 UI 컴포넌트
│   │   ├── adapter/                # 재사용 가능한 어댑터
│   │   ├── extension/              # UI 관련 확장 함수
│   │   └── view/                   # 커스텀 뷰
│   │
│   ├── main/                       # 메인 화면
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   │
│   ├── apr/                        # APR 관리 화면
│   │   ├── AprFragment.kt
│   │   ├── AprViewModel.kt
│   │   ├── import/                 # APR 가져오기 관련 화면
│   │   │   ├── ImportAprFragment.kt
│   │   │   └── ImportAprViewModel.kt
│   │   │
│   │   └── adapter/
│   │       └── AprListAdapter.kt
│   │
│   ├── settings/                   # 설정 화면
│   │   ├── SettingsFragment.kt
│   │   └── SettingsViewModel.kt
│   │
│   └── communication/              # 통신 관리 화면
│       ├── UsbConnectionFragment.kt
│       ├── UsbConnectionViewModel.kt
│       ├── Usb422Fragment.kt
│       ├── Usb422ViewModel.kt
│       ├── Usb232Fragment.kt
│       └── Usb232ViewModel.kt
│
├── di/                             # 의존성 주입 (Hilt)
│   ├── AppModule.kt                # 앱 일반 의존성 제공
│   ├── DatabaseModule.kt           # Room DB 관련 의존성 제공
│   ├── DeviceModule.kt             # USB 통신 관련 의존성 제공
│   ├── RepositoryModule.kt         # 리포지토리 의존성 제공
│   └── UseCaseModule.kt            # 유스케이스 의존성 제공
│
└── util/                           # 유틸리티 클래스
├── CsvParser.kt                # CSV 파일 파싱 유틸리티
├── FileUtils.kt                # 파일 관련 유틸리티
├── SerialCommunicationUtils.kt # 시리얼 통신 유틸리티
└── Constants.kt                # 상수 정의