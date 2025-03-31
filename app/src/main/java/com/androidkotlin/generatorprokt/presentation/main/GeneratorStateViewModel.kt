package com.androidkotlin.generatorprokt.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.generatorprokt.domain.model.MainMode
import com.androidkotlin.generatorprokt.domain.repository.GeneratorRepository
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
    private val generatorRepository: GeneratorRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Loading)
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    // 현재 모드
    private val _currentMode = MutableStateFlow(MainMode.NONE)
    val currentMode: StateFlow<MainMode> = _currentMode.asStateFlow()

    init {
        observeMainMode()
        requestSystemStatus()
    }

    /**
     * 발전기 모드 변경 감지 및 UI 업데이트
     */
    private fun observeMainMode() {
        generatorRepository.observeMainMode()
            .onEach { mode ->
                _currentMode.value = mode

                // 모드에 따른 UI 상태 업데이트
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
            .launchIn(viewModelScope)
    }

    /**
     * 시스템 상태 요청
     */
    fun requestSystemStatus() {
        viewModelScope.launch {
            _uiState.value = GeneratorUiState.Loading

            generatorRepository.requestSystemStatus()
                .onSuccess {
                    // 성공 시 상태가 observeMainMode에서 자동으로 업데이트됨
                }
                .onFailure { error ->
                    Timber.e(error, "시스템 상태 요청 실패")
                    _uiState.value = GeneratorUiState.Error("시스템 상태 요청 실패: ${error.message}")
                }
        }
    }

    /**
     * 특정 모드로 변경 요청
     */
    fun setMode(mode: MainMode) {
        viewModelScope.launch {
            _uiState.value = GeneratorUiState.Loading

            generatorRepository.setMode(mode)
                .onSuccess {
                    Timber.d("모드 변경 성공: $mode")
                    // 상태는 observeMainMode에서 자동으로 업데이트됨
                }
                .onFailure { error ->
                    Timber.e(error, "모드 변경 실패")
                    _uiState.value = GeneratorUiState.Error("모드 변경 실패: ${error.message}")
                }
        }
    }

    /**
     * READY 모드로 변경
     */
    fun setReadyMode() {
        setMode(MainMode.EXPOSURE_READY)
    }

    /**
     * STANDBY 모드로 변경
     */
    fun setStandbyMode() {
        setMode(MainMode.STANDBY)
    }

    /**
     * EXPOSURE 모드로 변경
     */
    fun setExposureMode() {
        setMode(MainMode.EXPOSURE)
    }
}

/**
 * UI 상태를 나타내는 sealed class
 */
sealed class GeneratorUiState {
    object Loading : GeneratorUiState()
    data class Idle(val message: String) : GeneratorUiState()
    data class Ready(val message: String) : GeneratorUiState()
    data class Exposing(val message: String) : GeneratorUiState()
    data class Error(val message: String) : GeneratorUiState()
}