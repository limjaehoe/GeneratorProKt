package com.androidkotlin.generatorprokt.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.usecase.ConnectSerialUseCase
import com.androidkotlin.generatorprokt.domain.usecase.ReceiveSerialDataUseCase
import com.androidkotlin.generatorprokt.domain.usecase.SendCommandUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val connectSerialUseCase: ConnectSerialUseCase,
    private val sendCommandUseCase: SendCommandUseCase,
    private val receiveSerialDataUseCase: ReceiveSerialDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Idle)
    val uiState: StateFlow<GeneratorUiState> = _uiState

    private val _deviceStatus = MutableStateFlow<DeviceStatus>(DeviceStatus.Disconnected)
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus

    init {
        // 데이터 수신 Flow 구독
        receiveSerialDataUseCase()
            .onEach { response ->
                when (response) {
                    is SerialResponse.Success -> {
                        processResponse(response)
                    }
                    is SerialResponse.Error -> {
                        Timber.e(response.exception, "Error receiving data")
                        _uiState.value = GeneratorUiState.Error("통신 오류: ${response.exception.message}")
                    }
                    is SerialResponse.Timeout -> {
                        _uiState.value = GeneratorUiState.Error("통신 시간 초과")
                    }
                }
            }
            .catch { e ->
                Timber.e(e, "Error in data flow")
                _uiState.value = GeneratorUiState.Error("통신 오류: ${e.message}")
            }
            .launchIn(viewModelScope)
    }

    /**
     * 시리얼 포트 연결
     */
    fun connectDevice() {
        viewModelScope.launch {
            _uiState.value = GeneratorUiState.Loading

            connectSerialUseCase()
                .onSuccess {
                    _deviceStatus.value = DeviceStatus.Connected
                    _uiState.value = GeneratorUiState.Success("장치에 연결되었습니다")

                    // 초기 상태 요청
                    requestInitialStatus()
                }
                .onFailure { e ->
                    _deviceStatus.value = DeviceStatus.Error
                    _uiState.value = GeneratorUiState.Error("연결 실패: ${e.message}")
                }
        }
    }

    /**
     * 심박 명령 전송 (장치 연결 확인용)
     */
    fun sendHeartbeat() {
        viewModelScope.launch {
            sendCommandUseCase(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.HeartBeat
            )
        }
    }

    /**
     * 발전기 초기 상태 요청
     */
    private fun requestInitialStatus() {
        viewModelScope.launch {
            sendCommandUseCase(
                controlCommand = SerialCommand.Control.CommGetInfo,
                actionCommand = SerialCommand.Action.InitialReq
            )
        }
    }

    /**
     * 응답 데이터 처리
     */
    private fun processResponse(response: SerialResponse.Success) {
        when (response.actionCommand) {
            SerialCommand.Action.HeartBeat.value -> {
                _deviceStatus.value = DeviceStatus.Connected
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
}

sealed class GeneratorUiState {
    object Idle : GeneratorUiState()
    object Loading : GeneratorUiState()
    data class Success(val message: String) : GeneratorUiState()
    data class Error(val message: String) : GeneratorUiState()
}

enum class DeviceStatus {
    Disconnected,
    Connected,
    Error
}