package com.androidkotlin.generatorprokt.data.repository

import com.androidkotlin.generatorprokt.data.device.Serial422Device
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
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Serial422RepositoryImpl @Inject constructor(
    private val serial422Device: Serial422Device,
    private val ioDispatcher: CoroutineDispatcher
) : Serial422Repository {

    // 응답 처리를 위한 콜백 플로우
    private val responseFlow = callbackFlow {
        serial422Device.onDataReceived = { buffer, size ->
            try {
                val response = parseResponse(buffer, size)
                trySend(response)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing response")
                trySend(SerialResponse.Error(e))
            }
        }

        awaitClose {
            serial422Device.onDataReceived = null
        }
    }

    override suspend fun connect(): Result<Unit> {
        return serial422Device.connect()
    }

    override suspend fun disconnect() {
        serial422Device.disconnect()
    }

    override suspend fun sendPacket(packet: SerialPacket): Result<SerialResponse> = withContext(ioDispatcher) {
        try {
            val serializedPacket = serializePacket(packet)
            val result = serial422Device.sendData(serializedPacket)

            if (result.isSuccess) {
                // 실제 구현에서는 응답을 기다려야 함
                // 여기서는 단순화를 위해 성공 응답을 반환
                Result.success(SerialResponse.Success(
                    controlCommand = packet.controlCommand.value,
                    actionCommand = packet.actionCommand.value,
                    data = null
                ))
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending packet")
            Result.failure(e)
        }
    }

    override fun receiveData(): Flow<SerialResponse> = responseFlow

    override fun isConnected(): Boolean = serial422Device.isConnected()

    /**
     * 패킷을 바이트 배열로 직렬화
     */
    private fun serializePacket(packet: SerialPacket): ByteArray {
        val dataLength = packet.data?.size ?: 0
        val totalLength = 10 + dataLength // 헤더(8) + 데이터 + 체크섬(1) + 종료(1)

        val result = ByteArray(totalLength)

        // 프로토콜 헤더 설정
        result[0] = SerialPacket.PROTOCOL_FIRST
        result[1] = SerialPacket.PROTOCOL_SECOND
        result[2] = packet.targetId
        result[3] = packet.sourceId

        // 패킷 길이 설정 (2바이트)
        val packetLength = 8 + dataLength
        result[4] = ((packetLength shr 8) and 0xFF).toByte()
        result[5] = (packetLength and 0xFF).toByte()

        // 명령어 설정
        result[6] = packet.controlCommand.value.toByte()
        result[7] = packet.actionCommand.value.toByte()

        // 데이터 복사
        packet.data?.forEachIndexed { index, byte ->
            result[8 + index] = byte
        }

        // 체크섬 계산 및 설정
        var checksum: Byte = 0
        for (i in 0 until 8 + dataLength) {
            checksum = (checksum + result[i]).toByte()
        }
        result[8 + dataLength] = checksum

        // 종료 바이트 설정
        result[9 + dataLength] = SerialPacket.PROTOCOL_END

        return result
    }

    /**
     * 수신된 바이트 배열을 응답 객체로 파싱
     */
    private fun parseResponse(buffer: ByteArray, size: Int): SerialResponse {
        // 패킷 유효성 검사 (간단한 구현)
        if (size < 10 || buffer[0] != SerialPacket.PROTOCOL_FIRST || buffer[1] != SerialPacket.PROTOCOL_SECOND) {
            return SerialResponse.Error(Exception("Invalid packet format"))
        }

        // 체크섬 검증
        val dataLength = (((buffer[4].toInt() and 0xFF) shl 8) or (buffer[5].toInt() and 0xFF)) - 8
        var calculatedChecksum: Byte = 0

        for (i in 0 until 8 + dataLength) {
            calculatedChecksum = (calculatedChecksum + buffer[i]).toByte()
        }

        if (calculatedChecksum != buffer[8 + dataLength]) {
            return SerialResponse.Error(Exception("Checksum mismatch"))
        }

        // 컨트롤 및 액션 명령어 추출
        val controlCommand = buffer[6].toInt() and 0xFF
        val actionCommand = buffer[7].toInt() and 0xFF

        // 데이터 추출
        val data = if (dataLength > 0) {
            ByteArray(dataLength).apply {
                System.arraycopy(buffer, 8, this, 0, dataLength)
            }
        } else null

        return SerialResponse.Success(
            controlCommand = controlCommand,
            actionCommand = actionCommand,
            data = data
        )
    }
}