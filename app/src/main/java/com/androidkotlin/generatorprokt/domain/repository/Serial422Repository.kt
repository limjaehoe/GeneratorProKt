package com.androidkotlin.generatorprokt.domain.repository

import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialPacket
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import kotlinx.coroutines.flow.Flow

/**
 * 422 시리얼 통신을 위한 Repository 인터페이스
 */
interface Serial422Repository {
    /**
     * 시리얼 포트 연결
     * @return 연결 결과
     */
    suspend fun connect(): Result<Unit>

    /**
     * 시리얼 포트 연결 해제
     */
    suspend fun disconnect()

    /**
     * 명령 패킷 전송
     * @param packet 전송할 패킷
     * @return 응답 결과
     */
    suspend fun sendPacket(packet: SerialPacket): Result<SerialResponse>

    /**
     * 수신된 데이터를 Flow로 제공
     * @return 수신 데이터 Flow
     */
    fun receiveData(): Flow<SerialResponse>

    /**
     * 현재 시리얼 포트 연결 상태 확인
     * @return 연결 상태
     */
    fun isConnected(): Boolean
}