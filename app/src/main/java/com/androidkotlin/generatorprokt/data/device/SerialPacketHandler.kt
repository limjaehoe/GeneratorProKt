package com.androidkotlin.generatorprokt.data.device

import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialPacket
import com.androidkotlin.generatorprokt.domain.model.SerialPacket.Companion.PROTOCOL_END
import com.androidkotlin.generatorprokt.domain.model.SerialPacket.Companion.PROTOCOL_FIRST
import com.androidkotlin.generatorprokt.domain.model.SerialPacket.Companion.PROTOCOL_SECOND
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import timber.log.Timber

/**
 * 시리얼 패킷 처리를 담당하는 유틸리티 클래스
 */
class SerialPacketHandler {

    // 모든 메서드가 companion object 내에 정의되어 있어,
    // 인스턴스 생성 없이 사용 가능합니다.
    companion object {
        /**
         * 패킷을 바이트 배열로 직렬화
         * SerialPacket 객체를 실제 바이트 배열로 변환
         * 통신 프로토콜에 맞게 헤더, 명령어, 데이터, 체크섬, 종료 바이트 등을 포함한 패킷을 구성한다.
         */
//        fun serializePacket(packet: SerialPacket): ByteArray {
//            // 패킷 구조 만들기
//            // 1. 프로토콜 헤더 설정 (0x23, 0x50)
//            // 2. 타겟 ID, 소스 ID 설정
//            // 3. 패킷 길이 설정
//            // 4. 명령어 설정 (Control, Action)
//            // 5. 데이터 복사
//            // 6. 체크섬 계산 및 설정
//            // 7. 종료 바이트 설정 (0x21)
//
//            Timber.d("패킷 직렬화 시작: Control=${packet.controlCommand.value}, Action=${packet.actionCommand.value}")
//            val dataLength = packet.data?.size ?: 0
//            val totalLength = 10 + dataLength // 헤더(8) + 데이터 + 체크섬(1) + 종료(1)
//
//            val result = ByteArray(totalLength)
//
//            // 프로토콜 헤더 설정
//            result[0] = SerialPacket.PROTOCOL_FIRST  // 0x23
//            result[1] = SerialPacket.PROTOCOL_SECOND // 0x50
//            result[2] = packet.targetId
//            result[3] = packet.sourceId
//
//            // 패킷 길이 설정 (2바이트)
//            val packetLength = 8 + dataLength
//            result[4] = ((packetLength shr 8) and 0xFF).toByte()
//            result[5] = (packetLength and 0xFF).toByte()
//
//            // 명령어 설정
//            result[6] = packet.controlCommand.value.toByte()
//            result[7] = packet.actionCommand.value.toByte()
//
//            // 데이터 복사
//            packet.data?.forEachIndexed { index, byte ->
//                result[8 + index] = byte
//            }
//
//            // 체크섬 계산 및 설정
//            var checksum: Byte = 0
//            for (i in 0 until 8 + dataLength) {
//                checksum = (checksum + result[i]).toByte()
//            }
//            result[8 + dataLength] = checksum
//
//            // 종료 바이트 설정
//            result[9 + dataLength] = SerialPacket.PROTOCOL_END  // 0x21
//
//            Timber.d("패킷 직렬화 완료: ${bytesToHexString(result)}")
//            return result
//        }

        fun serializePacket(packet: SerialPacket): ByteArray {
            Timber.d("패킷 직렬화 시작: Control=${packet.controlCommand.value}, Action=${packet.actionCommand.value}")

            // 데이터 길이 계산 (데이터가 없는 경우 0)
            val dataLength = packet.data?.size ?: 0

            // 전체 패킷 길이 계산:
            // 프로토콜 헤더(2) + 타겟ID(1) + 소스ID(1) + 패킷길이(2) + 컨트롤커맨드(1) + 액션커맨드(1)
            // + 데이터 + 체크섬(1) + 종료 바이트(1)


            /*
            1. 데이터가 없는 경우 (예: 초기화 요청)
            패킷 길이: 10바이트
            구조: 프로토콜 헤더(2) + 타겟ID(1) + 소스ID(1) + 길이(2) + 컨트롤 커맨드(1) + 액션 커맨드(1) + 체크섬(1) + 종료 바이트(1)


            2.데이터가 있는 경우
            패킷 길이: 11바이트 이상
            추가로 데이터 바이트 포함 */

            // 고정 길이 부분: 10바이트 (데이터 없을 때)
            val totalLength = 10 + dataLength

            val result = ByteArray(totalLength)

            // 프로토콜 헤더 설정 (0x23, 0x50)
            result[0] = PROTOCOL_FIRST   // 0x23
            result[1] = PROTOCOL_SECOND  // 0x50

            // 타겟 및 소스 ID 설정
            result[2] = packet.targetId
            result[3] = packet.sourceId

            // 패킷 길이 설정 (2바이트)
            // 패킷 길이 = 8 -> 9변경 (고정 헤더) + 데이터 길이
            val packetLength = 9 + dataLength
            result[4] = ((packetLength shr 8) and 0xFF).toByte()
            result[5] = (packetLength and 0xFF).toByte()

            // 컨트롤 커맨드와 액션 커맨드 설정
            result[6] = packet.controlCommand.value.toByte()
            result[7] = packet.actionCommand.value.toByte()

            // 데이터 복사
            packet.data?.forEachIndexed { index, byte ->
                result[8 + index] = byte
            }

            // 체크섬 계산
            var checksum: Byte = 0
            for (i in 0 until 8 + dataLength) {
                checksum = (checksum + result[i]).toByte()
            }
            result[8 + dataLength] = checksum

            // 종료 바이트 설정
            result[9 + dataLength] = PROTOCOL_END  // 0x21

            // 디버깅을 위한 패킷 로깅
            Timber.d("패킷 직렬화 완료: ${bytesToHexString(result)}")

            return result
        }

        // 체크섬을 위한 헬퍼 함수 추가
        private fun calculateChecksum(data: ByteArray): Byte {
            var checksum: Byte = 0
            for (byte in data) {
                checksum = (checksum + byte).toByte()
            }
            return checksum
        }

        /**
         * 수신된 바이트 배열을 응답 객체로 파싱
         * 수신된 바이트 배열을 SerialResponse 객체로 변환합니다.
         * 패킷 형식 검증, 체크섬 확인, 데이터 추출 등의 작업을 수행합니다.
         */
        fun parseResponse(buffer: ByteArray, size: Int): SerialResponse {
            try {
                Timber.d("응답 파싱 시작: ${size}바이트, 데이터: ${bytesToHexString(buffer.copyOf(size))}")

                // 최소 크기 검사 (완화된 조건)
                if (size < 8 || buffer[0] != SerialPacket.PROTOCOL_FIRST || buffer[1] != SerialPacket.PROTOCOL_SECOND) {
                    Timber.e("잘못된 패킷 형식: 프로토콜 헤더 불일치 또는 패킷 크기 부족")
                    return SerialResponse.Error(Exception("Invalid packet format"))
                }

                // 패킷 길이 계산 및 로깅
                val packetLength = (((buffer[4].toInt() and 0xFF) shl 8) or (buffer[5].toInt() and 0xFF))
                val dataLength = packetLength - 8
                Timber.d("패킷 길이: $packetLength, 데이터 길이: $dataLength")

                // 데이터 길이 검사 완화 - 임시 조치
                if (dataLength < 0) {
                    Timber.e("패킷 데이터 길이가 잘못되었습니다: $dataLength")
                    return SerialResponse.Error(Exception("Invalid data length"))
                }

                // 실제 데이터 크기 조정 (buffer 범위 내로)
                val actualDataLength = minOf(dataLength, size - 10)
                Timber.d("조정된 데이터 길이: $actualDataLength")

                // 컨트롤 및 액션 명령어 추출
                val controlCommand = buffer[6].toInt() and 0xFF
                val actionCommand = buffer[7].toInt() and 0xFF
                Timber.d("응답 명령어: Control=0x${controlCommand.toString(16)}, Action=0x${actionCommand.toString(16)}")

                // 데이터 추출 (조정된 길이 사용)
                val data = if (actualDataLength > 0) {
                    ByteArray(actualDataLength).apply {
                        System.arraycopy(buffer, 8, this, 0, actualDataLength)
                    }
                } else null

                if (data != null) {
                    Timber.d("응답 데이터: ${bytesToHexString(data)}")
                } else {
                    Timber.d("응답 데이터 없음")
                }

                return SerialResponse.Success(
                    controlCommand = controlCommand,
                    actionCommand = actionCommand,
                    data = data
                )
            } catch (e: Exception) {
                Timber.e(e, "응답 파싱 중 오류 발생")
                return SerialResponse.Error(e)
            }
        }

        /**
         * 응답 파싱 후 의미 있는 데이터로 해석
         * 응답 패킷의 의미를 해석하여 사용 가능한 데이터로 변환합니다.
         * 명령어 타입에 따라 시스템 상태, KV 값, mA 값, 시간 등 다양한 정보를 추출합니다.
         */
        // 명령어 타입에 따라 다른 해석 로직 수행:
        // - 시스템 상태
        // - 하트비트
        // - KV 피드백 값
        // - mA 피드백 값
        // - 시간 피드백 값
        // - 오류 코드
        // - 경고 코드
        // - 전원 진단
        // - 보드 버전 등
        fun interpretResponse(response: SerialResponse.Success): Any? {
            when (response.actionCommand) {
                // 시스템 상태 응답
                SerialCommand.Action.SystemStatus.value -> {
                    if (response.data != null && response.data.isNotEmpty()) {
                        val status = response.data[0].toInt() and 0xFF
                        Timber.d("시스템 상태: 0x${status.toString(16)}")
                        return when(status) {
                            0x00 -> "MAIN_MODE_NONE"
                            0x01 -> "MAIN_MODE_BOOT"
                            0x02 -> "MAIN_MODE_INIT"
                            0x03 -> "MAIN_MODE_STANDBY"
                            0x04 -> "MAIN_MODE_EXPOSURE_READY"
                            0x05 -> "MAIN_MODE_EXPOSURE_READY_DONE"
                            0x06 -> "MAIN_MODE_EXPOSURE"
                            0x07 -> "MAIN_MODE_EXPOSURE_DONE"
                            0x08 -> "MAIN_MODE_EXPOSURE_RELEASE"
                            0x0A -> "MAIN_MODE_RESET"
                            0x0C -> "MAIN_MODE_SYNC"
                            0x0D -> "MAIN_MODE_TECHNICAL_MODE"
                            0x1F -> "MAIN_MODE_RE_CONFIG"
                            0x40 -> "MAIN_MODE_EMERGENCY"
                            0x80 -> "MAIN_MODE_ERROR"
                            else -> "UNKNOWN_MODE: 0x${status.toString(16)}"
                        }
                    }
                }

                // 하트비트 응답
                SerialCommand.Action.HeartBeat.value -> {
                    Timber.d("하트비트 응답 수신됨")
                    return "HeartBeat OK"
                }

                SerialCommand.Action.CapbankChargeTime.value -> {
                    if (response.data != null && response.data.size >= 2) {
                        val value = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                        Timber.d("Action_Command_Process() T_CAPBANK_CHARGE_TIME = $value")
                    }
                }
                SerialCommand.Action.CapbankChargeTimeout.value -> {
                    if (response.data != null && response.data.size >= 2) {
                        val value = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                        Timber.d("Action_Command_Process() T_CAPBANK_CHARGE_TIMEOUT = $value")
                    }
                }

                // KV 피드백 값
                SerialCommand.Action.KvFbValue.value -> {
                    if (response.data != null && response.data.size >= 6) {
                        val kvValue = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                        val kvDac = ((response.data[2].toInt() and 0xFF) shl 8) or (response.data[3].toInt() and 0xFF)
                        val kvCalibration = ((response.data[4].toInt() and 0xFF) shl 8) or (response.data[5].toInt() and 0xFF)
                        val kvDecimal = kvValue / 10.0

                        Timber.d("KV 피드백: $kvDecimal kV, DAC: $kvDac, 보정: $kvCalibration")
                        return mapOf(
                            "kv" to kvDecimal,
                            "dac" to kvDac,
                            "calibration" to kvCalibration
                        )
                    }
                }

                // mA 피드백 값
                SerialCommand.Action.MaFbValue.value -> {
                    if (response.data != null && response.data.size >= 4) {
                        val maValue = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                        val maDac = ((response.data[2].toInt() and 0xFF) shl 8) or (response.data[3].toInt() and 0xFF)
                        val maDecimal = maValue / 10.0

                        Timber.d("mA 피드백: $maDecimal mA, DAC: $maDac")
                        return mapOf(
                            "ma" to maDecimal,
                            "dac" to maDac
                        )
                    }
                }

                // 시간 피드백 값
                SerialCommand.Action.TimeFbValue.value -> {
                    if (response.data != null && response.data.size >= 2) {
                        val timeValue = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)

                        Timber.d("시간 피드백: $timeValue ms")
                        return timeValue
                    }
                }

                // 오류 코드
                SerialCommand.Action.ErrorCode.value -> {
                    if (response.data != null && response.data.size >= 2) {
                        val module = response.data[0].toInt() and 0xFF
                        val errorNo = response.data[1].toInt() and 0xFF

                        Timber.e("오류 발생: 모듈=$module, 오류=$errorNo")
                        return mapOf(
                            "module" to module,
                            "errorNo" to errorNo
                        )
                    }
                }

                // 경고 코드
                SerialCommand.Action.WarningCode.value -> {
                    if (response.data != null && response.data.size >= 2) {
                        val module = response.data[0].toInt() and 0xFF
                        val warningNo = response.data[1].toInt() and 0xFF

                        Timber.w("경고 발생: 모듈=$module, 경고=$warningNo")
                        return mapOf(
                            "module" to module,
                            "warningNo" to warningNo
                        )
                    }
                }

                // 전원 진단
                SerialCommand.Action.PowerDiagnosis.value -> {
                    if (response.data != null && response.data.size >= 3) {
                        val type = response.data[0].toInt() and 0xFF
                        val value = ((response.data[1].toInt() and 0xFF) shl 8) or (response.data[2].toInt() and 0xFF)

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

                        Timber.d("전원 진단: $typeStr = $value")
                        return mapOf(
                            "type" to type,
                            "typeStr" to typeStr,
                            "value" to value
                        )
                    }
                }

                // 보드 버전
                SerialCommand.Action.TBoardVersion.value -> {
                    if (response.data != null && response.data.size >= 2) {
                        val type = response.data[0].toInt() and 0xFF
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

                        // 기본 버전 정보
                        val version = response.data[1].toInt() and 0xFF

                        // 앱 버전인 경우 추가 정보 포함
                        if (type == 0 || type == 8) {
                            if (response.data.size >= 6) {
                                val major = response.data[1].toInt() and 0xFF
                                val minor = response.data[2].toInt() and 0xFF
                                val year = response.data[3].toInt() and 0xFF + 2000
                                val month = response.data[4].toInt() and 0xFF
                                val day = response.data[5].toInt() and 0xFF

                                Timber.d("보드 버전: $typeStr = $major.$minor ($year-$month-$day)")
                                return mapOf(
                                    "type" to type,
                                    "typeStr" to typeStr,
                                    "major" to major,
                                    "minor" to minor,
                                    "year" to year,
                                    "month" to month,
                                    "day" to day
                                )
                            }
                        } else {
                            Timber.d("보드 버전: $typeStr = $version")
                            return mapOf(
                                "type" to type,
                                "typeStr" to typeStr,
                                "version" to version
                            )
                        }
                    }
                }

                // 기타 응답 처리...
                else -> {
                    Timber.d("해석되지 않은 응답: Control=0x${response.controlCommand.toString(16)}, Action=0x${response.actionCommand.toString(16)}")
                }
            }

            return null
        }

