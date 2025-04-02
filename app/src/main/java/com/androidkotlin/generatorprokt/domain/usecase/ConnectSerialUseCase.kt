package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import timber.log.Timber
import javax.inject.Inject

/**
 * 시리얼 포트에 연결하는 UseCase
 */
class ConnectSerialUseCase @Inject constructor(
    private val serial422Repository: Serial422Repository
) {
    /**
     * 시리얼 포트에 연결 시도
     */
    suspend operator fun invoke(): Result<Unit> {
        Timber.d("ConnectSerialUseCase: 연결 시도 중...")

        return try {
            // 이미 연결되어 있는지 확인
            if (isConnected()) {
                Timber.d("ConnectSerialUseCase: 이미 연결되어 있습니다")
                return Result.success(Unit)
            }

            // 연결 시도
            val result = serial422Repository.connect()

            // 연결 결과 로깅
            result.fold(
                onSuccess = {
                    Timber.d("ConnectSerialUseCase: 연결 성공")

                    // 연결 성공 후 하트비트 전송
                    serial422Repository.sendHeartbeat()

                    // 시스템 상태 요청
                    serial422Repository.sendSystemStatus(3) // 대기 모드(STANDBY)
                },
                onFailure = { e ->
                    Timber.e(e, "ConnectSerialUseCase: 연결 실패")
                }
            )

            result
        } catch (e: Exception) {
            Timber.e(e, "ConnectSerialUseCase: 연결 시도 중 예외 발생")
            Result.failure(e)
        }
    }

    /**
     * 현재 연결 상태 확인
     * @return 연결 상태
     */
    fun isConnected(): Boolean {
        val connected = serial422Repository.isConnected()
        Timber.d("ConnectSerialUseCase: 현재 연결 상태 - $connected")
        return connected
    }
}