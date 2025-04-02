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

    /**
     * 하트비트 패킷 전송
     * @return 응답 결과
     */
    suspend fun sendHeartbeat(): Result<SerialResponse>

    /**
     * kV 값 설정 패킷 전송
     * @param kvValue kV 값 (예: 123은 12.3kV)
     * @return 응답 결과
     */
    suspend fun setKvValue(kvValue: Int): Result<SerialResponse>

    /**
     * mA 값 설정 패킷 전송
     * @param maValue mA 값 (예: 123은 12.3mA)
     * @return 응답 결과
     */
    suspend fun setMaValue(maValue: Int): Result<SerialResponse>

    /**
     * 노출 시간 값 설정 패킷 전송
     * @param timeMs 노출 시간(밀리초)
     * @return 응답 결과
     */
    suspend fun setTimeValue(timeMs: Int): Result<SerialResponse>

    /**
     * 동작 모드 설정 패킷 전송
     * @param mode 모드 값 (0: Manual, 1: AEC, 2: Manual_Continuous)
     * @return 응답 결과
     */
    suspend fun setMode(mode: Int): Result<SerialResponse>

    /**
     * 포커스 설정 패킷 전송
     * @param smallFocus true일 경우 Small 포커스, false일 경우 Large 포커스
     * @return 응답 결과
     */
    suspend fun setFocus(smallFocus: Boolean): Result<SerialResponse>

    /**
     * 버키 선택 패킷 전송
     * @param buckyIndex 버키 인덱스 (0: Non Bucky, 1: Bucky 1, 2: Bucky 2)
     * @return 응답 결과
     */
    suspend fun selectBucky(buckyIndex: Int): Result<SerialResponse>

    /**
     * 전원 진단 요청 패킷 전송
     * @param type 진단 유형 (0: 3.3V, 1: 5V, 2: +12V, 3: -12V, 4: Frequency, 5: DC Link,
     *              6: Filament Current(Preheat), 7: Rotor Current(Starting),
     *              9: Filament Current(Preheat, Small/Large))
     * @return 응답 결과
     */
    suspend fun requestPowerDiagnosis(type: Int): Result<SerialResponse>

    /**
     * 보드 버전 정보 요청 패킷 전송
     * @param type 버전 정보 유형 (0: Main Board App, 1: AEC Board PCB, 2: DC Link Board PCB,
     *              3: Filament Board PCB, 4: Interface Board PCB, 5: LSSBrake Board PCB,
     *              6: Main Board PCB, 7: HSS Board PCB, 8: HSS Board App)
     * @return 응답 결과
     */
    suspend fun requestBoardVersion(type: Int): Result<SerialResponse>

    /**
     * 시스템 상태 변경 명령 전송
     * @param status 원하는 시스템 상태 코드 (예: 3 = MAIN_MODE_STANDBY)
     * @return 응답 결과
     */
    suspend fun sendSystemStatus(status: Int): Result<SerialResponse>
}