package com.androidkotlin.generatorprokt.domain.model

/**
 * 발전기 메인 모드 상태를 정의하는 Enum 클래스
 * 각 모드는 고유한 헥사값과 설명을 가짐
 */
enum class MainMode(val hexValue: Int, val korDescription: String) {
    NONE(0x00, "모드 없음"),
    BOOT(0x01, "부트모드"),
    INIT(0x02, "초기화 모드"),
    STANDBY(0x03, "대기 모드"),
    EXPOSURE_READY(0x04, "Exposure Ready"),
    EXPOSURE_READY_DONE(0x05, "Exposure Ready 완료"),
    EXPOSURE(0x06, "Exposure 모드"),
    EXPOSURE_DONE(0x07, "Exposure 완료"),
    EXPOSURE_RELEASE(0x08, "모드"),

    RESET(0x0A, "재부팅 모드"),
    //SYNC(0x0C, ""),
    TECHNICAL_MODE(0x0D, "테크니컬 모드 : 개발 테스트 진행"),
    //RE_CONFIG(0x1F, ""),
    EMERGENCY(0x40, "Emergency Button 눌러졌을경우 6번 Bit 1 설정, 해제시 STANDBY모드"),
    ERROR(0x80, "Error 시 7번 Bit 1설정, 해제시 STANDBY 모드");

    companion object {
        /**
         * 헥사값으로 모드 찾기
         */
        fun fromHexValue(hexValue: Int): MainMode {
            return values().find { it.hexValue == hexValue } ?: NONE
        }

        /**
         * 특별 모드 처리 (비트 연산 필요한 경우)
         * MODE + 14bit 1, MODE + 15bit 1 처리
         */
        fun fromRawValue(rawValue: Int): MainMode {
            // 비트 연산으로 기본 모드 값 추출
            // 이미지에 따르면, EMERGENCY와 ERROR는 특별한 비트 패턴을 가짐

            if ((rawValue and 0x40) == 0x40) {
                return EMERGENCY
            }

            if ((rawValue and 0x80) == 0x80) {
                return ERROR
            }

            // 그 외의 경우에는 기본 헥사값으로 모드 찾기
            val basicMode = rawValue and 0x1F  // 하위 5비트만 고려
            return fromHexValue(basicMode)
        }
    }
}