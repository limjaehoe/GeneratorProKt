package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import timber.log.Timber
import javax.inject.Inject

/**
 * 발전기에 명령을 전송하는 UseCase
 */
class SendGeneratorCommandUseCase @Inject constructor(
    private val serial422Repository: Serial422Repository
) {
    /**
     * 하트비트 전송 (통신 연결 확인)
     */
    suspend operator fun invoke(): Result<SerialResponse> {
        Timber.d("하트비트 전송 UseCase 실행")
        return serial422Repository.sendHeartbeat()
    }

    /**
     * kV 값 설정
     * @param kvValue kV 값 (예: 123은 12.3kV)
     */
    suspend fun setKvValue(kvValue: Int): Result<SerialResponse> {
        Timber.d("kV 값 설정 UseCase 실행: $kvValue")

        // 유효한 kV 범위 검증 (4.0kV ~ 15.0kV)
        if (kvValue < 40 || kvValue > 150) {
            Timber.e("유효하지 않은 kV 값: $kvValue")
            return Result.failure(IllegalArgumentException("kV 값은 40에서 150 사이여야 합니다 (4.0~15.0kV)"))
        }

        return serial422Repository.setKvValue(kvValue)
    }

    /**
     * mA 값 설정
     * @param maValue mA 값 (예: 123은 12.3mA)
     */
    suspend fun setMaValue(maValue: Int): Result<SerialResponse> {
        Timber.d("mA 값 설정 UseCase 실행: $maValue")

        // 유효한 mA 범위 검증 (1.0mA ~ 50.0mA)
        if (maValue < 10 || maValue > 500) {
            Timber.e("유효하지 않은 mA 값: $maValue")
            return Result.failure(IllegalArgumentException("mA 값은 10에서 500 사이여야 합니다 (1.0~50.0mA)"))
        }

        return serial422Repository.setMaValue(maValue)
    }

    /**
     * 노출 시간 설정
     * @param timeMs 노출 시간(밀리초)
     */
    suspend fun setTimeValue(timeMs: Int): Result<SerialResponse> {
        Timber.d("노출 시간 설정 UseCase 실행: $timeMs ms")

        // 유효한 시간 범위 검증 (1ms ~ 10000ms)
        if (timeMs < 1 || timeMs > 10000) {
            Timber.e("유효하지 않은 노출 시간: $timeMs ms")
            return Result.failure(IllegalArgumentException("노출 시간은 1ms에서 10000ms 사이여야 합니다"))
        }

        return serial422Repository.setTimeValue(timeMs)
    }

    /**
     * 동작 모드 설정
     * @param mode 모드 (0: Manual, 1: AEC, 2: Manual_Continuous)
     */
    suspend fun setMode(mode: Int): Result<SerialResponse> {
        Timber.d("동작 모드 설정 UseCase 실행: $mode")

        // 유효한 모드 검증
        if (mode < 0 || mode > 2) {
            Timber.e("유효하지 않은 동작 모드: $mode")
            return Result.failure(IllegalArgumentException("동작 모드는 0(Manual), 1(AEC), 2(Manual_Continuous) 중 하나여야 합니다"))
        }

        return serial422Repository.setMode(mode)
    }

    /**
     * 포커스 설정
     * @param smallFocus true일 경우 Small 포커스, false일 경우 Large 포커스
     */
    suspend fun setFocus(smallFocus: Boolean): Result<SerialResponse> {
        val focusStr = if (smallFocus) "Small" else "Large"
        Timber.d("포커스 설정 UseCase 실행: $focusStr")
        return serial422Repository.setFocus(smallFocus)
    }

    /**
     * 버키 선택
     * @param buckyIndex 버키 인덱스 (0: Non Bucky, 1: Bucky 1, 2: Bucky 2)
     */
    suspend fun selectBucky(buckyIndex: Int): Result<SerialResponse> {
        Timber.d("버키 선택 UseCase 실행: $buckyIndex")

        // 유효한 버키 인덱스 검증
        if (buckyIndex < 0 || buckyIndex > 2) {
            Timber.e("유효하지 않은 버키 인덱스: $buckyIndex")
            return Result.failure(IllegalArgumentException("버키 인덱스는 0(Non Bucky), 1(Bucky 1), 2(Bucky 2) 중 하나여야 합니다"))
        }

        return serial422Repository.selectBucky(buckyIndex)
    }

    /**
     * 전원 진단 요청
     * @param type 진단 유형
     */
    suspend fun requestPowerDiagnosis(type: Int): Result<SerialResponse> {
        Timber.d("전원 진단 요청 UseCase 실행: 유형 $type")

        // 유효한 진단 유형 검증
        if (type < 0 || type > 9 || type == 8) {
            Timber.e("유효하지 않은 진단 유형: $type")
            return Result.failure(IllegalArgumentException("유효하지 않은 진단 유형입니다"))
        }

        return serial422Repository.requestPowerDiagnosis(type)
    }

    /**
     * 보드 버전 정보 요청
     * @param type 버전 정보 유형
     */
    suspend fun requestBoardVersion(type: Int): Result<SerialResponse> {
        Timber.d("보드 버전 정보 요청 UseCase 실행: 유형 $type")

        // 유효한 버전 정보 유형 검증
        if (type < 0 || type > 8) {
            Timber.e("유효하지 않은 버전 정보 유형: $type")
            return Result.failure(IllegalArgumentException("유효하지 않은 버전 정보 유형입니다"))
        }

        return serial422Repository.requestBoardVersion(type)
    }
}