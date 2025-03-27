package com.androidkotlin.generatorprokt.data.model

import com.androidkotlin.generatorprokt.domain.model.Device

data class DeviceDto(
    val id: String,
    val name: String,
    val connectionStatus: Boolean
) {
    fun toDomain(): Device {
        return Device(
            id = id,
            name = name,
            isConnected = connectionStatus
        )
    }
}