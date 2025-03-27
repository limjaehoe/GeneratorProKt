package com.androidkotlin.generatorprokt.domain.repository

import com.androidkotlin.generatorprokt.domain.model.Device

import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDevices(): Flow<List<Device>>
    suspend fun connectDevice(deviceId: String): Result<Unit>
    suspend fun disconnectDevice(deviceId: String): Result<Unit>
}
