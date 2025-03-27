package com.androidkotlin.generatorprokt.data.repository


import com.androidkotlin.generatorprokt.data.device.DeviceDataSource
import com.androidkotlin.generatorprokt.domain.model.Device
import com.androidkotlin.generatorprokt.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val deviceDataSource: DeviceDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : DeviceRepository {

    override fun getDevices(): Flow<List<Device>> {
        return deviceDataSource.getDevices().map { deviceDtos ->
            deviceDtos.map { it.toDomain() }
        }
    }

    override suspend fun connectDevice(deviceId: String): Result<Unit> =
        withContext(ioDispatcher) {
            deviceDataSource.connectDevice(deviceId)
        }

    override suspend fun disconnectDevice(deviceId: String): Result<Unit> =
        withContext(ioDispatcher) {
            deviceDataSource.disconnectDevice(deviceId)
        }
}