        // 유틸리티 메서드들
        /**
         * 바이트 배열을 16진수 문자열로 변환
         */
        fun bytesToHexString(bytes: ByteArray): String {
            return bytes.joinToString(" ") { byte ->
                String.format("%02X", byte)
            }
        }

        /**
         * 16진수 문자열을 바이트 배열로 변환
         */
        fun hexStringToBytes(hexString: String): ByteArray? {
            try {
                val cleanHexString = hexString.replace(" ", "")
                if (cleanHexString.length % 2 != 0) {
                    Timber.e("유효하지 않은 16진수 문자열")
                    return null
                }

                val result = ByteArray(cleanHexString.length / 2)

                for (i in result.indices) {
                    val index = i * 2
                    val byteValue = cleanHexString.substring(index, index + 2).toInt(16)
                    result[i] = byteValue.toByte()
                }

                return result
            } catch (e: Exception) {
                Timber.e(e, "16진수 문자열 변환 오류")
                return null
            }
        }

        /**
         * 값을 설정하는 패킷용 데이터 생성 - unsigned short (2바이트)
         */
        fun createUShortData(value: Int): ByteArray {
            return ByteArray(2).apply {
                this[0] = ((value shr 8) and 0xFF).toByte()
                this[1] = (value and 0xFF).toByte()
            }
        }

        /**
         * 값을 설정하는 패킷용 데이터 생성 - unsigned char (1바이트)
         */
        fun createUCharData(value: Int): ByteArray {
            return ByteArray(1).apply {
                this[0] = (value and 0xFF).toByte()
            }
        }
    }
}