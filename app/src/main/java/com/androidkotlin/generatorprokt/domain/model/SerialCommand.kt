package com.androidkotlin.generatorprokt.domain.model

/**
 * 발전기 통신 명령어를 나타내는 sealed class
 * 프로토콜 문서 기반으로 구현됨
 */
sealed class SerialCommand(val value: Int) {

    /**
     * 제어 명령어 (Control Command)
     */
    sealed class Control(value: Int) : SerialCommand(value) {
        // 기본 제어 명령어
        object CommInitial : Control(0x3C)        // 초기화
        object CommControl : Control(0x3E)        // 제어 명령 및 수행
        object CommGetInfo : Control(0x5E)        // DATA 요청 및 응답
        object CommStatusInfo : Control(0x2C)     // 상태 요청 및 응답
        object CommMemWrite : Control(0x2E)       // 메모리 저장 명령
        object CommVersionInfo : Control(0x3F)    // 버전 정보
        object CommSysControl : Control(0x7A)     // 시스템 제어
        object CommConfiguration : Control(0x22)  // 시스템 환경 설정

        // 추가적인 제어 명령어는 필요에 따라 확장 가능
    }

    /**
     * 동작 명령어 (Action Command)
     * 프로토콜 문서의 Act Cmd(HEX) 기반으로 구현됨
     */
    sealed class Action(value: Int) : SerialCommand(value) {
        // 기본 시스템 동작 명령어
        object HeartBeat : Action(0xFC)                 // 하트비트 (통신 연결 확인)
        object InitialReq : Action(0xF6)                // 초기 연결 상태 확인
        object FeedbackRequest : Action(0x76)           // 피드백 요청
        object PowerDiagnosis : Action(0xDA)            // 전원 진단
        object WarningCode : Action(0xE2)               // 경고 코드
        object ErrorCode : Action(0xE0)                 // 오류 코드
        object CommandDone : Action(0x80)               // 명령 완료

        // 시간 관련 명령어
        object TimeMin : Action(0x00)                   // 최소 노출 시간
        object TimeMax : Action(0x01)                   // 최대 노출 시간
        object TimeValue : Action(0x42)                 // 설정된 노출 시간
        object TimeFbValue : Action(0x72)               // 노출 시간 피드백 값

        // 로터 관련 명령어
        object RotorStartingCurrentMin : Action(0x02)   // 로터 시작 전류 최소값
        object RotorStartingCurrentMax : Action(0x03)   // 로터 시작 전류 최대값
        object RotorStartingTime : Action(0x04)         // 로터 시작 시간
        object RotorRunningCurrentMin : Action(0x05)    // 로터 구동 전류 최소값
        object RotorRunningCurrentMax : Action(0x06)    // 로터 구동 전류 최대값
        object RotorRunningTime : Action(0x07)          // 로터 구동 시간
        object RotorContinuousMode : Action(0x62)       // 로터 연속 모드
        object RotorPhaseOffsetValue : Action(0x20)     // 로터 위상 오프셋 값
        object FbRotorCurrent : Action(0xE5)            // 로터 전류 피드백

        // 그리드 관련 명령어
        object GridDelayTime : Action(0x08)             // 그리드 지연 시간

        // 버키 관련 명령어
        object Bucky1DetectorType : Action(0x09)        // 버키1 감지기 타입
        object Bucky1GridType : Action(0x0A)            // 버키1 그리드 타입
        object Bucky2DetectorType : Action(0x0B)        // 버키2 감지기 타입
        object Bucky2GridType : Action(0x0C)            // 버키2 그리드 타입
        object BuckySelect : Action(0x52)               // 버키 선택
        object Bk1Fb : Action(0x1E)                     // 버키1 피드백
        object Bk2Fb : Action(0x1F)                     // 버키2 피드백

        // kV 관련 명령어
        object KvValue : Action(0x40)                   // kV 설정값
        object KvFbValue : Action(0x70)                 // kV 피드백 값
        object KvFbCalValue : Action(0x75)              // kV 피드백 보정값
        object KvCal60Value : Action(0x21)              // 60kV 보정값
        object KvCal120Value : Action(0x22)             // 120kV 보정값

