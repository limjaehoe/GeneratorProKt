package com.androidkotlin.generatorprokt.domain.repository

import com.androidkotlin.generatorprokt.domain.model.MainMode
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import kotlinx.coroutines.flow.Flow

/**
 * 발전기 상태 및 통신을 담당하는 리포지토리 인터페이스
 */
interface GeneratorRepository {
    /**
     * 현재 발전기 모드를 관찰하는 Flow 반환
     */
    fun observeMainMode(): Flow<MainMode>

    /**
     * 현재 발전기 모드 가져오기
     */
    suspend fun getCurrentMode(): MainMode

    /**
     * 발전기 모드 변경 요청
     * @param mode 변경하려는 모드
     * @return 성공 또는 실패 결과
     */
    suspend fun setMode(mode: MainMode): Result<SerialResponse>

    /**
     * 발전기 모드 변경 요청 (raw 값 사용)
     * @param hexValue 변경하려는 모드의 16진수 값
     * @return 성공 또는 실패 결과
     */
    suspend fun setModeByHexValue(hexValue: Int): Result<SerialResponse>

    /**
     * 발전기 상태 (MAIN_MODE) 요청
     * @return 성공 또는 실패 결과
     */
    suspend fun requestSystemStatus(): Result<SerialResponse>
}