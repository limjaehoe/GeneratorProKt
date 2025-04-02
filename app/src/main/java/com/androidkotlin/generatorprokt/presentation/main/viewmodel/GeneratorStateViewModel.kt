package com.androidkotlin.generatorprokt.presentation.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.generatorprokt.domain.model.MainMode
import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.repository.GeneratorRepository
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import com.androidkotlin.generatorprokt.presentation.main.state.GeneratorUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 발전기 상태를 UI에 표시하고 제어하기 위한 ViewModel
 */
@HiltViewModel
class GeneratorStateViewModel @Inject constructor(
    private val generatorRepository: GeneratorRepository,
    private val serial422Repository: Serial422Repository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Loading)
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    // 현재 모드
    private val _currentMode = MutableStateFlow(MainMode.NONE)
    val currentMode: StateFlow<MainMode> = _currentMode.asStateFlow()

    // kV, mA, 시간 피드백 값
    private val _kvFeedback = MutableStateFlow<Double?>(null)
    val kvFeedback: StateFlow<Double?> = _kvFeedback.asStateFlow()

    private val _maFeedback = MutableStateFlow<Double?>(null)
    val maFeedback: StateFlow<Double?> = _maFeedback.asStateFlow()

    private val _timeFeedback = MutableStateFlow<Int?>(null)
    val timeFeedback: StateFlow<Int?> = _timeFeedback.asStateFlow()

    // 오류 및 경고 코드
    private val _errorCode = MutableStateFlow<Int?>(null)
    val errorCode: StateFlow<Int?> = _errorCode.asStateFlow()

    private val _warningCode = MutableStateFlow<Int?>(null)
    val warningCode: StateFlow<Int?> = _warningCode.asStateFlow()

    // Ready 스위치 상태
    private val _readySwitch = MutableStateFlow<Int?>(null)
    val readySwitch: StateFlow<Int?> = _readySwitch.asStateFlow()

    // 포커스 설정 (0: Large, 1: Small)
    private val _focus = MutableStateFlow<Int?>(null)
    val focus: StateFlow<Int?> = _focus.asStateFlow()

    init {
        observeSerialData()
        requestSystemStatus()
    }

    /**
     * 시리얼 데이터 관찰 및 처리
     */
    private fun observeSerialData() {
        serial422Repository.receiveData()
            .onEach { response ->
                Timber.d("시리얼 데이터 수신: $response")

                when (response) {
                    is com.androidkotlin.generatorprokt.domain.model.SerialResponse.Success -> {
                        // 수신된 데이터 타입에 따라 처리
                        when (response.actionCommand) {
                            SerialCommand.Action.SystemStatus.value -> {
                                processSystemStatusResponse(response)
                            }
                            SerialCommand.Action.KvFbValue.value -> {
                                if (response.data != null && response.data.size >= 2) {
                                    val kvValue = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                                    _kvFeedback.value = kvValue / 10.0
                                    Timber.d("KV 피드백: ${_kvFeedback.value} kV")
                                }
                            }
                            SerialCommand.Action.MaFbValue.value -> {
                                if (response.data != null && response.data.size >= 2) {
                                    val maValue = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                                    _maFeedback.value = maValue / 10.0
                                    Timber.d("mA 피드백: ${_maFeedback.value} mA")
                                }
                            }
                            SerialCommand.Action.TimeFbValue.value -> {
                                if (response.data != null && response.data.size >= 2) {
                                    val timeValue = ((response.data[0].toInt() and 0xFF) shl 8) or (response.data[1].toInt() and 0xFF)
                                    _timeFeedback.value = timeValue
                                    Timber.d("시간 피드백: ${_timeFeedback.value} ms")
                                }
                            }
                            SerialCommand.Action.ErrorCode.value -> {
                                if (response.data != null && response.data.size >= 2) {
                                    val module = response.data[0].toInt() and 0xFF
                                    val errorNo = response.data[1].toInt() and 0xFF
                                    _errorCode.value = errorNo
                                    Timber.e("오류 발생: 모듈=$module, 오류=$errorNo")
                                    _uiState.value = GeneratorUiState.Error("오류 발생: 코드 $errorNo")
                                }
                            }
                            SerialCommand.Action.WarningCode.value -> {
                                if (response.data != null && response.data.size >= 2) {
                                    val module = response.data[0].toInt() and 0xFF
                                    val warningNo = response.data[1].toInt() and 0xFF
                                    _warningCode.value = warningNo
                                    Timber.w("경고 발생: 모듈=$module, 경고=$warningNo")
                                    _uiState.value = GeneratorUiState.Error("경고 발생: 코드 $warningNo")
                                }
                            }
                            SerialCommand.Action.ReadySw.value -> {
                                if (response.data != null && response.data.isNotEmpty()) {
                                    val readyStatus = response.data[0].toInt() and 0xFF
                                    _readySwitch.value = readyStatus
                                    Timber.d("Ready 스위치 상태: $readyStatus")
                                }
                            }
                        }
                    }
                    is com.androidkotlin.generatorprokt.domain.model.SerialResponse.Error -> {
                        Timber.e(response.exception, "시리얼 데이터 오류")
                        _uiState.value = GeneratorUiState.Error("통신 오류: ${response.exception.message}")
                    }
                    is com.androidkotlin.generatorprokt.domain.model.SerialResponse.Timeout -> {
                        Timber.w("시리얼 데이터 타임아웃")
                        _uiState.value = GeneratorUiState.Error("통신 시간 초과")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 시스템 상태 응답 처리
     */
    private fun processSystemStatusResponse(response: com.androidkotlin.generatorprokt.domain.model.SerialResponse.Success) {
        response.data?.let { data ->
            if (data.isNotEmpty()) {
                val modeValue = data[0].toInt() and 0xFF
                val newMode = MainMode.fromRawValue(modeValue)

                Timber.d("시스템 상태 업데이트: $newMode (0x${modeValue.toString(16)})")
                _currentMode.value = newMode // 여기서 StateFlow가 업데이트됨

                // 모드에 따른 UI 상태 업데이트
                updateUiStateBasedOnMode(newMode)
            }
        }
    }

    /**
     * 모드에 따른 UI 상태 업데이트
     */
    private fun updateUiStateBasedOnMode(mode: MainMode) {
        when (mode) {
            MainMode.ERROR -> {
                _uiState.value = GeneratorUiState.Error("발전기 오류가 발생했습니다")
            }
            MainMode.EMERGENCY -> {
                _uiState.value = GeneratorUiState.Error("긴급 정지 상태입니다")
            }
            MainMode.EXPOSURE, MainMode.EXPOSURE_READY, MainMode.EXPOSURE_READY_DONE -> {
                _uiState.value = GeneratorUiState.Exposing(mode.korDescription)
            }
            MainMode.STANDBY -> {
                _uiState.value = GeneratorUiState.Ready("대기 모드")
            }
            else -> {
                _uiState.value = GeneratorUiState.Idle(mode.korDescription)
            }
        }
    }

    /**
     * 시스템 상태 요청
     */
    fun requestSystemStatus() {
        viewModelScope.launch {
            _uiState.value = GeneratorUiState.Loading

            generatorRepository.requestSystemStatus()
                .onSuccess {
                    Timber.d("시스템 상태 요청 성공")
                }
                .onFailure { error ->
                    Timber.e(error, "시스템 상태 요청 실패")
                    _uiState.value = GeneratorUiState.Error("시스템 상태 요청 실패: ${error.message}")
                }
        }
    }

    /**
     * 포커스 설정
     * @param smallFocus true일 경우 Small(1), false일 경우 Large(0) 포커스
     */
    fun setFocus(smallFocus: Boolean) {
        val focusValue = if (smallFocus) 1 else 0
        _focus.value = focusValue

        viewModelScope.launch {
            try {
                serial422Repository.setFocus(smallFocus)
                    .onSuccess {
                        Timber.d("포커스 설정 성공: ${if (smallFocus) "Small" else "Large"}")
                    }
                    .onFailure { error ->
                        Timber.e(error, "포커스 설정 실패")
                    }
            } catch (e: Exception) {
                Timber.e(e, "포커스 설정 중 예외 발생")
            }
        }
    }

    /**
     * Ready 스위치 설정
     * @param values 조사 조건 값들 (kV, mA, ms, focus 등)
     */
    fun setReadySwitch(kv: Double, mA: Double, ms: Double, focus: Int? = null) {
        viewModelScope.launch {
            try {
                // kV 값 설정 (123.4kV -> 1234)
                val kvValue = (kv * 10).toInt()
                serial422Repository.setKvValue(kvValue)

                // mA 값 설정 (12.3mA -> 123)
                val maValue = (mA * 10).toInt()
                serial422Repository.setMaValue(maValue)

                // 시간 값 설정 (ms)
                val timeValue = ms.toInt()
                serial422Repository.setTimeValue(timeValue)

                // 포커스 설정 (있는 경우)
                focus?.let {
                    val smallFocus = it == 1
                    serial422Repository.setFocus(smallFocus)
                }

                // Ready 명령 전송 (ReadySw 액션)
                sendReadyCommand(true)

                Timber.d("Ready 설정 성공: kV=$kv, mA=$mA, ms=$ms, focus=$focus")
            } catch (e: Exception) {
                Timber.e(e, "Ready 설정 중 예외 발생")
            }
        }
    }

    /**
     * Ready 상태 전환 명령 전송 (1: 켜기, 0: 끄기)
     */
    private suspend fun sendReadyCommand(on: Boolean): Result<com.androidkotlin.generatorprokt.domain.model.SerialResponse> {
        try {
            Timber.d("Ready 명령 전송 중: ${if (on) "ON" else "OFF"}")

            val value = if (on) 1 else 0
            val data = com.androidkotlin.generatorprokt.data.device.SerialPacketHandler.createUCharData(value)

            val packet = com.androidkotlin.generatorprokt.domain.model.SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.ReadySw,
                data = data,
                targetId = com.androidkotlin.generatorprokt.domain.model.SerialPacket.TARGET_MAIN,
                sourceId = com.androidkotlin.generatorprokt.domain.model.SerialPacket.SOURCE_CONSOLE
            )

            return serial422Repository.sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "Ready 명령 전송 중 예외 발생")
            return Result.failure(e)
        }
    }

    /**
     * Expose 명령 전송
     */
    fun setExpose(on: Boolean) {
        viewModelScope.launch {
            try {
                sendExposeCommand(on)
                Timber.d("Expose 명령 전송 성공: ${if (on) "ON" else "OFF"}")
            } catch (e: Exception) {
                Timber.e(e, "Expose 명령 전송 중 예외 발생")
            }
        }
    }

    /**
     * Expose 상태 전환 명령 전송 (1: 켜기, 0: 끄기)
     */
    private suspend fun sendExposeCommand(on: Boolean): Result<com.androidkotlin.generatorprokt.domain.model.SerialResponse> {
        try {
            Timber.d("Expose 명령 전송 중: ${if (on) "ON" else "OFF"}")

            val value = if (on) 1 else 0
            val data = com.androidkotlin.generatorprokt.data.device.SerialPacketHandler.createUCharData(value)

            val packet = com.androidkotlin.generatorprokt.domain.model.SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.ExposureSw,
                data = data,
                targetId = com.androidkotlin.generatorprokt.domain.model.SerialPacket.TARGET_MAIN,
                sourceId = com.androidkotlin.generatorprokt.domain.model.SerialPacket.SOURCE_CONSOLE
            )

            return serial422Repository.sendPacket(packet)
        } catch (e: Exception) {
            Timber.e(e, "Expose 명령 전송 중 예외 발생")
            return Result.failure(e)
        }
    }
}