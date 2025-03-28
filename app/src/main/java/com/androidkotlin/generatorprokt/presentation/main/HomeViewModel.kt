package com.androidkotlin.generatorprokt.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.generatorprokt.data.device.SerialPacketHandler
import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialPacket
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.usecase.ConnectSerialUseCase
import com.androidkotlin.generatorprokt.domain.usecase.ReceiveSerialDataUseCase
import com.androidkotlin.generatorprokt.domain.usecase.SendGeneratorCommandUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectSerialUseCase: ConnectSerialUseCase,
    private val sendGeneratorCommandUseCase: SendGeneratorCommandUseCase,
    private val receiveSerialDataUseCase: ReceiveSerialDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Idle)
    val uiState: StateFlow<GeneratorUiState> = _uiState

    private val _deviceStatus = MutableStateFlow<DeviceStatus>(DeviceStatus.Disconnected)
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus

    private val _receivedData = MutableSharedFlow<String>(replay = 10)
    val receivedData: SharedFlow<String> = _receivedData.asSharedFlow()

    init {
        // 데이터 수신 Flow 구독
        receiveSerialDataUseCase()
            .onEach { response ->
                when (response) {
                    is SerialResponse.Success -> {
                        processResponse(response)
                        logResponse(response)
                    }
                    is SerialResponse.Error -> {
                        Timber.e(response.exception, "Error receiving data")
                        _uiState.value = GeneratorUiState.Error("통신 오류: ${response.exception.message}")
                        _receivedData.emit("ERROR: ${response.exception.message}")
                    }
                    is SerialResponse.Timeout -> {
                        _uiState.value = GeneratorUiState.Error("통신 시간 초과")
                        _receivedData.emit("TIMEOUT")
                    }
                }
            }
            .catch { e ->
                Timber.e(e, "Error in data flow")
                _uiState.value = GeneratorUiState.Error("통신 오류: ${e.message}")
                _receivedData.emit("FLOW ERROR: ${e.message}")
            }
            .launchIn(viewModelScope)
    }

    /**
     * 시리얼 포트 연결
     */
    fun connectDevice() {
        viewModelScope.launch {
            _uiState.value = GeneratorUiState.Loading
            _receivedData.emit("장치에 연결 시도 중...")

            try {
                // 연결 시도 전 상태 로깅
                _receivedData.emit("USB 장치 연결 시작...")

                val result = connectSerialUseCase()
                result.fold(
                    onSuccess = {
                        _deviceStatus.value = DeviceStatus.Connected
                        _uiState.value = GeneratorUiState.Success("장치에 연결되었습니다")
                        _receivedData.emit("성공적으로 연결되었습니다")

                        // 보드 버전 정보 요청
                        requestBoardVersion()
                    },
                    onFailure = { e ->
                        _deviceStatus.value = DeviceStatus.Error
                        _uiState.value = GeneratorUiState.Error("연결 실패: ${e.message}")
                        _receivedData.emit("연결 실패: ${e.message}")

                        // 더 자세한 오류 정보 로깅
                        Timber.e(e, "장치 연결 중 오류 발생")
                    }
                )
            } catch (e: Exception) {
                _deviceStatus.value = DeviceStatus.Error
                _uiState.value = GeneratorUiState.Error("예외 발생: ${e.message}")
                _receivedData.emit("예외 발생: ${e.message}")
                Timber.e(e, "connectDevice() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * 하트비트 명령 전송 (장치 연결 확인용)
     */
    fun sendHeartbeat() {
        viewModelScope.launch {
            try {
                _receivedData.emit("하트비트 전송 중...")

                if (!isConnected()) {
                    _receivedData.emit("하트비트 전송 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase()
                    .onSuccess {
                        _receivedData.emit("하트비트가 성공적으로 전송되었습니다")
                    }
                    .onFailure { e ->
                        _receivedData.emit("하트비트 전송 실패: ${e.message}")
                        Timber.e(e, "하트비트 전송 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("하트비트 전송 중 예외 발생: ${e.message}")
                Timber.e(e, "sendHeartbeat() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * kV 값 설정
     */
    fun setKvValue(kvValue: Int) {
        viewModelScope.launch {
            try {
                _receivedData.emit("kV 값 설정 중: $kvValue...")

                if (!isConnected()) {
                    _receivedData.emit("kV 값 설정 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase.setKvValue(kvValue)
                    .onSuccess {
                        _receivedData.emit("kV 값이 성공적으로 설정되었습니다: $kvValue")
                    }
                    .onFailure { e ->
                        _receivedData.emit("kV 값 설정 실패: ${e.message}")
                        Timber.e(e, "kV 값 설정 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("kV 값 설정 중 예외 발생: ${e.message}")
                Timber.e(e, "setKvValue() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * mA 값 설정
     */
    fun setMaValue(maValue: Int) {
        viewModelScope.launch {
            try {
                _receivedData.emit("mA 값 설정 중: $maValue...")

                if (!isConnected()) {
                    _receivedData.emit("mA 값 설정 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase.setMaValue(maValue)
                    .onSuccess {
                        _receivedData.emit("mA 값이 성공적으로 설정되었습니다: $maValue")
                    }
                    .onFailure { e ->
                        _receivedData.emit("mA 값 설정 실패: ${e.message}")
                        Timber.e(e, "mA 값 설정 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("mA 값 설정 중 예외 발생: ${e.message}")
                Timber.e(e, "setMaValue() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * 노출 시간 설정
     */
    fun setTimeValue(timeMs: Int) {
        viewModelScope.launch {
            try {
                _receivedData.emit("노출 시간 설정 중: $timeMs ms...")

                if (!isConnected()) {
                    _receivedData.emit("노출 시간 설정 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase.setTimeValue(timeMs)
                    .onSuccess {
                        _receivedData.emit("노출 시간이 성공적으로 설정되었습니다: $timeMs ms")
                    }
                    .onFailure { e ->
                        _receivedData.emit("노출 시간 설정 실패: ${e.message}")
                        Timber.e(e, "노출 시간 설정 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("노출 시간 설정 중 예외 발생: ${e.message}")
                Timber.e(e, "setTimeValue() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * 포커스 설정
     */
    fun setFocus(smallFocus: Boolean) {
        viewModelScope.launch {
            try {
                val focusStr = if (smallFocus) "Small" else "Large"
                _receivedData.emit("포커스 설정 중: $focusStr...")

                if (!isConnected()) {
                    _receivedData.emit("포커스 설정 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase.setFocus(smallFocus)
                    .onSuccess {
                        _receivedData.emit("포커스가 성공적으로 설정되었습니다: $focusStr")
                    }
                    .onFailure { e ->
                        _receivedData.emit("포커스 설정 실패: ${e.message}")
                        Timber.e(e, "포커스 설정 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("포커스 설정 중 예외 발생: ${e.message}")
                Timber.e(e, "setFocus() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * 전원 진단 요청
     */
    fun requestPowerDiagnosis(type: Int = 0) {
        viewModelScope.launch {
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
                    else -> "Unknown"
                }
                _receivedData.emit("전원 진단 요청 중: $typeStr...")

                if (!isConnected()) {
                    _receivedData.emit("전원 진단 요청 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase.requestPowerDiagnosis(type)
                    .onSuccess {
                        _receivedData.emit("전원 진단 요청이 성공적으로 전송되었습니다")
                    }
                    .onFailure { e ->
                        _receivedData.emit("전원 진단 요청 실패: ${e.message}")
                        Timber.e(e, "전원 진단 요청 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("전원 진단 요청 중 예외 발생: ${e.message}")
                Timber.e(e, "requestPowerDiagnosis() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * 보드 버전 정보 요청
     */
    fun requestBoardVersion(type: Int = 0) {
        viewModelScope.launch {
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
                    else -> "Unknown"
                }
                _receivedData.emit("보드 버전 정보 요청 중: $typeStr...")

                if (!isConnected()) {
                    _receivedData.emit("보드 버전 정보 요청 실패: 장치가 연결되어 있지 않습니다")
                    return@launch
                }

                sendGeneratorCommandUseCase.requestBoardVersion(type)
                    .onSuccess {
                        _receivedData.emit("보드 버전 정보 요청이 성공적으로 전송되었습니다")
                    }
                    .onFailure { e ->
                        _receivedData.emit("보드 버전 정보 요청 실패: ${e.message}")
                        Timber.e(e, "보드 버전 정보 요청 중 오류 발생")
                    }
            } catch (e: Exception) {
                _receivedData.emit("보드 버전 정보 요청 중 예외 발생: ${e.message}")
                Timber.e(e, "requestBoardVersion() 메서드에서 예외 발생")
            }
        }
    }

    /**
     * 응답 데이터 처리
     */
    private fun processResponse(response: SerialResponse.Success) {
        when (response.actionCommand) {
            SerialCommand.Action.HeartBeat.value -> {
                _deviceStatus.value = DeviceStatus.Connected
                _uiState.value = GeneratorUiState.Success("하트비트 수신됨")

            }
            SerialCommand.Action.ErrorCode.value -> {
                if (response.data != null && response.data.size >= 2) {
                    val module = response.data[0].toInt() and 0xFF
                    val errorNo = response.data[1].toInt() and 0xFF
                    _uiState.value = GeneratorUiState.Error("오류 발생: 모듈=$module, 오류=$errorNo")
                } else {
                    _uiState.value = GeneratorUiState.Error("알 수 없는 오류 발생")
                }
            }
            SerialCommand.Action.SystemStatus.value -> {
                if (response.data != null && response.data.isNotEmpty()) {
                    val status = response.data[0].toInt() and 0xFF
                    val statusStr = when(status) {
                        0x00 -> "NONE"
                        0x01 -> "BOOT"
                        0x02 -> "INIT"
                        0x03 -> "STANDBY"
                        0x04 -> "EXPOSURE_READY"
                        0x05 -> "EXPOSURE_READY_DONE"
                        0x06 -> "EXPOSURE"
                        0x07 -> "EXPOSURE_DONE"
                        0x08 -> "EXPOSURE_RELEASE"
                        0x0A -> "RESET"
                        0x0C -> "SYNC"
                        0x0D -> "TECHNICAL_MODE"
                        0x1F -> "RE_CONFIG"
                        0x40 -> "EMERGENCY"
                        0x80 -> "ERROR"
                        else -> "UNKNOWN: 0x${status.toString(16)}"
                    }
                    _uiState.value = GeneratorUiState.Success("시스템 상태: $statusStr")
                }
            }
            else -> {
                _uiState.value = GeneratorUiState.Success("명령 실행 완료")
            }
        }
    }

    /**
     * 응답 데이터 로깅
     */
    private fun logResponse(response: SerialResponse.Success) {
        viewModelScope.launch {
            try {
                val controlCommandName = getControlCommandName(response.controlCommand)
                val actionCommandName = getActionCommandName(response.actionCommand)

                val dataString = response.data?.let { data ->
                    if (data.isNotEmpty()) {
                        "Data: ${bytesToHexString(data)}"
                    } else {
                        "No data"
                    }
                } ?: "No data"

                _receivedData.emit("Received: [$controlCommandName - $actionCommandName] $dataString")

                // 특정 응답에 대한 추가 처리
                when (response.actionCommand) {
                    SerialCommand.Action.TBoardVersion.value -> {
                        if (response.data != null && response.data.size >= 2) {
                            val type = response.data[0].toInt() and 0xFF
                            if (type == 0 && response.data.size >= 6) {
                                val major = response.data[1].toInt() and 0xFF
                                val minor = response.data[2].toInt() and 0xFF
                                val year = response.data[3].toInt() and 0xFF + 2000
                                val month = response.data[4].toInt() and 0xFF
                                val day = response.data[5].toInt() and 0xFF

                                _receivedData.emit("보드 버전: Main Board App = v$major.$minor ($year-$month-$day)")
                            }
                        }
                    }
                    SerialCommand.Action.PowerDiagnosis.value -> {
                        if (response.data != null && response.data.size >= 3) {
                            val type = response.data[0].toInt() and 0xFF
                            val value = ((response.data[1].toInt() and 0xFF) shl 8) or (response.data[2].toInt() and 0xFF)

                            val typeStr = when(type) {
                                0 -> "전원 3.3V: ${value / 100.0}V"
                                1 -> "전원 5V: ${value / 100.0}V"
                                2 -> "전원 +12V: ${value / 100.0}V"
                                3 -> "전원 -12V: ${value / 100.0}V"
                                4 -> "주파수: ${value}Hz"
                                5 -> "DC Link: ${value}V"
                                6 -> "필라멘트 예열 전류: ${value / 100.0}A"
                                7 -> "로터 시작 전류: ${value / 100.0}A"
                                9 -> "필라멘트 예열 전류(Small/Large): ${value / 100.0}A"
                                else -> "알 수 없는 유형($type): $value"
                            }

                            _receivedData.emit("전원 진단 결과: $typeStr")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "응답 로깅 중 예외 발생")
            }
        }
    }

    /**
     * 제어 명령어 이름 가져오기
     */
    private fun getControlCommandName(commandValue: Int): String {
        return when (commandValue) {
            SerialCommand.Control.CommInitial.value -> "COMM_INITIAL"
            SerialCommand.Control.CommControl.value -> "COMM_CONTROL"
            SerialCommand.Control.CommGetInfo.value -> "COMM_GET_INFO"
            SerialCommand.Control.CommStatusInfo.value -> "COMM_STATUS_INFO"
            SerialCommand.Control.CommMemWrite.value -> "COMM_MEM_WRITE"
            SerialCommand.Control.CommVersionInfo.value -> "COMM_VERSION_INFO"
            SerialCommand.Control.CommSysControl.value -> "COMM_SYS_CONTROL"
            SerialCommand.Control.CommConfiguration.value -> "COMM_CONFIGURATION"
            else -> "UNKNOWN(0x${commandValue.toString(16)})"
        }
    }

    /**
     * 동작 명령어 이름 가져오기
     */
    private fun getActionCommandName(commandValue: Int): String {
        return when (commandValue) {
            SerialCommand.Action.HeartBeat.value -> "HEART_BEAT"
            SerialCommand.Action.InitialReq.value -> "INITIAL_REQ"
            SerialCommand.Action.FeedbackRequest.value -> "FEEDBACK_REQUEST"
            SerialCommand.Action.PowerDiagnosis.value -> "POWER_DIAGNOSIS"
            SerialCommand.Action.WarningCode.value -> "WARNING_CODE"
            SerialCommand.Action.ErrorCode.value -> "ERROR_CODE"
            SerialCommand.Action.KvValue.value -> "KV_VALUE"
            SerialCommand.Action.MaValue.value -> "MA_VALUE"
            SerialCommand.Action.TimeValue.value -> "TIME_VALUE"
            SerialCommand.Action.KvFbValue.value -> "KV_FB_VALUE"
            SerialCommand.Action.MaFbValue.value -> "MA_FB_VALUE"
            SerialCommand.Action.TimeFbValue.value -> "TIME_FB_VALUE"
            SerialCommand.Action.Mode.value -> "MODE"
            SerialCommand.Action.SystemStatus.value -> "SYSTEM_STATUS"
            SerialCommand.Action.Focus.value -> "FOCUS"
            SerialCommand.Action.BuckySelect.value -> "BUCKY_SELECT"
            SerialCommand.Action.TBoardVersion.value -> "T_BOARD_VERSION"
            SerialCommand.Action.CommandDone.value -> "COMMAND_DONE"
            else -> "UNKNOWN(0x${commandValue.toString(16)})"
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private fun bytesToHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    /**
     * 현재 연결 상태 확인
     */
    private fun isConnected(): Boolean {
        val connected = connectSerialUseCase.isConnected()
        if (!connected) {
            _deviceStatus.value = DeviceStatus.Disconnected
        }
        return connected
    }

}