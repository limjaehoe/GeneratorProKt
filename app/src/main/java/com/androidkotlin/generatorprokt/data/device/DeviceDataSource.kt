package com.androidkotlin.generatorprokt.data.device

import com.androidkotlin.generatorprokt.data.model.DeviceDto
import kotlinx.coroutines.flow.Flow

interface DeviceDataSource {
    fun getDevices(): Flow<List<DeviceDto>>
    suspend fun connectDevice(deviceId: String): Result<Unit>
    suspend fun disconnectDevice(deviceId: String): Result<Unit>
}