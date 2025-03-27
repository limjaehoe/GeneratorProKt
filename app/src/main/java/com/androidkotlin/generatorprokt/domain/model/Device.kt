package com.androidkotlin.generatorprokt.domain.model

data class Device(
    val id: String,
    val name: String,
    val isConnected: Boolean = false
)