        // mA 관련 명령어
        object MaValue : Action(0x41)                   // mA 설정값
        object MaFbValue : Action(0x71)                 // mA 피드백 값
        object MaFbCalValue : Action(0x77)              // mA 피드백 보정값
        object MaGainSelect : Action(0x50)              // mA 게인 선택
        object MaMaxRating : Action(0x49)               // mA 최대 레이팅
        object MaSingleFaultRatio : Action(0x24)        // mA 단일 고장 비율
        object MaCalValue : Action(0x27)                // mA 보정값

        // 인터록 관련 명령어
        object InterlockConfig : Action(0x0E)           // 인터록 설정
        object DoorlockConfig : Action(0x0F)            // 도어록 설정
        object ExtlockConfig : Action(0x10)             // 외부 인터록 설정

        // 모드 관련 명령어
        object Mode : Action(0x30)                      // 동작 모드
        object SystemStatus : Action(0x31)              // 시스템 상태
        object CalMode : Action(0x60)                   // 보정 모드
        object CalControl : Action(0x61)                // 보정 제어

        // 스위치 관련 명령어
        object ExposureSw : Action(0x32)                // 노출 스위치
        object ReadySw : Action(0x33)                   // 준비 스위치

        // 커패시터 뱅크 관련 명령어
        object CapbankChargeTime : Action(0x11)         // 커패시터 뱅크 충전 시간
        object CapbankChargeTimeout : Action(0x12)      // 커패시터 뱅크 충전 타임아웃
        object CapbankLevelMin : Action(0x13)           // 커패시터 뱅크 최소 레벨
        object CapbankLevelMax : Action(0x14)           // 커패시터 뱅크 최대 레벨

        // 필라멘트 관련 명령어
        object FilamentMin : Action(0x15)               // 필라멘트 최소값
        object FilamentMax : Action(0x16)               // 필라멘트 최대값
        object FilamentBoostTime : Action(0x1D)         // 필라멘트 부스트 시간
        object FilamentPrepareTime : Action(0x2A)       // 필라멘트 준비 시간
        object FbFilamentCurrent : Action(0xE4)         // 필라멘트 전류 피드백
        object FilamentGetManage : Action(0xDB)         // 필라멘트 관리 정보

        // 포커스 관련 명령어
        object Focus : Action(0x51)                     // 포커스 (Small/Large)
        object TubeFocusSmallCurrentStandby : Action(0x1B)  // 튜브 포커스 Small 대기 전류
        object TubeFocusLargeCurrentStandby : Action(0x1C)  // 튜브 포커스 Large 대기 전류
        object TubeFocusSmallCurrentBoost : Action(0x43)    // 튜브 포커스 Small 부스트 전류
        object TubeFocusLargeCurrentBoost : Action(0x44)    // 튜브 포커스 Large 부스트 전류
        object FocusChangeTime : Action(0x28)              // 포커스 변경 시간
        object FocusChangeAddPrepareTime : Action(0x29)    // 포커스 변경 추가 준비 시간
        object FocusChangeRecommendTime : Action(0x2B)     // 포커스 변경 권장 시간
        object FocusChangePrepareMode : Action(0x2D)       // 포커스 변경 준비 모드

        // AEC 관련 명령어
        object TDrResponseTime : Action(0x0D)           // DR 응답 시간
        object AecBackupTime : Action(0x19)             // AEC 백업 시간
        object AecRef : Action(0x47)                    // AEC 참조값
        object AecChSelect : Action(0x53)               // AEC 채널 선택
        object AecFieldSelect : Action(0x54)            // AEC 필드 선택
        object AecErrorTime : Action(0x26)              // AEC 오류 시간

