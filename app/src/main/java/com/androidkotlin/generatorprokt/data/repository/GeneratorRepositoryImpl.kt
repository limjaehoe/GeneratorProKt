package com.androidkotlin.generatorprokt.data.repository

import com.androidkotlin.generatorprokt.data.device.SerialPacketHandler
import com.androidkotlin.generatorprokt.domain.model.MainMode
import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialPacket
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.repository.GeneratorRepository
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneratorRepositoryImpl @Inject constructor(
    private val serial422Repository: Serial422Repository,
    private val ioDispatcher: CoroutineDispatcher
) : GeneratorRepository {

    // 현재 모드를 저장하는 StateFlow
    private val _currentMode = MutableStateFlow(MainMode.NONE)

    // 리포지토리 자체적인 코루틴 스코프 생성
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        // 초기 상태 요청 및 Flow 수집 설정
        setupModeListener()
    }

    /**
     * 발전기 상태 변경을 감지하도록 리스너 설정
     */
    private fun setupModeListener() {
        // 시리얼 응답 Flow를 수집하여 상태 변경 감지
        repositoryScope.launch {
            serial422Repository.receiveData().collect { response ->
                when (response) {
                    is SerialResponse.Success -> {
                        if (response.actionCommand == SerialCommand.Action.SystemStatus.value) {
                            // 시스템 상태 응답인 경우 처리
                            processSystemStatusResponse(response)
                        }
                    }
                    else -> {
                        // 오류 상태는 무시
                    }
                }
            }
        }
    }

    /**
     * 시스템 상태 응답 처리
     */
    private fun processSystemStatusResponse(response: SerialResponse.Success) {
        response.data?.let { data ->
            if (data.isNotEmpty()) {
                val modeValue = data[0].toInt() and 0xFF
                val newMode = MainMode.fromRawValue(modeValue)

                Timber.d("시스템 상태 업데이트: $newMode (0x${modeValue.toString(16)})")
                _currentMode.value = newMode
            }
        }
    }

    override fun observeMainMode(): Flow<MainMode> {
        return _currentMode.asStateFlow()
    }

    override suspend fun getCurrentMode(): MainMode {
        return _currentMode.value
    }

    override suspend fun setMode(mode: MainMode): Result<SerialResponse> = withContext(ioDispatcher) {
        return@withContext setModeByHexValue(mode.hexValue)
    }

    override suspend fun setModeByHexValue(hexValue: Int): Result<SerialResponse> = withContext(ioDispatcher) {
        try {
            Timber.d("모드 변경 요청: 0x${hexValue.toString(16)}")

            // 모드 변경 패킷 생성
            val data = SerialPacketHandler.createUCharData(hexValue)
            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.SystemStatus,
                data = data,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            // 패킷 전송
            val result = serial422Repository.sendPacket(packet)

            result.onSuccess {
                Timber.d("모드 변경 요청 성공")
            }.onFailure { error ->
                Timber.e(error, "모드 변경 요청 실패")
            }

            return@withContext result
        } catch (e: Exception) {
            Timber.e(e, "모드 변경 중 예외 발생")
            return@withContext Result.failure(e)
        }
    }

    override suspend fun requestSystemStatus(): Result<SerialResponse> = withContext(ioDispatcher) {
        try {
            Timber.d("시스템 상태 요청")

            // 시스템 상태 요청 패킷 생성
            val packet = SerialPacket(
                controlCommand = SerialCommand.Control.CommStatusInfo,
                actionCommand = SerialCommand.Action.SystemStatus,
                data = null,
                targetId = SerialPacket.TARGET_MAIN,
                sourceId = SerialPacket.SOURCE_CONSOLE
            )

            // 패킷 전송
            val result = serial422Repository.sendPacket(packet)

            result.onSuccess {
                Timber.d("시스템 상태 요청 성공")
            }.onFailure { error ->
                Timber.e(error, "시스템 상태 요청 실패")
            }

            return@withContext result
        } catch (e: Exception) {
            Timber.e(e, "시스템 상태 요청 중 예외 발생")
            return@withContext Result.failure(e)
        }
    }
}