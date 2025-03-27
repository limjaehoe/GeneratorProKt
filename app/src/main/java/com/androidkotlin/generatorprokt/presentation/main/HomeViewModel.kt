package com.androidkotlin.generatorprokt.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.usecase.ConnectSerialUseCase
import com.androidkotlin.generatorprokt.domain.usecase.ReceiveSerialDataUseCase
import com.androidkotlin.generatorprokt.domain.usecase.SendCommandUseCase
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
    private val sendCommandUseCase: SendCommandUseCase,
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
            _receivedData.emit("Connecting to device...")

            connectSerialUseCase()
                .onSuccess {
                    _deviceStatus.value = DeviceStatus.Connected
                    _uiState.value = GeneratorUiState.Success("장치에 연결되었습니다")
                    _receivedData.emit("Connected successfully")

                    // 초기 상태 요청
                    requestInitialStatus()
                }
                .onFailure { e ->
                    _deviceStatus.value = DeviceStatus.Error
                    _uiState.value = GeneratorUiState.Error("연결 실패: ${e.message}")
                    _receivedData.emit("Connection failed: ${e.message}")
                }
        }
    }

    /**
     * 심박 명령 전송 (장치 연결 확인용)
     */
    fun sendHeartbeat() {
        viewModelScope.launch {
            _receivedData.emit("Sending heartbeat...")
            sendCommandUseCase(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.HeartBeat
            )
                .onSuccess {
                    _receivedData.emit("Heartbeat sent successfully")
                }
                .onFailure { e ->
                    _receivedData.emit("Failed to send heartbeat: ${e.message}")
                }
        }
    }

    /**
     * 발전기 초기 상태 요청
     */
    private fun requestInitialStatus() {
        viewModelScope.launch {
            _receivedData.emit("Requesting initial status...")
            sendCommandUseCase(
                controlCommand = SerialCommand.Control.CommGetInfo,
                actionCommand = SerialCommand.Action.InitialReq
            )
                .onSuccess {
                    _receivedData.emit("Initial status request sent")
                }
                .onFailure { e ->
                    _receivedData.emit("Failed to request initial status: ${e.message}")
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
                val errorCode = response.data?.firstOrNull()?.toInt() ?: 0
                _uiState.value = GeneratorUiState.Error("에러 코드: $errorCode")
            }
            // 다른 명령에 대한 처리 추가...
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
        }
    }

    /**
     * 제어 명령어 이름 가져오기
     */
    private fun getControlCommandName(commandValue: Int): String {
        return when (commandValue) {
            SerialCommand.Control.CommGetInfo.value -> "COMM_GET_INFO"
            SerialCommand.Control.CommSetInfo.value -> "COMM_SET_INFO"
            SerialCommand.Control.CommStatusInfo.value -> "COMM_STATUS_INFO"
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
}