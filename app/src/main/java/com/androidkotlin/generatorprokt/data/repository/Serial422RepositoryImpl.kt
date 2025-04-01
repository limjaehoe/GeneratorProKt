package com.androidkotlin.generatorprokt.data.repository

import com.androidkotlin.generatorprokt.data.device.Serial422Device
import com.androidkotlin.generatorprokt.data.device.SerialPacketHandler
import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialPacket
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Serial422RepositoryImpl @Inject constructor(
    private val serial422Device: Serial422Device,
    private val ioDispatcher: CoroutineDispatcher
) : Serial422Repository {

    // 응답 처리를 위한 콜백 플로우
    private val responseFlow = callbackFlow {
        Timber.d("응답 처리 콜백 플로우 초기화")

        //Timber.d("woghl1129:"+serial422Device.onDataReceived)
        serial422Device.onDataReceived = { buffer, size ->
            try {
                Timber.d("${size}바이트 데이터 수신됨")
                val response = SerialPacketHandler.parseResponse(buffer, size)
                //Timber.d("woghl1129:"+response.toString())

                when (response) {
                    is SerialResponse.Success -> {
                        // 응답 데이터 해석
                        val interpreted = SerialPacketHandler.interpretResponse(response)
                        if (interpreted != null) {
                            Timber.d("응답 해석 결과: $interpreted")
                        }
                    }
                    is SerialResponse.Error -> {
                        Timber.e("응답 오류: ${response.exception.message}")
                    }
                    is SerialResponse.Timeout -> {
                        Timber.w("응답 시간 초과")
                    }
                }

                trySend(response)
            } catch (e: Exception) {
                Timber.e(e, "응답 파싱 중 오류 발생")
                trySend(SerialResponse.Error(e))
            }
        }

        awaitClose {
            Timber.d("응답 처리 콜백 플로우 종료")
            serial422Device.onDataReceived = null
        }
    }

    override suspend fun connect(): Result<Unit> {
        Timber.d("connect() 호출됨")
        return try {
            val result = serial422Device.connect()

            if (result.isSuccess) {
                Timber.d("장치 연결 성공")

                // 연결 성공 후 초기화 요청 전송
                sendInitialRequest()
            } else {
                val exception = result.exceptionOrNull() ?: Exception("알 수 없는 오류")
                Timber.e(exception, "장치 연결 실패")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "장치 연결 중 예외 발생")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        Timber.d("disconnect() 호출됨")
        try {
            val result = serial422Device.disconnect()

            if (result.isSuccess) {
                Timber.d("장치 연결 해제 성공")
            } else {
                val exception = result.exceptionOrNull() ?: Exception("알 수 없는 오류")
                Timber.e(exception, "장치 연결 해제 실패")
            }
        } catch (e: Exception) {
            Timber.e(e, "장치 연결 해제 중 예외 발생")
        }
    }

    override suspend fun sendPacket(packet: SerialPacket): Result<SerialResponse> = withContext(ioDispatcher) {
        Timber.d("sendPacket() 호출됨 - Control: ${packet.controlCommand.javaClass.simpleName}, Action: ${packet.actionCommand.javaClass.simpleName}")

        try {
            if (!isConnected()) {
                Timber.e("장치가 연결되어 있지 않아 패킷을 보낼 수 없습니다")
                return@withContext Result.failure(Exception("장치가 연결되어 있지 않습니다"))
            }

            // 패킷 직렬화 직전 로그
            Timber.d("woghl1129: 패킷 직렬화 시작 - Control: ${packet.controlCommand.value}, Action: ${packet.actionCommand.value}")

            val serializedPacket = SerialPacketHandler.serializePacket(packet)
            //Timber.d("패킷 직렬화 완료: ${SerialPacketHandler.bytesToHexString(serializedPacket)}")
            Timber.d("woghl1129: 패킷 직렬화 완료 - ${SerialPacketHandler.bytesToHexString(serializedPacket)}")
            Timber.d("woghl1129: 패킷 전송 시작")

            val result = serial422Device.sendData(serializedPacket)

            // 패킷 전송 직후 로그
            Timber.d("woghl1129: 패킷 전송 완료 - 결과: ${result.isSuccess}")

            if (result.isSuccess) {
                Timber.d("패킷 전송 성공")
                Timber.d("woghl1129: 패킷 전송 성공 - 응답 대기 중")

                // TODO: 실제 구현에서는 응답을 기다려야 함
                // 여기서는 단순화를 위해 성공 응답을 바로 반환
                Result.success(SerialResponse.Success(
                    controlCommand = packet.controlCommand.value,
                    actionCommand = packet.actionCommand.value,
                    data = null
                ))
            } else {
                val exception = result.exceptionOrNull() ?: Exception("알 수 없는 오류")
                Timber.e(exception, "패킷 전송 실패")
                Timber.e("woghl1129: 패킷 전송 실패 - ${exception.message}")
                Result.failure(exception)
            }
        } catch (e: Exception) {
            Timber.e(e, "패킷 전송 중 예외 발생")
            Timber.e("woghl1129: 패킷 전송 중 예외 - ${e.message}")
            Result.failure(e)
        }
    }

    override fun receiveData(): Flow<SerialResponse> {
        Timber.d("receiveData() 호출됨")
        return responseFlow
    }

    override fun isConnected(): Boolean {
        val connected = serial422Device.isConnected()
        Timber.d("isConnected() = $connected")
        return connected
    }

    /**
     * 초기화 요청 패킷 전송
     */
    private suspend fun sendInitialRequest() {
        try {
            Timber.d("초기화 요청 전송 중...")

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommGetInfo,
                actionCommand = SerialCommand.Action.InitialReq,
                data = null,
                targetId = SerialPacket.TARGET_MAIN,    // 0x24
                sourceId = SerialPacket.SOURCE_CONSOLE  // 0x25
            )

            val result = sendPacket(packet)

            if (result.isSuccess) {
                Timber.d("초기화 요청 전송 성공")
            } else {
                Timber.e("초기화 요청 전송 실패")
            }
        } catch (e: Exception) {
            Timber.e(e, "초기화 요청 전송 중 예외 발생")
        }
    }

    /**
     * 하트비트 패킷 전송
     */
    override suspend fun sendHeartbeat(): Result<SerialResponse> {
        try {
            Timber.d("하트비트 전송 중...")

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.HeartBeat,
                data = null,
                targetId = SerialPacket.TARGET_MAIN,    // 0x24
                sourceId = SerialPacket.SOURCE_CONSOLE  // 0x25
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "하트비트 전송 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * kV 값 설정 패킷 전송
     */
    override suspend fun setKvValue(kvValue: Int): Result<SerialResponse> {
        try {
            Timber.d("kV 값 설정 중: $kvValue")

            // kV 값은 10배로 전송 (123.4kV -> 1234)
            val kvData = SerialPacketHandler.createUShortData(kvValue * 10)

            // DAC 값은 기기 의존적이므로 임시로 0 설정
            val dacData = SerialPacketHandler.createUShortData(0)

            // 최종 데이터 구성 (4바이트: kV값 2바이트 + DAC값 2바이트)
            val data = ByteArray(4)
            System.arraycopy(kvData, 0, data, 0, 2)
            System.arraycopy(dacData, 0, data, 2, 2)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommControl,
                actionCommand = SerialCommand.Action.KvValue,
                data = data,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "kV 값 설정 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * mA 값 설정 패킷 전송
     */
    override suspend fun setMaValue(maValue: Int): Result<SerialResponse> {
        try {
            Timber.d("mA 값 설정 중: $maValue")

            // mA 값은 10배로 전송 (123.4mA -> 1234)
            val maData = SerialPacketHandler.createUShortData(maValue * 10)

            // DAC 값은 기기 의존적이므로 임시로 0 설정
            val dacData = SerialPacketHandler.createUShortData(0)

            // 최종 데이터 구성 (4바이트: mA값 2바이트 + DAC값 2바이트)
            val data = ByteArray(4)
            System.arraycopy(maData, 0, data, 0, 2)
            System.arraycopy(dacData, 0, data, 2, 2)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommControl,
                actionCommand = SerialCommand.Action.MaValue,
                data = data,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "mA 값 설정 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 노출 시간 값 설정 패킷 전송
     */
    override suspend fun setTimeValue(timeMs: Int): Result<SerialResponse> {
        try {
            Timber.d("노출 시간 설정 중: $timeMs ms")

            val timeData = SerialPacketHandler.createUShortData(timeMs)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommControl,
                actionCommand = SerialCommand.Action.TimeValue,
                data = timeData,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "노출 시간 설정 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 동작 모드 설정 패킷 전송
     */
    override suspend fun setMode(mode: Int): Result<SerialResponse> {
        try {
            val modeStr = when(mode) {
                0 -> "Manual"
                1 -> "AEC"
                2 -> "Manual_Continuous"
                else -> "Unknown"
            }
            Timber.d("동작 모드 설정 중: $mode ($modeStr)")

            val modeData = SerialPacketHandler.createUCharData(mode)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommControl,
                actionCommand = SerialCommand.Action.Mode,
                data = modeData,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "동작 모드 설정 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 포커스 설정 패킷 전송 (0: Large, 1: Small)
     */
    override suspend fun setFocus(smallFocus: Boolean): Result<SerialResponse> {
        try {
            val focusValue = if (smallFocus) 1 else 0
            val focusStr = if (smallFocus) "Small" else "Large"
            Timber.d("포커스 설정 중: $focusValue ($focusStr)")

            val focusData = SerialPacketHandler.createUCharData(focusValue)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommControl,
                actionCommand = SerialCommand.Action.Focus,
                data = focusData,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "포커스 설정 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 버키 선택 패킷 전송 (0: Non Bucky, 1: Bucky 1, 2: Bucky 2)
     */
    override suspend fun selectBucky(buckyIndex: Int): Result<SerialResponse> {
        try {
            Timber.d("버키 선택 중: $buckyIndex")

            val buckyData = SerialPacketHandler.createUCharData(buckyIndex)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommControl,
                actionCommand = SerialCommand.Action.BuckySelect,
                data = buckyData,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "버키 선택 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 전원 진단 요청 패킷 전송
     */
    override suspend fun requestPowerDiagnosis(type: Int): Result<SerialResponse> {
        try {
            val typeStr = when(type) {
                0 -> "Power 3.3V"
                1 -> "Power 5V"
                2 -> "Power +12V"
                3 -> "Power -12V"
                4 -> "Frequency"
                5 -> "DC Link"
                6 -> "Filament Current(Preheat)"
                7 -> "Rotor Current(Starting)"
                9 -> "Filament Current(Preheat, Small/Large)"
                else -> "Unknown Type $type"
            }
            Timber.d("전원 진단 요청 중: $type ($typeStr)")

            val typeData = SerialPacketHandler.createUCharData(type)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.PowerDiagnosis,
                data = typeData,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "전원 진단 요청 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 보드 버전 정보 요청 패킷 전송
     */
    override suspend fun requestBoardVersion(type: Int): Result<SerialResponse> {
        try {
            val typeStr = when(type) {
                0 -> "Main Board App Version"
                1 -> "AEC Board PCB Version"
                2 -> "DC Link Board PCB Version"
                3 -> "Filament Board PCB Version"
                4 -> "Interface Board PCB Version"
                5 -> "LSSBrake Board PCB Version"
                6 -> "Main Board PCB Version"
                7 -> "HSS Board PCB Version"
                8 -> "HSS Board App Version"
                else -> "Unknown Type $type"
            }
            Timber.d("보드 버전 정보 요청 중: $type ($typeStr)")

            val typeData = SerialPacketHandler.createUCharData(type)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.TBoardVersion,
                data = typeData,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "보드 버전 정보 요청 중 예외 발생")
            return Result.failure(e)
        }
    }



    /**
     * Ready 상태 전환 명령 전송 (1: 켜기, 0: 끄기)
     */
    suspend fun sendReadyCommand(on: Boolean): Result<SerialResponse> {
        try {
            Timber.d("Ready 명령 전송 중: ${if (on) "ON" else "OFF"}")

            val value = if (on) 1 else 0
            val data = SerialPacketHandler.createUCharData(value)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.ReadySw,
                data = data,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "Ready 명령 전송 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * Expose 상태 전환 명령 전송 (1: 켜기, 0: 끄기)
     */
    suspend fun sendExposeCommand(on: Boolean): Result<SerialResponse> {
        try {
            Timber.d("Expose 명령 전송 중: ${if (on) "ON" else "OFF"}")

            val value = if (on) 1 else 0
            val data = SerialPacketHandler.createUCharData(value)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.ExposureSw,
                data = data,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "Expose 명령 전송 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * 시스템 상태 변경 명령 전송
     * @param status 원하는 시스템 상태 코드 (예: 4 = MAIN_MODE_EXPOSURE_READY)
     */
    suspend fun sendSystemStatus(status: Int): Result<SerialResponse> {
        try {
            Timber.d("시스템 상태 변경 명령 전송: $status")

            val data = SerialPacketHandler.createUCharData(status)

            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.SystemStatus,
                data = data,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            return sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "시스템 상태 변경 명령 전송 중 예외 발생")
            return Result.failure(e)
        }
    }
}