        // HSS 관련 명령어
        object HssEnable : Action(0x2C)                 // HSS 활성화
        object HssMode : Action(0x4A)                   // HSS 모드
        object HssStartingVoltageDuty : Action(0xB0)    // HSS 시작 전압/듀티
        object HssRunningVoltageDuty : Action(0xB1)     // HSS 구동 전압/듀티
        object HssBrakeVoltageDuty : Action(0xB2)       // HSS 제동 전압/듀티
        object HssStartingTime : Action(0xB3)           // HSS 시작 시간
        object HssRunningTime : Action(0xB4)            // HSS 구동 시간
        object HssBrakeTimeFixed : Action(0xB5)         // HSS 제동 시간(고정)
        object HssComCurrentPhaseOffset : Action(0xB6)  // HSS 전류 위상 오프셋
        object HssStartingCurrentMax : Action(0xB7)     // HSS 시작 최대 전류
        object HssStartingCurrentMin : Action(0xB8)     // HSS 시작 최소 전류
        object HssRunningCurrentMax : Action(0xB9)      // HSS 구동 최대 전류
        object HssRunningCurrentMin : Action(0xBA)      // HSS 구동 최소 전류
        object HssPwmFrequency : Action(0xBB)           // HSS PWM 주파수
        object HssHighOpLimits : Action(0xBC)           // HSS 동작 상한 제한
        object HssInputVoltageMax : Action(0xBD)        // HSS 입력 전압 최대
        object HssInputVoltageMin : Action(0xBE)        // HSS 입력 전압 최소
        object HssHighOpLimitChanges : Action(0xBF)     // HSS 상한 제한 변경
        object HssBrakeTimeVariable : Action(0xCD)      // HSS 제동 시간(가변)
        object HssBrakeMode : Action(0xCE)              // HSS 제동 모드
        object HssSineDistortion : Action(0xCF)         // HSS 사인파 왜곡

        // 펌웨어 업데이트 관련 명령어
        object FwUpdateCheck : Action(0xD0)             // 펌웨어 업데이트 확인
        object FwUpdateFile : Action(0xD1)              // 펌웨어 업데이트 파일
        object FwUpdateEnter : Action(0xD2)             // 펌웨어 업데이트 모드 진입
        object FwUpdateStatus : Action(0xD3)            // 펌웨어 업데이트 상태
        object FwUpdateDownload : Action(0xD4)          // 펌웨어 업데이트 다운로드
        object FwUpdateNotify : Action(0xD5)            // 펌웨어 업데이트 알림

        // 기타 명령어
        object TKvExposureModeSet : Action(0xA0)        // kV 노출 모드 설정
        object TKvExposureModeStop : Action(0xA1)       // kV 노출 모드 중지
        object GenLogPop : Action(0xA5)                 // 제너레이터 로그 조회
        object PowerSource : Action(0x73)               // 전원 소스
        object ZeroCrossCount : Action(0x74)            // 제로 크로스 카운트
        object ErrorDelete : Action(0xE1)               // 오류 삭제
        object WarningDelete : Action(0xE3)             // 경고 삭제
        object TArcingOccur : Action(0xE6)              // 아킹 발생
        object TPhySwitchReject : Action(0xE7)          // 물리적 스위치 거부
        object TOpconStatus : Action(0xE8)              // 작동 콘솔 상태
        object TBoardVersion : Action(0xF1)             // 보드 버전
        object OpTimeSync : Action(0xF5)                // 시간 동기화
        object BaudRate : Action(0xFE)                  // 통신 속도

        // 스코프 관련 명령어
        object FbScope : Action(0x34)                   // 피드백 스코프
        object FbScopeConfig : Action(0x35)             // 피드백 스코프 설정
        object FbScopeDataCurrent : Action(0x36)        // 피드백 스코프 전류 데이터
        object FbScopeDataDuplicate : Action(0x37)      // 피드백 스코프 중복 데이터
        object FbScopeDataMa : Action(0x38)             // 피드백 스코프 mA 데이터
        object FbScopeDataKvMa : Action(0x39)           // 피드백 스코프 kV/mA 데이터
        object FbScopeDataStatus : Action(0x3A)         // 피드백 스코프 상태 데이터
        object FbScopeDivScale : Action(0x3B)           // 피드백 스코프 스케일

        // 기타 보조 명령어
        object AuxOnOff : Action(0x55)                  // 보조 On/Off
        object SysReq : Action(0x5F)                    // 시스템 요청
        object OverMaRef : Action(0x17)                 // 과전류 참조값
        object OverKvRef : Action(0x18)                 // 과전압 참조값
        object FootSwitchDelayTime : Action(0x1A)       // 풋스위치 지연 시간
        object InputSourcePhase : Action(0x23)          // 입력 소스 위상
        object OverMaAllowValue : Action(0x25)          // 과전류 허용값
        object DisSetTimeValue : Action(0x48)           // 시간 값 표시 설정
    }
}