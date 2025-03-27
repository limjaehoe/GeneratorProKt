package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.repository.DeviceRepository
import javax.inject.Inject

class ConnectDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> {
        return deviceRepository.connectDevice(deviceId)
    }
}