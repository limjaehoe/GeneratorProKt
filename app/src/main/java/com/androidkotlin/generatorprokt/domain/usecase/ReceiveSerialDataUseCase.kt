package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 시리얼 데이터를 수신하는 UseCase
 */
class ReceiveSerialDataUseCase @Inject constructor(
    private val serial422Repository: Serial422Repository
) {
    operator fun invoke(): Flow<SerialResponse> {
        return serial422Repository.receiveData()
    }
}