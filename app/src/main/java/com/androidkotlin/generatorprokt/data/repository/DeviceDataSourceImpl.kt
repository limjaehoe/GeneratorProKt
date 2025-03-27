package com.androidkotlin.generatorprokt.data.repository

import com.androidkotlin.generatorprokt.data.device.DeviceDataSource
import com.androidkotlin.generatorprokt.data.model.DeviceDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceDataSourceImpl @Inject constructor() : DeviceDataSource {

    // 샘플 데이터를 위한 임시 구현
    private val deviceList = listOf(
        DeviceDto(id = "1", name = "Generator 1", connectionStatus = false),
        DeviceDto(id = "2", name = "Generator 2", connectionStatus = false),
        DeviceDto(id = "3", name = "Generator 3", connectionStatus = false)
    )

    override fun getDevices(): Flow<List<DeviceDto>> = flow {
        // 실제 구현에서는 API나 로컬 DB에서 데이터를 가져올 것입니다
        emit(deviceList)
    }

    override suspend fun connectDevice(deviceId: String): Result<Unit> {
        // 실제 연결 로직 구현
        return Result.success(Unit)
    }

    override suspend fun disconnectDevice(deviceId: String): Result<Unit> {
        // 실제 연결 해제 로직 구현
        return Result.success(Unit)
    }
}