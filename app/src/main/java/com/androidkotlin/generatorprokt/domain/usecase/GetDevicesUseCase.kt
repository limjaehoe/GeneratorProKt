package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.model.Device
import com.androidkotlin.generatorprokt.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(): Flow<List<Device>> {
        return deviceRepository.getDevices()
    }
}